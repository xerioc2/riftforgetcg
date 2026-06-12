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
  void auditedStarterBasicAndAssaultUnitsAreSupported() {
    assertThat(service.statusFor(card("vanguard-sergeant", "Vanguard Sergeant", "Unit", "")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("daring-poro", "Daring Poro", "Unit", "[Assault] (+1 Might while attacking.)")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("laurent-duelist", "Laurent Duelist", "Unit", "[Assault 2] (+2 Might while attacking.)")))
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

  private CardDefinition card(String id, String name, String type, String rulesText) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, rulesText, 1, 1, List.of());
  }
}
