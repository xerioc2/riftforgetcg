package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.*;
import com.riftforge.service.CardDataService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    if (move instanceof AdjustScoreMove
        || move instanceof DealCardMove
        || move instanceof TapCardMove
        || move instanceof FlipCardMove
        || move instanceof MoveCardMove) {
      validateSandboxOnly(state, move);
      return;
    }
    if (state.getActiveShowdown() != null && !(move instanceof ResolveShowdownMove)) {
      throw new IllegalMoveException("Resolve the active showdown first.");
    }
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
    if (move instanceof ResolveShowdownMove resolve) { validateResolveShowdown(state, resolve); return; }
    if (move instanceof PassPhaseMove) return;
    if (move instanceof UndoRunesMove undo) { validateUndoRunes(state, undo); return; }
    if (move instanceof PlayCardMove play) { validatePlayCard(state, play); return; }
    if (move instanceof MoveToBattlefieldMove deploy) { validateMoveToBattlefield(state, deploy); return; }
    if (move instanceof RepositionCardMove reposition) { validateRepositionCard(state, reposition); return; }
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

  private void validateSandboxOnly(LiveGameState state, MoveRequest move) {
    if (state.getGameMode() == GameMode.SANDBOX) return;
    String moveName = "That action";
    if (move instanceof DealCardMove) moveName = "Deal Card";
    if (move instanceof AdjustScoreMove) moveName = "Adjust Score";
    if (move instanceof TapCardMove) moveName = "Tap Card";
    if (move instanceof FlipCardMove) moveName = "Flip Card";
    if (move instanceof MoveCardMove) moveName = "Move Card";
    throw new IllegalMoveException(moveName + " is only available in sandbox games.");
  }

  private void validatePlayCard(LiveGameState state, PlayCardMove move) {
    requireMain(state);
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.HAND) {
      throw new IllegalMoveException("Card is not in your hand.");
    }
    CardDefinition def = cardDataService.getCard(card.getCardId());
    boolean rune = isType(def, "Rune");
    boolean unit = isType(def, "Unit");
    boolean spell = isType(def, "Spell");
    boolean gear = isType(def, "Gear");
    if (isType(def, "Legend")) throw new IllegalMoveException("Legend cards cannot be played from hand.");
    if (isType(def, "Champion")) throw new IllegalMoveException("Champion cards cannot be played from hand.");
    if (isType(def, "Battlefield")) throw new IllegalMoveException("Battlefield cards cannot be played from hand.");
    if (rune && move.targetZone() != ZoneName.RUNE) throw new IllegalMoveException("Rune cards must be placed in the rune zone.");
    if (!rune && move.targetZone() == ZoneName.RUNE) throw new IllegalMoveException("Only Rune cards can be placed in the rune zone.");
    if ((unit || spell || gear) && move.targetZone() != ZoneName.BASE) throw new IllegalMoveException("Non-rune cards must be played to base.");
    if (!rune && !unit && !spell && !gear) throw new IllegalMoveException("That card type cannot be played from hand.");
    if (move.accelerate() && (!"Unit".equalsIgnoreCase(def.type()) || !cardDataService.hasKeyword(card, "ACCELERATE"))) {
      throw new IllegalMoveException("That card does not have ACCELERATE.");
    }
    validatePayment(state, move, def);
    int cost = def.cost() + (move.accelerate() ? 1 : 0);
    int energy = state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().orElseThrow().getAvailableEnergy();
    int selectedEnergy = move.paymentRuneIds().stream()
        .map(this::requirePaymentRuneId)
        .map(id -> findRune(state, id))
        .mapToInt(RuneState::getNormalEnergy)
        .sum();
    if (energy + selectedEnergy < cost) throw new IllegalMoveException("Insufficient energy.");
    boolean spellOrGear = spell || gear;
    if (spellOrGear && cardDataService.isUnsupportedAction(card.getCardId())) throw new IllegalMoveException("That card's effect is not supported yet.");
    if (spellOrGear && cardDataService.requiresBattlefieldTarget(card.getCardId())) validateTarget(state, move, card);
  }

  private void validatePayment(LiveGameState state, PlayCardMove move, CardDefinition def) {
    List<String> paymentRuneIds = move.paymentRuneIds();
    List<String> premiumRuneIds = move.premiumRuneIds();
    Set<String> allRuneIds = new HashSet<>();
    for (String runeId : paymentRuneIds) {
      validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
    }
    for (String runeId : premiumRuneIds) {
      RuneState rune = validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
      if (!runeMatchesCardDomain(rune, def)) throw new IllegalMoveException("Premium payment uses the wrong rune domain.");
    }
    int premiumCost = Math.max(0, def.premiumCost());
    if (premiumRuneIds.size() < premiumCost) {
      throw new IllegalMoveException("Insufficient premium payment.");
    }
    if (premiumRuneIds.size() > premiumCost) {
      throw new IllegalMoveException("Too many runes selected for premium payment.");
    }
  }

  private RuneState validatePaymentRune(LiveGameState state, String playerId, String runeId) {
    RuneState rune = findRune(state, runeId);
    if (!playerId.equals(rune.getOwnerId())) throw new IllegalMoveException("You cannot pay with an opponent's rune.");
    if (rune.isTapped()) throw new IllegalMoveException("Payment rune is already tapped.");
    return rune;
  }

  private String requirePaymentRuneId(String runeId) {
    if (runeId == null || runeId.isBlank()) throw new IllegalMoveException("Payment contains an invalid rune.");
    return runeId;
  }

  private boolean runeMatchesCardDomain(RuneState rune, CardDefinition card) {
    CardDefinition runeDef = cardDataService.getCard(rune.getCardId());
    if (runeDef == null || runeDef.domains() == null || runeDef.domains().isEmpty()) return false;
    if (card.domains() == null || card.domains().isEmpty()) return false;
    return runeDef.domains().stream()
        .filter(domain -> !"COLORLESS".equalsIgnoreCase(domain))
        .anyMatch(domain -> card.domains().stream().anyMatch(cardDomain -> cardDomain.equalsIgnoreCase(domain)));
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
    if (state.getActiveShowdown() != null) throw new IllegalMoveException("A showdown is already active.");
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    CardDefinition def = cardDataService.getCard(card.getCardId());
    boolean unit = isType(def, "Unit");
    boolean champion = isType(def, "Champion");
    if (!unit && !champion) {
      throw new IllegalMoveException("Only Units and Champions can move to the battlefield.");
    }
    if (unit && card.getZone() != ZoneName.BASE) {
      throw new IllegalMoveException("Only units from your base can move to the battlefield.");
    }
    if (champion && card.getZone() != ZoneName.CHAMPION && card.getZone() != ZoneName.BASE) {
      throw new IllegalMoveException("Only Champions from your champion zone or base can move to the battlefield.");
    }
    if (card.isTapped()) throw new IllegalMoveException("Only ready cards can move to the battlefield.");
  }

  private void validateRepositionCard(LiveGameState state, RepositionCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    if (!isPublicRepositionZone(card.getZone())) {
      throw new IllegalMoveException("That card cannot be repositioned.");
    }
  }

  private boolean isPublicRepositionZone(ZoneName zone) {
    return zone == ZoneName.BASE
        || zone == ZoneName.BATTLEFIELD
        || zone == ZoneName.CHAMPION
        || zone == ZoneName.LEGEND
        || zone == ZoneName.DISCARD;
  }

  private boolean isType(CardDefinition def, String type) {
    return def != null && type.equalsIgnoreCase(def.type());
  }

  private void validateResolveShowdown(LiveGameState state, ResolveShowdownMove move) {
    requireMain(state);
    if (state.getActiveShowdown() == null) throw new IllegalMoveException("No showdown is active.");
    if (!move.playerId().equals(state.getActiveShowdown().attackingPlayerId())) {
      throw new IllegalMoveException("Only the attacking player can resolve this showdown.");
    }
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
