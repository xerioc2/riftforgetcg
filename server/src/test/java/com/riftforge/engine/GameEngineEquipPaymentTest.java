package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.EquipGearMove;
import com.riftforge.model.move.PlayCardMove;
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
class GameEngineEquipPaymentTest {
  private static final String BOOTS_TEXT =
      "[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)";
  private static final String GUARDIAN_TEXT =
      "[Equip] :rb_rune_calm: (:rb_rune_calm:: Attach this to a unit you control.)";
  private static final String ENERGY_GEAR_TEXT = "[Equip] :rb_energy_2:";
  private static final String ENERGY_AND_PREMIUM_TEXT = "[Equip] :rb_energy_1::rb_rune_chaos:";

  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  GameEngine engine;

  @BeforeEach
  void setUp() {
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(
        cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    engine = new GameEngine(
        rulesValidator, combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenReturn(false);
    when(cardDataService.isEquip(any(CardDefinition.class)))
        .thenAnswer(invocation -> {
          CardDefinition def = invocation.getArgument(0);
          return def != null && "Gear".equalsIgnoreCase(def.type());
        });
  }

  @Test
  void printedChaosPremiumEquipsAndSpendsExactlyOnce() {
    LiveGameState state = state(
        gear("boots-i", "boots"),
        unit("unit-i", "unit"),
        rune("chaos-r", "chaos-rune", "p1"));
    stubGear("boots", BOOTS_TEXT);
    stubUnit("unit");
    stubRune("chaos-rune", "CHAOS");

    engine.applyMove(state, equip("boots-i", "unit-i", List.of(), List.of("chaos-r")));

    CardInstance boots = findCard(state, "boots-i");
    assertThat(boots.getAttachedToInstanceId()).isEqualTo("unit-i");
    assertThat(boots.getZone()).isEqualTo(ZoneName.BASE);
    // Premium rune is consumed exactly once: removed from play and returned to the rune pool.
    assertThat(state.getRunes()).isEmpty();
    assertThat(player(state).getRuneDeckPool()).containsExactly("chaos-rune");
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  @Test
  void printedCalmPremiumEquipsSuccessfully() {
    LiveGameState state = state(
        gear("ga-i", "guardian"),
        unit("unit-i", "unit"),
        rune("calm-r", "calm-rune", "p1"));
    stubGear("guardian", GUARDIAN_TEXT);
    stubUnit("unit");
    stubRune("calm-rune", "CALM");

    engine.applyMove(state, equip("ga-i", "unit-i", List.of(), List.of("calm-r")));

    assertThat(findCard(state, "ga-i").getAttachedToInstanceId()).isEqualTo("unit-i");
    assertThat(state.getRunes()).isEmpty();
  }

  @Test
  void wrongDomainRejectsWithoutSpendingOrAttaching() {
    LiveGameState state = state(
        gear("boots-i", "boots"),
        unit("unit-i", "unit"),
        rune("calm-r", "calm-rune", "p1"));
    stubGear("boots", BOOTS_TEXT);
    stubUnit("unit");
    stubRune("calm-rune", "CALM");

    assertThatThrownBy(() -> engine.applyMove(state, equip("boots-i", "unit-i", List.of(), List.of("calm-r"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equip payment uses the wrong rune domain.");

    assertNoEquipMutation(state, "boots-i");
  }

  @Test
  void missingRequiredPremiumRejectsWithoutMutation() {
    LiveGameState state = state(
        gear("boots-i", "boots"),
        unit("unit-i", "unit"),
        rune("chaos-r", "chaos-rune", "p1"));
    stubGear("boots", BOOTS_TEXT);
    stubUnit("unit");
    stubRune("chaos-rune", "CHAOS");

    assertThatThrownBy(() -> engine.applyMove(state, equip("boots-i", "unit-i", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient equip payment.");

    assertNoEquipMutation(state, "boots-i");
  }

  @Test
  void insufficientEnergyRejectsWithoutMutation() {
    LiveGameState state = state(gear("gear-i", "energy-gear"), unit("unit-i", "unit"));
    stubGear("energy-gear", ENERGY_GEAR_TEXT);
    stubUnit("unit");
    player(state).setAvailableEnergy(0);

    assertThatThrownBy(() -> engine.applyMove(state, equip("gear-i", "unit-i", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient equip payment.");

    assertThat(findCard(state, "gear-i").getAttachedToInstanceId()).isNull();
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  @Test
  void opponentRuneRejectsWithoutMutation() {
    LiveGameState state = state(
        gear("boots-i", "boots"),
        unit("unit-i", "unit"),
        rune("chaos-r", "chaos-rune", "p2"));
    stubGear("boots", BOOTS_TEXT);
    stubUnit("unit");
    stubRune("chaos-rune", "CHAOS");

    assertThatThrownBy(() -> engine.applyMove(state, equip("boots-i", "unit-i", List.of(), List.of("chaos-r"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You cannot pay with an opponent's rune.");

    assertNoEquipMutation(state, "boots-i");
  }

  @Test
  void tappedRuneRejectsWithoutMutation() {
    RuneState tapped = rune("chaos-r", "chaos-rune", "p1");
    tapped.setTapped(true);
    LiveGameState state = state(gear("boots-i", "boots"), unit("unit-i", "unit"), tapped);
    stubGear("boots", BOOTS_TEXT);
    stubUnit("unit");
    stubRune("chaos-rune", "CHAOS");

    assertThatThrownBy(() -> engine.applyMove(state, equip("boots-i", "unit-i", List.of(), List.of("chaos-r"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Payment rune is already tapped.");

    assertThat(findCard(state, "boots-i").getAttachedToInstanceId()).isNull();
  }

  @Test
  void duplicateRuneRejectedWithoutMutation() {
    LiveGameState state = state(
        gear("gear-i", "energy-premium-gear"),
        unit("unit-i", "unit"),
        rune("chaos-r", "chaos-rune", "p1"));
    stubGear("energy-premium-gear", ENERGY_AND_PREMIUM_TEXT);
    stubUnit("unit");
    stubRune("chaos-rune", "CHAOS");

    assertThatThrownBy(() ->
        engine.applyMove(state, equip("gear-i", "unit-i", List.of("chaos-r"), List.of("chaos-r"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Payment cannot use the same rune twice.");

    assertNoEquipMutation(state, "gear-i");
  }

  @Test
  void playingGearFromHandUsesPlayCostNotEquipCost() {
    CardInstance gear = gear("gear-i", "split-cost-gear");
    gear.setZone(ZoneName.HAND);
    LiveGameState state = state(gear, unit("unit-i", "unit"));
    player(state).setAvailableEnergy(1);
    stubGear("split-cost-gear", ENERGY_GEAR_TEXT, 1);
    stubUnit("unit");

    engine.applyMove(state, new PlayCardMove("p1", "gear-i", ZoneName.BASE, 12, 24, null));

    CardInstance played = findCard(state, "gear-i");
    assertThat(played.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(played.getAttachedToInstanceId()).isNull();
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  @Test
  void equippingGearFromBaseUsesEquipCostNotPlayCost() {
    LiveGameState state = state(gear("gear-i", "split-cost-gear"), unit("unit-i", "unit"));
    player(state).setAvailableEnergy(1);
    stubGear("split-cost-gear", "[Equip] :rb_energy_1:", 3);
    stubUnit("unit");

    engine.applyMove(state, equip("gear-i", "unit-i", List.of(), List.of()));

    assertThat(findCard(state, "gear-i").getAttachedToInstanceId()).isEqualTo("unit-i");
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  @Test
  void exactEnergyEquipCostCanUseSelectedNormalRune() {
    LiveGameState state = state(
        gear("gear-i", "energy-gear"),
        unit("unit-i", "unit"),
        rune("normal-r", "calm-rune", "p1"));
    player(state).setAvailableEnergy(1);
    stubGear("energy-gear", ENERGY_GEAR_TEXT);
    stubUnit("unit");
    stubRune("calm-rune", "CALM");

    engine.applyMove(state, equip("gear-i", "unit-i", List.of("normal-r"), List.of()));

    assertThat(findCard(state, "gear-i").getAttachedToInstanceId()).isEqualTo("unit-i");
    assertThat(state.getRunes()).singleElement().satisfies(rune -> assertThat(rune.isTapped()).isTrue());
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  private void assertNoEquipMutation(LiveGameState state, String gearInstanceId) {
    CardInstance gear = findCard(state, gearInstanceId);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getRunes()).isNotEmpty();
    assertThat(state.getRunes()).allMatch(rune -> !rune.isTapped());
    assertThat(player(state).getRuneDeckPool()).isEmpty();
  }

  private LiveGameState state(CardInstance gear, CardInstance unit, RuneState... runes) {
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
    state.setCards(new ArrayList<>(List.of(gear, unit)));
    state.setRunes(new ArrayList<>(List.of(runes)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private PlayerState player(LiveGameState state) {
    return state.getPlayers().stream()
        .filter(candidate -> "p1".equals(candidate.getUserId()))
        .findFirst()
        .orElseThrow();
  }

  private CardInstance findCard(LiveGameState state, String instanceId) {
    return state.getCards().stream()
        .filter(card -> instanceId.equals(card.getInstanceId()))
        .findFirst()
        .orElseThrow();
  }

  private CardInstance gear(String instanceId, String cardId) {
    CardInstance gear = new CardInstance();
    gear.setInstanceId(instanceId);
    gear.setCardId(cardId);
    gear.setOwnerId("p1");
    gear.setZone(ZoneName.BASE);
    gear.setCurrentHealth(1);
    gear.setTempKeywords(new ArrayList<>());
    return gear;
  }

  private CardInstance unit(String instanceId, String cardId) {
    CardInstance unit = new CardInstance();
    unit.setInstanceId(instanceId);
    unit.setCardId(cardId);
    unit.setOwnerId("p1");
    unit.setZone(ZoneName.BASE);
    unit.setCurrentHealth(3);
    unit.setTempKeywords(new ArrayList<>());
    return unit;
  }

  private RuneState rune(String instanceId, String cardId, String ownerId) {
    RuneState rune = new RuneState();
    rune.setInstanceId(instanceId);
    rune.setCardId(cardId);
    rune.setOwnerId(ownerId);
    rune.setNormalEnergy(1);
    rune.setPremiumEnergy(2);
    return rune;
  }

  private EquipGearMove equip(
      String gearInstanceId, String targetInstanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    return new EquipGearMove("p1", gearInstanceId, targetInstanceId, paymentRuneIds, premiumRuneIds);
  }

  private void stubGear(String cardId, String rulesText) {
    stubGear(cardId, rulesText, 0);
  }

  private void stubGear(String cardId, String rulesText, int playCost) {
    when(cardDataService.getCard(cardId)).thenReturn(
        new CardDefinition(cardId, cardId, "Gear", null, List.of(), playCost, 0, null, null, null, rulesText, 0, 0, List.of()));
  }

  private void stubUnit(String cardId) {
    when(cardDataService.getCard(cardId)).thenReturn(
        new CardDefinition(cardId, cardId, "Unit", null, List.of(), 1, 0, null, null, null, null, 1, 3, List.of()));
  }

  private void stubRune(String cardId, String domain) {
    when(cardDataService.getCard(cardId)).thenReturn(
        new CardDefinition(cardId, cardId, "Rune", null, List.of(domain), 0, 0, null, null, null, null, 0, 0, List.of()));
  }
}
