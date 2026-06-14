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
import com.riftforge.model.PlayerState;
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
  DeathTriggerService deathTriggerService;

  @BeforeEach
  void setUp() {
    deathTriggerService = new DeathTriggerService(cardDataService);
    resolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    doAnswer(invocation -> {
      CardInstance card = invocation.getArgument(0);
      CardDefinition def = cardDataService.getCard(card.getCardId());
      if (def != null && "Champion".equalsIgnoreCase(def.type())) {
        card.setZone(ZoneName.CHAMPION);
        card.setCurrentHealth(def.health());
        card.setAttachedToInstanceId(null);
      } else {
        card.setZone(ZoneName.DISCARD);
        card.setAttachedToInstanceId(null);
      }
      return null;
    }).when(cardZoneService).moveToGraveyard(any(CardInstance.class));
    when(cardZoneService.returnAttachmentsToBase(any(LiveGameState.class), any(CardInstance.class))).thenAnswer(invocation -> {
      LiveGameState state = invocation.getArgument(0);
      CardInstance host = invocation.getArgument(1);
      List<CardInstance> attachments = state.getCards().stream()
          .filter(attachment -> host.getInstanceId().equals(attachment.getAttachedToInstanceId()))
          .toList();
      attachments.forEach(attachment -> {
            attachment.setZone(ZoneName.BASE);
            attachment.setAttachedToInstanceId(null);
          });
      return attachments;
    });
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
  void daringPoroAssaultChangesCombatDamageOnlyWhileAttacking() {
    CardInstance daringPoro = card("poro", "daring-poro", "p1");
    CardInstance defender = card("d", "two-health-defender", "p2");
    stub(daringPoro, 1, 2, "Daring Poro", List.of("Assault"));
    stub(defender, 1, 2);
    when(cardDataService.getKeywordValue(daringPoro, "ASSAULT")).thenReturn(1);

    resolver.resolve(state(daringPoro, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(daringPoro.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void daringPoroDoesNotGetAssaultBonusWhileDefending() {
    CardInstance attacker = card("a", "two-might-attacker", "p1");
    CardInstance daringPoro = card("poro", "daring-poro", "p2");
    stub(attacker, 2, 2);
    stub(daringPoro, 1, 2, "Daring Poro", List.of("Assault"));
    when(cardDataService.getKeywordValue(daringPoro, "SHIELD")).thenReturn(0);

    resolver.resolve(state(attacker, daringPoro), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(daringPoro.getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void laurentDuelistAssaultTwoChangesCombatDamageOnlyWhileAttacking() {
    CardInstance duelist = card("duelist", "laurent-duelist", "p1");
    CardInstance defender = card("d", "four-health-defender", "p2");
    stub(duelist, 2, 3, "Laurent Duelist", List.of("Assault 2"));
    stub(defender, 1, 4);
    when(cardDataService.getKeywordValue(duelist, "ASSAULT")).thenReturn(2);

    resolver.resolve(state(duelist, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(duelist.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void laurentDuelistDoesNotGetAssaultTwoWhileDefending() {
    CardInstance attacker = card("a", "three-might-attacker", "p1");
    CardInstance duelist = card("duelist", "laurent-duelist", "p2");
    stub(attacker, 3, 3);
    stub(duelist, 2, 3, "Laurent Duelist", List.of("Assault 2"));
    when(cardDataService.getKeywordValue(duelist, "SHIELD")).thenReturn(0);

    resolver.resolve(state(attacker, duelist), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(duelist.getZone()).isEqualTo(ZoneName.DISCARD);
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
  void shieldTwoChangesCombatDamageOnlyWhileDefending() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "fortified-position-target", "p2");
    stub(attacker, 4, 4);
    stub(defender, 2, 4, "Fortified Position Target", List.of());
    defender.setTempKeywords(new ArrayList<>(List.of("Shield 2")));
    when(cardDataService.getKeywordValue(defender, "SHIELD")).thenReturn(2);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(attacker.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void shieldTwoDoesNotApplyWhileAttacking() {
    CardInstance attacker = card("a", "fortified-position-target", "p1");
    CardInstance defender = card("d", "three-health-defender", "p2");
    stub(attacker, 2, 4, "Fortified Position Target", List.of());
    stub(defender, 1, 3);
    attacker.setTempKeywords(new ArrayList<>(List.of("Shield 2")));
    when(cardDataService.getKeywordValue(attacker, "ASSAULT")).thenReturn(0);

    resolver.resolve(state(attacker, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(attacker.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
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

  @Test
  void attachedGearReturnsToBaseWhenHostIsDestroyed() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance defender = card("d", "defender", "p2");
    CardInstance gear = card("g", "guardian-angel", "p2");
    gear.setZone(ZoneName.BASE);
    gear.setAttachedToInstanceId("d");
    stub(attacker, 2, 2);
    stub(defender, 1, 1);

    resolver.resolve(state(attacker, defender, gear), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
  }

  @Test
  void championDestroyedInCombatReturnsToChampionZoneAndGearReturnsToBase() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance champion = card("c", "friendly-champion", "p2");
    CardInstance gear = card("g", "guardian-angel", "p2");
    gear.setZone(ZoneName.BASE);
    gear.setAttachedToInstanceId("c");
    stub(attacker, 4, 4);
    stubCard("friendly-champion", "Friendly Champion", "Champion", 1, 3, List.of());
    stubCard("guardian-angel", "Guardian Angel", "Gear", 0, 0, List.of("EQUIP"));

    resolver.resolve(state(attacker, champion, gear), "p1");

    assertThat(champion.getZone()).isEqualTo(ZoneName.CHAMPION);
    assertThat(champion.getCurrentHealth()).isEqualTo(3);
    assertThat(champion.getAttachedToInstanceId()).isNull();
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
  }

  @Test
  void loyalPoroDeathknellDrawsWhenItDidNotDieAlone() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance loyalPoro = card("loyal", "loyal-poro", "p2");
    CardInstance friend = card("friend", "friend", "p2");
    stub(attacker, 1, 2);
    stub(loyalPoro, 0, 1, "Loyal Poro", List.of("DEATHKNELL"));
    stub(friend, 0, 2);
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("loyal-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, loyalPoro, friend);
    state.setPlayers(playersWithDeck("drawn"));

    resolver.resolve(state, "p1");

    assertThat(loyalPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p2");
      assertThat(card.getCardId()).isEqualTo("drawn");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(state.getLog().stream().filter(entry -> entry.text().contains("Loyal Poro's Deathknell drew 1."))).hasSize(1);
  }

  @Test
  void loyalPoroDeathknellDoesNotDrawWhenItDiesAlone() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance loyalPoro = card("loyal", "loyal-poro", "p2");
    stub(attacker, 1, 2);
    stub(loyalPoro, 0, 1, "Loyal Poro", List.of("DEATHKNELL"));
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("loyal-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, loyalPoro);
    state.setPlayers(playersWithDeck("drawn"));

    resolver.resolve(state, "p1");

    assertThat(loyalPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("Loyal Poro died alone."));
  }

  @Test
  void loyalPoroDrawsExactlyOnceWhenItDiesWithAnotherFriendlyUnit() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance loyalPoro = card("loyal", "loyal-poro", "p2");
    CardInstance friend = card("friend", "friend", "p2");
    stub(attacker, 2, 3);
    stub(loyalPoro, 0, 1, "Loyal Poro", List.of("DEATHKNELL"));
    stub(friend, 0, 1);
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("loyal-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, loyalPoro, friend);
    state.setPlayers(playersWithDeck("drawn", "second-card"));

    resolver.resolve(state, "p1");

    assertThat(loyalPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(friend.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards().stream()
        .filter(card -> card.getOwnerId().equals("p2"))
        .filter(card -> card.getZone() == ZoneName.HAND)
        .filter(card -> card.getCardId().equals("drawn") || card.getCardId().equals("second-card")))
        .hasSize(1);
    assertThat(state.getLog().stream().filter(entry -> entry.text().contains("Loyal Poro's Deathknell drew 1."))).hasSize(1);
  }

  @Test
  void lonelyPoroDeathknellDrawsWhenItDiesAlone() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance lonelyPoro = card("lonely", "lonely-poro", "p2");
    stub(attacker, 1, 2);
    stub(lonelyPoro, 0, 1, "Lonely Poro", List.of("DEATHKNELL"));
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("lonely-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, lonelyPoro);
    state.setPlayers(playersWithDeck("drawn"));

    resolver.resolve(state, "p1");

    assertThat(lonelyPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p2");
      assertThat(card.getCardId()).isEqualTo("drawn");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(state.getLog().stream().filter(entry -> entry.text().contains("Lonely Poro's Deathknell drew 1."))).hasSize(1);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Drawn Card"));
  }

  @Test
  void lonelyPoroDeathknellDoesNotDrawWhenFriendlyUnitWasAtSameLocation() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance lonelyPoro = card("lonely", "lonely-poro", "p2");
    CardInstance friend = card("friend", "friend", "p2");
    stub(attacker, 1, 2);
    stub(lonelyPoro, 0, 1, "Lonely Poro", List.of("DEATHKNELL"));
    stub(friend, 0, 2);
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("lonely-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, lonelyPoro, friend);
    state.setPlayers(playersWithDeck("drawn"));

    resolver.resolve(state, "p1");

    assertThat(lonelyPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("Lonely Poro did not die alone."));
  }

  @Test
  void simultaneousFriendlyDeathMeansLonelyPoroDidNotDieAlone() {
    CardInstance attacker = card("a", "attacker", "p1");
    CardInstance lonelyPoro = card("lonely", "lonely-poro", "p2");
    CardInstance friend = card("friend", "friend", "p2");
    stub(attacker, 2, 3);
    stub(lonelyPoro, 0, 1, "Lonely Poro", List.of("DEATHKNELL"));
    stub(friend, 0, 1);
    stubCard("drawn", "Drawn Card", "Unit", 1, 1, List.of());
    when(cardDataService.hasKeyword("lonely-poro", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, lonelyPoro, friend);
    state.setPlayers(playersWithDeck("drawn"));

    resolver.resolve(state, "p1");

    assertThat(lonelyPoro.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(friend.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().contains("Lonely Poro did not die alone."));
  }

  @Test
  void simultaneousDeathknellEventsAreProcessedDeterministicallyAndOnce() {
    CardInstance attacker = card("zed", "zed-deathknell", "p1");
    CardInstance defender = card("alpha", "alpha-deathknell", "p2");
    stub(attacker, 1, 1, "Zed Deathknell", List.of("DEATHKNELL"));
    stub(defender, 1, 1, "Alpha Deathknell", List.of("DEATHKNELL"));
    when(cardDataService.hasKeyword("zed-deathknell", "DEATHKNELL")).thenReturn(true);
    when(cardDataService.hasKeyword("alpha-deathknell", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state(attacker, defender);

    resolver.resolve(state, "p1");

    List<String> deathknellLogs = state.getLog().stream()
        .map(LiveGameState.LogEntry::text)
        .filter(text -> text.contains("Deathknell is not fully supported"))
        .toList();
    assertThat(deathknellLogs)
        .containsExactly(
            "Alpha Deathknell's Deathknell is not fully supported yet.",
            "Zed Deathknell's Deathknell is not fully supported yet.");
  }

  @Test
  void recruitTokenCanParticipateInCombat() {
    CardInstance recruit = card("recruit", TokenFactory.RECRUIT_TOKEN_CARD_ID, "p1");
    CardInstance defender = card("defender", "defender", "p2");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 1, 1, List.of());
    stub(defender, 0, 1);

    CombatResolver.CombatResult result = resolver.resolve(state(recruit, defender), "p1");

    assertThat(defender.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(recruit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(result.defendersEliminated()).isTrue();
  }

  private void stub(CardInstance card, int might) {
    stub(card, might, might);
  }

  private void stub(CardInstance card, int might, int health) {
    stub(card, might, health, card.getCardId(), List.of());
  }

  private void stub(CardInstance card, int might, int health, String name, List<String> keywords) {
    stubCard(card.getCardId(), name, "Unit", might, health, keywords);
  }

  private void stubCard(String cardId, String name, String type, int might, int health, List<String> keywords) {
    when(cardDataService.getCard(cardId)).thenReturn(
        new CardDefinition(cardId, name, type, null, List.of(), 0, 0, null, null, null, null, might, health, keywords));
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

  private List<PlayerState> playersWithDeck(String... p2Deck) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    p1.setName("Player One");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    p2.setName("Player Two");
    p2.setDeckPool(new ArrayList<>(List.of(p2Deck)));
    return new ArrayList<>(List.of(p1, p2));
  }
}
