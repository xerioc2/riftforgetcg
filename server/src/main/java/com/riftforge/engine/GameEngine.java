package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.*;
import com.riftforge.model.move.*;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GameEngine {
  private static final int MAX_RUNES = 11;

  private final RulesValidator rulesValidator;
  private final CombatResolver combatResolver;
  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final int targetScore;

  public GameEngine(RulesValidator rulesValidator, CombatResolver combatResolver, CardDataService cardDataService, CardEffectRegistry effects, @Value("${riftforge.target-score}") int targetScore) {
    this.rulesValidator = rulesValidator;
    this.combatResolver = combatResolver;
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.targetScore = targetScore;
  }

  public LiveGameState applyMove(LiveGameState state, MoveRequest move) {
    rulesValidator.validate(state, move);
    LiveGameState next = switch (move) {
      case DealCardMove m -> applyDealCard(state, m);
      case TapCardMove m -> applyTapCard(state, m);
      case FlipCardMove m -> applyFlipCard(state, m);
      case PlayCardMove m -> applyPlayCard(state, m);
      case MoveCardMove m -> applyMoveCard(state, m);
      case TapRuneMove m -> applyTapRune(state, m);
      case DiscardRuneMove m -> applyDiscardRune(state, m);
      case DeclareAttackMove m -> applyDeclareAttack(state, m);
      case DeclareBlockMove m -> applyDeclareBlock(state, m);
      case PassPhaseMove m -> applyPassPhase(state);
      case AdjustScoreMove m -> applyAdjustScore(state, m);
    };
    next.setUpdatedAt(Instant.now().toString());
    checkWinCondition(next);
    return next;
  }

  private LiveGameState applyDealCard(LiveGameState state, DealCardMove move) {
    CardDefinition def = cardDataService.getCard(move.cardId());
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance instance = new CardInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setCardId(move.cardId());
    instance.setOwnerId(move.playerId());
    instance.setZone(ZoneName.valueOf(move.targetZone().toUpperCase()));
    instance.setX(move.x());
    instance.setY(move.y());
    instance.setCurrentHealth(def.health());
    instance.setHasSummoningSickness(true);
    instance.setTempKeywords(new ArrayList<>());
    instance.setZIndex(maxZ + 1);
    state.getCards().add(instance);
    log(state, move.playerId(), "Drew " + def.name());
    return state;
  }

  private LiveGameState applyTapCard(LiveGameState state, TapCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    card.setTapped(!card.isTapped());
    String action = card.isTapped() ? "tapped" : "untapped";
    log(state, move.playerId(), "Player " + action + " " + cardDataService.getCard(card.getCardId()).name());
    return state;
  }

  private LiveGameState applyFlipCard(LiveGameState state, FlipCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    card.setFaceDown(!card.isFaceDown());
    log(state, move.playerId(), "Player flipped " + cardDataService.getCard(card.getCardId()).name());
    return state;
  }

  private LiveGameState applyPlayCard(LiveGameState state, PlayCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    CardDefinition def = cardDataService.getCard(card.getCardId());
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> p.setAvailableEnergy(p.getAvailableEnergy() - def.cost()));
    card.setZone(move.targetZone());
    card.setX(move.x());
    card.setY(move.y());
    card.setCurrentHealth(def.health());
    card.setHasSummoningSickness(!def.keywords().contains("RUSH"));
    if (move.targetZone() == ZoneName.BASE) card.setTapped(true);
    CardInstance target = move.targetInstanceId() == null ? null : state.getCards().stream()
        .filter(candidate -> candidate.getInstanceId().equals(move.targetInstanceId()))
        .findFirst()
        .orElse(null);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onPlay(card, target, state));
    String cardTypeLower = def.type() != null ? def.type().toLowerCase() : "";
    if (cardTypeLower.equals("spell") || cardTypeLower.equals("gear")) {
      card.setZone(ZoneName.DISCARD);
    }
    log(state, move.playerId(), "Played " + def.name());
    return state;
  }

  private LiveGameState applyMoveCard(LiveGameState state, MoveCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    ZoneName sourceZone = card.getZone();
    card.setZone(move.targetZone());
    card.setX(move.x());
    card.setY(move.y());
    if (move.targetZone() == ZoneName.BATTLEFIELD
        && (sourceZone == ZoneName.BASE || sourceZone == ZoneName.CHAMPION || sourceZone == ZoneName.LEGEND)) {
      card.setTapped(true);
      card.setHasSummoningSickness(false);
    }
    log(state, move.playerId(), "Moved " + cardDataService.getCard(card.getCardId()).name() + " to " + move.targetZone());
    return state;
  }

  private LiveGameState applyTapRune(LiveGameState state, TapRuneMove move) {
    RuneState rune = findRune(state, move.runeInstanceId());
    rune.setTapped(true);
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> p.setAvailableEnergy(p.getAvailableEnergy() + rune.getNormalEnergy()));
    log(state, move.playerId(), "Tapped a rune.");
    return state;
  }

  private LiveGameState applyDiscardRune(LiveGameState state, DiscardRuneMove move) {
    RuneState rune = findRune(state, move.runeInstanceId());
    state.getRunes().remove(rune);
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> p.setAvailableEnergy(p.getAvailableEnergy() + rune.getPremiumEnergy()));
    log(state, move.playerId(), "Discarded a rune.");
    return state;
  }

  private LiveGameState applyDeclareAttack(LiveGameState state, DeclareAttackMove move) {
    state.setDeclaredAttackers(move.attackerInstanceIds());
    move.attackerInstanceIds().forEach(id ->
        state.getCards().stream()
            .filter(card -> card.getInstanceId().equals(id))
            .findFirst()
            .ifPresent(card -> card.setTapped(true)));
    move.attackerInstanceIds().forEach(id ->
        state.getCards().stream()
            .filter(card -> card.getInstanceId().equals(id))
            .findFirst()
            .ifPresent(card -> effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onAttack(card, state))));
    log(state, move.playerId(), "Declared attackers.");
    return state;
  }

  private LiveGameState applyDeclareBlock(LiveGameState state, DeclareBlockMove move) {
    state.setBlockerToAttacker(move.blockerToAttacker());
    log(state, move.playerId(), "Declared blockers.");
    return state;
  }

  private LiveGameState applyPassPhase(LiveGameState state) {
    Phase next = switch (state.getCurrentPhase()) {
      case CHANNEL -> Phase.MAIN;
      case MAIN -> Phase.ATTACK_DECLARE;
      case ATTACK_DECLARE -> Phase.BLOCK_DECLARE;
      case BLOCK_DECLARE -> Phase.COMBAT_RESOLVE;
      case COMBAT_RESOLVE -> Phase.END;
      case END -> Phase.CHANNEL;
    };
    state.setCurrentPhase(next);
    if (next == Phase.COMBAT_RESOLVE) {
      List<String> attackers = new ArrayList<>(state.getDeclaredAttackers());
      combatResolver.resolve(state);
      state.setDeclaredAttackers(attackers);
      log(state, state.getActivePlayerId(), "Combat resolved.");
      state.setCurrentPhase(Phase.END);
      next = Phase.END;
      returnAttackersToBase(state);
      healBoardCards(state);
      log(state, state.getActivePlayerId(), "Advanced to END.");
      return state;
    }
    if (next == Phase.END) {
      returnAttackersToBase(state);
      healBoardCards(state);
    }
    if (next == Phase.CHANNEL) {
      advanceTurn(state);
      scoreUnchallengedBattlefield(state);
      grantRunes(state, state.getActivePlayerId(), 2);
      autoDraw(state, state.getActivePlayerId());
      state.setCurrentPhase(Phase.MAIN);
      next = Phase.MAIN;
    }
    log(state, state.getActivePlayerId(), "Advanced to " + next);
    return state;
  }

  private LiveGameState applyAdjustScore(LiveGameState state, AdjustScoreMove move) {
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.targetPlayerId())).findFirst().ifPresent(p -> p.setScore(Math.max(0, p.getScore() + move.delta())));
    log(state, move.playerId(), "Adjusted score.");
    return state;
  }

  private void advanceTurn(LiveGameState state) {
    int index = 0;
    for (int i = 0; i < state.getPlayers().size(); i++) if (state.getPlayers().get(i).getUserId().equals(state.getActivePlayerId())) index = i;
    PlayerState next = state.getPlayers().get((index + 1) % state.getPlayers().size());
    state.setActivePlayerId(next.getUserId());
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(next.getUserId()) && c.getZone() == ZoneName.BATTLEFIELD)
        .forEach(c -> effects.getEffect(c.getCardId()).ifPresent(effect -> effect.onTurnStart(c, state)));
    state.setTurnNumber(state.getTurnNumber() + 1);
    next.setAvailableEnergy(0);
    state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(next.getUserId()))
        .forEach(r -> r.setTapped(false));
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(next.getUserId()))
        .forEach(c -> c.setTapped(false));
  }

  private void grantRunes(LiveGameState state, String playerId, int amount) {
    long currentCount = state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(playerId))
        .count();
    for (int i = 0; i < amount && currentCount < MAX_RUNES; i++, currentCount++) {
      RuneState rune = new RuneState();
      rune.setInstanceId(UUID.randomUUID().toString());
      rune.setOwnerId(playerId);
      rune.setTapped(false);
      rune.setNormalEnergy(1);
      rune.setPremiumEnergy(2);
      state.getRunes().add(rune);
    }
  }

  private void scoreUnchallengedBattlefield(LiveGameState state) {
    String activePlayerId = state.getActivePlayerId();
    boolean controlsBattlefield = state.getCards().stream()
        .anyMatch(c -> c.getZone() == ZoneName.BATTLEFIELD && c.getOwnerId().equals(activePlayerId));
    boolean challenged = state.getCards().stream()
        .anyMatch(c -> c.getZone() == ZoneName.BATTLEFIELD && !c.getOwnerId().equals(activePlayerId));
    if (!controlsBattlefield || challenged) return;
    state.getPlayers().stream()
        .filter(p -> p.getUserId().equals(activePlayerId))
        .findFirst()
        .ifPresent(p -> p.setScore(p.getScore() + 1));
    log(state, activePlayerId, "Scored 1 point for controlling the battlefield.");
  }

  private void returnAttackersToBase(LiveGameState state) {
    for (String attackerId : state.getDeclaredAttackers()) {
      state.getCards().stream()
          .filter(c -> c.getInstanceId().equals(attackerId) && c.getZone() == ZoneName.BATTLEFIELD)
          .findFirst()
          .ifPresent(c -> {
            c.setZone(ZoneName.BASE);
            c.setTapped(true);
          });
    }
    state.setDeclaredAttackers(new ArrayList<>());
    state.setBlockerToAttacker(new java.util.HashMap<>());
  }

  private void healBoardCards(LiveGameState state) {
    state.getCards().stream()
        .filter(c -> c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD)
        .forEach(c -> {
          CardDefinition def = cardDataService.getCard(c.getCardId());
          if (def != null && def.health() > 0) c.setCurrentHealth(def.health());
          c.setHasSummoningSickness(false);
          c.getTempKeywords().clear();
        });
  }

  private void autoDraw(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(p -> p.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null || player.getDeckPool().isEmpty()) {
      log(state, playerId, "No cards left to draw.");
      return;
    }
    String cardId = player.getDeckPool().remove(0);
    CardDefinition def = cardDataService.getCard(cardId);
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance instance = new CardInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setCardId(cardId);
    instance.setOwnerId(playerId);
    instance.setZone(ZoneName.HAND);
    instance.setX(0);
    instance.setY(0);
    instance.setCurrentHealth(def.health());
    instance.setHasSummoningSickness(false);
    instance.setTempKeywords(new ArrayList<>());
    instance.setZIndex(maxZ + 1);
    state.getCards().add(instance);
    log(state, playerId, "Drew " + def.name() + ".");
  }

  private void checkWinCondition(LiveGameState state) {
    state.getPlayers().stream().filter(p -> p.getScore() >= targetScore).findFirst().ifPresent(p -> state.setWinnerId(p.getUserId()));
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  private RuneState findRune(LiveGameState state, String id) {
    return state.getRunes().stream().filter(r -> r.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  public static void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(UUID.randomUUID().toString(), Instant.now().toString(), userId, text));
  }
}
