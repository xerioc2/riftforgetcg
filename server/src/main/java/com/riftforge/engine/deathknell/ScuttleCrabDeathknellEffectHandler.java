package com.riftforge.engine.deathknell;

import com.riftforge.engine.DeathEvent;
import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import org.springframework.stereotype.Component;

@Component
public class ScuttleCrabDeathknellEffectHandler implements DeathknellEffectHandler {
  @Override
  public boolean supports(CardDefinition card) {
    return card != null && card.name() != null && card.name().trim().equalsIgnoreCase("Scuttle Crab");
  }

  @Override
  public void resolve(LiveGameState state, DeathEvent death, DeathknellEffectContext context) {
    String opponentId = state.getPlayers().stream()
        .map(PlayerState::getUserId)
        .filter(playerId -> !playerId.equals(death.ownerId()))
        .findFirst()
        .orElse(null);
    if (opponentId == null) {
      context.log(state, death.ownerId(), "Scuttle Crab's Deathknell found no opponent to reveal.");
      return;
    }
    CardEffectRegistry.revealHandToViewer(state, death.ownerId(), opponentId);
    context.log(state, death.ownerId(), "Scuttle Crab's Deathknell revealed an opponent's hand. XP and facedown viewing are deferred in alpha.");
  }
}
