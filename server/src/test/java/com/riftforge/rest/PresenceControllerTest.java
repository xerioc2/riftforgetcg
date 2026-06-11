package com.riftforge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.matchmaking.MatchmakingService;
import com.riftforge.service.PresenceService;
import com.riftforge.service.PresenceService.PresenceSummary;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresenceControllerTest {
  private final PresenceService presenceService = mock(PresenceService.class);
  private final MatchmakingService matchmakingService = mock(MatchmakingService.class);
  private final PresenceController controller =
      new PresenceController(presenceService, matchmakingService);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void presenceReturnsAggregateCountsOnly() {
    when(matchmakingService.queueSize()).thenReturn(3);
    when(presenceService.summary(3)).thenReturn(new PresenceSummary(2, 1, 3, 3));

    PresenceSummary summary = controller.presence();

    assertThat(summary.onlinePlayers()).isEqualTo(2);
    assertThat(summary.activeRooms()).isEqualTo(1);
    assertThat(summary.playersSearching()).isEqualTo(3);
    assertThat(summary.queueSize()).isEqualTo(3);
  }

  @Test
  void presenceResponseDoesNotExposePlayerOrRoomDetails() {
    when(matchmakingService.queueSize()).thenReturn(0);
    when(presenceService.summary(0)).thenReturn(new PresenceSummary(1, 1, 0, 0));

    JsonNode json = objectMapper.valueToTree(controller.presence());

    assertThat(fieldNames(json))
        .containsExactlyInAnyOrder("onlinePlayers", "activeRooms", "playersSearching", "queueSize");
    assertThat(json.has("playerId")).isFalse();
    assertThat(json.has("playerIds")).isFalse();
    assertThat(json.has("playerName")).isFalse();
    assertThat(json.has("roomCode")).isFalse();
    assertThat(json.has("sessionId")).isFalse();
    assertThat(json.has("deck")).isFalse();
  }

  private static List<String> fieldNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    Iterator<String> iterator = node.fieldNames();
    while (iterator.hasNext()) {
      names.add(iterator.next());
    }
    return names;
  }
}
