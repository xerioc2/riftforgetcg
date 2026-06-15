package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardSupportServiceTest {
  private final CardDataService cardDataService = mock(CardDataService.class);
  private final CardSupportService service = new CardSupportService(cardDataService);

  @Test
  void auditedStarterUnitsAreSupported() {
    assertThat(service.statusFor(card("vanguard-sergeant", "Vanguard Sergeant", "Unit", "")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("daring-poro", "Daring Poro", "Unit", "[Assault] (+1 Might while attacking.)")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("laurent-duelist", "Laurent Duelist", "Unit", "[Assault 2] (+2 Might while attacking.)")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("noxian-drummer", "Noxian Drummer", "Unit", "When I move to a battlefield, play a 1 Might Recruit unit token here.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("loyal-poro", "Loyal Poro", "Unit", "[Deathknell] If I didn't die alone, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("lonely-poro", "Lonely Poro", "Unit", "[Deathknell] If I died alone, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("vanguard-captain", "Vanguard Captain", "Unit", "[Legion] When you play me, play two Recruit unit tokens here.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("stellacorn-herder", "Stellacorn Herder", "Unit", "When I move, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("disarming-rake", "Disarming Rake", "Unit", "When you play me, you may kill a gear.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
  }

  @Test
  void unauditedGenericUnitsRemainPartial() {
    assertThat(service.statusFor(card("some-unit", "Some Unit", "Unit", "")))
        .isEqualTo(CardSupportStatus.PARTIAL);
  }

  @Test
  void unsupportedSpellsRemainBlockedEvenIfNamedDifferently() {
    CardDefinition spell = card("not-so-fast", "Not So Fast", "Spell", "Counter an enemy spell.");
    when(cardDataService.isUnsupportedAction("not-so-fast")).thenReturn(true);

    assertThat(service.statusFor(spell)).isEqualTo(CardSupportStatus.UNSUPPORTED);
  }

  @Test
  void ireliaPartialCardsExposeSpecificReasons() {
    assertThat(service.summarize(card("tideturner", "Tideturner", "Unit", "[Hidden]")).reason())
        .contains("Hidden foundation").contains("location swap");
    assertThat(service.summarize(card("adaptatron", "Adaptatron", "Unit", "When I conquer, you may kill a gear. If you do, buff me.")).reason())
        .contains("conquer trigger").contains("Buff state");
    assertThat(service.summarize(card("gust", "Gust", "Spell", "[Reaction] Return a unit at a battlefield with 3 Might or less.")).reason())
        .contains("Reaction timing").contains("3-or-less-Might");
    assertThat(service.summarize(card("defy", "Defy", "Spell", "[Reaction] Counter a spell.")).reason())
        .contains("no more than 4 energy")
        .contains("no more than 1 premium rune")
        .contains("countering counters remain deferred");
  }

  private CardDefinition card(String id, String name, String type, String rulesText) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, rulesText, 1, 1, List.of());
  }
}
