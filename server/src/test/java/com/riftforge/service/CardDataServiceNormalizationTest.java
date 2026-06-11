package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardDefinition;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardDataServiceNormalizationTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private CardDataService service;
  private Method normalize;

  @BeforeEach
  void setUp() throws Exception {
    service = new CardDataService(mapper, "http://example.invalid");
    normalize = CardDataService.class.getDeclaredMethod("normalize", JsonNode.class);
    normalize.setAccessible(true);
  }

  @Test
  void unitWithRealRiftcodexMightAndNoPowerUsesMightAsEngineHealth() throws Exception {
    CardDefinition card = normalize("""
        {
          "id": "69bc5bd1d308c64675ca8791",
          "name": "Tideturner",
          "classification": { "type": "Unit", "domain": ["Chaos"] },
          "attributes": { "energy": 2, "might": 2, "power": null },
          "text": { "plain": "[Hidden] When you play me, you may choose a unit you control." }
        }
        """);

    assertThat(card.name()).isEqualTo("Tideturner");
    assertThat(card.type()).isEqualTo("Unit");
    assertThat(card.cost()).isEqualTo(2);
    assertThat(card.power()).isEqualTo(2);
    assertThat(card.health()).isEqualTo(2);
  }

  @Test
  void championWithRealRiftcodexSupertypeUsesMightAsEngineHealth() throws Exception {
    CardDefinition card = normalize("""
        {
          "id": "69bc5bdbd308c64675ca8843",
          "name": "Irelia - Fervent",
          "classification": { "type": "Unit", "supertype": "Champion", "domain": ["Calm"] },
          "attributes": { "energy": 5, "might": 4, "power": null },
          "text": { "plain": "[Deflect] When you choose or ready me, give me +1 :rb_might: this turn." }
        }
        """);

    assertThat(card.type()).isEqualTo("Champion");
    assertThat(card.cost()).isEqualTo(5);
    assertThat(card.power()).isEqualTo(4);
    assertThat(card.health()).isEqualTo(4);
  }

  @Test
  void zeroMightUnitGetsMinimumPositiveEngineHealth() throws Exception {
    CardDefinition card = normalize("""
        {
          "id": "69c43cd2c2f7428c5d24b4e2",
          "name": "Scuttle Crab",
          "classification": { "type": "Unit", "domain": ["Calm"] },
          "attributes": { "energy": 2, "might": 0, "power": null },
          "text": { "plain": "(Units with 0 :rb_might: can conquer and hold.)" }
        }
        """);

    assertThat(card.power()).isEqualTo(0);
    assertThat(card.health()).isEqualTo(1);
  }

  @Test
  void explicitDefensiveHealthWinsOverRiftcodexPower() throws Exception {
    CardDefinition card = normalize("""
        {
          "id": "fixture-unit",
          "name": "Fixture Unit",
          "classification": { "type": "Unit", "domain": ["Calm"] },
          "attributes": { "energy": 1, "might": 3, "power": 9, "health": 4 },
          "text": { "plain": "" }
        }
        """);

    assertThat(card.power()).isEqualTo(3);
    assertThat(card.health()).isEqualTo(4);
  }

  @Test
  void healthDoesNotAccidentallyUseRiftcodexPowerField() throws Exception {
    CardDefinition card = normalize("""
        {
          "id": "69bc5bd3d308c64675ca87a7",
          "name": "Vanguard Captain",
          "classification": { "type": "Unit", "domain": ["Order"] },
          "attributes": { "energy": 3, "might": 3, "power": 1 },
          "text": { "plain": "[Legion] When you play me, play two Recruit unit tokens here." }
        }
        """);

    assertThat(card.power()).isEqualTo(3);
    assertThat(card.health()).isEqualTo(3);
  }

  private CardDefinition normalize(String json) throws Exception {
    return (CardDefinition) normalize.invoke(service, mapper.readTree(json));
  }
}
