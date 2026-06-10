package com.riftforge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameRestControllerAuthTest {
  @Mock GameService gameService;
  @Mock CardDataService cardDataService;
  @Mock RoomService roomService;
  @Mock MatchHistoryService matchHistoryService;
  RoomTokenService roomTokenService;
  GameRestController controller;
  LiveGameState state;
  RoomState room;

  @BeforeEach
  void setUp() {
    roomTokenService = new RoomTokenService();
    controller = new GameRestController(gameService, cardDataService, roomService, matchHistoryService, roomTokenService);
    state = new LiveGameState();
    state.setRoomCode("ABCD");
    room = new RoomState();
    room.setCode("ABCD");
    room.setHostId("host");
    room.getPlayers().add(new LobbyPlayer("host", "Host", false, List.of("card")));
    room.getPlayers().add(new LobbyPlayer("guest", "Guest", false, List.of("card")));
    when(gameService.currentStateFor("ABCD", null)).thenReturn(state);
    when(gameService.currentStateFor("ABCD", "guest")).thenReturn(state);
    when(roomService.get("ABCD")).thenReturn(room);
  }

  @Test
  void spectatorStateFetchUsesPublicProjection() {
    ResponseEntity<?> response = controller.state("ABCD", null, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(gameService).currentStateFor("ABCD", null);
  }

  @Test
  void playerStateFetchRequiresValidToken() {
    String token = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.state("ABCD", "guest", token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(gameService).currentStateFor("ABCD", "guest");
  }

  @Test
  void invalidTokenCannotFetchPrivateState() {
    ResponseEntity<?> response = controller.state("ABCD", "guest", "bad-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(gameService, never()).currentStateFor("ABCD", "guest");
  }

  @Test
  void partialIdentityCannotFallBackToSpectatorState() {
    ResponseEntity<?> response = controller.state("ABCD", "guest", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(gameService, never()).currentStateFor("ABCD", null);
    verify(gameService, never()).currentStateFor("ABCD", "guest");
  }

  @Test
  void tokenWithoutPlayerIdCannotFallBackToSpectatorState() {
    String token = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.state("ABCD", null, token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(gameService, never()).currentStateFor("ABCD", null);
    verify(gameService, never()).currentStateFor("ABCD", "guest");
  }

  @Test
  void playerTokenCannotFetchAnotherPlayersProjection() {
    String guestToken = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.state("ABCD", "host", guestToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(gameService, never()).currentStateFor("ABCD", "host");
  }

  @Test
  void hostTokenCanReset() {
    String token = roomTokenService.issue("ABCD", "host", "PLAYER");

    ResponseEntity<?> response = controller.reset("ABCD", "host", token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(gameService).reset(eq("ABCD"), any(), any(), any(), any());
  }

  @Test
  void nonHostTokenCannotReset() {
    String token = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.reset("ABCD", "guest", token);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(gameService, never()).reset(eq("ABCD"), any(), any(), any(), any());
  }
}
