package com.riftforge.model;

import java.util.List;

public record CompletedMatchSnapshot(
    int turnCount,
    String winnerId,
    List<MatchRecord.PlayerSummary> players,
    boolean hasBotPlayer
) {}
