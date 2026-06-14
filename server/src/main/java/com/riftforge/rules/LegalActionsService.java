package com.riftforge.rules;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LegalActionsService {
  private final CardDataService cardDataService;
  private final ShowdownParticipantRules showdownParticipantRules;

  public LegalActionsService() {
    this(null, new ShowdownParticipantRules());
  }

  public LegalActionsService(CardDataService cardDataService) {
    this(cardDataService, new ShowdownParticipantRules());
  }

  @Autowired
  public LegalActionsService(CardDataService cardDataService, ShowdownParticipantRules showdownParticipantRules) {
    this.cardDataService = cardDataService;
    this.showdownParticipantRules = showdownParticipantRules;
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

    addSandboxActions(state, actions);

    if (state.getActiveShowdown() != null) {
      if (showdownParticipantRules.isShowdownAttacker(state, playerId)) {
        actions.add(LegalAction.RESOLVE_SHOWDOWN);
      }
      if (showdownParticipantRules.isShowdownParticipant(state, playerId) && hasSupportedActionCardInHand(state, playerId)) actions.add(LegalAction.PLAY_CARD);
      return actions;
    }

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
        .anyMatch(cardDataService::isEquip);
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
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }
}
