package com.riftforge.model.move;

import com.riftforge.model.LiveGameState;
import java.util.List;

public record AssignCombatDamageMove(
    String playerId,
    List<LiveGameState.CombatDamageAssignment> assignments
) implements MoveRequest {
  public AssignCombatDamageMove {
    assignments = assignments == null ? List.of() : List.copyOf(assignments);
  }
}
