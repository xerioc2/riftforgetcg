package com.riftforge.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.riftforge.engine.IllegalMoveException;
import com.riftforge.engine.RulesValidator;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.ResolveChainTopMove;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.model.move.TapCardMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegalActionsServiceTest {
  private final LegalActionsService service = new LegalActionsService();
  @Mock CardDataService cardDataService;

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
  void pendingChoiceExposesOnlyResolveChoiceToOwner() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw a card?"));

    assertThat(service.legalActions(state, "p1"))
        .containsExactly(LegalAction.RESOLVE_CHOICE);
    assertThat(service.legalActions(state, "p2"))
        .isEmpty();
  }

  @Test
  void activeChainExposesOnlyPassToFocusedPlayer() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(false, "p1"));

    assertThat(service.legalActions(state, "p1"))
        .containsExactly(LegalAction.PASS_CHAIN_FOCUS);
    assertThat(service.legalActions(state, "p2"))
        .isEmpty();
  }

  @Test
  void activeChainExposesOnlyResolveToFocusedPlayerWhenReady() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(true, "p1"));

    assertThat(service.legalActions(state, "p1"))
        .containsExactly(LegalAction.RESOLVE_CHAIN_TOP);
    assertThat(service.legalActions(state, "p2"))
        .isEmpty();
  }

  @Test
  void focusedPlayerWithDefySeesPlayCardOnlyWhenLegalCounterTargetExists() {
    when(cardDataService.getCard("defy")).thenReturn(new CardDefinition("defy", "Defy", "Spell", null, List.of(), 1, 0, null, null, null, "[Reaction] Counter a spell.", 0, 0, List.of()));
    when(cardDataService.getCard("source-card")).thenReturn(new CardDefinition("source-card", "Stacked Deck", "Spell", null, List.of(), 2, 0, null, null, null, "Draw 1.", 0, 0, List.of()));
    when(cardDataService.isReactionCard(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[reaction]");
    });
    when(cardDataService.isDefyCounterReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Defy".equalsIgnoreCase(def.name());
    });
    when(cardDataService.isUnsupportedAction("defy")).thenReturn(false);
    LegalActionsService legalActions = new LegalActionsService(cardDataService);
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(false, "p2"));
    state.getPlayers().stream().filter(player -> "p2".equals(player.getUserId())).findFirst().orElseThrow().setAvailableEnergy(3);
    state.getCards().add(card("defy-1", "p2", "defy", ZoneName.HAND));

    assertThat(legalActions.legalActionsFor(state, "p2"))
        .containsExactlyInAnyOrder(LegalAction.PASS_CHAIN_FOCUS, LegalAction.PLAY_CARD);
    assertThatCode(() -> validator.validate(state, new PlayCardMove("p2", "defy-1", ZoneName.BASE, 0, 0, null, "item-1", List.of(), false, List.of(), List.of())))
        .doesNotThrowAnyException();
    assertThat(legalActions.legalActionsFor(state, "p1")).isEmpty();
    assertThatThrownBy(() -> validator.validate(state, new PlayCardMove("p1", "defy-1", ZoneName.BASE, 0, 0, null, "item-1", List.of(), false, List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class);

    state.setChainState(nonCounterableChain(false, "p2"));
    assertThat(legalActions.legalActionsFor(state, "p2"))
        .containsExactly(LegalAction.PASS_CHAIN_FOCUS);
  }

  @Test
  void defyLegalActionsRespectCounterTargetCostLimits() {
    when(cardDataService.getCard("defy")).thenReturn(new CardDefinition("defy", "Defy", "Spell", null, List.of(), 1, 0, null, null, null, "[Reaction] Counter a spell.", 0, 0, List.of()));
    when(cardDataService.isReactionCard(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[reaction]");
    });
    when(cardDataService.isDefyCounterReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Defy".equalsIgnoreCase(def.name());
    });
    when(cardDataService.isUnsupportedAction("defy")).thenReturn(false);
    LegalActionsService legalActions = new LegalActionsService(cardDataService);
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());

    for (CardDefinition invalidTarget : List.of(
        new CardDefinition("source-card", "Expensive Spell", "Spell", null, List.of(), 5, 1, null, null, null, "Draw 1.", 0, 0, List.of()),
        new CardDefinition("source-card", "Premium Spell", "Spell", null, List.of(), 4, 2, null, null, null, "Draw 1.", 0, 0, List.of()))) {
      when(cardDataService.getCard("source-card")).thenReturn(invalidTarget);
      LiveGameState state = state(Phase.MAIN, "p1");
      state.setChainState(chain(false, "p2"));
      state.getPlayers().stream().filter(player -> "p2".equals(player.getUserId())).findFirst().orElseThrow().setAvailableEnergy(3);
      state.getCards().add(card("defy-1", "p2", "defy", ZoneName.HAND));

      assertThat(legalActions.legalActionsFor(state, "p2"))
          .containsExactly(LegalAction.PASS_CHAIN_FOCUS);
      assertThatThrownBy(() -> validator.validate(state, new PlayCardMove("p2", "defy-1", ZoneName.BASE, 0, 0, null, "item-1", List.of(), false, List.of(), List.of())))
          .isInstanceOf(IllegalMoveException.class)
          .hasMessage("Defy can only counter a spell that costs no more than 4 and no more than 1 power.");
    }
  }

  @Test
  void focusedPlayerWithNotSoFastSeesPlayCardOnlyForEnemySpellTargetingFriendlyUnit() {
    when(cardDataService.getCard("not-so-fast")).thenReturn(new CardDefinition("not-so-fast", "Not So Fast", "Spell", null, List.of(), 1, 0, null, null, null, "[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.", 0, 0, List.of()));
    when(cardDataService.getCard("gust")).thenReturn(new CardDefinition("gust", "Gust", "Spell", null, List.of(), 0, 0, null, null, null, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.", 0, 0, List.of()));
    when(cardDataService.isReactionCard(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[reaction]");
    });
    when(cardDataService.isNotSoFastCounterReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Not So Fast".equalsIgnoreCase(def.name());
    });
    when(cardDataService.isUnsupportedAction("not-so-fast")).thenReturn(false);
    LegalActionsService legalActions = new LegalActionsService(cardDataService);
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(gustTargetingChain("p2", false, "p2"));
    state.getPlayers().stream().filter(player -> "p2".equals(player.getUserId())).findFirst().orElseThrow().setAvailableEnergy(3);
    state.getCards().add(card("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));

    assertThat(legalActions.legalActionsFor(state, "p2"))
        .containsExactlyInAnyOrder(LegalAction.PASS_CHAIN_FOCUS, LegalAction.PLAY_CARD);
    assertThatCode(() -> validator.validate(state, new PlayCardMove("p2", "not-so-fast-1", ZoneName.BASE, 0, 0, null, "item-1", List.of(), false, List.of(), List.of())))
        .doesNotThrowAnyException();

    state.setChainState(gustTargetingChain("p1", false, "p2"));
    assertThat(legalActions.legalActionsFor(state, "p2"))
        .containsExactly(LegalAction.PASS_CHAIN_FOCUS);
    assertThatThrownBy(() -> validator.validate(state, new PlayCardMove("p2", "not-so-fast-1", ZoneName.BASE, 0, 0, null, "item-1", List.of(), false, List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Not So Fast can only counter an enemy spell that chooses a friendly Unit or Gear.");
  }

  @Test
  void validatorAndLegalActionsAgreeForChainPassAndResolve() {
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(false, "p1"));

    assertThat(service.legalActions(state, "p1")).containsExactly(LegalAction.PASS_CHAIN_FOCUS);
    assertThatCode(() -> validator.validate(state, new PassChainFocusMove("p1"))).doesNotThrowAnyException();
    assertThat(service.legalActions(state, "p2")).isEmpty();
    assertThatThrownBy(() -> validator.validate(state, new PassChainFocusMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass chain focus.");
    assertThatThrownBy(() -> validator.validate(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("All relevant players must pass before resolving the chain item.");

    state.setChainState(chain(true, "p1"));
    assertThat(service.legalActions(state, "p1")).containsExactly(LegalAction.RESOLVE_CHAIN_TOP);
    assertThatCode(() -> validator.validate(state, new ResolveChainTopMove("p1"))).doesNotThrowAnyException();
    assertThat(service.legalActions(state, "p2")).isEmpty();
    assertThatThrownBy(() -> validator.validate(state, new ResolveChainTopMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can resolve the chain item.");
  }

  @Test
  void pendingChoiceTakesPrecedenceOverChainLegalActionsAndValidation() {
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(true, "p1"));
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw a card?"));

    assertThat(service.legalActions(state, "p1")).containsExactly(LegalAction.RESOLVE_CHOICE);
    assertThat(service.legalActions(state, "p2")).isEmpty();
    assertThatThrownBy(() -> validator.validate(state, new PassChainFocusMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");
    assertThatThrownBy(() -> validator.validate(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");
  }

  @Test
  void activeChainExposesPlayCardForFocusedPlayerWithPlayableGust() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setChainState(chain(false, "p1"));
    state.getCards().add(card("gust", "p1", ZoneName.HAND));
    state.getCards().add(card("target", "p2", ZoneName.BATTLEFIELD));
    CardDefinition gust = new CardDefinition("gust", "Gust", "Spell", null, List.of(), 0, 0, null, null, null, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.", 0, 0, List.of());
    CardDefinition target = unitDef("target", 3, 3);
    when(cardDataService.getCard("gust")).thenReturn(gust);
    when(cardDataService.getCard("target")).thenReturn(target);
    when(cardDataService.isReactionCard(gust)).thenReturn(true);
    when(cardDataService.isGustReaction(gust)).thenReturn(true);
    when(cardDataService.isUnsupportedAction("gust")).thenReturn(false);

    assertThat(serviceWithCards.legalActions(state, "p1"))
        .containsExactlyInAnyOrder(LegalAction.PASS_CHAIN_FOCUS, LegalAction.PLAY_CARD);
    assertThatCode(() -> validator.validate(
        state,
        new PlayCardMove("p1", "gust", ZoneName.BASE, 0, 0, "target")))
        .doesNotThrowAnyException();
    assertThat(serviceWithCards.legalActions(state, "p2")).isEmpty();
    assertThatThrownBy(() -> validator.validate(
        state,
        new PlayCardMove("p2", "gust", ZoneName.BASE, 0, 0, "target")))
        .isInstanceOf(IllegalMoveException.class);
  }

  @Test
  void mainPhaseActivePlayerCanHideHiddenCardWithReadyRune() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.getCards().add(card("tideturner", "p1", ZoneName.HAND));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    CardDefinition hidden = cardDef("tideturner", "Unit", "[Hidden] Hide now.");
    when(cardDataService.getCard("tideturner")).thenReturn(hidden);
    when(cardDataService.isHiddenCard(hidden)).thenReturn(true);

    assertThat(serviceWithCards.legalActions(state, "p1"))
        .contains(LegalAction.HIDE_CARD);
  }

  @Test
  void hideCardIsNotExposedWithoutReadyRuneOrHiddenCard() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.getCards().add(card("unit", "p1", ZoneName.HAND));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", true))));
    CardDefinition unit = cardDef("unit", "Unit", "No Hidden.");
    when(cardDataService.getCard("unit")).thenReturn(unit);
    when(cardDataService.isHiddenCard(unit)).thenReturn(false);

    assertThat(serviceWithCards.legalActions(state, "p1"))
        .doesNotContain(LegalAction.HIDE_CARD);
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
        .containsExactly(LegalAction.PASS_SHOWDOWN_FOCUS);
  }

  @Test
  void activeShowdownAllowsAttackerResolutionOnlyAfterFocusPassesComplete() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(showdown("p1", true));

    assertThat(service.legalActions(state, "p1"))
        .containsExactly(LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void activeShowdownDamageAssignmentOnlyExposesAssigningPlayerAction() {
    LiveGameState state = state(Phase.MAIN, "p1");
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
        List.of(),
        List.of()));

    assertThat(service.legalActions(state, "p1")).isEmpty();
    assertThat(service.legalActions(state, "p2")).containsExactly(LegalAction.ASSIGN_COMBAT_DAMAGE);
  }

  @Test
  void activeShowdownAllowsSupportedActionCardForAttacker() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    state.getCards().add(card("action", "p1", ZoneName.HAND));
    CardDefinition action = cardDef("action", "Spell", "[Action] Move a friendly unit and ready it.");
    when(cardDataService.getCard("action")).thenReturn(action);
    when(cardDataService.isActionCard(action)).thenReturn(true);
    when(cardDataService.isReactionCard(action)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("action")).thenReturn(false);

    assertThat(serviceWithCards.legalActions(state, "p1"))
        .containsExactlyInAnyOrder(LegalAction.PASS_SHOWDOWN_FOCUS, LegalAction.PLAY_CARD);
  }

  @Test
  void activeShowdownAllowsSupportedActionCardForDefender() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(showdown("p2", false));
    state.getCards().add(card("defender-unit", "p2", ZoneName.BATTLEFIELD));
    state.getCards().add(card("action", "p2", ZoneName.HAND));
    CardDefinition action = cardDef("action", "Spell", "[Action] Move a friendly unit and ready it.");
    when(cardDataService.getCard("action")).thenReturn(action);
    when(cardDataService.isActionCard(action)).thenReturn(true);
    when(cardDataService.isReactionCard(action)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("action")).thenReturn(false);

    assertThat(serviceWithCards.legalActions(state, "p2"))
        .containsExactlyInAnyOrder(LegalAction.PASS_SHOWDOWN_FOCUS, LegalAction.PLAY_CARD);
  }

  @Test
  void activeShowdownDoesNotExposePlayCardForNonParticipant() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    state.getCards().add(card("action", "p2", ZoneName.HAND));

    assertThat(serviceWithCards.legalActions(state, "p2")).isEmpty();
  }

  @Test
  void validatorAndLegalActionsAgreeBystanderCannotPassOrPlayActionDuringShowdown() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    PlayerState p3 = new PlayerState();
    p3.setUserId("p3");
    state.getPlayers().add(p3);
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    state.getCards().add(card("action", "p3", ZoneName.HAND));
    CardDefinition action = cardDef("action", "Spell", "[Action] Draw 1.");
    when(cardDataService.getCard("action")).thenReturn(action);

    assertThat(serviceWithCards.legalActions(state, "p3"))
        .doesNotContain(LegalAction.PASS_SHOWDOWN_FOCUS, LegalAction.PLAY_CARD);
    assertThatThrownBy(() -> validator.validate(state, new PassShowdownFocusMove("p3")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass showdown focus.");
    assertThatThrownBy(() -> validator.validate(state, new PlayCardMove("p3", "action", ZoneName.BASE, 0, 0, null)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only showdown participants can play Action cards here.");
  }

  @Test
  void activeShowdownDoesNotExposePlayCardForUnsupportedAction() {
    LegalActionsService serviceWithCards = new LegalActionsService(cardDataService);
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    state.getCards().add(card("stacked-deck", "p1", ZoneName.HAND));
    CardDefinition action = cardDef("stacked-deck", "Spell", "[Action] Look at the top 3 cards of your Main Deck.");
    when(cardDataService.getCard("stacked-deck")).thenReturn(action);
    when(cardDataService.isActionCard(action)).thenReturn(true);
    when(cardDataService.isReactionCard(action)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("stacked-deck")).thenReturn(true);

    assertThat(serviceWithCards.legalActions(state, "p1"))
        .containsExactly(LegalAction.PASS_SHOWDOWN_FOCUS);
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

  @Test
  void sandboxModeDoesNotExposeSandboxActionsDuringActiveShowdown() {
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setGameMode(GameMode.SANDBOX);
    state.setActiveShowdown(showdown("p2", false));

    assertThat(service.legalActions(state, "p1"))
        .doesNotContain(
            LegalAction.SANDBOX_DEAL_CARD,
            LegalAction.SANDBOX_ADJUST_SCORE,
            LegalAction.SANDBOX_TAP_CARD,
            LegalAction.SANDBOX_FLIP_CARD,
            LegalAction.SANDBOX_MOVE_CARD,
            LegalAction.PASS_SHOWDOWN_FOCUS,
            LegalAction.RESOLVE_SHOWDOWN);
  }

  @Test
  void validatorAndLegalActionsAgreeForShowdownFocusPassAndResolve() {
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setActiveShowdown(showdown("p1", false));

    assertThat(service.legalActions(state, "p1")).contains(LegalAction.PASS_SHOWDOWN_FOCUS);
    assertThatCode(() -> validator.validate(state, new PassShowdownFocusMove("p1"))).doesNotThrowAnyException();

    assertThat(service.legalActions(state, "p2")).doesNotContain(LegalAction.PASS_SHOWDOWN_FOCUS);
    assertThatThrownBy(() -> validator.validate(state, new PassShowdownFocusMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass showdown focus.");

    assertThat(service.legalActions(state, "p1")).doesNotContain(LegalAction.RESOLVE_SHOWDOWN);
    assertThatThrownBy(() -> validator.validate(state, new ResolveShowdownMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Both players must pass showdown focus before resolving.");

    state.setActiveShowdown(showdown("p1", true));
    assertThat(service.legalActions(state, "p1")).containsExactly(LegalAction.RESOLVE_SHOWDOWN);
    assertThatCode(() -> validator.validate(state, new ResolveShowdownMove("p1"))).doesNotThrowAnyException();

    assertThat(service.legalActions(state, "p2")).doesNotContain(LegalAction.RESOLVE_SHOWDOWN);
    assertThatThrownBy(() -> validator.validate(state, new ResolveShowdownMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the attacking player can resolve this showdown.");
  }

  @Test
  void validatorAndLegalActionsAgreeForDamageAssignment() {
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.getCards().add(card("attacker", "p1", ZoneName.BATTLEFIELD));
    state.getCards().add(card("defender", "p2", ZoneName.BATTLEFIELD));
    when(cardDataService.getCard("attacker")).thenReturn(unitDef("attacker", 1, 1));
    when(cardDataService.getCard("defender")).thenReturn(unitDef("defender", 1, 1));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of("p1", "p2"),
        "p1",
        2,
        true,
        "p1",
        List.of(),
        List.of()));

    AssignCombatDamageMove legal = new AssignCombatDamageMove(
        "p1",
        List.of(new LiveGameState.CombatDamageAssignment("attacker", "defender", 1)));
    assertThat(service.legalActions(state, "p1")).containsExactly(LegalAction.ASSIGN_COMBAT_DAMAGE);
    assertThatCode(() -> validator.validate(state, legal)).doesNotThrowAnyException();

    assertThat(service.legalActions(state, "p2")).isEmpty();
    assertThatThrownBy(() -> validator.validate(state, new AssignCombatDamageMove(
        "p2",
        List.of(new LiveGameState.CombatDamageAssignment("defender", "attacker", 1)))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Wait for your combat damage assignment.");
  }

  @Test
  void validatorAndLegalActionsAgreeSandboxMovesAreBlockedDuringShowdown() {
    RulesValidator validator = new RulesValidator(cardDataService, new ShowdownParticipantRules());
    LiveGameState state = state(Phase.MAIN, "p1");
    state.setGameMode(GameMode.SANDBOX);
    state.getCards().add(card("unit", "p1", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(showdown("p1", false));

    assertThat(service.legalActions(state, "p1"))
        .doesNotContain(LegalAction.SANDBOX_TAP_CARD);
    assertThatThrownBy(() -> validator.validate(state, new TapCardMove("p1", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the active showdown first.");
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
    state.setCards(new ArrayList<>());
    state.setMulligansDone(new HashSet<>());
    return state;
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }

  private CardInstance card(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = card(instanceId, ownerId, zone);
    card.setCardId(cardId);
    return card;
  }

  private CardDefinition cardDef(String id, String type, String rulesText) {
    return new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, rulesText, 0, 0, List.of());
  }

  private CardDefinition unitDef(String id, int might, int health) {
    return new CardDefinition(id, id, "Unit", null, List.of(), 0, 0, null, null, null, null, might, health, List.of());
  }

  private LiveGameState.ShowdownState showdown(String focusedPlayerId, boolean readyToResolve) {
    return new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of("p1", "p2"),
        focusedPlayerId,
        readyToResolve ? 2 : 0,
        readyToResolve);
  }

  private LiveGameState.ChainState chain(boolean readyToResolve, String focusedPlayerId) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
            List.of(),
            1,
            "test chain item")),
        List.of("p1", "p2"),
        focusedPlayerId,
        readyToResolve ? 2 : 0,
        readyToResolve,
        "TEST");
  }

  private LiveGameState.ChainState gustTargetingChain(String targetControllerPlayerId, boolean readyToResolve, String focusedPlayerId) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "gust-1",
            "gust",
            "Gust",
            LiveGameState.ChainItem.EFFECT_GUST_RETURN,
            List.of("target-1"),
            1,
            "Gust",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            true,
            true,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND,
            List.of(new LiveGameState.ChainTarget(
                "target",
                "target-1",
                null,
                targetControllerPlayerId,
                "UNIT",
                ZoneName.BATTLEFIELD,
                "Target Unit",
                true)))),
        List.of("p1", "p2"),
        focusedPlayerId,
        readyToResolve ? 2 : 0,
        readyToResolve,
        "TEST");
  }

  private LiveGameState.ChainState nonCounterableChain(boolean readyToResolve, String focusedPlayerId) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
            List.of(),
            1,
            "test chain item",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            false,
            false,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND)),
        List.of("p1", "p2"),
        focusedPlayerId,
        readyToResolve ? 2 : 0,
        readyToResolve,
        "TEST");
  }

  private RuneState rune(String id, String ownerId, boolean tapped) {
    RuneState rune = new RuneState();
    rune.setInstanceId(id);
    rune.setCardId(id);
    rune.setOwnerId(ownerId);
    rune.setTapped(tapped);
    rune.setNormalEnergy(1);
    rune.setPremiumEnergy(2);
    return rune;
  }
}
