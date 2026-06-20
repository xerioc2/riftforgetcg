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
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.ActivateAbilityMove;
import com.riftforge.service.CardDataService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivatedAbilityServiceTest {
  private static final String SYREN_TEXT = ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.";
  private static final String ZHONYA_TEXT = "[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)"
      + "If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it. (Send it to base. This isn't a move.)";
  private static final String IRELIA_TEXT = "When you choose a friendly unit, you may exhaust me and pay :rb_rune_rainbow: to ready it."
      + "When you conquer, you may pay :rb_energy_1: to ready me.";
  private static final String DIANA_TEXT = "[Reaction][>] :rb_exhaust:: [Add] :rb_energy_1:. Spend this Energy only during showdowns. (Abilities that add resources can't be reacted to.)";

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
    when(cardDataService.isZhonyasHourglass(any())).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Zhonya's Hourglass".equals(def.name());
    });
    when(cardDataService.isIreliaBladeDancerLegend(any())).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Irelia - Blade Dancer".equals(def.name());
    });
    when(cardDataService.isDianaScornShowdownEnergyLegend(any())).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.name() != null && def.name().startsWith("Diana - Scorn of the Moon");
    });
    when(cardDataService.getCard("syren-card")).thenReturn(card("syren-card", "The Syren", "Gear", SYREN_TEXT));
    when(cardDataService.getCard("zhonya-card")).thenReturn(card("zhonya-card", "Zhonya's Hourglass", "Gear", ZHONYA_TEXT));
    when(cardDataService.getCard("irelia-card")).thenReturn(card("irelia-card", "Irelia - Blade Dancer", "Legend", IRELIA_TEXT));
    when(cardDataService.getCard("diana-card")).thenReturn(card("diana-card", "Diana - Scorn of the Moon", "Legend", DIANA_TEXT));
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
      assertThat(definition.premiumCost()).isZero();
      assertThat(definition.requiresExhaust()).isTrue();
      assertThat(definition.timing()).isEqualTo(ActivatedAbilityTiming.MAIN_PHASE_IMMEDIATE);
      assertThat(definition.targetKind()).isEqualTo(ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_BATTLEFIELD_UNIT);
      assertThat(definition.reactable()).isFalse();
    });
  }

  @Test
  void exactCardRegistryExposesZhonyasHourglassAbilityDefinition() {
    List<ActivatedAbilityDefinition> definitions = service.definitionsFor(card("zhonya-card", "Zhonya's Hourglass", "Gear", ZHONYA_TEXT));

    assertThat(definitions).singleElement().satisfies(definition -> {
      assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT);
      assertThat(definition.sourceZone()).isEqualTo(ZoneName.BASE);
      assertThat(definition.energyCost()).isZero();
      assertThat(definition.premiumCost()).isZero();
      assertThat(definition.requiresExhaust()).isFalse();
      assertThat(definition.timing()).isEqualTo(ActivatedAbilityTiming.MAIN_PHASE_IMMEDIATE);
      assertThat(definition.targetKind()).isEqualTo(ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_PLAY_UNIT);
      assertThat(definition.reactable()).isFalse();
    });
  }

  @Test
  void exactCardRegistryExposesIreliaBladeDancerReadyAbilityDefinition() {
    List<ActivatedAbilityDefinition> definitions = service.definitionsFor(card("irelia-card", "Irelia - Blade Dancer", "Legend", IRELIA_TEXT));

    assertThat(definitions).singleElement().satisfies(definition -> {
      assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT);
      assertThat(definition.sourceZone()).isEqualTo(ZoneName.LEGEND);
      assertThat(definition.energyCost()).isZero();
      assertThat(definition.premiumCost()).isEqualTo(1);
      assertThat(definition.requiresExhaust()).isTrue();
      assertThat(definition.timing()).isEqualTo(ActivatedAbilityTiming.MAIN_PHASE_IMMEDIATE);
      assertThat(definition.targetKind()).isEqualTo(ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_PLAY_TAPPED_UNIT);
      assertThat(definition.reactable()).isFalse();
    });
  }

  @Test
  void exactCardRegistryExposesDianaScornShowdownEnergyAbilityDefinition() {
    List<ActivatedAbilityDefinition> definitions = service.definitionsFor(card("diana-card", "Diana - Scorn of the Moon", "Legend", DIANA_TEXT));

    assertThat(definitions).singleElement().satisfies(definition -> {
      assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY);
      assertThat(definition.sourceZone()).isEqualTo(ZoneName.LEGEND);
      assertThat(definition.energyCost()).isZero();
      assertThat(definition.premiumCost()).isZero();
      assertThat(definition.requiresExhaust()).isTrue();
      assertThat(definition.timing()).isEqualTo(ActivatedAbilityTiming.SHOWDOWN_FOCUS_RESOURCE);
      assertThat(definition.targetKind()).isEqualTo(ActivatedAbilityTargetKind.NONE);
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
  void validatesLegalZhonyasHourglassActivationWithoutPayment() {
    LiveGameState state = state(source("zhonya", "zhonya-card", "p1", ZoneName.BASE), unit("unit", "p1", ZoneName.BASE));

    ActivatedAbilityDefinition definition = service.validate(
        state,
        new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "unit", List.of(), List.of()));

    assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT);
  }

  @Test
  void validatesLegalIreliaActivationWithPremiumRune() {
    CardInstance target = unit("unit", "p1", ZoneName.BASE);
    target.setTapped(true);
    LiveGameState state = state(source("irelia", "irelia-card", "p1", ZoneName.LEGEND), target);
    state.setRunes(new java.util.ArrayList<>(List.of(rune("rune-1", "p1", false))));

    ActivatedAbilityDefinition definition = service.validate(
        state,
        new ActivateAbilityMove("p1", "irelia", ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT, "unit", List.of(), List.of("rune-1")));

    assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT);
  }

  @Test
  void validatesLegalDianaActivationDuringFocusedShowdownWithoutTargetOrPayment() {
    LiveGameState state = state(source("diana", "diana-card", "p1", ZoneName.LEGEND));
    state.setActiveShowdown(showdown("p1"));

    ActivatedAbilityDefinition definition = service.validate(
        state,
        new ActivateAbilityMove("p1", "diana", ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY, null, List.of(), List.of()));

    assertThat(definition.abilityKey()).isEqualTo(ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY);
  }

  @Test
  void rejectsDianaActivationOutsideFocusedShowdownOrWithTargetWithoutMutation() {
    CardInstance diana = source("diana", "diana-card", "p1", ZoneName.LEGEND);
    LiveGameState state = state(diana);

    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "diana", ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY, null, List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("only during a showdown");
    assertThat(diana.isTapped()).isFalse();

    state.setActiveShowdown(showdown("p2"));
    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "diana", ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY, null, List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("focused showdown player");
    assertThat(diana.isTapped()).isFalse();

    state.setActiveShowdown(showdown("p1"));
    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "diana", ActivatedAbilityService.DIANA_SCORN_SHOWDOWN_ENERGY, "unit", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("does not use a target");
    assertThat(diana.isTapped()).isFalse();
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
  void rejectsIreliaActivationWithoutPremiumOrTappedTargetWithoutMutation() {
    CardInstance irelia = source("irelia", "irelia-card", "p1", ZoneName.LEGEND);
    CardInstance target = unit("unit", "p1", ZoneName.BASE);
    target.setTapped(true);
    LiveGameState state = state(irelia, target);

    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "irelia", ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT, "unit", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("premium rune payment");
    assertThat(irelia.isTapped()).isFalse();
    assertThat(target.isTapped()).isTrue();

    state.setRunes(new java.util.ArrayList<>(List.of(rune("rune-1", "p1", false))));
    target.setTapped(false);
    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "irelia", ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT, "unit", List.of(), List.of("rune-1"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("exhausted friendly public Unit");
    assertThat(irelia.isTapped()).isFalse();
  }

  @Test
  void rejectsZhonyasHourglassEnemyOrPrivateTargetWithoutMutation() {
    CardInstance zhonya = source("zhonya", "zhonya-card", "p1", ZoneName.BASE);
    CardInstance enemy = unit("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(zhonya, enemy);

    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "enemy", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("friendly public Unit or Champion");
    assertThat(zhonya.isTapped()).isFalse();
    assertThat(enemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);

    CardInstance hidden = unit("hidden", "p1", ZoneName.BASE);
    hidden.setFaceDown(true);
    state.getCards().add(hidden);

    assertThatThrownBy(() -> service.validate(
        state,
        new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "hidden", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("friendly public Unit or Champion");
    assertThat(hidden.getZone()).isEqualTo(ZoneName.BASE);
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

  @Test
  void legalActivationIncludesZhonyasHourglassWithFriendlyBaseOrBattlefieldTarget() {
    LiveGameState state = state(source("zhonya", "zhonya-card", "p1", ZoneName.BASE), unit("unit", "p1", ZoneName.BASE));

    assertThat(service.hasLegalActivation(state, "p1")).isTrue();

    state.getCards().stream()
        .filter(card -> "unit".equals(card.getInstanceId()))
        .findFirst()
        .orElseThrow()
        .setZone(ZoneName.HAND);
    assertThat(service.hasLegalActivation(state, "p1")).isFalse();
  }

  @Test
  void legalActivationIncludesIreliaOnlyWithReadyLegendTappedTargetAndPremiumRune() {
    CardInstance irelia = source("irelia", "irelia-card", "p1", ZoneName.LEGEND);
    CardInstance target = unit("unit", "p1", ZoneName.BASE);
    target.setTapped(true);
    LiveGameState state = state(irelia, target);

    assertThat(service.hasLegalActivation(state, "p1")).isFalse();

    state.setRunes(new java.util.ArrayList<>(List.of(rune("rune-1", "p1", false))));
    assertThat(service.hasLegalActivation(state, "p1")).isTrue();

    irelia.setTapped(true);
    assertThat(service.hasLegalActivation(state, "p1")).isFalse();
  }

  @Test
  void legalActivationIncludesDianaOnlyDuringFocusedShowdownWithReadyLegend() {
    CardInstance diana = source("diana", "diana-card", "p1", ZoneName.LEGEND);
    LiveGameState state = state(diana);

    assertThat(service.hasLegalActivation(state, "p1")).isFalse();

    state.setActiveShowdown(showdown("p1"));
    assertThat(service.hasLegalActivation(state, "p1")).isTrue();
    assertThat(service.hasLegalActivation(state, "p2")).isFalse();

    diana.setTapped(true);
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

  private RuneState rune(String id, String ownerId, boolean tapped) {
    RuneState rune = new RuneState();
    rune.setInstanceId(id);
    rune.setOwnerId(ownerId);
    rune.setTapped(tapped);
    rune.setNormalEnergy(1);
    return rune;
  }

  private LiveGameState.ShowdownState showdown(String focusedPlayerId) {
    return new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        new java.util.HashMap<>(),
        ShowdownStep.ACTION_WINDOW,
        List.of("p1", "p2"),
        focusedPlayerId,
        0,
        false,
        null,
        List.of(),
        List.of(),
        "bf-0");
  }

  private CardInstance source(String instanceId, String ownerId, ZoneName zone) {
    return source(instanceId, "syren-card", ownerId, zone);
  }

  private CardInstance source(String instanceId, String cardId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
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
