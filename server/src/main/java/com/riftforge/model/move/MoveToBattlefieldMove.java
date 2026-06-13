package com.riftforge.model.move;

import java.util.List;

public record MoveToBattlefieldMove(
    String playerId,
    String instanceId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public MoveToBattlefieldMove {
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public MoveToBattlefieldMove(String playerId, String instanceId) {
    this(playerId, instanceId, List.of(), List.of());
  }
}
