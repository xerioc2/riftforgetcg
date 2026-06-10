package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStateProjectionServiceTest {
  private final GameStateProjectionService projectionService = new GameStateProjectionService();

  @Test
  void playerSeesOwnHandCardIds() {
    LiveGameState state = state(handCard("own-hand", "p1", "irelia"));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getCards()).extracting(CardInstance::getCardId).containsExactly("irelia");
    assertThat(state.getCards()).extracting(CardInstance::getCardId).containsExactly("irelia");
  }

  @Test
  void playerDoesNotSeeOpponentHandCardIds() {
    LiveGameState state = state(handCard("opp-hand", "p2", "secret-card"));

    LiveGameState view = projectionService.toPublicView(state, "p1");
    CardInstance hidden = view.getCards().getFirst();

    assertThat(hidden.getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(hidden.getCurrentHealth()).isZero();
    assertThat(state.getCards().getFirst().getCardId()).isEqualTo("secret-card");
    assertThat(state.getCards().getFirst().getCurrentHealth()).isEqualTo(3);
  }

  @Test
  void spectatorViewMasksAllHandCardIds() {
    LiveGameState state = state(
        handCard("p1-hand", "p1", "p1-card"),
        handCard("p2-hand", "p2", "p2-card"));

    LiveGameState view = projectionService.toPublicView(state, null);

    assertThat(view.getCards())
        .extracting(CardInstance::getCardId)
        .containsExactly(GameStateProjectionService.HIDDEN_CARD_ID, GameStateProjectionService.HIDDEN_CARD_ID);
  }

  @Test
  void cardsOutsideHandAreNotMasked() {
    LiveGameState state = state(card("battlefield-card", "p2", "visible-card", ZoneName.BATTLEFIELD));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getCards()).extracting(CardInstance::getCardId).containsExactly("visible-card");
  }

  @Test
  void playerSeesOwnVisionPeekLogEntry() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("vision", "p1", "VISION_PEEK|card-1|Card One"),
        log("normal", "p2", "Showdown started.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("VISION_PEEK|card-1|Card One", "Showdown started.");
  }

  @Test
  void playerDoesNotSeeOpponentVisionPeekLogEntry() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("vision", "p2", "VISION_PEEK|card-1|Card One"),
        log("normal", "p2", "Showdown started.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("Showdown started.");
  }

  @Test
  void nonVisionLogEntriesAreVisibleToAllPlayers() {
    LiveGameState state = state();
    state.setLog(List.of(
        log("normal", "p2", "Played a card."),
        log("vision-resolved", "p2", "VISION_RESOLVED|Kept Card One on top of the deck.")));

    LiveGameState view = projectionService.toPublicView(state, "p1");

    assertThat(view.getLog()).extracting(LiveGameState.LogEntry::text)
        .containsExactly("Played a card.");
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(List.of(cards));
    return state;
  }

  private CardInstance handCard(String instanceId, String ownerId, String cardId) {
    return card(instanceId, ownerId, cardId, ZoneName.HAND);
  }

  private CardInstance card(String instanceId, String ownerId, String cardId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setOwnerId(ownerId);
    card.setCardId(cardId);
    card.setZone(zone);
    card.setCurrentHealth(3);
    return card;
  }

  private LiveGameState.LogEntry log(String id, String userId, String text) {
    return new LiveGameState.LogEntry(id, Instant.now().toString(), userId, text);
  }
}
