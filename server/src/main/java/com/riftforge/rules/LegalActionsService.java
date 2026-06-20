package com.riftforge.rules;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.RuneState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.engine.ActivatedAbilityService;
import com.riftforge.engine.CombatStatsService;
import com.riftforge.engine.CombatStatsService.CombatContext;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LegalActionsService {
  private final CardDataService cardDataService;
  private final ShowdownParticipantRules showdownParticipantRules;
  private final CombatStatsService combatStatsService;
  private final ActivatedAbilityService activatedAbilityService;

  public LegalActionsService() {
    this(null, new ShowdownParticipantRules(), null, null);
  }

  public LegalActionsService(CardDataService cardDataService) {
    this(
        cardDataService,
        new ShowdownParticipantRules(),
        cardDataService == null ? null : new CombatStatsService(cardDataService),
        cardDataService == null ? null : new ActivatedAbilityService(cardDataService));
  }

  @Autowired
  public LegalActionsService(CardDataService cardDataService, ShowdownParticipantRules showdownParticipantRules, CombatStatsService combatStatsService, ActivatedAbilityService activatedAbilityService) {
    this.cardDataService = cardDataService;
    this.showdownParticipantRules = showdownParticipantRules;
    this.combatStatsService = combatStatsService;
    this.activatedAbilityService = activatedAbilityService;
  }

  public Set<LegalAction> legalActionsFor(LiveGameState state, String playerId) {
    return legalActions(state, playerId);
  }

  public Set<LegalAction> legalActions(LiveGameState state, String playerId) {
    if (state == null || playerId == null || state.getWinnerId() != null || !isPlayer(state, playerId)) {
      return Set.of();
    }

    EnumSet<LegalAction> actions = EnumSet.noneOf(LegalAction.class);
    if (state.getPendingChoice() != null) {
      if (playerId.equals(state.getPendingChoice().getPlayerId())) actions.add(LegalAction.RESOLVE_CHOICE);
      return actions;
    }

    if (state.getChainState() != null) {
      if (!playerId.equals(state.getChainState().focusedPlayerId())) return actions;
      if (state.getChainState().readyToResolveTop()) {
        actions.add(LegalAction.RESOLVE_CHAIN_TOP);
      } else {
        actions.add(LegalAction.PASS_CHAIN_FOCUS);
        if (hasPlayableGustInHand(state, playerId)
            || hasPlayableDisciplineInHand(state, playerId)
            || hasPlayableEnGardeInHand(state, playerId)
            || hasPlayableDefiantDanceInHand(state, playerId)
            || hasPlayableFlashInHand(state, playerId)
            || hasPlayableDefyInHand(state, playerId)
            || hasPlayableNotSoFastInHand(state, playerId)
            || hasPlayableAbandonInHand(state, playerId)) actions.add(LegalAction.PLAY_CARD);
      }
      return actions;
    }

    if (state.getCurrentPhase() == Phase.SELECT_BATTLEFIELD) {
      state.getPlayers().stream()
          .filter(player -> playerId.equals(player.getUserId()))
          .filter(player -> !player.getSelectedBattlefields().isEmpty())
          .findFirst()
          .filter(player -> player.getSelectedBattlefieldId() == null || player.getSelectedBattlefieldId().isBlank())
          .ifPresent(player -> actions.add(LegalAction.SELECT_BATTLEFIELD));
      return actions;
    }

    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      if (!state.getMulligansDone().contains(playerId)) {
        actions.add(LegalAction.KEEP_HAND);
        actions.add(LegalAction.MULLIGAN);
      }
      return actions;
    }

    if (state.getActiveShowdown() != null) {
      if (state.getActiveShowdown().step() == ShowdownStep.ASSIGN_DAMAGE) {
        if (playerId.equals(state.getActiveShowdown().assigningPlayerId())) actions.add(LegalAction.ASSIGN_COMBAT_DAMAGE);
        return actions;
      }
      if (!showdownParticipantRules.isFocusedPlayer(state, playerId)) return actions;
      if (state.getActiveShowdown().readyToResolve() && showdownParticipantRules.isShowdownAttacker(state, playerId)) {
        actions.add(LegalAction.RESOLVE_SHOWDOWN);
        return actions;
      }
      actions.add(LegalAction.PASS_SHOWDOWN_FOCUS);
      if (!state.getActiveShowdown().readyToResolve()
          && (hasSupportedActionCardInHand(state, playerId) || hasPlayableTargetedReactionInHand(state, playerId))) {
        actions.add(LegalAction.PLAY_CARD);
      }
      return actions;
    }

    addSandboxActions(state, actions);

    if (!playerId.equals(state.getActivePlayerId())) {
      // TODO: Add Reaction/Action windows once the chain and timing permissions are modeled.
      return actions;
    }

    if (state.getCurrentPhase() == Phase.MAIN) {
      actions.add(LegalAction.PASS_PHASE);
      actions.add(LegalAction.END_TURN);
      actions.add(LegalAction.PLAY_CARD);
      actions.add(LegalAction.MOVE_TO_BATTLEFIELD);
      actions.add(LegalAction.REPOSITION_CARD);
      actions.add(LegalAction.TAP_RUNE);
      actions.add(LegalAction.DISCARD_RUNE);
      actions.add(LegalAction.UNDO_RUNES);
      actions.add(LegalAction.VISION_CHOICE);
      if (hasHideableCardInHand(state, playerId) && hasReadyRune(state, playerId)) actions.add(LegalAction.HIDE_CARD);
      if (hasEquippableGearAtBase(state, playerId) && hasLegalEquipTarget(state, playerId)) actions.add(LegalAction.EQUIP_GEAR);
      if (hasActivatableAbilityAtBase(state, playerId)) actions.add(LegalAction.ACTIVATE_ABILITY);
      return actions;
    }

    actions.add(LegalAction.PASS_PHASE);
    if (state.getCurrentPhase() == Phase.END) actions.add(LegalAction.END_TURN);
    return actions;
  }

  private boolean isPlayer(LiveGameState state, String playerId) {
    return state.getPlayers().stream().anyMatch(player -> playerId.equals(player.getUserId()));
  }

  private void addSandboxActions(LiveGameState state, EnumSet<LegalAction> actions) {
    if (state.getGameMode() != GameMode.SANDBOX) return;
    actions.add(LegalAction.SANDBOX_DEAL_CARD);
    actions.add(LegalAction.SANDBOX_ADJUST_SCORE);
    actions.add(LegalAction.SANDBOX_TAP_CARD);
    actions.add(LegalAction.SANDBOX_FLIP_CARD);
    actions.add(LegalAction.SANDBOX_MOVE_CARD);
  }

  private boolean hasSupportedActionCardInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isActionCard(def)
            && !cardDataService.isReactionCard(def)
            && !cardDataService.isUnsupportedAction(def.id()));
  }

  private boolean hasPlayableGustInHand(LiveGameState state, String playerId) {
    if (cardDataService == null || combatStatsService == null) return false;
    boolean legalTarget = state.getCards().stream().anyMatch(this::isLegalGustTarget);
    if (!legalTarget) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isGustReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableDisciplineInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    boolean legalTarget = state.getCards().stream().anyMatch(this::isPublicBattlefieldUnit);
    if (!legalTarget) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isDisciplineReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableEnGardeInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    boolean legalTarget = state.getCards().stream().anyMatch(card -> isFriendlyPublicBattlefieldUnit(card, playerId));
    if (!legalTarget) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isEnGardeReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableDefiantDanceInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    long legalTargets = state.getCards().stream().filter(this::isPublicBattlefieldUnit).count();
    if (legalTargets < 2) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isDefiantDanceReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableFlashInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    boolean legalTarget = state.getCards().stream().anyMatch(card -> isFriendlyPublicBattlefieldUnit(card, playerId));
    if (!legalTarget) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isFlashReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableDefyInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    if (state.getChainState() == null || !hasLegalDefyTarget(state, playerId)) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isDefyCounterReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableNotSoFastInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    if (state.getChainState() == null || !hasLegalNotSoFastTarget(state, playerId)) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isNotSoFastCounterReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableAbandonInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    if (state.getChainState() == null || !hasLegalAbandonTarget(state)) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> def != null
            && cardDataService.isAbandonCounterReaction(def)
            && !cardDataService.isUnsupportedAction(def.id())
            && canPay(state, playerId, def));
  }

  private boolean hasPlayableTargetedReactionInHand(LiveGameState state, String playerId) {
    return hasPlayableGustInHand(state, playerId)
        || hasPlayableDisciplineInHand(state, playerId)
        || hasPlayableEnGardeInHand(state, playerId)
        || hasPlayableDefiantDanceInHand(state, playerId)
        || hasPlayableFlashInHand(state, playerId);
  }

  private boolean hasLegalDefyTarget(LiveGameState state, String playerId) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) return false;
    return chain.chainItems().stream().anyMatch(item -> isLegalDefyTarget(item, playerId));
  }

  private boolean isLegalDefyTarget(LiveGameState.ChainItem item, String playerId) {
    if (item == null || !item.isPending() || !item.counterable() || !item.targetableOnChain()) return false;
    if (!item.isPubliclyVisible() && !playerId.equals(item.controllerPlayerId())) return false;
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(item.chainItemType())) return false;
    CardDefinition def = item.sourceCardId() == null || item.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(item.sourceCardId());
    return def != null
        && "Spell".equalsIgnoreCase(def.type())
        && Math.max(0, def.cost()) <= 4
        && Math.max(0, def.premiumCost()) <= 1;
  }

  private boolean hasLegalNotSoFastTarget(LiveGameState state, String playerId) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) return false;
    return chain.chainItems().stream().anyMatch(item -> isLegalNotSoFastTarget(item, playerId));
  }

  private boolean isLegalNotSoFastTarget(LiveGameState.ChainItem item, String playerId) {
    if (item == null || !item.isPending() || !item.counterable() || !item.targetableOnChain()) return false;
    if (!item.isPubliclyVisible()) return false;
    if (playerId.equals(item.controllerPlayerId())) return false;
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(item.chainItemType())) return false;
    CardDefinition def = item.sourceCardId() == null || item.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(item.sourceCardId());
    if (def == null || !"Spell".equalsIgnoreCase(def.type())) return false;
    return item.chainTargets().stream().anyMatch(target ->
        target.publicSafe()
            && playerId.equals(target.targetControllerPlayerId())
            && ("UNIT".equalsIgnoreCase(target.targetKind())
                || "CHAMPION_UNIT".equalsIgnoreCase(target.targetKind())
                || "GEAR".equalsIgnoreCase(target.targetKind())));
  }

  private boolean hasLegalAbandonTarget(LiveGameState state) {
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null) return false;
    return chain.chainItems().stream().anyMatch(this::isLegalAbandonTarget);
  }

  private boolean isLegalAbandonTarget(LiveGameState.ChainItem item) {
    if (item == null || !item.isPending() || !item.counterable() || !item.targetableOnChain()) return false;
    if (!item.isPubliclyVisible()) return false;
    if (!LiveGameState.ChainItem.TYPE_SPELL.equalsIgnoreCase(item.chainItemType())) return false;
    CardDefinition def = item.sourceCardId() == null || item.sourceCardId().isBlank()
        ? null
        : cardDataService.getCard(item.sourceCardId());
    return def != null && "Spell".equalsIgnoreCase(def.type());
  }

  private boolean isLegalGustTarget(CardInstance target) {
    if (target.getZone() != ZoneName.BATTLEFIELD || target.isFaceDown()) return false;
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    if (targetDef == null || (!"Unit".equalsIgnoreCase(targetDef.type()) && !"Champion".equalsIgnoreCase(targetDef.type()))) return false;
    return combatStatsService.effectiveMight(target, CombatContext.IDLE) <= 3;
  }

  private boolean isFriendlyPublicBattlefieldUnit(CardInstance target, String playerId) {
    return playerId.equals(target.getOwnerId()) && isPublicBattlefieldUnit(target);
  }

  private boolean isPublicBattlefieldUnit(CardInstance target) {
    if (target.getZone() != ZoneName.BATTLEFIELD || target.isFaceDown()) return false;
    CardDefinition targetDef = cardDataService.getCard(target.getCardId());
    return targetDef != null && ("Unit".equalsIgnoreCase(targetDef.type()) || "Champion".equalsIgnoreCase(targetDef.type()));
  }

  private boolean canPay(LiveGameState state, String playerId, CardDefinition def) {
    return canPayEnergy(state, playerId, Math.max(0, def.cost()));
  }

  private boolean canPayEnergy(LiveGameState state, String playerId, int cost) {
    int availableEnergy = state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .map(player -> player.getAvailableEnergy())
        .orElse(0);
    int readyRuneEnergy = state.getRunes().stream()
        .filter(rune -> playerId.equals(rune.getOwnerId()) && !rune.isTapped())
        .mapToInt(rune -> Math.max(0, rune.getNormalEnergy()))
        .sum();
    return availableEnergy + readyRuneEnergy >= Math.max(0, cost);
  }

  private boolean hasHideableCardInHand(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(cardDataService::isHiddenCard);
  }

  private boolean hasReadyRune(LiveGameState state, String playerId) {
    return state.getRunes().stream()
        .anyMatch(rune -> playerId.equals(rune.getOwnerId()) && !rune.isTapped());
  }

  private boolean hasEquippableGearAtBase(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()))
        .filter(card -> card.getZone() == ZoneName.BASE)
        .filter(card -> card.getAttachedToInstanceId() == null || card.getAttachedToInstanceId().isBlank())
        .map(card -> cardDataService.getCard(card.getCardId()))
        .anyMatch(def -> cardDataService.isEquip(def) && canPayEquip(state, playerId, def));
  }

  private boolean hasActivatableAbilityAtBase(LiveGameState state, String playerId) {
    return activatedAbilityService != null && activatedAbilityService.hasLegalActivation(state, playerId);
  }

  private boolean hasLegalEquipTarget(LiveGameState state, String playerId) {
    if (cardDataService == null) return false;
    return state.getCards().stream().anyMatch(card -> isLegalEquipTarget(card, playerId));
  }

  private boolean isLegalEquipTarget(CardInstance card, String playerId) {
    if (!playerId.equals(card.getOwnerId())) return false;
    if (card.getZone() != ZoneName.BASE && card.getZone() != ZoneName.BATTLEFIELD) return false;
    if (card.isFaceDown()) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && isUnitOrChampion(def);
  }

  private boolean isUnitOrChampion(CardDefinition def) {
    return "Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type());
  }

  private boolean canPayEquip(LiveGameState state, String playerId, CardDefinition gearDef) {
    EquipmentRules.EquipCost cost = EquipmentRules.equipCost(gearDef);
    List<RuneState> remainingRunes = new ArrayList<>(state.getRunes().stream()
        .filter(rune -> playerId.equals(rune.getOwnerId()) && !rune.isTapped())
        .toList());
    for (String domain : cost.premiumDomains()) {
      int index = firstMatchingRuneIndex(remainingRunes, domain);
      if (index < 0) return false;
      remainingRunes.remove(index);
    }
    int availableEnergy = state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .map(player -> player.getAvailableEnergy())
        .orElse(0);
    int readyRuneEnergy = remainingRunes.stream()
        .mapToInt(rune -> Math.max(0, rune.getNormalEnergy()))
        .sum();
    return availableEnergy + readyRuneEnergy >= cost.energyCost();
  }

  private int firstMatchingRuneIndex(List<RuneState> runes, String requiredDomain) {
    for (int i = 0; i < runes.size(); i++) {
      if (runeMatchesRequiredDomain(runes.get(i), requiredDomain)) return i;
    }
    return -1;
  }

  private boolean runeMatchesRequiredDomain(RuneState rune, String requiredDomain) {
    CardDefinition runeDef = cardDataService.getCard(rune.getCardId());
    if (runeDef == null || runeDef.domains() == null || runeDef.domains().isEmpty()) return false;
    String normalizedRequired = EquipmentRules.normalizeDomain(requiredDomain);
    return runeDef.domains().stream()
        .filter(domain -> !"COLORLESS".equalsIgnoreCase(domain))
        .anyMatch(domain -> "RAINBOW".equals(normalizedRequired) || EquipmentRules.normalizeDomain(domain).equals(normalizedRequired));
  }
}
