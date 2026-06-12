package com.riftforge.effect;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.engine.keyword.AssaultHandler;
import com.riftforge.engine.keyword.AmbushHandler;
import com.riftforge.engine.keyword.DeathknellHandler;
import com.riftforge.engine.keyword.HiddenHandler;
import com.riftforge.engine.keyword.ShieldHandler;
import com.riftforge.engine.keyword.TankHandler;
import com.riftforge.engine.keyword.VisionHandler;
import com.riftforge.model.CardDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectHandlerRegistryTest {
  @Test
  void handlerLookupWorksCaseInsensitively() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler(), new VisionHandler(), new AssaultHandler(), new ShieldHandler()));

    assertThat(registry.keywordHandler("tank")).isPresent();
    assertThat(registry.keywordHandler("VISION")).isPresent();
    assertThat(registry.keywordHandler("Assault 2")).isPresent();
    assertThat(registry.keywordHandler("SHIELD2")).isPresent();
  }

  @Test
  void unsupportedTrackedKeywordIsReportedClearly() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler()));

    EffectSupportStatus status = registry.keywordSupport("Ganking");

    assertThat(status.implemented()).isFalse();
    assertThat(status.reason()).contains("GANKING").contains("registered handler");
  }

  @Test
  void implementedKeywordCardIsSupported() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new TankHandler()));

    EffectSupportStatus status = registry.supportStatus(card("tank-card", "Unit", "", List.of("TANK")));

    assertThat(status.implemented()).isTrue();
  }

  @Test
  void assaultAndShieldKeywordCardsAreSupported() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new AssaultHandler(), new ShieldHandler()));

    assertThat(registry.supportStatus(card("assault-card", "Unit", "", List.of("ASSAULT 2"))).implemented()).isTrue();
    assertThat(registry.supportStatus(card("shield-card", "Unit", "", List.of("SHIELD2"))).implemented()).isTrue();
  }

  @Test
  void deathknellKeywordCardIsRecognizedByHandler() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new DeathknellHandler()));

    EffectSupportStatus status = registry.supportStatus(card("loyal-poro", "Unit", "", List.of("Deathknell")));

    assertThat(status.implemented()).isTrue();
  }

  @Test
  void hiddenKeywordCardIsRecognizedByHandler() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new HiddenHandler()));

    EffectSupportStatus status = registry.supportStatus(card("tideturner", "Unit", "[Hidden] Hide now.", List.of("Hidden")));

    assertThat(status.implemented()).isTrue();
  }

  @Test
  void ambushKeywordCardIsRecognizedByHandler() {
    EffectHandlerRegistry registry = new EffectHandlerRegistry(List.of(new AmbushHandler()));

    EffectSupportStatus status = registry.supportStatus(card("stalking-wolf", "Unit", "[Ambush] Play me to a battlefield.", List.of("Ambush")));

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
