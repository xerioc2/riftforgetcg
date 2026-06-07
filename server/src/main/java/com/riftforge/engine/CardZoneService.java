package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Component;

@Component
public class CardZoneService {
  private final CardDataService cardDataService;

  public CardZoneService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public void moveToGraveyard(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (def != null && "Champion".equalsIgnoreCase(def.type())) {
      card.setZone(ZoneName.CHAMPION);
      card.setCurrentHealth(def.health());
      card.setTemporaryPowerModifier(0);
      card.getTempKeywords().clear();
      card.setHasSummoningSickness(true);
      card.setTapped(false);
      card.setAttachedToInstanceId(null);
      return;
    }
    card.setZone(ZoneName.DISCARD);
  }
}
