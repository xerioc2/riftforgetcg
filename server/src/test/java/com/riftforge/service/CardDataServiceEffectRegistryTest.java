package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.effect.EffectHandlerRegistry;
import com.riftforge.engine.keyword.TankHandler;
import com.riftforge.model.CardDefinition;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardDataServiceEffectRegistryTest {
  @Test
  void unsupportedTrackedKeywordUsesEffectRegistry() throws Exception {
    CardDataService service = new CardDataService(
        new ObjectMapper(),
        "http://example.invalid",
        new EffectHandlerRegistry(List.of(new TankHandler())));
    install(service, new CardDefinition(
        "death-card",
        "Death Card",
        "Unit",
        null,
        List.of(),
        0,
        0,
        null,
        null,
        null,
        "",
        1,
        1,
        List.of("DEATHKNELL")));

    assertThat(service.isUnsupportedAction("death-card")).isTrue();
  }

  @SuppressWarnings("unchecked")
  private void install(CardDataService service, CardDefinition card) throws Exception {
    Field cards = CardDataService.class.getDeclaredField("cards");
    cards.setAccessible(true);
    ((Map<String, CardDefinition>) cards.get(service)).put(card.id(), card);
  }
}
