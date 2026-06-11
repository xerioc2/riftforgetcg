package com.riftforge.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class TournamentLegalityTest {
  @Test
  void bannedConstructedCardsAreDetected() {
    List<String> bannedCards = List.of(
        "Called Shot",
        "Draven, Vanquisher",
        "Fight or Flight",
        "Scrapheap");

    for (String name : bannedCards) {
      assertThat(TournamentLegality.isBannedInConstructed(card(name, "Unit")))
          .as(name)
          .isTrue();
    }
  }

  @Test
  void bannedConstructedBattlefieldsAreDetected() {
    List<String> bannedBattlefields = List.of(
        "Dreaming Tree",
        "Obelisk of Power",
        "Reaver's Row");

    for (String name : bannedBattlefields) {
      assertThat(TournamentLegality.isBannedInConstructed(card(name, "Battlefield")))
          .as(name)
          .isTrue();
    }
  }

  @Test
  void curlyApostropheBattlefieldNameIsNormalized() {
    assertThat(TournamentLegality.isBannedInConstructed(card("Reaver\u2019s Row", "Battlefield")))
        .isTrue();
  }

  @Test
  void matchingIsCaseInsensitive() {
    assertThat(TournamentLegality.isBannedInConstructed(card("called shot", "Unit"))).isTrue();
    assertThat(TournamentLegality.isBannedInConstructed(card("CALLED SHOT", "Unit"))).isTrue();
    assertThat(TournamentLegality.isBannedInConstructed(card("CaLlEd ShOt", "Unit"))).isTrue();
  }

  @Test
  void nonBannedCardReturnsFalse() {
    assertThat(TournamentLegality.isBannedInConstructed(card("Definitely Not Banned", "Unit")))
        .isFalse();
  }

  @Test
  void nullCardReturnsFalse() {
    assertThat(TournamentLegality.isBannedInConstructed(null)).isFalse();
  }

  @Test
  void nullOrBlankNameReturnsFalse() {
    assertThat(TournamentLegality.isBannedInConstructed(card(null, "Unit"))).isFalse();
    assertThat(TournamentLegality.isBannedInConstructed(card("   ", "Unit"))).isFalse();
  }

  private CardDefinition card(String name, String type) {
    String id = name == null ? "null-name" : name.toLowerCase().replace(' ', '-');
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, "test", null, null, 1, 1, List.of());
  }
}
