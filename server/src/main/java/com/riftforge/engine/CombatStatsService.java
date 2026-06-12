package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Service;

@Service
public class CombatStatsService {
  public enum CombatContext {
    IDLE,
    ATTACKING,
    DEFENDING
  }

  private final CardDataService cardDataService;

  public CombatStatsService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public int effectiveMight(CardInstance card, CombatContext context) {
    if (isCombatDamagePrevented(card, context)) return 0;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    int situational = switch (context) {
      case ATTACKING -> cardDataService.getKeywordValue(card, "ASSAULT");
      case DEFENDING -> cardDataService.getKeywordValue(card, "SHIELD");
      case IDLE -> 0;
    };
    return Math.max(0, def.power() + card.getMightBonus() + card.getTemporaryPowerModifier() + situational);
  }

  public boolean isMighty(CardInstance card) {
    return isMighty(card, CombatContext.IDLE);
  }

  public boolean isMighty(CardInstance card, CombatContext context) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (!"Unit".equalsIgnoreCase(def.type()) && !"Champion".equalsIgnoreCase(def.type())) return false;
    return effectiveMight(card, context) >= 5;
  }

  private boolean isCombatDamagePrevented(CardInstance card, CombatContext context) {
    if (context == CombatContext.IDLE) return false;
    return cardDataService.hasKeyword(card, "STUN") || cardDataService.hasKeyword(card, "STUNNED");
  }
}
