package com.riftforge.service;

import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

import com.riftforge.engine.GameEngine;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ChainFastPassService {
  private final LegalActionsService legalActionsService;

  public ChainFastPassService(LegalActionsService legalActionsService) {
    this.legalActionsService = legalActionsService;
  }

  public boolean shouldAutoPass(LiveGameState state) {
    if (state == null || state.getPendingChoice() != null) return false;
    LiveGameState.ChainState chain = state.getChainState();
    if (chain == null || chain.topItem() == null || chain.readyToResolveTop()) return false;
    String focusedPlayerId = chain.focusedPlayerId();
    if (focusedPlayerId == null || focusedPlayerId.isBlank()) return false;
    if (!ALL_BOT_IDS.contains(focusedPlayerId)) return false;
    Set<LegalAction> actions = legalActionsService.legalActionsFor(state, focusedPlayerId);
    return actions.size() == 1 && actions.contains(LegalAction.PASS_CHAIN_FOCUS);
  }

  public LiveGameState autoPassSafeWindows(GameEngine engine, LiveGameState state) {
    if (engine == null || state == null) return state;
    int guard = Math.max(2, state.getPlayers().size() * Math.max(1, chainItemCount(state)) + 2);
    LiveGameState next = state;
    while (guard-- > 0 && shouldAutoPass(next)) {
      next = engine.applyMove(next, new PassChainFocusMove(next.getChainState().focusedPlayerId()));
    }
    return next;
  }

  private int chainItemCount(LiveGameState state) {
    return state.getChainState() == null || state.getChainState().chainItems() == null
        ? 0
        : state.getChainState().chainItems().size();
  }
}
