package com.riftforge.model.move;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DealCardMove.class, name = "DEAL_CARD"),
  @JsonSubTypes.Type(value = TapCardMove.class, name = "TAP_CARD"),
  @JsonSubTypes.Type(value = FlipCardMove.class, name = "FLIP_CARD"),
  @JsonSubTypes.Type(value = PlayCardMove.class, name = "PLAY_CARD"),
  @JsonSubTypes.Type(value = MoveCardMove.class, name = "MOVE_CARD"),
  @JsonSubTypes.Type(value = RepositionCardMove.class, name = "REPOSITION_CARD"),
  @JsonSubTypes.Type(value = TapRuneMove.class, name = "TAP_RUNE"),
  @JsonSubTypes.Type(value = DiscardRuneMove.class, name = "DISCARD_RUNE"),
  @JsonSubTypes.Type(value = MoveToBattlefieldMove.class, name = "MOVE_TO_BATTLEFIELD"),
  @JsonSubTypes.Type(value = MoveToBaseMove.class, name = "MOVE_TO_BASE"),
  @JsonSubTypes.Type(value = SelectBattlefieldMove.class, name = "SELECT_BATTLEFIELD"),
  @JsonSubTypes.Type(value = MulliganMove.class, name = "MULLIGAN"),
  @JsonSubTypes.Type(value = UndoRunesMove.class, name = "UNDO_RUNES"),
  @JsonSubTypes.Type(value = PassPhaseMove.class, name = "PASS_PHASE"),
  @JsonSubTypes.Type(value = PassChainFocusMove.class, name = "PASS_CHAIN_FOCUS"),
  @JsonSubTypes.Type(value = ResolveChainTopMove.class, name = "RESOLVE_CHAIN_TOP"),
  @JsonSubTypes.Type(value = PassShowdownFocusMove.class, name = "PASS_SHOWDOWN_FOCUS"),
  @JsonSubTypes.Type(value = ResolveShowdownMove.class, name = "RESOLVE_SHOWDOWN"),
  @JsonSubTypes.Type(value = AssignCombatDamageMove.class, name = "ASSIGN_COMBAT_DAMAGE"),
  @JsonSubTypes.Type(value = AdjustScoreMove.class, name = "ADJUST_SCORE"),
  @JsonSubTypes.Type(value = VisionChoiceMove.class, name = "VISION_CHOICE"),
  @JsonSubTypes.Type(value = DismissRevealedMove.class, name = "DISMISS_REVEALED"),
  @JsonSubTypes.Type(value = HideCardMove.class, name = "HIDE_CARD"),
  @JsonSubTypes.Type(value = EquipGearMove.class, name = "EQUIP_GEAR"),
  @JsonSubTypes.Type(value = ActivateAbilityMove.class, name = "ACTIVATE_ABILITY"),
  @JsonSubTypes.Type(value = ResolveChoiceMove.class, name = "RESOLVE_CHOICE")
})
public sealed interface MoveRequest permits DealCardMove, TapCardMove, FlipCardMove, PlayCardMove, MoveCardMove, RepositionCardMove, TapRuneMove, DiscardRuneMove, MoveToBattlefieldMove, MoveToBaseMove, SelectBattlefieldMove, MulliganMove, UndoRunesMove, PassPhaseMove, PassChainFocusMove, ResolveChainTopMove, PassShowdownFocusMove, ResolveShowdownMove, AssignCombatDamageMove, AdjustScoreMove, VisionChoiceMove, DismissRevealedMove, HideCardMove, EquipGearMove, ActivateAbilityMove, ResolveChoiceMove {
  String playerId();
}
