package com.riftforge.rules;

import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import org.springframework.stereotype.Component;

@Component
public class ShowdownParticipantRules {
  public boolean isShowdownParticipant(LiveGameState state, String playerId) {
    return isShowdownAttacker(state, playerId) || isShowdownDefender(state, playerId);
  }

  public boolean isShowdownAttacker(LiveGameState state, String playerId) {
    return state != null
        && state.getActiveShowdown() != null
        && playerId != null
        && playerId.equals(state.getActiveShowdown().attackingPlayerId());
  }

  public boolean isShowdownDefender(LiveGameState state, String playerId) {
    return state != null
        && state.getActiveShowdown() != null
        && playerId != null
        && state.getCards().stream()
            .anyMatch(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.BATTLEFIELD);
  }
}
