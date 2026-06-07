package com.riftforge.engine;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.*;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Component;

@Component
public class RulesValidator {
  private final CardDataService cardDataService;

  public RulesValidator(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public void validate(LiveGameState state, MoveRequest move) {
    if (move instanceof PassPhaseMove m) { validatePassPhase(state, m); return; }
    if (move instanceof AdjustScoreMove) return;
    if (move instanceof DeclareBlockMove m) { validateBlock(state, m); return; }
    if (move instanceof DealCardMove) return;
    if (move instanceof TapCardMove m) { validateTapCard(state, m); return; }
    if (move instanceof FlipCardMove m) { validateFlipCard(state, m); return; }
    if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
    if (move instanceof PlayCardMove m) validatePlayCard(state, m);
    if (move instanceof MoveCardMove m) validateMoveCard(state, m);
    if (move instanceof TapRuneMove m) validateTapRune(state, m);
    if (move instanceof DiscardRuneMove m) validateDiscardRune(state, m);
    if (move instanceof DeclareAttackMove m) validateAttack(state, m);
  }

  private void validatePassPhase(LiveGameState state, PassPhaseMove move) {
    boolean isPlayer = state.getPlayers().stream().anyMatch(p -> p.getUserId().equals(move.playerId()));
    if (!isPlayer) throw new IllegalMoveException("Player is not in this game.");
    if (state.getCurrentPhase() == Phase.BLOCK_DECLARE) {
      if (move.playerId().equals(state.getActivePlayerId())) {
        throw new IllegalMoveException("Waiting for the defending player.");
      }
      return;
    }
    if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
  }

  private void validatePlayCard(LiveGameState state, PlayCardMove move) {
    if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Cards can only be played in MAIN.");
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.HAND) throw new IllegalMoveException("Card is not in your hand.");
    boolean isRune = "Rune".equalsIgnoreCase(cardDataService.getCard(card.getCardId()).type());
    if (move.targetZone() == ZoneName.RUNE && !isRune) throw new IllegalMoveException("Only Rune cards can be placed in the rune zone.");
    if (move.targetZone() == ZoneName.BASE && isRune) throw new IllegalMoveException("Rune cards must be placed in the rune zone.");
    if (move.targetZone() != ZoneName.BASE && move.targetZone() != ZoneName.RUNE) throw new IllegalMoveException("Cards must be played to base.");
    int cost = cardDataService.getCard(card.getCardId()).cost();
    int energy = state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().orElseThrow().getAvailableEnergy();
    if (energy < cost) throw new IllegalMoveException("Insufficient energy.");
    boolean isSpell = "Spell".equalsIgnoreCase(cardDataService.getCard(card.getCardId()).type());
    boolean isGear = "Gear".equalsIgnoreCase(cardDataService.getCard(card.getCardId()).type());
    if ((isSpell || isGear) && cardDataService.isUnsupportedAction(card.getCardId())) {
      throw new IllegalMoveException("That card's effect is not supported yet.");
    }
    if ((isSpell || isGear) && cardDataService.requiresBattlefieldTarget(card.getCardId())) {
      if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
        throw new IllegalMoveException("This card requires a target.");
      }
      CardInstance target = state.getCards().stream()
          .filter(candidate -> candidate.getInstanceId().equals(move.targetInstanceId()))
          .findFirst()
          .orElseThrow(() -> new IllegalMoveException("Target not found."));
      if (target.getZone() != ZoneName.BATTLEFIELD) throw new IllegalMoveException("Target must be on the battlefield.");
      if (cardDataService.requiresFriendlyTarget(card.getCardId()) && !target.getOwnerId().equals(move.playerId())) {
        throw new IllegalMoveException("That card requires a friendly unit.");
      }
      if (cardDataService.requiresEnemyTarget(card.getCardId()) && target.getOwnerId().equals(move.playerId())) {
        throw new IllegalMoveException("That card requires an enemy unit.");
      }
    }
  }

  private void validateMoveCard(LiveGameState state, MoveCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    if (card.getZone() == ZoneName.BASE && move.targetZone() == ZoneName.BATTLEFIELD) {
      if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
      if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Cards can only deploy during MAIN.");
      if (card.isTapped()) throw new IllegalMoveException("Only ready cards can move to the battlefield.");
    }
    if (card.getZone() == ZoneName.CHAMPION && move.targetZone() == ZoneName.BATTLEFIELD) {
      if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
      if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Champions can only deploy during MAIN.");
      if (card.isTapped()) throw new IllegalMoveException("Champion is exhausted.");
    }
    if (card.getZone() == ZoneName.LEGEND && move.targetZone() == ZoneName.BATTLEFIELD) {
      if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
      if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Legends can only deploy during MAIN.");
      if (card.isTapped()) throw new IllegalMoveException("Legend is exhausted.");
    }
  }

  private void validateTapCard(LiveGameState state, TapCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
  }

  private void validateFlipCard(LiveGameState state, FlipCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
  }

  private void validateTapRune(LiveGameState state, TapRuneMove move) {
    if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Runes can only be tapped in MAIN.");
    RuneState rune = findRune(state, move.runeInstanceId());
    if (!move.playerId().equals(rune.getOwnerId())) throw new IllegalMoveException("You do not own that rune.");
    if (rune.isTapped()) throw new IllegalMoveException("Rune is already tapped.");
  }

  private void validateDiscardRune(LiveGameState state, DiscardRuneMove move) {
    if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Runes can only be discarded in MAIN.");
    RuneState rune = findRune(state, move.runeInstanceId());
    if (!move.playerId().equals(rune.getOwnerId())) throw new IllegalMoveException("You do not own that rune.");
  }

  private void validateAttack(LiveGameState state, DeclareAttackMove move) {
    if (state.getCurrentPhase() != Phase.ATTACK_DECLARE) throw new IllegalMoveException("Not attack declaration phase.");
    boolean validTarget = state.getPlayers().stream()
        .anyMatch(player -> player.getUserId().equals(move.targetPlayerId())
            && !player.getUserId().equals(move.playerId()));
    if (!validTarget) throw new IllegalMoveException("Invalid attack target.");
    for (String id : move.attackerInstanceIds()) {
      CardInstance card = findCard(state, id);
      if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.BATTLEFIELD) throw new IllegalMoveException("Invalid attacker.");
      if (card.isTapped()) throw new IllegalMoveException("Attacker is exhausted.");
      if (card.isHasSummoningSickness() && !cardDataService.hasKeyword(card, "RUSH")) throw new IllegalMoveException("Attacker has summoning sickness.");
    }
  }

  private void validateBlock(LiveGameState state, DeclareBlockMove move) {
    if (state.getCurrentPhase() != Phase.BLOCK_DECLARE) throw new IllegalMoveException("Not block declaration phase.");
    long distinctAttackers = move.blockerToAttacker().values().stream().distinct().count();
    if (distinctAttackers < move.blockerToAttacker().size()) throw new IllegalMoveException("Cannot assign multiple blockers to one attacker.");
    for (String id : move.blockerToAttacker().keySet()) {
      CardInstance card = findCard(state, id);
      if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.BATTLEFIELD || card.isTapped()) throw new IllegalMoveException("Invalid blocker.");
      if (!state.getDeclaredAttackers().contains(move.blockerToAttacker().get(id))) throw new IllegalMoveException("Cannot block a non-attacking unit.");
      CardInstance attacker = findCard(state, move.blockerToAttacker().get(id));
      if (cardDataService.hasKeyword(attacker, "ELUSIVE")) throw new IllegalMoveException("Cannot block an ELUSIVE attacker.");
    }
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow(() -> new IllegalMoveException("Card not found."));
  }

  private RuneState findRune(LiveGameState state, String id) {
    return state.getRunes().stream().filter(r -> r.getInstanceId().equals(id)).findFirst().orElseThrow(() -> new IllegalMoveException("Rune not found."));
  }
}
