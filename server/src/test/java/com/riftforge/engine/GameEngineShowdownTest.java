package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.PlayCardMove;
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
    doAnswer(invocation -> {
      LiveGameState state = invocation.getArgument(0);
      CardInstance host = invocation.getArgument(1);
      List<CardInstance> attachments = state.getCards().stream()
          .filter(attachment -> host.getInstanceId().equals(attachment.getAttachedToInstanceId()))
          .toList();
      attachments.forEach(attachment -> {
        attachment.setAttachedToInstanceId(null);
        attachment.setZone(ZoneName.BASE);
        attachment.setX(0);
        attachment.setY(0);
        attachment.setTapped(false);
        attachment.setHasSummoningSickness(false);
      });
      return attachments;
    }).when(cardZoneService).returnAttachmentsToBase(any(LiveGameState.class), any(CardInstance.class));
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
    assertThat(state.getActiveShowdown().relevantPlayerIds()).containsExactly("p1", "p2");
    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().readyToResolve()).isFalse();
  }

  @Test
  void passFocusCyclesToDefenderThenMarksShowdownReadyToResolve() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    engine.applyMove(state, new PassShowdownFocusMove("p1"));

    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getActiveShowdown().consecutivePasses()).isEqualTo(1);
    assertThat(state.getActiveShowdown().readyToResolve()).isFalse();

    engine.applyMove(state, new PassShowdownFocusMove("p2"));

    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().consecutivePasses()).isEqualTo(2);
    assertThat(state.getActiveShowdown().readyToResolve()).isTrue();
  }

  @Test
  void rejectedPassFocusDoesNotMutateFocusState() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThatThrownBy(() -> engine.applyMove(state, new PassShowdownFocusMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass showdown focus.");

    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().consecutivePasses()).isZero();
    assertThat(state.getActiveShowdown().readyToResolve()).isFalse();
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
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 3, 1);

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getActiveShowdown()).isNull();
  }

  @Test
  void assignedCombatWhereBothSidesSurviveRecallsAttackersAndClearsAssignmentState() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE, 3), card("defender", "p2", ZoneName.BATTLEFIELD, 3));
    stubUnit("attacker", 1, 3);
    stubUnit("defender", 1, 3);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 1, 1);

    assertThat(state.getActiveShowdown()).isNull();
    assertThat(cardById(state, "attacker").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(cardById(state, "defender").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(cardById(state, "attacker").getCurrentHealth()).isEqualTo(3);
    assertThat(cardById(state, "defender").getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void assignedCombatWhereOnlyAttackersSurviveConquersAndScores() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE, 3), card("defender", "p2", ZoneName.BATTLEFIELD, 1));
    stubUnit("attacker", 3, 3);
    stubUnit("defender", 1, 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 3, 1);

    assertThat(state.getActiveShowdown()).isNull();
    assertThat(cardById(state, "attacker").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(cardById(state, "attacker").getCurrentHealth()).isEqualTo(3);
    assertThat(cardById(state, "defender").getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getBattlefieldController()).containsEntry("BATTLEFIELD", "p1");
    assertThat(player(state, "p1").getScore()).isEqualTo(1);
  }

  @Test
  void assignedCombatWhereOnlyDefendersSurviveLeavesControlAndClearsShowdown() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE, 1), card("defender", "p2", ZoneName.BATTLEFIELD, 3));
    stubUnit("attacker", 1, 1);
    stubUnit("defender", 3, 3);
    state.getBattlefieldController().put("BATTLEFIELD", "p2");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 1, 3);

    assertThat(state.getActiveShowdown()).isNull();
    assertThat(cardById(state, "attacker").getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(cardById(state, "defender").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(cardById(state, "defender").getCurrentHealth()).isEqualTo(3);
    assertThat(state.getBattlefieldController()).containsEntry("BATTLEFIELD", "p2");
    assertThat(player(state, "p1").getScore()).isZero();
  }

  @Test
  void resolvingShowdownStartsDamageAssignmentBeforeCleanup() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().step()).isEqualTo(ShowdownStep.ASSIGN_DAMAGE);
    assertThat(state.getActiveShowdown().assigningPlayerId()).isEqualTo("p1");
  }

  @Test
  void attackerAssignsBeforeDefenderThenShowdownResolves() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));
    engine.applyMove(state, assign("p1", "attacker", "defender", 3));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().assigningPlayerId()).isEqualTo("p2");
    assertThat(state.getActiveShowdown().attackerAssignments()).hasSize(1);

    engine.applyMove(state, assign("p2", "defender", "attacker", 1));

    assertThat(state.getActiveShowdown()).isNull();
    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
  }

  @Test
  void actionDuringShowdownFocusResetsConsecutivePassesAndKeepsShowdownUnresolved() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE, 3),
        card("defender", "p2", ZoneName.BATTLEFIELD, 3),
        card("action", "p2", ZoneName.HAND, 0));
    stubUnit("attacker", 1, 3);
    stubUnit("defender", 1, 3);
    CardDefinition action = new CardDefinition("action", "Focus Action", "Spell", null, List.of(), 0, 0, null, null, null, "[Action] Draw 1.", 0, 0, List.of());
    when(cardDataService.getCard("action")).thenReturn(action);
    when(cardDataService.isActionCard(action)).thenReturn(true);
    when(cardDataService.isReactionCard(action)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("action")).thenReturn(false);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    engine.applyMove(state, new PassShowdownFocusMove("p1"));
    assertThat(state.getActiveShowdown().consecutivePasses()).isEqualTo(1);
    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p2");

    engine.applyMove(state, new PlayCardMove("p2", "action", ZoneName.BASE, 0, 0, null));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().consecutivePasses()).isZero();
    assertThat(state.getActiveShowdown().readyToResolve()).isFalse();
    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
  }

  @Test
  void bystanderCannotPassFocusOrPlayActionDuringShowdownAndStateDoesNotMutate() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE, 3),
        card("defender", "p2", ZoneName.BATTLEFIELD, 3),
        card("action", "p3", ZoneName.HAND, 0));
    PlayerState p3 = new PlayerState();
    p3.setUserId("p3");
    p3.setName("Bystander");
    state.getPlayers().add(p3);
    stubUnit("attacker", 1, 3);
    stubUnit("defender", 1, 3);
    CardDefinition action = new CardDefinition("action", "Bystander Action", "Spell", null, List.of(), 0, 0, null, null, null, "[Action] Draw 1.", 0, 0, List.of());
    when(cardDataService.getCard("action")).thenReturn(action);
    when(cardDataService.isActionCard(action)).thenReturn(true);
    when(cardDataService.isReactionCard(action)).thenReturn(false);
    when(cardDataService.isUnsupportedAction("action")).thenReturn(false);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThatThrownBy(() -> engine.applyMove(state, new PassShowdownFocusMove("p3")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the focused player can pass showdown focus.");
    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove("p3", "action", ZoneName.BASE, 0, 0, null)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only showdown participants can play Action cards here.");

    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().consecutivePasses()).isZero();
    assertThat(cardById(state, "action").getZone()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void nonAssigningPlayerCannotAssignDamageAndDoesNotMutate() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), card("defender", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThatThrownBy(() -> engine.applyMove(state, assign("p2", "defender", "attacker", 1)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Wait for your combat damage assignment.");

    assertThat(state.getActiveShowdown().assigningPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().attackerAssignments()).isEmpty();
    assertThat(state.getCards()).filteredOn(card -> card.getInstanceId().equals("attacker") || card.getInstanceId().equals("defender"))
        .allSatisfy(card -> assertThat(card.getZone()).isEqualTo(ZoneName.BATTLEFIELD));
  }

  @Test
  void damageAssignmentMustAssignLethalBeforeSpreading() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE),
        card("defender-one", "p2", ZoneName.BATTLEFIELD),
        card("defender-two", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 4);
    stubUnit("defender-one", 2);
    stubUnit("defender-two", 2);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    AssignCombatDamageMove illegalSplit = new AssignCombatDamageMove("p1", List.of(
        new LiveGameState.CombatDamageAssignment("attacker", "defender-one", 1),
        new LiveGameState.CombatDamageAssignment("attacker", "defender-two", 3)));

    assertThatThrownBy(() -> engine.applyMove(state, illegalSplit))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Assign lethal damage before spreading damage.");
  }

  @Test
  void damageAssignmentMustAssignLethalToTankBeforeNonTank() {
    CardInstance tank = card("tank", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), tank, card("non-tank", "p2", ZoneName.BATTLEFIELD));
    stubUnit("attacker", 4);
    stubUnit("tank", 2);
    stubUnit("non-tank", 2);
    when(cardDataService.hasKeyword(eq(tank), eq("TANK"))).thenReturn(true);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThatThrownBy(() -> engine.applyMove(state, assign("p1", "attacker", "non-tank", 4)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Assign lethal damage to Tank units before non-Tank units.");
  }

  @Test
  void failedShowdownResolveDoesNotMutateCombatState() {
    CardInstance attacker = card("attacker", "p1", ZoneName.BASE);
    CardInstance defender = card("defender", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(attacker, defender);
    stubUnit("attacker", 3);
    stubUnit("defender", 1);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveShowdownMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only the attacking player can resolve this showdown.");

    assertThat(state.getCurrentPhase()).isEqualTo(Phase.MAIN);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().step()).isEqualTo(ShowdownStep.ACTION_WINDOW);
    assertThat(attacker.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(defender.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(attacker.getCurrentHealth()).isEqualTo(3);
    assertThat(defender.getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void deathknellFromAssignedCombatDamageFiresExactlyOnceAndDoesNotLeakDrawnCardName() {
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE, 3), card("lonely-poro", "p2", ZoneName.BATTLEFIELD, 1));
    player(state, "p2").getDeckPool().add("secret-draw");
    stubUnit("attacker", 3, 3);
    when(cardDataService.getCard("lonely-poro")).thenReturn(new CardDefinition("lonely-poro", "Lonely Poro", "Unit", null, List.of(), 0, 0, null, null, null, "[Deathknell] If I died alone, draw 1.", 1, 1, List.of("DEATHKNELL")));
    when(cardDataService.getCard("secret-draw")).thenReturn(new CardDefinition("secret-draw", "Secret Draw", "Unit", null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
    when(cardDataService.hasKeyword("lonely-poro", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "lonely-poro", 3, 1);

    assertThat(state.getLog().stream().filter(entry -> entry.text().equals("Lonely Poro's Deathknell drew 1."))).hasSize(1);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Secret Draw"));
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("secret-draw");
      assertThat(card.getOwnerId()).isEqualTo("p2");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
  }

  @Test
  void recalledDeathknellUnitFromBothSidesSurvivingDoesNotTriggerDeathknell() {
    LiveGameState state = state(card("lonely-poro", "p1", ZoneName.BASE, 3), card("defender", "p2", ZoneName.BATTLEFIELD, 3));
    player(state, "p1").getDeckPool().add("secret-draw");
    when(cardDataService.getCard("lonely-poro")).thenReturn(new CardDefinition("lonely-poro", "Lonely Poro", "Unit", null, List.of(), 0, 0, null, null, null, "[Deathknell] If I died alone, draw 1.", 1, 3, List.of("DEATHKNELL")));
    stubUnit("defender", 1, 3);
    when(cardDataService.hasKeyword("lonely-poro", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "lonely-poro"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "lonely-poro", "defender", 1, 1);

    assertThat(cardById(state, "lonely-poro").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Lonely Poro's Deathknell"));
    assertThat(player(state, "p1").getDeckPool()).containsExactly("secret-draw");
  }

  @Test
  void assignedCombatDeathReturnsAttachedGearToBaseWithoutDeathknell() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE, 3),
        card("host", "p2", ZoneName.BATTLEFIELD, 1),
        attachedGear("gear", "p2", "host"));
    stubUnit("attacker", 3, 3);
    stubUnit("host", 1, 1);
    when(cardDataService.getCard("gear")).thenReturn(new CardDefinition("gear", "Attached Gear", "Gear", null, List.of(), 0, 0, null, null, null, "[Equip] [Deathknell]", 0, 0, List.of("DEATHKNELL")));
    when(cardDataService.hasKeyword("gear", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "host", 3, 1);

    CardInstance gear = cardById(state, "gear");
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Attached Gear's Deathknell"));
  }

  @Test
  void assignedCombatRecallKeepsAttachedGearWithHostUnderCurrentPublicZonePolicy() {
    LiveGameState state = state(
        card("host", "p1", ZoneName.BASE, 3),
        card("defender", "p2", ZoneName.BATTLEFIELD, 3),
        attachedGear("gear", "p1", "host"));
    stubUnit("host", 1, 3);
    stubUnit("defender", 1, 3);
    when(cardDataService.getCard("gear")).thenReturn(new CardDefinition("gear", "Attached Gear", "Gear", null, List.of(), 0, 0, null, null, null, "[Equip]", 0, 0, List.of()));

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "host"));
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "host", "defender", 1, 1);

    CardInstance gear = cardById(state, "gear");
    assertThat(cardById(state, "host").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("host");
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
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker-one", "defender-one", 3, 1);
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
    passShowdownFocusCycle(state);
    resolveShowdownWithAssignments(state, "attacker", "defender", 3, 1);

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
    return card(id, ownerId, zone, 3);
  }

  private CardInstance card(String id, String ownerId, ZoneName zone, int health) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(health);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private CardInstance attachedGear(String id, String ownerId, String hostId) {
    CardInstance gear = card(id, ownerId, ZoneName.BASE, 0);
    gear.setAttachedToInstanceId(hostId);
    return gear;
  }

  private void stubUnit(String id, int might) {
    stubUnit(id, might, might);
  }

  private void stubUnit(String id, int might, int health) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, "Unit", null, List.of(), 0, 0, null, null, null, null, might, health, List.of()));
  }

  private CardInstance cardById(LiveGameState state, String instanceId) {
    return state.getCards().stream()
        .filter(card -> card.getInstanceId().equals(instanceId))
        .findFirst()
        .orElseThrow();
  }

  private PlayerState player(LiveGameState state, String playerId) {
    return state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .orElseThrow();
  }
}
