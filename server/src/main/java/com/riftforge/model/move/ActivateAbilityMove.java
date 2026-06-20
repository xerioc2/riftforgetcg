package com.riftforge.model.move;

import java.util.List;

public record ActivateAbilityMove(
    String playerId,
    String sourceInstanceId,
    String abilityKey,
    String targetInstanceId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public ActivateAbilityMove {
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public ActivateAbilityMove(String playerId, String sourceInstanceId, String targetInstanceId) {
    this(playerId, sourceInstanceId, null, targetInstanceId, List.of(), List.of());
  }

  public ActivateAbilityMove(String playerId, String sourceInstanceId, String targetInstanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, sourceInstanceId, null, targetInstanceId, paymentRuneIds, premiumRuneIds);
  }
}
