package com.riftforge.model;

import java.util.List;

public record MatchRecord(
    String id,
    String completedAt,
    int turnCount,
    String winnerId,
    List<PlayerSummary> players
) {
  public record PlayerSummary(String userId, String name, int score) {}
}
