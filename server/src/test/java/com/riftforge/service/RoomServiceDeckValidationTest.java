package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import com.riftforge.model.RoomState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RoomServiceDeckValidationTest {
  @Mock SimpMessagingTemplate messaging;
  @Mock CardDataService cardDataService;
  RoomService roomService;
  Map<String, CardDefinition> cards;

  @BeforeEach
  void setUp() {
    cards = new HashMap<>();
    roomService = new RoomService(messaging, cardDataService, new CardSupportService(cardDataService));
    when(cardDataService.getAll()).thenReturn(cards);
  }

  @Test
  void validDeckPassesValidation() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));

    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
    assertThatNoException().isThrownBy(() -> roomService.start(room.getCode(), "p1"));
    assertThat(room.getStatus()).isEqualTo("playing");
  }

  @Test
  void ireliaStarterDeckPassesValidation() {
    List<String> deck = addIreliaStarterDeck();
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));

    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
  }

  @Test
  void botUsesStarterDeckWhenCardsAreAvailable() {
    addIreliaStarterDeck();

    RoomState room = roomService.create("p1", "Player One", true);
    List<String> botDeck = room.getPlayers().stream()
        .filter(player -> "bot-player-riftbot".equals(player.getId()))
        .findFirst()
        .orElseThrow()
        .getDeckCardIds();

    assertThat(botDeck).hasSize(56);
    assertThatNoException().isThrownBy(() -> roomService.setBotDeck(room.getCode(), botDeck));
  }

  @Test
  void missingLegendIsRejected() {
    add("champion", "Champion");
    List<String> deck = new ArrayList<>();
    deck.add("champion");
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include a Legend card.");
  }

  @Test
  void fullConstructedDeckWithTwoLegendsIsRejected() {
    add("legend-1", "Legend");
    add("legend-2", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend-1", "legend-2", "champion"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include exactly 1 Legend card.");
  }

  @Test
  void unknownCardIdIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "missing-card"));
    addMainDeckCards(deck, 38);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown card ID: missing-card");
  }

  @Test
  void moreThanThreeCopiesIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    add("copy-card", "Unit");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "copy-card", "copy-card", "copy-card", "copy-card"));
    addMainDeckCards(deck, 35);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot include more than 3 copies of Copy Card.");
  }

  @Test
  void fullConstructedDeckWithWrongMainDeckCountIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 18);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Main deck must contain exactly 39 cards (Champion is in addition).");
  }

  @Test
  void fullConstructedDeckWithFortyNonChampionMainCardsIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 40);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Main deck must contain exactly 39 cards (Champion is in addition).");
  }

  @Test
  void fullConstructedDeckWithNoChampionIsRejected() {
    add("legend", "Legend");
    List<String> deck = new ArrayList<>(List.of("legend"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include exactly 1 Champion card.");
  }

  @Test
  void fullConstructedDeckWithTwoChampionsIsRejected() {
    add("legend", "Legend");
    add("champion-1", "Champion");
    add("champion-2", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion-1", "champion-2"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include exactly 1 Champion card.");
  }

  @Test
  void fullConstructedDeckWithNoRunesIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 39);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Rune Pool must contain exactly 12 runes.");
  }

  @Test
  void fullConstructedDeckWithBannedCardIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    add("called-shot", "Unit", "Called Shot");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "called-shot"));
    addMainDeckCards(deck, 38);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Called Shot is banned in Constructed.");
  }

  @Test
  void fullConstructedDeckWithBannedBattlefieldIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    add("dreaming-tree", "Battlefield", "Dreaming Tree");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "dreaming-tree"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 2);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Dreaming Tree is banned in Constructed.");
  }

  @Test
  void fullConstructedDeckWithFewerThanThreeBattlefieldsIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 2);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Choose exactly 3 Battlefields.");
  }

  @Test
  void playtestBotDeckAcceptsNoRunesAndNoBattlefields() {
    add("legend", "Legend");
    List<String> botDeck = new ArrayList<>(List.of("legend"));
    addMainDeckCards(botDeck, 20);
    RoomState room = roomService.create("p1", "Player One", true);

    assertThatNoException().isThrownBy(() -> roomService.setBotDeck(room.getCode(), botDeck));
  }

  @Test
  void playtestBotDeckAcceptsNoChampion() {
    add("legend", "Legend");
    List<String> botDeck = new ArrayList<>(List.of("legend"));
    addMainDeckCards(botDeck, 20);
    RoomState room = roomService.create("p1", "Player One", true);

    assertThatNoException().isThrownBy(() -> roomService.setBotDeck(room.getCode(), botDeck));
  }

  @Test
  void championCopiesCountTowardCopyLimit() {
    add("legend", "Legend");
    add("champion", "Champion", "Test Champion");
    List<String> botDeck = new ArrayList<>(List.of("legend", "champion", "champion", "champion", "champion"));
    addMainDeckCards(botDeck, 20);
    RoomState room = roomService.create("p1", "Player One", true);

    assertThatThrownBy(() -> roomService.setBotDeck(room.getCode(), botDeck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot include more than 3 copies of Test Champion.");
  }

  @Test
  void unsupportedCardIsBlockedInSupportedOnlyMode() {
    add("legend", "Legend");
    add("champion", "Champion");
    add("unsupported-spell", "Spell", "Unimplemented Spell");
    when(cardDataService.isUnsupportedAction("unsupported-spell")).thenReturn(true);
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "unsupported-spell"));
    addMainDeckCards(deck, 38);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Deck contains unsupported or not-audited cards")
        .hasMessageContaining("Unimplemented Spell (UNSUPPORTED)");
  }

  @Test
  void partialCardsProduceReadyWarnings() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 39);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    roomService.ready(room.getCode(), "p1", deck, true);

    assertThat(room.getPlayers().getFirst().getDeckWarnings()).isNotEmpty();
    assertThat(room.getPlayers().getFirst().getDeckSupport())
        .anySatisfy(summary -> assertThat(summary.status()).isEqualTo(CardSupportStatus.PARTIAL));
  }

  @Test
  void supportedCardStatusAppearsInReadyResponse() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 39);
    add("calm-rune", "Rune", "Calm Rune");
    addCopies(deck, "calm-rune", 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    RoomState response = roomService.ready(room.getCode(), "p1", deck, true);

    assertThat(response.getPlayers().getFirst().getDeckSupport())
        .anySatisfy(summary -> {
          assertThat(summary.name()).isEqualTo("Calm Rune");
          assertThat(summary.status()).isEqualTo(CardSupportStatus.SUPPORTED);
        });
  }

  private void addMainDeckCards(List<String> deck, int count) {
    for (int i = 0; i < count; i++) {
      String id = "unit-" + i;
      add(id, "Unit");
      deck.add(id);
    }
  }

  private void addRunes(List<String> deck, int count) {
    for (int i = 0; i < count; i++) {
      String id = "rune-" + i;
      add(id, "Rune");
      deck.add(id);
    }
  }

  private void addBattlefields(List<String> deck, int count) {
    for (int i = 0; i < count; i++) {
      String id = "battlefield-" + i;
      add(id, "Battlefield");
      deck.add(id);
    }
  }

  private List<String> addIreliaStarterDeck() {
    add("irelia-legend", "Legend", "Irelia - Blade Dancer");
    add("irelia-champion", "Champion", "Irelia - Fervent");
    add("defy", "Spell", "Defy");
    add("discipline", "Spell", "Discipline");
    add("tideturner", "Unit", "Tideturner");
    add("stellacorn-herder", "Unit", "Stellacorn Herder");
    add("guardian-angel", "Gear", "Guardian Angel");
    add("boots-of-swiftness", "Gear", "Boots of Swiftness");
    add("defiant-dance", "Spell", "Defiant Dance");
    add("scuttle-crab", "Unit", "Scuttle Crab");
    add("charm", "Spell", "Charm");
    add("en-garde", "Spell", "En Garde");
    add("gust", "Spell", "Gust");
    add("ride-the-wind", "Spell", "Ride The Wind");
    add("stacked-deck", "Spell", "Stacked Deck");
    add("not-so-fast", "Spell", "Not So Fast");
    add("star-crossed", "Spell", "Star-Crossed");
    add("adaptatron", "Unit", "Adaptatron");
    add("calm-rune", "Rune", "Calm Rune");
    add("chaos-rune", "Rune", "Chaos Rune");
    add("targons-peak", "Battlefield", "Targon's Peak");
    add("sunken-temple", "Battlefield", "Sunken Temple");
    add("abandoned-hall", "Battlefield", "Abandoned Hall");

    List<String> deck = new ArrayList<>(List.of("irelia-legend", "irelia-champion"));
    addCopies(deck, "defy", 3);
    addCopies(deck, "discipline", 3);
    addCopies(deck, "tideturner", 3);
    addCopies(deck, "stellacorn-herder", 3);
    addCopies(deck, "guardian-angel", 3);
    addCopies(deck, "boots-of-swiftness", 3);
    addCopies(deck, "defiant-dance", 3);
    addCopies(deck, "scuttle-crab", 3);
    addCopies(deck, "charm", 2);
    addCopies(deck, "en-garde", 2);
    addCopies(deck, "gust", 2);
    addCopies(deck, "ride-the-wind", 2);
    addCopies(deck, "stacked-deck", 2);
    addCopies(deck, "not-so-fast", 2);
    addCopies(deck, "star-crossed", 2);
    addCopies(deck, "adaptatron", 1);
    addCopies(deck, "calm-rune", 6);
    addCopies(deck, "chaos-rune", 6);
    deck.add("targons-peak");
    deck.add("sunken-temple");
    deck.add("abandoned-hall");
    return deck;
  }

  private void addCopies(List<String> deck, String cardId, int quantity) {
    for (int i = 0; i < quantity; i++) deck.add(cardId);
  }

  private void add(String id, String type) {
    add(id, type, name(id));
  }

  private void add(String id, String type, String name) {
    cards.put(id, new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
  }

  private String name(String id) {
    String[] parts = id.split("-");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (builder.length() > 0) builder.append(' ');
      builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }
    return builder.toString();
  }
}
