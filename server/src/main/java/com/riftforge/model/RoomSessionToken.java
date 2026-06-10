package com.riftforge.model;

import java.time.Instant;

public record RoomSessionToken(
    String token,
    String roomCode,
    String playerId,
    String role,
    Instant issuedAt,
    Instant expiresAt
) {}
