package com.riftforge.model.move;

import com.riftforge.model.ZoneName;

public record PlayCardMove(
    String playerId,
    String instanceId,
    ZoneName targetZone,
    int x,
    int y,
    String targetInstanceId,
    boolean accelerate
) implements MoveRequest {
  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, false);
  }
}
