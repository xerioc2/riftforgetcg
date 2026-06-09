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
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BotService {
  private final Set<String> actingRooms = ConcurrentHashMap.newKeySet();
  private final GameService gameService;
  private final CardDataService cardDataService;

  public BotService(@Lazy GameService gameService, CardDataService cardDataService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
  }

  @EventListener
  public void onStateChanged(GameStateChangedEvent event) {
    LiveGameState state = event.getState();
    if (state.getWinnerId() != null) return;

    boolean anyBotInGame = state.getPlayers().stream()
        .anyMatch(p -> ALL_BOT_IDS.contains(p.getUserId()));
    if (!anyBotInGame) return;

    String actingBotId = null;
    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      actingBotId = state.getPlayers().stream()
          .map(PlayerState::getUserId)
          .filter(ALL_BOT_IDS::contains)
          .filter(id -> !state.getMulligansDone().contains(id))
          .findFirst()
          .orElse(null);
    } else if (ALL_BOT_IDS.contains(state.getActivePlayerId())) {
      actingBotId = state.getActivePlayerId();
    }
    if (actingBotId == null) return;
    if (!actingRooms.add(event.getRoomCode())) return;

    boolean isBotVsBot = state.getPlayers().stream()
        .allMatch(p -> ALL_BOT_IDS.contains(p.getUserId()));
    long delay = isBotVsBot ? 350L : 700L;
    final String botId = actingBotId;
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(delay);
        LiveGameState current = gameService.currentState(event.getRoomCode());
        if (current == null || current.getWinnerId() != null) return;
        doActiveTurn(event.getRoomCode(), current, botId);
      } catch (Exception ignored) {
        // A bot failure must never interrupt the game server.
      } finally {
        actingRooms.remove(event.getRoomCode());
        LiveGameState latest = gameService.currentState(event.getRoomCode());
        if (latest != null) onStateChanged(new GameStateChangedEvent(this, event.getRoomCode(), latest));
      }
    });
  }

  private void doActiveTurn(String roomCode, LiveGameState state, String botId) {
    if (state.getActiveShowdown() != null) {
      gameService.processMove(roomCode, new ResolveShowdownMove(botId));
      return;
    }
    switch (state.getCurrentPhase()) {
      case MULLIGAN -> doMulligan(roomCode, state, botId);
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END -> gameService.processMove(roomCode, new PassPhaseMove(botId));
      case MAIN -> doMain(roomCode, state, botId);
    }
  }

  private void doMulligan(String roomCode, LiveGameState state, String botId) {
    gameService.processMove(roomCode, new MulliganMove(botId, List.of()));
  }

  private void doMain(String roomCode, LiveGameState state, String botId) {
    List<CardInstance> readyChampions = state.getCards().stream()
        .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.CHAMPION && !c.isTapped())
        .toList();
    for (CardInstance champion : readyChampions) {
      int battlefieldCount = (int) state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD)
          .count();
      gameService.processMove(roomCode, new MoveToBattlefieldMove(botId, champion.getInstanceId()));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
      if (state != null && state.getActiveShowdown() != null) {
        gameService.processMove(roomCode, new ResolveShowdownMove(botId));
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
      gameService.processMove(roomCode, new MoveToBattlefieldMove(botId, card.getInstanceId()));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
      if (state != null && state.getActiveShowdown() != null) {
        gameService.processMove(roomCode, new ResolveShowdownMove(botId));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
      }
    }
    if (state == null) return;

    List<RuneState> untappedRunes = state.getRunes().stream()
        .filter(r -> botId.equals(r.getOwnerId()) && !r.isTapped())
        .toList();
    for (RuneState rune : untappedRunes) {
      gameService.processMove(roomCode, new TapRuneMove(botId, rune.getInstanceId()));
      sleepBriefly(200);
    }

    LiveGameState playableState = gameService.currentState(roomCode);
    boolean anyPlayable = playableState != null && playableState.getCards().stream()
        .anyMatch(c -> botId.equals(c.getOwnerId())
            && c.getZone() == ZoneName.HAND
            && cardDataService.getCard(c.getCardId()).cost() <= botEnergy(playableState, botId));
    if (!anyPlayable) {
      gameService.processMove(roomCode, new PassPhaseMove(botId));
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
        gameService.processMove(roomCode, new PlayCardMove(botId, pick.get().getInstanceId(), ZoneName.BASE, 60 + boardCount * 100, 220, targetInstanceId));
        sleepBriefly(300);
        played = true;
      }
    }
    gameService.processMove(roomCode, new PassPhaseMove(botId));
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
}
