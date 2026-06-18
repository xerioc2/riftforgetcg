package com.riftforge.rules;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public final class BattlefieldLocationRules {
  private BattlefieldLocationRules() {}

  public static String normalize(String locationId) {
    return locationId == null || locationId.isBlank()
        ? CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID
        : locationId.trim();
  }

  public static String locationOf(CardInstance card) {
    return card == null
        ? CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID
        : normalize(card.getBattlefieldLocationId());
  }

  public static String activeShowdownLocation(LiveGameState state) {
    return state == null || state.getActiveShowdown() == null
        ? CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID
        : normalize(state.getActiveShowdown().locationId());
  }

  public static boolean isAtLocation(CardInstance card, String locationId) {
    return locationOf(card).equals(normalize(locationId));
  }
}
