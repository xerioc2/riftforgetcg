package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CompletedMatchSnapshot;
import com.riftforge.model.MatchRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchHistoryServiceTest {
  private final MatchHistoryService service = new MatchHistoryService();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void recordsCompletedMatchWinnerAndPublicScores() {
    service.record(new CompletedMatchSnapshot(
        9,
        "p1",
        List.of(
            new MatchRecord.PlayerSummary("p1", "Player One", 8),
            new MatchRecord.PlayerSummary("p2", "Player Two", 6)),
        false));

    assertThat(service.getAll()).hasSize(1);
    MatchRecord record = service.getAll().getFirst();
    assertThat(record.winnerId()).isEqualTo("p1");
    assertThat(record.turnCount()).isEqualTo(9);
    assertThat(record.players())
        .extracting(MatchRecord.PlayerSummary::score)
        .containsExactly(8, 6);
  }

  @Test
  void botMatchesAreExcludedFromPublicHistory() {
    service.record(new CompletedMatchSnapshot(
        4,
        "bot",
        List.of(new MatchRecord.PlayerSummary("bot", "RiftBot", 8)),
        true));

    assertThat(service.getAll()).isEmpty();
  }

  @Test
  void publicHistorySerializationContainsOnlySummaryData() throws Exception {
    service.record(new CompletedMatchSnapshot(
        7,
        "p1",
        List.of(
            new MatchRecord.PlayerSummary("p1", "Player One", 8),
            new MatchRecord.PlayerSummary("p2", "Player Two", 5)),
        false));

    String json = objectMapper.writeValueAsString(service.getAll());

    assertThat(json).contains("Player One", "Player Two", "winnerId", "turnCount");
    assertThat(json).doesNotContain(
        "deckPool",
        "runeDeckPool",
        "selectedBattlefields",
        "sessionToken",
        "private-hand-card",
        "private-deck-card",
        "VISION_PEEK",
        "VISION_RESOLVED");
  }
}
