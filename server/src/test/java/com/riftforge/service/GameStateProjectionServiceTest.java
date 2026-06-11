package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RevealedHandSnapshot;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameStateProjectionServiceTest {
  private final GameStateProjectionService projectionService = new GameStateProjectionService(new LegalActionsService());

  @Test
  void playerSeesOwnHandCardIds() {
    LiveGameState state = state(handCard("own-hand", "p1", "irelia"));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getCards()).extracting(CardInstance::getCardId).containsExactly("irelia");
    assertThat(state.getCards()).extracting(CardInstance::getCardId).containsExactly("irelia");
  }

  @Test
  void playerDoesNotSeeOpponentHandCardIds() {
    LiveGameState state = state(handCard("opp-hand", "p2", "secret-card"));

    LiveGameState view = projectionService.toPublicView(state, "p1");
    CardInstance hidden = view.getCards().getFirst();

    assertThat(hidden.getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(hidden.getCurrentHealth()).isZero();
    assertThat(state.getCards().getFirst().getCardId()).isEqualTo("secret-card");
    assertThat(state.getCards().getFirst().getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void spectatorViewMasksAllHandCardIds() {
    LiveGameState state = state(
        handCard("p1-hand", "p1", "p1-card"),
        handCard("p2-hand", "p2", "p2-card"));

    LiveGameState view = projectionService.toPublicView(state, null);

    assertThat(view.getCards())
        .extracting(CardInstance::getCardId)
        .containsExactly(GameStateProjectionService.HIDDEN_CARD_ID, GameStateProjectionService.HIDDEN_CARD_ID);
  }

  @Test
  void cardsOutsideHandAreNotMasked() {
    LiveGameState state = state(card("battlefield-card", "p2", "visible-card", ZoneName.BATTLEFIELD));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getCards()).extracting(CardInstance::getCardId).containsExactly("visible-card");
  }

  @Test
  void playerSeesOwnVisionPeekLogEntry() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("vision", "p1", "VISION_PEEK|card-1|Card One"),
        log("normal", "p2", "Showdown started.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("VISION_PEEK|card-1|Card One", "Showdown started.");
  }

  @Test
  void playerDoesNotSeeOpponentVisionPeekLogEntry() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("vision", "p2", "VISION_PEEK|card-1|Card One"),
        log("normal", "p2", "Showdown started.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("Showdown started.");
  }

  @Test
  void nonVisionLogEntriesAreVisibleToAllPlayers() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("normal", "p2", "Played a card."),
        log("vision-resolved", "p2", "VISION_RESOLVED|Kept Card One on top of the deck.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("Played a card.");
  }

  @Test
  void playerOnlySeesRevealedHandSnapshotsAddressedToThem() {
    LiveGameState state = state();
    state.setRevealedHands(List.of(
        revealedHand("p1", "p2", "p2-card"),
        revealedHand("p2", "p1", "p1-card")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getRevealedHands()).hasSize(1);
    assertThat(view.getRevealedHands().getFirst().getRevealedToPlayerId()).isEqualTo("p1");
    assertThat(view.getRevealedHands().getFirst().getInstanceIds()).containsExactly("p2-card");
  }

  @Test
  void spectatorDoesNotSeeRevealedHandSnapshots() {
    LiveGameState state = state();
    state.setRevealedHands(List.of(revealedHand("p1", "p2", "p2-card")));

    LiveGameState view = projectionService.toPublicView(state, null);

    assertThat(view.getRevealedHands()).isEmpty();
  }

  @Test
  void activePlayerMainProjectionIncludesNormalActions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLegalActions())
        .contains(LegalAction.PLAY_CARD, LegalAction.MOVE_TO_BATTLEFIELD, LegalAction.REPOSITION_CARD, LegalAction.PASS_PHASE)
        .doesNotContain(LegalAction.SANDBOX_DEAL_CARD);
  }

  @Test
  void nonActivePlayerProjectionDoesNotIncludeNormalMainActions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);

    LiveGameState view = projectionService.toPublicView(state, "p2");

    assertThat(view.getLegalActions())
        .doesNotContain(LegalAction.PLAY_CARD, LegalAction.MOVE_TO_BATTLEFIELD, LegalAction.REPOSITION_CARD, LegalAction.PASS_PHASE);
  }

  @Test
  void spectatorProjectionHasNoLegalActions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);

    LiveGameState view = projectionService.toPublicView(state, null);

    assertThat(view.getLegalActions()).isEmpty();
  }

  @Test
  void activeShowdownProjectionOnlyIncludesResolveForAttacker() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLegalActions())
        .containsExactly(LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void activeShowdownDefenderGetsNoActions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));

    LiveGameState view = projectionService.toPublicView(state, "p2");

    assertThat(view.getLegalActions())
        .doesNotContain(
            LegalAction.RESOLVE_SHOWDOWN,
            LegalAction.PLAY_CARD,
            LegalAction.MOVE_TO_BATTLEFIELD,
            LegalAction.REPOSITION_CARD,
            LegalAction.PASS_PHASE,
            LegalAction.END_TURN)
        .isEmpty();
  }

  @Test
  void mulliganProjectionIncludesMulliganActionsForRelevantPlayer() {
    LiveGameState state = stateWithPlayers(Phase.MULLIGAN, "p1", GameMode.ENFORCED);

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLegalActions())
        .containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
  }

  @Test
  void sandboxProjectionIncludesSandboxActions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.SANDBOX);

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLegalActions())
        .contains(LegalAction.SANDBOX_DEAL_CARD, LegalAction.SANDBOX_ADJUST_SCORE, LegalAction.SANDBOX_MOVE_CARD);
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(List.of(cards));
    return state;
  }

  private LiveGameState stateWithPlayers(Phase phase, String activePlayerId, GameMode gameMode) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(phase);
    state.setActivePlayerId(activePlayerId);
    state.setGameMode(gameMode);
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>());
    state.setMulligansDone(new HashSet<>());
    return state;
  }

  private CardInstance handCard(String instanceId, String ownerId, String cardId) {
    return card(instanceId, ownerId, cardId, ZoneName.HAND);
  }

  private CardInstance card(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setOwnerId(ownerId);
    card.setCardId(cardId);
    card.setZone(zone);
    card.setCurrentHealth(3);
    return card;
  }

  private LiveGameState.LogEntry log(String id, String userId, String text) {
    return new LiveGameState.LogEntry(id, Instant.now().toString(), userId, text);
  }

  private RevealedHandSnapshot revealedHand(String toPlayerId, String ownerId, String... instanceIds) {
    RevealedHandSnapshot snapshot = new RevealedHandSnapshot();
    snapshot.setRevealedToPlayerId(toPlayerId);
    snapshot.setRevealedOwnerId(ownerId);
    snapshot.setInstanceIds(List.of(instanceIds));
    return snapshot;
  }
}
