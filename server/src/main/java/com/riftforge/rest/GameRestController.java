package com.riftforge.rest;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameRestController {
  private final GameService gameService;
  private final CardDataService cardDataService;
  private final RoomService roomService;

  public GameRestController(GameService gameService, CardDataService cardDataService, RoomService roomService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
    this.roomService = roomService;
  }

  @GetMapping("/game/{code}/state")
  public ResponseEntity<LiveGameState> state(@PathVariable String code) {
    LiveGameState state = gameService.currentState(code.toUpperCase());
    return state == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(state);
  }

  @GetMapping("/cards")
  public Collection<CardDefinition> cards() {
    return cardDataService.getAll().values();
  }

  @PostMapping("/game/{code}/reset")
  public ResponseEntity<Void> reset(@PathVariable String code) {
    RoomState room = roomService.get(code.toUpperCase());
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();
    Map<String, List<String>> decks = room.getPlayers().stream().collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    gameService.reset(code.toUpperCase(), playerIds, decks);
    return ResponseEntity.ok().build();
  }
}
