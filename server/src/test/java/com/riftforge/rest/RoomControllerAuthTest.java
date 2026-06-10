package com.riftforge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.service.GameService;
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
class RoomControllerAuthTest {
  @Mock RoomService roomService;
  @Mock GameService gameService;
  RoomTokenService roomTokenService;
  RoomController controller;
  RoomState room;

  @BeforeEach
  void setUp() {
    roomTokenService = new RoomTokenService();
    controller = new RoomController(roomService, gameService, roomTokenService);
    room = new RoomState();
    room.setCode("ABCD");
    room.setHostId("host");
    room.getPlayers().add(new LobbyPlayer("host", "Host", false, List.of("card")));
    room.getPlayers().add(new LobbyPlayer("guest", "Guest", false, List.of("card")));
    when(roomService.get("ABCD")).thenReturn(room);
  }

  @Test
  void validTokenCanReadySelf() {
    String token = roomTokenService.issue("ABCD", "guest", "PLAYER");
    when(roomService.ready("ABCD", "guest", List.of("card"))).thenReturn(room);

    ResponseEntity<?> response = controller.ready("ABCD", new RoomController.ReadyRequest("guest", token, List.of("card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(roomService).ready("ABCD", "guest", List.of("card"));
  }

  @Test
  void missingTokenCannotReady() {
    ResponseEntity<?> response = controller.ready("ABCD", new RoomController.ReadyRequest("guest", null, List.of("card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).ready("ABCD", "guest", List.of("card"));
  }

  @Test
  void invalidTokenCannotReady() {
    ResponseEntity<?> response = controller.ready("ABCD", new RoomController.ReadyRequest("guest", "bad-token", List.of("card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).ready("ABCD", "guest", List.of("card"));
  }

  @Test
  void playerTokenCannotReadyAnotherPlayer() {
    String guestToken = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.ready("ABCD", new RoomController.ReadyRequest("host", guestToken, List.of("card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).ready("ABCD", "host", List.of("card"));
  }

  @Test
  void hostTokenCanStart() {
    String hostToken = roomTokenService.issue("ABCD", "host", "PLAYER");
    when(roomService.start("ABCD", "host")).thenReturn(room);

    ResponseEntity<?> response = controller.start("ABCD", new RoomController.StartRequest("host", hostToken));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(roomService).start("ABCD", "host");
  }

  @Test
  void nonHostTokenCannotStart() {
    String guestToken = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.start("ABCD", new RoomController.StartRequest("guest", guestToken));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).start("ABCD", "guest");
  }

  @Test
  void hostTokenCanSetBotDeck() {
    String hostToken = roomTokenService.issue("ABCD", "host", "PLAYER");
    when(roomService.setBotDeck("ABCD", List.of("bot-card"))).thenReturn(room);

    ResponseEntity<?> response = controller.setBotDeck("ABCD", new RoomController.BotDeckRequest("host", hostToken, List.of("bot-card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(roomService).setBotDeck("ABCD", List.of("bot-card"));
  }

  @Test
  void noTokenCannotSetBotDeck() {
    ResponseEntity<?> response = controller.setBotDeck("ABCD", new RoomController.BotDeckRequest("host", null, List.of("bot-card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).setBotDeck("ABCD", List.of("bot-card"));
  }

  @Test
  void nonHostTokenCannotSetBotDeck() {
    String guestToken = roomTokenService.issue("ABCD", "guest", "PLAYER");

    ResponseEntity<?> response = controller.setBotDeck("ABCD", new RoomController.BotDeckRequest("guest", guestToken, List.of("bot-card")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(roomService, never()).setBotDeck("ABCD", List.of("bot-card"));
  }
}
