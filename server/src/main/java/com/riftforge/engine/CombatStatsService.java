package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.service.CardDataService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CombatStatsService {
  public enum CombatContext {
    IDLE,
    ATTACKING,
    DEFENDING
  }

  public record AppliedModifier(
      String sourceInstanceId,
      String sourceCardId,
      String sourceName,
      int mightBonus,
      int maxHealthBonus) {}

  public record EffectiveStats(
      int printedMight,
      int printedMaxHealth,
      int effectiveMight,
      int effectiveMaxHealth,
      int currentHealth,
      int markedDamage,
      List<AppliedModifier> modifiers) {}

  private final CardDataService cardDataService;
  private final EquipmentStatModifierRegistry equipmentStatModifierRegistry;

  public CombatStatsService(CardDataService cardDataService) {
    this(cardDataService, new EquipmentStatModifierRegistry());
  }

  @Autowired
  public CombatStatsService(CardDataService cardDataService, EquipmentStatModifierRegistry equipmentStatModifierRegistry) {
    this.cardDataService = cardDataService;
    this.equipmentStatModifierRegistry = equipmentStatModifierRegistry;
  }

  public int effectiveMight(CardInstance card, CombatContext context) {
    return effectiveMight(null, card, context);
  }

  public int effectiveMight(LiveGameState state, CardInstance card, CombatContext context) {
    return effectiveStats(state, card, context).effectiveMight();
  }

  public int effectiveMaxHealth(LiveGameState state, CardInstance card) {
    return effectiveStats(state, card, CombatContext.IDLE).effectiveMaxHealth();
  }

  public EffectiveStats effectiveStats(LiveGameState state, CardInstance card, CombatContext context) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    int printedMight = def == null ? 0 : def.power();
    int printedMaxHealth = def == null ? 0 : def.health();
    List<AppliedModifier> equipmentModifiers = attachedEquipmentModifiers(state, card);
    int equipmentMight = equipmentModifiers.stream().mapToInt(AppliedModifier::mightBonus).sum();
    int equipmentMaxHealth = equipmentModifiers.stream().mapToInt(AppliedModifier::maxHealthBonus).sum();
    int situational = switch (context) {
      case ATTACKING -> cardDataService.getKeywordValue(card, "ASSAULT");
      case DEFENDING -> cardDataService.getKeywordValue(card, "SHIELD");
      case IDLE -> 0;
    };
    int effectiveMight = isCombatDamagePrevented(card, context)
        ? 0
        : Math.max(0, printedMight + card.getMightBonus() + card.getTemporaryPowerModifier() + situational + equipmentMight);
    int effectiveMaxHealth = Math.max(0, printedMaxHealth + equipmentMaxHealth);
    int currentHealth = effectiveMaxHealth > 0
        ? Math.max(0, Math.min(effectiveMaxHealth, card.getCurrentHealth() > 0 ? card.getCurrentHealth() : effectiveMaxHealth))
        : 0;
    int markedDamage = Math.max(0, effectiveMaxHealth - currentHealth);
    return new EffectiveStats(
        printedMight,
        printedMaxHealth,
        effectiveMight,
        effectiveMaxHealth,
        currentHealth,
        markedDamage,
        equipmentModifiers);
  }

  public boolean hasCombatStats(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null
        && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  public boolean isMighty(CardInstance card) {
    return isMighty(card, CombatContext.IDLE);
  }

  public boolean isMighty(CardInstance card, CombatContext context) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (!"Unit".equalsIgnoreCase(def.type()) && !"Champion".equalsIgnoreCase(def.type())) return false;
    return effectiveMight(card, context) >= 5;
  }

  public boolean isMighty(LiveGameState state, CardInstance card, CombatContext context) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (def == null || (!"Unit".equalsIgnoreCase(def.type()) && !"Champion".equalsIgnoreCase(def.type()))) return false;
    return effectiveMight(state, card, context) >= 5;
  }

  private List<AppliedModifier> attachedEquipmentModifiers(LiveGameState state, CardInstance host) {
    if (state == null || host == null || host.getInstanceId() == null) return List.of();
    return state.getCards().stream()
        .filter(gear -> host.getInstanceId().equals(gear.getAttachedToInstanceId()))
        .filter(gear -> !gear.isFaceDown())
        .map(gear -> {
          CardDefinition def = cardDataService.getCard(gear.getCardId());
          if (def == null || !"Gear".equalsIgnoreCase(def.type())) return null;
          EquipmentStatModifierRegistry.StatModifier modifier = equipmentStatModifierRegistry.modifierFor(def);
          if (modifier.isEmpty()) return null;
          return new AppliedModifier(gear.getInstanceId(), gear.getCardId(), def.name(), modifier.mightBonus(), modifier.maxHealthBonus());
        })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private boolean isCombatDamagePrevented(CardInstance card, CombatContext context) {
    if (context == CombatContext.IDLE) return false;
    return cardDataService.hasKeyword(card, "STUN") || cardDataService.hasKeyword(card, "STUNNED");
  }
}
