package com.riftforge.model.move;

import com.riftforge.model.ZoneName;
import java.util.List;

public record PlayCardMove(
    String playerId,
    String instanceId,
    ZoneName targetZone,
    int x,
    int y,
    String targetInstanceId,
    boolean accelerate,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public PlayCardMove {
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, false, List.of(), List.of());
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId, boolean accelerate) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, accelerate, List.of(), List.of());
  }
}
