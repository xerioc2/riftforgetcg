package com.riftforge.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.engine.CardZoneService;
import com.riftforge.engine.CombatResolver;
import com.riftforge.engine.GameEngine;
import com.riftforge.engine.RulesValidator;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.Phase;
import com.riftforge.model.RoomState;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.service.RoomService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
class BotServicePhaseFlowTest {
  @Mock CardDataService cardDataService;
  @Mock SimpMessagingTemplate messaging;
  @Mock ApplicationEventPublisher eventPublisher;

  private final Map<String, CardDefinition> cards = new LinkedHashMap<>();
  private GameService gameService;
  private RoomService roomService;
  private BotService botService;

  @BeforeEach
  void setUp() {
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    CardEffectRegistry effects = new CardEffectRegistry(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService);
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    GameEngine engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, 8);
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService(), new GameStateProjectionService(new LegalActionsService()));
    roomService = new RoomService(messaging, cardDataService);
    botService = new BotService(gameService, cardDataService);
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
    add("legend", "Legend", 0);
    add("champion", "Champion", 0);
    for (int i = 0; i < 10; i++) add("unit-" + i, "Unit", 99);
  }

  @Test
  void botGameProgressesThroughOpeningPhasesToMain() throws Exception {
    RoomState room = roomService.createBotVsBot();
    Map<String, List<String>> decks = room.getPlayers().stream()
        .collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getDeckCardIds));
    Map<String, String> names = room.getPlayers().stream()
        .collect(Collectors.toMap(LobbyPlayer::getId, LobbyPlayer::getName));
    List<String> playerIds = room.getPlayers().stream().map(LobbyPlayer::getId).toList();

    gameService.initGame(room.getCode(), playerIds, decks, names);

    List<Phase> observed = new ArrayList<>();
    LiveGameState initial = gameService.currentState(room.getCode());
    observed.add(initial.getCurrentPhase());
    botService.onStateChanged(new GameStateChangedEvent(this, room.getCode(), initial));

    long deadline = System.currentTimeMillis() + 6_000;
    while (System.currentTimeMillis() < deadline) {
      LiveGameState state = gameService.currentState(room.getCode());
      Phase phase = state.getCurrentPhase();
      if (observed.isEmpty() || observed.get(observed.size() - 1) != phase) observed.add(phase);
      if (phase == Phase.MAIN && observed.containsAll(List.of(
          Phase.MULLIGAN,
          Phase.AWAKEN,
          Phase.BEGINNING,
          Phase.CHANNEL,
          Phase.DRAW,
          Phase.MAIN))) {
        state.setWinnerId("test-complete");
        break;
      }
      Thread.sleep(25);
    }

    assertThat(observed).containsSubsequence(
        Phase.MULLIGAN,
        Phase.AWAKEN,
        Phase.BEGINNING,
        Phase.CHANNEL,
        Phase.DRAW,
        Phase.MAIN);
  }

  private void add(String id, String type, int cost) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), cost, 0, null, null, null, null, 1, 1, List.of()));
  }
}
