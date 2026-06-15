package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Component;

@Component
public class CombatDamageRules {
  private final CardDataService cardDataService;

  public CombatDamageRules(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public int lethalDamage(CardInstance card) {
    int currentHealth = card.getCurrentHealth();
    if (currentHealth <= 0) {
      CardDefinition def = cardDataService.getCard(card.getCardId());
      currentHealth = def == null ? 0 : def.health();
    }
    return Math.max(1, currentHealth);
  }
}
