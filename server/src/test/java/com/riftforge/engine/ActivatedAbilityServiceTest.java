package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.ActivateAbilityMove;
import com.riftforge.service.CardDataService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivatedAbilityServiceTest {
  private static final String SYREN_TEXT = ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.";

  private CardDataService cardDataService;
  private ActivatedAbilityService service;

  @BeforeEach
  void setUp() {
    cardDataService = mock(CardDataService.class);
    service = new ActivatedAbilityService(cardDataService);
    when(cardDataService.isTheSyrenActivatedAbility(any())).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "The Syren".equals(def.name());
    });
    when(cardDataService.getCard("syren-card")).thenReturn(card("syren-card", "The Syren", "Gear", SYREN_TEXT));
    when(cardDataService.getCard("unit-card")).thenReturn(card("unit-card", "Friendly Unit", "Unit", ""));
    when(cardDataService.getCard("enemy-card")).thenReturn(card("enemy-card", "Enemy Unit", "Unit", ""));
  }

  @Test
  void exactCardRegistryExposesTheSyrenAbilityDefinition() {
    List<ActivatedAbilityDefinition> definitions = service.definitionsFor(card("syren-card", "The Syren", "Gear", SYREN_TEXT));

    assertThat(definitions).singleElement().satisfies(definition -> {
      assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.THE_SYREN_RECALL);
      assertThat(definition.sourceZone()).isEqualTo(ZoneName.BASE);
      assertThat(definition.energyCost()).isEqualTo(1);
      assertThat(definition.requiresExhaust()).isTrue();
      assertThat(definition.timing()).isEqualTo(ActivatedAbilityTiming.MAIN_PHASE_IMMEDIATE);
      assertThat(definition.targetKind()).isEqualTo(ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_BATTLEFIELD_UNIT);
      assertThat(definition.reactable()).isFalse();
    });
  }

  @Test
  void validatesLegalImmediateActivation() {
    LiveGameState state = state(source("syren", "p1", ZoneName.BASE), unit("unit", "p1", ZoneName.BATTLEFIELD));
    state.getPlayers().getFirst().setAvailableEnergy(1);

    ActivatedAbilityDefinition definition = service.validate(
        state,
        new ActivateAbilityMove("p1", "syren", ActivatedAbilityService.THE_SYREN_RECALL, "unit", List.of(), List.of()));

    assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.THE_SYREN_RECALL);
  }

  @Test
  void rejectsNonOwnerWithoutMutatingSourceOrTarget() {
    CardInstance syren = source("syren", "p1", ZoneName.BASE);
    CardInstance unit = unit("unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(syren, unit);

    assertThatThrownBy(() -> service.validate(state, new ActivateAbilityMove("p2", "syren", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("do not own");
    assertThat(syren.isTapped()).isFalse();
    assertThat(unit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void rejectsIllegalZoneAndHiddenSource() {
    LiveGameState handState = state(source("syren", "p1", ZoneName.HAND), unit("unit", "p1", ZoneName.BATTLEFIELD));
    assertThatThrownBy(() -> service.validate(handState, new ActivateAbilityMove("p1", "syren", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("zone");

    CardInstance hidden = source("syren", "p1", ZoneName.BASE);
    hidden.setFaceDown(true);
    LiveGameState hiddenState = state(hidden, unit("unit", "p1", ZoneName.BATTLEFIELD));
    assertThatThrownBy(() -> service.validate(hiddenState, new ActivateAbilityMove("p1", "syren", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("Hidden or face-down");
  }

  @Test
  void rejectsInvalidPaymentAndTargetWithoutMutation() {
    CardInstance syren = source("syren", "p1", ZoneName.BASE);
    CardInstance target = unit("unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(syren, target);

    assertThatThrownBy(() -> service.validate(state, new ActivateAbilityMove("p1", "syren", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("Insufficient energy");
    assertThat(syren.isTapped()).isFalse();
    assertThat(target.getZone()).isEqualTo(ZoneName.BATTLEFIELD);

    state.getPlayers().getFirst().setAvailableEnergy(1);
    CardInstance enemy = unit("enemy", "p2", ZoneName.BATTLEFIELD);
    state.getCards().add(enemy);
    assertThatThrownBy(() -> service.validate(state, new ActivateAbilityMove("p1", "syren", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("friendly public Unit");
    assertThat(syren.isTapped()).isFalse();
    assertThat(enemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void legalActivationRequiresPublicMainPhaseSourceTargetAndPayment() {
    LiveGameState state = state(source("syren", "p1", ZoneName.BASE), unit("unit", "p1", ZoneName.BATTLEFIELD));
    assertThat(service.hasLegalActivation(state, "p1")).isFalse();

    state.getPlayers().getFirst().setAvailableEnergy(1);
    assertThat(service.hasLegalActivation(state, "p1")).isTrue();

    state.setCurrentPhase(Phase.AWAKEN);
    assertThat(service.hasLegalActivation(state, "p1")).isFalse();
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    PlayerState player = new PlayerState();
    player.setUserId("p1");
    PlayerState opponent = new PlayerState();
    opponent.setUserId("p2");
    state.setPlayers(List.of(player, opponent));
    state.setCards(new java.util.ArrayList<>(List.of(cards)));
    state.setRunes(List.of());
    return state;
  }

  private CardInstance source(String instanceId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId("syren-card");
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }

  private CardInstance unit(String instanceId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(ownerId.equals("p1") ? "unit-card" : "enemy-card");
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }

  private CardDefinition card(String id, String name, String type, String rulesText) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, "COMMON", "Test", null, rulesText, 1, 1, List.of());
  }
}
