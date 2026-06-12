package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RoomState;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.LegalAction;
import com.riftforge.testsupport.GameStackFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlphaPlaytestRegressionTest {
  private final GameStackFixture fx = new GameStackFixture();

  @Test
  void constructedAlphaStartProjectsSafeHandsAndDeckPartitions() {
    List<String> deck = fx.registerConstructedDeck();
    RoomState room = fx.roomService.create("host", "Host", false, GameMode.ENFORCED);
    fx.roomService.join(room.getCode(), "guest", "Guest");

    fx.roomService.ready(room.getCode(), "host", deck);
    fx.roomService.ready(room.getCode(), "guest", deck);
    fx.roomService.start(room.getCode(), "host");
    fx.gameService.initGame(
        room.getCode(),
        List.of("host", "guest"),
        Map.of("host", deck, "guest", deck),
        Map.of("host", "Host", "guest", "Guest"),
        room.getGameMode());

    LiveGameState authoritative = fx.gameService.currentState(room.getCode());
    assertThat(authoritative.getGameMode()).isEqualTo(GameMode.ENFORCED);
    assertThat(authoritative.getPlayers()).hasSize(2);
    assertDeckPartitions(authoritative, "host");
    assertDeckPartitions(authoritative, "guest");

    LiveGameState hostView = fx.gameService.currentStateFor(room.getCode(), "host");
    LiveGameState guestView = fx.gameService.currentStateFor(room.getCode(), "guest");
    LiveGameState spectatorView = fx.gameService.currentStateFor(room.getCode(), null);

    assertThat(visibleHandIds(hostView, "host"))
        .hasSize(4)
        .doesNotContain(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(visibleHandIds(hostView, "guest"))
        .hasSize(4)
        .containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(visibleHandIds(guestView, "guest"))
        .hasSize(4)
        .doesNotContain(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(visibleHandIds(spectatorView, "host"))
        .hasSize(4)
        .containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(visibleHandIds(spectatorView, "guest"))
        .hasSize(4)
        .containsOnly(GameStateProjectionService.HIDDEN_CARD_ID);
    assertThat(spectatorView.getLegalActions()).isEmpty();
    assertThat(hostView.getLegalActions()).containsExactlyInAnyOrder(LegalAction.KEEP_HAND, LegalAction.MULLIGAN);

    JsonNode json = new ObjectMapper().valueToTree(hostView);
    for (JsonNode playerNode : json.path("players")) {
      assertThat(playerNode.has("deckPool")).isFalse();
      assertThat(playerNode.has("runeDeckPool")).isFalse();
      assertThat(playerNode.has("selectedBattlefields")).isFalse();
      assertThat(playerNode.path("deckCount").asInt()).isEqualTo(35);
      assertThat(playerNode.path("runePoolRemaining").asInt()).isEqualTo(12);
    }
  }

  private void assertDeckPartitions(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> playerId.equals(candidate.getUserId()))
        .findFirst()
        .orElseThrow();
    assertThat(player.getDeckCount()).isEqualTo(35);
    assertThat(player.getRunePoolRemaining()).isEqualTo(12);
    assertThat(player.getSelectedBattlefields()).hasSize(3);
    assertThat(player.getDeckPool())
        .allSatisfy(cardId -> assertThat(fx.cards.get(cardId).type())
            .isNotIn("Legend", "Champion", "Rune", "Battlefield"));
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo(playerId);
      assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
    });
    assertThat(state.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo(playerId);
      assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
    });
    assertThat(state.getCards())
        .filteredOn(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .hasSize(4)
        .allSatisfy(card -> assertThat(fx.cards.get(card.getCardId()).type())
            .isNotIn("Legend", "Champion", "Rune", "Battlefield"));
  }

  private List<String> visibleHandIds(LiveGameState view, String ownerId) {
    return view.getCards().stream()
        .filter(card -> ownerId.equals(card.getOwnerId()))
        .filter(card -> card.getZone() == ZoneName.HAND)
        .map(CardInstance::getCardId)
        .toList();
  }
}
