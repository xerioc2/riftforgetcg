package com.riftforge.model.move;

import com.riftforge.model.ZoneName;

public record PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y) implements MoveRequest {}
