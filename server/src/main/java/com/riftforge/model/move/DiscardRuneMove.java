package com.riftforge.model.move;

public record DiscardRuneMove(String playerId, String runeInstanceId) implements MoveRequest {}
