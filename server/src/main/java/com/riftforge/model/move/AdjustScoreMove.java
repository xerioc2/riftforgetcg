package com.riftforge.model.move;

public record AdjustScoreMove(String playerId, String targetPlayerId, int delta) implements MoveRequest {}
