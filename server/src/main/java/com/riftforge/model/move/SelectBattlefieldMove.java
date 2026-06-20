package com.riftforge.model.move;

public record SelectBattlefieldMove(String playerId, String battlefieldCardId) implements MoveRequest {}
