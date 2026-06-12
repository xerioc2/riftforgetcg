package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardDataServiceTimingTest {
  private final CardDataService service = new CardDataService(new ObjectMapper(), "http://example.invalid");

  @Test
  void actionCardDetectionIsCaseInsensitiveAndBracketed() {
    CardDefinition card = card("[Action] Move a friendly unit and ready it.");

    assertThat(service.isActionCard(card)).isTrue();
    assertThat(service.isReactionCard(card)).isFalse();
  }

  @Test
  void reactionCardDoesNotCountAsAction() {
    CardDefinition card = card("[Reaction] Give a unit +2 :rb_might: this turn.");

    assertThat(service.isReactionCard(card)).isTrue();
    assertThat(service.isActionCard(card)).isFalse();
  }

  @Test
  void ambushCardDetectionAcceptsBracketedTimingAndKeyword() {
    assertThat(service.isAmbushCard(card("[Ambush] You may play me to a battlefield.", List.of()))).isTrue();
    assertThat(service.isAmbushCard(card("Reminder text only.", List.of("Ambush")))).isTrue();
  }

  @Test
  void unsupportedAdditionalCostDetectionIsExplicit() {
    assertThat(service.hasUnsupportedAdditionalCost(card("As an additional cost to play me, kill a Poro you control."))).isTrue();
    assertThat(service.hasUnsupportedAdditionalCost(card("[Ambush] You may play me to a battlefield."))).isFalse();
  }

  @Test
  void unbracketedWordsDoNotCountAsTimingPermissions() {
    CardDefinition card = card("This card mentions action and reaction in reminder text only.");

    assertThat(service.isActionCard(card)).isFalse();
    assertThat(service.isReactionCard(card)).isFalse();
  }

  private CardDefinition card(String rulesText) {
    return card(rulesText, List.of());
  }

  private CardDefinition card(String rulesText, List<String> keywords) {
    return new CardDefinition("card", "Card", "Spell", null, List.of(), 0, 0, null, null, null, rulesText, 0, 0, keywords);
  }
}
