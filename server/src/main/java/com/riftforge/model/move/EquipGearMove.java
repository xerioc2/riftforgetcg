package com.riftforge.model.move;

import java.util.List;

public record EquipGearMove(
    String playerId,
    String gearInstanceId,
    String targetInstanceId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds,
    List<String> selectedRecycleCardInstanceIds
) implements MoveRequest {
  public EquipGearMove {
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
    selectedRecycleCardInstanceIds = selectedRecycleCardInstanceIds == null ? List.of() : List.copyOf(selectedRecycleCardInstanceIds);
  }

  public EquipGearMove(String playerId, String gearInstanceId, String targetInstanceId) {
    this(playerId, gearInstanceId, targetInstanceId, List.of(), List.of(), List.of());
  }

  public EquipGearMove(
      String playerId,
      String gearInstanceId,
      String targetInstanceId,
      List<String> paymentRuneIds,
      List<String> premiumRuneIds) {
    this(playerId, gearInstanceId, targetInstanceId, paymentRuneIds, premiumRuneIds, List.of());
  }
}
