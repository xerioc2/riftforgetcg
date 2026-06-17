package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardInstance;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.ResolveChoiceMove;
import com.riftforge.model.move.ResolveChainTopMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameStateProjectionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameEngineChainTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  @Mock CardZoneService cardZoneService;
  GameEngine engine;
  Map<String, CardDefinition> definitions;

  @BeforeEach
  void setUp() {
    definitions = new HashMap<>();
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    engine = new GameEngine(new RulesValidator(cardDataService), combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> card(invocation.getArgument(0)));
    when(cardDataService.isReactionCard(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[reaction]");
    });
    when(cardDataService.isGustReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Gust".equalsIgnoreCase(def.name()) && text.contains("[reaction]") && text.contains("return a unit") && text.contains("battlefield");
    });
    when(cardDataService.isDisciplineReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Discipline".equalsIgnoreCase(def.name()) && text.contains("[reaction]") && text.contains("give a unit") && text.contains("+2") && text.contains("draw 1");
    });
    when(cardDataService.isEnGardeReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "En Garde".equalsIgnoreCase(def.name()) && text.contains("[reaction]") && text.contains("friendly unit") && text.contains("additional +1");
    });
    when(cardDataService.isDefyCounterReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Defy".equalsIgnoreCase(def.name()) && text.contains("[reaction]") && text.contains("counter a spell");
    });
    when(cardDataService.isNotSoFastCounterReaction(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Not So Fast".equalsIgnoreCase(def.name())
          && text.contains("[reaction]")
          && text.contains("counter an enemy spell or ability")
          && text.contains("friendly unit or gear");
    });
    when(cardDataService.isStackedDeckEffect(org.mockito.ArgumentMatchers.any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Stacked Deck".equalsIgnoreCase(def.name())
          && text.contains("look at the top 3")
          && text.contains("put 1")
          && text.contains("hand")
          && text.contains("recycle");
    });
    doAnswer(invocation -> {
      CardInstance card = invocation.getArgument(0);
      card.setZone(ZoneName.DISCARD);
      return null;
    }).when(cardZoneService).moveToGraveyard(any(CardInstance.class));
  }

  @Test
  void passChainFocusCyclesAndMarksTopReadyAfterAllRelevantPlayersPass() {
    LiveGameState state = state(chain(false, "p1"));

    engine.applyMove(state, new PassChainFocusMove("p1"));

    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getChainState().consecutivePasses()).isEqualTo(1);
    assertThat(state.getChainState().readyToResolveTop()).isFalse();

    engine.applyMove(state, new PassChainFocusMove("p2"));

    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().consecutivePasses()).isEqualTo(2);
    assertThat(state.getChainState().readyToResolveTop()).isTrue();
  }

  @Test
  void nonFocusedPlayerCannotPassChainAndStateDoesNotMutate() {
    LiveGameState state = state(chain(false, "p1"));

    assertThatThrownBy(() -> engine.applyMove(state, new PassChainFocusMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass chain focus.");

    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().consecutivePasses()).isZero();
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
  }

  @Test
  void resolveChainTopDrawsPrivatelyAndClearsSingleItemChain() {
    LiveGameState state = state(chain(true, "p1"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));

    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getChainState()).isNull();
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p1");
      assertThat(card.getCardId()).isEqualTo("drawn-card");
    });
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Drawn Card"));
  }

  @Test
  void multiItemChainResolvesLastItemFirstAndResetsFocusForNextTop() {
    LiveGameState state = state(multiItemChain(true, "p2"));
    player(state, "p2").setDeckPool(new ArrayList<>(List.of("drawn-card")));

    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::itemId)
        .containsExactly("item-1");
    assertThat(state.getChainState().topItem().controllerPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().consecutivePasses()).isZero();
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p2");
      assertThat(card.getCardId()).isEqualTo("drawn-card");
    });
  }

  @Test
  void finalMultiItemResolutionClearsChainState() {
    LiveGameState state = state(multiItemChain(true, "p2"));

    engine.applyMove(state, new ResolveChainTopMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getChainState()).isNull();
  }

  @Test
  void pendingChoiceCreatedByTopChainItemPausesRemainingChainUntilResolved() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    LiveGameState state = state(stackedDeckAboveNoOpChain(true, "p2"));
    player(state, "p2").setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));
    state.getCards().add(cardInstance("stacked-1", "p2", "stacked", ZoneName.LIMBO));
    LegalActionsService legalActions = new LegalActionsService(cardDataService);

    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(state.getPendingChoice()).isNotNull();
    assertThat(state.getPendingChoice().getPlayerId()).isEqualTo("p2");
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::itemId)
        .containsExactly("item-1");
    assertThat(legalActions.legalActionsFor(state, "p2")).containsExactly(LegalAction.RESOLVE_CHOICE);
    assertThat(legalActions.legalActionsFor(state, "p1")).isEmpty();
    assertThatThrownBy(() -> engine.applyMove(state, new PassChainFocusMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");
    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");

    PendingChoice choice = state.getPendingChoice();
    engine.applyMove(state, new ResolveChoiceMove(
        "p2",
        choice.getChoiceId(),
        null,
        "card-0",
        PendingChoice.ACTION_HAND,
        List.of()));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::itemId)
        .containsExactly("item-1");

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getChainState()).isNull();
  }

  @Test
  void resolvedTopItemCannotResolveTwice() {
    LiveGameState state = state(chain(true, "p1"));

    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("No chain is active.");
    assertThat(state.getChainState()).isNull();
  }

  @Test
  void invalidMultiItemResolveDoesNotMutateChainState() {
    LiveGameState state = state(multiItemChain(true, "p2"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can resolve the chain item.");

    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::itemId)
        .containsExactly("item-1", "item-2");
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getChainState().readyToResolveTop()).isTrue();
  }

  @Test
  void cannotResolveChainBeforeAllRelevantPlayersPass() {
    LiveGameState state = state(chain(false, "p1"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChainTopMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("All relevant players must pass before resolving the chain item.");

    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getCards()).isEmpty();
  }

  @Test
  void normalMovesAreBlockedWhileChainIsActive() {
    LiveGameState state = state(chain(false, "p1"));

    assertThatThrownBy(() -> engine.applyMove(state, new PassPhaseMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the chain before taking another action.");

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getChainState()).isNotNull();
  }

  @Test
  void stackedDeckOpensPublicChainInRealGameplay() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    LiveGameState state = state(null);
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND));

    engine.applyMove(state, play("p1", "stacked-1"));

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("stacked-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.LIMBO));
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getChainState().consecutivePasses()).isZero();
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
    assertThat(state.getChainState().topItem()).satisfies(item -> {
      assertThat(item.sourceCardId()).isEqualTo("stacked");
      assertThat(item.sourceCardName()).isEqualTo("Stacked Deck");
      assertThat(item.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE);
      assertThat(item.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
      assertThat(item.status()).isEqualTo(LiveGameState.ChainItem.STATUS_PENDING);
      assertThat(item.counterable()).isTrue();
      assertThat(item.targetableOnChain()).isTrue();
      assertThat(item.chainItemType()).isEqualTo(LiveGameState.ChainItem.TYPE_SPELL);
      assertThat(item.sourceZoneBeforeChain()).isEqualTo(ZoneName.HAND);
    });
  }

  @Test
  void stackedDeckChainItemMetadataMatchesPriorityWindowService() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    LiveGameState state = state(null);
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND));
    CardDefinition def = cardDataService.getCard("stacked");
    PriorityWindowService priorityWindowService = new PriorityWindowService(cardDataService);
    PriorityWindowService.PriorityWindow expected = priorityWindowService
        .openingWindowForPlayedCard(state, def, false)
        .orElseThrow();

    engine.applyMove(state, play("p1", "stacked-1"));

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("stacked-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.LIMBO));
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().sourceContext()).isEqualTo(expected.sourceContext());
    assertThat(state.getChainState().topItem()).satisfies(item -> {
      assertThat(item.controllerPlayerId()).isEqualTo("p1");
      assertThat(item.sourceCardInstanceId()).isEqualTo("stacked-1");
      assertThat(item.sourceCardId()).isEqualTo(def.id());
      assertThat(item.sourceCardName()).isEqualTo(def.name());
      assertThat(item.effectKey()).isEqualTo(expected.effectKey());
      assertThat(item.chainItemType()).isEqualTo(expected.chainItemType());
      assertThat(item.visibility()).isEqualTo(expected.visibility());
      assertThat(item.counterable()).isEqualTo(expected.counterable());
      assertThat(item.targetableOnChain()).isEqualTo(expected.targetableOnChain());
      assertThat(item.sourceZoneBeforeChain()).isEqualTo(expected.sourceZoneBeforeChain());
      assertThat(item.publicDescription()).isEqualTo(expected.publicDescription());
    });
  }

  @Test
  void normalDrawSpellResolvesWithoutOpeningChainState() {
    stubCard("draw-spell", "Simple Insight", "Spell", 0, 0, 0, "Draw 1.");
    LiveGameState state = state(null);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));
    state.getCards().add(cardInstance("draw-spell-1", "p1", "draw-spell", ZoneName.HAND));

    engine.applyMove(state, play("p1", "draw-spell-1"));

    assertThat(state.getChainState()).isNull();
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("draw-spell-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD));
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p1");
      assertThat(card.getCardId()).isEqualTo("drawn-card");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(player(state, "p1").getDeckPool()).isEmpty();
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).isEqualTo("Played Simple Insight"));
  }

  @Test
  void nonPendingTopChainItemIsCleanedWithoutResolvingEffect() {
    LiveGameState state = state(statusChain(LiveGameState.ChainItem.STATUS_COUNTERED, true, "p1"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));

    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getChainState()).isNull();
    assertThat(player(state, "p1").getDeckPool()).containsExactly("drawn-card");
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-card"));
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("already countered"));
  }

  @Test
  void defyCountersStackedDeckAndPreventsPendingChoice() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(stackedDeckChain(false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.LIMBO));
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c")));

    engine.applyMove(state, counter("p2", "defy-1", "item-1"));

    assertThat(state.getChainState().chainItems()).hasSize(2);
    assertThat(state.getChainState().topItem().effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DEFY_COUNTER);
    assertThat(state.getChainState().topItem().counterable()).isFalse();

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().topItem().status()).isEqualTo(LiveGameState.ChainItem.STATUS_COUNTERED);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("stacked-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD));
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD));
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("countered Stacked Deck"));

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getChainState()).isNull();
    assertThat(state.getPendingChoice()).isNull();
    assertThat(player(state, "p1").getDeckPool()).containsExactly("top-a", "top-b", "top-c");
  }

  @Test
  void defyInvalidChainTargetDoesNotMoveCounterCard() {
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(statusChain(LiveGameState.ChainItem.STATUS_COUNTERED, false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only pending chain items can be countered.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).singleElement()
        .satisfies(item -> assertThat(item.status()).isEqualTo(LiveGameState.ChainItem.STATUS_COUNTERED));
  }

  @Test
  void defyCannotTargetNonCounterableChainItem() {
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(nonCounterableChain(false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That chain item cannot be countered.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void defyCanCounterSpellAtExactCostLimit() {
    stubCard("limit-spell", "Limit Spell", "Spell", 4, 1, 0, 0, "Draw 1.");
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(costedSpellChain("limit-spell", true, true, false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("limit-1", "p1", "limit-spell", ZoneName.LIMBO));
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    engine.applyMove(state, counter("p2", "defy-1", "item-1"));

    assertThat(state.getChainState().topItem().effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DEFY_COUNTER);
  }

  @Test
  void defyCannotCounterSpellAboveEnergyLimitWithoutMutation() {
    assertDefyCostRejected("expensive-spell", 5, 1, "Defy can only counter a spell that costs no more than 4 and no more than 1 power.");
  }

  @Test
  void defyCannotCounterSpellAbovePremiumLimitWithoutMutation() {
    assertDefyCostRejected("premium-spell", 4, 2, "Defy can only counter a spell that costs no more than 4 and no more than 1 power.");
  }

  @Test
  void nonFocusedPlayerCannotPlayDefy() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(stackedDeckChain(false, "p1"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Wait for your chain focus.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  @Test
  void defyRequiresTargetChainItemId() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(stackedDeckChain(false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", null)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Choose a chain item to counter.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  @Test
  void defyRejectsStaleTargetChainItemId() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(stackedDeckChain(false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", "missing-item")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That chain item is no longer available.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  @Test
  void invalidStackedDeckOpenerDoesNotSpendEnergyOrLeaveHand() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    LiveGameState state = state(null);
    player(state, "p1").setAvailableEnergy(1);
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, play("p1", "stacked-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient energy.");

    assertThat(player(state, "p1").getAvailableEnergy()).isEqualTo(1);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("stacked-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState()).isNull();
    assertThat(state.getLog()).isEmpty();
  }

  @Test
  void stackedDeckOpenerPaymentAndHandMovementHappenOnce() {
    stubCard("stacked", "Stacked Deck", "Spell", 2, 0, 0, stackedDeckText());
    LiveGameState state = state(null);
    player(state, "p1").setAvailableEnergy(3);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-1", "drawn-2", "drawn-3")));
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND));

    engine.applyMove(state, play("p1", "stacked-1"));
    assertThat(player(state, "p1").getAvailableEnergy()).isEqualTo(1);

    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(player(state, "p1").getAvailableEnergy()).isEqualTo(1);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("stacked-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD));
    assertThat(state.getPendingChoice()).isNotNull();
    assertThat(state.getPendingChoice().getType()).isEqualTo(PendingChoice.TYPE_TOP_DECK_PICK_ONE);
  }

  @Test
  void opponentCanPlayGustOnStackedDeckChainAndGustResolvesBeforeOpener() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    LiveGameState state = state(null);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-1", "drawn-2", "drawn-3")));
    CardInstance stacked = cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND);
    CardInstance gust = cardInstance("gust-1", "p2", "gust", ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p1", "target", ZoneName.BATTLEFIELD);
    state.getCards().addAll(List.of(stacked, gust, target));

    engine.applyMove(state, play("p1", "stacked-1"));
    engine.applyMove(state, new PlayCardMove("p2", "gust-1", ZoneName.BASE, 0, 0, "target-1"));

    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::effectKey)
        .containsExactly(
            LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
            LiveGameState.ChainItem.EFFECT_GUST_RETURN);

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(target.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(gust.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().chainItems())
        .extracting(LiveGameState.ChainItem::effectKey)
        .containsExactly(LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE);

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(stacked.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getChainState()).isNull();
    assertThat(state.getPendingChoice()).isNotNull();
    assertThat(state.getPendingChoice().getPlayerId()).isEqualTo("p1");
    assertThat(state.getPendingChoice().getType()).isEqualTo(PendingChoice.TYPE_TOP_DECK_PICK_ONE);
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("returned Target Unit"));
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("Stacked Deck is waiting for a private card choice"));
  }

  @Test
  void gustBounceDoesNotTriggerDeathknell() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("loyal", "Loyal Poro", "Unit", 0, 1, 1, "[Deathknell] If I did not die alone, draw 1.");
    LiveGameState state = state(null);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-1", "drawn-2", "drawn-3")));
    player(state, "p2").setDeckPool(new ArrayList<>(List.of("death-draw")));
    state.getCards().addAll(List.of(
        cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND),
        cardInstance("gust-1", "p2", "gust", ZoneName.HAND),
        cardInstance("loyal-1", "p1", "loyal", ZoneName.BATTLEFIELD)));
    when(cardDataService.hasKeyword("loyal", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, play("p1", "stacked-1"));
    engine.applyMove(state, new PlayCardMove("p2", "gust-1", ZoneName.BASE, 0, 0, "loyal-1"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("loyal-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(player(state, "p2").getDeckPool()).containsExactly("death-draw");
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("death-draw"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("destroyed") || entry.text().contains("died"));
  }

  @Test
  void gustBouncedUnitIsPrivateInOpponentAndSpectatorProjections() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    LiveGameState state = state(null);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-1", "drawn-2", "drawn-3")));
    state.getCards().addAll(List.of(
        cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND),
        cardInstance("gust-1", "p2", "gust", ZoneName.HAND),
        cardInstance("target-1", "p1", "target", ZoneName.BATTLEFIELD)));

    engine.applyMove(state, play("p1", "stacked-1"));
    engine.applyMove(state, new PlayCardMove("p2", "gust-1", ZoneName.BASE, 0, 0, "target-1"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p2"));

    GameStateProjectionService projectionService = new GameStateProjectionService(new LegalActionsService(cardDataService));
    LiveGameState ownerView = projectionService.toPublicView(state, "p1");
    LiveGameState opponentView = projectionService.toPublicView(state, "p2");
    LiveGameState spectatorView = projectionService.toPublicView(state, null);

    assertThat(ownerView.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("target-1");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
      assertThat(card.getCardId()).isEqualTo("target");
    });
    assertThat(opponentView.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("target-1");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
      assertThat(card.getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
      assertThat(card.isFaceDown()).isTrue();
    });
    assertThat(spectatorView.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("target-1");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
      assertThat(card.getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
      assertThat(card.isFaceDown()).isTrue();
    });
    assertThat(opponentView.getCards()).noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD && card.getInstanceId().equals("target-1"));
    assertThat(spectatorView.getCards()).noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD && card.getInstanceId().equals("target-1"));
  }

  @Test
  void gustCanBePlayedByFocusedPlayerDuringChainAndCreatesPublicTopItem() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(cardInstance("gust-1", "p1", "gust", com.riftforge.model.ZoneName.HAND));
    state.getCards().add(cardInstance("target-1", "p2", "target", com.riftforge.model.ZoneName.BATTLEFIELD));

    engine.applyMove(state, new com.riftforge.model.move.PlayCardMove("p1", "gust-1", com.riftforge.model.ZoneName.BASE, 0, 0, "target-1"));

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(com.riftforge.model.ZoneName.LIMBO));
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getChainState().consecutivePasses()).isZero();
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
    assertThat(state.getChainState().topItem()).satisfies(item -> {
      assertThat(item.sourceCardId()).isEqualTo("gust");
      assertThat(item.sourceCardName()).isEqualTo("Gust");
      assertThat(item.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_GUST_RETURN);
      assertThat(item.targetInstanceIds()).containsExactly("target-1");
      assertThat(item.chainTargets()).singleElement().satisfies(targetSummary -> {
        assertThat(targetSummary.targetInstanceId()).isEqualTo("target-1");
        assertThat(targetSummary.targetControllerPlayerId()).isEqualTo("p2");
        assertThat(targetSummary.targetKind()).isEqualTo("UNIT");
        assertThat(targetSummary.targetZone()).isEqualTo(ZoneName.BATTLEFIELD);
        assertThat(targetSummary.publicLabel()).isEqualTo("Target Unit");
        assertThat(targetSummary.publicSafe()).isTrue();
      });
      assertThat(item.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
    });
  }

  @Test
  void stackedDeckChainItemHasNoTargetMetadata() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    LiveGameState state = state(null);
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.HAND));

    engine.applyMove(state, play("p1", "stacked-1"));

    assertThat(state.getChainState().topItem().chainTargets()).isEmpty();
  }

  @Test
  void defyChainItemRecordsPublicChainTargetMetadata() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    stubCard("defy", "Defy", "Spell", 0, 0, 0, defyText());
    LiveGameState state = state(stackedDeckChain(false, "p2"));
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.LIMBO));
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));

    engine.applyMove(state, counter("p2", "defy-1", "item-1"));

    assertThat(state.getChainState().topItem().chainTargets()).singleElement().satisfies(targetSummary -> {
      assertThat(targetSummary.targetChainItemId()).isEqualTo("item-1");
      assertThat(targetSummary.targetControllerPlayerId()).isEqualTo("p1");
      assertThat(targetSummary.targetKind()).isEqualTo("CHAIN_ITEM");
      assertThat(targetSummary.publicLabel()).isEqualTo("Stacked Deck");
      assertThat(targetSummary.publicSafe()).isTrue();
    });
  }

  @Test
  void notSoFastIsRejectedOutsideChainState() {
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    LiveGameState state = state(null);
    state.getCards().add(cardInstance("not-so-fast-1", "p1", "not-so-fast", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p1", "not-so-fast-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Reaction timing is not implemented yet.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("not-so-fast-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void notSoFastCannotCounterUntargetedStackedDeck() {
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, stackedDeckText());
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    LiveGameState state = state(stackedDeckChain(false, "p2"));
    state.getCards().add(cardInstance("stacked-1", "p1", "stacked", ZoneName.LIMBO));
    state.getCards().add(cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Not So Fast can only counter an enemy spell that chooses a friendly Unit or Gear.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("not-so-fast-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void notSoFastCanCounterGustTargetingPlayersFriendlyUnit() {
    LiveGameState state = state(gustTargetingChain("p2", false, "p2"));
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.LIMBO));
    state.getCards().add(cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));
    state.getCards().add(cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD));

    engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1"));

    assertThat(state.getChainState().topItem()).satisfies(item -> {
      assertThat(item.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_NOT_SO_FAST_COUNTER);
      assertThat(item.counterable()).isFalse();
      assertThat(item.targetableOnChain()).isFalse();
      assertThat(item.chainTargets()).singleElement()
          .satisfies(targetSummary -> assertThat(targetSummary.targetChainItemId()).isEqualTo("item-1"));
    });
  }

  @Test
  void notSoFastCannotCounterGustTargetingOpponentUnit() {
    LiveGameState state = state(gustTargetingChain("p1", false, "p2"));
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.LIMBO));
    state.getCards().add(cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));
    state.getCards().add(cardInstance("target-1", "p1", "target", ZoneName.BATTLEFIELD));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Not So Fast can only counter an enemy spell that chooses a friendly Unit or Gear.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("not-so-fast-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void notSoFastCannotCounterDefyBecauseCounteringCountersIsDeferred() {
    LiveGameState state = state(defyChain(false, "p2"));
    stubCard("defy", "Defy", "Spell", 0, 0, 0, defyText());
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    state.getCards().add(cardInstance("defy-1", "p1", "defy", ZoneName.LIMBO));
    state.getCards().add(cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Not So Fast can only counter an enemy spell that chooses a friendly Unit or Gear.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("not-so-fast-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().topItem().effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DEFY_COUNTER);
  }

  @Test
  void notSoFastResolutionCountersGustAndPreventsBounce() {
    LiveGameState state = state(gustTargetingChain("p2", false, "p2"));
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    CardInstance gust = cardInstance("gust-1", "p1", "gust", ZoneName.LIMBO);
    CardInstance notSoFast = cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD);
    state.getCards().addAll(List.of(gust, notSoFast, target));

    engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(target.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(gust.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(notSoFast.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards())
        .filteredOn(card -> "gust".equals(card.getCardId()) && card.getZone() == ZoneName.DISCARD)
        .hasSize(1);
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().topItem().status()).isEqualTo(LiveGameState.ChainItem.STATUS_COUNTERED);
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("countered Gust"));

    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(target.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getChainState()).isNull();
  }

  @Test
  void notSoFastFizzlesSafelyIfTargetIsNoLongerPending() {
    LiveGameState state = state(gustTargetingChain("p2", false, "p2"));
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("not-so-fast", "Not So Fast", "Spell", 0, 0, 0, notSoFastText());
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.LIMBO));
    state.getCards().add(cardInstance("not-so-fast-1", "p2", "not-so-fast", ZoneName.HAND));
    state.getCards().add(cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD));

    engine.applyMove(state, counter("p2", "not-so-fast-1", "item-1"));
    List<LiveGameState.ChainItem> items = new ArrayList<>(state.getChainState().chainItems());
    items.set(0, items.get(0).withStatus(LiveGameState.ChainItem.STATUS_COUNTERED));
    state.setChainState(new LiveGameState.ChainState("chain-1", items, List.of("p1", "p2"), "p2", 2, true, "TEST"));

    engine.applyMove(state, new ResolveChainTopMove("p2"));

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.LIMBO));
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("not-so-fast-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD));
    assertThat(state.getLog()).anySatisfy(entry -> assertThat(entry.text()).contains("target was no longer legal"));
  }

  @Test
  void gustCannotBePlayedOutsideSupportedChainWindow() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    LiveGameState state = state(null);
    state.getCards().add(cardInstance("gust-1", "p1", "gust", com.riftforge.model.ZoneName.HAND));

    assertThatThrownBy(() -> engine.applyMove(state, new com.riftforge.model.move.PlayCardMove("p1", "gust-1", com.riftforge.model.ZoneName.BASE, 0, 0, "target-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Reaction timing is not implemented yet.");
  }

  @Test
  void gustRejectsIllegalTargetsBeforeMutation() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("big", "Big Unit", "Unit", 0, 4, 4, null);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(cardInstance("gust-1", "p1", "gust", com.riftforge.model.ZoneName.HAND));
    state.getCards().add(cardInstance("big-1", "p2", "big", com.riftforge.model.ZoneName.BATTLEFIELD));

    assertThatThrownBy(() -> engine.applyMove(state, new com.riftforge.model.move.PlayCardMove("p1", "gust-1", com.riftforge.model.ZoneName.BASE, 0, 0, "big-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Gust can only target a battlefield Unit or Champion with 3 Might or less.");

    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(com.riftforge.model.ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  @Test
  void invalidGustTargetDoesNotSpendEnergyOrTapRunes() {
    stubCard("gust", "Gust", "Spell", 1, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("big", "Big Unit", "Unit", 0, 4, 4, null);
    LiveGameState state = state(chain(false, "p1"));
    player(state, "p1").setAvailableEnergy(1);
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.HAND));
    state.getCards().add(cardInstance("big-1", "p2", "big", ZoneName.BATTLEFIELD));

    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove("p1", "gust-1", ZoneName.BASE, 0, 0, "big-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Gust can only target a battlefield Unit or Champion with 3 Might or less.");

    assertThat(player(state, "p1").getAvailableEnergy()).isEqualTo(1);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  @Test
  void temporaryMightOverThreeMakesGustTargetIllegalBeforeMutation() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    LiveGameState state = state(chain(false, "p1"));
    CardInstance target = cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD);
    target.setTemporaryPowerModifier(1);
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.HAND));
    state.getCards().add(target);

    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove("p1", "gust-1", ZoneName.BASE, 0, 0, "target-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Gust can only target a battlefield Unit or Champion with 3 Might or less.");

    assertThat(target.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void gustRejectsBaseGearLegendHiddenAndMissingTargets() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    assertGustTargetRejected(cardInstance("base-1", "p2", "base-unit", ZoneName.BASE), "base-unit", "Base Unit", "Unit");
    assertGustTargetRejected(cardInstance("gear-1", "p2", "gear", ZoneName.BATTLEFIELD), "gear", "Gear", "Gear");
    assertGustTargetRejected(cardInstance("legend-1", "p2", "legend", ZoneName.BATTLEFIELD), "legend", "Legend", "Legend");
    CardInstance hidden = cardInstance("hidden-1", "p2", "hidden-unit", ZoneName.BATTLEFIELD);
    hidden.setFaceDown(true);
    assertGustTargetRejected(hidden, "hidden-unit", "Hidden Unit", "Unit");

    LiveGameState missing = state(chain(false, "p1"));
    missing.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.HAND));
    assertThatThrownBy(() -> engine.applyMove(missing, new PlayCardMove("p1", "gust-1", ZoneName.BASE, 0, 0, "missing-target")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Card not found.");
    assertThat(missing.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  @Test
  void gustResolvesAndReturnsTargetToOwnerHandWithAttachmentsReturnedToBase() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    stubCard("gear", "Attached Gear", "Gear", 0, 0, 0, "[Equip]");
    LiveGameState state = state(chain(false, "p1"));
    CardInstance gust = cardInstance("gust-1", "p1", "gust", com.riftforge.model.ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p2", "target", com.riftforge.model.ZoneName.BATTLEFIELD);
    CardInstance gear = cardInstance("gear-1", "p2", "gear", com.riftforge.model.ZoneName.BASE);
    gear.setAttachedToInstanceId("target-1");
    state.getCards().addAll(List.of(gust, target, gear));
    when(cardZoneService.returnAttachmentsToBase(state, target)).thenAnswer(invocation -> {
      gear.setAttachedToInstanceId(null);
      gear.setZone(com.riftforge.model.ZoneName.BASE);
      return List.of(gear);
    });
    doAnswer(invocation -> {
      gust.setZone(com.riftforge.model.ZoneName.DISCARD);
      return null;
    }).when(cardZoneService).moveToGraveyard(gust);

    engine.applyMove(state, new com.riftforge.model.move.PlayCardMove("p1", "gust-1", com.riftforge.model.ZoneName.BASE, 0, 0, "target-1"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(target.getZone()).isEqualTo(com.riftforge.model.ZoneName.HAND);
    assertThat(gear.getZone()).isEqualTo(com.riftforge.model.ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(gust.getZone()).isEqualTo(com.riftforge.model.ZoneName.DISCARD);
    assertThat(state.getChainState().chainItems()).extracting(LiveGameState.ChainItem::itemId).containsExactly("item-1");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("returned Target Unit to its owner's hand"));
  }

  @Test
  void gustFizzlesIfTargetBecomesIllegalBeforeResolution() {
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    stubCard("target", "Target Unit", "Unit", 0, 3, 3, null);
    LiveGameState state = state(chain(false, "p1"));
    CardInstance gust = cardInstance("gust-1", "p1", "gust", com.riftforge.model.ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p2", "target", com.riftforge.model.ZoneName.BATTLEFIELD);
    state.getCards().addAll(List.of(gust, target));
    doAnswer(invocation -> {
      gust.setZone(com.riftforge.model.ZoneName.DISCARD);
      return null;
    }).when(cardZoneService).moveToGraveyard(gust);

    engine.applyMove(state, new com.riftforge.model.move.PlayCardMove("p1", "gust-1", com.riftforge.model.ZoneName.BASE, 0, 0, "target-1"));
    target.setMightBonus(1);
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(target.getZone()).isEqualTo(com.riftforge.model.ZoneName.BATTLEFIELD);
    assertThat(gust.getZone()).isEqualTo(com.riftforge.model.ZoneName.DISCARD);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("target was no longer legal"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("top chain item fizzled"));
  }

  @Test
  void disciplineResolvesFromChainToBoostSelectedUnitAndDrawPrivately() {
    stubCard("discipline", "Discipline", "Spell", 0, 0, 0, disciplineText());
    stubCard("target", "Target Unit", "Unit", 0, 2, 3, null);
    LiveGameState state = state(chain(false, "p1"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));
    CardInstance discipline = cardInstance("discipline-1", "p1", "discipline", ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD);
    state.getCards().addAll(List.of(discipline, target));

    engine.applyMove(state, new PlayCardMove("p1", "discipline-1", ZoneName.BASE, 0, 0, "target-1"));

    assertThat(discipline.getZone()).isEqualTo(ZoneName.LIMBO);
    assertThat(state.getChainState().topItem()).satisfies(item -> {
      assertThat(item.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DISCIPLINE_BOOST_DRAW);
      assertThat(item.targetInstanceIds()).containsExactly("target-1");
      assertThat(item.chainTargets()).singleElement()
          .satisfies(targetSummary -> assertThat(targetSummary.publicLabel()).isEqualTo("Target Unit"));
    });

    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(target.getTemporaryPowerModifier()).isEqualTo(2);
    assertThat(discipline.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p1");
      assertThat(card.getCardId()).isEqualTo("drawn-card");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Drawn Card"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("Resolved Discipline: gave +2 Might and drew a card."));
  }

  @Test
  void enGardeResolvesFromChainWithLoneFriendlyBonusOnlyWhenAlone() {
    stubCard("en-garde", "En Garde", "Spell", 0, 0, 0, enGardeText());
    stubCard("target", "Target Unit", "Unit", 0, 2, 3, null);
    LiveGameState lone = state(chain(false, "p1"));
    CardInstance enGarde = cardInstance("en-garde-1", "p1", "en-garde", ZoneName.HAND);
    CardInstance target = cardInstance("target-1", "p1", "target", ZoneName.BATTLEFIELD);
    lone.getCards().addAll(List.of(enGarde, target));

    engine.applyMove(lone, new PlayCardMove("p1", "en-garde-1", ZoneName.BASE, 0, 0, "target-1"));
    engine.applyMove(lone, new PassChainFocusMove("p2"));
    engine.applyMove(lone, new PassChainFocusMove("p1"));
    engine.applyMove(lone, new ResolveChainTopMove("p1"));

    assertThat(target.getTemporaryPowerModifier()).isEqualTo(2);

    LiveGameState crowded = state(chain(false, "p1"));
    CardInstance secondEnGarde = cardInstance("en-garde-2", "p1", "en-garde", ZoneName.HAND);
    CardInstance crowdedTarget = cardInstance("target-2", "p1", "target", ZoneName.BATTLEFIELD);
    CardInstance otherFriendly = cardInstance("target-3", "p1", "target", ZoneName.BATTLEFIELD);
    crowded.getCards().addAll(List.of(secondEnGarde, crowdedTarget, otherFriendly));

    engine.applyMove(crowded, new PlayCardMove("p1", "en-garde-2", ZoneName.BASE, 0, 0, "target-2"));
    engine.applyMove(crowded, new PassChainFocusMove("p2"));
    engine.applyMove(crowded, new PassChainFocusMove("p1"));
    engine.applyMove(crowded, new ResolveChainTopMove("p1"));

    assertThat(crowdedTarget.getTemporaryPowerModifier()).isEqualTo(1);
  }

  @Test
  void enGardeRejectsEnemyTargetWithoutMovingOrSpending() {
    stubCard("en-garde", "En Garde", "Spell", 0, 0, 0, enGardeText());
    stubCard("target", "Target Unit", "Unit", 0, 2, 3, null);
    LiveGameState state = state(chain(false, "p1"));
    CardInstance enGarde = cardInstance("en-garde-1", "p1", "en-garde", ZoneName.HAND);
    CardInstance enemy = cardInstance("target-1", "p2", "target", ZoneName.BATTLEFIELD);
    state.getCards().addAll(List.of(enGarde, enemy));

    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove("p1", "en-garde-1", ZoneName.BASE, 0, 0, "target-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("En Garde requires a friendly public Unit or Champion target.");

    assertThat(enGarde.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(enemy.getTemporaryPowerModifier()).isZero();
    assertThat(state.getChainState().chainItems()).hasSize(1);
  }

  private LiveGameState state(LiveGameState.ChainState chain) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    p1.setName("Player One");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    p2.setName("Player Two");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>());
    state.setLog(new ArrayList<>());
    state.setChainState(chain);
    return state;
  }

  private LiveGameState.ChainState chain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_DRAW_1_TEST,
            List.of(),
            1,
            "test draw")),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState multiItemChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(
            new LiveGameState.ChainItem(
                "item-1",
                "p1",
                "source-1",
                "source-card-1",
                "Source Card One",
                LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
                List.of(),
                1,
                "first item"),
            new LiveGameState.ChainItem(
                "item-2",
                "p2",
                "source-2",
                "source-card-2",
                "Source Card Two",
                LiveGameState.ChainItem.EFFECT_DRAW_1_TEST,
                List.of(),
                2,
                "second item")),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState stackedDeckAboveNoOpChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(
            new LiveGameState.ChainItem(
                "item-1",
                "p1",
                "source-1",
                "source-card-1",
                "Source Card One",
                LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
                List.of(),
                1,
                "first item"),
            new LiveGameState.ChainItem(
                "item-2",
                "p2",
                "stacked-1",
                "stacked",
                "Stacked Deck",
                LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
                List.of(),
                2,
                "Stacked Deck")),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState statusChain(String status, boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_DRAW_1_TEST,
            List.of(),
            1,
            "status item",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            status,
            true,
            true,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND)),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState stackedDeckChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "stacked-1",
            "stacked",
            "Stacked Deck",
            LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
            List.of(),
            1,
            "Stacked Deck",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            true,
            true,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND)),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "MAIN_ACTION");
  }

  private LiveGameState.ChainState nonCounterableChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_DRAW_1_TEST,
            List.of(),
            1,
            "non-counterable item",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            false,
            false,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND)),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState costedSpellChain(String sourceCardId, boolean counterable, boolean targetable, boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            sourceCardId,
            "Costed Spell",
            LiveGameState.ChainItem.EFFECT_DRAW_1_TEST,
            List.of(),
            1,
            "Costed Spell",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            counterable,
            targetable,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND)),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState gustTargetingChain(String targetController, boolean ready, String focus) {
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
                targetController,
                "UNIT",
                ZoneName.BATTLEFIELD,
                "Target Unit",
                true)))),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private LiveGameState.ChainState defyChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "defy-1",
            "defy",
            "Defy",
            LiveGameState.ChainItem.EFFECT_DEFY_COUNTER,
            List.of("item-0"),
            1,
            "Defy",
            LiveGameState.ChainItem.VISIBILITY_PUBLIC,
            LiveGameState.ChainItem.STATUS_PENDING,
            false,
            false,
            LiveGameState.ChainItem.TYPE_SPELL,
            ZoneName.HAND,
            List.of(new LiveGameState.ChainTarget(
                "counterTarget",
                null,
                "item-0",
                "p2",
                "CHAIN_ITEM",
                null,
                "Stacked Deck",
                true)))),
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "TEST");
  }

  private CardDefinition card(String id) {
    if (definitions.containsKey(id)) return definitions.get(id);
    String name = "drawn-card".equals(id) ? "Drawn Card" : id;
    return new CardDefinition(id, name, "Unit", null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of());
  }

  private void stubCard(String id, String name, String type, int cost, int might, int health, String rulesText) {
    definitions.put(id, new CardDefinition(id, name, type, null, List.of(), cost, 0, null, null, null, rulesText, might, health, List.of()));
  }

  private void stubCard(String id, String name, String type, int cost, int premiumCost, int might, int health, String rulesText) {
    definitions.put(id, new CardDefinition(id, name, type, null, List.of(), cost, premiumCost, null, null, null, rulesText, might, health, List.of()));
  }

  private void assertDefyCostRejected(String sourceCardId, int sourceCost, int sourcePremiumCost, String message) {
    stubCard(sourceCardId, "Costed Spell", "Spell", sourceCost, sourcePremiumCost, 0, 0, "Draw 1.");
    stubCard("defy", "Defy", "Spell", 1, 0, 0, defyText());
    LiveGameState state = state(costedSpellChain(sourceCardId, true, true, false, "p2"));
    player(state, "p2").setAvailableEnergy(3);
    state.getCards().add(cardInstance("source-1", "p1", sourceCardId, ZoneName.LIMBO));
    state.getCards().add(cardInstance("defy-1", "p2", "defy", ZoneName.HAND));
    int initialEnergy = player(state, "p2").getAvailableEnergy();
    int initialLogSize = state.getLog().size();

    assertThatThrownBy(() -> engine.applyMove(state, counter("p2", "defy-1", "item-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage(message);

    assertThat(player(state, "p2").getAvailableEnergy()).isEqualTo(initialEnergy);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("defy-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("source-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.LIMBO));
    assertThat(state.getChainState().chainItems()).singleElement()
        .satisfies(item -> assertThat(item.status()).isEqualTo(LiveGameState.ChainItem.STATUS_PENDING));
    assertThat(state.getLog()).hasSize(initialLogSize);
  }

  private String stackedDeckText() {
    return "[Action] Look at the top 3 cards of your Main Deck. Put 1 of them into your hand and recycle the rest.";
  }

  private String defyText() {
    return "[Reaction] (Play any time, even before spells and abilities resolve.) Counter a spell that costs no more than 4 and no more than 1.";
  }

  private String notSoFastText() {
    return "[Reaction] (Play any time, even before spells and abilities resolve.) Counter an enemy spell or ability that chooses a friendly unit or gear.";
  }

  private String disciplineText() {
    return "[Reaction] (Play any time, even before spells and abilities resolve.) Give a unit +2 :rb_might: this turn. Draw 1.";
  }

  private String enGardeText() {
    return "[Reaction] (Play any time, even before spells and abilities resolve.) Give a friendly unit +1 :rb_might: this turn, then an additional +1 :rb_might: this turn if it is the only unit you control there.";
  }

  private PlayCardMove play(String playerId, String instanceId) {
    return new PlayCardMove(playerId, instanceId, ZoneName.BASE, 0, 0, null);
  }

  private PlayCardMove counter(String playerId, String instanceId, String targetChainItemId) {
    return new PlayCardMove(playerId, instanceId, ZoneName.BASE, 0, 0, null, targetChainItemId, List.of(), false, List.of(), List.of());
  }

  private void assertGustTargetRejected(CardInstance target, String cardId, String cardName, String type) {
    stubCard(cardId, cardName, type, 0, 1, 1, null);
    ZoneName originalZone = target.getZone();
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(cardInstance("gust-1", "p1", "gust", ZoneName.HAND));
    state.getCards().add(target);

    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove("p1", "gust-1", ZoneName.BASE, 0, 0, target.getInstanceId())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Gust can only target a battlefield Unit or Champion with 3 Might or less.");

    assertThat(target.getZone()).isEqualTo(originalZone);
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("gust-1")).singleElement()
        .satisfies(card -> assertThat(card.getZone()).isEqualTo(ZoneName.HAND));
  }

  private CardInstance cardInstance(String instanceId, String ownerId, String cardId, com.riftforge.model.ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setOwnerId(ownerId);
    card.setCardId(cardId);
    card.setZone(zone);
    card.setCurrentHealth(card(cardId).health());
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private PlayerState player(LiveGameState state, String playerId) {
    return state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .orElseThrow();
  }
}
