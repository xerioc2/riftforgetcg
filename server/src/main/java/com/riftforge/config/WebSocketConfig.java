package com.riftforge.config;

import com.riftforge.service.PresenceService;
import com.riftforge.service.RoomTokenService;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.util.UriUtils;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private static final String ATTR_SESSION_TOKEN = "sessionToken";

  private final RoomTokenService roomTokenService;
  private final PresenceService presenceService;

  public WebSocketConfig(RoomTokenService roomTokenService, PresenceService presenceService) {
    this.roomTokenService = roomTokenService;
    this.presenceService = presenceService;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new PlayerHandshakeInterceptor())
        .setHandshakeHandler(new PlayerHandshakeHandler());
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration reg) {
    reg.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> msg, MessageChannel ch) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
        if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
          String playerId = acc.getFirstNativeHeader("playerId");
          String roomCode = acc.getFirstNativeHeader("roomCode");
          String sessionToken = acc.getFirstNativeHeader("sessionToken");
          String role = acc.getFirstNativeHeader("role");
          Map<String, Object> attrs = acc.getSessionAttributes();
          playerId = firstNonBlank(playerId, attr(attrs, PresenceService.ATTR_PLAYER_ID));
          roomCode = firstNonBlank(roomCode, attr(attrs, PresenceService.ATTR_ROOM_CODE));
          sessionToken = firstNonBlank(sessionToken, attr(attrs, ATTR_SESSION_TOKEN));
          role = firstNonBlank(role, attr(attrs, PresenceService.ATTR_ROLE));
          if (acc.getUser() != null && !isBlank(playerId) && !acc.getUser().getName().equals(playerId)) {
            throw new MessageDeliveryException("Player ID mismatch.");
          }
          boolean roomScoped = !isBlank(roomCode) || !isBlank(sessionToken);
          if (roomScoped && (isBlank(playerId) || !roomTokenService.validate(sessionToken, roomCode, playerId))) {
            throw new MessageDeliveryException("Invalid or missing session token.");
          }
          if (!isBlank(playerId)) {
            String resolvedPlayerId = playerId;
            if (acc.getUser() == null) acc.setUser(() -> resolvedPlayerId);
            String resolvedRole = isBlank(role) ? (roomScoped ? "PLAYER" : "GLOBAL") : role;
            if (attrs != null) {
              putIfPresent(attrs, PresenceService.ATTR_PLAYER_ID, resolvedPlayerId);
              putIfPresent(attrs, PresenceService.ATTR_ROOM_CODE, normalizeRoomCode(roomCode));
              putIfPresent(attrs, PresenceService.ATTR_ROLE, resolvedRole);
            }
            presenceService.connect(acc.getSessionId(), resolvedPlayerId, roomCode, resolvedRole);
          }
        }
        return msg;
      }
    });
  }

  private class PlayerHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
      Map<String, String> query = queryParams(request);
      String playerId = query.get("playerId");
      String roomCode = query.get("roomCode");
      String sessionToken = query.get("sessionToken");
      String role = query.get("role");
      boolean roomScoped = !isBlank(roomCode) || !isBlank(sessionToken);
      if (roomScoped && (isBlank(playerId) || !roomTokenService.validate(sessionToken, roomCode, playerId))) {
        return false;
      }
      if (!isBlank(playerId)) putIfPresent(attributes, PresenceService.ATTR_PLAYER_ID, playerId);
      if (!isBlank(roomCode)) putIfPresent(attributes, PresenceService.ATTR_ROOM_CODE, normalizeRoomCode(roomCode));
      if (!isBlank(sessionToken)) putIfPresent(attributes, ATTR_SESSION_TOKEN, sessionToken);
      putIfPresent(attributes, PresenceService.ATTR_ROLE, firstNonBlank(role, roomScoped ? "PLAYER" : "GLOBAL"));
      return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
      // No-op.
    }
  }

  private class PlayerHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
      String playerId = attr(attributes, PresenceService.ATTR_PLAYER_ID);
      return isBlank(playerId) ? super.determineUser(request, wsHandler, attributes) : () -> playerId;
    }
  }

  private Map<String, String> queryParams(ServerHttpRequest request) {
    String rawQuery = request.getURI().getRawQuery();
    Map<String, String> params = new HashMap<>();
    if (isBlank(rawQuery)) return params;
    for (String part : rawQuery.split("&")) {
      if (part.isBlank()) continue;
      String[] pair = part.split("=", 2);
      String key = UriUtils.decode(pair[0], StandardCharsets.UTF_8);
      String value = pair.length > 1 ? UriUtils.decode(pair[1], StandardCharsets.UTF_8) : "";
      params.put(key, value);
    }
    return params;
  }

  private String attr(Map<String, Object> attrs, String key) {
    if (attrs == null) return null;
    Object value = attrs.get(key);
    return value instanceof String text ? text : null;
  }

  private void putIfPresent(Map<String, Object> attrs, String key, String value) {
    if (attrs != null && !isBlank(value)) attrs.put(key, value);
  }

  private String firstNonBlank(String first, String second) {
    return isBlank(first) ? second : first;
  }

  private String normalizeRoomCode(String roomCode) {
    return isBlank(roomCode) ? null : roomCode.toUpperCase();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
