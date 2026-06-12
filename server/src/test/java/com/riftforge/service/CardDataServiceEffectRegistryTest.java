package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.effect.EffectHandlerRegistry;
import com.riftforge.engine.keyword.AssaultHandler;
import com.riftforge.engine.keyword.ShieldHandler;
import com.riftforge.engine.keyword.TankHandler;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import java.lang.reflect.Field;
import java.util.ArrayList;
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

  @Test
  void valuedAssaultAndShieldKeywordsAreParsedAndSupported() throws Exception {
    CardDataService service = new CardDataService(
        new ObjectMapper(),
        "http://example.invalid",
        new EffectHandlerRegistry(List.of(new AssaultHandler(), new ShieldHandler())));
    install(service, new CardDefinition(
        "combat-card",
        "Combat Card",
        "Unit",
        null,
        List.of(),
        0,
        0,
        null,
        null,
        null,
        "",
        2,
        2,
        List.of("ASSAULT2")));
    CardInstance instance = new CardInstance();
    instance.setCardId("combat-card");
    instance.setTempKeywords(new ArrayList<>(List.of("SHIELD 1")));

    assertThat(service.hasKeyword(instance, "ASSAULT")).isTrue();
    assertThat(service.getKeywordValue(instance, "ASSAULT")).isEqualTo(2);
    assertThat(service.hasKeyword(instance, "SHIELD")).isTrue();
    assertThat(service.getKeywordValue(instance, "SHIELD")).isEqualTo(1);
    assertThat(service.isUnsupportedAction("combat-card")).isFalse();
  }

  @Test
  void plainAssaultAndShieldDefaultToOne() throws Exception {
    CardDataService service = new CardDataService(
        new ObjectMapper(),
        "http://example.invalid",
        new EffectHandlerRegistry(List.of(new AssaultHandler(), new ShieldHandler())));
    install(service, new CardDefinition(
        "plain-combat-card",
        "Plain Combat Card",
        "Unit",
        null,
        List.of(),
        0,
        0,
        null,
        null,
        null,
        "",
        2,
        2,
        List.of("Assault")));
    CardInstance instance = new CardInstance();
    instance.setCardId("plain-combat-card");
    instance.setTempKeywords(new ArrayList<>(List.of("Shield")));

    assertThat(service.getKeywordValue(instance, "ASSAULT")).isEqualTo(1);
    assertThat(service.getKeywordValue(instance, "SHIELD")).isEqualTo(1);
  }

  @Test
  void keywordValuesAreCaseInsensitiveWhitespaceTolerantAndMissingKeywordsReturnZero() throws Exception {
    CardDataService service = new CardDataService(
        new ObjectMapper(),
        "http://example.invalid",
        new EffectHandlerRegistry(List.of(new AssaultHandler(), new ShieldHandler())));
    install(service, new CardDefinition(
        "mixed-combat-card",
        "Mixed Combat Card",
        "Unit",
        null,
        List.of(),
        0,
        0,
        null,
        null,
        null,
        "",
        2,
        2,
        List.of("  assault   2  ")));
    CardInstance instance = new CardInstance();
    instance.setCardId("mixed-combat-card");
    instance.setTempKeywords(new ArrayList<>(List.of("sHiElD2")));

    assertThat(service.getKeywordValue(instance, "ASSAULT")).isEqualTo(2);
    assertThat(service.getKeywordValue(instance, "shield")).isEqualTo(2);
    assertThat(service.getKeywordValue(instance, "TANK")).isZero();
  }

  @SuppressWarnings("unchecked")
  private void install(CardDataService service, CardDefinition card) throws Exception {
    Field cards = CardDataService.class.getDeclaredField("cards");
    cards.setAccessible(true);
    ((Map<String, CardDefinition>) cards.get(service)).put(card.id(), card);
  }
}
