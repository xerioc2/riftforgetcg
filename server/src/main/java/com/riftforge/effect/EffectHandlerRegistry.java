package com.riftforge.effect;

import com.riftforge.engine.keyword.KeywordHandler;
import com.riftforge.engine.keyword.KeywordText;
import com.riftforge.model.CardDefinition;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class EffectHandlerRegistry {
  private static final Set<String> TRACKED_KEYWORDS = Set.of(
      "TANK",
      "SHIELD",
      "ASSAULT",
      "GANKING",
      "DEATHKNELL",
      "VISION",
      "HIDDEN",
      "AMBUSH",
      "STUN",
      "STUNNED",
      "TEMPORARY");

  private final Map<String, KeywordHandler> keywordHandlers = new ConcurrentHashMap<>();

  public EffectHandlerRegistry(List<KeywordHandler> handlers) {
    handlers.forEach(this::register);
  }

  public void register(KeywordHandler handler) {
    keywordHandlers.put(normalize(handler.keyword()), handler);
  }

  public Optional<KeywordHandler> keywordHandler(String keyword) {
    return Optional.ofNullable(keywordHandlers.get(keywordName(keyword)));
  }

  public EffectSupportStatus keywordSupport(String keyword) {
    String normalized = keywordName(keyword);
    KeywordHandler handler = keywordHandlers.get(normalized);
    if (handler != null) return handler.supportStatus();
    if (TRACKED_KEYWORDS.contains(normalized)) {
      return EffectSupportStatus.unsupported("Keyword " + normalized + " does not have a registered handler yet.");
    }
    return EffectSupportStatus.supported();
  }

  public EffectSupportStatus supportStatus(CardDefinition card) {
    if (card == null) return EffectSupportStatus.unsupported("Missing card definition.");
    for (String keyword : card.keywords()) {
      EffectSupportStatus status = keywordSupport(keyword);
      if (!status.implemented()) return status;
    }
    if ("Gear".equalsIgnoreCase(card.type()) && !isSupportedEquip(card)) {
      return EffectSupportStatus.unsupported("That gear ability is not supported yet.");
    }
    if ("Spell".equalsIgnoreCase(card.type()) && !isSupportedSpell(card)) {
      return EffectSupportStatus.unsupported("That spell effect is not supported yet.");
    }
    return EffectSupportStatus.supported();
  }

  public boolean hasKeywordHandler(String keyword) {
    return keywordHandler(keyword).isPresent();
  }

  private boolean isSupportedEquip(CardDefinition card) {
    return card.rulesText() != null && card.rulesText().toLowerCase(Locale.ROOT).contains("[equip]");
  }

  private boolean isSupportedSpell(CardDefinition card) {
    String normalized = card.rulesText() == null ? "" : card.rulesText().toLowerCase(Locale.ROOT);
    boolean supportedFriendlyEnemyReturn = normalized.contains("return")
        && normalized.contains("friendly unit")
        && normalized.contains("enemy unit");
    boolean requiresMultipleTargets = (normalized.contains("another unit") && !isDefiantDanceEffect(card, normalized))
        || (normalized.contains("a friendly unit and an enemy unit") && !supportedFriendlyEnemyReturn);
    boolean supportedEffect = normalized.contains(":rb_might:")
        || normalized.contains("return a unit")
        || normalized.contains("move up to 2 friendly units")
        || supportedFriendlyEnemyReturn
        || normalized.contains("ready it")
        || normalized.contains("draw 1")
        || isDefyCounterEffect(card, normalized)
        || isNotSoFastCounterEffect(card, normalized)
        || isDefiantDanceEffect(card, normalized)
        || isFlashEffect(card, normalized)
        || isStackedDeckEffect(normalized);
    return !(normalized.contains("counter a spell") && !isDefyCounterEffect(card, normalized))
        && !(normalized.contains("counter an enemy spell") && !isNotSoFastCounterEffect(card, normalized))
        && !requiresMultipleTargets
        && supportedEffect;
  }

  private boolean isDefyCounterEffect(CardDefinition card, String normalized) {
    return "Spell".equalsIgnoreCase(card.type())
        && card.name() != null
        && card.name().trim().equalsIgnoreCase("Defy")
        && normalized.contains("[reaction]")
        && normalized.contains("counter a spell");
  }

  private boolean isNotSoFastCounterEffect(CardDefinition card, String normalized) {
    return "Spell".equalsIgnoreCase(card.type())
        && card.name() != null
        && card.name().trim().equalsIgnoreCase("Not So Fast")
        && normalized.contains("[reaction]")
        && normalized.contains("counter an enemy spell or ability")
        && normalized.contains("friendly unit or gear");
  }

  private boolean isDefiantDanceEffect(CardDefinition card, String normalized) {
    return "Spell".equalsIgnoreCase(card.type())
        && card.name() != null
        && card.name().trim().equalsIgnoreCase("Defiant Dance")
        && normalized.contains("[reaction]")
        && normalized.contains("give a unit")
        && normalized.contains("+2")
        && normalized.contains("another unit")
        && normalized.contains("-2");
  }

  private boolean isFlashEffect(CardDefinition card, String normalized) {
    return "Spell".equalsIgnoreCase(card.type())
        && card.name() != null
        && card.name().trim().equalsIgnoreCase("Flash")
        && normalized.contains("[reaction]")
        && normalized.contains("move up to 2 friendly units")
        && normalized.contains("base");
  }

  private boolean isStackedDeckEffect(String normalized) {
    return normalized.contains("look at the top 3")
        && normalized.contains("put 1")
        && normalized.contains("hand")
        && normalized.contains("recycle");
  }

  private String keywordName(String keyword) {
    return KeywordText.name(keyword);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
  }
}
