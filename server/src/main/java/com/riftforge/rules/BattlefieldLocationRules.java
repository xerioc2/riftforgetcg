package com.riftforge.rules;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import java.util.List;

public final class BattlefieldLocationRules {
  public static final List<String> SUPPORTED_BATTLEFIELD_LOCATION_IDS = List.of("bf-0", "bf-1", "bf-2");
  public static final List<String> DUEL_BATTLEFIELD_LOCATION_IDS = List.of("bf-0", "bf-1");

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

  public static List<String> activeLocationIds(LiveGameState state) {
    int playerCount = state == null || state.getPlayers() == null ? 2 : state.getPlayers().size();
    return playerCount <= 2 ? DUEL_BATTLEFIELD_LOCATION_IDS : SUPPORTED_BATTLEFIELD_LOCATION_IDS;
  }

  public static boolean isActiveLocation(LiveGameState state, String locationId) {
    return activeLocationIds(state).contains(normalize(locationId));
  }
}
