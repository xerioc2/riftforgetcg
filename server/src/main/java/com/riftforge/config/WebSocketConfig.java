package com.riftforge.config;

import com.riftforge.service.RoomTokenService;
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
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final RoomTokenService roomTokenService;

  public WebSocketConfig(RoomTokenService roomTokenService) {
    this.roomTokenService = roomTokenService;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue", "/user");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
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
          if (isBlank(roomCode) && isBlank(sessionToken)) {
            return msg;
          }
          if (isBlank(playerId) || !roomTokenService.validate(sessionToken, roomCode, playerId)) {
            throw new MessageDeliveryException("Invalid or missing session token.");
          }
          if (!playerId.isBlank()) {
            acc.setUser(() -> playerId);
          }
        }
        return msg;
      }
    });
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
