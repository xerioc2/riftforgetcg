package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.ResolveShowdownMove;
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
class GameEngineShowdownTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  @Mock CardZoneService cardZoneService;
  GameEngine engine;

  @BeforeEach
  void setUp() {
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenReturn(false);
    when(cardDataService.getKeywordValue(any(CardInstance.class), anyString())).thenReturn(0);
    doAnswer(invocation -> {
      ((CardInstance) invocation.getArgument(0)).setZone(ZoneName.DISCARD);
      return null;
    }).when(cardZoneService).moveToGraveyard(any(CardInstance.class));
  }

  @Test
  void startsShowdownDuringMainPhase() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackingPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("attacker");
    assertThat(state.getActiveShowdown().step()).isEqualTo(ShowdownStep.ACTION_WINDOW);
  }

  @Test
  void preventsNestedShowdowns() {
    LiveGameState state = state(
        card("attacker-one", "p1", ZoneName.BASE),
        card("attacker-two", "p1", ZoneName.BASE),
        card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker-one", 3);
    stubUnit("attacker-two", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker-one"));

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker-two")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("Resolve the active showdown");
  }

  @Test
  void resolvingShowdownReturnsToMainPhase() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getActiveShowdown()).isNull();
  }

  @Test
  void allowsMultipleShowdownsInOneMainPhase() {
    LiveGameState state = state(
        card("attacker-one", "p1", ZoneName.BASE),
        card("attacker-two", "p1", ZoneName.BASE),
        card("defender-one", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker-one", 3);
    stubUnit("attacker-two", 3);
    stubUnit("defender-one", 1);
    stubUnit("defender-two", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker-one"));
    engine.applyMove(state, new ResolveShowdownMove("p1"));
    state.getCards().add(card("defender-two", "p2", ZoneName.BATTLEFIELD));

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker-two"));

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("attacker-two");
  }

  @Test
  void turnEndsOnlyAfterPlayerPassesMainPhase() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);

    engine.applyMove(state, new PassPhaseMove("p1"));

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.END);
  }

  private LiveGameState state(CardInstance... cards) {
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
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(3);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private void stubUnit(String id, int might) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, "Unit", null, List.of(), 0, 0, null, null, null, null, might, might, List.of()));
  }
}
