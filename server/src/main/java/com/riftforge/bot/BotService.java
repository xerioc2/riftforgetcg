package com.riftforge.bot;

import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.model.move.MoveRequest;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.model.move.TapRuneMove;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BotService {
  private static final Logger log = LoggerFactory.getLogger(BotService.class);
  private final Set<String> actingRooms = ConcurrentHashMap.newKeySet();
  private final GameService gameService;
  private final CardDataService cardDataService;

  public BotService(@Lazy GameService gameService, CardDataService cardDataService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
  }

  @PostConstruct
  void logStartup() {
    log.info("BotService active; bot ids={}", ALL_BOT_IDS);
  }

  @Scheduled(fixedDelay = 1_000, initialDelay = 1_000)
  void recoverMissedBotTurns() {
    for (LiveGameState state : gameService.activeStates()) {
      if (state == null || state.getWinnerId() != null) continue;
      String roomCode = normalizeRoomCode(state.getRoomCode());
      String actingBotId = actingBotId(state);
      if (actingBotId == null || actingRooms.contains(roomCode)) continue;
      log.info(
          "Bot sweep found pending bot action: room={}, phase={}, activePlayer={}, actingBotId={}",
          roomCode,
          state.getCurrentPhase(),
          state.getActivePlayerId(),
          actingBotId);
      onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    }
  }

  @EventListener
  public void onStateChanged(GameStateChangedEvent event) {
    String roomCode = normalizeRoomCode(event.getRoomCode());
    LiveGameState state = event.getState();
    if (state.getWinnerId() != null) {
      log.debug("Bot event ignored for finished game: rawRoom={}, room={}, winner={}", event.getRoomCode(), roomCode, state.getWinnerId());
      return;
    }

    boolean anyBotInGame = state.getPlayers().stream()
        .anyMatch(p -> isBotId(p.getUserId()));
    List<String> playerIds = state.getPlayers().stream()
        .map(PlayerState::getUserId)
        .toList();

    String actingBotId = actingBotId(state);
    boolean alreadyActing = actingRooms.contains(roomCode);
    log.info(
        "Bot event received: rawRoom={}, room={}, phase={}, activePlayer={}, players={}, botIds={}, anyBotInGame={}, actingBotId={}, actingRoomsBeforeAdd={}, actingRoomAlready={}",
        event.getRoomCode(),
        roomCode,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        playerIds,
        ALL_BOT_IDS,
        anyBotInGame,
        actingBotId,
        actingRooms,
        alreadyActing);
    if (!anyBotInGame) return;
    if (actingBotId == null) return;
    boolean addedActingRoom = actingRooms.add(roomCode);
    log.info("Bot acting-room guard: room={}, added={}, activeRooms={}", roomCode, addedActingRoom, actingRooms);
    if (!addedActingRoom) return;

    boolean isBotVsBot = state.getPlayers().stream()
        .allMatch(p -> isBotId(p.getUserId()));
    long delay = isBotVsBot ? 350L : 700L;
    final String botId = actingBotId;
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(delay);
        LiveGameState current = gameService.currentState(roomCode);
        log.info(
            "Bot async started: room={}, bot={}, currentState={}, phase={}, activePlayer={}",
            roomCode,
            botId,
            current == null ? "null" : "present",
            current == null ? null : current.getCurrentPhase(),
            current == null ? null : current.getActivePlayerId());
        if (current == null || current.getWinnerId() != null) return;
        log.info(
            "Bot acting: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}",
            roomCode,
            botId,
            current.getCurrentPhase(),
            current.getActivePlayerId(),
            current.getActiveShowdown() != null,
            current.getGameMode());
        doActiveTurn(roomCode, current, botId);
      } catch (Exception e) {
        // A bot failure must never interrupt the game server.
        LiveGameState current = gameService.currentState(roomCode);
        log.warn(
            "Bot action failed: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}",
            roomCode,
            botId,
            current == null ? null : current.getCurrentPhase(),
            current == null ? null : current.getActivePlayerId(),
            current != null && current.getActiveShowdown() != null,
            current == null ? null : current.getGameMode(),
            e);
      } finally {
        actingRooms.remove(roomCode);
        LiveGameState latest = gameService.currentState(roomCode);
        if (latest != null) onStateChanged(new GameStateChangedEvent(this, roomCode, latest));
      }
    });
  }

  private void doActiveTurn(String roomCode, LiveGameState state, String botId) {
    if (state.getActiveShowdown() != null) {
      processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
      return;
    }
    switch (state.getCurrentPhase()) {
      case MULLIGAN -> doMulligan(roomCode, state, botId);
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END -> processBotMove(roomCode, state, botId, new PassPhaseMove(botId));
      case MAIN -> doMain(roomCode, state, botId);
    }
  }

  private void doMulligan(String roomCode, LiveGameState state, String botId) {
    processBotMove(roomCode, state, botId, new MulliganMove(botId, List.of()));
  }

  private void doMain(String roomCode, LiveGameState state, String botId) {
    List<CardInstance> readyChampions = state.getCards().stream()
        .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.CHAMPION && !c.isTapped())
        .toList();
    for (CardInstance champion : readyChampions) {
      int battlefieldCount = (int) state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD)
          .count();
      processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, champion.getInstanceId()));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
      if (state != null && state.getActiveShowdown() != null) {
        processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
      }
    }
    if (state == null) return;

    List<CardInstance> readyBaseCards = state.getCards().stream()
        .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BASE && !c.isTapped())
        .toList();
    for (CardInstance card : readyBaseCards) {
      int battlefieldCount = (int) state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD)
          .count();
      processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, card.getInstanceId()));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
      if (state != null && state.getActiveShowdown() != null) {
        processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
      }
    }
    if (state == null) return;

    List<RuneState> untappedRunes = state.getRunes().stream()
        .filter(r -> botId.equals(r.getOwnerId()) && !r.isTapped())
        .toList();
    for (RuneState rune : untappedRunes) {
      processBotMove(roomCode, state, botId, new TapRuneMove(botId, rune.getInstanceId()));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
      if (state == null) return;
    }

    LiveGameState playableState = gameService.currentState(roomCode);
    boolean anyPlayable = playableState != null && playableState.getCards().stream()
        .anyMatch(c -> botId.equals(c.getOwnerId())
            && c.getZone() == ZoneName.HAND
            && cardDataService.getCard(c.getCardId()).cost() <= botEnergy(playableState, botId));
    if (!anyPlayable) {
      processBotMove(roomCode, playableState, botId, new PassPhaseMove(botId));
      return;
    }

    boolean played = true;
    while (played) {
      played = false;
      LiveGameState current = gameService.currentState(roomCode);
      if (current == null || current.getWinnerId() != null || current.getCurrentPhase() != Phase.MAIN) break;
      int energy = botEnergy(current, botId);
      Optional<CardInstance> pick = current.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.HAND)
          .filter(c -> hasValidSpellTarget(current, c, botId))
          .filter(c -> cardDataService.getCard(c.getCardId()).cost() <= energy)
          .max(Comparator.comparingInt(c -> cardDataService.getCard(c.getCardId()).cost()));

      if (pick.isPresent()) {
        CardDefinition pickedDef = cardDataService.getCard(pick.get().getCardId());
        String targetInstanceId = targetForCard(current, pickedDef, botId)
            .map(CardInstance::getInstanceId)
            .orElse(null);
        int boardCount = (int) current.getCards().stream()
            .filter(c -> botId.equals(c.getOwnerId()) && (c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD))
            .count();
        processBotMove(roomCode, current, botId, new PlayCardMove(botId, pick.get().getInstanceId(), ZoneName.BASE, 60 + boardCount * 100, 220, targetInstanceId));
        sleepBriefly(300);
        played = true;
      }
    }
    processBotMove(roomCode, gameService.currentState(roomCode), botId, new PassPhaseMove(botId));
  }

  private void processBotMove(String roomCode, LiveGameState state, String botId, MoveRequest move) {
    Phase beforePhase = state == null ? null : state.getCurrentPhase();
    String beforeActivePlayer = state == null ? null : state.getActivePlayerId();
    log.info(
        "Bot move before: room={}, bot={}, move={}, phaseBefore={}, activePlayerBefore={}, activeShowdown={}, gameMode={}",
        roomCode,
        botId,
        move.getClass().getSimpleName(),
        state == null ? null : state.getCurrentPhase(),
        state == null ? null : state.getActivePlayerId(),
        state != null && state.getActiveShowdown() != null,
        state == null ? null : state.getGameMode());
    gameService.processMove(roomCode, move);
    LiveGameState latest = gameService.currentState(roomCode);
    boolean phaseChanged = latest != null && beforePhase != latest.getCurrentPhase();
    boolean activePlayerChanged = latest != null && beforeActivePlayer != null && !beforeActivePlayer.equals(latest.getActivePlayerId());
    log.info(
        "Bot move after: room={}, bot={}, move={}, latestPhase={}, latestActivePlayer={}, phaseChanged={}, activePlayerChanged={}, winner={}, activeShowdown={}",
        roomCode,
        botId,
        move.getClass().getSimpleName(),
        latest == null ? null : latest.getCurrentPhase(),
        latest == null ? null : latest.getActivePlayerId(),
        phaseChanged,
        activePlayerChanged,
        latest == null ? null : latest.getWinnerId(),
        latest != null && latest.getActiveShowdown() != null);
    if (move instanceof PassPhaseMove
        && latest != null
        && beforePhase == latest.getCurrentPhase()
        && botId.equals(latest.getActivePlayerId())
        && latest.getWinnerId() == null) {
      log.warn(
          "Bot PassPhaseMove did not advance phase: room={}, bot={}, phase={}, activePlayer={}",
          roomCode,
          botId,
          latest.getCurrentPhase(),
          latest.getActivePlayerId());
    }
  }

  private boolean hasValidSpellTarget(LiveGameState state, CardInstance card, String botId) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (cardDataService.isUnsupportedAction(def.id())) return false;
    if (!cardDataService.requiresBattlefieldTarget(def.id())) return true;
    return targetForCard(state, def, botId).isPresent();
  }

  private Optional<CardInstance> targetForCard(LiveGameState state, CardDefinition def, String botId) {
    if (!cardDataService.requiresBattlefieldTarget(def.id())) return Optional.empty();
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    boolean preferFriendly = cardDataService.requiresFriendlyTarget(def.id())
        || text.contains("give a unit")
        || text.contains("ready it");
    return state.getCards().stream()
        .filter(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD)
        .filter(candidate -> preferFriendly == botId.equals(candidate.getOwnerId()))
        .findFirst();
  }

  private int botEnergy(LiveGameState state, String botId) {
    return state.getPlayers().stream()
        .filter(p -> botId.equals(p.getUserId()))
        .findFirst()
        .map(PlayerState::getAvailableEnergy)
        .orElse(0);
  }

  private void sleepBriefly(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean isBotId(String playerId) {
    return playerId != null && ALL_BOT_IDS.stream().anyMatch(botId -> botId.equalsIgnoreCase(playerId));
  }

  private String actingBotId(LiveGameState state) {
    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      return state.getPlayers().stream()
          .map(PlayerState::getUserId)
          .filter(this::isBotId)
          .filter(id -> !state.getMulligansDone().contains(id))
          .findFirst()
          .orElse(null);
    }
    if (isBotId(state.getActivePlayerId())) return state.getActivePlayerId();
    return null;
  }

  private String normalizeRoomCode(String roomCode) {
    return roomCode == null ? "" : roomCode.toUpperCase();
  }
}
