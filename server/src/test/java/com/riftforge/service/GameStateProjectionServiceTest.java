package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RevealedHandSnapshot;
import com.riftforge.model.ShowdownStep;
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
  private final ObjectMapper objectMapper = new ObjectMapper();

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
  void publicBattlefieldCardProjectsDefaultLocationId() {
    LiveGameState state = state(card("battlefield-card", "p2", "visible-card", ZoneName.BATTLEFIELD));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    CardInstance projected = view.getCards().getFirst();
    assertThat(projected.getCardId()).isEqualTo("visible-card");
    assertThat(projected.getBattlefieldLocationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void faceDownBattlefieldCardDoesNotExposeLocationIdToOpponentOrSpectator() {
    CardInstance hidden = card("hidden-battlefield", "p2", "private-card", ZoneName.BATTLEFIELD);
    hidden.setFaceDown(true);
    LiveGameState state = state(hidden);

    LiveGameState opponentView = projectionService.toPublicView(state, "p1");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(opponentView.getCards().getFirst().getBattlefieldLocationId()).isNull();
    assertThat(spectatorView.getCards().getFirst().getBattlefieldLocationId()).isNull();
  }

  @Test
  void attachedPublicGearIsVisibleToOwnerOpponentAndSpectator() {
    CardInstance host = card("host", "p1", "host-unit", ZoneName.BATTLEFIELD);
    CardInstance gear = card("gear", "p1", "equip-gear", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(host, gear);

    LiveGameState ownerView = projectionService.toPublicView(state, "p1");
    LiveGameState opponentView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertAttachedGearIsPublic(ownerView);
    assertAttachedGearIsPublic(opponentView);
    assertAttachedGearIsPublic(spectatorView);
  }

  @Test
  void ownerSeesOwnHiddenCardIdentity() {
    LiveGameState state = state(card("own-hidden", "p1", "tideturner", ZoneName.HIDDEN));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    CardInstance hidden = view.getCards().getFirst();
    assertThat(hidden.getCardId()).isEqualTo("tideturner");
    assertThat(hidden.getZone()).isEqualTo(ZoneName.HIDDEN);
    assertThat(hidden.getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void opponentDoesNotSeeHiddenCardIdentity() {
    LiveGameState state = state(card("opp-hidden", "p2", "tideturner", ZoneName.HIDDEN));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    CardInstance hidden = view.getCards().getFirst();
    assertThat(hidden.getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(hidden.getZone()).isEqualTo(ZoneName.HIDDEN);
    assertThat(hidden.isFaceDown()).isTrue();
    assertThat(hidden.getCurrentHealth()).isZero();
    assertThat(state.getCards().getFirst().getCardId()).isEqualTo("tideturner");
  }

  @Test
  void spectatorDoesNotSeeHiddenCardIdentities() {
    LiveGameState state = state(
        card("p1-hidden", "p1", "tideturner", ZoneName.HIDDEN),
        card("p2-hidden", "p2", "facebreaker", ZoneName.HIDDEN));

    LiveGameState view = projectionService.toPublicView(state, null);

    assertThat(view.getCards())
        .extracting(CardInstance::getCardId)
        .containsExactly(GameStateProjectionService.HIDDEN_CARD_ID, GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(view.getCards()).allSatisfy(card -> {
      assertThat(card.isFaceDown()).isTrue();
      assertThat(card.getCurrentHealth()).isZero();
    });
  }

  @Test
  void serializedSpectatorProjectionContainsNoPrivateCardOrDeckIds() throws Exception {
    LiveGameState state = state(
        handCard("p1-hand", "p1", "p1-private-hand-card"),
        handCard("p2-hand", "p2", "p2-private-hand-card"),
        card("p1-hidden", "p1", "p1-private-hidden-card", ZoneName.HIDDEN),
        card("p2-hidden", "p2", "p2-private-hidden-card", ZoneName.HIDDEN),
        card("public-unit", "p2", "public-battlefield-unit", ZoneName.BATTLEFIELD),
        card("public-gear", "p2", "public-attached-gear", ZoneName.BASE));
    PlayerState p1 = player("p1");
    p1.setDeckPool(List.of("p1-private-deck-card"));
    p1.setRuneDeckPool(List.of("p1-private-rune-card"));
    p1.setSelectedBattlefields(List.of("p1-private-battlefield"));
    PlayerState p2 = player("p2");
    p2.setDeckPool(List.of("p2-private-deck-card"));
    state.setPlayers(List.of(p1, p2));
    state.setLog(List.of(
        log("p1-vision", "p1", "VISION_PEEK|p1-private-top-card|Secret Top"),
        log("p2-vision", "p2", "VISION_RESOLVED|Kept p2-private-top-card on top."),
        log("public", "p2", "Played public-battlefield-unit.")));

    String json = objectMapper.writeValueAsString(projectionService.toPublicView(state, null));

    assertThat(json).contains("public-battlefield-unit", "public-attached-gear", "\"deckCount\"");
    assertThat(json).doesNotContain(
        "p1-private-hand-card",
        "p2-private-hand-card",
        "p1-private-hidden-card",
        "p2-private-hidden-card",
        "p1-private-deck-card",
        "p2-private-deck-card",
        "p1-private-rune-card",
        "p1-private-battlefield",
        "p1-private-top-card",
        "p2-private-top-card",
        "deckPool",
        "runeDeckPool",
        "selectedBattlefields");
  }

  @Test
  void battlefieldChoicesAndSelectionsRemainPrivateDuringSelection() {
    LiveGameState state = stateWithPlayers(Phase.SELECT_BATTLEFIELD, "p1", GameMode.ENFORCED);
    PlayerState p1 = state.getPlayers().get(0);
    PlayerState p2 = state.getPlayers().get(1);
    p1.setSelectedBattlefields(List.of("p1-battlefield-a", "p1-battlefield-b", "p1-battlefield-c"));
    p1.setSelectedBattlefieldId("p1-battlefield-a");
    p2.setSelectedBattlefields(List.of("p2-battlefield-a", "p2-battlefield-b", "p2-battlefield-c"));
    p2.setSelectedBattlefieldId("p2-battlefield-a");

    LiveGameState p1View = projectionService.toPublicView(state, "p1");
    PlayerState own = p1View.getPlayers().stream().filter(player -> player.getUserId().equals("p1")).findFirst().orElseThrow();
    PlayerState opponent = p1View.getPlayers().stream().filter(player -> player.getUserId().equals("p2")).findFirst().orElseThrow();
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(own.getBattlefieldChoices()).containsExactly("p1-battlefield-a", "p1-battlefield-b", "p1-battlefield-c");
    assertThat(own.getSelectedBattlefieldId()).isEqualTo("p1-battlefield-a");
    assertThat(opponent.getBattlefieldChoices()).isEmpty();
    assertThat(opponent.getSelectedBattlefieldId()).isNull();
    assertThat(spectatorView.getPlayers()).allSatisfy(player -> {
      assertThat(player.getBattlefieldChoices()).isEmpty();
      assertThat(player.getSelectedBattlefieldId()).isNull();
    });
  }

  @Test
  void selectedBattlefieldsBecomePublicAfterSelectionCompletes() {
    LiveGameState state = stateWithPlayers(Phase.MULLIGAN, "p1", GameMode.ENFORCED);
    state.getPlayers().get(0).setSelectedBattlefields(List.of("p1-private-choice-a", "p1-private-choice-b"));
    state.getPlayers().get(0).setSelectedBattlefieldId("p1-selected-battlefield");
    state.getPlayers().get(1).setSelectedBattlefields(List.of("p2-private-choice-a", "p2-private-choice-b"));
    state.getPlayers().get(1).setSelectedBattlefieldId("p2-selected-battlefield");

    LiveGameState p1View = projectionService.toPublicView(state, "p1");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(p1View.getPlayers()).extracting(PlayerState::getSelectedBattlefieldId)
        .containsExactly("p1-selected-battlefield", "p2-selected-battlefield");
    assertThat(spectatorView.getPlayers()).extracting(PlayerState::getSelectedBattlefieldId)
        .containsExactly("p1-selected-battlefield", "p2-selected-battlefield");
    assertThat(spectatorView.getPlayers()).allSatisfy(player -> assertThat(player.getBattlefieldChoices()).isEmpty());
  }

  @Test
  void selectedBattlefieldIdsTransitionFromPrivateDuringSelectionToPublicAfterSetup() {
    LiveGameState state = stateWithPlayers(Phase.SELECT_BATTLEFIELD, "p1", GameMode.ENFORCED);
    state.getPlayers().get(0).setSelectedBattlefields(List.of("p1-choice-a", "p1-choice-b", "p1-choice-c"));
    state.getPlayers().get(0).setSelectedBattlefieldId("p1-selected-battlefield");
    state.getPlayers().get(1).setSelectedBattlefields(List.of("p2-choice-a", "p2-choice-b", "p2-choice-c"));
    state.getPlayers().get(1).setSelectedBattlefieldId("p2-selected-battlefield");

    LiveGameState p1SelectingView = projectionService.toPublicView(state, "p1");
    LiveGameState p2SelectingView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorSelectingView = projectionService.toPublicView(state, null);

    assertThat(playerView(p1SelectingView, "p1").getSelectedBattlefieldId()).isEqualTo("p1-selected-battlefield");
    assertThat(playerView(p1SelectingView, "p2").getSelectedBattlefieldId()).isNull();
    assertThat(playerView(p2SelectingView, "p1").getSelectedBattlefieldId()).isNull();
    assertThat(playerView(p2SelectingView, "p2").getSelectedBattlefieldId()).isEqualTo("p2-selected-battlefield");
    assertThat(spectatorSelectingView.getPlayers()).allSatisfy(player -> {
      assertThat(player.getSelectedBattlefieldId()).isNull();
      assertThat(player.getBattlefieldChoices()).isEmpty();
    });

    state.setCurrentPhase(Phase.MULLIGAN);
    LiveGameState p1PostSetupView = projectionService.toPublicView(state, "p1");
    LiveGameState spectatorPostSetupView = projectionService.toPublicView(state, null);

    assertThat(p1PostSetupView.getPlayers()).extracting(PlayerState::getSelectedBattlefieldId)
        .containsExactly("p1-selected-battlefield", "p2-selected-battlefield");
    assertThat(spectatorPostSetupView.getPlayers()).extracting(PlayerState::getSelectedBattlefieldId)
        .containsExactly("p1-selected-battlefield", "p2-selected-battlefield");
    assertThat(playerView(p1PostSetupView, "p2").getBattlefieldChoices()).isEmpty();
    assertThat(spectatorPostSetupView.getPlayers()).allSatisfy(player -> assertThat(player.getBattlefieldChoices()).isEmpty());
  }

  @Test
  void serializedPlayerProjectionShowsOwnPrivateCardsButNotOpponentPrivateCards() throws Exception {
    LiveGameState state = state(
        handCard("p1-hand", "p1", "p1-private-hand-card"),
        handCard("p2-hand", "p2", "p2-private-hand-card"),
        card("p1-hidden", "p1", "p1-private-hidden-card", ZoneName.HIDDEN),
        card("p2-hidden", "p2", "p2-private-hidden-card", ZoneName.HIDDEN));
    state.setPlayers(List.of(player("p1"), player("p2")));
    state.setLog(List.of(
        log("p1-vision", "p1", "VISION_PEEK|p1-private-top-card|Secret Top"),
        log("p2-vision", "p2", "VISION_PEEK|p2-private-top-card|Opponent Top")));

    String json = objectMapper.writeValueAsString(projectionService.toPublicView(state, "p1"));

    assertThat(json).contains("p1-private-hand-card", "p1-private-hidden-card", "p1-private-top-card");
    assertThat(json).doesNotContain("p2-private-hand-card", "p2-private-hidden-card", "p2-private-top-card");
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
  void scuttleCrabRevealDoesNotExposeOpponentHandToSpectatorOrWrongPlayer() throws Exception {
    LiveGameState state = state(
        handCard("p2-hand", "p2", "secret-card"));
    state.setRevealedHands(List.of(revealedHand("p1", "p2", "p2-hand")));

    LiveGameState controllerView = projectionService.toPublicView(state, "p1");
    LiveGameState ownerView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(controllerView.getRevealedHands()).singleElement()
        .satisfies(snapshot -> assertThat(snapshot.getInstanceIds()).containsExactly("p2-hand"));
    assertThat(objectMapper.writeValueAsString(controllerView)).doesNotContain("secret-card");
    assertThat(ownerView.getRevealedHands()).isEmpty();
    assertThat(spectatorView.getRevealedHands()).isEmpty();
    assertThat(objectMapper.writeValueAsString(spectatorView)).doesNotContain("secret-card");
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
  void activeShowdownProjectionOnlyIncludesPassFocusForFocusedAttacker() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLegalActions())
        .containsExactly(LegalAction.PASS_SHOWDOWN_FOCUS);
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
  void activeShowdownFocusStateSurvivesProjection() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of("attacker", 1),
        ShowdownStep.ACTION_WINDOW,
        List.of("p1", "p2"),
        "p2",
        1,
        false));

    LiveGameState view = projectionService.toPublicView(state, "p2");

    assertThat(view.getActiveShowdown()).isNotNull();
    assertThat(view.getActiveShowdown().relevantPlayerIds()).containsExactly("p1", "p2");
    assertThat(view.getActiveShowdown().focusedPlayerId()).isEqualTo("p2");
    assertThat(view.getActiveShowdown().consecutivePasses()).isEqualTo(1);
    assertThat(view.getActiveShowdown().readyToResolve()).isFalse();
    assertThat(view.getActiveShowdown().locationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void activeShowdownReadyToResolveSurvivesProjection() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of("p1", "p2"),
        "p1",
        2,
        true));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getActiveShowdown()).isNotNull();
    assertThat(view.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
    assertThat(view.getActiveShowdown().consecutivePasses()).isEqualTo(2);
    assertThat(view.getActiveShowdown().readyToResolve()).isTrue();
    assertThat(view.getLegalActions()).containsExactly(LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void activeShowdownDamageAssignmentStateSurvivesProjection() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    LiveGameState.CombatDamageAssignment assignment =
        new LiveGameState.CombatDamageAssignment("attacker", "defender", 2);
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of("p1", "p2"),
        "p1",
        2,
        true,
        "p2",
        List.of(assignment),
        List.of()));

    LiveGameState view = projectionService.toPublicView(state, "p2");

    assertThat(view.getActiveShowdown()).isNotNull();
    assertThat(view.getActiveShowdown().step()).isEqualTo(ShowdownStep.ASSIGN_DAMAGE);
    assertThat(view.getActiveShowdown().assigningPlayerId()).isEqualTo("p2");
    assertThat(view.getActiveShowdown().attackerAssignments()).containsExactly(assignment);
    assertThat(view.getLegalActions()).containsExactly(LegalAction.ASSIGN_COMBAT_DAMAGE);
  }

  @Test
  void activeChainStateSurvivesProjectionWithFocusedLegalActionsOnly() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
            List.of("target-1"),
            1,
            "public chain item")),
        List.of("p1", "p2"),
        "p2",
        1,
        false,
        "SHOWDOWN_ACTION"));

    LiveGameState focusedView = projectionService.toPublicView(state, "p2");
    LiveGameState otherView = projectionService.toPublicView(state, "p1");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(focusedView.getChainState()).isNotNull();
    assertThat(focusedView.getChainState().focusedPlayerId()).isEqualTo("p2");
    assertThat(focusedView.getChainState().chainItems()).singleElement()
        .satisfies(item -> {
          assertThat(item.publicDescription()).isEqualTo("public chain item");
          assertThat(item.targetInstanceIds()).containsExactly("target-1");
        });
    assertThat(focusedView.getLegalActions()).containsExactly(LegalAction.PASS_CHAIN_FOCUS);
    assertThat(otherView.getChainState()).isNotNull();
    assertThat(otherView.getLegalActions()).isEmpty();
    assertThat(spectatorView.getChainState()).isNotNull();
    assertThat(spectatorView.getLegalActions()).isEmpty();
  }

  @Test
  void publicChainItemShowsSafeSourceInfoToBothPlayersAndSpectator() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(chainState(new LiveGameState.ChainItem(
        "item-1",
        "p1",
        "source-1",
        "public-card",
        "Public Card",
        LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
        List.of("public-target"),
        1,
        "public card effect",
        LiveGameState.ChainItem.VISIBILITY_PUBLIC)));

    for (String viewerId : List.of("p1", "p2")) {
      LiveGameState.ChainItem item = projectionService.toPublicView(state, viewerId)
          .getChainState()
          .chainItems()
          .getFirst();
      assertThat(item.sourceCardInstanceId()).isEqualTo("source-1");
      assertThat(item.sourceCardId()).isEqualTo("public-card");
      assertThat(item.sourceCardName()).isEqualTo("Public Card");
      assertThat(item.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_NO_OP_TEST);
      assertThat(item.targetInstanceIds()).containsExactly("public-target");
      assertThat(item.status()).isEqualTo(LiveGameState.ChainItem.STATUS_PENDING);
      assertThat(item.counterable()).isTrue();
      assertThat(item.targetableOnChain()).isTrue();
    }

    LiveGameState.ChainItem spectatorItem = projectionService.toPublicView(state, null)
        .getChainState()
        .chainItems()
        .getFirst();
    assertThat(spectatorItem.sourceCardId()).isEqualTo("public-card");
    assertThat(spectatorItem.targetInstanceIds()).containsExactly("public-target");
  }

  @Test
  void publicChainTargetMetadataProjectsButPrivateTargetMetadataIsMasked() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(chainState(new LiveGameState.ChainItem(
        "item-1",
        "p1",
        "source-1",
        "public-card",
        "Public Card",
        LiveGameState.ChainItem.EFFECT_GUST_RETURN,
        List.of("public-target", "private-target"),
        1,
        "public card effect",
        LiveGameState.ChainItem.VISIBILITY_PUBLIC,
        LiveGameState.ChainItem.STATUS_PENDING,
        true,
        true,
        LiveGameState.ChainItem.TYPE_SPELL,
        ZoneName.HAND,
        List.of(
            new LiveGameState.ChainTarget("target", "public-target", null, "p2", "UNIT", ZoneName.BATTLEFIELD, "Public Unit", true),
            new LiveGameState.ChainTarget("target", "private-target", null, "p2", "UNIT", ZoneName.HIDDEN, "Private Unit", false)))));

    LiveGameState.ChainItem opponentItem = projectionService.toPublicView(state, "p2")
        .getChainState()
        .chainItems()
        .getFirst();
    LiveGameState.ChainItem controllerItem = projectionService.toPublicView(state, "p1")
        .getChainState()
        .chainItems()
        .getFirst();
    LiveGameState.ChainItem spectatorItem = projectionService.toPublicView(state, null)
        .getChainState()
        .chainItems()
        .getFirst();

    assertThat(opponentItem.chainTargets()).hasSize(2);
    assertThat(opponentItem.chainTargets().get(0).publicLabel()).isEqualTo("Public Unit");
    assertThat(opponentItem.chainTargets().get(0).targetInstanceId()).isEqualTo("public-target");
    assertThat(opponentItem.chainTargets().get(1).publicLabel()).isEqualTo("Hidden target");
    assertThat(opponentItem.chainTargets().get(1).targetInstanceId()).isNull();
    assertThat(opponentItem.chainTargets().get(1).targetControllerPlayerId()).isNull();
    assertThat(opponentItem.chainTargets().get(1).targetKind()).isEqualTo("MASKED");
    assertThat(opponentItem.chainTargets().get(1).targetZone()).isNull();
    assertThat(spectatorItem.chainTargets().get(1).publicLabel()).isEqualTo("Hidden target");
    assertThat(spectatorItem.chainTargets().get(1).targetInstanceId()).isNull();
    assertThat(spectatorItem.chainTargets().get(1).targetControllerPlayerId()).isNull();
    assertThat(spectatorItem.chainTargets().get(1).targetKind()).isEqualTo("MASKED");
    assertThat(spectatorItem.chainTargets().get(1).targetZone()).isNull();
    assertThat(controllerItem.chainTargets().get(1).publicLabel()).isEqualTo("Private Unit");
    assertThat(objectMapper.writeValueAsString(opponentItem)).doesNotContain("Private Unit", "private-target");
    assertThat(objectMapper.writeValueAsString(spectatorItem)).doesNotContain("Private Unit", "private-target");
  }

  @Test
  void controllerOnlyChainItemMasksSourceEffectAndTargetsFromOpponentAndSpectator() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(chainState(new LiveGameState.ChainItem(
        "item-1",
        "p1",
        "private-source-instance",
        "private-hidden-card",
        "Private Hidden Card",
        "PRIVATE_HIDDEN_EFFECT",
        List.of("private-hidden-target"),
        1,
        "A hidden effect",
        LiveGameState.ChainItem.VISIBILITY_CONTROLLER_ONLY)));

    LiveGameState controllerView = projectionService.toPublicView(state, "p1");
    LiveGameState opponentView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    LiveGameState.ChainItem controllerItem = controllerView.getChainState().chainItems().getFirst();
    assertThat(controllerItem.sourceCardInstanceId()).isEqualTo("private-source-instance");
    assertThat(controllerItem.sourceCardId()).isEqualTo("private-hidden-card");
    assertThat(controllerItem.sourceCardName()).isEqualTo("Private Hidden Card");
    assertThat(controllerItem.effectKey()).isEqualTo("PRIVATE_HIDDEN_EFFECT");
    assertThat(controllerItem.targetInstanceIds()).containsExactly("private-hidden-target");

    assertMaskedChainItem(opponentView.getChainState().chainItems().getFirst());
    assertMaskedChainItem(spectatorView.getChainState().chainItems().getFirst());
    assertThat(objectMapper.writeValueAsString(opponentView))
        .doesNotContain("private-source-instance", "private-hidden-card", "Private Hidden Card", "PRIVATE_HIDDEN_EFFECT", "private-hidden-target");
    assertThat(objectMapper.writeValueAsString(spectatorView))
        .doesNotContain("private-source-instance", "private-hidden-card", "Private Hidden Card", "PRIVATE_HIDDEN_EFFECT", "private-hidden-target");
  }

  @Test
  void maskedChainItemStatusProjectsWithoutPrivateCounterTargetMetadata() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(chainState(new LiveGameState.ChainItem(
        "item-1",
        "p1",
        "private-source-instance",
        "private-hidden-card",
        "Private Hidden Card",
        "PRIVATE_HIDDEN_EFFECT",
        List.of("private-hidden-target"),
        1,
        "A hidden effect",
        LiveGameState.ChainItem.VISIBILITY_CONTROLLER_ONLY,
        LiveGameState.ChainItem.STATUS_FIZZLED,
        true,
        true,
        LiveGameState.ChainItem.TYPE_SPELL,
        ZoneName.HIDDEN)));

    LiveGameState.ChainItem opponentItem = projectionService.toPublicView(state, "p2")
        .getChainState()
        .chainItems()
        .getFirst();

    assertMaskedChainItem(opponentItem);
    assertThat(opponentItem.status()).isEqualTo(LiveGameState.ChainItem.STATUS_FIZZLED);
    assertThat(opponentItem.counterable()).isFalse();
    assertThat(opponentItem.targetableOnChain()).isFalse();
    assertThat(opponentItem.chainItemType()).isEqualTo("MASKED");
    assertThat(opponentItem.sourceZoneBeforeChain()).isNull();
    assertThat(objectMapper.writeValueAsString(opponentItem))
        .doesNotContain("private-source-instance", "private-hidden-card", "Private Hidden Card", "PRIVATE_HIDDEN_EFFECT", "private-hidden-target", "HIDDEN");
  }

  @Test
  void publicChainItemMasksNonPublicSafeChainTargetsFromOpponentAndSpectator() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setChainState(chainState(new LiveGameState.ChainItem(
        "item-1",
        "p1",
        "p1-source",
        "p1-card",
        "P1 Card",
        LiveGameState.ChainItem.EFFECT_GUST_RETURN,
        List.of(),
        1,
        "Public chain item",
        LiveGameState.ChainItem.VISIBILITY_PUBLIC,
        LiveGameState.ChainItem.STATUS_PENDING,
        true,
        true,
        LiveGameState.ChainItem.TYPE_SPELL,
        ZoneName.HAND,
        List.of(
            new LiveGameState.ChainTarget("publicTarget", "public-unit", null, "p2", "UNIT", ZoneName.BATTLEFIELD, "Public Unit", true),
            new LiveGameState.ChainTarget("privateTarget", "secret-hand-card", null, "p1", "UNIT", ZoneName.HAND, "Secret Hand Card", false)))));

    LiveGameState.ChainItem controllerItem = projectionService.toPublicView(state, "p1").getChainState().chainItems().getFirst();
    LiveGameState.ChainItem opponentItem = projectionService.toPublicView(state, "p2").getChainState().chainItems().getFirst();
    LiveGameState.ChainItem spectatorItem = projectionService.toPublicView(state, null).getChainState().chainItems().getFirst();

    // Controller sees both of its own targets, including the non-public-safe one.
    assertThat(controllerItem.chainTargets()).hasSize(2);
    assertThat(controllerItem.chainTargets()).anySatisfy(target -> {
      assertThat(target.role()).isEqualTo("privateTarget");
      assertThat(target.targetInstanceId()).isEqualTo("secret-hand-card");
      assertThat(target.targetKind()).isEqualTo("UNIT");
      assertThat(target.targetControllerPlayerId()).isEqualTo("p1");
    });

    // Opponent sees the public-safe target but the non-public-safe target is masked.
    assertThat(opponentItem.chainTargets()).anySatisfy(target -> {
      assertThat(target.role()).isEqualTo("publicTarget");
      assertThat(target.targetInstanceId()).isEqualTo("public-unit");
      assertThat(target.targetKind()).isEqualTo("UNIT");
    });
    assertThat(opponentItem.chainTargets()).anySatisfy(target -> {
      assertThat(target.role()).isEqualTo("privateTarget");
      assertThat(target.targetInstanceId()).isNull();
      assertThat(target.targetChainItemId()).isNull();
      assertThat(target.targetControllerPlayerId()).isNull();
      assertThat(target.targetKind()).isEqualTo("MASKED");
      assertThat(target.targetZone()).isNull();
      assertThat(target.publicLabel()).isEqualTo("Hidden target");
    });

    // Spectator/public projection masks the non-public-safe target the same way.
    assertThat(spectatorItem.chainTargets()).anySatisfy(target -> {
      assertThat(target.role()).isEqualTo("privateTarget");
      assertThat(target.targetInstanceId()).isNull();
      assertThat(target.targetKind()).isEqualTo("MASKED");
    });

    // Serialized opponent/spectator projections never leak the private target id, name, or zone.
    assertThat(objectMapper.writeValueAsString(opponentItem)).doesNotContain("secret-hand-card", "Secret Hand Card");
    assertThat(objectMapper.writeValueAsString(spectatorItem)).doesNotContain("secret-hand-card", "Secret Hand Card");
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

  @Test
  void pendingChoiceOwnerProjectionSeesPromptAndResolveAction() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setPendingChoice(PendingChoice.optionalPayment(
        "choice-1",
        "p1",
        "safe-source",
        "Pay 2 to draw a card?",
        2,
        PendingChoice.EFFECT_DRAW_1));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getPendingChoice()).isNotNull();
    assertThat(view.getPendingChoice().getPrompt()).isEqualTo("Pay 2 to draw a card?");
    assertThat(view.getPendingChoice().getOptions()).extracting(PendingChoice.ChoiceOption::id)
        .containsExactly(PendingChoice.OPTION_PAY_1, PendingChoice.OPTION_DECLINE);
    assertThat(view.getPendingChoice().getPaymentAmount()).isEqualTo(2);
    assertThat(view.getPendingChoice().getEffect()).isEqualTo(PendingChoice.EFFECT_DRAW_1);
    assertThat(view.getLegalActions()).containsExactly(LegalAction.RESOLVE_CHOICE);
  }

  @Test
  void pendingChoiceOpponentAndSpectatorDoNotSeePrivatePrompt() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "private-source-card", "Private prompt with private-source-card"));

    LiveGameState opponentView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(opponentView.getPendingChoice()).isNull();
    assertThat(opponentView.getLegalActions()).isEmpty();
    assertThat(spectatorView.getPendingChoice()).isNull();
    assertThat(spectatorView.getLegalActions()).isEmpty();
    assertThat(objectMapper.writeValueAsString(opponentView)).doesNotContain("Private prompt", "private-source-card");
    assertThat(objectMapper.writeValueAsString(spectatorView)).doesNotContain("Private prompt", "private-source-card");
  }

  @Test
  void pendingCardChoiceOwnerSeesPrivateCardOptions() {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "p1",
        "stacked-deck",
        "source-instance",
        List.of(cardDef("private-top-a", "Private Top A"), cardDef("private-top-b", "Private Top B"))));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getPendingChoice()).isNotNull();
    assertThat(view.getPendingChoice().getCardOptions())
        .extracting(PendingChoice.CardChoiceOption::cardId)
        .containsExactly("private-top-a", "private-top-b");
    assertThat(view.getLegalActions()).containsExactly(LegalAction.RESOLVE_CHOICE);
  }

  @Test
  void pendingCardChoiceOpponentAndSpectatorDoNotSeePrivateCardOptions() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "p1", GameMode.ENFORCED);
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "p1",
        "private-stacked-deck",
        "source-instance",
        List.of(cardDef("private-top-a", "Private Top A"), cardDef("private-top-b", "Private Top B"))));

    LiveGameState opponentView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(opponentView.getPendingChoice()).isNull();
    assertThat(spectatorView.getPendingChoice()).isNull();
    assertThat(objectMapper.writeValueAsString(opponentView))
        .doesNotContain("private-top-a", "Private Top A", "private-top-b", "Private Top B", "private-stacked-deck");
    assertThat(objectMapper.writeValueAsString(spectatorView))
        .doesNotContain("private-top-a", "Private Top A", "private-top-b", "Private Top B", "private-stacked-deck");
  }

  @Test
  void botPendingCardChoiceDoesNotLeakToHumanProjection() throws Exception {
    LiveGameState state = stateWithPlayers(Phase.MAIN, "bot-player-riftbot", GameMode.ENFORCED);
    state.getPlayers().get(1).setUserId("bot-player-riftbot");
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "bot-player-riftbot",
        "bot-stacked-deck",
        "source-instance",
        List.of(cardDef("bot-private-top-a", "Bot Private Top A"), cardDef("bot-private-top-b", "Bot Private Top B"))));

    String json = objectMapper.writeValueAsString(projectionService.toPublicView(state, "p1"));

    assertThat(json)
        .doesNotContain("bot-private-top-a", "Bot Private Top A", "bot-private-top-b", "Bot Private Top B", "bot-stacked-deck");
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(List.of(cards));
    return state;
  }

  private LiveGameState stateWithPlayers(Phase phase, String activePlayerId, GameMode gameMode) {
    PlayerState p1 = player("p1");
    PlayerState p2 = player("p2");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(phase);
    state.setActivePlayerId(activePlayerId);
    state.setGameMode(gameMode);
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>());
    state.setMulligansDone(new HashSet<>());
    return state;
  }

  private PlayerState player(String playerId) {
    PlayerState player = new PlayerState();
    player.setUserId(playerId);
    return player;
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

  private CardDefinition cardDef(String id, String name) {
    return new CardDefinition(id, name, "Unit", null, List.of(), 0, 1, null, null, null, "Private rules text", 1, 1, List.of());
  }

  private PlayerState playerView(LiveGameState view, String playerId) {
    return view.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .orElseThrow();
  }

  private LiveGameState.ChainState chainState(LiveGameState.ChainItem item) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(item),
        List.of("p1", "p2"),
        "p1",
        0,
        false,
        "TEST");
  }

  private void assertAttachedGearIsPublic(LiveGameState view) {
    assertThat(view.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("gear");
      assertThat(card.getCardId()).isEqualTo("equip-gear");
      assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
      assertThat(card.getAttachedToInstanceId()).isEqualTo("host");
    });
  }

  private void assertMaskedChainItem(LiveGameState.ChainItem item) {
    assertThat(item.sourceCardInstanceId()).isNull();
    assertThat(item.sourceCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(item.sourceCardName()).isEqualTo("Hidden chain item");
    assertThat(item.effectKey()).isNull();
    assertThat(item.targetInstanceIds()).isEmpty();
    assertThat(item.publicDescription()).isEqualTo("A hidden effect");
    assertThat(item.counterable()).isFalse();
    assertThat(item.targetableOnChain()).isFalse();
    assertThat(item.chainItemType()).isEqualTo("MASKED");
    assertThat(item.sourceZoneBeforeChain()).isNull();
    assertThat(item.chainTargets()).allSatisfy(target -> {
      assertThat(target.publicLabel()).isEqualTo("Hidden target");
      assertThat(target.targetInstanceId()).isNull();
      assertThat(target.targetChainItemId()).isNull();
      assertThat(target.targetKind()).isEqualTo("MASKED");
    });
  }
}
