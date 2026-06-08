package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.*;
import com.riftforge.model.move.*;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GameEngine {
  private static final int MAX_RUNES = 11;

  private final RulesValidator rulesValidator;
  private final CombatResolver combatResolver;
  private final CardZoneService cardZoneService;
  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final int targetScore;

  public GameEngine(RulesValidator rulesValidator, CombatResolver combatResolver, CardZoneService cardZoneService, CardDataService cardDataService, CardEffectRegistry effects, @Value("${riftforge.target-score}") int targetScore) {
    this.rulesValidator = rulesValidator;
    this.combatResolver = combatResolver;
    this.cardZoneService = cardZoneService;
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
      case MoveToBattlefieldMove m -> applyMoveToBattlefield(state, m);
      case MulliganMove m -> applyMulligan(state, m);
      case UndoRunesMove m -> applyUndoRunes(state, m);
      case PassPhaseMove m -> applyPassPhase(state);
      case AdjustScoreMove m -> applyAdjustScore(state, m);
      case VisionChoiceMove m -> applyVisionChoice(state, m);
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
    boolean cardPlayedEarlierThisTurn = state.isCardPlayedThisTurn();
    int paidCost = def.cost() + (move.accelerate() ? 1 : 0);
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> p.setAvailableEnergy(p.getAvailableEnergy() - paidCost));
    card.setZone(move.targetZone());
    card.setX(move.x());
    card.setY(move.y());
    card.setCurrentHealth(def.health());
    card.setHasSummoningSickness(!move.accelerate());
    if (move.targetZone() == ZoneName.BASE) card.setTapped(!move.accelerate());
    applyLegion(state, card, cardPlayedEarlierThisTurn);
    state.setCardPlayedThisTurn(true);
    CardInstance target = move.targetInstanceId() == null ? null : state.getCards().stream()
        .filter(candidate -> candidate.getInstanceId().equals(move.targetInstanceId()))
        .findFirst()
        .orElse(null);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onPlay(card, target, state));
    if (move.accelerate()) log(state, move.playerId(), def.name() + " entered ready with Accelerate.");
    if (cardDataService.hasKeyword(card, "VISION")) {
      String topCardId = player(state, move.playerId()).getDeckPool().stream().findFirst().orElse("");
      String topCardName = topCardId.isBlank() ? "No card" : cardDataService.getCard(topCardId).name();
      log(state, move.playerId(), "VISION_PEEK|" + topCardId + "|" + topCardName);
    }
    String cardTypeLower = def.type() != null ? def.type().toLowerCase() : "";
    applyRulesTextEffect(card, target, state, def);
    moveDestroyedBoardCards(state);
    if (cardTypeLower.equals("spell")) {
      cardZoneService.moveToGraveyard(card);
    } else if (cardDataService.isEquip(def) && target != null) {
      card.setZone(ZoneName.BASE);
      card.setAttachedToInstanceId(target.getInstanceId());
      card.setX(target.getX() + 34);
      card.setY(target.getY() + 34);
      card.setTapped(false);
    }
    log(state, move.playerId(), "Played " + def.name());
    return state;
  }

  private LiveGameState applyMoveCard(LiveGameState state, MoveCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    ZoneName sourceZone = card.getZone();
    if (move.targetZone() == ZoneName.DISCARD) cardZoneService.moveToGraveyard(card);
    else card.setZone(move.targetZone());
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
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> {
      p.setAvailableEnergy(p.getAvailableEnergy() + rune.getPremiumEnergy());
      p.setRunePoolRemaining(Math.max(0, p.getRunePoolRemaining() - 1));
    });
    log(state, move.playerId(), "Discarded a rune.");
    return state;
  }

  private LiveGameState applyMulligan(LiveGameState state, MulliganMove move) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(move.playerId()))
        .findFirst()
        .orElseThrow();
    List<CardInstance> returned = state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(move.playerId()))
        .filter(card -> card.getZone() == ZoneName.HAND)
        .filter(card -> move.discardInstanceIds().contains(card.getInstanceId()))
        .toList();
    state.getCards().removeAll(returned);
    for (int i = 0; i < returned.size() && !player.getDeckPool().isEmpty(); i++) autoDraw(state, move.playerId());
    returned.forEach(card -> player.getDeckPool().add(card.getCardId()));
    Collections.shuffle(player.getDeckPool());
    state.getMulligansDone().add(move.playerId());
    log(state, move.playerId(), returned.isEmpty() ? "Kept their opening hand." : "Mulliganed " + returned.size() + " card(s).");
    if (state.getMulligansDone().size() == state.getPlayers().size()) {
      state.setCurrentPhase(Phase.AWAKEN);
      log(state, state.getActivePlayerId(), "Mulligans complete. Advanced to AWAKEN.");
    }
    return state;
  }

  private LiveGameState applyMoveToBattlefield(LiveGameState state, MoveToBattlefieldMove move) {
    CardInstance card = findCard(state, move.instanceId());
    CardDefinition def = cardDataService.getCard(card.getCardId());
    card.setZone(ZoneName.BATTLEFIELD);
    card.setTapped(true);
    card.setHasSummoningSickness(false);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onAttack(card, state));
    log(state, move.playerId(), "Moved " + def.name() + " to the battlefield.");
    boolean opposed = state.getCards().stream()
        .anyMatch(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD && !move.playerId().equals(candidate.getOwnerId()));
    if (!opposed) {
      state.getBattlefieldController().put("BATTLEFIELD", move.playerId());
      return state;
    }
    state.setCurrentPhase(Phase.COMBAT);
    CombatResolver.CombatResult result = combatResolver.resolve(state, move.playerId());
    if (result.attackersRemain() && result.defendersEliminated()) conquerBattlefield(state, move.playerId());
    else if (!result.defendersEliminated()) returnBattlefieldCardsToBase(state, move.playerId());
    log(state, move.playerId(), "Combat resolved.");
    state.setCurrentPhase(Phase.MAIN);
    return state;
  }

  private LiveGameState applyUndoRunes(LiveGameState state, UndoRunesMove move) {
    state.getRunes().stream()
        .filter(rune -> rune.getOwnerId().equals(move.playerId()) && rune.isTapped())
        .forEach(rune -> rune.setTapped(false));
    state.getPlayers().stream()
        .filter(player -> player.getUserId().equals(move.playerId()))
        .findFirst()
        .ifPresent(player -> player.setAvailableEnergy(0));
    log(state, move.playerId(), "Undid rune taps.");
    return state;
  }

  private LiveGameState applyPassPhase(LiveGameState state) {
    Phase next;
    switch (state.getCurrentPhase()) {
      case MULLIGAN -> next = Phase.AWAKEN;
      case AWAKEN -> { awaken(state); next = Phase.BEGINNING; }
      case BEGINNING -> { applyBeginning(state); next = Phase.CHANNEL; }
      case CHANNEL -> { applyChannel(state); next = Phase.DRAW; }
      case DRAW -> { applyDraw(state); clearEnergy(state); next = Phase.MAIN; }
      case MAIN -> next = Phase.END;
      case COMBAT -> next = Phase.MAIN;
      case END -> {
        finishEndPhase(state);
        clearEnergy(state);
        advanceTurn(state);
        next = Phase.AWAKEN;
      }
      default -> next = Phase.AWAKEN;
    }
    state.setCurrentPhase(next);
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
    state.setTurnNumber(state.getTurnNumber() + 1);
    state.setCardPlayedThisTurn(false);
    state.setScoredBattlefieldsThisTurn(new HashSet<>());
  }

  private LiveGameState applyVisionChoice(LiveGameState state, VisionChoiceMove move) {
    PlayerState player = player(state, move.playerId());
    if (player.getDeckPool().isEmpty()) {
      log(state, move.playerId(), "Vision found no card to move.");
      return state;
    }
    String topCardId = player.getDeckPool().get(0);
    if (move.recycle()) {
      player.getDeckPool().remove(0);
      player.getDeckPool().add(topCardId);
      log(state, move.playerId(), "VISION_RESOLVED|Recycled " + cardDataService.getCard(topCardId).name() + " to the bottom of the deck.");
    } else {
      log(state, move.playerId(), "VISION_RESOLVED|Kept " + cardDataService.getCard(topCardId).name() + " on top of the deck.");
    }
    return state;
  }

  private void awaken(LiveGameState state) {
    PlayerState active = player(state, state.getActivePlayerId());
    active.setAvailableEnergy(0);
    state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(active.getUserId()))
        .forEach(r -> r.setTapped(false));
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(active.getUserId()))
        .forEach(c -> c.setTapped(false));
  }

  private void applyBeginning(LiveGameState state) {
    expireTemporaryCards(state);
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(state.getActivePlayerId()) && (c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD))
        .forEach(c -> effects.getEffect(c.getCardId()).ifPresent(effect -> effect.onTurnStart(c, state)));
    scoreHeldBattlefield(state);
  }

  private void applyChannel(LiveGameState state) {
    int amount = !state.getActivePlayerId().equals(state.getFirstPlayerId()) && state.getTurnNumber() == 2 ? 3 : 2;
    grantRunes(state, state.getActivePlayerId(), amount);
  }

  private void applyDraw(LiveGameState state) {
    if (state.getTurnNumber() == 1 && state.getActivePlayerId().equals(state.getFirstPlayerId())) {
      log(state, state.getActivePlayerId(), "First player skips their first draw.");
      return;
    }
    autoDraw(state, state.getActivePlayerId());
  }

  private void clearEnergy(LiveGameState state) {
    player(state, state.getActivePlayerId()).setAvailableEnergy(0);
  }

  private void grantRunes(LiveGameState state, String playerId, int amount) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getRunePoolRemaining() <= 0) {
      log(state, playerId, player.getName() + "'s rune deck is exhausted.");
      return;
    }
    long currentCount = state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(playerId))
        .count();
    int granted = 0;
    for (int i = 0; i < amount && currentCount < MAX_RUNES && granted < player.getRunePoolRemaining(); i++, currentCount++, granted++) {
      RuneState rune = new RuneState();
      rune.setInstanceId(UUID.randomUUID().toString());
      rune.setOwnerId(playerId);
      rune.setTapped(false);
      rune.setNormalEnergy(1);
      rune.setPremiumEnergy(2);
      state.getRunes().add(rune);
    }
    player.setRunePoolRemaining(player.getRunePoolRemaining() - granted);
  }

  private void scoreHeldBattlefield(LiveGameState state) {
    String activePlayerId = state.getActivePlayerId();
    if (!activePlayerId.equals(state.getBattlefieldController().get("BATTLEFIELD"))) return;
    if (!state.getScoredBattlefieldsThisTurn().add("BATTLEFIELD")) return;
    PlayerState player = player(state, activePlayerId);
    player.setScore(player.getScore() + 1);
    log(state, activePlayerId, "Held the battlefield - score +1.");
  }

  private void conquerBattlefield(LiveGameState state, String playerId) {
    state.getBattlefieldController().put("BATTLEFIELD", playerId);
    if (state.getScoredBattlefieldsThisTurn().contains("BATTLEFIELD")) return;
    PlayerState scorer = player(state, playerId);
    boolean scoredAllBattlefields = state.getScoredBattlefieldsThisTurn().containsAll(List.of("BATTLEFIELD"));
    state.getScoredBattlefieldsThisTurn().add("BATTLEFIELD");
    if (scorer.getScore() >= targetScore - 1 && !scoredAllBattlefields) {
      autoDraw(state, scorer.getUserId());
      log(state, scorer.getUserId(), "Conquers but draws a card (must score all battlefields to win).");
      return;
    }
    scorer.setScore(scorer.getScore() + 1);
    log(state, scorer.getUserId(), scorer.getScore() >= targetScore ? "Conquers for the winning point!" : "Conquers - +1 point.");
  }

  private void returnBattlefieldCardsToBase(LiveGameState state, String playerId) {
    state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && playerId.equals(card.getOwnerId()))
        .forEach(card -> card.setZone(ZoneName.BASE));
  }

  private void healBoardCards(LiveGameState state) {
    state.getCards().stream()
        .filter(c -> c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD)
        .forEach(c -> {
          CardDefinition def = cardDataService.getCard(c.getCardId());
          if (def != null && def.health() > 0) c.setCurrentHealth(def.health());
          c.setHasSummoningSickness(false);
          c.setTemporaryPowerModifier(0);
          c.getTempKeywords().clear();
        });
  }

  private void finishEndPhase(LiveGameState state) {
    state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(state.getActivePlayerId()) && card.getZone() == ZoneName.BATTLEFIELD)
        .forEach(card -> effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onTurnEnd(card, state)));
    healBoardCards(state);
  }

  private PlayerState player(LiveGameState state, String playerId) {
    return state.getPlayers().stream().filter(player -> playerId.equals(player.getUserId())).findFirst().orElseThrow();
  }

  private void applyLegion(LiveGameState state, CardInstance card, boolean cardPlayedEarlierThisTurn) {
    if (!cardPlayedEarlierThisTurn || !cardDataService.hasKeyword(card, "LEGION")) return;
    card.setMightBonus(card.getMightBonus() + 1);
    log(state, card.getOwnerId(), cardDataService.getCard(card.getCardId()).name() + " gains Legion +1 might.");
  }

  private void expireTemporaryCards(LiveGameState state) {
    List<CardInstance> expired = state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(state.getActivePlayerId()))
        .filter(card -> card.getZone() == ZoneName.BASE || card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> cardDataService.hasKeyword(card, "TEMPORARY"))
        .toList();
    for (CardInstance card : expired) {
      CardDefinition def = cardDataService.getCard(card.getCardId());
      state.getCards().stream()
          .filter(attachment -> card.getInstanceId().equals(attachment.getAttachedToInstanceId()))
          .toList()
          .forEach(attachment -> {
            cardZoneService.moveToGraveyard(attachment);
            attachment.setAttachedToInstanceId(null);
          });
      cardZoneService.moveToGraveyard(card);
      effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
      log(state, card.getOwnerId(), def.name() + " expired (Temporary).");
    }
  }

  private void moveDestroyedBoardCards(LiveGameState state) {
    List<CardInstance> destroyed = state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BASE || card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> {
          CardDefinition def = cardDataService.getCard(card.getCardId());
          return def != null
              && ("Champion".equalsIgnoreCase(def.type()) || "Unit".equalsIgnoreCase(def.type()))
              && card.getCurrentHealth() <= 0;
        })
        .toList();
    for (CardInstance card : destroyed) {
      state.getCards().stream()
          .filter(attachment -> card.getInstanceId().equals(attachment.getAttachedToInstanceId()))
          .forEach(attachment -> {
            cardZoneService.moveToGraveyard(attachment);
            attachment.setAttachedToInstanceId(null);
          });
      cardZoneService.moveToGraveyard(card);
      effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
    }
  }

  private void applyRulesTextEffect(CardInstance card, CardInstance target, LiveGameState state, CardDefinition def) {
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    if (target != null) {
      Matcher powerBoost = Pattern.compile("\\+(\\d+)\\s*:rb_might:").matcher(text);
      if (powerBoost.find()) {
        target.setTemporaryPowerModifier(target.getTemporaryPowerModifier() + Integer.parseInt(powerBoost.group(1)));
        if (text.contains("additional +1") && state.getCards().stream()
            .filter(candidate -> candidate.getOwnerId().equals(target.getOwnerId()) && candidate.getZone() == target.getZone())
            .count() == 1) {
          target.setTemporaryPowerModifier(target.getTemporaryPowerModifier() + 1);
        }
      }
      if (text.contains("return a unit") || text.contains("return target unit")) {
        state.getCards().stream()
            .filter(candidate -> target.getInstanceId().equals(candidate.getAttachedToInstanceId()))
            .forEach(candidate -> {
              cardZoneService.moveToGraveyard(candidate);
              candidate.setAttachedToInstanceId(null);
            });
        target.setZone(ZoneName.HAND);
        target.setTapped(false);
        target.setHasSummoningSickness(false);
      }
      if (text.contains("ready it")) target.setTapped(false);
    }
    if (text.contains("draw 1")) autoDraw(state, card.getOwnerId());
  }

  private void autoDraw(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(p -> p.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getDeckPool().isEmpty()) {
      log(state, playerId, player.getName() + "'s deck is empty \u2014 no draw.");
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
