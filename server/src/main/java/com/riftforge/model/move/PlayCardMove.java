package com.riftforge.model.move;

import com.riftforge.model.ZoneName;
import java.util.List;

public record PlayCardMove(
    String playerId,
    String instanceId,
    ZoneName targetZone,
    int x,
    int y,
    String targetInstanceId,
    String targetChainItemId,
    List<TargetSelection> targets,
    boolean accelerate,
    List<String> paymentRuneIds,
    List<String> premiumRuneIds
) implements MoveRequest {
  public PlayCardMove {
    targets = targets == null ? List.of() : List.copyOf(targets);
    paymentRuneIds = paymentRuneIds == null ? List.of() : List.copyOf(paymentRuneIds);
    premiumRuneIds = premiumRuneIds == null ? List.of() : List.copyOf(premiumRuneIds);
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, null, List.of(), false, List.of(), List.of());
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId, boolean accelerate) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, null, List.of(), accelerate, List.of(), List.of());
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId, boolean accelerate, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, null, List.of(), accelerate, paymentRuneIds, premiumRuneIds);
  }

  public PlayCardMove(String playerId, String instanceId, ZoneName targetZone, int x, int y, String targetInstanceId, List<TargetSelection> targets, boolean accelerate, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    this(playerId, instanceId, targetZone, x, y, targetInstanceId, null, targets, accelerate, paymentRuneIds, premiumRuneIds);
  }

  public record TargetSelection(String role, String instanceId) {
    public static final String FRIENDLY_UNIT = "friendlyUnit";
    public static final String ENEMY_UNIT = "enemyUnit";
    public static final String BOOST_UNIT = "boostUnit";
    public static final String WEAKEN_UNIT = "weakenUnit";
    public static final String FIRST_FRIENDLY_UNIT = "firstFriendlyUnit";
    public static final String SECOND_FRIENDLY_UNIT = "secondFriendlyUnit";
  }
}
