package com.riftforge.model.move;

import com.riftforge.rules.BattlefieldLocationRules;
import com.riftforge.model.ZoneName;
import java.util.List;

public record MoveToBattlefieldMove(
    String playerId,
    String instanceId,
    String battlefieldLocationId,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds,
    ZoneName targetZone
) implements MoveRequest {
  public MoveToBattlefieldMove {
    targetZone = targetZone == null ? ZoneName.BATTLEFIELD : targetZone;
    battlefieldLocationId = BattlefieldLocationRules.normalize(battlefieldLocationId);
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public MoveToBattlefieldMove(String playerId, String instanceId) {
    this(playerId, instanceId, null, List.of(), List.of(), ZoneName.BATTLEFIELD);
  }

  public MoveToBattlefieldMove(String playerId, String instanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, instanceId, null, paymentRuneIds, premiumRuneIds, ZoneName.BATTLEFIELD);
  }

  public MoveToBattlefieldMove(String playerId, String instanceId, String battlefieldLocationId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, instanceId, battlefieldLocationId, paymentRuneIds, premiumRuneIds, ZoneName.BATTLEFIELD);
  }
}
