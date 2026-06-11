package com.riftforge.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Service
public class PresenceService {
  public static final String ATTR_PLAYER_ID = "playerId";
  public static final String ATTR_ROOM_CODE = "roomCode";
  public static final String ATTR_ROLE = "role";

  private final Map<String, PresenceSession> sessions = new ConcurrentHashMap<>();

  public void connect(String sessionId, String playerId, String roomCode, String role) {
    if (isBlank(sessionId) || isBlank(playerId)) return;
    sessions.put(sessionId, new PresenceSession(playerId, normalizeRoomCode(roomCode), role));
  }

  public void disconnect(String sessionId) {
    if (isBlank(sessionId)) return;
    sessions.remove(sessionId);
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
