package com.riftforge.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.LegalAction;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.testsupport.GameStackFixture;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthProjectionIntegrationTest {
  private final GameStackFixture fx = new GameStackFixture();
  private GameRestController controller;
  private String hostToken;
  private String guestToken;

  @BeforeEach
  void startGame() {
    controller = new GameRestController(fx.gameService, fx.cardDataService, fx.roomService, new MatchHistoryService(), fx.roomTokenService);
    List<String> deck = fx.registerConstructedDeck();
    fx.gameService.initGame("ROOM", List.of("host", "guest"),
        Map.of("host", deck, "guest", deck),
        Map.of("host", "Host", "guest", "Guest"));
    hostToken = fx.roomTokenService.issue("ROOM", "host", "PLAYER");
    guestToken = fx.roomTokenService.issue("ROOM", "guest", "PLAYER");
  }

  @Test
  void validTokenFetchesViewerSpecificProjection() {
    ResponseEntity<?> response = controller.state("ROOM", "host", hostToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    LiveGameState view = stateBody(response);
    assertThat(view.getLegalActions()).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
    assertThat(handCardIds(view, "host"))
        .hasSize(4)
        .doesNotContain(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(handCardIds(view, "guest"))
        .hasSize(4)
        .containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
  }

  @Test
  void invalidTokenIsRejected() {
    ResponseEntity<?> response = controller.state("ROOM", "host", "not-a-real-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotInstanceOf(LiveGameState.class);
  }

  @Test
  void playerTokenCannotFetchAnotherPlayersProjection() {
    ResponseEntity<?> response = controller.state("ROOM", "guest", hostToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotInstanceOf(LiveGameState.class);

    ResponseEntity<?> reversed = controller.state("ROOM", "host", guestToken);
    assertThat(reversed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void tokenlessFetchGetsSpectatorProjectionWithAllHandsMasked() {
    ResponseEntity<?> response = controller.state("ROOM", null, null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    LiveGameState view = stateBody(response);
    assertThat(view.getLegalActions()).isEmpty();
    assertThat(handCardIds(view, "host")).containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(handCardIds(view, "guest")).containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
  }

  @Test
  void spectatorProjectionDoesNotSerializeHiddenInformation() {
    LiveGameState view = stateBody(controller.state("ROOM", null, null));

    JsonNode json = new ObjectMapper().valueToTree(view);
    assertThat(json.path("players").size()).isEqualTo(2);
    for (JsonNode playerNode : json.path("players")) {
      assertThat(playerNode.has("deckPool")).isFalse();
      assertThat(playerNode.has("runeDeckPool")).isFalse();
      assertThat(playerNode.has("selectedBattlefields")).isFalse();
      assertThat(playerNode.path("deckCount").isInt()).isTrue();
    }
    for (JsonNode cardNode : json.path("cards")) {
      if ("hand".equalsIgnoreCase(cardNode.path("zone").asText())) {
        assertThat(cardNode.path("cardId").asText()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
      }
    }
  }

  private LiveGameState stateBody(ResponseEntity<?> response) {
    return (LiveGameState) Objects.requireNonNull(response.getBody());
  }

  private List<String> handCardIds(LiveGameState view, String ownerId) {
    return view.getCards().stream()
        .filter(card -> ownerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .map(CardInstance::getCardId)
        .toList();
  }
}
