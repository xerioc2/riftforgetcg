package com.riftforge.rest;

import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.service.GameService;
import com.riftforge.service.RoomService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

  public RoomController(RoomService roomService, GameService gameService) {
    this.roomService = roomService;
    this.gameService = gameService;
  }

  record CreateRequest(String playerId, String playerName, boolean withBot) {}
  record JoinRequest(String playerId, String playerName) {}
  record ReadyRequest(String playerId, List<String> deckCardIds) {}
  record StartRequest(String playerId) {}
  record BotDeckRequest(List<String> deckCardIds) {}

  @PostMapping
  public RoomState create(@RequestBody CreateRequest req) {
    return roomService.create(req.playerId(), req.playerName(), req.withBot());
  }

  @GetMapping("/{code}")
  public RoomState get(@PathVariable String code) {
    return roomService.get(code);
  }

  @PostMapping("/{code}/join")
  public RoomState join(@PathVariable String code, @RequestBody JoinRequest req) {
    return roomService.join(code, req.playerId(), req.playerName());
  }

  @PostMapping("/{code}/ready")
  public RoomState ready(@PathVariable String code, @RequestBody ReadyRequest req) {
    return roomService.ready(code, req.playerId(), req.deckCardIds());
  }

  @PostMapping("/{code}/bot-deck")
  public RoomState setBotDeck(@PathVariable String code, @RequestBody BotDeckRequest req) {
    return roomService.setBotDeck(code, req.deckCardIds());
  }

  @PostMapping("/{code}/start")
  public ResponseEntity<RoomState> start(@PathVariable String code, @RequestBody StartRequest req) {
    RoomState room = roomService.start(code, req.playerId());
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    gameService.initGame(code.toUpperCase(), playerIds, decks);
    return ResponseEntity.ok(room);
  }

  @PostMapping("/bot-vs-bot")
  public ResponseEntity<Map<String, String>> createBotVsBot() {
    RoomState room = roomService.createBotVsBot();
    String code = room.getCode();
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    gameService.initGame(code, playerIds, decks);
    return ResponseEntity.ok(Map.of("code", code));
  }
}
