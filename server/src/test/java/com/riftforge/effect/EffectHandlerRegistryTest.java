package com.riftforge.effect;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.engine.keyword.TankHandler;
import com.riftforge.engine.keyword.VisionHandler;
import com.riftforge.model.CardDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectHandlerRegistryTest {
  @Test
  void handlerLookupWorksCaseInsensitively() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler(), new VisionHandler()));

    assertThat(registry.keywordHandler("tank")).isPresent();
    assertThat(registry.keywordHandler("VISION")).isPresent();
  }

  @Test
  void unsupportedTrackedKeywordIsReportedClearly() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler()));

    EffectSupportStatus status = registry.keywordSupport("Deathknell");

    assertThat(status.implemented()).isFalse();
    assertThat(status.reason()).contains("DEATHKNELL").contains("registered handler");
  }

  @Test
  void implementedKeywordCardIsSupported() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler()));

    EffectSupportStatus status = registry.supportStatus(card("tank-card", "Unit", "", List.of("TANK")));

    assertThat(status.implemented()).isTrue();
  }

  @Test
  void unsupportedSpellEffectIsReported() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler()));

    EffectSupportStatus status = registry.supportStatus(card("counter", "Spell", "Counter a spell.", List.of()));

    assertThat(status.implemented()).isFalse();
    assertThat(status.reason()).isEqualTo("That spell effect is not supported yet.");
  }

  private CardDefinition card(String id, String type, String rulesText, List<String> keywords) {
    return new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, rulesText, 1, 1, keywords);
  }
}
