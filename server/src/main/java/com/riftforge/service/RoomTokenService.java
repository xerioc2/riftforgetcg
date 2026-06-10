package com.riftforge.service;

import com.riftforge.model.RoomSessionToken;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RoomTokenService {
  private static final int TOKEN_TTL_HOURS = 8;

  private final ConcurrentHashMap<String, RoomSessionToken> tokens = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> tokenByRoomPlayer = new ConcurrentHashMap<>();

  public String issue(String roomCode, String playerId, String role) {
    String key = roomPlayerKey(roomCode, playerId);
    String previousToken = tokenByRoomPlayer.remove(key);
    if (previousToken != null) tokens.remove(previousToken);

    Instant issuedAt = Instant.now();
    String token = UUID.randomUUID().toString();
    RoomSessionToken sessionToken = new RoomSessionToken(
        token,
        normalizeRoomCode(roomCode),
        playerId,
        role,
        issuedAt,
        issuedAt.plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS)
    );
    tokens.put(token, sessionToken);
    tokenByRoomPlayer.put(key, token);
    return token;
  }

  public boolean validate(String token, String roomCode, String playerId) {
    if (token == null || token.isBlank() || roomCode == null || roomCode.isBlank() || playerId == null || playerId.isBlank()) {
      return false;
    }
    RoomSessionToken sessionToken = tokens.get(token);
    return sessionToken != null
        && !sessionToken.expiresAt().isBefore(Instant.now())
        && sessionToken.roomCode().equals(normalizeRoomCode(roomCode))
        && sessionToken.playerId().equals(playerId);
  }

  private String roomPlayerKey(String roomCode, String playerId) {
    return normalizeRoomCode(roomCode) + ":" + playerId;
  }

  private String normalizeRoomCode(String roomCode) {
    return roomCode == null ? "" : roomCode.toUpperCase();
  }
}
