package com.riftforge.matchmaking;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {
  private final MatchmakingService matchmakingService;

  public MatchmakingController(MatchmakingService matchmakingService) {
    this.matchmakingService = matchmakingService;
  }

  record LeaveRequest(String playerId) {}

  @PostMapping("/join")
  public ResponseEntity<Void> join(@RequestBody MatchmakingRequest request) {
    matchmakingService.enqueue(new MatchmakingEntry(request.playerId(), request.playerName(), request.deckCardIds()));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/leave")
  public ResponseEntity<Void> leave(@RequestBody LeaveRequest request) {
    matchmakingService.dequeue(request.playerId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status")
  public Map<String, Integer> status() {
    return Map.of("queueSize", matchmakingService.queueSize());
  }
}
