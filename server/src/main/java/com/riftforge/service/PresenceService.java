package com.riftforge.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Service
public class PresenceService {
  public static final String ATTR_PLAYER_ID = "playerId";
  public static final String ATTR_ROOM_CODE = "roomCode";
  public static final String ATTR_ROLE = "role";

  private final Map<String, PresenceSession> sessions = new ConcurrentHashMap<>();
  private final SimpMessagingTemplate messaging;

  public PresenceService(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  public void connect(String sessionId, String playerId, String roomCode, String role) {
    if (isBlank(sessionId) || isBlank(playerId)) return;
    sessions.put(sessionId, new PresenceSession(playerId, normalizeRoomCode(roomCode), role));
    broadcast();
  }

  public void disconnect(String sessionId) {
    if (isBlank(sessionId)) return;
    if (sessions.remove(sessionId) != null) broadcast();
  }

  public PresenceSummary summary(int playersSearching) {
    Set<String> playerIds = ConcurrentHashMap.newKeySet();
    Set<String> roomCodes = ConcurrentHashMap.newKeySet();
    sessions.values().forEach(session -> {
      if (!isBlank(session.playerId())) playerIds.add(session.playerId());
      if (!isBlank(session.roomCode())) roomCodes.add(session.roomCode());
    });
    return new PresenceSummary(playerIds.size(), roomCodes.size(), playersSearching, playersSearching);
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    disconnect(event.getSessionId());
  }

  private void broadcast() {
    // TODO: Push live matchmaking queue size here when queue-change events are emitted.
    messaging.convertAndSend("/topic/presence", summary(0));
  }

  private String normalizeRoomCode(String roomCode) {
    return isBlank(roomCode) ? null : roomCode.toUpperCase();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record PresenceSession(String playerId, String roomCode, String role) {
    private PresenceSession {
      Objects.requireNonNull(playerId);
    }
  }

  public record PresenceSummary(int onlinePlayers, int activeRooms, int playersSearching, int queueSize) {}
}
