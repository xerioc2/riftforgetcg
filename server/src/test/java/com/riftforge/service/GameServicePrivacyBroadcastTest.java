package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.testsupport.GameStackFixture;
import com.riftforge.websocket.GameMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GameServicePrivacyBroadcastTest {
  private static final String ROOM = "PRIV";

  private final GameStackFixture fx = new GameStackFixture();

  @Test
  void sharedRoomBroadcastUsesSpectatorProjection() {
    List<String> deck = fx.registerConstructedDeck();
    fx.gameService.initGame(ROOM, List.of("p1", "p2"),
        Map.of("p1", deck, "p2", deck),
        Map.of("p1", "Player One", "p2", "Player Two"));
    LiveGameState authoritative = fx.gameService.currentState(ROOM);
    authoritative.getCards().add(card("p1-hidden", "p1-private-hidden-card", "p1", ZoneName.HIDDEN));
    authoritative.getCards().add(card("p2-hidden", "p2-private-hidden-card", "p2", ZoneName.HIDDEN));
    clearInvocations(fx.messaging);

    fx.gameService.processMove(ROOM, new MulliganMove("p1", List.of()));

    ArgumentCaptor<GameMessage> captor = ArgumentCaptor.forClass(GameMessage.class);
    verify(fx.messaging).convertAndSend(eq("/topic/game/" + ROOM), captor.capture());
    GameMessage.StateUpdate update = (GameMessage.StateUpdate) captor.getValue();
    assertThat(update.state().getLegalActions()).isEmpty();
    assertThat(update.state().getCards())
        .filteredOn(card -> card.getZone() == ZoneName.HAND || card.getZone() == ZoneName.HIDDEN)
        .extracting(CardInstance::getCardId)
        .containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
  }

  private CardInstance card(String instanceId, String cardId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(3);
    return card;
  }
}
