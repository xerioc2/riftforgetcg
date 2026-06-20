package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.riftforge.engine.GameEngine;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChainFastPassServiceTest {
  @Mock CardDataService cardDataService;
  @Mock GameEngine engine;
  ChainFastPassService service;

  @BeforeEach
  void setUp() {
    service = new ChainFastPassService(new LegalActionsService(cardDataService));
  }

  @Test
  void focusedHumanPlayerWithOnlyPassIsNotAutoPassed() {
    LiveGameState state = state(chain(false, "p1"));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void focusedBotPlayerWithOnlyPassCanAutoPassSafely() {
    LiveGameState state = state(chain(false, "bot-player-riftbot"));

    assertThat(service.shouldAutoPass(state)).isTrue();
  }

  @Test
  void focusedPlayerWithLegalGustResponseIsNotAutoPassed() {
    CardDefinition gust = def("gust", "Gust", "Spell", 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    CardDefinition target = def("target", "Target Unit", "Unit", 0, 0, "");
    when(cardDataService.getCard("gust")).thenReturn(gust);
    when(cardDataService.getCard("target")).thenReturn(target);
    when(cardDataService.isGustReaction(gust)).thenReturn(true);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(card("gust-1", "p1", "gust", ZoneName.HAND));
    state.getCards().add(card("target-1", "p2", "target", ZoneName.BATTLEFIELD));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void focusedPlayerWithLegalDefyResponseIsNotAutoPassed() {
    CardDefinition stacked = def("stacked", "Stacked Deck", "Spell", 4, 1, "Look at the top 3 cards.");
    CardDefinition defy = def("defy", "Defy", "Spell", 0, 0, "[Reaction] Counter a spell.");
    when(cardDataService.getCard("stacked")).thenReturn(stacked);
    when(cardDataService.getCard("defy")).thenReturn(defy);
    when(cardDataService.isDefyCounterReaction(defy)).thenReturn(true);
    LiveGameState state = state(stackedDeckChain(false, "p1"));
    state.getCards().add(card("defy-1", "p1", "defy", ZoneName.HAND));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void focusedPlayerWithLegalNotSoFastResponseIsNotAutoPassed() {
    CardDefinition gust = def("gust", "Gust", "Spell", 0, 0, "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.");
    CardDefinition notSoFast = def("not-so-fast", "Not So Fast", "Spell", 0, 0, "[Reaction] Counter an enemy spell targeting your unit.");
    when(cardDataService.getCard("gust")).thenReturn(gust);
    when(cardDataService.getCard("not-so-fast")).thenReturn(notSoFast);
    when(cardDataService.isNotSoFastCounterReaction(notSoFast)).thenReturn(true);
    LiveGameState state = state(notSoFastTargetableGustChain("p1"));
    state.getCards().add(card("not-so-fast-1", "p1", "not-so-fast", ZoneName.HAND));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void focusedPlayerWithLegalDisciplineResponseIsNotAutoPassed() {
    CardDefinition discipline = def("discipline", "Discipline", "Spell", 0, 0, "[Reaction] Give a unit +2 Might this turn. Draw 1.");
    CardDefinition target = def("target", "Target Unit", "Unit", 0, 0, "");
    when(cardDataService.getCard("discipline")).thenReturn(discipline);
    when(cardDataService.getCard("target")).thenReturn(target);
    when(cardDataService.isDisciplineReaction(discipline)).thenReturn(true);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(card("discipline-1", "p1", "discipline", ZoneName.HAND));
    state.getCards().add(card("target-1", "p2", "target", ZoneName.BATTLEFIELD));

    assertThat(service.shouldAutoPass(state)).isFalse();
    assertThat(new LegalActionsService(cardDataService).legalActionsFor(state, "p1")).contains(LegalAction.PLAY_CARD);
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
  }

  @Test
  void focusedPlayerWithLegalEnGardeResponseIsNotAutoPassed() {
    CardDefinition enGarde = def("en-garde", "En Garde", "Spell", 0, 0, "[Reaction] Give a friendly unit +1 Might this turn, then an additional +1 Might this turn if it is the only unit you control there.");
    CardDefinition target = def("target", "Target Unit", "Unit", 0, 0, "");
    when(cardDataService.getCard("en-garde")).thenReturn(enGarde);
    when(cardDataService.getCard("target")).thenReturn(target);
    when(cardDataService.isEnGardeReaction(enGarde)).thenReturn(true);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(card("en-garde-1", "p1", "en-garde", ZoneName.HAND));
    state.getCards().add(card("target-1", "p1", "target", ZoneName.BATTLEFIELD));

    assertThat(service.shouldAutoPass(state)).isFalse();
    assertThat(new LegalActionsService(cardDataService).legalActionsFor(state, "p1")).contains(LegalAction.PLAY_CARD);
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
  }

  @Test
  void focusedPlayerWithLegalEclipseOrStupefyResponseIsNotAutoPassed() {
    CardDefinition eclipse = def("eclipse", "Eclipse", "Spell", 0, 0, "[Reaction] Give a unit -4 :rb_might: this turn. [Predict].");
    CardDefinition stupefy = def("stupefy", "Stupefy", "Spell", 0, 0, "[Reaction] Give a unit -1 :rb_might: this turn, to a minimum of 1 :rb_might:. Draw 1.");
    CardDefinition target = def("target", "Target Unit", "Unit", 0, 0, "");
    when(cardDataService.getCard("eclipse")).thenReturn(eclipse);
    when(cardDataService.getCard("stupefy")).thenReturn(stupefy);
    when(cardDataService.getCard("target")).thenReturn(target);
    when(cardDataService.isEclipseReaction(eclipse)).thenReturn(true);
    when(cardDataService.isStupefyReaction(stupefy)).thenReturn(true);
    LiveGameState eclipseState = state(chain(false, "p1"));
    eclipseState.getCards().add(card("eclipse-1", "p1", "eclipse", ZoneName.HAND));
    eclipseState.getCards().add(card("target-1", "p2", "target", ZoneName.BATTLEFIELD));
    LiveGameState stupefyState = state(chain(false, "p1"));
    stupefyState.getCards().add(card("stupefy-1", "p1", "stupefy", ZoneName.HAND));
    stupefyState.getCards().add(card("target-2", "p2", "target", ZoneName.BATTLEFIELD));

    assertThat(service.shouldAutoPass(eclipseState)).isFalse();
    assertThat(new LegalActionsService(cardDataService).legalActionsFor(eclipseState, "p1")).contains(LegalAction.PLAY_CARD);
    assertThat(eclipseState.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(eclipseState.getChainState().readyToResolveTop()).isFalse();

    assertThat(service.shouldAutoPass(stupefyState)).isFalse();
    assertThat(new LegalActionsService(cardDataService).legalActionsFor(stupefyState, "p1")).contains(LegalAction.PLAY_CARD);
    assertThat(stupefyState.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(stupefyState.getChainState().readyToResolveTop()).isFalse();
  }

  @Test
  void focusedPlayerWithLegalStarCrossedResponseIsNotAutoPassed() {
    CardDefinition starCrossed = def("star-crossed", "Star-Crossed", "Spell", 0, 0, "[Reaction] Return a friendly unit and an enemy unit to their owners' hands.");
    CardDefinition friendly = def("friendly", "Friendly Unit", "Unit", 0, 0, "");
    CardDefinition enemy = def("enemy", "Enemy Unit", "Unit", 0, 0, "");
    when(cardDataService.getCard("star-crossed")).thenReturn(starCrossed);
    when(cardDataService.getCard("friendly")).thenReturn(friendly);
    when(cardDataService.getCard("enemy")).thenReturn(enemy);
    when(cardDataService.isStarCrossedReaction(starCrossed)).thenReturn(true);
    LiveGameState state = state(chain(false, "p1"));
    state.getCards().add(card("star-crossed-1", "p1", "star-crossed", ZoneName.HAND));
    state.getCards().add(card("friendly-1", "p1", "friendly", ZoneName.BATTLEFIELD));
    state.getCards().add(card("enemy-1", "p2", "enemy", ZoneName.BATTLEFIELD));

    assertThat(service.shouldAutoPass(state)).isFalse();
    assertThat(new LegalActionsService(cardDataService).legalActionsFor(state, "p1")).contains(LegalAction.PLAY_CARD);
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().readyToResolveTop()).isFalse();
  }

  @Test
  void pendingChoicePreventsAutoPass() {
    LiveGameState state = state(chain(false, "p1"));
    state.setPendingChoice(PendingChoice.yesNo("choice-1", "p1", "source", "Choose?", PendingChoice.EFFECT_NONE));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void resolveChainTopAvailabilityPreventsAutoPass() {
    LiveGameState state = state(chain(true, "p1"));

    assertThat(service.shouldAutoPass(state)).isFalse();
  }

  @Test
  void ambiguousStateWithoutChainDoesNotMutate() {
    LiveGameState state = state(null);

    LiveGameState result = service.autoPassSafeWindows(engine, state);

    assertThat(result).isSameAs(state);
    verifyNoInteractions(engine);
  }

  @Test
  void autoPassSafeWindowsMarksStackedDeckReadyWhenNoResponsesExist() {
    LiveGameState state = state(botOnlyChain(false, "bot-player-riftbot"));
    when(engine.applyMove(any(LiveGameState.class), any(PassChainFocusMove.class))).thenAnswer(invocation -> {
      LiveGameState current = invocation.getArgument(0);
      PassChainFocusMove move = invocation.getArgument(1);
      applyPass(current, move.playerId());
      return current;
    });

    service.autoPassSafeWindows(engine, state);

    assertThat(state.getChainState().readyToResolveTop()).isTrue();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("bot-player-codex");
    assertThat(state.getChainState().consecutivePasses()).isEqualTo(2);
    verify(engine, times(2)).applyMove(any(LiveGameState.class), any(PassChainFocusMove.class));
  }

  @Test
  void autoPassSafeWindowsStopsOnFocusedHumanToPreserveBluffWindow() {
    LiveGameState state = state(chain(false, "bot-player-riftbot"));
    when(engine.applyMove(any(LiveGameState.class), any(PassChainFocusMove.class))).thenAnswer(invocation -> {
      LiveGameState current = invocation.getArgument(0);
      PassChainFocusMove move = invocation.getArgument(1);
      applyPass(current, move.playerId());
      return current;
    });

    service.autoPassSafeWindows(engine, state);

    assertThat(state.getChainState().readyToResolveTop()).isFalse();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().consecutivePasses()).isEqualTo(1);
    verify(engine).applyMove(any(LiveGameState.class), any(PassChainFocusMove.class));
  }

  private void applyPass(LiveGameState state, String playerId) {
    LiveGameState.ChainState chain = state.getChainState();
    int passes = Math.min(chain.relevantPlayerIds().size(), chain.consecutivePasses() + 1);
    boolean ready = passes >= chain.relevantPlayerIds().size();
    String focus = ready ? chain.topItem().controllerPlayerId() : nextFocus(chain.relevantPlayerIds(), playerId);
    state.setChainState(new LiveGameState.ChainState(
        chain.chainId(),
        chain.chainItems(),
        chain.relevantPlayerIds(),
        focus,
        passes,
        ready,
        chain.sourceContext()));
  }

  private String nextFocus(List<String> relevant, String current) {
    int index = relevant.indexOf(current);
    return relevant.get((index < 0 ? 0 : index + 1) % relevant.size());
  }

  private LiveGameState state(LiveGameState.ChainState chain) {
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"), player("bot-player-riftbot"), player("bot-player-codex"))));
    state.setChainState(chain);
    return state;
  }

  private PlayerState player(String id) {
    PlayerState player = new PlayerState();
    player.setUserId(id);
    player.setName(id);
    player.setAvailableEnergy(0);
    return player;
  }

  private LiveGameState.ChainState chain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "p1",
            "source-1",
            "stacked",
            "Stacked Deck",
            LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
            List.of(),
            1,
            "Stacked Deck")),
        List.of("p1", "bot-player-riftbot"),
        focus,
        ready ? 2 : 0,
        ready,
        "MAIN");
  }

  private LiveGameState.ChainState stackedDeckChain(boolean ready, String focus) {
    return chain(ready, focus);
  }

  private LiveGameState.ChainState botOnlyChain(boolean ready, String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            "bot-player-codex",
            "source-1",
            "stacked",
            "Stacked Deck",
            LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
            List.of(),
            1,
            "Stacked Deck")),
        List.of("bot-player-codex", "bot-player-riftbot"),
        focus,
        ready ? 2 : 0,
        ready,
        "MAIN");
  }

  private LiveGameState.ChainState notSoFastTargetableGustChain(String focus) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "gust-item-1",
            "p2",
            "gust-source-1",
            "gust",
            "Gust",
            LiveGameState.ChainItem.EFFECT_GUST_RETURN,
            List.of("p1-unit-1"),
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
                "p1-unit-1",
                null,
                "p1",
                "UNIT",
                ZoneName.BATTLEFIELD,
                "Target Unit",
                true)))),
        List.of("p2", "p1"),
        focus,
        0,
        false,
        "MAIN");
  }

  private CardInstance card(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setOwnerId(ownerId);
    card.setCardId(cardId);
    card.setZone(zone);
    card.setCurrentHealth(1);
    return card;
  }

  private CardDefinition def(String id, String name, String type, int cost, int premiumCost, String rulesText) {
    return new CardDefinition(id, name, type, null, List.of(), cost, premiumCost, null, null, null, rulesText, 1, 1, List.of());
  }
}
