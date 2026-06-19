package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriorityWindowServiceTest {
  @Mock CardDataService cardDataService;

  @Test
  void stackedDeckOpensPublicCounterableSpellPriorityWindow() {
    CardDefinition stackedDeck = card("stacked", "Stacked Deck", "Spell", "Draw from the top cards.");
    when(cardDataService.isStackedDeckEffect(stackedDeck)).thenReturn(true);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    PriorityWindowService.PriorityWindow window = service.openingWindowForPlayedCard(state(), stackedDeck, false).orElseThrow();

    assertThat(window.type()).isEqualTo(PriorityWindowService.PriorityWindowType.SPELL_PLAYED);
    assertThat(window.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE);
    assertThat(window.chainItemType()).isEqualTo(LiveGameState.ChainItem.TYPE_SPELL);
    assertThat(window.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
    assertThat(window.counterable()).isTrue();
    assertThat(window.targetableOnChain()).isTrue();
    assertThat(window.sourceZoneBeforeChain()).isEqualTo(ZoneName.HAND);
    assertThat(window.publicDescription()).isEqualTo("Stacked Deck");
    assertThat(window.sourceContext()).isEqualTo("MAIN_ACTION");
  }

  @Test
  void stackedDeckPlayedDuringShowdownKeepsShowdownSourceContext() {
    CardDefinition stackedDeck = card("stacked", "Stacked Deck", "Spell", "Draw from the top cards.");
    when(cardDataService.isStackedDeckEffect(stackedDeck)).thenReturn(true);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    PriorityWindowService.PriorityWindow window = service.openingWindowForPlayedCard(state(), stackedDeck, true).orElseThrow();

    assertThat(window.type()).isEqualTo(PriorityWindowService.PriorityWindowType.SHOWDOWN_ACTION);
    assertThat(window.sourceContext()).isEqualTo("SHOWDOWN_ACTION");
  }

  @Test
  void simplePublicDrawOneSpellOpensReactionWindow() {
    CardDefinition draw = card("draw", "Simple Insight", "Spell", "Draw 1.");
    when(cardDataService.isStackedDeckEffect(draw)).thenReturn(false);
    when(cardDataService.isReactionCard(draw)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("draw")).thenReturn(false);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    PriorityWindowService.PriorityWindow window = service.openingWindowForPlayedCard(state(), draw, false).orElseThrow();

    assertThat(service.chainOpenReasonForPlayedCard(draw)).contains(PriorityWindowService.ChainOpenReason.SIMPLE_DRAW_ONE_SPELL);
    assertThat(window.type()).isEqualTo(PriorityWindowService.PriorityWindowType.SPELL_PLAYED);
    assertThat(window.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DRAW_1);
    assertThat(window.chainItemType()).isEqualTo(LiveGameState.ChainItem.TYPE_SPELL);
    assertThat(window.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
    assertThat(window.counterable()).isTrue();
    assertThat(window.targetableOnChain()).isTrue();
    assertThat(window.publicDescription()).isEqualTo("Simple Insight");
  }

  @Test
  void unsupportedAndPrivateChoiceSpellsDoNotOpenReactionWindowByPolicy() {
    CardDefinition unsupported = card("unsupported", "Unsupported Trick", "Spell", "Draw 1.");
    CardDefinition choice = card("choice", "Choice Trick", "Spell", "Choose a unit. Draw 1.");
    when(cardDataService.isStackedDeckEffect(unsupported)).thenReturn(false);
    when(cardDataService.isStackedDeckEffect(choice)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("unsupported")).thenReturn(true);
    when(cardDataService.isUnsupportedAction("choice")).thenReturn(false);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    assertThat(service.openingWindowForPlayedCard(state(), unsupported, false)).isEmpty();
    assertThat(service.openingWindowForPlayedCard(state(), choice, false)).isEmpty();
  }

  @Test
  void normalCardsDoNotOpenPriorityByDefault() {
    CardDefinition unit = card("unit", "Tideturner", "Unit", "A normal unit.");
    when(cardDataService.isStackedDeckEffect(unit)).thenReturn(false);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    assertThat(service.openingWindowForPlayedCard(state(), unit, false)).isEmpty();
  }

  @Test
  void existingChainPreventsNewOpeningWindow() {
    CardDefinition stackedDeck = card("stacked", "Stacked Deck", "Spell", "Draw from the top cards.");
    PriorityWindowService service = new PriorityWindowService(cardDataService);
    LiveGameState state = state();
    state.setChainState(new LiveGameState.ChainState("chain", List.of(), List.of("p1", "p2"), "p2", 0, false, "MAIN_ACTION"));

    assertThat(service.openingWindowForPlayedCard(state, stackedDeck, false)).isEmpty();
  }

  @Test
  void unsupportedReactionDoesNotGetReactionWindowByDefault() {
    CardDefinition unknownReaction = card("unknown-reaction", "Unknown Reaction", "Spell", "[Reaction] Do something unsupported.");
    when(cardDataService.isGustReaction(unknownReaction)).thenReturn(false);
    when(cardDataService.isDefyCounterReaction(unknownReaction)).thenReturn(false);
    when(cardDataService.isNotSoFastCounterReaction(unknownReaction)).thenReturn(false);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    assertThat(service.reactionWindowFor(unknownReaction)).isEmpty();
  }

  @Test
  void supportedReactionWindowsPreserveCurrentMetadataPolicy() {
    CardDefinition gust = card("gust", "Gust", "Spell", "[Reaction] Return a unit.");
    CardDefinition defy = card("defy", "Defy", "Spell", "[Reaction] Counter a spell.");
    CardDefinition notSoFast = card(
        "not-so-fast",
        "Not So Fast",
        "Spell",
        "[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.");
    when(cardDataService.isGustReaction(gust)).thenReturn(true);
    when(cardDataService.isGustReaction(defy)).thenReturn(false);
    when(cardDataService.isDefyCounterReaction(defy)).thenReturn(true);
    when(cardDataService.isGustReaction(notSoFast)).thenReturn(false);
    when(cardDataService.isDefyCounterReaction(notSoFast)).thenReturn(false);
    when(cardDataService.isNotSoFastCounterReaction(notSoFast)).thenReturn(true);
    PriorityWindowService service = new PriorityWindowService(cardDataService);

    PriorityWindowService.PriorityWindow gustWindow = service.reactionWindowFor(gust).orElseThrow();
    PriorityWindowService.PriorityWindow defyWindow = service.reactionWindowFor(defy).orElseThrow();
    PriorityWindowService.PriorityWindow notSoFastWindow = service.reactionWindowFor(notSoFast).orElseThrow();

    assertThat(gustWindow.type()).isEqualTo(PriorityWindowService.PriorityWindowType.REACTION_PLAYED);
    assertThat(gustWindow.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_GUST_RETURN);
    assertThat(gustWindow.counterable()).isTrue();
    assertThat(gustWindow.targetableOnChain()).isTrue();

    assertThat(defyWindow.type()).isEqualTo(PriorityWindowService.PriorityWindowType.REACTION_PLAYED);
    assertThat(defyWindow.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_DEFY_COUNTER);
    assertThat(defyWindow.counterable()).isFalse();
    assertThat(defyWindow.targetableOnChain()).isFalse();

    assertThat(notSoFastWindow.type()).isEqualTo(PriorityWindowService.PriorityWindowType.REACTION_PLAYED);
    assertThat(notSoFastWindow.effectKey()).isEqualTo(LiveGameState.ChainItem.EFFECT_NOT_SO_FAST_COUNTER);
    assertThat(notSoFastWindow.chainItemType()).isEqualTo(LiveGameState.ChainItem.TYPE_SPELL);
    assertThat(notSoFastWindow.visibility()).isEqualTo(LiveGameState.ChainItem.VISIBILITY_PUBLIC);
    assertThat(notSoFastWindow.counterable()).isFalse();
    assertThat(notSoFastWindow.targetableOnChain()).isFalse();
    assertThat(notSoFastWindow.sourceZoneBeforeChain()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void relevantPlayersAreDeterministicAndFocusAdvancesInAlphaOrder() {
    PriorityWindowService service = new PriorityWindowService(cardDataService);
    LiveGameState state = state();
    state.setPlayers(new ArrayList<>(List.of(player("p2"), player("p1"))));

    List<String> relevant = service.relevantPlayers(state, null);

    assertThat(relevant).containsExactly("p1", "p2");
    assertThat(service.nextFocusedPlayerId(relevant, "p1")).isEqualTo("p2");
    assertThat(service.nextFocusedPlayerId(relevant, "p2")).isEqualTo("p1");
  }

  @Test
  void relevantPlayersPreserveExistingChainOrder() {
    PriorityWindowService service = new PriorityWindowService(cardDataService);
    LiveGameState state = state();
    LiveGameState.ChainState chain = new LiveGameState.ChainState(
        "chain",
        List.of(),
        List.of("p2", "p1"),
        "p2",
        0,
        false,
        "MAIN_ACTION");

    List<String> relevant = service.relevantPlayers(state, chain);

    assertThat(relevant).containsExactly("p2", "p1");
    assertThat(service.nextFocusedPlayerId(relevant, "p2")).isEqualTo("p1");
    assertThat(service.nextFocusedPlayerId(relevant, "p1")).isEqualTo("p2");
  }

  private LiveGameState state() {
    LiveGameState state = new LiveGameState();
    state.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
    return state;
  }

  private PlayerState player(String id) {
    PlayerState player = new PlayerState();
    player.setUserId(id);
    return player;
  }

  private CardDefinition card(String id, String name, String type, String text) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, text, 0, 0, List.of());
  }
}
