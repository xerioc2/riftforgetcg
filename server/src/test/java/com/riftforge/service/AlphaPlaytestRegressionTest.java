package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardInstance;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RoomState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.EquipGearMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.testsupport.GameStackFixture;
import java.util.ArrayList;
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
      assertThat(playerNode.path("deckCount").asInt()).isEqualTo(36);
      assertThat(playerNode.path("runePoolRemaining").asInt()).isEqualTo(12);
    }
  }

  @Test
  void equippedUnitCanMoveFightDieAndReturnGearToBaseSafely() {
    List<String> deck = fx.registerConstructedDeck();
    registerCard("equip-gear", "Equip Gear", "Gear", "[Equip] Attached unit gets +1.", 0, 0);
    registerCard("host-unit", "Host Unit", "Unit", "", 1, 1);
    registerCard("enemy-unit", "Enemy Unit", "Unit", "", 2, 2);
    String roomCode = "AR01";
    fx.gameService.initGame(
        roomCode,
        List.of("host", "guest"),
        Map.of("host", deck, "guest", deck),
        Map.of("host", "Host", "guest", "Guest"),
        GameMode.ENFORCED);
    LiveGameState state = fx.gameService.currentState(roomCode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("host");
    state.setActiveShowdown(null);
    state.getCards().add(card("gear", "equip-gear", "host", ZoneName.HAND));
    state.getCards().add(card("host-unit", "host-unit", "host", ZoneName.BASE));
    state.getCards().add(card("enemy-unit", "enemy-unit", "guest", ZoneName.BATTLEFIELD));

    fx.gameService.processMove(roomCode, new PlayCardMove("host", "gear", ZoneName.BASE, 0, 0, null));
    fx.gameService.processMove(roomCode, new EquipGearMove("host", "gear", "host-unit"));
    fx.gameService.processMove(roomCode, new MoveToBattlefieldMove("host", "host-unit"));
    fx.gameService.processMove(roomCode, new ResolveShowdownMove("host"));

    LiveGameState authoritative = fx.gameService.currentState(roomCode);
    assertThat(authoritative.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("host-unit");
      assertThat(card.getZone()).isEqualTo(ZoneName.DISCARD);
    });
    assertThat(authoritative.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("gear");
      assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
      assertThat(card.getAttachedToInstanceId()).isNull();
    });
    assertThat(authoritative.getLog()).anyMatch(entry -> entry.text().equals("Moved Host Unit to the battlefield."));
    assertThat(authoritative.getLog()).anyMatch(entry -> entry.text().equals("Equip Gear returned to Base."));

    LiveGameState ownerView = fx.gameService.currentStateFor(roomCode, "host");
    LiveGameState spectatorView = fx.gameService.currentStateFor(roomCode, null);
    assertThat(ownerView.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("gear");
      assertThat(card.getCardId()).isEqualTo("equip-gear");
      assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
      assertThat(card.getAttachedToInstanceId()).isNull();
    });
    assertThat(spectatorView.getCards()).anySatisfy(card -> {
      assertThat(card.getInstanceId()).isEqualTo("gear");
      assertThat(card.getCardId()).isEqualTo("equip-gear");
      assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
    });
  }

  private void assertDeckPartitions(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> playerId.equals(candidate.getUserId()))
        .findFirst()
        .orElseThrow();
    assertThat(player.getDeckCount()).isEqualTo(36);
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

  private void registerCard(String id, String name, String type, String rulesText, int power, int health) {
    fx.cards.put(id, new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, rulesText, power, health, List.of()));
  }

  private CardInstance card(String instanceId, String cardId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(health(cardId));
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private int health(String cardId) {
    CardDefinition def = fx.cards.get(cardId);
    return def == null ? 1 : def.health();
  }
}
