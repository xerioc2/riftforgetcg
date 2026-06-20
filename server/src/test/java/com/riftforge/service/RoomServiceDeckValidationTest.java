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
  void uploadedIreliaPlaytestDeckPassesValidation() {
    List<String> deck = addUploadedIreliaPlaytestDeck();
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));

    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
  }

  @Test
  void botUsesUploadedIreliaPlaytestDeckWhenCardsAreAvailable() {
    addUploadedIreliaPlaytestDeck();

    RoomState room = roomService.create("p1", "Player One", true);
    List<String> botDeck = room.getPlayers().stream()
        .filter(player -> "bot-player-riftbot".equals(player.getId()))
        .findFirst()
        .orElseThrow()
        .getDeckCardIds();

    assertThat(botDeck).hasSize(56);
    assertThat(botDeck.get(0)).isEqualTo("irelia-legend");
    assertThat(botDeck.get(1)).isEqualTo("irelia-champion");
    assertThat(botDeck).contains("vex-apathetic", "zhonyas-hourglass", "the-syren", "mindsplitter");
    assertThat(frequency(botDeck, "vex-apathetic")).isEqualTo(2);
    assertThat(frequency(botDeck, "scuttle-crab")).isEqualTo(3);
    assertThat(frequency(botDeck, "charm")).isEqualTo(3);
    assertThat(frequency(botDeck, "calm-rune")).isEqualTo(6);
    assertThat(frequency(botDeck, "chaos-rune")).isEqualTo(6);
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
    addMainDeckCards(deck, 39);
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
  void fullConstructedDeckWithThirtyEightMainDeckCardsIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 38);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Main Deck must contain exactly 39 cards. Current count: 38.");
  }

  @Test
  void fullConstructedDeckWithFortyMainDeckCardsIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 40);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Main Deck must contain exactly 39 cards. Current count: 40.");
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
        .hasMessage("Deck must include exactly 1 Chosen Champion card.");
  }

  @Test
  void mainDeckChampionUnitDoesNotSatisfyChosenChampionRole() {
    add("legend", "Legend");
    add("annie", "Champion", "Annie, Stubborn");
    List<String> deck = new ArrayList<>(List.of("legend"));
    addMainDeckCards(deck, 38);
    deck.add("annie");
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include exactly 1 Chosen Champion card.");
  }

  @Test
  void mainDeckChampionUnitsAreAllowedAndCountAsMainDeckCards() {
    add("legend", "Legend");
    add("chosen-champion", "Champion", "Irelia - Fervent");
    add("annie", "Champion", "Annie, Stubborn");
    add("fizz", "Champion", "Fizz, Trickster");
    List<String> deck = new ArrayList<>(List.of("legend", "chosen-champion", "annie", "fizz"));
    addMainDeckCards(deck, 37);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));
    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
  }

  @Test
  void ireliaDeckWithThirtyNineMainDeckCardsPassesValidation() {
    add("legend", "Legend", "Irelia - Blade Dancer");
    add("chosen-champion", "Champion", "Irelia - Fervent");
    add("annie", "Champion", "Annie, Stubborn");
    add("fizz", "Champion", "Fizz, Trickster");
    List<String> deck = new ArrayList<>(List.of("legend", "chosen-champion", "annie", "fizz"));
    addMainDeckCards(deck, 37);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));
    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
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
  void chosenChampionPlusTwoMainDeckCopiesPassesCopyLimit() {
    add("legend", "Legend");
    add("champion", "Champion", "Test Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "champion", "champion"));
    addMainDeckCards(deck, 37);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));
  }

  @Test
  void chosenChampionPlusThreeMainDeckCopiesFailsCopyLimit() {
    add("legend", "Legend");
    add("champion", "Champion", "Test Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "champion", "champion", "champion"));
    addMainDeckCards(deck, 36);
    addRunes(deck, 12);
    addBattlefields(deck, 3);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
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

  private List<String> addUploadedIreliaPlaytestDeck() {
    add("irelia-legend", "Legend", "Irelia - Blade Dancer");
    add("irelia-champion", "Champion", "Irelia - Fervent");
    add("not-so-fast", "Spell", "Not So Fast");
    add("abandoned-hall", "Battlefield", "Abandoned Hall");
    add("vex-apathetic", "Champion", "Vex - Apathetic");
    add("scuttle-crab", "Unit", "Scuttle Crab");
    add("back-off", "Spell", "Back Off");
    add("sunken-temple", "Battlefield", "Sunken Temple");
    add("defiant-dance", "Spell", "Defiant Dance");
    add("edge-of-night", "Gear", "Edge of Night");
    add("boots-of-swiftness", "Gear", "Boots of Swiftness");
    add("guardian-angel", "Gear", "Guardian Angel");
    add("stellacorn-herder", "Unit", "Stellacorn Herder");
    add("calm-rune", "Rune", "Calm Rune");
    add("lonely-poro", "Unit", "Lonely Poro");
    add("flash", "Spell", "Flash");
    add("charm", "Spell", "Charm");
    add("defy", "Spell", "Defy");
    add("en-garde", "Spell", "En Garde");
    add("discipline", "Spell", "Discipline");
    add("zhonyas-hourglass", "Gear", "Zhonya's Hourglass");
    add("chaos-rune", "Rune", "Chaos Rune");
    add("ride-the-wind", "Spell", "Ride The Wind");
    add("the-syren", "Gear", "The Syren");
    add("mindsplitter", "Unit", "Mindsplitter");
    add("tideturner", "Unit", "Tideturner");
    add("targons-peak", "Battlefield", "Targon's Peak");

    List<String> deck = new ArrayList<>(List.of("irelia-legend", "irelia-champion"));
    addCopies(deck, "not-so-fast", 1);
    addCopies(deck, "vex-apathetic", 2);
    addCopies(deck, "scuttle-crab", 3);
    addCopies(deck, "back-off", 1);
    addCopies(deck, "defiant-dance", 3);
    addCopies(deck, "edge-of-night", 1);
    addCopies(deck, "boots-of-swiftness", 2);
    addCopies(deck, "guardian-angel", 2);
    addCopies(deck, "stellacorn-herder", 2);
    addCopies(deck, "lonely-poro", 3);
    addCopies(deck, "flash", 2);
    addCopies(deck, "charm", 3);
    addCopies(deck, "defy", 3);
    addCopies(deck, "en-garde", 2);
    addCopies(deck, "discipline", 3);
    addCopies(deck, "zhonyas-hourglass", 1);
    addCopies(deck, "ride-the-wind", 1);
    addCopies(deck, "the-syren", 1);
    addCopies(deck, "mindsplitter", 2);
    addCopies(deck, "tideturner", 1);
    addCopies(deck, "calm-rune", 6);
    addCopies(deck, "chaos-rune", 6);
    deck.add("abandoned-hall");
    deck.add("sunken-temple");
    deck.add("targons-peak");
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

  private int frequency(List<String> deck, String cardId) {
    return (int) deck.stream().filter(cardId::equals).count();
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
