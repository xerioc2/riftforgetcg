package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.*;
import com.riftforge.rules.BattlefieldLocationRules;
import com.riftforge.rules.EquipmentRules;
import com.riftforge.rules.EquipmentRules.EquipCost;
import com.riftforge.rules.ShowdownParticipantRules;
import com.riftforge.service.CardDataService;
import com.riftforge.engine.CombatStatsService.CombatContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RulesValidator {
  private final CardDataService cardDataService;
  private final ShowdownParticipantRules showdownParticipantRules;
  private final CombatStatsService combatStatsService;
  private final CombatDamageRules combatDamageRules;
  private final ActivatedAbilityService activatedAbilityService;

  public RulesValidator(CardDataService cardDataService) {
    this(cardDataService, new ShowdownParticipantRules(), new CombatStatsService(cardDataService), new CombatDamageRules(cardDataService), new ActivatedAbilityService(cardDataService));
  }

  public RulesValidator(CardDataService cardDataService, ShowdownParticipantRules showdownParticipantRules) {
    this(cardDataService, showdownParticipantRules, new CombatStatsService(cardDataService), new CombatDamageRules(cardDataService), new ActivatedAbilityService(cardDataService));
  }

  @Autowired
  public RulesValidator(CardDataService cardDataService, ShowdownParticipantRules showdownParticipantRules, CombatStatsService combatStatsService, CombatDamageRules combatDamageRules, ActivatedAbilityService activatedAbilityService) {
    this.cardDataService = cardDataService;
    this.showdownParticipantRules = showdownParticipantRules;
    this.combatStatsService = combatStatsService;
    this.combatDamageRules = combatDamageRules;
    this.activatedAbilityService = activatedAbilityService;
  }

  public void validate(LiveGameState state, MoveRequest move) {
    if (move instanceof DismissRevealedMove) return;
    if (move instanceof ResolveChoiceMove choice) {
      validateResolveChoice(state, choice);
      return;
    }
    if (state.getPendingChoice() != null) {
      throw new IllegalMoveException("Resolve the pending choice before taking another action.");
    }
    if (move instanceof PassChainFocusMove pass) {
      validatePassChainFocus(state, pass);
      return;
    }
    if (move instanceof ResolveChainTopMove resolve) {
      validateResolveChainTop(state, resolve);
      return;
    }
    if (state.getChainState() != null) {
      if (move instanceof PlayCardMove play) {
        validatePlayCard(state, play);
        return;
      }
      throw new IllegalMoveException("Resolve the chain before taking another action.");
    }
    if (state.getCurrentPhase() == Phase.SELECT_BATTLEFIELD) {
      if (move instanceof SelectBattlefieldMove select) {
        validateSelectBattlefield(state, select);
        return;
      }
      throw new IllegalMoveException("Choose a Battlefield before mulligan.");
    }
    if (move instanceof SelectBattlefieldMove) throw new IllegalMoveException("Battlefields are already selected.");
    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      if (move instanceof MulliganMove mulligan) {
        validateMulligan(state, mulligan);
        return;
      }
      throw new IllegalMoveException("Complete your mulligan before making other moves.");
    }
    if (move instanceof MulliganMove) throw new IllegalMoveException("Mulligans are already complete.");
    if (state.getActiveShowdown() != null) {
      if (move instanceof AssignCombatDamageMove assign) { validateAssignCombatDamage(state, assign); return; }
      if (move instanceof PassShowdownFocusMove pass) { validatePassShowdownFocus(state, pass); return; }
      if (move instanceof ResolveShowdownMove resolve) { validateResolveShowdown(state, resolve); return; }
      if (move instanceof PlayCardMove play) { validatePlayCard(state, play); return; }
      throw new IllegalMoveException("Resolve the active showdown first.");
    }
    if (move instanceof AdjustScoreMove
        || move instanceof DealCardMove
        || move instanceof TapCardMove
        || move instanceof FlipCardMove
        || move instanceof MoveCardMove) {
      validateSandboxOnly(state, move);
      return;
    }
    if (!move.playerId().equals(state.getActivePlayerId())) {
      if (move instanceof PlayCardMove play && isReactionPlayFromHand(state, play)) {
        throw new IllegalMoveException("No reaction window is active.");
      }
      throw new IllegalMoveException("Not your turn.");
    }
    if (move instanceof VisionChoiceMove vision) { validateVisionChoice(state, vision); return; }
    if (move instanceof ResolveShowdownMove resolve) { validateResolveShowdown(state, resolve); return; }
    if (move instanceof PassPhaseMove) return;
    if (move instanceof UndoRunesMove undo) { validateUndoRunes(state, undo); return; }
    if (move instanceof HideCardMove hide) { validateHideCard(state, hide); return; }
    if (move instanceof EquipGearMove equip) { validateEquipGear(state, equip); return; }
    if (move instanceof ActivateAbilityMove activate) { validateActivateAbility(state, activate); return; }
    if (move instanceof PlayCardMove play) { validatePlayCard(state, play); return; }
    if (move instanceof MoveToBattlefieldMove deploy) { validateMoveToBattlefield(state, deploy); return; }
    if (move instanceof RepositionCardMove reposition) { validateRepositionCard(state, reposition); return; }
    if (move instanceof TapRuneMove tapRune) { validateTapRune(state, tapRune); return; }
    if (move instanceof DiscardRuneMove discardRune) validateDiscardRune(state, discardRune);
  }

  private void validateResolveChoice(LiveGameState state, ResolveChoiceMove move) {
    PendingChoice choice = state.getPendingChoice();
    if (choice == null) throw new IllegalMoveException("No choice is pending.");
    if (!choice.getPlayerId().equals(move.playerId())) throw new IllegalMoveException("That choice belongs to another player.");
    if (!choice.getChoiceId().equals(move.choiceId())) throw new IllegalMoveException("That choice is no longer pending.");
    if (PendingChoice.TYPE_TOP_DECK_PICK_ONE.equals(choice.getType())) {
      boolean validCard = choice.getCardOptions().stream()
          .anyMatch(option -> option.optionId().equals(move.selectedCardOptionId()));
      if (!validCard) throw new IllegalMoveException("Choose one of the revealed cards.");
      return;
    }
    if (PendingChoice.TYPE_PREDICT_ORDER.equals(choice.getType())) {
      validatePredictChoice(choice, move);
      return;
    }
    if (PendingChoice.TYPE_TARGET_GEAR.equals(choice.getType())) {
      if (PendingChoice.OPTION_DECLINE.equals(move.selectedOptionId())) return;
      validateDestroyGearChoiceTarget(state, move);
      return;
    }
    boolean validOption = choice.getOptions().stream()
        .anyMatch(option -> option.id().equals(move.selectedOptionId()));
    if (!validOption) throw new IllegalMoveException("Invalid choice option.");
    if ((PendingChoice.TYPE_OPTIONAL_PAY_1_DRAW_ONE.equals(choice.getType())
        || PendingChoice.TYPE_OPTIONAL_PAYMENT.equals(choice.getType()))
        && PendingChoice.OPTION_PAY_1.equals(move.selectedOptionId())
        && playerEnergy(state, move.playerId()) < optionalPaymentAmount(choice)) {
      throw new IllegalMoveException("Insufficient energy for that choice.");
    }
  }

  private int optionalPaymentAmount(PendingChoice choice) {
    return choice.getPaymentAmount() > 0 ? choice.getPaymentAmount() : 1;
  }

  private void validateDestroyGearChoiceTarget(LiveGameState state, ResolveChoiceMove move) {
    if (move.selectedTargetInstanceId() == null || move.selectedTargetInstanceId().isBlank()) {
      throw new IllegalMoveException("Choose a Gear to destroy.");
    }
    CardInstance target = findCard(state, move.selectedTargetInstanceId());
    if (!isLegalGearDestroyTarget(target)) throw new IllegalMoveException("Choose a public Gear in play.");
  }

  private void validatePredictChoice(PendingChoice choice, ResolveChoiceMove move) {
    if (move.assignments().size() != choice.getCardOptions().size()) {
      throw new IllegalMoveException("Choose top or bottom for each revealed card.");
    }
    Set<String> optionIds = new HashSet<>();
    choice.getCardOptions().forEach(option -> optionIds.add(option.optionId()));
    Set<String> assignedIds = new HashSet<>();
    for (PendingChoice.CardChoiceAssignment assignment : move.assignments()) {
      if (!optionIds.contains(assignment.optionId())) {
        throw new IllegalMoveException("That card is not part of this choice.");
      }
      if (!assignedIds.add(assignment.optionId())) {
        throw new IllegalMoveException("Each revealed card can only be assigned once.");
      }
      if (!PendingChoice.ACTION_TOP.equals(assignment.action())
          && !PendingChoice.ACTION_BOTTOM.equals(assignment.action())) {
        throw new IllegalMoveException("Choose top or bottom for each revealed card.");
      }
    }
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

  private void validateSelectBattlefield(LiveGameState state, SelectBattlefieldMove move) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(move.playerId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("Player is not in this game."));
    if (player.getSelectedBattlefieldId() != null && !player.getSelectedBattlefieldId().isBlank()) {
      throw new IllegalMoveException("You already selected a Battlefield.");
    }
    if (move.battlefieldCardId() == null || move.battlefieldCardId().isBlank()) {
      throw new IllegalMoveException("Battlefield selection is required.");
    }
    if (!player.getSelectedBattlefields().contains(move.battlefieldCardId())) {
      throw new IllegalMoveException("Choose one of your Battlefields.");
    }
    CardDefinition def = cardDataService.getCard(move.battlefieldCardId());
    if (!isType(def, "Battlefield")) throw new IllegalMoveException("Selected card must be a Battlefield.");
  }

  private void validatePlayCard(LiveGameState state, PlayCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId()) || card.getZone() != ZoneName.HAND) {
      throw new IllegalMoveException("Card is not in your hand.");
    }
    CardDefinition def = cardDataService.getCard(card.getCardId());
    validatePlayTiming(state, move, def);
    boolean rune = isType(def, "Rune");
    boolean unit = isType(def, "Unit");
    boolean champion = isType(def, "Champion");
    boolean spell = isType(def, "Spell");
    boolean gear = isType(def, "Gear");
    boolean ambushToBattlefield = move.targetZone() == ZoneName.BATTLEFIELD;
    if (isType(def, "Legend")) throw new IllegalMoveException("Legend cards cannot be played from hand.");
    if (isType(def, "Battlefield")) throw new IllegalMoveException("Battlefield cards cannot be played from hand.");
    if (cardDataService.hasUnsupportedAdditionalCost(def)) throw new IllegalMoveException("That card's additional cost is not supported yet.");
    if (rune && move.targetZone() != ZoneName.RUNE) throw new IllegalMoveException("Rune cards must be placed in the rune zone.");
    if (!rune && move.targetZone() == ZoneName.RUNE) throw new IllegalMoveException("Only Rune cards can be placed in the rune zone.");
    if (ambushToBattlefield) validateAmbushBattlefieldPlay(state, move, def, unit);
    else if ((unit || champion || spell || gear) && move.targetZone() != ZoneName.BASE) throw new IllegalMoveException("Non-rune cards must be played to base.");
    if (!rune && !unit && !champion && !spell && !gear) throw new IllegalMoveException("That card type cannot be played from hand.");
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
    boolean hasExplicitTarget = move.targetInstanceId() != null && !move.targetInstanceId().isBlank();
    boolean hasStructuredTargets = !move.targets().isEmpty();
    if (gear && hasExplicitTarget) {
      throw new IllegalMoveException("Play Equipment to Base first, then equip it from Base.");
    }
    if (cardDataService.requiresFriendlyAndEnemyTargets(card.getCardId())) {
      validateFriendlyAndEnemyTargets(state, move);
      return;
    }
    if (spell && cardDataService.isGustReaction(def)) {
      validateGustTarget(state, move);
      return;
    }
    if (spell && cardDataService.isDisciplineReaction(def)) {
      validatePublicUnitTarget(state, move, "Discipline requires a public Unit or Champion target.");
      return;
    }
    if (spell && cardDataService.isEnGardeReaction(def)) {
      validateFriendlyPublicUnitTarget(state, move, "En Garde requires a friendly public Unit or Champion target.");
      return;
    }
    if (spell && cardDataService.isDefiantDanceReaction(def)) {
      validateDefiantDanceTargets(state, move);
      return;
    }
    if (spell && cardDataService.isFlashReaction(def)) {
      validateFlashTargets(state, move);
      return;
    }
    if (spell && cardDataService.isCharmMoveEffect(def)) {
      validateCharmTarget(state, move);
      return;
    }
    if (hasStructuredTargets) throw new IllegalMoveException("That card does not use multiple targets.");
    if (spell && cardDataService.isDefyCounterReaction(def)) {
      validateDefyCounterTarget(state, move);
      return;
    }
    if (spell && cardDataService.isNotSoFastCounterReaction(def)) {
      validateNotSoFastCounterTarget(state, move);
      return;
    }
    if (spell && cardDataService.isAbandonCounterReaction(def)) {
      validateAbandonCounterTarget(state, move);
      return;
    }
    if (spell && (cardDataService.requiresBattlefieldTarget(card.getCardId()) || hasExplicitTarget)) {
      validateTarget(state, move, card);
    }
  }

  private void validatePlayTiming(LiveGameState state, PlayCardMove move, CardDefinition def) {
    if (state.getCurrentPhase() != Phase.MAIN && move.targetZone() == ZoneName.BATTLEFIELD && cardDataService.isAmbushCard(def)) {
      throw new IllegalMoveException("Ambush reaction timing is not implemented yet.");
    }
    requireMain(state);
    if (state.getChainState() != null) {
      if (!cardDataService.isReactionCard(def)) {
        throw new IllegalMoveException("Only supported Reaction cards can be played while the chain is active.");
      }
      if (!cardDataService.isGustReaction(def)
          && !cardDataService.isDisciplineReaction(def)
          && !cardDataService.isEnGardeReaction(def)
          && !cardDataService.isDefiantDanceReaction(def)
          && !cardDataService.isFlashReaction(def)
          && !cardDataService.isDefyCounterReaction(def)
          && !cardDataService.isNotSoFastCounterReaction(def)
          && !cardDataService.isAbandonCounterReaction(def)) {
        throw new IllegalMoveException("That Reaction is not supported yet.");
      }
      if (state.getChainState().readyToResolveTop()) {
        throw new IllegalMoveException("Resolve the chain item before playing more Reactions.");
      }
      if (!move.playerId().equals(state.getChainState().focusedPlayerId())) {
        throw new IllegalMoveException("Wait for your chain focus.");
      }
      return;
    }
    if (state.getActiveShowdown() == null) {
      if (cardDataService.isReactionCard(def)) {
        if (!move.playerId().equals(state.getActivePlayerId())) {
          throw new IllegalMoveException("No reaction window is active.");
        }
        if (!isSupportedReaction(def)) {
          throw new IllegalMoveException("That Reaction's effect is not supported yet.");
        }
        if (!isSupportedTargetedReaction(def)) {
          throw new IllegalMoveException("No reaction window is active.");
        }
        return;
      }
      if (!move.playerId().equals(state.getActivePlayerId())) throw new IllegalMoveException("Not your turn.");
      return;
    }
    if (!showdownParticipantRules.isShowdownParticipant(state, move.playerId())) {
      throw new IllegalMoveException("Only showdown participants can play Action cards here.");
    }
    if (cardDataService.isReactionCard(def)) {
      if (!isSupportedReaction(def)) {
        throw new IllegalMoveException("That Reaction's effect is not supported yet.");
      }
      if (!isSupportedTargetedReaction(def)) {
        throw new IllegalMoveException("No reaction window is active.");
      }
      if (!showdownParticipantRules.isFocusedPlayer(state, move.playerId())) {
        throw new IllegalMoveException("Wait for your showdown focus.");
      }
      if (state.getActiveShowdown().readyToResolve()) {
        throw new IllegalMoveException("Resolve the showdown before playing more Reactions.");
      }
      return;
    }
    if (!cardDataService.isActionCard(def)) {
      throw new IllegalMoveException("Only supported Action cards can be played during this showdown window.");
    }
    if (!showdownParticipantRules.isFocusedPlayer(state, move.playerId())) {
      throw new IllegalMoveException("Wait for your showdown focus.");
    }
    if (state.getActiveShowdown().readyToResolve()) {
      throw new IllegalMoveException("Resolve the showdown before playing more Actions.");
    }
  }

  private boolean isSupportedReaction(CardDefinition def) {
    return isSupportedTargetedReaction(def)
        || cardDataService.isDefyCounterReaction(def)
        || cardDataService.isNotSoFastCounterReaction(def)
        || cardDataService.isAbandonCounterReaction(def);
  }

  private boolean isSupportedTargetedReaction(CardDefinition def) {
    return cardDataService.isGustReaction(def)
        || cardDataService.isDisciplineReaction(def)
        || cardDataService.isEnGardeReaction(def)
        || cardDataService.isDefiantDanceReaction(def)
        || cardDataService.isFlashReaction(def);
  }

  private boolean isReactionPlayFromHand(LiveGameState state, PlayCardMove move) {
    return state.getCards().stream()
        .filter(card -> move.instanceId().equals(card.getInstanceId()))
        .filter(card -> move.playerId().equals(card.getOwnerId()))
        .filter(card -> card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null && cardDataService.isReactionCard(def));
  }

  private void validateDefyCounterTarget(LiveGameState state, PlayCardMove move) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) throw new IllegalMoveException("No chain item can be countered.");
    String targetChainItemId = move.targetChainItemId();
    if (targetChainItemId == null || targetChainItemId.isBlank()) {
      throw new IllegalMoveException("Choose a chain item to counter.");
    }
    LiveGameState.ChainItem target = chain.chainItems().stream()
        .filter(item -> targetChainItemId.equals(item.itemId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("That chain item is no longer available."));
    if (!target.isPending()) throw new IllegalMoveException("Only pending chain items can be countered.");
    if (!target.counterable() || !target.targetableOnChain()) {
      throw new IllegalMoveException("That chain item cannot be countered.");
    }
    if (!target.isPubliclyVisible() && !move.playerId().equals(target.controllerPlayerId())) {
      throw new IllegalMoveException("That chain item cannot be targeted.");
    }
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(target.chainItemType())) {
      throw new IllegalMoveException("Defy can only counter a spell.");
    }
    CardDefinition targetDef = target.sourceCardId() == null || target.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(target.sourceCardId());
    if (targetDef == null || !"Spell".equalsIgnoreCase(targetDef.type())) {
      throw new IllegalMoveException("Defy can only counter a spell.");
    }
    if (Math.max(0, targetDef.cost()) > 4 || Math.max(0, targetDef.premiumCost()) > 1) {
      throw new IllegalMoveException("Defy can only counter a spell that costs no more than 4 and no more than 1 power.");
    }
  }

  private void validateNotSoFastCounterTarget(LiveGameState state, PlayCardMove move) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) throw new IllegalMoveException("No chain item can be countered.");
    String targetChainItemId = move.targetChainItemId();
    if (targetChainItemId == null || targetChainItemId.isBlank()) {
      throw new IllegalMoveException("Choose a chain item to counter.");
    }
    LiveGameState.ChainItem target = chain.chainItems().stream()
        .filter(item -> targetChainItemId.equals(item.itemId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("That chain item is no longer available."));
    if (!isLegalNotSoFastTarget(target, move.playerId())) {
      throw new IllegalMoveException("Not So Fast can only counter an enemy spell that chooses a friendly Unit or Gear.");
    }
  }

  private void validateAbandonCounterTarget(LiveGameState state, PlayCardMove move) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) throw new IllegalMoveException("No chain item can be countered.");
    String targetChainItemId = move.targetChainItemId();
    if (targetChainItemId == null || targetChainItemId.isBlank()) {
      throw new IllegalMoveException("Choose a chain item to counter.");
    }
    LiveGameState.ChainItem target = chain.chainItems().stream()
        .filter(item -> targetChainItemId.equals(item.itemId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("That chain item is no longer available."));
    if (!target.isPending()) throw new IllegalMoveException("Only pending chain items can be countered.");
    if (!target.counterable() || !target.targetableOnChain() || !target.isPubliclyVisible()) {
      throw new IllegalMoveException("Abandon can only counter a public pending spell.");
    }
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(target.chainItemType())) {
      throw new IllegalMoveException("Abandon can only counter a spell.");
    }
    CardDefinition targetDef = target.sourceCardId() == null || target.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(target.sourceCardId());
    if (targetDef == null || !"Spell".equalsIgnoreCase(targetDef.type())) {
      throw new IllegalMoveException("Abandon can only counter a spell.");
    }
  }

  private boolean isLegalNotSoFastTarget(LiveGameState.ChainItem target, String playerId) {
    if (target == null || !target.isPending() || !target.counterable() || !target.targetableOnChain()) return false;
    if (!target.isPubliclyVisible()) return false;
    if (playerId.equals(target.controllerPlayerId())) return false;
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(target.chainItemType())) return false;
    CardDefinition targetDef = target.sourceCardId() == null || target.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(target.sourceCardId());
    if (targetDef == null || !"Spell".equalsIgnoreCase(targetDef.type())) return false;
    return target.chainTargets().stream().anyMatch(summary ->
        summary.publicSafe()
            && playerId.equals(summary.targetControllerPlayerId())
            && ("UNIT".equalsIgnoreCase(summary.targetKind())
                || "CHAMPION_UNIT".equalsIgnoreCase(summary.targetKind())
                || "GEAR".equalsIgnoreCase(summary.targetKind())));
  }

  private void validateAmbushBattlefieldPlay(LiveGameState state, PlayCardMove move, CardDefinition def, boolean unit) {
    if (!unit) throw new IllegalMoveException("Only Ambush units can be played directly to the battlefield.");
    if (!cardDataService.isAmbushCard(def)) throw new IllegalMoveException("Non-rune cards must be played to base.");
    boolean hasFriendlyBattlefieldUnit = state.getCards().stream()
        .filter(card -> move.playerId().equals(card.getOwnerId()))
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(card -> isType(card, "Unit") || isType(card, "Champion"));
    if (!hasFriendlyBattlefieldUnit) throw new IllegalMoveException("Ambush requires a friendly unit at that battlefield.");
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
    CardDefinition playedDef = cardDataService.getCard(card.getCardId());
    if (cardDataService.isEquip(playedDef)) {
      if (target.getZone() != ZoneName.BASE && target.getZone() != ZoneName.BATTLEFIELD) {
        throw new IllegalMoveException("Equip requires a friendly Unit or Champion in Base or at the battlefield.");
      }
      if (target.isFaceDown()) {
        throw new IllegalMoveException("Equip requires a friendly Unit or Champion in Base or at the battlefield.");
      }
      CardDefinition targetDef = cardDataService.getCard(target.getCardId());
      if (!isType(targetDef, "Unit") && !isType(targetDef, "Champion")) {
        throw new IllegalMoveException("Target must be a Unit or Champion.");
      }
      if (!target.getOwnerId().equals(move.playerId())) {
        throw new IllegalMoveException("Equip requires a friendly Unit or Champion in Base or at the battlefield.");
      }
      return;
    }
    if (target.getZone() == ZoneName.BASE
        && cardDataService.hasKeyword(target, "HIDDEN")
        && !target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("Cannot target a unit with Hidden while it's at their base.");
    }
    if (target.getZone() != ZoneName.BATTLEFIELD) throw new IllegalMoveException("Target must be on the battlefield.");
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    if (!isType(targetDef, "Unit") && !isType(targetDef, "Champion")) {
      throw new IllegalMoveException("Target must be a Unit or Champion.");
    }
    if (cardDataService.requiresFriendlyTarget(card.getCardId()) && !target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("That card requires a friendly unit.");
    }
    if (cardDataService.requiresEnemyTarget(card.getCardId()) && target.getOwnerId().equals(move.playerId())) {
      throw new IllegalMoveException("That card requires an enemy unit.");
    }
  }

  private void validateGustTarget(LiveGameState state, PlayCardMove move) {
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
      throw new IllegalMoveException("Gust requires a battlefield Unit or Champion with 3 Might or less.");
    }
    CardInstance target = findCard(state, move.targetInstanceId());
    if (!isLegalGustTarget(target)) {
      throw new IllegalMoveException("Gust can only target a battlefield Unit or Champion with 3 Might or less.");
    }
  }

  private boolean isLegalGustTarget(CardInstance target) {
    if (target.getZone() != ZoneName.BATTLEFIELD || target.isFaceDown()) return false;
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    if (!isType(targetDef, "Unit") && !isType(targetDef, "Champion")) return false;
    return combatStatsService.effectiveMight(target, CombatContext.IDLE) <= 3;
  }

  private void validatePublicUnitTarget(LiveGameState state, PlayCardMove move, String message) {
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
      throw new IllegalMoveException(message);
    }
    CardInstance target = findCard(state, move.targetInstanceId());
    if (!isPublicBattlefieldUnit(target)) throw new IllegalMoveException(message);
  }

  private void validateFriendlyPublicUnitTarget(LiveGameState state, PlayCardMove move, String message) {
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
      throw new IllegalMoveException(message);
    }
    CardInstance target = findCard(state, move.targetInstanceId());
    if (!isPublicBattlefieldUnit(target) || !move.playerId().equals(target.getOwnerId())) {
      throw new IllegalMoveException(message);
    }
  }

  private void validateDefiantDanceTargets(LiveGameState state, PlayCardMove move) {
    if (move.targets().size() != 2) {
      throw new IllegalMoveException("Defiant Dance requires one boosted unit and another weakened unit.");
    }
    CardInstance boostTarget = structuredTarget(state, move, PlayCardMove.TargetSelection.BOOST_UNIT, "Choose the unit to get +2 Might.");
    CardInstance weakenTarget = structuredTarget(state, move, PlayCardMove.TargetSelection.WEAKEN_UNIT, "Choose another unit to get -2 Might.");
    if (boostTarget.getInstanceId().equals(weakenTarget.getInstanceId())) {
      throw new IllegalMoveException("Defiant Dance targets must be different units.");
    }
    if (!isPublicBattlefieldUnit(boostTarget) || !isPublicBattlefieldUnit(weakenTarget)) {
      throw new IllegalMoveException("Defiant Dance requires public Unit or Champion targets at a battlefield.");
    }
  }

  private void validateFlashTargets(LiveGameState state, PlayCardMove move) {
    List<PlayCardMove.TargetSelection> targets = move.targets();
    if (targets.isEmpty() && move.targetInstanceId() != null && !move.targetInstanceId().isBlank()) {
      CardInstance target = findCard(state, move.targetInstanceId());
      if (!isFriendlyPublicBattlefieldUnit(target, move.playerId())) {
        throw new IllegalMoveException("Flash requires one or two friendly Unit or Champion targets at a battlefield.");
      }
      return;
    }
    if (targets.isEmpty() || targets.size() > 2) {
      throw new IllegalMoveException("Flash requires one or two friendly Unit or Champion targets at a battlefield.");
    }
    Set<String> seenInstances = new HashSet<>();
    for (PlayCardMove.TargetSelection target : targets) {
      if (!PlayCardMove.TargetSelection.FIRST_FRIENDLY_UNIT.equals(target.role())
          && !PlayCardMove.TargetSelection.SECOND_FRIENDLY_UNIT.equals(target.role())) {
        throw new IllegalMoveException("Flash requires friendly unit targets.");
      }
      if (target.instanceId() == null || target.instanceId().isBlank()) {
        throw new IllegalMoveException("Flash requires one or two friendly Unit or Champion targets at a battlefield.");
      }
      if (!seenInstances.add(target.instanceId())) throw new IllegalMoveException("Flash targets must be different units.");
      CardInstance card = findCard(state, target.instanceId());
      if (!isFriendlyPublicBattlefieldUnit(card, move.playerId())) {
        throw new IllegalMoveException("Flash requires one or two friendly Unit or Champion targets at a battlefield.");
      }
    }
  }

  private void validateCharmTarget(LiveGameState state, PlayCardMove move) {
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
      throw new IllegalMoveException("Charm requires an enemy Unit or Champion at a battlefield.");
    }
    CardInstance target = findCard(state, move.targetInstanceId());
    if (!isPublicBattlefieldUnit(target)) {
      throw new IllegalMoveException("Charm can only target a public enemy Unit or Champion at a battlefield.");
    }
    if (move.playerId().equals(target.getOwnerId())) {
      throw new IllegalMoveException("Charm can only target an enemy Unit or Champion.");
    }
  }

  private CardInstance structuredTarget(LiveGameState state, PlayCardMove move, String role, String message) {
    return move.targets().stream()
        .filter(target -> role.equals(target.role()))
        .findFirst()
        .map(target -> findCard(state, target.instanceId()))
        .orElseThrow(() -> new IllegalMoveException(message));
  }

  private boolean isPublicBattlefieldUnit(CardInstance target) {
    if (target.getZone() != ZoneName.BATTLEFIELD || target.isFaceDown()) return false;
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    return isType(targetDef, "Unit") || isType(targetDef, "Champion");
  }

  private boolean isFriendlyPublicBattlefieldUnit(CardInstance target, String playerId) {
    return isPublicBattlefieldUnit(target) && playerId.equals(target.getOwnerId());
  }

  private void validateFriendlyAndEnemyTargets(LiveGameState state, PlayCardMove move) {
    if (move.targets().size() != 2) throw new IllegalMoveException("This card requires a friendly target and an enemy target.");
    Set<String> seenRoles = new HashSet<>();
    Set<String> seenInstances = new HashSet<>();
    for (PlayCardMove.TargetSelection target : move.targets()) {
      if (target == null || target.role() == null || target.role().isBlank()) {
        throw new IllegalMoveException("This card requires a friendly target and an enemy target.");
      }
      if (target.instanceId() == null || target.instanceId().isBlank()) {
        throw new IllegalMoveException("This card requires a friendly target and an enemy target.");
      }
      if (!seenRoles.add(target.role())) throw new IllegalMoveException("This card requires one target for each role.");
      if (!seenInstances.add(target.instanceId())) throw new IllegalMoveException("Targets must be different cards.");
      CardInstance card = findCard(state, target.instanceId());
      validateMultiTargetRole(state, move.playerId(), target.role(), card);
    }
    if (!seenRoles.contains(PlayCardMove.TargetSelection.FRIENDLY_UNIT)
        || !seenRoles.contains(PlayCardMove.TargetSelection.ENEMY_UNIT)) {
      throw new IllegalMoveException("This card requires a friendly target and an enemy target.");
    }
  }

  private void validateMultiTargetRole(LiveGameState state, String playerId, String role, CardInstance target) {
    if (target.getZone() != ZoneName.BASE && target.getZone() != ZoneName.BATTLEFIELD) {
      throw new IllegalMoveException("Target must be a public Unit or Champion.");
    }
    if (target.isFaceDown()) throw new IllegalMoveException("Target must be a public Unit or Champion.");
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    if (!isType(targetDef, "Unit") && !isType(targetDef, "Champion")) {
      throw new IllegalMoveException("Target must be a Unit or Champion.");
    }
    if (PlayCardMove.TargetSelection.FRIENDLY_UNIT.equals(role)) {
      if (!target.getOwnerId().equals(playerId)) throw new IllegalMoveException("Friendly target must be controlled by you.");
      return;
    }
    if (PlayCardMove.TargetSelection.ENEMY_UNIT.equals(role)) {
      if (target.getOwnerId().equals(playerId)) throw new IllegalMoveException("Enemy target must be controlled by an opponent.");
      return;
    }
    throw new IllegalMoveException("Unknown target role.");
  }

  private void validateEquipGear(LiveGameState state, EquipGearMove move) {
    requireMain(state);
    if (state.getActiveShowdown() != null) throw new IllegalMoveException("Resolve the active showdown first.");
    CardInstance gear = findCard(state, move.gearInstanceId());
    if (!move.playerId().equals(gear.getOwnerId())) throw new IllegalMoveException("You do not own that gear.");
    if (gear.getZone() != ZoneName.BASE) throw new IllegalMoveException("Equipment must be in your Base to equip.");
    if (gear.getAttachedToInstanceId() != null && !gear.getAttachedToInstanceId().isBlank()) {
      throw new IllegalMoveException("Attached Equipment cannot be re-equipped.");
    }
    CardDefinition gearDef = cardDataService.getCard(gear.getCardId());
    if (!cardDataService.isEquip(gearDef)) throw new IllegalMoveException("That gear cannot be equipped.");
    if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) throw new IllegalMoveException("Equipment requires a target.");
    CardInstance target = findCard(state, move.targetInstanceId());
    validateEquipTarget(move.playerId(), target);
    validateEquipPayment(state, move, gearDef);
  }

  private void validateActivateAbility(LiveGameState state, ActivateAbilityMove move) {
    activatedAbilityService.validate(state, move);
  }

  private void validateEquipPayment(LiveGameState state, EquipGearMove move, CardDefinition gearDef) {
    EquipCost cost = EquipmentRules.equipCost(gearDef);
    Set<String> allRuneIds = new HashSet<>();
    for (String runeId : move.paymentRuneIds()) {
      validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
    }
    List<String> requiredDomains = new ArrayList<>(cost.premiumDomains());
    for (String runeId : move.premiumRuneIds()) {
      RuneState rune = validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
      if (!consumeMatchingEquipDomain(requiredDomains, rune)) throw new IllegalMoveException("Equip payment uses the wrong rune domain.");
    }
    if (!requiredDomains.isEmpty()) throw new IllegalMoveException("Insufficient equip payment.");
    if (move.premiumRuneIds().size() > cost.premiumDomains().size()) {
      throw new IllegalMoveException("Too many runes selected for equip payment.");
    }
    int availableEnergy = state.getPlayers().stream()
        .filter(player -> move.playerId().equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getAvailableEnergy();
    if (availableEnergy + selectedPaymentEnergy(state, move.paymentRuneIds()) < cost.energyCost()) {
      throw new IllegalMoveException("Insufficient equip payment.");
    }
  }

  private boolean consumeMatchingEquipDomain(List<String> requiredDomains, RuneState rune) {
    for (int i = 0; i < requiredDomains.size(); i++) {
      if (runeMatchesRequiredDomain(rune, requiredDomains.get(i))) {
        requiredDomains.remove(i);
        return true;
      }
    }
    return false;
  }

  private boolean runeMatchesRequiredDomain(RuneState rune, String requiredDomain) {
    CardDefinition runeDef = cardDataService.getCard(rune.getCardId());
    if (runeDef == null || runeDef.domains() == null || runeDef.domains().isEmpty()) return false;
    String normalizedRequired = EquipmentRules.normalizeDomain(requiredDomain);
    return runeDef.domains().stream()
        .filter(domain -> !"COLORLESS".equalsIgnoreCase(domain))
        .anyMatch(domain -> "RAINBOW".equals(normalizedRequired) || EquipmentRules.normalizeDomain(domain).equals(normalizedRequired));
  }

  private void validateEquipTarget(String playerId, CardInstance target) {
    if (target.getZone() != ZoneName.BASE && target.getZone() != ZoneName.BATTLEFIELD) {
      throw new IllegalMoveException("Equipment target must be in Base or at a battlefield.");
    }
    if (target.isFaceDown()) {
      throw new IllegalMoveException("Equipment cannot attach to hidden or face-down cards.");
    }
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    if (isType(targetDef, "Gear")) {
      throw new IllegalMoveException("Equipment cannot attach to another Equipment.");
    }
    if (isType(targetDef, "Rune")) {
      throw new IllegalMoveException("Equipment cannot attach to Runes.");
    }
    if (isType(targetDef, "Battlefield")) {
      throw new IllegalMoveException("Equipment cannot attach to Battlefields.");
    }
    if (isType(targetDef, "Legend")) {
      throw new IllegalMoveException("Equipment cannot attach to Legends.");
    }
    if (!isType(targetDef, "Unit") && !isType(targetDef, "Champion")) {
      throw new IllegalMoveException("Equipment can only attach to a friendly Unit or Champion.");
    }
    if (!target.getOwnerId().equals(playerId)) {
      throw new IllegalMoveException("Equipment can only attach to a friendly Unit or Champion.");
    }
  }

  private boolean isLegalGearDestroyTarget(CardInstance target) {
    if (target.getZone() != ZoneName.BASE && target.getZone() != ZoneName.BATTLEFIELD) return false;
    if (target.isFaceDown()) return false;
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    return isType(targetDef, "Gear");
  }

  private void validateMoveToBattlefield(LiveGameState state, MoveToBattlefieldMove move) {
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    CardDefinition def = cardDataService.getCard(card.getCardId());
    String destinationLocationId = BattlefieldLocationRules.normalize(move.battlefieldLocationId());
    boolean unit = isType(def, "Unit");
    boolean champion = isType(def, "Champion");
    boolean legend = isType(def, "Legend");
    if (legend) throw new IllegalMoveException("Legends cannot be moved to the battlefield in this alpha model.");
    if (champion && card.getZone() == ZoneName.CHAMPION && state.getCurrentPhase() != Phase.MAIN) {
      throw new IllegalMoveException("You can only play your Champion during a legal play window.");
    }
    requireMain(state);
    if (state.getActiveShowdown() != null) throw new IllegalMoveException("A showdown is already active.");
    if (!unit && !champion) {
      throw new IllegalMoveException("Only Units and Champions can move to the battlefield.");
    }
    if (!BattlefieldLocationRules.isActiveLocation(state, destinationLocationId)) {
      throw new IllegalMoveException("That battlefield lane is not active in this game.");
    }
    if (card.getAttachedToInstanceId() != null && !card.getAttachedToInstanceId().isBlank()) {
      throw new IllegalMoveException("Attached Equipment follows its host and cannot move as a combatant.");
    }
    if (card.isFaceDown() || card.getZone() == ZoneName.HIDDEN) {
      throw new IllegalMoveException("Hidden cards cannot move to a battlefield lane this way.");
    }
    boolean fromBattlefield = card.getZone() == ZoneName.BATTLEFIELD;
    if (fromBattlefield) {
      if (BattlefieldLocationRules.locationOf(card).equals(destinationLocationId)) {
        throw new IllegalMoveException("That card is already at that battlefield lane.");
      }
    } else if (unit && card.getZone() != ZoneName.BASE) {
      throw new IllegalMoveException("Only units from your base or another battlefield lane can move to the battlefield.");
    } else if (champion && card.getZone() != ZoneName.CHAMPION && card.getZone() != ZoneName.BASE) {
      throw new IllegalMoveException("Only Champions from your champion zone or base can move to the battlefield.");
    }
    if (card.getZone() != ZoneName.CHAMPION
        && (!move.paymentRuneIds().isEmpty() || !move.premiumRuneIds().isEmpty())) {
      throw new IllegalMoveException("Payment runes can only be selected when playing your Champion.");
    }
    if (champion && card.getZone() == ZoneName.CHAMPION) {
      validateMoveToBattlefieldPayment(state, move, def);
    }
    if (champion && card.getZone() == ZoneName.CHAMPION && playerEnergy(state, move.playerId()) + selectedPaymentEnergy(state, move.paymentRuneIds()) < Math.max(0, def.cost())) {
      throw new IllegalMoveException("Not enough energy to play " + def.name() + ".");
    }
    if (card.isTapped()) throw new IllegalMoveException("Only ready cards can move to the battlefield.");
  }

  private void validateMoveToBattlefieldPayment(LiveGameState state, MoveToBattlefieldMove move, CardDefinition def) {
    Set<String> allRuneIds = new HashSet<>();
    for (String runeId : move.paymentRuneIds()) {
      validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
    }
    for (String runeId : move.premiumRuneIds()) {
      RuneState rune = validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
      if (!runeMatchesCardDomain(rune, def)) throw new IllegalMoveException("Premium payment uses the wrong rune domain.");
    }
    int premiumCost = Math.max(0, def.premiumCost());
    if (move.premiumRuneIds().size() < premiumCost) {
      throw new IllegalMoveException("Insufficient premium payment.");
    }
    if (move.premiumRuneIds().size() > premiumCost) {
      throw new IllegalMoveException("Too many runes selected for premium payment.");
    }
  }

  private int selectedPaymentEnergy(LiveGameState state, List<String> paymentRuneIds) {
    return paymentRuneIds.stream()
        .map(this::requirePaymentRuneId)
        .map(id -> findRune(state, id))
        .mapToInt(RuneState::getNormalEnergy)
        .sum();
  }

  private void validateHideCard(LiveGameState state, HideCardMove move) {
    requireMain(state);
    CardInstance card = findCard(state, move.instanceId());
    if (!move.playerId().equals(card.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    if (card.getZone() != ZoneName.HAND) throw new IllegalMoveException("Only cards in hand can be hidden.");
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (!cardDataService.isHiddenCard(def)) throw new IllegalMoveException("Only cards with Hidden can be hidden.");
    String runeId = requirePaymentRuneId(move.paymentRuneId());
    validatePaymentRune(state, move.playerId(), runeId);
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
    return def != null && def.type() != null && type.equalsIgnoreCase(def.type().trim());
  }

  private void validateResolveShowdown(LiveGameState state, ResolveShowdownMove move) {
    requireMain(state);
    if (state.getActiveShowdown() == null) throw new IllegalMoveException("No showdown is active.");
    if (state.getActiveShowdown().step() == com.riftforge.model.ShowdownStep.ASSIGN_DAMAGE) {
      throw new IllegalMoveException("Assign combat damage before resolving.");
    }
    if (!move.playerId().equals(state.getActiveShowdown().attackingPlayerId())) {
      throw new IllegalMoveException("Only the attacking player can resolve this showdown.");
    }
    if (!state.getActiveShowdown().readyToResolve()) {
      throw new IllegalMoveException("Both players must pass showdown focus before resolving.");
    }
  }

  private void validatePassChainFocus(LiveGameState state, PassChainFocusMove move) {
    requireMain(state);
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) throw new IllegalMoveException("No chain is active.");
    if (chain.topItem() == null) throw new IllegalMoveException("No chain item is active.");
    if (chain.readyToResolveTop()) throw new IllegalMoveException("Resolve the chain item before passing.");
    if (!move.playerId().equals(chain.focusedPlayerId())) {
      throw new IllegalMoveException("Only the focused player can pass chain focus.");
    }
  }

  private void validateResolveChainTop(LiveGameState state, ResolveChainTopMove move) {
    requireMain(state);
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) throw new IllegalMoveException("No chain is active.");
    if (chain.topItem() == null) throw new IllegalMoveException("No chain item is active.");
    if (!chain.readyToResolveTop()) throw new IllegalMoveException("All relevant players must pass before resolving the chain item.");
    if (!move.playerId().equals(chain.focusedPlayerId())) {
      throw new IllegalMoveException("Only the focused player can resolve the chain item.");
    }
  }

  private void validatePassShowdownFocus(LiveGameState state, PassShowdownFocusMove move) {
    requireMain(state);
    if (state.getActiveShowdown() == null) throw new IllegalMoveException("No showdown is active.");
    if (state.getActiveShowdown().step() == com.riftforge.model.ShowdownStep.ASSIGN_DAMAGE) {
      throw new IllegalMoveException("Assign combat damage before taking more showdown actions.");
    }
    if (state.getActiveShowdown().readyToResolve()) {
      throw new IllegalMoveException("Showdown is ready to resolve.");
    }
    if (!showdownParticipantRules.isFocusedPlayer(state, move.playerId())) {
      throw new IllegalMoveException("Only the focused player can pass showdown focus.");
    }
  }

  private void validateAssignCombatDamage(LiveGameState state, AssignCombatDamageMove move) {
    requireMain(state);
    LiveGameState.ShowdownState showdown = state.getActiveShowdown();
    if (showdown == null || showdown.step() != com.riftforge.model.ShowdownStep.ASSIGN_DAMAGE) {
      throw new IllegalMoveException("No combat damage assignment is pending.");
    }
    if (!move.playerId().equals(showdown.assigningPlayerId())) {
      throw new IllegalMoveException("Wait for your combat damage assignment.");
    }
    boolean attackingAssignment = move.playerId().equals(showdown.attackingPlayerId());
    validateDamageAssignments(state, move.assignments(), move.playerId(), attackingAssignment, showdown.locationId());
  }

  private void validateDamageAssignments(
      LiveGameState state,
      List<LiveGameState.CombatDamageAssignment> assignments,
      String sourceOwnerId,
      boolean attackingAssignment,
      String locationId) {
    List<CardInstance> sources = combatantsFor(state, sourceOwnerId, locationId);
    List<CardInstance> targets = state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .filter(card -> !sourceOwnerId.equals(card.getOwnerId()))
        .toList();
    Map<String, CardInstance> targetById = targets.stream().collect(Collectors.toMap(CardInstance::getInstanceId, card -> card));
    Map<String, Integer> assignedByTarget = new HashMap<>();
    for (LiveGameState.CombatDamageAssignment assignment : assignments) {
      if (assignment.amount() <= 0) throw new IllegalMoveException("Combat damage assignments must be positive.");
      CardInstance target = targetById.get(assignment.targetInstanceId());
      if (target == null) throw new IllegalMoveException("Damage target is not a valid opposing combatant.");
      if (assignedByTarget.containsKey(target.getInstanceId())) throw new IllegalMoveException("Duplicate damage assignment.");
      assignedByTarget.merge(target.getInstanceId(), assignment.amount(), Integer::sum);
    }
    CombatContext context = attackingAssignment ? CombatContext.ATTACKING : CombatContext.DEFENDING;
    int totalMight = 0;
    for (CardInstance source : sources) {
      int might = combatStatsService.effectiveMight(state, source, context);
      totalMight += might;
    }
    int totalAssigned = assignedByTarget.values().stream().mapToInt(Integer::intValue).sum();
    if (totalAssigned != totalMight) throw new IllegalMoveException("Assign all available combat damage.");
    CombatContext targetContext = attackingAssignment ? CombatContext.DEFENDING : CombatContext.ATTACKING;
    enforceTankAndLethal(state, targets, assignedByTarget, targetContext);
  }

  private void enforceTankAndLethal(
      LiveGameState state,
      List<CardInstance> targets,
      Map<String, Integer> assignedByTarget,
      CombatContext targetContext) {
    List<CardInstance> tanks = targets.stream()
        .filter(card -> cardDataService.hasKeyword(card, "TANK"))
        .toList();
    boolean nonTankDamaged = targets.stream()
        .filter(card -> !cardDataService.hasKeyword(card, "TANK"))
        .anyMatch(card -> assignedByTarget.getOrDefault(card.getInstanceId(), 0) > 0);
    if (nonTankDamaged) {
      for (CardInstance tank : tanks) {
        if (assignedByTarget.getOrDefault(tank.getInstanceId(), 0) < combatLethalDamage(state, tank, targetContext)) {
          throw new IllegalMoveException("Assign lethal damage to Tank units before non-Tank units.");
        }
      }
    }
    long overAssigned = targets.stream()
        .filter(card -> assignedByTarget.getOrDefault(card.getInstanceId(), 0) > combatLethalDamage(state, card, targetContext))
        .count();
    if (overAssigned > 1) throw new IllegalMoveException("Excess damage can only be assigned to one final target.");
    if (overAssigned == 1) {
      for (CardInstance target : targets) {
        int assigned = assignedByTarget.getOrDefault(target.getInstanceId(), 0);
        if (assigned == 0) throw new IllegalMoveException("Assign lethal damage before assigning excess damage.");
        if (assigned < combatLethalDamage(state, target, targetContext)) throw new IllegalMoveException("Assign lethal damage before assigning excess damage.");
      }
    }
    long damagedTargets = targets.stream()
        .filter(card -> assignedByTarget.getOrDefault(card.getInstanceId(), 0) > 0)
        .count();
    if (damagedTargets > 1) {
      for (CardInstance target : targets) {
        int assigned = assignedByTarget.getOrDefault(target.getInstanceId(), 0);
        if (assigned > 0 && assigned < combatLethalDamage(state, target, targetContext)) {
          throw new IllegalMoveException("Assign lethal damage before spreading damage.");
        }
      }
    }
  }

  private List<CardInstance> combatantsFor(LiveGameState state, String playerId, String locationId) {
    return state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .filter(card -> playerId.equals(card.getOwnerId()))
        .toList();
  }

  private boolean isCombatant(CardInstance card) {
    if (card.getZone() != ZoneName.BATTLEFIELD || card.isFaceDown()) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private int lethalDamage(CardInstance card) {
    return combatDamageRules.lethalDamage(card);
  }

  private int lethalDamage(LiveGameState state, CardInstance card) {
    return combatDamageRules.lethalDamage(state, card);
  }

  private int combatLethalDamage(LiveGameState state, CardInstance card, CombatContext context) {
    return combatDamageRules.combatLethalDamage(state, card, context);
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

  private int playerEnergy(LiveGameState state, String playerId) {
    return state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .map(player -> player.getAvailableEnergy())
        .orElse(0);
  }
}
