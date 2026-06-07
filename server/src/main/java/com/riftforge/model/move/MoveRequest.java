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
  @JsonSubTypes.Type(value = TapRuneMove.class, name = "TAP_RUNE"),
  @JsonSubTypes.Type(value = DiscardRuneMove.class, name = "DISCARD_RUNE"),
  @JsonSubTypes.Type(value = DeclareAttackMove.class, name = "DECLARE_ATTACK"),
  @JsonSubTypes.Type(value = DeclareBlockMove.class, name = "DECLARE_BLOCK"),
  @JsonSubTypes.Type(value = MulliganMove.class, name = "MULLIGAN"),
  @JsonSubTypes.Type(value = UndoRunesMove.class, name = "UNDO_RUNES"),
  @JsonSubTypes.Type(value = PassPhaseMove.class, name = "PASS_PHASE"),
  @JsonSubTypes.Type(value = AdjustScoreMove.class, name = "ADJUST_SCORE")
})
public sealed interface MoveRequest permits DealCardMove, TapCardMove, FlipCardMove, PlayCardMove, MoveCardMove, TapRuneMove, DiscardRuneMove, DeclareAttackMove, DeclareBlockMove, MulliganMove, UndoRunesMove, PassPhaseMove, AdjustScoreMove {
  String playerId();
}
