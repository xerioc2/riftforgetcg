package com.riftforge.service;

import com.riftforge.engine.GameEngine;
import com.riftforge.engine.IllegalMoveException;
import com.riftforge.bot.GameStateChangedEvent;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class GameService {
  private final ConcurrentHashMap<String, LiveGameState> games = new ConcurrentHashMap<>();
  private final GameEngine engine;
  private final CardDataService cardDataService;
  private final SimpMessagingTemplate messaging;
  private final ApplicationEventPublisher eventPublisher;

  public GameService(GameEngine engine, CardDataService cardDataService, SimpMessagingTemplate messaging, ApplicationEventPublisher eventPublisher) {
    this.engine = engine;
    this.cardDataService = cardDataService;
    this.messaging = messaging;
    this.eventPublisher = eventPublisher;
  }

  public void initGame(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer) {
    LiveGameState state = createInitialState(roomCode, playerIds, decksByPlayer);
    games.put(roomCode, state);
    broadcast(roomCode, state);
  }

  public void processMove(String roomCode, MoveRequest move) {
    LiveGameState state = games.get(roomCode);
    if (state == null) throw new IllegalStateException("Game not found: " + roomCode);
    try {
      LiveGameState next = engine.applyMove(state, move);
      games.put(roomCode, next);
      broadcast(roomCode, next);
    } catch (IllegalMoveException e) {
      broadcastError(roomCode, e.getMessage(), move.playerId());
    } catch (Exception e) {
      broadcastError(roomCode, "Server error: " + e.getMessage(), move.playerId());
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

  public LiveGameState reset(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer) {
    LiveGameState state = createInitialState(roomCode, playerIds, decksByPlayer);
    games.put(roomCode, state);
    broadcast(roomCode, state);
    return state;
  }

  private LiveGameState createInitialState(String roomCode, List<String> playerIds, Map<String, List<String>> decksByPlayer) {
    LiveGameState state = new LiveGameState();
    state.setRoomCode(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId(playerIds.isEmpty() ? null : playerIds.get(0));
    state.setTurnNumber(1);
    state.setUpdatedAt(Instant.now().toString());
    state.setPlayers(playerIds.stream().map(id -> {
      PlayerState player = new PlayerState();
      player.setUserId(id);
      player.setScore(0);
      player.setAvailableEnergy(0);
      return player;
    }).toList());

    int zIndex = 0;
    for (String playerId : playerIds) {
      List<String> dealable = decksByPlayer.getOrDefault(playerId, List.of()).stream()
          .filter(id -> !"Champion".equalsIgnoreCase(cardDataService.getCard(id).type()))
          .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
      Collections.shuffle(dealable);
      List<String> hand = dealable.stream().limit(5).toList();
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
          .ifPresent(player -> player.setDeckPool(new ArrayList<>(dealable.subList(Math.min(5, dealable.size()), dealable.size()))));
    }
    if (!playerIds.isEmpty()) {
      for (int i = 0; i < 2; i++) {
        RuneState rune = new RuneState();
        rune.setInstanceId(UUID.randomUUID().toString());
        rune.setOwnerId(playerIds.get(0));
        rune.setTapped(false);
        rune.setNormalEnergy(1);
        rune.setPremiumEnergy(2);
        state.getRunes().add(rune);
      }
    }
    return state;
  }

  private void broadcast(String roomCode, LiveGameState state) {
    messaging.convertAndSend("/topic/game/" + roomCode, new GameMessage.StateUpdate(state));
    eventPublisher.publishEvent(new GameStateChangedEvent(this, roomCode, state));
  }

  private void broadcastError(String roomCode, String message, String playerId) {
    messaging.convertAndSend("/topic/game/" + roomCode, new GameMessage.GameError(message, playerId));
  }
}
