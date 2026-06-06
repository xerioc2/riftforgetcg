package com.riftforge.bot;

import com.riftforge.model.LiveGameState;
import org.springframework.context.ApplicationEvent;

public class GameStateChangedEvent extends ApplicationEvent {
  private final String roomCode;
  private final LiveGameState state;

  public GameStateChangedEvent(Object source, String roomCode, LiveGameState state) {
    super(source);
    this.roomCode = roomCode;
    this.state = state;
  }

  public String getRoomCode() { return roomCode; }
  public LiveGameState getState() { return state; }
}
