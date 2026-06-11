package com.riftforge.rules;

import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalActionsService {
  public Set<LegalAction> legalActionsFor(LiveGameState state, String playerId) {
    return legalActions(state, playerId);
  }

  public Set<LegalAction> legalActions(LiveGameState state, String playerId) {
    if (state == null || playerId == null || state.getWinnerId() != null || !isPlayer(state, playerId)) {
      return Set.of();
    }

    EnumSet<LegalAction> actions = EnumSet.noneOf(LegalAction.class);
    addSandboxActions(state, actions);

    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      if (!state.getMulligansDone().contains(playerId)) {
        actions.add(LegalAction.KEEP_HAND);
        actions.add(LegalAction.MULLIGAN);
      }
      return actions;
    }

    if (state.getActiveShowdown() != null) {
      if (playerId.equals(state.getActiveShowdown().attackingPlayerId())) {
        actions.add(LegalAction.RESOLVE_SHOWDOWN);
      }
      return actions;
    }

    if (!playerId.equals(state.getActivePlayerId())) {
      // TODO: Add Reaction/Action windows once the chain and timing permissions are modeled.
      return actions;
    }

    if (state.getCurrentPhase() == Phase.MAIN) {
      actions.add(LegalAction.PASS_PHASE);
      actions.add(LegalAction.END_TURN);
      actions.add(LegalAction.PLAY_CARD);
      actions.add(LegalAction.MOVE_TO_BATTLEFIELD);
      actions.add(LegalAction.REPOSITION_CARD);
      actions.add(LegalAction.TAP_RUNE);
      actions.add(LegalAction.DISCARD_RUNE);
      actions.add(LegalAction.UNDO_RUNES);
      actions.add(LegalAction.VISION_CHOICE);
      return actions;
    }

    actions.add(LegalAction.PASS_PHASE);
    if (state.getCurrentPhase() == Phase.END) actions.add(LegalAction.END_TURN);
    return actions;
  }

  private boolean isPlayer(LiveGameState state, String playerId) {
    return state.getPlayers().stream().anyMatch(player -> playerId.equals(player.getUserId()));
  }

  private void addSandboxActions(LiveGameState state, EnumSet<LegalAction> actions) {
    if (state.getGameMode() != GameMode.SANDBOX) return;
    actions.add(LegalAction.SANDBOX_DEAL_CARD);
    actions.add(LegalAction.SANDBOX_ADJUST_SCORE);
    actions.add(LegalAction.SANDBOX_TAP_CARD);
    actions.add(LegalAction.SANDBOX_FLIP_CARD);
    actions.add(LegalAction.SANDBOX_MOVE_CARD);
  }
}
