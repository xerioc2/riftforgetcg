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
import com.riftforge.model.move.ResolveChainTopMove;
import com.riftforge.service.CardDataService;
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
    });
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
      assertThat(item.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
    });
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

  private CardDefinition card(String id) {
    if (definitions.containsKey(id)) return definitions.get(id);
    String name = "drawn-card".equals(id) ? "Drawn Card" : id;
    return new CardDefinition(id, name, "Unit", null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of());
  }

  private void stubCard(String id, String name, String type, int cost, int might, int health, String rulesText) {
    definitions.put(id, new CardDefinition(id, name, type, null, List.of(), cost, 0, null, null, null, rulesText, might, health, List.of()));
  }

  private String stackedDeckText() {
    return "[Action] Look at the top 3 cards of your Main Deck. Put 1 of them into your hand and recycle the rest.";
  }

  private PlayCardMove play(String playerId, String instanceId) {
    return new PlayCardMove(playerId, instanceId, ZoneName.BASE, 0, 0, null);
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
