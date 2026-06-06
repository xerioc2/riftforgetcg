package com.riftforge.model.move;

public record DealCardMove(String playerId, String cardId, String targetZone, int x, int y) implements MoveRequest {}
