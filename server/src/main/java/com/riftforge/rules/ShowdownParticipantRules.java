package com.riftforge.rules;

import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShowdownParticipantRules {
  public boolean isShowdownParticipant(LiveGameState state, String playerId) {
    return relevantPlayerIds(state).contains(playerId);
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

  public boolean isFocusedPlayer(LiveGameState state, String playerId) {
    return state != null
        && state.getActiveShowdown() != null
        && playerId != null
        && playerId.equals(focusedPlayerId(state));
  }

  public String focusedPlayerId(LiveGameState state) {
    if (state == null || state.getActiveShowdown() == null) return null;
    String focused = state.getActiveShowdown().focusedPlayerId();
    if (focused != null && !focused.isBlank()) return focused;
    return state.getActiveShowdown().attackingPlayerId();
  }

  public List<String> relevantPlayerIds(LiveGameState state) {
    if (state == null || state.getActiveShowdown() == null) return List.of();
    List<String> fromState = state.getActiveShowdown().relevantPlayerIds();
    if (fromState != null && !fromState.isEmpty()) return fromState;
    List<String> relevant = new ArrayList<>();
    String attacker = state.getActiveShowdown().attackingPlayerId();
    if (attacker != null && !attacker.isBlank()) relevant.add(attacker);
    state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD)
        .map(card -> card.getOwnerId())
        .filter(owner -> owner != null && !owner.isBlank())
        .filter(owner -> !owner.equals(attacker))
        .distinct()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .forEach(relevant::add);
    return relevant;
  }

  public String nextFocusedPlayerId(LiveGameState state) {
    List<String> relevant = relevantPlayerIds(state);
    if (relevant.isEmpty()) return null;
    String focused = focusedPlayerId(state);
    int index = relevant.indexOf(focused);
    if (index < 0) return relevant.get(0);
    return relevant.get((index + 1) % relevant.size());
  }
}
