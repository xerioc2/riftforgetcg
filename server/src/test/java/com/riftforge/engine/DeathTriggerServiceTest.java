package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.riftforge.engine.deathknell.LonelyPoroDeathknellEffectHandler;
import com.riftforge.engine.deathknell.LoyalPoroDeathknellEffectHandler;
import com.riftforge.engine.deathknell.ScuttleCrabDeathknellEffectHandler;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeathTriggerServiceTest {
  @Mock CardDataService cardDataService;
  DeathTriggerService service;

  @BeforeEach
  void setUp() {
    service = new DeathTriggerService(cardDataService, List.of(
        new LonelyPoroDeathknellEffectHandler(),
        new LoyalPoroDeathknellEffectHandler(),
        new ScuttleCrabDeathknellEffectHandler()));
  }

  @Test
  void lonelyPoroDeathknellDrawsWhenItDiedAlone() {
    when(cardDataService.getCard("lonely")).thenReturn(card("lonely", "Lonely Poro", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.getCard("drawn")).thenReturn(card("drawn", "Secret Draw", "Unit", List.of()));
    when(cardDataService.hasKeyword("lonely", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();
    state.setPlayers(playersWithDeck("drawn"));

    service.process(state, List.of(death("lonely", "Lonely Poro", false)));

    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p1");
      assertThat(card.getCardId()).isEqualTo("drawn");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Lonely Poro's Deathknell drew 1."));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Secret Draw"));
  }

  @Test
  void lonelyPoroDeathknellDoesNotDrawWhenAnotherFriendlyUnitWasThere() {
    when(cardDataService.getCard("lonely")).thenReturn(card("lonely", "Lonely Poro", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.hasKeyword("lonely", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();
    state.setPlayers(playersWithDeck("drawn"));

    service.process(state, List.of(death("lonely", "Lonely Poro", true)));

    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Lonely Poro did not die alone. Deathknell did not draw."));
  }

  @Test
  void deathSnapshotIgnoresEnemyUnitsAndFriendlyGearForLonelyPoroAloneCheck() {
    when(cardDataService.getCard("lonely")).thenReturn(card("lonely", "Lonely Poro", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.getCard("enemy")).thenReturn(card("enemy", "Enemy Unit", "Unit", List.of()));
    when(cardDataService.getCard("gear")).thenReturn(card("gear", "Friendly Gear", "Gear", List.of()));
    when(cardDataService.hasKeyword("lonely", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();
    CardInstance lonely = boardCard("lonely-i", "p1", "lonely", ZoneName.BATTLEFIELD);
    state.getCards().add(lonely);
    state.getCards().add(boardCard("enemy-i", "p2", "enemy", ZoneName.BATTLEFIELD));
    state.getCards().add(boardCard("gear-i", "p1", "gear", ZoneName.BATTLEFIELD));

    DeathEvent death = service.capture(lonely, state, DeathEvent.DeathCause.COMBAT);

    assertThat(death.hadOtherFriendlyUnitAtLocation()).isFalse();
  }

  @Test
  void deathSnapshotCountsSimultaneousFriendlyUnitBeforeCleanup() {
    when(cardDataService.getCard("lonely")).thenReturn(card("lonely", "Lonely Poro", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.getCard("friend")).thenReturn(card("friend", "Friendly Unit", "Unit", List.of()));
    LiveGameState state = state();
    CardInstance lonely = boardCard("lonely-i", "p1", "lonely", ZoneName.BATTLEFIELD);
    state.getCards().add(lonely);
    state.getCards().add(boardCard("friend-i", "p1", "friend", ZoneName.BATTLEFIELD));

    DeathEvent death = service.capture(lonely, state, DeathEvent.DeathCause.COMBAT);

    assertThat(death.hadOtherFriendlyUnitAtLocation()).isTrue();
  }

  @Test
  void loyalPoroDeathknellResolvesThroughCardSpecificHandler() {
    when(cardDataService.getCard("loyal")).thenReturn(card("loyal", "Loyal Poro", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.getCard("drawn")).thenReturn(card("drawn", "Drawn Card", "Unit", List.of()));
    when(cardDataService.hasKeyword("loyal", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();
    state.setPlayers(playersWithDeck("drawn"));

    service.process(state, List.of(death("loyal", "Loyal Poro", true)));

    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("p1");
      assertThat(card.getCardId()).isEqualTo("drawn");
      assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
    });
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Loyal Poro's Deathknell drew 1."));
  }

  @Test
  void scuttleCrabDeathknellUsesPartialCardSpecificHandlerWithoutPrivateLeak() {
    when(cardDataService.getCard("scuttle")).thenReturn(card("scuttle", "Scuttle Crab", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.hasKeyword("scuttle", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();
    state.setPlayers(players("p1", "p2"));
    state.getCards().add(handCard("enemy-hand", "p2", "secret-card"));

    service.process(state, List.of(death("scuttle", "Scuttle Crab", false)));

    assertThat(state.getRevealedHands())
        .filteredOn(snapshot -> snapshot.getRevealedToPlayerId().equals("p1") && snapshot.getRevealedOwnerId().equals("p2"))
        .singleElement()
        .satisfies(snapshot -> assertThat(snapshot.getInstanceIds()).containsExactly("enemy-hand"));
    assertThat(state.getLog()).singleElement()
        .satisfies(entry -> assertThat(entry.text())
            .isEqualTo("Scuttle Crab's Deathknell revealed an opponent's hand. XP and facedown viewing are deferred in alpha."));
  }

  @Test
  void unsupportedDeathknellCardLogsSafelyInsteadOfCrashing() {
    when(cardDataService.getCard("unknown-death")).thenReturn(card("unknown-death", "Mystery Deathknell", "Unit", List.of("DEATHKNELL")));
    when(cardDataService.hasKeyword("unknown-death", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();

    service.process(state, List.of(death("unknown-death", "Mystery Deathknell", false)));

    assertThat(state.getLog()).singleElement()
        .satisfies(entry -> assertThat(entry.text())
            .isEqualTo("Mystery Deathknell's Deathknell is not fully supported yet."));
  }

  @Test
  void deathknellKeywordOnGearDoesNotResolveFromGearCleanup() {
    when(cardDataService.getCard("gear")).thenReturn(card("gear", "Haunted Gear", "Gear", List.of("DEATHKNELL")));
    when(cardDataService.hasKeyword("gear", "DEATHKNELL")).thenReturn(true);
    LiveGameState state = state();

    service.process(state, List.of(death("gear", "Haunted Gear", false)));

    assertThat(state.getLog()).isEmpty();
  }

  @Test
  void cardWithoutDeathknellKeywordIsIgnored() {
    when(cardDataService.getCard("plain")).thenReturn(card("plain", "Plain Unit", "Unit", List.of()));
    when(cardDataService.hasKeyword("plain", "DEATHKNELL")).thenReturn(false);
    LiveGameState state = state();

    service.process(state, List.of(death("plain", "Plain Unit", false)));

    assertThat(state.getLog()).isEmpty();
  }

  private DeathEvent death(String cardId, String cardName, boolean hadOtherFriendlyUnit) {
    return new DeathEvent("instance-" + cardId, cardId, cardName, "p1", ZoneName.BATTLEFIELD, hadOtherFriendlyUnit, DeathEvent.DeathCause.COMBAT);
  }

  private CardDefinition card(String id, String name, String type, List<String> keywords) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, "", 1, 1, keywords);
  }

  private CardInstance handCard(String instanceId, String ownerId, String cardId) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setOwnerId(ownerId);
    card.setCardId(cardId);
    card.setZone(ZoneName.HAND);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private CardInstance boardCard(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = handCard(instanceId, ownerId, cardId);
    card.setZone(zone);
    return card;
  }

  private LiveGameState state() {
    LiveGameState state = new LiveGameState();
    state.setCards(new ArrayList<>());
    state.setLog(new ArrayList<>());
    return state;
  }

  private List<PlayerState> playersWithDeck(String... deck) {
    PlayerState player = new PlayerState();
    player.setUserId("p1");
    player.setName("Player One");
    player.setDeckPool(new ArrayList<>(List.of(deck)));
    return new ArrayList<>(List.of(player));
  }

  private List<PlayerState> players(String... playerIds) {
    List<PlayerState> players = new ArrayList<>();
    for (String playerId : playerIds) {
      PlayerState player = new PlayerState();
      player.setUserId(playerId);
      player.setName(playerId);
      players.add(player);
    }
    return players;
  }
}
