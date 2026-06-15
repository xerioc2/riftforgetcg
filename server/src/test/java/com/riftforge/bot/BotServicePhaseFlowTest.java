package com.riftforge.bot;

import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;
import static com.riftforge.bot.BotConstants.BOT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.engine.CardZoneService;
import com.riftforge.engine.CombatResolver;
import com.riftforge.engine.CombatStatsService;
import com.riftforge.engine.DeathTriggerService;
import com.riftforge.engine.GameEngine;
import com.riftforge.engine.IllegalMoveException;
import com.riftforge.engine.RulesValidator;
import com.riftforge.engine.TokenFactory;
import com.riftforge.model.CardInstance;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.RoomState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.LegalAction;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.service.RoomService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
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
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    GameEngine engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService(), new GameStateProjectionService(new LegalActionsService()));
    roomService = new RoomService(messaging, cardDataService);
    botService = new BotService(gameService, cardDataService, new LegalActionsService());
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
    add("legend", "Legend", 0);
    add("champion", "Champion", 0);
    for (int i = 0; i < 10; i++) add("unit-" + i, "Unit", 99);
    add("top-a", "Unit", 0);
    add("top-b", "Unit", 0);
    add("top-c", "Unit", 0);
    add("rest", "Unit", 0);
  }

  @Test
  void roomBotIdMatchesConfiguredBotIds() {
    RoomState room = roomService.create("human", "Human", true);
    LobbyPlayer bot = room.getPlayers().stream()
        .filter(player -> player.getName().equals("RiftBot"))
        .findFirst()
        .orElseThrow();

    assertThat(bot.getId()).isEqualTo(BOT_ID);
    assertThat(ALL_BOT_IDS).contains(bot.getId());
  }

  @Test
  void passPhaseMoveAdvancesAwakenToBeginningForActiveBot() {
    String roomCode = "wake";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setFirstPlayerId(BOT_ID);
    state.setCurrentPhase(Phase.AWAKEN);

    gameService.processMove(roomCode, new PassPhaseMove(BOT_ID));

    LiveGameState latest = gameService.currentState(roomCode);
    assertThat(latest.getCurrentPhase()).isEqualTo(Phase.BEGINNING);
    assertThat(latest.getLog())
        .anySatisfy(entry -> assertThat(entry.text()).isEqualTo("Advanced to BEGINNING"));
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
    waitForNoActingRooms();
  }

  @Test
  void activeBotProgressesFromAwakenToMain() throws Exception {
    String roomCode = "BOT1";
    List<String> playerIds = List.of("human", BOT_ID);
    Map<String, List<String>> decks = Map.of(
        "human", playtestDeck(),
        BOT_ID, playtestDeck());
    Map<String, String> names = Map.of(
        "human", "Human",
        BOT_ID, "RiftBot");
    gameService.initGame(roomCode, playerIds, decks, names);

    LiveGameState initial = gameService.currentState(roomCode);
    initial.setActivePlayerId(BOT_ID);
    initial.setFirstPlayerId(BOT_ID);
    initial.setCurrentPhase(Phase.AWAKEN);

    List<Phase> observed = new ArrayList<>();
    observed.add(initial.getCurrentPhase());
    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, initial));

    long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      LiveGameState state = gameService.currentState(roomCode);
      Phase phase = state.getCurrentPhase();
      if (observed.isEmpty() || observed.get(observed.size() - 1) != phase) observed.add(phase);
      if (phase == Phase.MAIN) {
        state.setWinnerId("test-complete");
        break;
      }
      Thread.sleep(25);
    }

    assertThat(observed).containsSubsequence(
        Phase.AWAKEN,
        Phase.BEGINNING,
        Phase.CHANNEL,
        Phase.DRAW,
        Phase.MAIN);
    waitForNoActingRooms();
  }

  @Test
  void botEventUsesNormalizedRoomCodeWhenLookingUpState() throws Exception {
    String roomCode = "MIX1";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState initial = gameService.currentState("mix1");
    initial.setActivePlayerId(BOT_ID);
    initial.setFirstPlayerId(BOT_ID);
    initial.setCurrentPhase(Phase.AWAKEN);

    List<Phase> observed = new ArrayList<>();
    observed.add(initial.getCurrentPhase());
    botService.onStateChanged(new GameStateChangedEvent(this, "mix1", initial));

    long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      LiveGameState state = gameService.currentState("MiX1");
      Phase phase = state.getCurrentPhase();
      if (observed.isEmpty() || observed.get(observed.size() - 1) != phase) observed.add(phase);
      if (phase == Phase.MAIN) {
        state.setWinnerId("test-complete");
        break;
      }
      Thread.sleep(25);
    }

    assertThat(observed).containsSubsequence(
        Phase.AWAKEN,
        Phase.BEGINNING,
        Phase.CHANNEL,
        Phase.DRAW,
        Phase.MAIN);
    waitForNoActingRooms();
  }

  @Test
  void exactStuckAwakenStateAdvancesBot() throws Exception {
    String roomCode = "2ERB";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState stuck = gameService.currentState("2erb");
    stuck.setActivePlayerId(BOT_ID);
    stuck.setFirstPlayerId("human");
    stuck.setCurrentPhase(Phase.AWAKEN);
    stuck.setTurnNumber(2);
    stuck.setActiveShowdown(null);

    botService.onStateChanged(new GameStateChangedEvent(this, "2erb", stuck));

    LiveGameState latest = waitUntilNotAwakenWithBotActive(gameService, roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getCurrentPhase()).isIn(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN, Phase.END);
    waitForNoActingRooms();
  }

  @Test
  void recoverySweepAdvancesStuckBotAwakenStateWithoutFreshEvent() throws Exception {
    String roomCode = "5MLF";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState stuck = gameService.currentState(roomCode);
    stuck.setActivePlayerId(BOT_ID);
    stuck.setFirstPlayerId(BOT_ID);
    stuck.setCurrentPhase(Phase.AWAKEN);
    stuck.setTurnNumber(1);
    stuck.setActiveShowdown(null);

    botService.recoverMissedBotTurns();

    LiveGameState latest = waitUntilNotAwakenWithBotActive(gameService, roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getCurrentPhase()).isIn(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN, Phase.END);
    waitForNoActingRooms();
  }

  @Test
  void publishedHumanEndToBotAwakenEventAdvancesBot() throws Exception {
    ForwardingEventPublisher publisher = new ForwardingEventPublisher();
    CardEffectRegistry eventedEffects = new CardEffectRegistry(cardDataService);
    CardZoneService eventedCardZoneService = new CardZoneService(cardDataService);
    DeathTriggerService eventedDeathTriggerService = new DeathTriggerService(cardDataService);
    GameService eventedGameService = new GameService(
        new GameEngine(
            new RulesValidator(cardDataService),
            new CombatResolver(
                cardDataService,
                eventedEffects,
                eventedCardZoneService,
                new CombatStatsService(cardDataService),
                eventedDeathTriggerService),
            eventedCardZoneService,
            cardDataService,
            eventedEffects,
            eventedDeathTriggerService,
            new TokenFactory(cardDataService),
            8),
        cardDataService,
        messaging,
        publisher,
        new MatchHistoryService(),
        new GameStateProjectionService(new LegalActionsService()));
    BotService eventedBotService = new BotService(eventedGameService, cardDataService, new LegalActionsService());
    publisher.setBotService(eventedBotService);

    String roomCode = "2ERB";
    eventedGameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = eventedGameService.currentState(roomCode);
    state.setActivePlayerId("human");
    state.setFirstPlayerId("human");
    state.setCurrentPhase(Phase.END);
    state.setTurnNumber(1);

    eventedGameService.processMove("2erb", new PassPhaseMove("human"));

    assertThat(publisher.sawBotAwakenEvent()).isTrue();
    LiveGameState latest = waitUntilNotAwakenWithBotActive(eventedGameService, roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getCurrentPhase()).isIn(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN, Phase.END);
  }

  @Test
  void botDoesNotPassAwakenWhenPassPhaseIsNotLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of()));
    String roomCode = "NOPE";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.AWAKEN);

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    assertThat(gameService.currentState(roomCode).getCurrentPhase()).isEqualTo(Phase.AWAKEN);
  }

  @Test
  void botPassesAwakenOnlyWhenPassPhaseIsLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PASS_PHASE)));
    String roomCode = "PASS";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.AWAKEN);

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilNotAwakenWithBotActive(gameService, roomCode);
    latest.setWinnerId("test-complete");

    assertThat(latest.getCurrentPhase()).isEqualTo(Phase.BEGINNING);
  }

  @Test
  void botPassesShowdownFocusWhenLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PASS_SHOWDOWN_FOCUS)));
    String roomCode = "FOCS";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    state.getCards().add(testCard("bot-attacker", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("human-defender", "human", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        BOT_ID,
        List.of("bot-attacker"),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of(BOT_ID, "human"),
        BOT_ID,
        1,
        false));

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilShowdownReady(gameService, roomCode);
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown()).isNotNull();
    assertThat(latest.getActiveShowdown().readyToResolve()).isTrue();
    assertThat(latest.getActiveShowdown().focusedPlayerId()).isEqualTo(BOT_ID);
  }

  @Test
  void botPassesChainFocusWhenLegal() throws Exception {
    String roomCode = "CHNP";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    state.setChainState(chain(false, BOT_ID));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilChainFocus(gameService, roomCode, "human");
    latest.setWinnerId("test-complete");

    assertThat(latest.getChainState()).isNotNull();
    assertThat(latest.getChainState().focusedPlayerId()).isEqualTo("human");
    assertThat(latest.getChainState().consecutivePasses()).isEqualTo(1);
    waitForNoActingRooms();
  }

  @Test
  void botResolvesReadyChainTopWhenLegal() throws Exception {
    String roomCode = "CHNR";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    state.setChainState(chain(true, BOT_ID));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilNoChain(gameService, roomCode);
    latest.setWinnerId("test-complete");

    assertThat(latest.getChainState()).isNull();
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Chain resolved."));
    waitForNoActingRooms();
  }

  @Test
  void botPassesFocusThenResolvesOnlyAfterShowdownIsReady() throws Exception {
    String roomCode = "BFRS";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    add("bot-attacker", "Unit", 2);
    add("human-defender", "Unit", 1);
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    CardInstance attacker = testCard("bot-attacker", BOT_ID, ZoneName.BATTLEFIELD);
    attacker.setCurrentHealth(2);
    CardInstance defender = testCard("human-defender", "human", ZoneName.BATTLEFIELD);
    defender.setCurrentHealth(1);
    state.getCards().add(attacker);
    state.getCards().add(defender);
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        BOT_ID,
        List.of("bot-attacker"),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of(BOT_ID, "human"),
        "human",
        0,
        false));

    gameService.processMove(roomCode, new PassShowdownFocusMove("human"));
    LiveGameState afterHumanPass = gameService.currentState(roomCode);
    assertThat(afterHumanPass.getActiveShowdown()).isNotNull();
    assertThat(afterHumanPass.getActiveShowdown().readyToResolve()).isFalse();
    assertThat(afterHumanPass.getActiveShowdown().focusedPlayerId()).isEqualTo(BOT_ID);

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, afterHumanPass));
    LiveGameState latest = waitUntilAssigningPlayer(gameService, roomCode, "human");
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown()).isNotNull();
    assertThat(latest.getActiveShowdown().step()).isEqualTo(ShowdownStep.ASSIGN_DAMAGE);
    assertThat(latest.getActiveShowdown().assigningPlayerId()).isEqualTo("human");
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Passed showdown focus. Showdown is ready to resolve."));
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Combat damage assignment started."));
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Assigned attacking combat damage."));
  }

  @Test
  void botAssignsCombatDamageWhenLegal() throws Exception {
    String roomCode = "BDMG";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    add("bot-attacker", "Unit", 1);
    add("human-defender", "Unit", 1);
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    state.getCards().add(testCard("bot-attacker", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("human-defender", "human", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        BOT_ID,
        List.of("bot-attacker"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of(BOT_ID, "human"),
        BOT_ID,
        2,
        true,
        BOT_ID,
        List.of(),
        List.of()));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilAssigningPlayer(gameService, roomCode, "human");
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown()).isNotNull();
    assertThat(latest.getActiveShowdown().attackerAssignments()).containsExactly(
        new LiveGameState.CombatDamageAssignment("bot-attacker", "human-defender", 1));
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Assigned attacking combat damage."));
  }

  @Test
  void botAssignsAllCombatDamageWhenMoreSourcesThanTargets() throws Exception {
    String roomCode = "B2V1";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    addStats("bot-one", "Unit", 3, 3);
    addStats("bot-two", "Unit", 3, 3);
    addStats("human-defender", "Unit", 1, 6);
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    state.getCards().add(testCard("bot-one", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("bot-two", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("human-defender", "human", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        BOT_ID,
        List.of("bot-one", "bot-two"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of(BOT_ID, "human"),
        BOT_ID,
        2,
        true,
        BOT_ID,
        List.of(),
        List.of()));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilAssigningPlayer(gameService, roomCode, "human");
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown()).isNotNull();
    assertThat(latest.getActiveShowdown().attackerAssignments())
        .containsExactly(
            new LiveGameState.CombatDamageAssignment("bot-one", "human-defender", 3),
            new LiveGameState.CombatDamageAssignment("bot-two", "human-defender", 3));
    assertThat(latest.getActiveShowdown().attackerAssignments().stream().mapToInt(LiveGameState.CombatDamageAssignment::amount).sum())
        .isEqualTo(6);
  }

  @Test
  void botAssignmentsRespectTankFirstLethalThenExcessToFinalTarget() throws Exception {
    String roomCode = "B3V2";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    addStats("bot-one", "Unit", 2, 2);
    addStats("bot-two", "Unit", 2, 2);
    addStats("bot-three", "Unit", 2, 2);
    addStats("human-tank", "Unit", 1, 2);
    addStats("human-other", "Unit", 1, 2);
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(BOT_ID);
    CardInstance tank = testCard("human-tank", "human", ZoneName.BATTLEFIELD);
    state.getCards().add(testCard("bot-one", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("bot-two", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("bot-three", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("human-other", "human", ZoneName.BATTLEFIELD));
    state.getCards().add(tank);
    when(cardDataService.hasKeyword(eq(tank), eq("TANK"))).thenReturn(true);
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        BOT_ID,
        List.of("bot-one", "bot-two", "bot-three"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of(BOT_ID, "human"),
        BOT_ID,
        2,
        true,
        BOT_ID,
        List.of(),
        List.of()));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilAssigningPlayer(gameService, roomCode, "human");
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown().attackerAssignments())
        .containsExactly(
            new LiveGameState.CombatDamageAssignment("bot-one", "human-tank", 2),
            new LiveGameState.CombatDamageAssignment("bot-two", "human-other", 2),
            new LiveGameState.CombatDamageAssignment("bot-three", "human-other", 2));
  }

  @Test
  void botDefenderAssignsAllCombatDamageWhenMoreSourcesThanTargetsAndClearsShowdown() throws Exception {
    String roomCode = "BD21";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    addStats("human-attacker", "Unit", 1, 6);
    addStats("bot-one", "Unit", 3, 3);
    addStats("bot-two", "Unit", 3, 3);
    LiveGameState state = gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("human");
    state.getCards().add(testCard("human-attacker", "human", ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("bot-one", BOT_ID, ZoneName.BATTLEFIELD));
    state.getCards().add(testCard("bot-two", BOT_ID, ZoneName.BATTLEFIELD));
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        "human",
        List.of("human-attacker"),
        Map.of(),
        ShowdownStep.ASSIGN_DAMAGE,
        List.of("human", BOT_ID),
        "human",
        2,
        true,
        BOT_ID,
        List.of(new LiveGameState.CombatDamageAssignment("human-attacker", "bot-one", 1)),
        List.of()));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    LiveGameState latest = waitUntilShowdownCleared(gameService, roomCode);
    latest.setWinnerId("test-complete");

    assertThat(latest.getActiveShowdown()).isNull();
    assertThat(latest.getLog()).anyMatch(entry -> entry.text().equals("Combat damage assigned. Showdown resolved."));
  }

  @Test
  void botDoesNotPlayCardDuringAwakenEvenIfPlayCardIsAdvertised() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PLAY_CARD)));
    String roomCode = "NOWP";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.AWAKEN);
    long handBefore = state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .count();

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    LiveGameState latest = gameService.currentState(roomCode);
    long handAfter = latest.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .count();
    assertThat(latest.getCurrentPhase()).isEqualTo(Phase.AWAKEN);
    assertThat(handAfter).isEqualTo(handBefore);
  }

  @Test
  void rulesValidatorStillRejectsManualPlayCardOutsideMain() {
    String roomCode = "RULE";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.AWAKEN);
    String handCardId = state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .findFirst()
        .orElseThrow()
        .getInstanceId();

    assertThatThrownBy(() -> new RulesValidator(cardDataService).validate(
        state,
        new PlayCardMove(BOT_ID, handCardId, ZoneName.BASE, 0, 0, null)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That action can only be taken during MAIN.");
  }

  @Test
  void botDoesNotMoveToBattlefieldWhenActionIsNotLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PASS_PHASE)));
    String roomCode = "NOMV";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.MAIN);
    state.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .setAvailableEnergy(99);
    state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.CHAMPION)
        .forEach(card -> card.setTapped(false));

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    assertThat(gameService.currentState(roomCode).getCards())
        .filteredOn(card -> BOT_ID.equals(card.getOwnerId()))
        .noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD);
  }

  @Test
  void botCanMoveToBattlefieldWhenActionIsLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.MOVE_TO_BATTLEFIELD, LegalAction.PASS_PHASE)));
    String roomCode = "MOVE";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.MAIN);
    state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.CHAMPION)
        .forEach(card -> card.setTapped(false));

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    long deadline = System.currentTimeMillis() + 2_000;
    while (System.currentTimeMillis() < deadline
        && gameService.currentState(roomCode).getCards().stream()
            .noneMatch(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.BATTLEFIELD)) {
      Thread.sleep(25);
    }

    gameService.currentState(roomCode).setWinnerId("test-complete");
    assertThat(gameService.currentState(roomCode).getCards())
        .filteredOn(card -> BOT_ID.equals(card.getOwnerId()))
        .anyMatch(card -> card.getZone() == ZoneName.BATTLEFIELD);
  }

  @Test
  void botDoesNotPlayCardInMainWhenPlayCardIsNotLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PASS_PHASE, LegalAction.TAP_RUNE)));
    String roomCode = "NOPL";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.MAIN);
    long handBefore = state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .count();

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    long handAfter = gameService.currentState(roomCode).getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .count();
    assertThat(handAfter).isEqualTo(handBefore);
  }

  @Test
  void botCanPlayCardInMainWhenPlayCardIsLegal() throws Exception {
    BotService gatedBot = new BotService(
        gameService,
        cardDataService,
        new FixedLegalActionsService((state, playerId) -> Set.of(LegalAction.PLAY_CARD, LegalAction.PASS_PHASE)));
    String roomCode = "PLAY";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.MAIN);
    state.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .setAvailableEnergy(400);
    long handBefore = state.getCards().stream()
        .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .count();

    gatedBot.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    long deadline = System.currentTimeMillis() + 2_000;
    long handAfter = handBefore;
    while (System.currentTimeMillis() < deadline && handAfter == handBefore) {
      Thread.sleep(25);
      handAfter = gameService.currentState(roomCode).getCards().stream()
          .filter(card -> BOT_ID.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
          .count();
    }

    gameService.currentState(roomCode).setWinnerId("test-complete");
    assertThat(handAfter).isLessThan(handBefore);
  }

  @Test
  void botResolvesStackedDeckPendingChoice() throws Exception {
    String roomCode = "CHC1";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        BOT_ID,
        "stacked-deck",
        "source-instance",
        List.of(cards.get("top-a"), cards.get("top-b"), cards.get("top-c"))));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));

    LiveGameState latest = waitUntilNoPendingChoice(gameService, roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getPendingChoice()).isNull();
    assertThat(latest.getCards())
        .anySatisfy(card -> {
          assertThat(card.getOwnerId()).isEqualTo(BOT_ID);
          assertThat(card.getCardId()).isEqualTo("top-a");
          assertThat(card.getZone()).isEqualTo(ZoneName.HAND);
        });
    assertThat(latest.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getDeckPool())
        .containsExactly("rest", "top-b", "top-c");
    waitForNoActingRooms();
  }

  @Test
  void botResolvesPredictPendingChoice() throws Exception {
    String roomCode = "PRED";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    state.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .setDeckPool(new ArrayList<>(List.of("top-a", "top-b", "top-c", "rest")));
    state.setPendingChoice(PendingChoice.predictOrder(
        "choice-1",
        BOT_ID,
        "predict-source",
        "source-instance",
        List.of(cards.get("top-a"), cards.get("top-b"), cards.get("top-c"))));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));

    LiveGameState latest = waitUntilNoPendingChoice(gameService, roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getPendingChoice()).isNull();
    assertThat(latest.getPlayers().stream()
        .filter(player -> BOT_ID.equals(player.getUserId()))
        .findFirst()
        .orElseThrow()
        .getDeckPool())
        .containsExactly("top-c", "top-b", "top-a", "rest");
    waitForNoActingRooms();
  }

  @Test
  void botDoesNotAutoResolveHumanPendingChoice() throws Exception {
    String roomCode = "HCHC";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId("human");
    state.setPendingChoice(PendingChoice.topDeckPickOne(
        "choice-1",
        "human",
        "stacked-deck",
        "source-instance",
        List.of(cards.get("top-a"), cards.get("top-b"))));

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    assertThat(gameService.currentState(roomCode).getPendingChoice()).isNotNull();
  }

  @Test
  void unsupportedBotChoiceDoesNotSpinForever() throws Exception {
    String roomCode = "UNKN";
    gameService.initGame(
        roomCode,
        List.of("human", BOT_ID),
        Map.of("human", playtestDeck(), BOT_ID, playtestDeck()),
        Map.of("human", "Human", BOT_ID, "RiftBot"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setActivePlayerId(BOT_ID);
    PendingChoice choice = new PendingChoice();
    choice.setChoiceId("choice-1");
    choice.setPlayerId(BOT_ID);
    choice.setType("UNKNOWN_PRIVATE_CHOICE");
    choice.setPrompt("Unknown choice");
    state.setPendingChoice(choice);

    botService.onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    Thread.sleep(900);

    assertThat(gameService.currentState(roomCode).getPendingChoice()).isNotNull();
    waitForNoActingRooms();
  }

  private void add(String id, String type, int cost) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), cost, 0, null, null, null, null, 1, 1, List.of()));
  }

  private void addStats(String id, String type, int might, int health) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, null, might, health, List.of()));
  }

  private List<String> playtestDeck() {
    List<String> deck = new ArrayList<>();
    deck.add("legend");
    deck.add("champion");
    for (int i = 0; i < 10; i++) {
      deck.add("unit-" + i);
      deck.add("unit-" + i);
    }
    return deck;
  }

  private CardInstance testCard(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }

  @SuppressWarnings("unchecked")
  private Set<String> actingRooms() throws Exception {
    Field field = BotService.class.getDeclaredField("actingRooms");
    field.setAccessible(true);
    return (Set<String>) field.get(botService);
  }

  private void waitForNoActingRooms() throws Exception {
    long deadline = System.currentTimeMillis() + 2_000;
    while (System.currentTimeMillis() < deadline) {
      if (actingRooms().isEmpty()) return;
      Thread.sleep(25);
    }
    assertThat(actingRooms()).isEmpty();
  }

  private LiveGameState waitUntilNotAwakenWithBotActive(GameService service, String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null
          && !(latest.getCurrentPhase() == Phase.AWAKEN && BOT_ID.equals(latest.getActivePlayerId()))) {
        return latest;
      }
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState waitUntilNoPendingChoice(GameService service, String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null && latest.getPendingChoice() == null) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState waitUntilShowdownReady(GameService service, String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null && latest.getActiveShowdown() != null && latest.getActiveShowdown().readyToResolve()) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState waitUntilShowdownCleared(GameService service, String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null && latest.getActiveShowdown() == null) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState waitUntilNoChain(GameService service, String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null && latest.getChainState() == null) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState waitUntilChainFocus(GameService service, String roomCode, String playerId) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null
          && latest.getChainState() != null
          && playerId.equals(latest.getChainState().focusedPlayerId())) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private LiveGameState.ChainState chain(boolean readyToResolve, String focusedPlayerId) {
    return new LiveGameState.ChainState(
        "chain-1",
        List.of(new LiveGameState.ChainItem(
            "item-1",
            BOT_ID,
            "source-1",
            "source-card",
            "Source Card",
            LiveGameState.ChainItem.EFFECT_NO_OP_TEST,
            List.of(),
            1,
            "bot test chain item")),
        List.of(BOT_ID, "human"),
        focusedPlayerId,
        readyToResolve ? 2 : 0,
        readyToResolve,
        "TEST");
  }

  private LiveGameState waitUntilAssigningPlayer(GameService service, String roomCode, String playerId) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = service.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = service.currentState(roomCode);
      if (latest != null
          && latest.getActiveShowdown() != null
          && playerId.equals(latest.getActiveShowdown().assigningPlayerId())) return latest;
      Thread.sleep(25);
    }
    return latest;
  }

  private static final class ForwardingEventPublisher implements ApplicationEventPublisher {
    private BotService botService;
    private volatile boolean sawBotAwakenEvent;

    void setBotService(BotService botService) {
      this.botService = botService;
    }

    boolean sawBotAwakenEvent() {
      return sawBotAwakenEvent;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
      publishEvent((Object) event);
    }

    @Override
    public void publishEvent(Object event) {
      if (event instanceof GameStateChangedEvent gameEvent) {
        if (gameEvent.getState().getCurrentPhase() == Phase.AWAKEN
            && BOT_ID.equals(gameEvent.getState().getActivePlayerId())) {
          sawBotAwakenEvent = true;
        }
        if (botService != null) botService.onStateChanged(gameEvent);
      }
    }
  }

  private static final class FixedLegalActionsService extends LegalActionsService {
    private final BiFunction<LiveGameState, String, Set<LegalAction>> delegate;

    private FixedLegalActionsService(BiFunction<LiveGameState, String, Set<LegalAction>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Set<LegalAction> legalActionsFor(LiveGameState state, String playerId) {
      return delegate.apply(state, playerId);
    }
  }
}
