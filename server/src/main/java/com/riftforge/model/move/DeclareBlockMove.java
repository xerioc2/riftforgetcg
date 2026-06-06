package com.riftforge.model.move;

import java.util.Map;

public record DeclareBlockMove(String playerId, Map<String, String> blockerToAttacker) implements MoveRequest {}
