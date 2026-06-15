package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.HashMap;
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
class GameEngineScoringTest {
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
  void holdCanScoreWinningPointAtBeginning() {
    LiveGameState state = state(Phase.BEGINNING);
    player(state, "p1").setScore(7);
    state.getBattlefieldController().put("BATTLEFIELD", "p1");

    engine.applyMove(state, new PassPhaseMove("p1"));

    assertThat(player(state, "p1").getScore()).isEqualTo(8);
    assertThat(state.getWinnerId()).isEqualTo("p1");
    assertThat(state.getScoredBattlefieldsThisTurn()).containsExactly("BATTLEFIELD");
  }

  @Test
  void holdScoresEachControlledBattlefieldOnce() {
    LiveGameState state = state(Phase.BEGINNING);
    state.setBattlefieldController(new HashMap<>());
    state.getBattlefieldController().put("BATTLEFIELD", "p1");
    state.getBattlefieldController().put("SECOND_BATTLEFIELD", "p1");

    engine.applyMove(state, new PassPhaseMove("p1"));

    assertThat(player(state, "p1").getScore()).isEqualTo(2);
    assertThat(state.getScoredBattlefieldsThisTurn()).containsExactlyInAnyOrder("BATTLEFIELD", "SECOND_BATTLEFIELD");
  }

  @Test
  void conquerCannotBeIllegalWinningPointAndDrawsInstead() {
    LiveGameState state = state(Phase.MAIN, card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    player(state, "p1").setScore(7);
    player(state, "p1").getDeckPool().add("draw-card");
    state.getBattlefieldController().put("BATTLEFIELD", "p2");
    state.getBattlefieldController().put("SECOND_BATTLEFIELD", "p1");
    stubUnit("attacker", 3);
    stubUnit("defender", 1);
    stubUnit("draw-card", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 3, 1);

    assertThat(player(state, "p1").getScore()).isEqualTo(7);
    assertThat(state.getWinnerId()).isNull();
    assertThat(state.getCards())
        .anySatisfy(card -> {
          assertThat(card.getCardId()).isEqualTo("draw-card");
          assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
        });
    assertThat(state.getBattlefieldController()).containsEntry("BATTLEFIELD", "p1");
  }

  @Test
  void conquerCanScoreWinningPointWhenAllBattlefieldsScored() {
    LiveGameState state = state(Phase.MAIN, card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    player(state, "p1").setScore(7);
    state.getBattlefieldController().put("BATTLEFIELD", "p2");
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 3, 1);

    assertThat(player(state, "p1").getScore()).isEqualTo(8);
    assertThat(state.getWinnerId()).isEqualTo("p1");
  }

  private LiveGameState state(Phase phase, CardInstance... cards) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    p1.setName("Player One");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    p2.setName("Player Two");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(phase);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private PlayerState player(LiveGameState state, String playerId) {
    return state.getPlayers().stream().filter(player -> playerId.equals(player.getUserId())).findFirst().orElseThrow();
  }

  private void passShowdownFocusCycle(LiveGameState state) {
    engine.applyMove(state, new PassShowdownFocusMove("p1"));
    engine.applyMove(state, new PassShowdownFocusMove("p2"));
  }

  private void resolveShowdownWithAssignments(LiveGameState state, String attackerId, String defenderId, int attackerDamage, int defenderDamage) {
    engine.applyMove(state, new ResolveShowdownMove("p1"));
    engine.applyMove(state, assign("p1", attackerId, defenderId, attackerDamage));
    engine.applyMove(state, assign("p2", defenderId, attackerId, defenderDamage));
  }

  private AssignCombatDamageMove assign(String playerId, String sourceId, String targetId, int amount) {
    return new AssignCombatDamageMove(playerId, List.of(new LiveGameState.CombatDamageAssignment(sourceId, targetId, amount)));
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
