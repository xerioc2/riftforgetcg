package com.riftforge.service;

import com.riftforge.model.CompletedMatchSnapshot;
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

  public synchronized void record(CompletedMatchSnapshot snapshot) {
    if (snapshot.hasBotPlayer()) return;

    history.addFirst(new MatchRecord(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        snapshot.turnCount(),
        snapshot.winnerId(),
        snapshot.players()));

    while (history.size() > MAX_RECORDS) history.removeLast();
  }

  public synchronized List<MatchRecord> getAll() {
    return List.copyOf(history);
  }
}
