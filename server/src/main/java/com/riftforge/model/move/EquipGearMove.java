package com.riftforge.model.move;

public record EquipGearMove(String playerId, String gearInstanceId, String targetInstanceId) implements MoveRequest {}
