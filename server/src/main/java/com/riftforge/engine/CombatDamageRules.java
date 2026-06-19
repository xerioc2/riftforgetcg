package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.service.CardDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CombatDamageRules {
  private final CardDataService cardDataService;
  private final CombatStatsService combatStatsService;

  public CombatDamageRules(CardDataService cardDataService) {
    this(cardDataService, new CombatStatsService(cardDataService));
  }

  @Autowired
  public CombatDamageRules(CardDataService cardDataService, CombatStatsService combatStatsService) {
    this.cardDataService = cardDataService;
    this.combatStatsService = combatStatsService;
  }

  public int lethalDamage(CardInstance card) {
    int currentHealth = card.getCurrentHealth();
    if (currentHealth <= 0) {
      CardDefinition def = cardDataService.getCard(card.getCardId());
      currentHealth = def == null ? 0 : def.health();
    }
    return Math.max(1, currentHealth);
  }

  public int lethalDamage(LiveGameState state, CardInstance card) {
    int currentHealth = card.getCurrentHealth();
    if (currentHealth <= 0) {
      currentHealth = combatStatsService.effectiveMaxHealth(state, card);
    }
    return Math.max(1, currentHealth);
  }

  public int combatLethalDamage(LiveGameState state, CardInstance card, CombatStatsService.CombatContext context) {
    int lethalMight = combatStatsService.effectiveMight(state, card, context);
    if (cardDataService.hasKeyword(card, "STUN") || cardDataService.hasKeyword(card, "STUNNED")) {
      lethalMight = combatStatsService.effectiveMight(state, card, CombatStatsService.CombatContext.IDLE);
      lethalMight += switch (context) {
        case ATTACKING -> cardDataService.getKeywordValue(card, "ASSAULT");
        case DEFENDING -> cardDataService.getKeywordValue(card, "SHIELD");
        case IDLE -> 0;
      };
    }
    return Math.max(1, lethalMight);
  }
}
