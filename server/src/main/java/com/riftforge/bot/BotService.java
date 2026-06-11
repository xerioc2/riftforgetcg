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
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
  private final LegalActionsService legalActionsService;

  public BotService(@Lazy GameService gameService, CardDataService cardDataService, LegalActionsService legalActionsService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
    this.legalActionsService = legalActionsService;
  }

  @PostConstruct
  void logStartup() {
    log.info("BotService active. Bot IDs={}", ALL_BOT_IDS);
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

    List<String> playerIds = state.getPlayers().stream()
        .map(PlayerState::getUserId)
        .toList();
    boolean anyBotInGame = playerIds.stream().anyMatch(this::isBotId);

    log.info(
        "BotService received GameStateChangedEvent: room={}, phase={}, activePlayer={}, turnNumber={}, players={}, anyBotInGame={}",
        roomCode,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getTurnNumber(),
        playerIds,
        anyBotInGame);

    String actingBotId = actingBotId(state);
    boolean alreadyActing = actingRooms.contains(roomCode);
    log.info(
        "BotService bot selection: rawRoom={}, room={}, botIds={}, actingBotId={}, actingRoomsContainsRoom={}, actingRoomsBeforeAdd={}",
        event.getRoomCode(),
        roomCode,
        ALL_BOT_IDS,
        actingBotId,
        alreadyActing,
        actingRooms);
    if (!anyBotInGame) return;
    if (actingBotId == null) return;
    boolean addedActingRoom = actingRooms.add(roomCode);
    log.info("BotService acting-room guard: room={}, actingBotId={}, added={}, activeRooms={}", roomCode, actingBotId, addedActingRoom, actingRooms);
    if (!addedActingRoom) return;

    boolean isBotVsBot = state.getPlayers().stream()
        .allMatch(p -> isBotId(p.getUserId()));
    long delay = isBotVsBot ? 350L : 700L;
    final String botId = actingBotId;
    AtomicBoolean acted = new AtomicBoolean(false);
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(delay);
        LiveGameState current = gameService.currentState(roomCode);
        Set<LegalAction> currentLegalActions = current == null ? Set.of() : legalActions(current, botId);
        log.info(
            "Bot async started: room={}, bot={}, currentState={}, phase={}, activePlayer={}, legalActions={}",
            roomCode,
            botId,
            current == null ? "null" : "present",
            current == null ? null : current.getCurrentPhase(),
            current == null ? null : current.getActivePlayerId(),
            currentLegalActions);
        if (current == null || current.getWinnerId() != null) return;
        log.debug(
            "Bot acting: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}",
            roomCode,
            botId,
            current.getCurrentPhase(),
            current.getActivePlayerId(),
            current.getActiveShowdown() != null,
            current.getGameMode());
        acted.set(doActiveTurn(roomCode, current, botId));
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
        String nextBotId = latest == null ? null : actingBotId(latest);
        if (latest != null && nextBotId != null && (acted.get() || hasRelevantLegalAction(latest, nextBotId))) {
          onStateChanged(new GameStateChangedEvent(this, roomCode, latest));
        }
      }
    });
  }

  private boolean doActiveTurn(String roomCode, LiveGameState state, String botId) {
    Set<LegalAction> legalActions = legalActions(state, botId);
    log.debug(
        "Bot legal actions: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, actions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        legalActions);
    if (legalActions.isEmpty()) {
      logNoLegalAction(roomCode, state, botId, legalActions);
      return false;
    }
    if (state.getActiveShowdown() != null) {
      if (legalActions.contains(LegalAction.RESOLVE_SHOWDOWN)) {
        return processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
      } else {
        log.debug("Bot waiting during showdown: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
      }
      return false;
    }
    return switch (state.getCurrentPhase()) {
      case MULLIGAN -> {
        if (legalActions.contains(LegalAction.MULLIGAN) || legalActions.contains(LegalAction.KEEP_HAND)) {
          yield doMulligan(roomCode, state, botId);
        } else {
          log.warn("Bot cannot mulligan because no mulligan action is legal: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
          yield false;
        }
      }
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END -> passIfLegal(roomCode, state, botId, legalActions);
      case MAIN -> doMain(roomCode, state, botId);
    };
  }

  private boolean doMulligan(String roomCode, LiveGameState state, String botId) {
    return processBotMove(roomCode, state, botId, new MulliganMove(botId, List.of()));
  }

  private boolean doMain(String roomCode, LiveGameState state, String botId) {
    boolean acted = false;
    if (legalActions(state, botId).contains(LegalAction.MOVE_TO_BATTLEFIELD)) {
      List<CardInstance> readyChampions = state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.CHAMPION && !c.isTapped())
          .toList();
      for (CardInstance champion : readyChampions) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)) break;
        acted |= processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, champion.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        state = resolveShowdownIfLegal(roomCode, state, botId);
        if (state == null) return acted;
      }

      List<CardInstance> readyBaseCards = state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BASE && !c.isTapped())
          .toList();
      for (CardInstance card : readyBaseCards) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)) break;
        acted |= processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, card.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        state = resolveShowdownIfLegal(roomCode, state, botId);
        if (state == null) return acted;
      }
    }
    if (state == null) return acted;

    if (legalActions(state, botId).contains(LegalAction.TAP_RUNE)) {
      List<RuneState> untappedRunes = state.getRunes().stream()
          .filter(r -> botId.equals(r.getOwnerId()) && !r.isTapped())
          .toList();
      for (RuneState rune : untappedRunes) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.TAP_RUNE)) break;
        acted |= processBotMove(roomCode, state, botId, new TapRuneMove(botId, rune.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        if (state == null) return acted;
      }
    }

    LiveGameState playableState = gameService.currentState(roomCode);
    Set<LegalAction> playableActions = legalActions(playableState, botId);
    if (!playableActions.contains(LegalAction.PLAY_CARD)) {
      return passIfLegal(roomCode, playableState, botId, playableActions) || acted;
    }
    boolean anyPlayable = playableState != null && playableState.getCards().stream()
        .anyMatch(c -> botId.equals(c.getOwnerId())
            && c.getZone() == ZoneName.HAND
            && cardDataService.getCard(c.getCardId()).cost() <= botEnergy(playableState, botId));
    if (!anyPlayable) {
      return passIfLegal(roomCode, playableState, botId, playableActions) || acted;
    }

    boolean played = true;
    while (played) {
      played = false;
      LiveGameState current = gameService.currentState(roomCode);
      if (current == null || current.getWinnerId() != null || current.getCurrentPhase() != Phase.MAIN) break;
      if (!legalActions(current, botId).contains(LegalAction.PLAY_CARD)) break;
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
        acted |= processBotMove(roomCode, current, botId, new PlayCardMove(botId, pick.get().getInstanceId(), ZoneName.BASE, 60 + boardCount * 100, 220, targetInstanceId));
        sleepBriefly(300);
        played = true;
      }
    }
    LiveGameState latest = gameService.currentState(roomCode);
    return passIfLegal(roomCode, latest, botId, legalActions(latest, botId)) || acted;
  }

  private LiveGameState resolveShowdownIfLegal(String roomCode, LiveGameState state, String botId) {
    if (state == null || state.getActiveShowdown() == null) return state;
    Set<LegalAction> actions = legalActions(state, botId);
    if (!actions.contains(LegalAction.RESOLVE_SHOWDOWN)) {
      log.debug("Bot cannot resolve showdown yet: room={}, bot={}, legalActions={}", roomCode, botId, actions);
      return state;
    }
    processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
    sleepBriefly(200);
    return gameService.currentState(roomCode);
  }

  private boolean passIfLegal(String roomCode, LiveGameState state, String botId, Set<LegalAction> legalActions) {
    if (state == null) return false;
    if (legalActions.contains(LegalAction.PASS_PHASE) || legalActions.contains(LegalAction.END_TURN)) {
      return processBotMove(roomCode, state, botId, new PassPhaseMove(botId));
    }
    log.warn(
        "Bot cannot pass because PASS_PHASE is not legal: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}, legalActions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        state.getGameMode(),
        legalActions);
    return false;
  }

  private boolean processBotMove(String roomCode, LiveGameState state, String botId, MoveRequest move) {
    Phase beforePhase = state == null ? null : state.getCurrentPhase();
    String beforeActivePlayer = state == null ? null : state.getActivePlayerId();
    log.debug(
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
    log.debug(
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
    return true;
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

  private Set<LegalAction> legalActions(LiveGameState state, String botId) {
    return state == null ? Set.of() : legalActionsService.legalActionsFor(state, botId);
  }

  private boolean hasRelevantLegalAction(LiveGameState state, String botId) {
    Set<LegalAction> actions = legalActions(state, botId);
    if (actions.isEmpty()) return false;
    if (state.getActiveShowdown() != null) return actions.contains(LegalAction.RESOLVE_SHOWDOWN);
    return switch (state.getCurrentPhase()) {
      case MULLIGAN -> actions.contains(LegalAction.MULLIGAN) || actions.contains(LegalAction.KEEP_HAND);
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END ->
          actions.contains(LegalAction.PASS_PHASE) || actions.contains(LegalAction.END_TURN);
      case MAIN -> actions.contains(LegalAction.PLAY_CARD)
          || actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)
          || actions.contains(LegalAction.TAP_RUNE)
          || actions.contains(LegalAction.DISCARD_RUNE)
          || actions.contains(LegalAction.RESOLVE_SHOWDOWN)
          || actions.contains(LegalAction.PASS_PHASE)
          || actions.contains(LegalAction.END_TURN);
    };
  }

  private void logNoLegalAction(String roomCode, LiveGameState state, String botId, Set<LegalAction> legalActions) {
    log.warn(
        "Bot is active but has no legal actions: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}, legalActions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        state.getGameMode(),
        legalActions);
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
