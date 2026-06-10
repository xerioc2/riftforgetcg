package com.riftforge.rest;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.MatchRecord;
import com.riftforge.model.RoomState;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameRestController {
  private final GameService gameService;
  private final CardDataService cardDataService;
  private final RoomService roomService;
  private final MatchHistoryService matchHistoryService;
  private final RoomTokenService roomTokenService;

  public GameRestController(GameService gameService, CardDataService cardDataService, RoomService roomService, MatchHistoryService matchHistoryService, RoomTokenService roomTokenService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
    this.roomService = roomService;
    this.matchHistoryService = matchHistoryService;
    this.roomTokenService = roomTokenService;
  }

  @GetMapping("/game/{code}/state")
  public ResponseEntity<?> state(
      @PathVariable String code,
      @RequestParam(required = false) String playerId,
      @RequestParam(required = false) String sessionToken) {
    String roomCode = code.toUpperCase();
    boolean hasPlayerId = playerId != null && !playerId.isBlank();
    boolean hasToken = sessionToken != null && !sessionToken.isBlank();
    if (hasPlayerId != hasToken) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Player state requires both playerId and sessionToken.");
    }
    String viewerPlayerId = null;
    if (hasPlayerId) {
      if (!roomTokenService.validate(sessionToken, roomCode, playerId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or missing session token.");
      }
      viewerPlayerId = playerId;
    }
    LiveGameState state = gameService.currentStateFor(roomCode, viewerPlayerId);
    return state == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(state);
  }

  @GetMapping("/cards")
  public Collection<CardDefinition> cards() {
    return cardDataService.getAll().values();
  }

  @GetMapping("/matches")
  public List<MatchRecord> matches() {
    return matchHistoryService.getAll();
  }

  @PostMapping("/game/{code}/reset")
  public ResponseEntity<?> reset(@PathVariable String code, @RequestParam(required = false) String playerId, @RequestParam(required = false) String sessionToken) {
    String roomCode = code.toUpperCase();
    RoomState room = roomService.get(roomCode);
    ResponseEntity<String> authError = validateHost(room, roomCode, playerId, sessionToken);
    if (authError != null) return authError;
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    gameService.reset(roomCode, playerIds, decks, names, room.getGameMode());
    return ResponseEntity.ok().build();
  }

  private ResponseEntity<String> validateHost(RoomState room, String roomCode, String playerId, String sessionToken) {
    if (playerId == null || playerId.isBlank() || sessionToken == null || sessionToken.isBlank()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Host action requires playerId and sessionToken.");
    }
    if (!roomTokenService.validate(sessionToken, roomCode, playerId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or missing session token.");
    }
    if (!playerId.equals(room.getHostId())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the room host can perform that action.");
    }
    return null;
  }
}
