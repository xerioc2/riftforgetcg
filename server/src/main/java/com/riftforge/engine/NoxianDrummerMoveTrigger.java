package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NoxianDrummerMoveTrigger implements TriggerHandler {
  private final CardDataService cardDataService;
  private final TokenFactory tokenFactory;

  public NoxianDrummerMoveTrigger(CardDataService cardDataService, TokenFactory tokenFactory) {
    this.cardDataService = cardDataService;
    this.tokenFactory = tokenFactory;
  }

  @Override
  public boolean supports(TriggerEvent event) {
    return event.type() == TriggerType.CARD_MOVED
        && event.oldZone() != event.newZone()
        && event.newZone() == ZoneName.BATTLEFIELD
        && isNamed(event.sourceCard(), "Noxian Drummer");
  }

  @Override
  public void handle(LiveGameState state, TriggerEvent event) {
    CardInstance card = event.sourceCard();
    CardInstance token = tokenFactory.createRecruit(state, card.getOwnerId(), ZoneName.BATTLEFIELD, card.getX() + 40, card.getY() + 40);
    token.setBattlefieldLocationId(event.locationKey());
    log(state, card.getOwnerId(), "Noxian Drummer created a Recruit token.");
  }

  private boolean isNamed(CardInstance card, String name) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && def.name() != null && def.name().trim().equalsIgnoreCase(name);
  }

  private void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        userId,
        text));
  }
}
