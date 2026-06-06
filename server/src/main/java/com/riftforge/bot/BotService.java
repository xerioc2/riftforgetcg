package com.riftforge.bot;

import static com.riftforge.bot.BotConstants.BOT_ID;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.DeclareAttackMove;
import com.riftforge.model.move.DeclareBlockMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.MoveCardMove;
import com.riftforge.model.move.TapRuneMove;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BotService {
  private static final int BOT_DELAY_MS = 700;

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
    boolean botInGame = state.getPlayers().stream().anyMatch(p -> BOT_ID.equals(p.getUserId()));
    if (!botInGame) return;

    boolean botIsActive = BOT_ID.equals(state.getActivePlayerId());
    boolean botIsDefender = !botIsActive && state.getCurrentPhase() == Phase.BLOCK_DECLARE;
    boolean botShouldAct = state.getCurrentPhase() == Phase.BLOCK_DECLARE ? botIsDefender : botIsActive;
    if (!botShouldAct) return;
    if (!actingRooms.add(event.getRoomCode())) return;

    CompletableFuture.runAsync(() -> {
      try {
        sleepBriefly(BOT_DELAY_MS);
        LiveGameState current = gameService.currentState(event.getRoomCode());
        if (current == null || current.getWinnerId() != null) return;
        boolean defenderNow = !BOT_ID.equals(current.getActivePlayerId()) && current.getCurrentPhase() == Phase.BLOCK_DECLARE;
        if (defenderNow) doBlock(event.getRoomCode(), current);
        else if (BOT_ID.equals(current.getActivePlayerId())) doActiveTurn(event.getRoomCode(), current);
      } catch (Exception ignored) {
        // A bot failure must never interrupt the game server.
      } finally {
        actingRooms.remove(event.getRoomCode());
        LiveGameState latest = gameService.currentState(event.getRoomCode());
        if (latest != null) onStateChanged(new GameStateChangedEvent(this, event.getRoomCode(), latest));
      }
    });
  }

  private void doActiveTurn(String roomCode, LiveGameState state) {
    switch (state.getCurrentPhase()) {
      case CHANNEL, COMBAT_RESOLVE, END -> gameService.processMove(roomCode, new PassPhaseMove(BOT_ID));
      case MAIN -> doMain(roomCode, state);
      case ATTACK_DECLARE -> doAttack(roomCode, state);
      case BLOCK_DECLARE -> {
        // The defending player owns this phase.
      }
    }
  }

  private void doMain(String roomCode, LiveGameState state) {
    List<CardInstance> readyBaseCards = state.getCards().stream()
        .filter(c -> BOT_ID.equals(c.getOwnerId()) && c.getZone() == ZoneName.BASE && !c.isTapped())
        .toList();
    for (CardInstance card : readyBaseCards) {
      int battlefieldCount = (int) state.getCards().stream()
          .filter(c -> BOT_ID.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD)
          .count();
      gameService.processMove(roomCode, new MoveCardMove(BOT_ID, card.getInstanceId(), ZoneName.BATTLEFIELD, 60 + battlefieldCount * 100, 220));
      sleepBriefly(200);
      state = gameService.currentState(roomCode);
    }

    List<RuneState> untappedRunes = state.getRunes().stream()
        .filter(r -> BOT_ID.equals(r.getOwnerId()) && !r.isTapped())
        .toList();
    for (RuneState rune : untappedRunes) {
      gameService.processMove(roomCode, new TapRuneMove(BOT_ID, rune.getInstanceId()));
      sleepBriefly(200);
    }

    boolean played = true;
    while (played) {
      played = false;
      LiveGameState current = gameService.currentState(roomCode);
      if (current == null || current.getWinnerId() != null || current.getCurrentPhase() != Phase.MAIN) break;
      int energy = botEnergy(current);
      Optional<CardInstance> pick = current.getCards().stream()
          .filter(c -> BOT_ID.equals(c.getOwnerId()) && c.getZone() == ZoneName.HAND)
          .filter(c -> hasValidSpellTarget(current, c))
          .filter(c -> cardDataService.getCard(c.getCardId()).cost() <= energy)
          .max(Comparator.comparingInt(c -> cardDataService.getCard(c.getCardId()).cost()));

      if (pick.isPresent()) {
        int boardCount = (int) current.getCards().stream()
            .filter(c -> BOT_ID.equals(c.getOwnerId()) && (c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD))
            .count();
        gameService.processMove(roomCode, new PlayCardMove(BOT_ID, pick.get().getInstanceId(), ZoneName.BASE, 60 + boardCount * 100, 220));
        sleepBriefly(300);
        played = true;
      }
    }
    gameService.processMove(roomCode, new PassPhaseMove(BOT_ID));
  }

  private boolean hasValidSpellTarget(LiveGameState state, CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    boolean isTargetedSpell = "Spell".equalsIgnoreCase(def.type())
        && cardDataService.requiresBattlefieldTarget(def.id());
    if (!isTargetedSpell) return true;
    return state.getCards().stream().anyMatch(candidate ->
        candidate.getZone() == ZoneName.BATTLEFIELD
            && !BOT_ID.equals(candidate.getOwnerId()));
  }

  private void doAttack(String roomCode, LiveGameState state) {
    List<String> attackers = state.getCards().stream()
        .filter(c -> BOT_ID.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD && !c.isTapped() && !c.isHasSummoningSickness())
        .map(CardInstance::getInstanceId)
        .toList();
    String targetPlayerId = state.getPlayers().stream()
        .filter(p -> !BOT_ID.equals(p.getUserId()))
        .map(PlayerState::getUserId)
        .findFirst()
        .orElse(null);

    if (!attackers.isEmpty() && targetPlayerId != null) {
      gameService.processMove(roomCode, new DeclareAttackMove(BOT_ID, attackers, targetPlayerId));
    }
    gameService.processMove(roomCode, new PassPhaseMove(BOT_ID));
  }

  private void doBlock(String roomCode, LiveGameState state) {
    List<CardInstance> available = new ArrayList<>(state.getCards().stream()
        .filter(c -> BOT_ID.equals(c.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD && !c.isTapped())
        .toList());
    Map<String, String> blockMap = new HashMap<>();
    for (String attackerId : state.getDeclaredAttackers()) {
      if (available.isEmpty()) break;
      CardDefinition attackerDef = state.getCards().stream()
          .filter(c -> c.getInstanceId().equals(attackerId))
          .findFirst()
          .map(c -> cardDataService.getCard(c.getCardId()))
          .orElse(cardDataService.getCard(""));
      Optional<CardInstance> survivor = available.stream()
          .filter(b -> (b.getCurrentHealth() > 0 ? b.getCurrentHealth() : cardDataService.getCard(b.getCardId()).health()) > attackerDef.power())
          .findFirst();
      CardInstance chosen = survivor.orElse(available.get(0));
      blockMap.put(chosen.getInstanceId(), attackerId);
      available.remove(chosen);
    }
    gameService.processMove(roomCode, new DeclareBlockMove(BOT_ID, blockMap));
    gameService.processMove(roomCode, new PassPhaseMove(BOT_ID));
  }

  private int botEnergy(LiveGameState state) {
    return state.getPlayers().stream()
        .filter(p -> BOT_ID.equals(p.getUserId()))
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
