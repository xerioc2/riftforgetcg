package com.riftforge.model.move;

import java.util.List;

public record MulliganMove(String playerId, List<String> keepInstanceIds) implements MoveRequest {}
