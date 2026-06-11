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
import com.riftforge.model.ZoneName;
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
class CombatResolverTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  @Mock CardZoneService cardZoneService;
  CombatResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new CombatResolver(cardDataService, effects, cardZoneService);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    doAnswer(invocation -> {
      ((CardInstance) invocation.getArgument(0)).setZone(ZoneName.DISCARD);
      return null;
    }).when(cardZoneService).moveToGraveyard(any(CardInstance.class));
  }

  @Test
  void assaultAddsMightWhileAttacking() {
    CardInstance attacker = card("a", "assault", "p1");
    CardInstance defender = card("d", "defender", "p2");
    stub(attacker, 2);
    stub(defender, 3);
    when(cardDataService.getKeywordValue(attacker, "ASSAULT")).thenReturn(1);

    CombatResolver.CombatResult result = resolver.resolve(state(attacker, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(result.defendersEliminated()).isTrue();
  }

  @Test
  void shieldAddsMightWhileDefending() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "shield", "p2");
    stub(attacker, 2);
    stub(defender, 1);
    when(cardDataService.getKeywordValue(defender, "SHIELD")).thenReturn(1);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void tankTakesDamageBeforeOtherDefenders() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance tank = card("tank", "tank", "p2");
    CardInstance backline = card("back", "back", "p2");
    stub(attacker, 2);
    stub(tank, 2);
    stub(backline, 1);
    when(cardDataService.hasKeyword(tank, "TANK")).thenReturn(true);
    when(cardDataService.hasKeyword(backline, "BACKLINE")).thenReturn(true);

    resolver.resolve(state(attacker, backline, tank), "p1");

    assertThat(tank.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(backline.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void simultaneousDamageCanDestroyBothSides() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "defender", "p2");
    stub(attacker, 2, 2);
    stub(defender, 2, 2);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void lethalDamageUsesCurrentHealth() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "defender", "p2");
    defender.setCurrentHealth(1);
    stub(attacker, 1, 3);
    stub(defender, 0, 3);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void survivorsHealAfterCombatCleanup() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "defender", "p2");
    stub(attacker, 1, 3);
    stub(defender, 0, 3);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(defender.getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void stunnedUnitAssignsNoCombatDamage() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "defender", "p2");
    stub(attacker, 5, 5);
    stub(defender, 1, 1);
    when(cardDataService.hasKeyword(attacker, "STUN")).thenReturn(true);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(defender.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  private void stub(CardInstance card, int might) {
    stub(card, might, might);
  }

  private void stub(CardInstance card, int might, int health) {
    when(cardDataService.getCard(card.getCardId())).thenReturn(
        new CardDefinition(card.getCardId(), card.getCardId(), "Unit", null, List.of(), 0, 0, null, null, null, null, might, health, List.of()));
  }

  private CardInstance card(String instanceId, String cardId, String ownerId) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(ZoneName.BATTLEFIELD);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setLog(new ArrayList<>());
    return state;
  }
}
