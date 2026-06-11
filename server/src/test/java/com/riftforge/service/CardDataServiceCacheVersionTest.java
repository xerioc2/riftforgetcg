package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.model.CardDefinition;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CardDataServiceCacheVersionTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @TempDir Path tempDir;

  @Test
  void currentVersionCacheLoads() throws Exception {
    withUserHome(tempDir, () -> {
      writeVersionedCache(Map.of("unit", card("unit", "Vanguard Captain", "Unit", 3, 3)));
      CardDataService service = service();

      assertThat(loadFreshCache(service)).isTrue();

      assertThat(service.getCard("unit").health()).isEqualTo(3);
      assertThat(service.getAll()).containsKey("unit");
    });
  }

  @Test
  void unversionedFreshCacheIsIgnoredSoStalePositiveWrongHealthCannotPersist() throws Exception {
    withUserHome(tempDir, () -> {
      writeUnversionedCache(Map.of("unit", card("unit", "Vanguard Captain", "Unit", 3, 1)));
      CardDataService service = service();

      assertThat(loadFreshCache(service)).isFalse();

      assertThat(service.getAll()).isEmpty();
    });
  }

  @Test
  void staleFallbackCanUseUnversionedCacheWhenApiIsUnavailable() throws Exception {
    withUserHome(tempDir, () -> {
      writeUnversionedCache(Map.of("unit", card("unit", "Vanguard Captain", "Unit", 3, 1)));
      CardDataService service = service();

      assertThat(loadStaleCacheFallback(service)).isTrue();

      assertThat(service.getCard("unit").health()).isEqualTo(1);
      assertThat(service.getAll()).containsKey("unit");
    });
  }

  @Test
  void staleFallbackRepairsNonPositiveCombatHealth() throws Exception {
    withUserHome(tempDir, () -> {
      writeUnversionedCache(Map.of("tideturner", card("tideturner", "Tideturner", "Unit", 2, 0)));
      CardDataService service = service();

      assertThat(loadStaleCacheFallback(service)).isTrue();

      assertThat(service.getCard("tideturner").health()).isEqualTo(2);
    });
  }

  private CardDataService service() {
    return new CardDataService(mapper, "http://127.0.0.1:9/cards-unreachable-in-tests");
  }

  private boolean loadFreshCache(CardDataService service) throws Exception {
    return (boolean) privateMethod("loadFreshCache").invoke(service);
  }

  private boolean loadStaleCacheFallback(CardDataService service) throws Exception {
    return (boolean) privateMethod("loadStaleCacheFallback").invoke(service);
  }

  private Method privateMethod(String name) throws Exception {
    Method method = CardDataService.class.getDeclaredMethod(name);
    method.setAccessible(true);
    return method;
  }

  private void writeVersionedCache(Map<String, CardDefinition> cards) throws Exception {
    Path cacheFile = cacheFile();
    Files.createDirectories(cacheFile.getParent());
    mapper.writeValue(cacheFile.toFile(), Map.of("version", 2, "cards", cards));
  }

  private void writeUnversionedCache(Map<String, CardDefinition> cards) throws Exception {
    Path cacheFile = cacheFile();
    Files.createDirectories(cacheFile.getParent());
    mapper.writeValue(cacheFile.toFile(), cards);
  }

  private Path cacheFile() {
    return tempDir.resolve(".riftforge").resolve("cards-cache.json");
  }

  private CardDefinition card(String id, String name, String type, int power, int health) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, "", power, health, List.of());
  }

  private void withUserHome(Path home, ThrowingRunnable action) throws Exception {
    String original = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
    try {
      action.run();
    } finally {
      System.setProperty("user.home", original);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
