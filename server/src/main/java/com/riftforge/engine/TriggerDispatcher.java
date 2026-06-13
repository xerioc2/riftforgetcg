package com.riftforge.engine;

import com.riftforge.model.LiveGameState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TriggerDispatcher {
  private final List<TriggerHandler> handlers;

  public TriggerDispatcher(List<TriggerHandler> handlers) {
    this.handlers = new ArrayList<>(handlers);
    this.handlers.sort(Comparator.comparing(handler -> handler.getClass().getName()));
  }

  public void dispatch(LiveGameState state, TriggerEvent event) {
    for (TriggerHandler handler : handlers) {
      if (handler.supports(event)) handler.handle(state, event);
    }
  }
}
