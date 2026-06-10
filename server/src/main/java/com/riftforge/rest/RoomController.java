package com.riftforge.rest;

import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.GameMode;
import com.riftforge.model.RoomState;
import com.riftforge.service.GameService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
  private final RoomService roomService;
  private final GameService gameService;
  private final RoomTokenService roomTokenService;

  public RoomController(RoomService roomService, GameService gameService, RoomTokenService roomTokenService) {
    this.roomService = roomService;
    this.gameService = gameService;
    this.roomTokenService = roomTokenService;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<String> badRequest(IllegalStateException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }

  record CreateRequest(String playerId, String playerName, boolean withBot, GameMode gameMode) {}
  record JoinRequest(String playerId, String playerName) {}
  record ReadyRequest(String playerId, String sessionToken, List<String> deckCardIds) {}
  record StartRequest(String playerId, String sessionToken) {}
  record BotDeckRequest(String playerId, String sessionToken, List<String> deckCardIds) {}
  record TokenizedRoom(RoomState room, String sessionToken) {}

  @PostMapping
  public TokenizedRoom create(@RequestBody CreateRequest req) {
    RoomState room = roomService.create(req.playerId(), req.playerName(), req.withBot(), req.gameMode());
    String sessionToken = roomTokenService.issue(room.getCode(), req.playerId(), "PLAYER");
    return new TokenizedRoom(room, sessionToken);
  }

  @GetMapping("/{code}")
  public RoomState get(@PathVariable String code) {
    return roomService.get(code);
  }

  @PostMapping("/{code}/join")
  public TokenizedRoom join(@PathVariable String code, @RequestBody JoinRequest req) {
    RoomState room = roomService.join(code, req.playerId(), req.playerName());
    String sessionToken = roomTokenService.issue(room.getCode(), req.playerId(), "PLAYER");
    return new TokenizedRoom(room, sessionToken);
  }

  @PostMapping("/{code}/ready")
  public ResponseEntity<?> ready(@PathVariable String code, @RequestBody ReadyRequest req) {
    ResponseEntity<String> authError = validatePlayer(code, req.playerId(), req.sessionToken());
    if (authError != null) return authError;
    return ResponseEntity.ok(roomService.ready(code, req.playerId(), req.deckCardIds()));
  }

  @PostMapping("/{code}/bot-deck")
  public ResponseEntity<?> setBotDeck(@PathVariable String code, @RequestBody BotDeckRequest req) {
    RoomState room = roomService.get(code);
    ResponseEntity<String> authError = validateHost(room, code, req.playerId(), req.sessionToken());
    if (authError != null) return authError;
    return ResponseEntity.ok(roomService.setBotDeck(code, req.deckCardIds()));
  }

  @PostMapping("/{code}/start")
  public ResponseEntity<?> start(@PathVariable String code, @RequestBody StartRequest req) {
    RoomState existingRoom = roomService.get(code);
    ResponseEntity<String> authError = validateHost(existingRoom, code, req.playerId(), req.sessionToken());
    if (authError != null) return authError;
    RoomState room = roomService.start(code, req.playerId());
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    gameService.initGame(code.toUpperCase(), playerIds, decks, names, room.getGameMode());
    return ResponseEntity.ok(room);
  }

  @PostMapping("/bot-vs-bot")
  public ResponseEntity<Map<String, String>> createBotVsBot() {
    RoomState room = roomService.createBotVsBot();
    String code = room.getCode();
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    gameService.initGame(code, playerIds, decks, names, room.getGameMode());
    return ResponseEntity.ok(Map.of("code", code));
  }

  private ResponseEntity<String> validatePlayer(String code, String playerId, String sessionToken) {
    if (playerId == null || playerId.isBlank() || sessionToken == null || sessionToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Room action requires playerId and sessionToken.");
    }
    if (!roomTokenService.validate(sessionToken, code, playerId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or missing session token.");
    }
    return null;
  }

  private ResponseEntity<String> validateHost(RoomState room, String code, String playerId, String sessionToken) {
    ResponseEntity<String> playerError = validatePlayer(code, playerId, sessionToken);
    if (playerError != null) return playerError;
    if (!playerId.equals(room.getHostId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the room host can perform that action.");
    }
    return null;
  }
}
