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
    if (move instanceof PassPhaseMove) return;
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
    if (isSpell && cardDataService.requiresBattlefieldTarget(card.getCardId())) {
      boolean hasTarget = state.getCards().stream().anyMatch(candidate ->
          candidate.getZone() == ZoneName.BATTLEFIELD
              && !candidate.getOwnerId().equals(move.playerId()));
      if (!hasTarget) throw new IllegalMoveException("No valid targets for that spell.");
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
    for (String id : move.attackerInstanceIds()) {
      CardInstance card = findCard(state, id);
      if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.BATTLEFIELD) throw new IllegalMoveException("Invalid attacker.");
      if (card.isHasSummoningSickness() && !cardDataService.hasKeyword(card.getCardId(), "RUSH")) throw new IllegalMoveException("Attacker has summoning sickness.");
    }
  }

  private void validateBlock(LiveGameState state, DeclareBlockMove move) {
    if (state.getCurrentPhase() != Phase.BLOCK_DECLARE) throw new IllegalMoveException("Not block declaration phase.");
    for (String id : move.blockerToAttacker().keySet()) {
      CardInstance card = findCard(state, id);
      if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.BATTLEFIELD || card.isTapped()) throw new IllegalMoveException("Invalid blocker.");
    }
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow(() -> new IllegalMoveException("Card not found."));
  }

  private RuneState findRune(LiveGameState state, String id) {
    return state.getRunes().stream().filter(r -> r.getInstanceId().equals(id)).findFirst().orElseThrow(() -> new IllegalMoveException("Rune not found."));
  }
}
