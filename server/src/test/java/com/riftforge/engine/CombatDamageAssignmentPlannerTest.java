package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class CombatDamageAssignmentPlannerTest {
  @Mock CardDataService cardDataService;

  private CombatDamageAssignmentPlanner planner;
  private RulesValidator validator;

  @BeforeEach
  void setUp() {
    CombatStatsService stats = new CombatStatsService(cardDataService);
    CombatDamageRules damageRules = new CombatDamageRules(cardDataService);
    planner = new CombatDamageAssignmentPlanner(cardDataService, stats, damageRules);
    validator = new RulesValidator(cardDataService);
    when(cardDataService.hasKeyword(org.mockito.ArgumentMatchers.any(CardInstance.class), anyString())).thenReturn(false);
  }

  @Test
  void insufficientDamageForFirstTargetAssignsAllDamageToThatTarget() {
    LiveGameState state = assignmentState("bot", "bot",
        unit("source", "bot", 2, 2),
        unit("target-one", "human", 1, 3),
        unit("target-two", "human", 1, 1));

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target-one", 1),
        new LiveGameState.CombatDamageAssignment("bot", "target-two", 1));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void enoughDamageKillsFirstTargetBeforeAssigningToSecond() {
    LiveGameState state = assignmentState("bot", "bot",
        unit("source-one", "bot", 2, 2),
        unit("source-two", "bot", 2, 2),
        unit("target-one", "human", 1, 2),
        unit("target-two", "human", 1, 2));

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target-one", 1),
        new LiveGameState.CombatDamageAssignment("bot", "target-two", 3));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void moreTargetsThanTotalMightDoesNotSpreadPartialDamageIllegally() {
    LiveGameState state = assignmentState("bot", "bot",
        unit("source-one", "bot", 2, 2),
        unit("source-two", "bot", 2, 2),
        unit("target-one", "human", 1, 3),
        unit("target-two", "human", 1, 5));

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target-one", 1),
        new LiveGameState.CombatDamageAssignment("bot", "target-two", 3));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void tankTargetReceivesLethalBeforeNonTankTarget() {
    CardInstance tank = unit("tank", "human", 1, 2);
    LiveGameState state = assignmentState("bot", "bot",
        unit("source-one", "bot", 2, 2),
        unit("source-two", "bot", 5, 5),
        tank,
        unit("non-tank", "human", 1, 5));
    when(cardDataService.hasKeyword(eq(tank), eq("TANK"))).thenReturn(true);

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "tank", 1),
        new LiveGameState.CombatDamageAssignment("bot", "non-tank", 6));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void defenderSideAssignmentUsesDefendingMightAndStillRespectsLethalPolicy() {
    CardInstance defender = unit("defender", "bot", 2, 2);
    LiveGameState state = assignmentState("human", "bot",
        defender,
        unit("attacker-one", "human", 1, 3),
        unit("attacker-two", "human", 1, 5));
    when(cardDataService.getKeywordValue(defender, "SHIELD")).thenReturn(2);

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "attacker-one", 1),
        new LiveGameState.CombatDamageAssignment("bot", "attacker-two", 3));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void moreSourcesThanTargetsAssignsAllMight() {
    LiveGameState state = assignmentState("bot", "bot",
        unit("source-one", "bot", 3, 3),
        unit("source-two", "bot", 3, 3),
        unit("target", "human", 1, 6));

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target", 6));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void plannerOnlyUsesCombatantsAtActiveShowdownLocation() {
    CardInstance sourceHere = atLocation(unit("source-here", "bot", 3, 3), "bf-1");
    CardInstance sourceElsewhere = atLocation(unit("source-elsewhere", "bot", 9, 9), "bf-2");
    CardInstance targetHere = atLocation(unit("target-here", "human", 1, 3), "bf-1");
    CardInstance targetElsewhere = atLocation(unit("target-elsewhere", "human", 1, 9), "bf-2");
    LiveGameState state = assignmentState(
        "human",
        "bot",
        "bf-1",
        sourceHere,
        sourceElsewhere,
        targetHere,
        targetElsewhere);

    List<LiveGameState.CombatDamageAssignment> assignments = plan(state, "bot");

    assertThat(assignments).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target-here", 3));
    assertAccepted(state, "bot", assignments);
  }

  @Test
  void projectedAssignmentStateExcludesOffLocationCardsAndAttachedGear() {
    CardInstance sourceHere = atLocation(unit("source-here", "bot", 3, 3), "bf-1");
    CardInstance sourceElsewhere = atLocation(unit("source-elsewhere", "bot", 9, 9), "bf-2");
    CardInstance targetHere = atLocation(unit("target-here", "human", 1, 3), "bf-1");
    CardInstance targetElsewhere = atLocation(unit("target-elsewhere", "human", 1, 9), "bf-2");
    CardInstance attachedGear = atLocation(card("attached-gear", "bot", "attached-gear-card", ZoneName.BATTLEFIELD), "bf-1");
    attachedGear.setAttachedToInstanceId("source-here");
    when(cardDataService.getCard("attached-gear-card"))
        .thenReturn(new CardDefinition("attached-gear-card", "Attached Gear", "Gear", null, List.of(), 0, 0, null, null, null, "[Equip]", 0, 0, List.of()));
    LiveGameState state = assignmentState(
        "human",
        "bot",
        "bf-1",
        sourceHere,
        sourceElsewhere,
        targetHere,
        targetElsewhere,
        attachedGear);

    LiveGameState.CombatAssignmentState assignmentState = planner.assignmentState(state, "bot").orElseThrow();

    assertThat(assignmentState.locationId()).isEqualTo("bf-1");
    assertThat(assignmentState.assigningPlayerId()).isEqualTo("bot");
    assertThat(assignmentState.validSources())
        .extracting(LiveGameState.CombatDamageSourceOption::sourceInstanceId)
        .containsExactly("source-here");
    assertThat(assignmentState.validTargetInstanceIds()).containsExactly("target-here");
    assertThat(assignmentState.damagePool()).isEqualTo(3);
    assertThat(assignmentState.validTargets()).containsExactly(
        new LiveGameState.CombatDamageTargetOption("target-here", 1, false));
    assertThat(assignmentState.suggestedAssignments()).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot", "target-here", 3));
    assertThat(assignmentState.canAutoAssign()).isTrue();
  }

  @Test
  void zeroDamagePoolReturnsEmptyAutoAssignment() {
    CardInstance stunned = unit("stunned", "bot", 2, 2);
    LiveGameState state = assignmentState("bot", "bot",
        stunned,
        unit("target", "human", 1, 5));
    when(cardDataService.hasKeyword(eq(stunned), eq("STUN"))).thenReturn(true);

    Optional<List<LiveGameState.CombatDamageAssignment>> assignments = planner.plan(state, "bot");

    assertThat(assignments).contains(List.of());
  }

  private List<LiveGameState.CombatDamageAssignment> plan(LiveGameState state, String playerId) {
    return planner.plan(state, playerId).orElseThrow();
  }

  private void assertAccepted(
      LiveGameState state,
      String playerId,
      List<LiveGameState.CombatDamageAssignment> assignments) {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state, new AssignCombatDamageMove(playerId, assignments)));
  }

  private LiveGameState assignmentState(String attackerId, String assigningPlayerId, CardInstance... cards) {
    return assignmentState(attackerId, assigningPlayerId, CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID, cards);
  }

  private LiveGameState assignmentState(String attackerId, String assigningPlayerId, String locationId, CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(assigningPlayerId);
    state.setPlayers(new ArrayList<>(List.of(player("bot"), player("human"))));
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        attackerId,
        List.of(),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of("bot", "human"),
        attackerId,
        2,
        true,
        assigningPlayerId,
        List.of(),
        List.of(),
        locationId));
    return state;
  }

  private PlayerState player(String id) {
    PlayerState player = new PlayerState();
    player.setUserId(id);
    return player;
  }

  private CardInstance unit(String id, String ownerId, int might, int health) {
    CardInstance card = card(id, ownerId, id, ZoneName.BATTLEFIELD);
    card.setCurrentHealth(health);
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, "Unit", null, List.of(), 0, 0, null, null, null, null, might, health, List.of()));
    return card;
  }

  private CardInstance card(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }

  private CardInstance atLocation(CardInstance card, String locationId) {
    card.setBattlefieldLocationId(locationId);
    return card;
  }
}
