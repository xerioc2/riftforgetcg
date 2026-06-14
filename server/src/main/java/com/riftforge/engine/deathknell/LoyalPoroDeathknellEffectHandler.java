package com.riftforge.engine.deathknell;

import com.riftforge.engine.DeathEvent;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import org.springframework.stereotype.Component;

@Component
public class LoyalPoroDeathknellEffectHandler implements DeathknellEffectHandler {
  @Override
  public boolean supports(CardDefinition card) {
    return card != null && card.name() != null && card.name().trim().equalsIgnoreCase("Loyal Poro");
  }

  @Override
  public void resolve(LiveGameState state, DeathEvent death, DeathknellEffectContext context) {
    if (!death.hadOtherFriendlyUnitAtLocation()) {
      context.log(state, death.ownerId(), "Loyal Poro died alone. Deathknell did not draw.");
      return;
    }
    context.autoDraw(state, death.ownerId(), playerName(state, death.ownerId()) + "'s deck is empty - no Deathknell draw.");
    context.log(state, death.ownerId(), "Loyal Poro's Deathknell drew 1.");
  }

  private String playerName(LiveGameState state, String playerId) {
    return state.getPlayers().stream()
        .filter(player -> player.getUserId().equals(playerId))
        .map(player -> player.getName() == null ? playerId : player.getName())
        .findFirst()
        .orElse(playerId);
  }
}
