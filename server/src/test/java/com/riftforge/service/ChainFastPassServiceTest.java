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
  void focusedPlayerWithOnlyPassCanAutoPassSafely() {
    LiveGameState state = state(chain(false, "p1"));

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
    LiveGameState state = state(chain(false, "p2"));
    when(engine.applyMove(any(LiveGameState.class), any(PassChainFocusMove.class))).thenAnswer(invocation -> {
      LiveGameState current = invocation.getArgument(0);
      PassChainFocusMove move = invocation.getArgument(1);
      applyPass(current, move.playerId());
      return current;
    });

    service.autoPassSafeWindows(engine, state);

    assertThat(state.getChainState().readyToResolveTop()).isTrue();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getChainState().consecutivePasses()).isEqualTo(2);
    verify(engine, times(2)).applyMove(any(LiveGameState.class), any(PassChainFocusMove.class));
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
    state.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
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
        List.of("p1", "p2"),
        focus,
        ready ? 2 : 0,
        ready,
        "MAIN");
  }

  private LiveGameState.ChainState stackedDeckChain(boolean ready, String focus) {
    return chain(ready, focus);
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
