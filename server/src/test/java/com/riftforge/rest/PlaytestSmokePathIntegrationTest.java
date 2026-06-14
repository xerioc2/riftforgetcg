package com.riftforge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.SelectBattlefieldMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.testsupport.GameStackFixture;
import com.riftforge.websocket.GameController;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PlaytestSmokePathIntegrationTest {
  private final GameStackFixture fx = new GameStackFixture();
  private RoomController roomController;
  private GameRestController gameRestController;
  private GameController gameController;

  @BeforeEach
  void setUp() {
    roomController = new RoomController(fx.roomService, fx.gameService, fx.roomTokenService);
    gameRestController = new GameRestController(fx.gameService, fx.cardDataService, fx.roomService, new MatchHistoryService(), fx.roomTokenService);
    gameController = new GameController(fx.gameService);
  }

  @Test
  void createJoinReadyStartMulliganAndReachMainWithoutErrors() {
    List<String> deck = fx.registerConstructedDeck();

    RoomController.TokenizedRoom created =
        roomController.create(new RoomController.CreateRequest("host", "Host", false, GameMode.ENFORCED));
    String code = created.room().getCode();
    String hostToken = created.sessionToken();
    assertThat(hostToken).isNotBlank();

    RoomController.TokenizedRoom joined =
        roomController.join(code, new RoomController.JoinRequest("guest", "Guest"));
    String guestToken = joined.sessionToken();
    assertThat(joined.room().getPlayers()).hasSize(2);

    assertThat(roomController.ready(code, new RoomController.ReadyRequest("host", hostToken, deck, false)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(roomController.ready(code, new RoomController.ReadyRequest("guest", guestToken, deck, false)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(roomController.start(code, new RoomController.StartRequest("host", hostToken)).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    LiveGameState hostView = stateFor(code, "host", hostToken);
    LiveGameState guestView = stateFor(code, "guest", guestToken);
    assertThat(hostView.getCurrentPhase()).isEqualTo(Phase.SELECT_BATTLEFIELD);
    assertThat(hostView.getLegalActions()).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(guestView.getLegalActions()).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    assertThat(hostView.getPlayers().stream().filter(player -> "host".equals(player.getUserId())).findFirst().orElseThrow().getBattlefieldChoices())
        .containsExactly("battlefield-0", "battlefield-1", "battlefield-2");
    assertThat(hostView.getPlayers().stream().filter(player -> "guest".equals(player.getUserId())).findFirst().orElseThrow().getBattlefieldChoices())
        .isEmpty();
    gameController.handleMove(code, new MulliganMove("host", List.of()), () -> "host");
    assertThat(fx.gameService.currentState(code).getCurrentPhase()).isEqualTo(Phase.SELECT_BATTLEFIELD);
    assertThat(fx.gameService.currentState(code).getMulligansDone()).isEmpty();

    gameController.handleMove(code, new SelectBattlefieldMove("host", "battlefield-0"), () -> "host");
    assertThat(stateFor(code, "host", hostToken).getLegalActions()).isEmpty();
    assertThat(stateFor(code, "guest", guestToken).getLegalActions()).containsExactly(LegalAction.SELECT_BATTLEFIELD);
    gameController.handleMove(code, new SelectBattlefieldMove("guest", "battlefield-1"), () -> "guest");

    hostView = stateFor(code, "host", hostToken);
    guestView = stateFor(code, "guest", guestToken);
    assertThat(hostView.getCurrentPhase()).isEqualTo(Phase.MULLIGAN);
    assertThat(hostView.getLegalActions()).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
    assertThat(guestView.getLegalActions()).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);
    assertThat(ownHandSize(hostView, "host")).isEqualTo(4);
    assertThat(ownHandSize(guestView, "guest")).isEqualTo(4);
    assertThat(maskedHandSize(hostView, "guest")).isEqualTo(4);

    assertThatThrownBy(() -> gameController.handleMove(code, new MulliganMove("guest", List.of()), () -> "host"))
        .isInstanceOf(SecurityException.class);

    gameController.handleMove(code, new MulliganMove("host", List.of()), () -> "host");
    assertThat(stateFor(code, "host", hostToken).getLegalActions()).isEmpty();
    assertThat(stateFor(code, "guest", guestToken).getLegalActions())
        .containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);

    gameController.handleMove(code, new MulliganMove("guest", List.of()), () -> "guest");
    LiveGameState state = fx.gameService.currentState(code);
    assertThat(state.getCurrentPhase()).isEqualTo(Phase.AWAKEN);

    String active = state.getActivePlayerId();
    for (Phase expected : List.of(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN)) {
      gameController.handleMove(code, new PassPhaseMove(active), () -> active);
      assertThat(fx.gameService.currentState(code).getCurrentPhase()).isEqualTo(expected);
    }

    assertThat(fx.gameService.currentState(code).getWinnerId()).isNull();
    String activeToken = "host".equals(active) ? hostToken : guestToken;
    assertThat(stateFor(code, active, activeToken).getLegalActions())
        .contains(LegalAction.PLAY_CARD, LegalAction.PASS_PHASE, LegalAction.MOVE_TO_BATTLEFIELD);
  }

  private LiveGameState stateFor(String code, String playerId, String token) {
    ResponseEntity<?> response = gameRestController.state(code, playerId, token);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (LiveGameState) Objects.requireNonNull(response.getBody());
  }

  private long ownHandSize(LiveGameState view, String ownerId) {
    return view.getCards().stream()
        .filter(card -> ownerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .filter(card -> !GameStateProjectionService.HIDDEN_CARD_ID.equals(card.getCardId()))
        .count();
  }

  private long maskedHandSize(LiveGameState view, String ownerId) {
    return view.getCards().stream()
        .filter(card -> ownerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .filter(card -> GameStateProjectionService.HIDDEN_CARD_ID.equals(card.getCardId()))
        .count();
  }
}
