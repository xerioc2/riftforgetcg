package com.riftforge.engine;

import com.riftforge.model.ZoneName;

public record DeathEvent(
    String instanceId,
    String cardId,
    String cardName,
    String ownerId,
    ZoneName previousZone,
    boolean hadOtherFriendlyUnitAtLocation,
    DeathCause cause) {
  public enum DeathCause {
    COMBAT,
    EFFECT,
    CLEANUP,
    UNKNOWN
  }
}
