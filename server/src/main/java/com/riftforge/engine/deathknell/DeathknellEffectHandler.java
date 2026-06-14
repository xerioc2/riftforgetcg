package com.riftforge.engine.deathknell;

import com.riftforge.engine.DeathEvent;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;

public interface DeathknellEffectHandler {
  boolean supports(CardDefinition card);

  void resolve(LiveGameState state, DeathEvent death, DeathknellEffectContext context);
}
