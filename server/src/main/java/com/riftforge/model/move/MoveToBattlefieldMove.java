package com.riftforge.model.move;

import com.riftforge.rules.BattlefieldLocationRules;
import java.util.List;

public record MoveToBattlefieldMove(
    String playerId,
    String instanceId,
    String battlefieldLocationId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public MoveToBattlefieldMove {
    battlefieldLocationId = BattlefieldLocationRules.normalize(battlefieldLocationId);
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public MoveToBattlefieldMove(String playerId, String instanceId) {
    this(playerId, instanceId, null, List.of(), List.of());
  }

  public MoveToBattlefieldMove(String playerId, String instanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, instanceId, null, paymentRuneIds, premiumRuneIds);
  }
}
