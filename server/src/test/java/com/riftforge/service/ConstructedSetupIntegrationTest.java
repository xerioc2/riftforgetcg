package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RoomState;
import com.riftforge.model.ZoneName;
import com.riftforge.testsupport.GameStackFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConstructedSetupIntegrationTest {
  private final GameStackFixture fx = new GameStackFixture();

  @Test
  void fullConstructedRoomFlowStartsWithCorrectDeckPartitions() {
    List<String> deck = fx.registerConstructedDeck();
    RoomState room = fx.roomService.create("host", "Host", false, GameMode.ENFORCED);
    String code = room.getCode();
    fx.roomService.join(code, "guest", "Guest");
    fx.roomService.ready(code, "host", deck);
    fx.roomService.ready(code, "guest", deck);
    fx.roomService.start(code, "host");
    fx.gameService.initGame(code, List.of("host", "guest"),
        Map.of("host", deck, "guest", deck),
        Map.of("host", "Host", "guest", "Guest"),
        room.getGameMode());

    for (String playerId : List.of("host", "guest")) {
      LiveGameState view = fx.gameService.currentStateFor(code, playerId);
      PlayerState me = view.getPlayers().stream()
          .filter(player -> playerId.equals(player.getUserId()))
          .findFirst()
          .orElseThrow();
      assertThat(me.getDeckCount()).isEqualTo(35);
      assertThat(me.getRunePoolRemaining()).isEqualTo(12);

      List<CardInstance> handCards = view.getCards().stream()
          .filter(card -> playerId.equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
          .toList();
      assertThat(handCards).hasSize(4);
      assertThat(handCards).allSatisfy(card ->
          assertThat(fx.cards.get(card.getCardId()).type())
              .isNotIn("Legend", "Champion", "Rune", "Battlefield"));

      assertThat(view.getCards()).anySatisfy(card -> {
        assertThat(card.getOwnerId()).isEqualTo(playerId);
        assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
      });
      assertThat(view.getCards()).anySatisfy(card -> {
        assertThat(card.getOwnerId()).isEqualTo(playerId);
        assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
      });
    }
  }

  @Test
  void bannedCardIsRejectedForConstructedDecks() {
    List<String> deck = new ArrayList<>(fx.registerConstructedDeck());
    fx.addCard("called-shot", "Called Shot", "Spell");
    deck.set(deck.indexOf("unit-0"), "called-shot");
    RoomState room = fx.roomService.create("host", "Host", false, GameMode.ENFORCED);

    assertThatThrownBy(() -> fx.roomService.ready(room.getCode(), "host", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("banned in Constructed");
  }

  @Test
  void bannedBattlefieldIsRejectedForConstructedDecks() {
    List<String> deck = new ArrayList<>(fx.registerConstructedDeck());
    fx.addCard("reavers-row", "Reaver’s Row", "Battlefield");
    deck.set(deck.indexOf("battlefield-0"), "reavers-row");
    RoomState room = fx.roomService.create("host", "Host", false, GameMode.ENFORCED);

    assertThatThrownBy(() -> fx.roomService.ready(room.getCode(), "host", deck))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("banned in Constructed");
  }

  @Test
  void starterDeckValidatesWhenStarterCardsExist() {
    registerIreliaStarterPool();
    RoomState room = fx.roomService.create("host", "Host", true, GameMode.ENFORCED);
    LobbyPlayer bot = room.getPlayers().stream()
        .filter(player -> !"host".equals(player.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(bot.getDeckCardIds()).isNotEmpty();
    assertThatNoException().isThrownBy(() -> fx.roomService.setBotDeck(room.getCode(), List.of()));

    fx.roomService.join(room.getCode(), "guest", "Guest");
    assertThatNoException()
        .isThrownBy(() -> fx.roomService.ready(room.getCode(), "guest", bot.getDeckCardIds()));
  }

  @Test
  void ireliaStarterDeckCanReadyAndPlacesRolesInCorrectZones() {
    List<String> deck = registerIreliaStarterPool();
    RoomState room = fx.roomService.create("host", "Host", false, GameMode.ENFORCED);
    fx.roomService.join(room.getCode(), "guest", "Guest");

    assertThatNoException().isThrownBy(() -> fx.roomService.ready(room.getCode(), "host", deck));
    assertThatNoException().isThrownBy(() -> fx.roomService.ready(room.getCode(), "guest", deck));

    fx.roomService.start(room.getCode(), "host");
    fx.gameService.initGame(room.getCode(), List.of("host", "guest"),
        Map.of("host", deck, "guest", deck),
        Map.of("host", "Host", "guest", "Guest"),
        room.getGameMode());

    LiveGameState view = fx.gameService.currentStateFor(room.getCode(), "host");
    assertThat(view.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("host");
      assertThat(card.getZone()).isEqualTo(ZoneName.LEGEND);
      assertThat(fx.cards.get(card.getCardId()).name()).isEqualTo("Irelia - Blade Dancer");
    });
    assertThat(view.getCards()).anySatisfy(card -> {
      assertThat(card.getOwnerId()).isEqualTo("host");
      assertThat(card.getZone()).isEqualTo(ZoneName.CHAMPION);
      assertThat(fx.cards.get(card.getCardId()).name()).isEqualTo("Irelia - Fervent");
    });
    assertThat(view.getCards())
        .filteredOn(card -> "host".equals(card.getOwnerId()) && card.getZone() == ZoneName.HAND)
        .allSatisfy(card -> assertThat(fx.cards.get(card.getCardId()).type())
            .isNotIn("Legend", "Champion", "Rune", "Battlefield"));
  }

  private List<String> registerIreliaStarterPool() {
    starterCard("Irelia - Blade Dancer", "Legend");
    starterCard("Irelia - Fervent", "Champion");
    starterCard("Calm Rune", "Rune");
    starterCard("Chaos Rune", "Rune");
    starterCard("Targon's Peak", "Battlefield");
    starterCard("Sunken Temple", "Battlefield");
    starterCard("Abandoned Hall", "Battlefield");
    for (String name : List.of(
        "Not So Fast", "Vex - Apathetic", "Scuttle Crab", "Back Off",
        "Defiant Dance", "Edge of Night", "Boots of Swiftness", "Guardian Angel",
        "Stellacorn Herder", "Lonely Poro", "Flash", "Charm", "Defy",
        "En Garde", "Discipline", "Zhonya's Hourglass", "Ride The Wind",
        "The Syren", "Mindsplitter", "Tideturner")) {
      starterCard(name, "Unit");
    }
    return List.of(
        idFor("Irelia - Blade Dancer"),
        idFor("Irelia - Fervent"),
        idFor("Not So Fast"),
        idFor("Vex - Apathetic"), idFor("Vex - Apathetic"),
        idFor("Scuttle Crab"), idFor("Scuttle Crab"), idFor("Scuttle Crab"),
        idFor("Back Off"),
        idFor("Defiant Dance"), idFor("Defiant Dance"), idFor("Defiant Dance"),
        idFor("Edge of Night"),
        idFor("Boots of Swiftness"), idFor("Boots of Swiftness"),
        idFor("Guardian Angel"), idFor("Guardian Angel"),
        idFor("Stellacorn Herder"), idFor("Stellacorn Herder"),
        idFor("Lonely Poro"), idFor("Lonely Poro"), idFor("Lonely Poro"),
        idFor("Flash"), idFor("Flash"),
        idFor("Charm"), idFor("Charm"), idFor("Charm"),
        idFor("Defy"), idFor("Defy"), idFor("Defy"),
        idFor("En Garde"), idFor("En Garde"),
        idFor("Discipline"), idFor("Discipline"), idFor("Discipline"),
        idFor("Zhonya's Hourglass"),
        idFor("Ride The Wind"),
        idFor("The Syren"),
        idFor("Mindsplitter"), idFor("Mindsplitter"),
        idFor("Tideturner"),
        idFor("Calm Rune"), idFor("Calm Rune"), idFor("Calm Rune"), idFor("Calm Rune"), idFor("Calm Rune"), idFor("Calm Rune"),
        idFor("Chaos Rune"), idFor("Chaos Rune"), idFor("Chaos Rune"), idFor("Chaos Rune"), idFor("Chaos Rune"), idFor("Chaos Rune"),
        idFor("Targon's Peak"),
        idFor("Sunken Temple"),
        idFor("Abandoned Hall"));
  }

  private void starterCard(String name, String type) {
    String id = idFor(name);
    fx.addCard(id, name, type);
  }

  private String idFor(String name) {
    return name.toLowerCase().replace(' ', '-').replace("'", "");
  }
}
