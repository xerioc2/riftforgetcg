package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class PresenceServiceTest {
  private final PresenceService presenceService = new PresenceService(mock(SimpMessagingTemplate.class));

  @Test
  void oneConnectedSessionCountsAsOnePlayer() {
    presenceService.connect("session-1", "player-1", null, "LOBBY");

    assertThat(presenceService.summary(0).onlinePlayers()).isEqualTo(1);
  }

  @Test
  void connectingTwoSessionsForSamePlayerCountsAsOnePlayer() {
    presenceService.connect("session-1", "player-1", null, "LOBBY");
    presenceService.connect("session-2", "player-1", null, "LOBBY");

    assertThat(presenceService.summary(0).onlinePlayers()).isEqualTo(1);
  }

  @Test
  void twoDifferentPlayerIdsCountAsTwoPlayers() {
    presenceService.connect("session-1", "player-1", null, "LOBBY");
    presenceService.connect("session-2", "player-2", null, "LOBBY");

    assertThat(presenceService.summary(0).onlinePlayers()).isEqualTo(2);
  }

  @Test
  void disconnectingOneOfTwoSessionsKeepsPlayerOnline() {
    presenceService.connect("session-1", "player-1", null, "LOBBY");
    presenceService.connect("session-2", "player-1", null, "LOBBY");

    presenceService.disconnect("session-1");

    assertThat(presenceService.summary(0).onlinePlayers()).isEqualTo(1);
  }

  @Test
  void disconnectingLastSessionRemovesPlayer() {
    presenceService.connect("session-1", "player-1", null, "LOBBY");
    presenceService.connect("session-2", "player-1", null, "LOBBY");

    presenceService.disconnect("session-1");
    presenceService.disconnect("session-2");

    assertThat(presenceService.summary(0).onlinePlayers()).isZero();
  }

  @Test
  void activeRoomsCountsUniqueRoomCodes() {
    presenceService.connect("session-1", "player-1", "abcd", "PLAYER");
    presenceService.connect("session-2", "player-2", "ABCD", "PLAYER");
    presenceService.connect("session-3", "player-3", "WXYZ", "PLAYER");

    assertThat(presenceService.summary(3).activeRooms()).isEqualTo(2);
    assertThat(presenceService.summary(3).queueSize()).isEqualTo(3);
    assertThat(presenceService.summary(3).playersSearching()).isEqualTo(3);
  }

  @Test
  void blankPlayerIdDoesNotBreakPresenceOrCountAsOnline() {
    presenceService.connect("session-1", "", null, "SPECTATOR");
    presenceService.connect("session-2", null, null, "SPECTATOR");

    assertThat(presenceService.summary(0).onlinePlayers()).isZero();
  }
}
