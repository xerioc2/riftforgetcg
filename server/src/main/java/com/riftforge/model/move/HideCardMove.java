package com.riftforge.model.move;

public record HideCardMove(String playerId, String instanceId, String paymentRuneId) implements MoveRequest {}
