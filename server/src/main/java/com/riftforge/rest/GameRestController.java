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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  public GameRestController(GameService gameService, CardDataService cardDataService, RoomService roomService, MatchHistoryService matchHistoryService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
    this.roomService = roomService;
    this.matchHistoryService = matchHistoryService;
  }

  @GetMapping("/game/{code}/state")
  public ResponseEntity<LiveGameState> state(@PathVariable String code, @RequestParam(required = false) String viewerPlayerId) {
    LiveGameState state = gameService.currentStateFor(code.toUpperCase(), viewerPlayerId);
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
  public ResponseEntity<Void> reset(@PathVariable String code) {
    RoomState room = roomService.get(code.toUpperCase());
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    gameService.reset(code.toUpperCase(), playerIds, decks, names, room.getGameMode());
    return ResponseEntity.ok().build();
  }
}
