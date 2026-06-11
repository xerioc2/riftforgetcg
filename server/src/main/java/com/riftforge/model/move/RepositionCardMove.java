package com.riftforge.model.move;

public record RepositionCardMove(String playerId, String instanceId, int x, int y) implements MoveRequest {}
