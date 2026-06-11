package com.riftforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.riftforge.config.WebSocketConfig;
import com.riftforge.matchmaking.MatchmakingService;
import com.riftforge.service.GameService;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.PresenceService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    // Unroutable port: card loading must fall back to cache/placeholder so
    // startup never depends on the external Riftcodex API.
    "riftforge.riftcodex-api=http://127.0.0.1:9/cards-unreachable-in-tests",
    "spring.main.allow-circular-references=false"
})
@AutoConfigureMockMvc
class ApplicationStartupIntegrationTest {
  @Autowired ApplicationContext context;
  @Autowired MockMvc mockMvc;
  @Autowired PresenceService presenceService;

  @Test
  void applicationContextStartsWithCoreBeansWired() {
    assertThat(context.getBean(WebSocketConfig.class)).isNotNull();
    assertThat(context.getBean(PresenceService.class)).isNotNull();
    assertThat(context.getBean(RoomService.class)).isNotNull();
    assertThat(context.getBean(MatchmakingService.class)).isNotNull();
    assertThat(context.getBean(GameService.class)).isNotNull();
    assertThat(context.getBean(RoomTokenService.class)).isNotNull();
    assertThat(context.getBean(GameStateProjectionService.class)).isNotNull();
    assertThat(context.containsBean("brokerMessagingTemplate")).isTrue();
  }

  @Test
  void presenceServiceDoesNotDependOnTheMessagingTemplate() {
    for (Constructor<?> constructor : PresenceService.class.getDeclaredConstructors()) {
      assertThat(constructor.getParameterTypes())
          .noneMatch(SimpMessagingTemplate.class::isAssignableFrom);
    }
    for (Field field : PresenceService.class.getDeclaredFields()) {
      assertThat(SimpMessagingTemplate.class.isAssignableFrom(field.getType()))
          .as("PresenceService field %s must not hold a messaging template", field.getName())
          .isFalse();
    }
    assertThatNoException().isThrownBy(PresenceService::new);
  }

  @Test
  void presenceEndpointReturnsAggregateCounts() throws Exception {
    mockMvc.perform(get("/api/presence"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onlinePlayers").isNumber())
        .andExpect(jsonPath("$.activeRooms").isNumber())
        .andExpect(jsonPath("$.playersSearching").isNumber())
        .andExpect(jsonPath("$.queueSize").isNumber());
  }

  @Test
  void presenceMisuseDoesNotBreakTheRunningApplication() throws Exception {
    presenceService.connect(null, "player", "ROOM", "PLAYER");
    presenceService.connect("session", null, null, null);
    presenceService.connect("session", "", "", "");
    presenceService.disconnect("never-connected-session");
    presenceService.disconnect(null);

    mockMvc.perform(get("/api/presence")).andExpect(status().isOk());
  }
}
