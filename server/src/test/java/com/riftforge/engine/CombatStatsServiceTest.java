package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.effect.EffectHandlerRegistry;
import com.riftforge.engine.CombatStatsService.CombatContext;
import com.riftforge.engine.keyword.AssaultHandler;
import com.riftforge.engine.keyword.ShieldHandler;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CombatStatsServiceTest {
  private CardDataService cardDataService;
  private CombatStatsService combatStatsService;

  @BeforeEach
  void setUp() {
    cardDataService = new CardDataService(
        new ObjectMapper(),
        "http://example.invalid",
        new EffectHandlerRegistry(List.of(new AssaultHandler(), new ShieldHandler())));
    combatStatsService = new CombatStatsService(cardDataService);
  }

  @Test
  void daringPoroGetsPlusOneOnlyWhileAttacking() throws Exception {
    install(card("daring-poro", "Daring Poro", "Unit", 1, 1, List.of("Assault")));
    CardInstance poro = instance("daring-poro");

    assertThat(combatStatsService.effectiveMight(poro, CombatContext.ATTACKING)).isEqualTo(2);
    assertThat(combatStatsService.effectiveMight(poro, CombatContext.DEFENDING)).isEqualTo(1);
    assertThat(combatStatsService.effectiveMight(poro, CombatContext.IDLE)).isEqualTo(1);
  }

  @Test
  void laurentDuelistGetsPlusTwoOnlyWhileAttacking() throws Exception {
    install(card("laurent-duelist", "Laurent Duelist", "Unit", 2, 2, List.of("Assault 2")));
    CardInstance duelist = instance("laurent-duelist");

    assertThat(combatStatsService.effectiveMight(duelist, CombatContext.ATTACKING)).isEqualTo(4);
    assertThat(combatStatsService.effectiveMight(duelist, CombatContext.DEFENDING)).isEqualTo(2);
    assertThat(combatStatsService.effectiveMight(duelist, CombatContext.IDLE)).isEqualTo(2);
  }

  @Test
  void shieldTwoAppliesOnlyWhileDefending() throws Exception {
    install(card("fortified-unit", "Fortified Position Target", "Unit", 2, 2, List.of()));
    CardInstance target = instance("fortified-unit");
    target.setTempKeywords(new ArrayList<>(List.of("Shield 2")));

    assertThat(combatStatsService.effectiveMight(target, CombatContext.DEFENDING)).isEqualTo(4);
    assertThat(combatStatsService.effectiveMight(target, CombatContext.ATTACKING)).isEqualTo(2);
    assertThat(combatStatsService.effectiveMight(target, CombatContext.IDLE)).isEqualTo(2);
  }

  @Test
  void mightyIsTrueForUnitOrChampionAtFiveEffectiveMight() throws Exception {
    install(
        card("sunken-temple-unit", "Sunken Temple Unit", "Unit", 5, 5, List.of()),
        card("fiora-worthy", "Fiora - Worthy", "Champion", 5, 5, List.of("Mighty")));

    assertThat(combatStatsService.isMighty(instance("sunken-temple-unit"))).isTrue();
    assertThat(combatStatsService.isMighty(instance("fiora-worthy"))).isTrue();
  }

  @Test
  void mightyIsFalseBelowFiveEffectiveMight() throws Exception {
    install(card("small-unit", "Small Unit", "Unit", 4, 4, List.of()));

    assertThat(combatStatsService.isMighty(instance("small-unit"))).isFalse();
  }

  @Test
  void temporaryMightCanMakeUnitMighty() throws Exception {
    install(card("boosted-unit", "Boosted Unit", "Unit", 4, 4, List.of()));
    CardInstance boosted = instance("boosted-unit");
    boosted.setTemporaryPowerModifier(1);

    assertThat(combatStatsService.isMighty(boosted)).isTrue();
  }

  @Test
  void assaultCanMakeUnitMightyOnlyWhileAttacking() throws Exception {
    install(card("assault-mighty", "Assault Mighty", "Unit", 4, 4, List.of("Assault")));
    CardInstance attacker = instance("assault-mighty");

    assertThat(combatStatsService.isMighty(attacker, CombatContext.ATTACKING)).isTrue();
    assertThat(combatStatsService.isMighty(attacker, CombatContext.DEFENDING)).isFalse();
    assertThat(combatStatsService.isMighty(attacker, CombatContext.IDLE)).isFalse();
  }

  @Test
  void shieldCanMakeUnitMightyOnlyWhileDefending() throws Exception {
    install(card("shield-mighty", "Shield Mighty", "Unit", 4, 4, List.of("Shield")));
    CardInstance defender = instance("shield-mighty");

    assertThat(combatStatsService.isMighty(defender, CombatContext.DEFENDING)).isTrue();
    assertThat(combatStatsService.isMighty(defender, CombatContext.ATTACKING)).isFalse();
    assertThat(combatStatsService.isMighty(defender, CombatContext.IDLE)).isFalse();
  }

  @Test
  void nonUnitChampionCardsAreNotMighty() throws Exception {
    install(card("sunken-temple", "Sunken Temple", "Battlefield", 5, 0, List.of("Mighty")));

    assertThat(combatStatsService.isMighty(instance("sunken-temple"))).isFalse();
  }

  @Test
  void stunnedCardsDealNoCombatDamageButStillHaveIdleMight() throws Exception {
    install(card("stunned-unit", "Stunned Unit", "Unit", 5, 5, List.of("Stunned")));
    CardInstance stunned = instance("stunned-unit");

    assertThat(combatStatsService.effectiveMight(stunned, CombatContext.ATTACKING)).isZero();
    assertThat(combatStatsService.effectiveMight(stunned, CombatContext.DEFENDING)).isZero();
    assertThat(combatStatsService.effectiveMight(stunned, CombatContext.IDLE)).isEqualTo(5);
  }

  @Test
  void unmodifiedUnitEffectiveStatsEqualPrintedStats() throws Exception {
    install(card("plain-unit", "Plain Unit", "Unit", 3, 4, List.of()));
    CardInstance unit = instance("plain-unit");
    unit.setCurrentHealth(4);

    CombatStatsService.EffectiveStats stats = combatStatsService.effectiveStats(state(unit), unit, CombatContext.IDLE);

    assertThat(stats.printedMight()).isEqualTo(3);
    assertThat(stats.printedMaxHealth()).isEqualTo(4);
    assertThat(stats.effectiveMight()).isEqualTo(3);
    assertThat(stats.effectiveMaxHealth()).isEqualTo(4);
    assertThat(stats.currentHealth()).isEqualTo(4);
    assertThat(stats.markedDamage()).isZero();
    assertThat(stats.modifiers()).isEmpty();
  }

  @Test
  void attachedUnsupportedGearDoesNotChangeEffectiveStats() throws Exception {
    install(
        card("plain-unit", "Plain Unit", "Unit", 3, 4, List.of()),
        card("unsupported-gear", "Unsupported Gear", "Gear", 0, 0, List.of()));
    CardInstance unit = instance("plain-unit");
    unit.setCurrentHealth(4);
    CardInstance gear = attachedGear("unsupported-gear", unit.getInstanceId());

    CombatStatsService.EffectiveStats stats = combatStatsService.effectiveStats(state(unit, gear), unit, CombatContext.IDLE);

    assertThat(stats.effectiveMight()).isEqualTo(3);
    assertThat(stats.effectiveMaxHealth()).isEqualTo(4);
    assertThat(stats.modifiers()).isEmpty();
  }

  @Test
  void attachedSupportedMightGearChangesEffectiveMight() throws Exception {
    install(
        card("plain-unit", "Plain Unit", "Unit", 3, 4, List.of()),
        card("might-gear", "Might Gear", "Gear", 0, 0, List.of()));
    CombatStatsService statsService = new CombatStatsService(
        cardDataService,
        new EquipmentStatModifierRegistry(Map.of("might-gear", new EquipmentStatModifierRegistry.StatModifier(2, 0))));
    CardInstance unit = instance("plain-unit");
    unit.setCurrentHealth(4);
    CardInstance gear = attachedGear("might-gear", unit.getInstanceId());

    CombatStatsService.EffectiveStats stats = statsService.effectiveStats(state(unit, gear), unit, CombatContext.IDLE);

    assertThat(stats.effectiveMight()).isEqualTo(5);
    assertThat(stats.effectiveMaxHealth()).isEqualTo(4);
    assertThat(stats.modifiers()).singleElement().satisfies(modifier -> {
      assertThat(modifier.sourceCardId()).isEqualTo("might-gear");
      assertThat(modifier.sourceName()).isEqualTo("Might Gear");
      assertThat(modifier.mightBonus()).isEqualTo(2);
    });
  }

  @Test
  void removingGearRemovesStatModifier() throws Exception {
    install(
        card("plain-unit", "Plain Unit", "Unit", 3, 4, List.of()),
        card("might-gear", "Might Gear", "Gear", 0, 0, List.of()));
    CombatStatsService statsService = new CombatStatsService(
        cardDataService,
        new EquipmentStatModifierRegistry(Map.of("might-gear", new EquipmentStatModifierRegistry.StatModifier(2, 0))));
    CardInstance unit = instance("plain-unit");
    unit.setCurrentHealth(4);
    CardInstance gear = attachedGear("might-gear", unit.getInstanceId());
    LiveGameState state = state(unit, gear);

    assertThat(statsService.effectiveMight(state, unit, CombatContext.IDLE)).isEqualTo(5);

    gear.setAttachedToInstanceId(null);

    assertThat(statsService.effectiveMight(state, unit, CombatContext.IDLE)).isEqualTo(3);
  }

  private CardDefinition card(String id, String name, String type, int might, int health, List<String> keywords) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, "", might, health, keywords);
  }

  private CardInstance instance(String cardId) {
    CardInstance card = new CardInstance();
    card.setInstanceId(cardId + "-instance");
    card.setCardId(cardId);
    card.setOwnerId("p1");
    card.setZone(ZoneName.BATTLEFIELD);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private CardInstance attachedGear(String cardId, String hostInstanceId) {
    CardInstance gear = instance(cardId);
    gear.setInstanceId(cardId + "-attachment");
    gear.setZone(ZoneName.BASE);
    gear.setAttachedToInstanceId(hostInstanceId);
    return gear;
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(new ArrayList<>(List.of(cards)));
    return state;
  }

  @SuppressWarnings("unchecked")
  private void install(CardDefinition... definitions) throws Exception {
    Field cards = CardDataService.class.getDeclaredField("cards");
    cards.setAccessible(true);
    Map<String, CardDefinition> cardMap = (Map<String, CardDefinition>) cards.get(cardDataService);
    for (CardDefinition definition : definitions) {
      cardMap.put(definition.id(), definition);
    }
  }
}
