package com.riftforge.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.riftforge.model.CardDefinition;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.RoomTokenService;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "riftforge.riftcodex-api=http://127.0.0.1:9/cards-unreachable-in-tests",
        "spring.main.allow-circular-references=false"
    })
class WebSocketUserDestinationIntegrationTest {
  private static final String PLAYER_A = "player-a";
  private static final String PLAYER_B = "player-b";

  @LocalServerPort int port;
  @Autowired GameService gameService;
  @Autowired RoomTokenService roomTokenService;
  @MockBean CardDataService cardDataService;

  private final Map<String, CardDefinition> cards = new LinkedHashMap<>();

  @BeforeEach
  void setUp() {
    cards.clear();
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
    add("legend", "Legend", "Legend");
    add("champion", "Champion", "Champion");
    for (int i = 0; i < 12; i++) add("unit-" + i, "Unit " + i, "Unit");
  }

  @Test
  void userDestinationDeliversPlayerProjectionOnlyToThatPlayer() throws Exception {
    String roomCode = "WS" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    gameService.initGame(
        roomCode,
        List.of(PLAYER_A, PLAYER_B),
        Map.of(PLAYER_A, deck(), PLAYER_B, deck()),
        Map.of(PLAYER_A, "Player A", PLAYER_B, "Player B"));
    String tokenA = roomTokenService.issue(roomCode, PLAYER_A, "PLAYER");
    String tokenB = roomTokenService.issue(roomCode, PLAYER_B, "PLAYER");

    WebSocketStompClient stompClient = stompClient();
    StompSession sessionA = connect(stompClient, roomCode, PLAYER_A, tokenA);
    StompSession sessionB = connect(stompClient, roomCode, PLAYER_B, tokenB);
    BlockingQueue<JsonNode> playerAMessages = new LinkedBlockingQueue<>();
    BlockingQueue<JsonNode> playerBMessages = new LinkedBlockingQueue<>();
    subscribeAndSettle(sessionA, "/user/topic/game/" + roomCode, playerAMessages);
    subscribeAndSettle(sessionB, "/user/topic/game/" + roomCode, playerBMessages);

    gameService.sendCurrentStateTo(roomCode, PLAYER_A);

    JsonNode body = playerAMessages.poll(3, TimeUnit.SECONDS);
    assertThat(body).isNotNull();
    JsonNode state = body.path("state");
    assertOwnHandVisibleAndOpponentHandMasked(state, PLAYER_A, PLAYER_B);
    assertThat(playerBMessages.poll(500, TimeUnit.MILLISECONDS)).isNull();

    sessionA.disconnect();
    sessionB.disconnect();
    stompClient.stop();
  }

  @Test
  void invalidRoomTokenConnectionIsRejected() {
    WebSocketStompClient stompClient = stompClient();
    assertThatThrownBy(() -> connect(stompClient, "BAD1", PLAYER_A, "not-a-real-token"))
        .hasRootCauseInstanceOf(Exception.class);
    stompClient.stop();
  }

  @Test
  void globalConnectionWithoutRoomCodeSucceedsAndCanSubscribeToPresence() throws Exception {
    WebSocketStompClient stompClient = stompClient();
    StompSession session = connectGlobal(stompClient, PLAYER_A);
    BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
    subscribeAndSettle(session, "/topic/presence", messages);
    assertThat(session.isConnected()).isTrue();
    session.disconnect();
    stompClient.stop();
  }

  private void assertOwnHandVisibleAndOpponentHandMasked(JsonNode state, String ownPlayerId, String opponentPlayerId) {
    boolean sawOwnVisibleHand = false;
    boolean sawOpponentMaskedHand = false;
    for (JsonNode card : state.path("cards")) {
      if (!"HAND".equalsIgnoreCase(card.path("zone").asText())) continue;
      if (ownPlayerId.equals(card.path("ownerId").asText()) && !"hidden".equals(card.path("cardId").asText())) {
        sawOwnVisibleHand = true;
      }
      if (opponentPlayerId.equals(card.path("ownerId").asText()) && "hidden".equals(card.path("cardId").asText())) {
        sawOpponentMaskedHand = true;
      }
    }
    assertThat(sawOwnVisibleHand).isTrue();
    assertThat(sawOpponentMaskedHand).isTrue();
  }

  private WebSocketStompClient stompClient() {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new MappingJackson2MessageConverter());
    return client;
  }

  private StompSession connect(WebSocketStompClient client, String roomCode, String playerId, String sessionToken) throws Exception {
    String url = UriComponentsBuilder.fromUriString("ws://localhost:" + port + "/ws")
        .queryParam("playerId", playerId)
        .queryParam("roomCode", roomCode)
        .queryParam("sessionToken", sessionToken)
        .build()
        .toUriString();
    StompHeaders headers = new StompHeaders();
    headers.add("playerId", playerId);
    headers.add("roomCode", roomCode);
    headers.add("sessionToken", sessionToken);
    return client.connectAsync(url, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
  }

  private StompSession connectGlobal(WebSocketStompClient client, String playerId) throws Exception {
    String url = UriComponentsBuilder.fromUriString("ws://localhost:" + port + "/ws")
        .queryParam("playerId", playerId)
        .queryParam("role", "LOBBY")
        .build()
        .toUriString();
    StompHeaders headers = new StompHeaders();
    headers.add("playerId", playerId);
    headers.add("role", "LOBBY");
    return client.connectAsync(url, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
  }

  private StompFrameHandler jsonHandler(BlockingQueue<JsonNode> messages) {
    return new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return JsonNode.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messages.add((JsonNode) payload);
      }
    };
  }

  private void subscribeAndSettle(StompSession session, String destination, BlockingQueue<JsonNode> messages) throws Exception {
    session.subscribe(destination, jsonHandler(messages));
    Thread.sleep(250);
  }

  private void add(String id, String name, String type) {
    cards.put(id, new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, "", 1, 1, List.of()));
  }

  private List<String> deck() {
    List<String> deck = new ArrayList<>();
    deck.add("legend");
    deck.add("champion");
    for (int i = 0; i < 12; i++) {
      deck.add("unit-" + i);
      deck.add("unit-" + i);
    }
    return deck;
  }
}
