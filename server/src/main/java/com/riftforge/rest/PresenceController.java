package com.riftforge.rest;

import com.riftforge.matchmaking.MatchmakingService;
import com.riftforge.service.PresenceService;
import com.riftforge.service.PresenceService.PresenceSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PresenceController {
  private final PresenceService presenceService;
  private final MatchmakingService matchmakingService;

  public PresenceController(PresenceService presenceService, MatchmakingService matchmakingService) {
    this.presenceService = presenceService;
    this.matchmakingService = matchmakingService;
  }

  @GetMapping("/api/presence")
  public PresenceSummary presence() {
    return presenceService.summary(matchmakingService.queueSize());
  }
}
