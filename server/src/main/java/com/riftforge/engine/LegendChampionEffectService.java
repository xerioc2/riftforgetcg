package com.riftforge.engine;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Service;

@Service
public class LegendChampionEffectService {
  private final CardDataService cardDataService;

  public LegendChampionEffectService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public boolean applyIreliaFerventReadyTrigger(LiveGameState state, CardInstance target, String readyingPlayerId) {
    if (!isIreliaFerventPublicPlaySource(target)) return false;
    if (!target.getOwnerId().equals(readyingPlayerId)) return false;
    target.setTemporaryPowerModifier(target.getTemporaryPowerModifier() + 1);
    return true;
  }

  public boolean isIreliaFerventPublicPlaySource(CardInstance card) {
    if (card == null || card.isFaceDown()) return false;
    if (card.getZone() != ZoneName.BASE && card.getZone() != ZoneName.BATTLEFIELD) return false;
    var def = cardDataService.getCard(card.getCardId());
    return cardDataService.isIreliaFerventChampion(def);
  }
}
