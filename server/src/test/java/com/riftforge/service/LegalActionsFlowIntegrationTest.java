package com.riftforge.service;

import static com.riftforge.bot.BotConstants.BOT_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.SelectBattlefieldMove;
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
  void battlefieldSelectionPrecedesMulliganAndIsOwnerProjected() {
    assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(Phase.SELECT_BATTLEFIELD);
    assertThat(legalActionsFor("p1")).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(legalActionsFor("p2")).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(fx.gameService.currentStateFor(ROOM, "p1").getPlayers().stream()
        .filter(player -> "p1".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getBattlefieldChoices()).containsExactly("battlefield-0", "battlefield-1", "battlefield-2");
    assertThat(fx.gameService.currentStateFor(ROOM, "p1").getPlayers().stream()
        .filter(player -> "p2".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getBattlefieldChoices()).isEmpty();

    fx.gameService.processMove(ROOM, new SelectBattlefieldMove("p1", "battlefield-0"));

    assertThat(legalActionsFor("p1")).isEmpty();
    assertThat(legalActionsFor("p2")).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(fx.gameService.currentStateFor(ROOM, "p2").getPlayers().stream()
        .filter(player -> "p1".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getSelectedBattlefieldId()).isNull();

    fx.gameService.processMove(ROOM, new SelectBattlefieldMove("p2", "battlefield-1"));

    assertThat(fx.gameService.currentStateFor(ROOM, "p2").getPlayers().stream()
        .filter(player -> "p1".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getSelectedBattlefieldId()).isEqualTo("battlefield-0");
  }

  @Test
  void humanVsRiftBotBattlefieldSelectionDoesNotSkipHumanPrompt() {
    String room = "BOTROOM";
    List<String> deck = fx.registerConstructedDeck();
    fx.gameService.initGame(room, List.of("human", BOT_ID),
        Map.of("human", deck, BOT_ID, deck),
        Map.of("human", "Human", BOT_ID, "RiftBot"));

    fx.gameService.processMove(room, new SelectBattlefieldMove(BOT_ID, "battlefield-0"));

    LiveGameState humanView = fx.gameService.currentStateFor(room, "human");
    assertThat(humanView.getCurrentPhase()).isEqualTo(Phase.SELECT_BATTLEFIELD);
    assertThat(humanView.getLegalActions()).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(humanView.getPlayers().stream()
        .filter(player -> "human".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getBattlefieldChoices()).containsExactly("battlefield-0", "battlefield-1", "battlefield-2");
    assertThat(humanView.getLegalActions()).doesNotContain(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void invalidBattlefieldSelectionDoesNotMutateState() {
    fx.gameService.processMove(ROOM, new SelectBattlefieldMove("p1", "battlefield-9"));

    LiveGameState state = fx.gameService.currentState(ROOM);
    assertThat(state.getCurrentPhase()).isEqualTo(Phase.SELECT_BATTLEFIELD);
    assertThat(state.getPlayers().stream()
        .filter(player -> "p1".equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getSelectedBattlefieldId()).isNull();
    assertThat(legalActionsFor("p1")).containsExactly(LegalAction.SELECT_BATTLEFIELD);
  }

  @Test
  void decksWithoutBattlefieldChoicesSkipSafelyToMulligan() {
    String room = "NOFIELD";
    List<String> deck = registerNoBattlefieldDeck();

    fx.gameService.initGame(room, List.of("p1", "p2"),
        Map.of("p1", deck, "p2", deck),
        Map.of("p1", "Player One", "p2", "Player Two"));

    assertThat(fx.gameService.currentState(room).getCurrentPhase()).isEqualTo(Phase.MULLIGAN);
    assertThat(fx.gameService.currentStateFor(room, "p1").getLegalActions())
        .containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void mulliganPhaseOffersKeepAndMulliganUntilEachPlayerDecides() {
    completeBattlefieldSelection();

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
  void contestedBattlefieldExposesPassFocusToTheFocusedAttacker() {
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
    assertThat(legalActionsFor(idle)).containsExactly(LegalAction.PASS_SHOWDOWN_FOCUS);
    assertThat(legalActionsFor(active)).isEmpty();
    assertThat(fx.gameService.currentStateFor(ROOM, null).getLegalActions()).isEmpty();
  }

  @Test
  void fixtureLegalActionsUseRealSupportedActionDetection() {
    completeMulligans();
    advanceToMain(active);
    LiveGameState state = fx.gameService.currentState(ROOM);
    fx.addCard("action-draw", "Action Draw", "Spell", "[Action] Draw 1.", List.of());
    state.getCards().add(card("action-instance", "action-draw", idle, ZoneName.HAND));
    state.getCards().add(card("idle-battlefield", "unit-0", idle, ZoneName.BATTLEFIELD));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        active,
        List.of(championOf(active)),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of(active, idle),
        idle,
        0,
        false));

    assertThat(legalActionsFor(idle)).containsExactlyInAnyOrder(LegalAction.PASS_SHOWDOWN_FOCUS, LegalAction.PLAY_CARD);
    assertThat(fx.gameService.currentStateFor(ROOM, null).getLegalActions()).isEmpty();
  }

  @Test
  void stackedDeckWithNoResponseFastPassesToReadyThroughProcessMove() {
    completeMulligans();
    advanceToMain(active);
    fx.addCard("stacked-deck", "Stacked Deck", "Spell",
        "Look at the top 3 cards of your deck. Put 1 into your hand and recycle the rest.", List.of());
    fx.gameService.currentState(ROOM).getCards().add(card("stacked-instance", "stacked-deck", active, ZoneName.HAND));

    fx.gameService.processMove(ROOM, new PlayCardMove(active, "stacked-instance", ZoneName.BASE, 0, 0, null));

    LiveGameState afterPlay = fx.gameService.currentState(ROOM);
    // Neither player can respond, so the dead windows auto-pass server-side and the chain becomes
    // ready to resolve with focus back on the opener's controller -- no manual passes required.
    assertThat(afterPlay.getChainState()).isNotNull();
    assertThat(afterPlay.getChainState().readyToResolveTop()).isTrue();
    assertThat(afterPlay.getChainState().focusedPlayerId()).isEqualTo(active);
    assertThat(afterPlay.getPendingChoice()).isNull();
    assertThat(legalActionsFor(active)).containsExactly(LegalAction.RESOLVE_CHAIN_TOP);
  }

  @Test
  void stackedDeckDoesNotFastPassPastFocusedGustResponderThroughProcessMove() {
    completeMulligans();
    advanceToMain(active);
    fx.addCard("stacked-deck", "Stacked Deck", "Spell",
        "Look at the top 3 cards of your deck. Put 1 into your hand and recycle the rest.", List.of());
    fx.addCard("gust", "Gust", "Spell",
        "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.", List.of());
    LiveGameState state = fx.gameService.currentState(ROOM);
    state.getCards().add(card("stacked-instance", "stacked-deck", active, ZoneName.HAND));
    state.getCards().add(card("gust-instance", "gust", idle, ZoneName.HAND));
    state.getCards().add(card("idle-unit", "unit-1", idle, ZoneName.BATTLEFIELD));

    fx.gameService.processMove(ROOM, new PlayCardMove(active, "stacked-instance", ZoneName.BASE, 0, 0, null));

    LiveGameState afterPlay = fx.gameService.currentState(ROOM);
    // Fast-pass must stop at the responder because they hold a legal Gust response.
    assertThat(afterPlay.getChainState()).isNotNull();
    assertThat(afterPlay.getChainState().readyToResolveTop()).isFalse();
    assertThat(afterPlay.getChainState().focusedPlayerId()).isEqualTo(idle);
    assertThat(legalActionsFor(idle)).contains(LegalAction.PLAY_CARD, LegalAction.PASS_CHAIN_FOCUS);
  }

  private Set<LegalAction> legalActionsFor(String playerId) {
    return fx.gameService.currentStateFor(ROOM, playerId).getLegalActions();
  }

  private void completeBattlefieldSelection() {
    if (fx.gameService.currentState(ROOM).getCurrentPhase() != Phase.SELECT_BATTLEFIELD) return;
    fx.gameService.processMove(ROOM, new SelectBattlefieldMove("p1", "battlefield-0"));
    fx.gameService.processMove(ROOM, new SelectBattlefieldMove("p2", "battlefield-1"));
    assertThat(fx.gameService.currentState(ROOM).getCurrentPhase()).isEqualTo(Phase.MULLIGAN);
  }

  private List<String> registerNoBattlefieldDeck() {
    List<String> deck = new java.util.ArrayList<>();
    fx.addCard("no-field-legend", "No Field Legend", "Legend");
    fx.addCard("no-field-champion", "No Field Champion", "Champion");
    deck.add("no-field-legend");
    deck.add("no-field-champion");
    for (int i = 0; i < 8; i++) {
      fx.addCard("no-field-unit-" + i, "No Field Unit " + i, "Unit");
      deck.add("no-field-unit-" + i);
    }
    return deck;
  }

  private void completeMulligans() {
    completeBattlefieldSelection();
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

  private CardInstance card(String instanceId, String cardId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(1);
    return card;
  }
}
