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
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService());
    roomService = new RoomService(messaging, cardDataService);
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
  }

  @Test
  void gameStartPlacesLegendAndChampionInCorrectZones() {
    add("legend", "Legend");
    add("champion", "Champion");
    addMainDeckCards(20);
    List<String> deck = deck("legend", "champion");

    gameService.initGame("ROOM", List.of("p1"), Map.of("p1", deck), Map.of("p1", "Player One"));

    LiveGameState state = gameService.currentState("ROOM");
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("legend");
      assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
    });
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getCardId()).isEqualTo("champion");
      assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
    });
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

  private List<String> deck(String legendId, String championId) {
    return cards.values().stream()
        .map(CardDefinition::id)
        .filter(id -> id.startsWith("unit-"))
        .collect(Collectors.collectingAndThen(Collectors.toList(), units -> {
          units.add(0, championId);
          units.add(0, legendId);
          return units;
        }));
  }

  private void addMainDeckCards(int count) {
    for (int i = 0; i < count; i++) add("unit-" + i, "Unit");
  }

  private void add(String id, String type) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
  }
}
