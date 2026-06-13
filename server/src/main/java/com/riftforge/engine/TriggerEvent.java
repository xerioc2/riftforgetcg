package com.riftforge.engine;

import com.riftforge.model.CardInstance;
import com.riftforge.model.ZoneName;

public record TriggerEvent(
    TriggerType type,
    CardInstance sourceCard,
    String controllerId,
    ZoneName oldZone,
    ZoneName newZone,
    String locationKey,
    String cause) {

  public static TriggerEvent cardMoved(
      CardInstance card,
      ZoneName oldZone,
      ZoneName newZone,
      String locationKey,
      String cause) {
    return new TriggerEvent(
        TriggerType.CARD_MOVED,
        card,
        card.getOwnerId(),
        oldZone,
        newZone,
        locationKey,
        cause);
  }
}
