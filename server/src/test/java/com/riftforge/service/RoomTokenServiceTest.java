package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.RoomSessionToken;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class RoomTokenServiceTest {
  @Test
  void issuedTokenValidatesSuccessfully() {
    RoomTokenService service = new RoomTokenService();

    String token = service.issue("ABCD", "player-1", "PLAYER");

    assertThat(service.validate(token, "ABCD", "player-1")).isTrue();
  }

  @Test
  void wrongRoomCodeFailsValidation() {
    RoomTokenService service = new RoomTokenService();

    String token = service.issue("ABCD", "player-1", "PLAYER");

    assertThat(service.validate(token, "WXYZ", "player-1")).isFalse();
  }

  @Test
  void wrongPlayerIdFailsValidation() {
    RoomTokenService service = new RoomTokenService();

    String token = service.issue("ABCD", "player-1", "PLAYER");

    assertThat(service.validate(token, "ABCD", "player-2")).isFalse();
  }

  @Test
  void expiredTokenFailsValidation() throws ReflectiveOperationException {
    RoomTokenService service = new RoomTokenService();
    Instant issuedAt = Instant.now().minusSeconds(10);
    String token = "expired-token";
    RoomSessionToken expired = new RoomSessionToken(
        token,
        "ABCD",
        "player-1",
        "PLAYER",
        issuedAt,
        Instant.now().minusSeconds(1)
    );

    tokens(service).put(token, expired);

    assertThat(service.validate(token, "ABCD", "player-1")).isFalse();
  }

  @Test
  void reissuingForSameRoomAndPlayerInvalidatesOldToken() {
    RoomTokenService service = new RoomTokenService();

    String oldToken = service.issue("ABCD", "player-1", "PLAYER");
    String newToken = service.issue("ABCD", "player-1", "PLAYER");

    assertThat(service.validate(oldToken, "ABCD", "player-1")).isFalse();
    assertThat(service.validate(newToken, "ABCD", "player-1")).isTrue();
  }

  @SuppressWarnings("unchecked")
  private ConcurrentHashMap<String, RoomSessionToken> tokens(RoomTokenService service) throws ReflectiveOperationException {
    Field field = RoomTokenService.class.getDeclaredField("tokens");
    field.setAccessible(true);
    return (ConcurrentHashMap<String, RoomSessionToken>) field.get(service);
  }
}
