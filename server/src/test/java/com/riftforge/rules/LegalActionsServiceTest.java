package com.riftforge.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LegalActionsServiceTest {
  private final LegalActionsService service = new LegalActionsService();

  @Test
  void duringMulliganPlayerCanKeepOrMulligan() {
    assertThat(service.legalActions(state(Phase.MULLIGAN, "p1"), "p1"))
        .containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void completedMulliganHasNoMulliganActions() {
    LiveGameState state = state(Phase.MULLIGAN, "p1");
    state.setMulligansDone(new HashSet<>(Set.of("p1")));

    assertThat(service.legalActions(state, "p1"))
        .doesNotContain(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void earlyTurnPhasesDoNotExposeNormalMainActions() {
    for (Phase phase : List.of(Phase.AWAKEN, Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW)) {
      assertThat(service.legalActions(state(phase, "p1"), "p1"))
          .contains(LegalAction.PASS_PHASE)
          .doesNotContain(LegalAction.PLAY_CARD, LegalAction.MOVE_TO_BATTLEFIELD);
    }
  }

  @Test
  void mainPhaseActivePlayerCanUseNormalMainActions() {
    assertThat(service.legalActions(state(Phase.MAIN, "p1"), "p1"))
        .contains(
            LegalAction.PASS_PHASE,
            LegalAction.END_TURN,
            LegalAction.PLAY_CARD,
            LegalAction.MOVE_TO_BATTLEFIELD,
            LegalAction.REPOSITION_CARD,
            LegalAction.TAP_RUNE,
            LegalAction.DISCARD_RUNE,
            LegalAction.UNDO_RUNES);
  }

  @Test
  void nonActivePlayerCannotUseNormalMainActions() {
    assertThat(service.legalActions(state(Phase.MAIN, "p1"), "p2"))
        .doesNotContain(LegalAction.PLAY_CARD, LegalAction.MOVE_TO_BATTLEFIELD, LegalAction.REPOSITION_CARD, LegalAction.PASS_PHASE);
  }

  @Test
  void activeShowdownPausesNormalMainActionsAndAllowsResolution() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));

    assertThat(service.legalActions(state, "p1"))
        .containsExactly(LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void afterShowdownClearsNormalMainActionsReturn() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(null);

    assertThat(service.legalActions(state, "p1"))
        .contains(LegalAction.PLAY_CARD, LegalAction.MOVE_TO_BATTLEFIELD, LegalAction.REPOSITION_CARD)
        .doesNotContain(LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void enforcedModeDoesNotExposeSandboxActions() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setGameMode(GameMode.ENFORCED);

    assertThat(service.legalActions(state, "p1"))
        .doesNotContain(
            LegalAction.SANDBOX_DEAL_CARD,
            LegalAction.SANDBOX_ADJUST_SCORE,
            LegalAction.SANDBOX_TAP_CARD,
            LegalAction.SANDBOX_FLIP_CARD,
            LegalAction.SANDBOX_MOVE_CARD);
  }

  @Test
  void sandboxModeMayExposeSandboxActions() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setGameMode(GameMode.SANDBOX);

    assertThat(service.legalActions(state, "p1"))
        .contains(
            LegalAction.SANDBOX_DEAL_CARD,
            LegalAction.SANDBOX_ADJUST_SCORE,
            LegalAction.SANDBOX_TAP_CARD,
            LegalAction.SANDBOX_FLIP_CARD,
            LegalAction.SANDBOX_MOVE_CARD);
  }

  private LiveGameState state(Phase phase, String activePlayerId) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(phase);
    state.setActivePlayerId(activePlayerId);
    state.setGameMode(GameMode.ENFORCED);
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setMulligansDone(new HashSet<>());
    return state;
  }
}
