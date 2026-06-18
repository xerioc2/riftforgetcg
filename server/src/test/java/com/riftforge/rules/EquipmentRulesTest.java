package com.riftforge.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.riftforge.model.CardDefinition;
import com.riftforge.rules.EquipmentRules.EquipCost;
import java.util.List;
import org.junit.jupiter.api.Test;

class EquipmentRulesTest {

  @Test
  void parsesCalmRuneEquipCost() {
    EquipCost cost = EquipmentRules.equipCost(
        gear("[Equip] :rb_rune_calm: (:rb_rune_calm:: Attach this to a unit you control.)"));

    assertThat(cost.energyCost()).isZero();
    assertThat(cost.premiumDomains()).containsExactly("CALM");
  }

  @Test
  void parsesChaosRuneEquipCost() {
    EquipCost cost = EquipmentRules.equipCost(
        gear("[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)"));

    assertThat(cost.energyCost()).isZero();
    assertThat(cost.premiumDomains()).containsExactly("CHAOS");
  }

  @Test
  void parsesEnergyAndPremiumCost() {
    EquipCost cost = EquipmentRules.equipCost(gear("[Equip] :rb_energy_2::rb_rune_calm:"));

    assertThat(cost.energyCost()).isEqualTo(2);
    assertThat(cost.premiumDomains()).containsExactly("CALM");
  }

  @Test
  void parsesMultipleCostIcons() {
    EquipCost cost = EquipmentRules.equipCost(
        gear("[Equip] :rb_energy_1::rb_energy_2::rb_rune_calm::rb_rune_chaos:"));

    assertThat(cost.energyCost()).isEqualTo(3);
    assertThat(cost.premiumDomains()).containsExactly("CALM", "CHAOS");
  }

  @Test
  void reminderTextAfterParenthesisIsNotCountedInCost() {
    // The printed reminder text repeats the rune icon inside parentheses; only the
    // header before the '(' is the actual cost, so the domain must appear exactly once.
    EquipCost cost = EquipmentRules.equipCost(
        gear("[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)"));

    assertThat(cost.premiumDomains()).hasSize(1);
  }

  @Test
  void normalizeDomainHandlesCaseDashesRainbowAndNull() {
    assertThat(EquipmentRules.normalizeDomain("calm")).isEqualTo("CALM");
    assertThat(EquipmentRules.normalizeDomain("multi-color")).isEqualTo("MULTI_COLOR");
    assertThat(EquipmentRules.normalizeDomain("RAINBOW")).isEqualTo("RAINBOW");
    assertThat(EquipmentRules.normalizeDomain(null)).isEmpty();
  }

  @Test
  void noEquipTextReturnsNoCost() {
    assertThat(EquipmentRules.equipCost(null).premiumDomains()).isEmpty();
    assertThat(EquipmentRules.equipCost(gear(null)).energyCost()).isZero();
    EquipCost withoutHeader = EquipmentRules.equipCost(gear("Give a unit +2 Might this turn."));
    assertThat(withoutHeader.energyCost()).isZero();
    assertThat(withoutHeader.premiumDomains()).isEmpty();
  }

  @Test
  void malformedEquipTextFailsSafely() {
    assertThatCode(() -> {
      EquipCost bareHeader = EquipmentRules.equipCost(gear("[Equip]"));
      assertThat(bareHeader.energyCost()).isZero();
      assertThat(bareHeader.premiumDomains()).isEmpty();

      // Non-numeric energy icon does not match the numeric pattern and is ignored.
      EquipCost garbledEnergy = EquipmentRules.equipCost(gear("[Equip] :rb_energy_x::rb_rune_:"));
      assertThat(garbledEnergy.energyCost()).isZero();
    }).doesNotThrowAnyException();
  }

  private CardDefinition gear(String rulesText) {
    return new CardDefinition(
        "g", "Test Gear", "Gear", null, List.of(), 0, 0, null, null, null, rulesText, 0, 0, List.of());
  }
}
