package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
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
    if (move instanceof DismissRevealedMove) return;
    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      if (move instanceof MulliganMove mulligan) {
        validateMulligan(state, mulligan);
        return;
      }
      throw new IllegalMoveException("Complete your mulligan before making other moves.");
    }
    if (move instanceof MulliganMove) throw new IllegalMoveException("Mulligans are already complete.");
    if (move instanceof AdjustScoreMove || move instanceof DealCardMove) return;
    if (move instanceof TapCardMove tap) { validateOwnedCard(state, tap.playerId(), tap.instanceId()); return; }
    if (move instanceof FlipCardMove flip) { validateOwnedCard(state, flip.playerId(), flip.instanceId()); return; }
    if (!move.playerId().equals(state.getActivePlayerId())) {
      if (move instanceof PlayCardMove play && state.getCurrentPhase() == Phase.MAIN) {
        CardInstance card = findCard(state, play.instanceId());
        CardDefinition def = cardDataService.getCard(card.getCardId());
        if ("Spell".equalsIgnoreCase(def.type())) {
          validatePlayCard(state, play);
          return;
        }
      }
      throw new IllegalMoveException("Not your turn.");
    }
    if (move instanceof VisionChoiceMove vision) { validateVisionChoice(state, vision); return; }
    if (move instanceof PassPhaseMove) return;
    if (move instanceof UndoRunesMove undo) { validateUndoRunes(state, undo); return; }
    if (move instanceof PlayCardMove play) { validatePlayCard(state, play); return; }
    if (move instanceof MoveToBattlefieldMove deploy) { validateMoveToBattlefield(state, deploy); return; }
    if (move instanceof MoveCardMove moveCard) { validateMoveCard(state, moveCard); return; }
    if (move instanceof TapRuneMove tapRune) { validateTapRune(state, tapRune); return; }
    if (move instanceof DiscardRuneMove discardRune) validateDiscardRune(state, discardRune);
  }

  private void validateMulligan(LiveGameState state, MulliganMove move) {
    if (state.getPlayers().stream().noneMatch(player -> player.getUserId().equals(move.playerId()))) {
      throw new IllegalMoveException("Player is not in this game.");
    }
    if (state.getMulligansDone().contains(move.playerId())) throw new IllegalMoveException("You already completed your mulligan.");
    if (move.discardInstanceIds() == null) throw new IllegalMoveException("Mulligan selection is required.");
    if (move.discardInstanceIds().size() > 2) throw new IllegalMoveException("You may mulligan up to 2 cards.");
    if (move.discardInstanceIds().stream().distinct().count() != move.discardInstanceIds().size()) {
      throw new IllegalMoveException("Mulligan selection contains duplicate cards.");
    }
    for (String instanceId : move.discardInstanceIds()) {
      CardInstance card = findCard(state, instanceId);
      if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.HAND) {
        throw new IllegalMoveException("You can only mulligan cards from your opening hand.");
      }
    }
  }

  private void validatePlayCard(LiveGameState state, PlayCardMove move) {
    requireMain(state);
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.HAND) {
      throw new IllegalMoveException("Card is not in your hand.");
    }
    CardDefinition def = cardDataService.getCard(card.getCardId());
    boolean rune = "Rune".equalsIgnoreCase(def.type());
    if (move.targetZone() == ZoneName.RUNE && !rune) throw new IllegalMoveException("Only Rune cards can be placed in the rune zone.");
    if (move.targetZone() == ZoneName.BASE && rune) throw new IllegalMoveException("Rune cards must be placed in the rune zone.");
    if (move.targetZone() != ZoneName.BASE && move.targetZone() != ZoneName.RUNE) throw new IllegalMoveException("Cards must be played to base.");
    if (move.accelerate() && (!"Unit".equalsIgnoreCase(def.type()) || !cardDataService.hasKeyword(card, "ACCELERATE"))) {
      throw new IllegalMoveException("That card does not have ACCELERATE.");
    }
    int cost = def.cost() + (move.accelerate() ? 1 : 0);
    int energy = state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().orElseThrow().getAvailableEnergy();
    if (energy < cost) throw new IllegalMoveException("Insufficient energy.");
    boolean spellOrGear = "Spell".equalsIgnoreCase(def.type()) || "Gear".equalsIgnoreCase(def.type());
    if (spellOrGear && cardDataService.isUnsupportedAction(card.getCardId())) throw new IllegalMoveException("That card's effect is not supported yet.");
    if (spellOrGear && cardDataService.requiresBattlefieldTarget(card.getCardId())) validateTarget(state, move, card);
  }

  private void validateVisionChoice(LiveGameState state, VisionChoiceMove move) {
    int lastPeek = -1;
    int lastResolution = -1;
    for (int i = 0; i < state.getLog().size(); i++) {
      LiveGameState.LogEntry entry = state.getLog().get(i);
      if (!move.playerId().equals(entry.userId())) continue;
      if (entry.text().startsWith("VISION_PEEK|")) lastPeek = i;
      if (entry.text().startsWith("VISION_RESOLVED|")) lastResolution = i;
    }
    if (lastPeek <= lastResolution) throw new IllegalMoveException("No Vision choice is pending.");
  }

  private void validateTarget(LiveGameState state, PlayCardMove move, CardInstance card) {
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) throw new IllegalMoveException("This card requires a target.");
    CardInstance target = findCard(state, move.targetInstanceId());
    if (target.getZone() == ZoneName.BASE
        && cardDataService.hasKeyword(target, "HIDDEN")
        && !target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("Cannot target a unit with Hidden while it's at their base.");
    }
    if (target.getZone() != ZoneName.BATTLEFIELD) throw new IllegalMoveException("Target must be on the battlefield.");
    if (cardDataService.requiresFriendlyTarget(card.getCardId()) && !target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("That card requires a friendly unit.");
    }
    if (cardDataService.requiresEnemyTarget(card.getCardId()) && target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("That card requires an enemy unit.");
    }
  }

  private void validateMoveToBattlefield(LiveGameState state, MoveToBattlefieldMove move) {
    requireMain(state);
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    if (card.getZone() != ZoneName.BASE && card.getZone() != ZoneName.CHAMPION && card.getZone() != ZoneName.LEGEND) {
      throw new IllegalMoveException("Only cards from your base can move to the battlefield.");
    }
    if (card.isTapped()) throw new IllegalMoveException("Only ready cards can move to the battlefield.");
  }

  private void validateMoveCard(LiveGameState state, MoveCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    if (move.targetZone() == ZoneName.BATTLEFIELD) throw new IllegalMoveException("Use Move to Battlefield during MAIN.");
  }

  private void validateUndoRunes(LiveGameState state, UndoRunesMove move) {
    requireMain(state);
    if (state.isCardPlayedThisTurn()) throw new IllegalMoveException("Cannot undo after playing a card.");
    if (state.getRunes().stream().noneMatch(rune -> rune.getOwnerId().equals(move.playerId()) && rune.isTapped())) {
      throw new IllegalMoveException("No tapped runes to undo.");
    }
  }

  private void validateTapRune(LiveGameState state, TapRuneMove move) {
    requireMain(state);
    RuneState rune = findRune(state, move.runeInstanceId());
    if (!move.playerId().equals(rune.getOwnerId())) throw new IllegalMoveException("You do not own that rune.");
    if (rune.isTapped()) throw new IllegalMoveException("Rune is already tapped.");
  }

  private void validateDiscardRune(LiveGameState state, DiscardRuneMove move) {
    requireMain(state);
    RuneState rune = findRune(state, move.runeInstanceId());
    if (!move.playerId().equals(rune.getOwnerId())) throw new IllegalMoveException("You do not own that rune.");
    if (rune.isTapped()) throw new IllegalMoveException("Cannot discard an already-tapped rune.");
  }

  private void validateOwnedCard(LiveGameState state, String playerId, String instanceId) {
    CardInstance card = findCard(state, instanceId);
    if (!playerId.equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
  }

  private void requireMain(LiveGameState state) {
    if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("That action can only be taken during MAIN.");
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalMoveException("Card not found."));
  }

  private RuneState findRune(LiveGameState state, String id) {
    return state.getRunes().stream().filter(r -> r.getInstanceId().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalMoveException("Rune not found."));
  }
}
