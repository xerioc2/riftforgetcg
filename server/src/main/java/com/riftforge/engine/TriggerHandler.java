package com.riftforge.engine;

import com.riftforge.model.LiveGameState;

public interface TriggerHandler {
  boolean supports(TriggerEvent event);
  void handle(LiveGameState state, TriggerEvent event);
}
