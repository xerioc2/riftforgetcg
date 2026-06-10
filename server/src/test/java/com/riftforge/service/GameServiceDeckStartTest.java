package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.engine.GameEngine;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.model.ZoneName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameServiceDeckStartTest {
  @Mock GameEngine engine;
  @Mock CardDataService cardDataService;
  @Mock SimpMessagingTemplate messaging;
  @Mock ApplicationEventPublisher eventPublisher;
  Map<String, CardDefinition> cards;
  GameService gameService;
  RoomService roomService;

  @BeforeEach
  void setUp() {
    cards = new HashMap<>();
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService(), new GameStateProjectionService());
    roomService = new RoomService(messaging, cardDataService);
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
  }

  @Test
  void fullConstructedGameStartPartitionsDeckSections() {
    add("legend", "Legend");
    add("champion", "Champion");
    addMainDeckCards(39);
    addRunes(12);
    addBattlefields(3);
    List<String> deck = constructedDeck("legend", "champion");

    gameService.initGame("ROOM", List.of("p1"), Map.of("p1", deck), Map.of("p1", "Player One"));

    LiveGameState state = gameService.currentState("ROOM");
    assertThat(state.getPlayers().getFirst().getDeckCount()).isEqualTo(35);
    assertThat(state.getPlayers().getFirst().getRunePoolRemaining()).isEqualTo(12);
    assertThat(state.getPlayers().getFirst().getSelectedBattlefields()).containsExactlyInAnyOrder("battlefield-0", "battlefield-1", "battlefield-2");
    assertThat(state.getPlayers().getFirst().getDeckPool())
        .allSatisfy(cardId -> assertThat(cards.get(cardId).type())
            .isNotIn("Rune", "Battlefield", "Legend", "Champion"));
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("legend");
      assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
    });
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("champion");
      assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
    });
    assertThat(state.getCards())
        .filteredOn(card -> card.getOwnerId().equals("p1") && card.getZone() == ZoneName.HAND)
        .hasSize(4)
        .allSatisfy(card -> assertThat(cards.get(card.getCardId()).type())
            .isNotIn("Rune", "Battlefield", "Legend", "Champion"));
  }

  @Test
  void botDeckStartupStillWorks() {
    add("legend", "Legend");
    add("champion", "Champion");
    addMainDeckCards(20);

    RoomState room = roomService.createBotVsBot();
    Map<String, List<String>> decks = room.getPlayers().stream()
        .collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream()
        .collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();

    assertThatNoException().isThrownBy(() -> gameService.initGame(room.getCode(), playerIds, decks, names));

    LiveGameState state = gameService.currentState(room.getCode());
    assertThat(state.getPlayers()).hasSize(2);
    for (String playerId : playerIds) {
      assertThat(state.getCards()).anySatisfy(card -> {
        assertThat(card.getOwnerId()).isEqualTo(playerId);
        assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
      });
      assertThat(state.getCards()).anySatisfy(card -> {
        assertThat(card.getOwnerId()).isEqualTo(playerId);
        assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
      });
    }
  }

  private List<String> constructedDeck(String legendId, String championId) {
    List<String> deck = new ArrayList<>();
    deck.add(legendId);
    deck.add(championId);
    deck.addAll(cards.values().stream()
        .map(CardDefinition::id)
        .filter(id -> id.startsWith("unit-"))
        .toList());
    deck.addAll(cards.values().stream()
        .map(CardDefinition::id)
        .filter(id -> id.startsWith("rune-"))
        .toList());
    deck.addAll(cards.values().stream()
        .map(CardDefinition::id)
        .filter(id -> id.startsWith("battlefield-"))
        .toList());
    return deck;
  }

  private void addMainDeckCards(int count) {
    for (int i = 0; i < count; i++) add("unit-" + i, "Unit");
  }

  private void addRunes(int count) {
    for (int i = 0; i < count; i++) add("rune-" + i, "Rune");
  }

  private void addBattlefields(int count) {
    for (int i = 0; i < count; i++) add("battlefield-" + i, "Battlefield");
  }

  private void add(String id, String type) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
  }
}
