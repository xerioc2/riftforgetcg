package com.riftforge.model.move;

import com.riftforge.model.PendingChoice;
import java.util.List;

public record ResolveChoiceMove(
    String playerId,
    String choiceId,
    String selectedOptionId,
    String selectedCardOptionId,
    String selectedAction,
    List<PendingChoice.CardChoiceAssignment> assignments)
    implements MoveRequest {

  public ResolveChoiceMove {
    assignments = assignments == null ? List.of() : List.copyOf(assignments);
  }

  public ResolveChoiceMove(String playerId, String choiceId, String selectedOptionId) {
    this(playerId, choiceId, selectedOptionId, null, null, List.of());
  }
}
