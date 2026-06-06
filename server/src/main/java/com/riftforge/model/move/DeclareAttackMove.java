package com.riftforge.model.move;

import java.util.List;

public record DeclareAttackMove(String playerId, List<String> attackerInstanceIds, String targetPlayerId) implements MoveRequest {}
