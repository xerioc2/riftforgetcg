package com.riftforge.service;

import com.riftforge.bot.BotConstants;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.MatchRecord;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MatchHistoryService {
  private static final int MAX_RECORDS = 100;
  private final LinkedList<MatchRecord> history = new LinkedList<>();

  public synchronized void record(LiveGameState state) {
    boolean hasBotPlayer = state.getPlayers().stream()
        .anyMatch(player -> BotConstants.ALL_BOT_IDS.contains(player.getUserId()));
    if (hasBotPlayer) return;

    List<MatchRecord.PlayerSummary> players = state.getPlayers().stream()
        .map(player -> new MatchRecord.PlayerSummary(
            player.getUserId(),
            player.getName() == null || player.getName().isBlank() ? player.getUserId() : player.getName(),
            player.getScore()))
        .toList();

    history.addFirst(new MatchRecord(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        state.getTurnNumber(),
        state.getWinnerId(),
        players));

    while (history.size() > MAX_RECORDS) history.removeLast();
  }

  public synchronized List<MatchRecord> getAll() {
    return List.copyOf(history);
  }
}
