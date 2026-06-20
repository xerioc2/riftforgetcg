package com.riftforge.model.move;

import java.util.List;

public record EquipGearMove(
    String playerId,
    String gearInstanceId,
    String targetInstanceId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public EquipGearMove {
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public EquipGearMove(String playerId, String gearInstanceId, String targetInstanceId) {
    this(playerId, gearInstanceId, targetInstanceId, List.of(), List.of());
  }
}
