package com.riftforge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riftforge.effect.EffectHandlerRegistry;
import com.riftforge.effect.EffectSupportStatus;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CardDataService {
  private static final Logger log = LoggerFactory.getLogger(CardDataService.class);
  private static final int PAGE_SIZE = 50;
  private static final long CACHE_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L;

  private final Map<String, CardDefinition> cards = new ConcurrentHashMap<>();
  private final RestClient restClient = RestClient.create();
  private final ObjectMapper mapper;
  private final String apiUrl;
  private final EffectHandlerRegistry effectHandlerRegistry;
  private final Path cacheFile = Path.of(System.getProperty("user.home"), ".riftforge", "cards-cache.json");

  public CardDataService(ObjectMapper mapper, @Value("${riftforge.riftcodex-api}") String apiUrl) {
    this(mapper, apiUrl, null);
  }

  @Autowired
  public CardDataService(ObjectMapper mapper, @Value("${riftforge.riftcodex-api}") String apiUrl, EffectHandlerRegistry effectHandlerRegistry) {
    this.mapper = mapper;
    this.apiUrl = apiUrl;
    this.effectHandlerRegistry = effectHandlerRegistry;
  }

  @PostConstruct
  void load() {
    if (loadFreshCache()) return;

    try {
      fetchFromApi();
      log.info("Loaded {} cards from Riftcodex", cards.size());
      saveCache();
    } catch (Exception ex) {
      log.warn("Could not load cards from Riftcodex at {}. Using placeholder card.", apiUrl, ex);
      installPlaceholder();
    }
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void refreshCache() {
    try {
      fetchFromApi();
      log.info("Loaded {} cards from Riftcodex", cards.size());
      saveCache();
    } catch (Exception ex) {
      log.warn("Could not refresh cards from Riftcodex at {}. Using placeholder card.", apiUrl, ex);
      installPlaceholder();
    }
  }

  private boolean loadFreshCache() {
    try {
      if (!Files.exists(cacheFile)) return false;
      long age = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis();
      if (age >= CACHE_MAX_AGE_MILLIS) return false;

      Map<String, CardDefinition> cachedCards = mapper.readValue(
          cacheFile.toFile(),
          new TypeReference<Map<String, CardDefinition>>() {});
      if (cachedCards.isEmpty()) return false;

      cards.clear();
      cards.putAll(cachedCards);
      log.info("Loaded {} cards from cache (~/.riftforge/cards-cache.json)", cards.size());
      return true;
    } catch (Exception ex) {
      log.warn("Could not load card cache from {}. Fetching from Riftcodex.", cacheFile, ex);
      return false;
    }
  }

  private void fetchFromApi() throws Exception {
    cards.clear();
    int page = 1;
    int totalPages;
    do {
      JsonNode root = fetchPage(page);
      JsonNode items = root.isArray() ? root : firstArray(root, "cards", "data", "results", "items");
      if (items == null || items.isEmpty()) break;

      for (JsonNode item : items) {
        CardDefinition card = normalize(item);
        if (card.id() != null && !card.id().isBlank()) cards.put(card.id(), card);
      }
      totalPages = root.path("pages").asInt(1);
      page++;
    } while (page <= totalPages);

    if (cards.isEmpty()) {
      throw new IllegalStateException("Riftcodex returned no cards");
    }
  }

  private void saveCache() {
    try {
      Files.createDirectories(cacheFile.getParent());
      mapper.writeValue(cacheFile.toFile(), cards);
      log.info("Saved {} cards to cache", cards.size());
    } catch (Exception ex) {
      log.warn("Could not save card cache to {}", cacheFile, ex);
    }
  }

  private JsonNode fetchPage(int page) throws Exception {
    String separator = apiUrl.contains("?") ? "&" : "?";
    String url = apiUrl + separator + "limit=" + PAGE_SIZE + "&page=" + page;
    String json = restClient.get().uri(url).retrieve().body(String.class);
    return mapper.readTree(json);
  }

  private void installPlaceholder() {
    cards.clear();
    cards.put("placeholder", new CardDefinition("placeholder", "Placeholder", "Unit", null, List.of(), 0, 0, null, null, null, "", 1, 1, List.of()));
  }

  public CardDefinition getCard(String id) {
    return cards.getOrDefault(id, new CardDefinition(id, "Unknown Card", "Unknown", null, List.of(), 0, 0, null, null, null, "", 1, 1, List.of()));
  }

  public Map<String, CardDefinition> getAll() {
    return Collections.unmodifiableMap(cards);
  }

  public boolean hasKeyword(String cardId, String keyword) {
    CardDefinition card = getCard(cardId);
    return card.keywords().stream().anyMatch(k -> k.equalsIgnoreCase(keyword));
  }

  public boolean hasKeyword(CardInstance instance, String keyword) {
    return hasKeyword(instance.getCardId(), keyword)
        || instance.getTempKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(keyword));
  }

  public int getKeywordValue(CardInstance instance, String keyword) {
    Pattern pattern = Pattern.compile("^" + Pattern.quote(keyword) + "\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    List<String> keywords = new ArrayList<>(getCard(instance.getCardId()).keywords());
    keywords.addAll(instance.getTempKeywords());
    for (String value : keywords) {
      Matcher matcher = pattern.matcher(value.trim());
      if (matcher.matches()) return Integer.parseInt(matcher.group(1));
    }
    return 0;
  }

  public boolean requiresBattlefieldTarget(String cardId) {
    CardDefinition def = getCard(cardId);
    if (def.rulesText() == null) return false;
    String text = def.rulesText().toLowerCase();
    return isEquip(def)
        || text.contains("target unit")
        || text.contains("target champion")
        || text.contains("target creature")
        || text.contains("target card")
        || text.contains("friendly unit")
        || text.contains("enemy unit")
        || text.contains("give a unit")
        || text.contains("return a unit")
        || text.contains("move a unit")
        || text.contains("counter target")
        || text.contains("destroy target")
        || text.contains("deal") && text.contains("damage to target");
  }

  public boolean requiresFriendlyTarget(String cardId) {
    CardDefinition def = getCard(cardId);
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    return isEquip(def) || text.contains("friendly unit") || text.contains("unit you control");
  }

  public boolean requiresEnemyTarget(String cardId) {
    String text = getCard(cardId).rulesText();
    return text != null && (text.toLowerCase().contains("enemy unit") || text.toLowerCase().contains("opponent unit"));
  }

  public boolean isEquip(CardDefinition def) {
    return "Gear".equalsIgnoreCase(def.type())
        && def.rulesText() != null
        && def.rulesText().toLowerCase().contains("[equip]");
  }

  public boolean isUnsupportedAction(String cardId) {
    CardDefinition def = getCard(cardId);
    if (effectHandlerRegistry != null) {
      EffectSupportStatus status = effectHandlerRegistry.supportStatus(def);
      return !status.implemented();
    }
    String text = def.rulesText();
    if (text == null) return false;
    String normalized = text.toLowerCase();
    if ("Gear".equalsIgnoreCase(def.type())) return !isEquip(def);
    if (!"Spell".equalsIgnoreCase(def.type())) return false;
    boolean requiresMultipleTargets = normalized.contains("another unit")
        || normalized.contains("a friendly unit and an enemy unit");
    boolean supportedEffect = normalized.contains(":rb_might:")
        || normalized.contains("return a unit")
        || normalized.contains("ready it")
        || normalized.contains("draw 1");
    return normalized.contains("counter a spell")
        || normalized.contains("counter an enemy spell")
        || requiresMultipleTargets
        || !supportedEffect;
  }

  private CardDefinition normalize(JsonNode card) {
    JsonNode classification = card.path("classification");
    JsonNode attributes = card.path("attributes");
    JsonNode text = card.path("text");
    JsonNode set = card.path("set");
    JsonNode media = card.path("media");
    String id = firstText(card, "id", "cardId", "uuid", "slug", "collectorNumber", "name");
    return new CardDefinition(
        id,
        firstText(card, "name", "title"),
        cardType(card, classification),
        firstText(card, "champion", "championName", "legend"),
        stringList(card, classification, "domains", "domain", "colors", "colorIdentity"),
        firstInt(card, attributes, "cost", "energyCost", "normalCost", "energy"),
        firstInt(card, attributes, "premiumCost", "premium", "alternateCost"),
        firstText(card, classification, "rarity"),
        firstText(card, set, "setName", "expansion", "label"),
        firstText(card, media, "imageUrl", "image_url"),
        firstText(card, text, "rulesText", "oracleText", "description", "rules", "plain"),
        firstInt(card, attributes, "might", "power", "attack"),
        firstInt(card, attributes, "power", "health", "defense", "toughness"),
        stringList(card, classification, "keywords", "abilities", "keywordAbilities"));
  }

  private JsonNode firstArray(JsonNode root, String... names) {
    for (String name : names) if (root.has(name) && root.get(name).isArray()) return root.get(name);
    return null;
  }

  private String cardType(JsonNode card, JsonNode classification) {
    String supertype = firstText(card, classification, "supertype");
    return "Champion".equalsIgnoreCase(supertype)
        ? "Champion"
        : firstText(card, classification, "type", "cardType", "supertype");
  }

  private String firstText(JsonNode primary, String... names) {
    return firstText(primary, null, names);
  }

  private String firstText(JsonNode primary, JsonNode secondary, String... names) {
    for (String name : names) {
      JsonNode value = primary.path(name);
      if (value.isTextual() && !value.asText().isBlank()) return value.asText();
      if (secondary != null) {
        JsonNode nested = secondary.path(name);
        if (nested.isTextual() && !nested.asText().isBlank()) return nested.asText();
      }
    }
    return null;
  }

  private int firstInt(JsonNode primary, JsonNode secondary, String... names) {
    for (String name : names) {
      JsonNode value = primary.path(name);
      if (value.isNumber()) return value.asInt();
      if (value.isTextual()) return parseInt(value.asText());
      if (secondary != null) {
        JsonNode nested = secondary.path(name);
        if (nested.isNumber()) return nested.asInt();
        if (nested.isTextual()) return parseInt(nested.asText());
      }
    }
    return 0;
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private List<String> stringList(JsonNode primary, JsonNode secondary, String... names) {
    List<String> values = new ArrayList<>();
    for (String name : names) {
      JsonNode value = primary.path(name);
      collect(value, values);
      if (secondary != null) collect(secondary.path(name), values);
      if (!values.isEmpty()) return values;
    }
    return values;
  }

  private void collect(JsonNode node, List<String> values) {
    if (node.isArray()) node.forEach(v -> values.add(v.asText().trim().toUpperCase()));
    if (node.isTextual()) for (String part : node.asText().split("[|,/]")) if (!part.isBlank()) values.add(part.trim().toUpperCase());
  }
}
