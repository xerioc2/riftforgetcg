package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.testsupport.GameStackFixture;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegalActionsFlowIntegrationTest {
  private static final String ROOM = "ROOM";

  private final GameStackFixture fx = new GameStackFixture();
  private String active;
  private String idle;

  @BeforeEach
  void startGame() {
    List<String> deck = fx.registerConstructedDeck();
    fx.gameService.initGame(ROOM, List.of("p1", "p2"),
        Map.of("p1", deck, "p2", deck),
        Map.of("p1", "Player One", "p2", "Player Two"));
    LiveGameState state = fx.gameService.currentState(ROOM);
    active = state.getActivePlayerId();
    idle = "p1".equals(active) ? "p2" : "p1";
  }

  @Test
  void mulliganPhaseOffersKeepAndMulliganUntilEachPlayerDecides() {
    assertThat(legalActionsFor("p1")).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
    assertThat(legalActionsFor("p2")).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);

    fx.gameService.processMove(ROOM, new MulliganMove("p1", List.of()));

    assertThat(legalActionsFor("p1")).isEmpty();
    assertThat(legalActionsFor("p2")).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void spectatorProjectionNeverIncludesLegalActions() {
    assertThat(fx.gameService.currentStateFor(ROOM, null).getLegalActions()).isEmpty();

    completeMulligans();
    advanceToMain(active);

    assertThat(fx.gameService.currentStateFor(ROOM, null).getLegalActions()).isEmpty();
  }

  @Test
  void activePlayerInMainSeesNormalActionsAndNoSandboxActions() {
    completeMulligans();
    advanceToMain(active);

    Set<LegalAction> actions = legalActionsFor(active);
    assertThat(actions).contains(
        LegalAction.PLAY_CARD,
        LegalAction.MOVE_TO_BATTLEFIELD,
        LegalAction.PASS_PHASE,
        LegalAction.END_TURN,
        LegalAction.TAP_RUNE,
        LegalAction.DISCARD_RUNE);
    assertThat(actions).doesNotContain(
        LegalAction.SANDBOX_DEAL_CARD,
        LegalAction.SANDBOX_ADJUST_SCORE,
        LegalAction.SANDBOX_MOVE_CARD,
        LegalAction.SANDBOX_TAP_CARD,
        LegalAction.SANDBOX_FLIP_CARD);
    assertThat(legalActionsFor(idle)).doesNotContain(
        LegalAction.PLAY_CARD,
        LegalAction.MOVE_TO_BATTLEFIELD,
        LegalAction.PASS_PHASE,
        LegalAction.END_TURN);
  }

  @Test
  void contestedBattlefieldExposesOnlyResolveShowdownToTheAttacker() {
    completeMulligans();
    advanceToMain(active);

    fx.gameService.processMove(ROOM, new MoveToBattlefieldMove(active, championOf(active)));
    assertThat(fx.gameService.currentState(ROOM).getActiveShowdown()).isNull();

    endTurn(active);
    advanceToMain(idle);
    fx.gameService.processMove(ROOM, new MoveToBattlefieldMove(idle, championOf(idle)));

    LiveGameState state = fx.gameService.currentState(ROOM);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackingPlayerId()).isEqualTo(idle);
    assertThat(legalActionsFor(idle)).containsExactly(LegalAction.RESOLVE_SHOWDOWN);
    assertThat(legalActionsFor(active)).isEmpty();
    assertThat(fx.gameService.currentStateFor(ROOM, null).getLegalActions()).isEmpty();
  }

  private Set<LegalAction> legalActionsFor(String playerId) {
    return fx.gameService.currentStateFor(ROOM, playerId).getLegalActions();
  }

  private void completeMulligans() {
    fx.gameService.processMove(ROOM, new MulliganMove("p1", List.of()));
    fx.gameService.processMove(ROOM, new MulliganMove("p2", List.of()));
    assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(Phase.AWAKEN);
  }

  private void advanceToMain(String playerId) {
    for (Phase expected : List.of(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN)) {
      fx.gameService.processMove(ROOM, new PassPhaseMove(playerId));
      assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(expected);
    }
  }

  private void endTurn(String playerId) {
    fx.gameService.processMove(ROOM, new PassPhaseMove(playerId));
    assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(Phase.END);
    fx.gameService.processMove(ROOM, new PassPhaseMove(playerId));
    assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(Phase.AWAKEN);
  }

  private String championOf(String playerId) {
    return fx.gameService.currentState(ROOM).getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.CHAMPION)
        .findFirst()
        .orElseThrow()
        .getInstanceId();
  }
}
