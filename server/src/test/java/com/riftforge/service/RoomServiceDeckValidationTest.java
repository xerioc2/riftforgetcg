package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
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
    roomService = new RoomService(messaging, cardDataService);
    when(cardDataService.getAll()).thenReturn(cards);
  }

  @Test
  void validDeckPassesValidation() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 20);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatNoException().isThrownBy(() -> roomService.ready(room.getCode(), "p1", deck));

    assertThat(room.getPlayers().getFirst().isReady()).isTrue();
    assertThatNoException().isThrownBy(() -> roomService.start(room.getCode(), "p1"));
    assertThat(room.getStatus()).isEqualTo("playing");
  }

  @Test
  void missingLegendIsRejected() {
    add("champion", "Champion");
    List<String> deck = new ArrayList<>();
    deck.add("champion");
    addMainDeckCards(deck, 20);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Deck must include a Legend card.");
  }

  @Test
  void unknownCardIdIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion", "missing-card"));
    addMainDeckCards(deck, 20);
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
    addMainDeckCards(deck, 16);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot include more than 3 copies of Copy Card.");
  }

  @Test
  void deckUnderTwentyMainCardsIsRejected() {
    add("legend", "Legend");
    add("champion", "Champion");
    List<String> deck = new ArrayList<>(List.of("legend", "champion"));
    addMainDeckCards(deck, 19);
    RoomState room = roomService.create("p1", "Player One", false);

    assertThatThrownBy(() -> roomService.ready(room.getCode(), "p1", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Main deck must have at least 20 cards.");
  }

  private void addMainDeckCards(List<String> deck, int count) {
    for (int i = 0; i < count; i++) {
      String id = "unit-" + i;
      add(id, "Unit");
      deck.add(id);
    }
  }

  private void add(String id, String type) {
    cards.put(id, new CardDefinition(id, name(id), type, null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
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
