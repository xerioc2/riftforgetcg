package com.riftforge.websocket;

import com.riftforge.model.move.MoveRequest;
import com.riftforge.service.GameService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {
  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @MessageMapping("/game/{code}/move")
  public void handleMove(@DestinationVariable String code, @Payload MoveRequest move, Principal principal) {
    if (principal == null || !principal.getName().equals(move.playerId())) throw new SecurityException("Player ID mismatch");
    gameService.processMove(code.toUpperCase(), move);
  }

  @MessageMapping("/game/{code}/join")
  public void handleJoin(@DestinationVariable String code, Principal principal) {
    if (principal == null) throw new SecurityException("Missing principal");
    gameService.sendCurrentStateTo(code.toUpperCase(), principal.getName());
  }
}
