package com.riftforge.engine;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CardZoneService {
  public CardZoneService(CardDataService cardDataService) {}

  public void moveToGraveyard(CardInstance card) {
    card.setAttachedToInstanceId(null);
    card.setBattlefieldLocationId(null);
    card.setTemporaryPowerModifier(0);
    card.getTempKeywords().clear();
    card.setHasSummoningSickness(false);
    card.setTapped(false);
    card.setZone(ZoneName.DISCARD);
  }

  public List<CardInstance> returnAttachmentsToBase(LiveGameState state, CardInstance host) {
    List<CardInstance> attachments = state.getCards().stream()
        .filter(attachment -> host.getInstanceId().equals(attachment.getAttachedToInstanceId()))
        .toList();
    attachments.forEach(attachment -> {
      attachment.setAttachedToInstanceId(null);
      attachment.setZone(ZoneName.BASE);
      attachment.setX(0);
      attachment.setY(0);
      attachment.setTapped(false);
      attachment.setHasSummoningSickness(false);
    });
    return attachments;
  }

  public void moveAttachmentsToGraveyard(LiveGameState state, CardInstance host) {
    returnAttachmentsToBase(state, host);
  }
}
