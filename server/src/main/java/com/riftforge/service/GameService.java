package com.riftforge.service;

import com.riftforge.engine.GameEngine;
import com.riftforge.engine.IllegalMoveException;
import com.riftforge.bot.GameStateChangedEvent;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.CompletedMatchSnapshot;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.MatchRecord;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveRequest;
import com.riftforge.websocket.GameMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

@Service
public class GameService {
  private static final Logger log = LoggerFactory.getLogger(GameService.class);
  private final ConcurrentHashMap<String, LiveGameState> games = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();
  private final GameEngine engine;
  private final CardDataService cardDataService;
  private final SimpMessagingTemplate messaging;
  private final ApplicationEventPublisher eventPublisher;
  private final MatchHistoryService matchHistoryService;

  public GameService(GameEngine engine, CardDataService cardDataService, SimpMessagingTemplate messaging, ApplicationEventPublisher eventPublisher, MatchHistoryService matchHistoryService) {
    this.engine = engine;
    this.cardDataService = cardDataService;
    this.messaging = messaging;
    this.eventPublisher = eventPublisher;
    this.matchHistoryService = matchHistoryService;
  }

  public void initGame(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer, Map<String, String> playerNames) {
    initGame(roomCode, playerIds, decksByPlayer, playerNames, GameMode.ENFORCED);
  }

  public void initGame(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer, Map<String, String> playerNames, GameMode gameMode) {
    if (games.containsKey(roomCode)) {
      log.warn("initGame called on existing game {}, ignoring", roomCode);
      return;
    }
    LiveGameState state = createInitialState(roomCode, playerIds, decksByPlayer, playerNames, gameMode);
    games.put(roomCode, state);
    broadcast(roomCode, state);
  }

  public void processMove(String roomCode, MoveRequest move) {
    String normalizedRoomCode = roomCode.toUpperCase();
    ReentrantLock lock = lockFor(normalizedRoomCode);
    LiveGameState next = null;
    CompletedMatchSnapshot completedMatch = null;
    String validationError = null;
    Exception unexpectedError = null;
    lock.lock();
    try {
      LiveGameState before = games.get(normalizedRoomCode);
      if (before == null) throw new IllegalStateException("Game not found: " + normalizedRoomCode);
      String previousWinnerId = before.getWinnerId();
      next = engine.applyMove(before, move);
      games.put(normalizedRoomCode, next);
      if (next.getWinnerId() != null && previousWinnerId == null) {
        completedMatch = completedMatchSnapshot(next);
      }
    } catch (IllegalMoveException e) {
      validationError = e.getMessage();
    } catch (Exception e) {
      unexpectedError = e;
    } finally {
      lock.unlock();
    }

    if (next != null) {
      if (completedMatch != null) matchHistoryService.record(completedMatch);
      broadcast(normalizedRoomCode, next);
    } else if (validationError != null) {
      broadcastError(normalizedRoomCode, validationError, move.playerId());
    } else if (unexpectedError != null) {
      log.error("Failed to process {} in room {}", move.getClass().getSimpleName(), normalizedRoomCode, unexpectedError);
      broadcastError(normalizedRoomCode, "Server error: " + unexpectedError.getMessage(), move.playerId());
    }
  }

  public void sendCurrentStateTo(String roomCode, String userId) {
    LiveGameState state = games.get(roomCode);
    if (state != null) {
      messaging.convertAndSendToUser(userId, "/topic/game/" + roomCode, new GameMessage.StateUpdate(state));
      eventPublisher.publishEvent(new GameStateChangedEvent(this, roomCode, state));
    }
  }

  public LiveGameState currentState(String roomCode) {
    return games.get(roomCode);
  }

  @Scheduled(fixedDelay = 3_600_000)
  public void evictFinishedGames() {
    games.entrySet().removeIf(entry -> entry.getValue().getWinnerId() != null);
    roomLocks.keySet().removeIf(roomCode -> !games.containsKey(roomCode));
    log.info("Evicted finished games; {} active games remain", games.size());
  }

  public LiveGameState reset(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer, Map<String, String> playerNames) {
    return reset(roomCode, playerIds, decksByPlayer, playerNames, GameMode.ENFORCED);
  }

  public LiveGameState reset(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer, Map<String, String> playerNames, GameMode gameMode) {
    String normalizedRoomCode = roomCode.toUpperCase();
    ReentrantLock lock = lockFor(normalizedRoomCode);
    LiveGameState state;
    lock.lock();
    try {
      state = createInitialState(normalizedRoomCode, playerIds, decksByPlayer, playerNames, gameMode);
      games.put(normalizedRoomCode, state);
    } finally {
      lock.unlock();
    }
    broadcast(normalizedRoomCode, state);
    return state;
  }

  private LiveGameState createInitialState(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer, Map<String, String> playerNames, GameMode gameMode) {
    LiveGameState state = new LiveGameState();
    state.setRoomCode(roomCode);
    state.setGameMode(gameMode);
    state.setCurrentPhase(Phase.MULLIGAN);
    String firstPlayerId = playerIds.isEmpty() ? null
        : playerIds.get(ThreadLocalRandom.current().nextInt(playerIds.size()));
    state.setActivePlayerId(firstPlayerId);
    state.setFirstPlayerId(firstPlayerId);
    state.setTurnNumber(1);
    state.setUpdatedAt(Instant.now().toString());
    state.setPlayers(playerIds.stream().map(id -> {
      PlayerState player = new PlayerState();
      player.setUserId(id);
      player.setName(playerNames.getOrDefault(id, id));
      player.setScore(0);
      player.setAvailableEnergy(0);
      player.setRunePoolRemaining(10);
      return player;
    }).toList());

    int zIndex = 0;
    for (String playerId : playerIds) {
      List<String> deck = decksByPlayer.getOrDefault(playerId, List.of());
      List<String> champions = deck.stream()
          .filter(id -> "Champion".equalsIgnoreCase(cardDataService.getCard(id).type()))
          .toList();
      List<String> legends = deck.stream()
          .filter(id -> "Legend".equalsIgnoreCase(cardDataService.getCard(id).type()))
          .toList();
      List<String> dealable = deck.stream()
          .filter(id -> !"Champion".equalsIgnoreCase(cardDataService.getCard(id).type()))
          .filter(id -> !"Legend".equalsIgnoreCase(cardDataService.getCard(id).type()))
          .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
      if (!champions.isEmpty()) {
        state.getCards().add(createZoneCard(champions.get(0), playerId, ZoneName.CHAMPION, ++zIndex));
      }
      if (!legends.isEmpty()) {
        state.getCards().add(createZoneCard(legends.get(0), playerId, ZoneName.LEGEND, ++zIndex));
      }
      Collections.shuffle(dealable);
      List<String> hand = dealable.stream().limit(4).toList();
      for (int i = 0; i < hand.size(); i++) {
        CardDefinition def = cardDataService.getCard(hand.get(i));
        CardInstance instance = new CardInstance();
        instance.setInstanceId(UUID.randomUUID().toString());
        instance.setCardId(hand.get(i));
        instance.setOwnerId(playerId);
        instance.setZone(ZoneName.HAND);
        instance.setX(120 + i * 92);
        instance.setY(0);
        instance.setCurrentHealth(def.health());
        instance.setHasSummoningSickness(true);
        instance.setTempKeywords(new ArrayList<>());
        instance.setZIndex(++zIndex);
        state.getCards().add(instance);
      }
      state.getPlayers().stream()
          .filter(player -> player.getUserId().equals(playerId))
          .findFirst()
          .ifPresent(player -> player.setDeckPool(new ArrayList<>(dealable.subList(Math.min(4, dealable.size()), dealable.size()))));
    }
    return state;
  }

  private CardInstance createZoneCard(String cardId, String playerId, ZoneName zone, int zIndex) {
    CardDefinition def = cardDataService.getCard(cardId);
    CardInstance card = new CardInstance();
    card.setInstanceId(UUID.randomUUID().toString());
    card.setCardId(cardId);
    card.setOwnerId(playerId);
    card.setZone(zone);
    card.setX(0);
    card.setY(0);
    card.setCurrentHealth(def.health());
    card.setHasSummoningSickness(false);
    card.setTempKeywords(new ArrayList<>());
    card.setZIndex(zIndex);
    return card;
  }

  private void broadcast(String roomCode, LiveGameState state) {
    messaging.convertAndSend("/topic/game/" + roomCode, new GameMessage.StateUpdate(state));
    eventPublisher.publishEvent(new GameStateChangedEvent(this, roomCode, state));
  }

  private void broadcastError(String roomCode, String message, String playerId) {
    messaging.convertAndSend("/topic/game/" + roomCode, new GameMessage.GameError(message, playerId));
  }

  private CompletedMatchSnapshot completedMatchSnapshot(LiveGameState state) {
    List<MatchRecord.PlayerSummary> players = state.getPlayers().stream()
        .map(player -> new MatchRecord.PlayerSummary(
            player.getUserId(),
            player.getName() == null || player.getName().isBlank() ? player.getUserId() : player.getName(),
            player.getScore()))
        .toList();
    boolean hasBotPlayer = state.getPlayers().stream()
        .anyMatch(player -> ALL_BOT_IDS.contains(player.getUserId()));
    return new CompletedMatchSnapshot(state.getTurnNumber(), state.getWinnerId(), players, hasBotPlayer);
  }

  private ReentrantLock lockFor(String roomCode) {
    return roomLocks.computeIfAbsent(roomCode.toUpperCase(), ignored -> new ReentrantLock());
  }
}
