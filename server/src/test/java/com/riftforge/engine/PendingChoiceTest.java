package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.CardInstance;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.ResolveChoiceMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PendingChoiceTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  GameEngine engine;

  @BeforeEach
  void setUp() {
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.getCard("drawn-card")).thenReturn(card("drawn-card", "Drawn Card"));
    when(cardDataService.getCard("source")).thenReturn(card("source", "Test Source"));
    when(cardDataService.getCard("stacked-deck")).thenReturn(card("stacked-deck", "Stacked Deck",
        "Look at the top 3 cards of your Main Deck. Put 1 of them into your hand and recycle the rest."));
    when(cardDataService.getCard("top-a")).thenReturn(card("top-a", "Top A"));
    when(cardDataService.getCard("top-b")).thenReturn(card("top-b", "Top B"));
    when(cardDataService.getCard("top-c")).thenReturn(card("top-c", "Top C"));
    when(cardDataService.getCard("rest")).thenReturn(card("rest", "Rest"));
  }

  @Test
  void ownerCanResolveYesChoiceAndDrawOne() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw one?"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));

    engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", PendingChoice.OPTION_YES));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(player(state, "p1").getDeckPool()).isEmpty();
    assertThat(state.getCards()).anySatisfy(card -> assertThat(card.getCardId()).isEqualTo("drawn-card"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Alice chose to draw 1."));
  }

  @Test
  void noChoiceClearsWithoutApplyingEffect() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw one?"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));

    engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", PendingChoice.OPTION_NO));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(player(state, "p1").getDeckPool()).containsExactly("drawn-card");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Alice declined Test Source."));
  }

  @Test
  void payOneChoiceConsumesEnergyAndDraws() {
    LiveGameState state = state();
    player(state, "p1").setAvailableEnergy(1);
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("drawn-card")));
    state.setPendingChoice(PendingChoice.optionalPayOneDrawOne("choice-1", "p1", "source", "Pay 1 to draw one?"));

    engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", PendingChoice.OPTION_PAY_1));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(player(state, "p1").getAvailableEnergy()).isZero();
    assertThat(state.getCards()).anySatisfy(card -> assertThat(card.getCardId()).isEqualTo("drawn-card"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Alice paid 1 to draw 1."));
  }

  @Test
  void payOneChoiceValidatesEnergyAtResolution() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalPayOneDrawOne("choice-1", "p1", "source", "Pay 1 to draw one?"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", PendingChoice.OPTION_PAY_1)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient energy for that choice.");
  }

  @Test
  void onlyChoiceOwnerCanResolve() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw one?"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p2", "choice-1", PendingChoice.OPTION_YES)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That choice belongs to another player.");
  }

  @Test
  void invalidOptionIsRejected() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw one?"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", "MAYBE")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Invalid choice option.");
  }

  @Test
  void otherMovesPauseWhileChoiceIsPending() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.optionalDrawOne("choice-1", "p1", "source", "Draw one?"));

    assertThatThrownBy(() -> engine.applyMove(state, new PassPhaseMove("p1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");
  }

  @Test
  void stackedDeckCreatesPrivateTopThreeChoice() {
    LiveGameState state = state();
    state.getCards().add(handCard("stacked", "stacked-deck", "p1"));
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));

    engine.applyMove(state, new PlayCardMove("p1", "stacked", ZoneName.BASE, 0, 0, null));

    assertThat(state.getPendingChoice()).isNotNull();
    assertThat(state.getPendingChoice().getType()).isEqualTo(PendingChoice.TYPE_TOP_DECK_PICK_ONE);
    assertThat(state.getPendingChoice().getCardOptions())
        .extracting(PendingChoice.CardChoiceOption::cardId)
        .containsExactly("top-a", "top-b", "top-c");
    assertThat(player(state, "p1").getDeckPool()).containsExactly("top-a", "top-b", "top-c", "rest");
  }

  @Test
  void stackedDeckSelectedCardGoesToHandAndRestRecycleInOriginalOrder() {
    LiveGameState state = state();
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "p1",
        "stacked-deck",
        "source-instance",
        List.of(cardDataService.getCard("top-a"), cardDataService.getCard("top-b"), cardDataService.getCard("top-c"))));

    engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", null, "card-1", PendingChoice.ACTION_HAND, List.of()));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("top-b");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(player(state, "p1").getDeckPool()).containsExactly("rest", "top-a", "top-c");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Stacked Deck put a card into hand and recycled the rest."));
  }

  @Test
  void stackedDeckRejectsCardOutsidePrompt() {
    LiveGameState state = state();
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("top-a", "top-b")));
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "p1",
        "stacked-deck",
        "source-instance",
        List.of(cardDataService.getCard("top-a"), cardDataService.getCard("top-b"))));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", null, "card-9", PendingChoice.ACTION_HAND, List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Choose one of the revealed cards.");
  }

  @Test
  void predictTopChoicesResolveLifoAndBottomChoicesKeepAssignmentOrder() {
    LiveGameState state = state();
    player(state, "p1").setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));
    state.setPendingChoice(PendingChoice.predictOrder(
        "choice-1",
        "p1",
        "source",
        "source-instance",
        List.of(cardDataService.getCard("top-a"), cardDataService.getCard("top-b"), cardDataService.getCard("top-c"))));

    engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", null, null, null, List.of(
        new PendingChoice.CardChoiceAssignment("card-0", PendingChoice.ACTION_TOP, 0),
        new PendingChoice.CardChoiceAssignment("card-1", PendingChoice.ACTION_TOP, 1),
        new PendingChoice.CardChoiceAssignment("card-2", PendingChoice.ACTION_BOTTOM, 2))));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(player(state, "p1").getDeckPool()).containsExactly("top-b", "top-a", "rest", "top-c");
  }

  @Test
  void predictRequiresEveryRevealedCardAssignedOnce() {
    LiveGameState state = state();
    state.setPendingChoice(PendingChoice.predictOrder(
        "choice-1",
        "p1",
        "source",
        "source-instance",
        List.of(cardDataService.getCard("top-a"), cardDataService.getCard("top-b"))));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p1", "choice-1", null, null, null, List.of(
        new PendingChoice.CardChoiceAssignment("card-0", PendingChoice.ACTION_TOP, 0)))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Choose top or bottom for each revealed card.");
  }

  private LiveGameState state() {
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(player("p1", "Alice"), player("p2", "Bob"))));
    state.setCards(new ArrayList<>());
    return state;
  }

  private PlayerState player(String id, String name) {
    PlayerState player = new PlayerState();
    player.setUserId(id);
    player.setName(name);
    return player;
  }

  private PlayerState player(LiveGameState state, String id) {
    return state.getPlayers().stream().filter(player -> id.equals(player.getUserId())).findFirst().orElseThrow();
  }

  private CardDefinition card(String id, String name) {
    return card(id, name, "");
  }

  private CardDefinition card(String id, String name, String rulesText) {
    return new CardDefinition(id, name, "Spell", null, List.of(), 0, 0, null, null, null, rulesText, 0, 0, List.of());
  }

  private CardInstance handCard(String instanceId, String cardId, String ownerId) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(ZoneName.HAND);
    return card;
  }

}
