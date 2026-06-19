package com.riftforge.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.riftforge.rules.LegalAction;

public class LiveGameState {
  private String roomCode;
  private Phase currentPhase;
  private String activePlayerId;
  private String firstPlayerId;
  private int turnNumber;
  private List<CardInstance> cards = new ArrayList<>();
  private List<PlayerState> players = new ArrayList<>();
  private List<RuneState> runes = new ArrayList<>();
  private List<LogEntry> log = new ArrayList<>();
  private String updatedAt;
  private String winnerId;
  private Set<String> mulligansDone = new HashSet<>();
  private boolean cardPlayedThisTurn;
  private Map<String, String> battlefieldController = new HashMap<>();
  private Set<String> scoredBattlefieldsThisTurn = new HashSet<>();
  private List<RevealedHandSnapshot> revealedHands = new ArrayList<>();
  private ShowdownState activeShowdown;
  private GameMode gameMode = GameMode.ENFORCED;
  private Set<LegalAction> legalActions = new HashSet<>();
  private PendingChoice pendingChoice;
  private ChainState chainState;
  private CombatAssignmentState combatAssignmentState;

  public record LogEntry(String id, String timestamp, String userId, String text) {}
  public record ShowdownState(
      String attackingPlayerId,
      List<String> attackerInstanceIds,
      Map<String, Integer> gankingBonuses,
      ShowdownStep step,
      List<String> relevantPlayerIds,
      String focusedPlayerId,
      int consecutivePasses,
      boolean readyToResolve,
      String assigningPlayerId,
      List<CombatDamageAssignment> attackerAssignments,
      List<CombatDamageAssignment> defenderAssignments,
      String locationId) {
    public ShowdownState {
      locationId = locationId == null || locationId.isBlank()
          ? CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID
          : locationId;
    }

    public ShowdownState(String attackingPlayerId, List<String> attackerInstanceIds, Map<String, Integer> gankingBonuses) {
      this(attackingPlayerId, attackerInstanceIds, gankingBonuses, ShowdownStep.ACTION_WINDOW);
    }

    public ShowdownState(String attackingPlayerId, List<String> attackerInstanceIds, Map<String, Integer> gankingBonuses, ShowdownStep step) {
      this(attackingPlayerId, attackerInstanceIds, gankingBonuses, step, List.of(), attackingPlayerId, 0, false, null, List.of(), List.of(), CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
    }

    public ShowdownState(
        String attackingPlayerId,
        List<String> attackerInstanceIds,
        Map<String, Integer> gankingBonuses,
        ShowdownStep step,
        List<String> relevantPlayerIds,
        String focusedPlayerId,
        int consecutivePasses,
        boolean readyToResolve) {
      this(attackingPlayerId, attackerInstanceIds, gankingBonuses, step, relevantPlayerIds, focusedPlayerId, consecutivePasses, readyToResolve, null, List.of(), List.of(), CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
    }

    public ShowdownState(
        String attackingPlayerId,
        List<String> attackerInstanceIds,
        Map<String, Integer> gankingBonuses,
        ShowdownStep step,
        List<String> relevantPlayerIds,
        String focusedPlayerId,
        int consecutivePasses,
        boolean readyToResolve,
        String assigningPlayerId,
        List<CombatDamageAssignment> attackerAssignments,
        List<CombatDamageAssignment> defenderAssignments) {
      this(attackingPlayerId, attackerInstanceIds, gankingBonuses, step, relevantPlayerIds, focusedPlayerId, consecutivePasses, readyToResolve, assigningPlayerId, attackerAssignments, defenderAssignments, CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
    }
  }

  public record CombatDamageAssignment(String sourceInstanceId, String targetInstanceId, int amount) {}

  public record CombatDamageSourceOption(
      String sourceInstanceId,
      int availableDamage,
      List<String> validTargetInstanceIds) {
    public CombatDamageSourceOption {
      validTargetInstanceIds = validTargetInstanceIds == null ? List.of() : List.copyOf(validTargetInstanceIds);
    }
  }

  public record CombatDamageTargetOption(
      String targetInstanceId,
      int lethalDamage,
      boolean tank) {}

  public record CombatAssignmentState(
      String locationId,
      String assigningPlayerId,
      String step,
      int damagePool,
      List<CombatDamageSourceOption> validSources,
      List<CombatDamageTargetOption> validTargets,
      List<String> validTargetInstanceIds,
      List<CombatDamageAssignment> suggestedAssignments,
      boolean canAutoAssign) {
    public CombatAssignmentState {
      locationId = locationId == null || locationId.isBlank()
          ? CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID
          : locationId;
      validSources = validSources == null ? List.of() : List.copyOf(validSources);
      validTargets = validTargets == null ? List.of() : List.copyOf(validTargets);
      validTargetInstanceIds = validTargetInstanceIds == null ? List.of() : List.copyOf(validTargetInstanceIds);
      suggestedAssignments = suggestedAssignments == null ? List.of() : List.copyOf(suggestedAssignments);
    }
  }

  public record ChainState(
      String chainId,
      List<ChainItem> chainItems,
      List<String> relevantPlayerIds,
      String focusedPlayerId,
      int consecutivePasses,
      boolean readyToResolveTop,
      String sourceContext) {
    public ChainState {
      chainItems = chainItems == null ? List.of() : List.copyOf(chainItems);
      relevantPlayerIds = relevantPlayerIds == null ? List.of() : List.copyOf(relevantPlayerIds);
    }

    public ChainItem topItem() {
      return chainItems.isEmpty() ? null : chainItems.get(chainItems.size() - 1);
    }
  }

  public record ChainItem(
      String itemId,
      String controllerPlayerId,
      String sourceCardInstanceId,
      String sourceCardId,
      String sourceCardName,
      String effectKey,
      List<String> targetInstanceIds,
      int order,
      String publicDescription,
      String visibility,
      String status,
      boolean counterable,
      boolean targetableOnChain,
      String chainItemType,
      ZoneName sourceZoneBeforeChain,
      List<ChainTarget> chainTargets) {
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_CONTROLLER_ONLY = "CONTROLLER_ONLY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_COUNTERED = "COUNTERED";
    public static final String STATUS_FIZZLED = "FIZZLED";
    public static final String TYPE_SPELL = "SPELL";
    public static final String TYPE_ABILITY = "ABILITY";
    public static final String TYPE_TEST = "TEST";
    public static final String EFFECT_NO_OP_TEST = "NO_OP_TEST";
    public static final String EFFECT_DRAW_1_TEST = "DRAW_1_TEST";
    public static final String EFFECT_DRAW_1 = "DRAW_1";
    public static final String EFFECT_GUST_RETURN = "GUST_RETURN";
    public static final String EFFECT_DISCIPLINE_BOOST_DRAW = "DISCIPLINE_BOOST_DRAW";
    public static final String EFFECT_EN_GARDE_BOOST = "EN_GARDE_BOOST";
    public static final String EFFECT_DEFIANT_DANCE_MODIFIERS = "DEFIANT_DANCE_MODIFIERS";
    public static final String EFFECT_FLASH_RECALL = "FLASH_RECALL";
    public static final String EFFECT_STACKED_DECK_PICK_ONE = "STACKED_DECK_PICK_ONE";
    public static final String EFFECT_DEFY_COUNTER = "DEFY_COUNTER";
    public static final String EFFECT_NOT_SO_FAST_COUNTER = "NOT_SO_FAST_COUNTER";

    public ChainItem(
        String itemId,
        String controllerPlayerId,
        String sourceCardInstanceId,
        String sourceCardId,
        String sourceCardName,
        String effectKey,
        List<String> targetInstanceIds,
        int order,
        String publicDescription,
        String visibility,
        String status,
        boolean counterable,
        boolean targetableOnChain,
        String chainItemType,
        ZoneName sourceZoneBeforeChain) {
      this(
          itemId,
          controllerPlayerId,
          sourceCardInstanceId,
          sourceCardId,
          sourceCardName,
          effectKey,
          targetInstanceIds,
          order,
          publicDescription,
          visibility,
          status,
          counterable,
          targetableOnChain,
          chainItemType,
          sourceZoneBeforeChain,
          List.of());
    }

    public ChainItem(
        String itemId,
        String controllerPlayerId,
        String sourceCardInstanceId,
        String sourceCardId,
        String sourceCardName,
        String effectKey,
        List<String> targetInstanceIds,
        int order,
        String publicDescription,
        String visibility) {
      this(
          itemId,
          controllerPlayerId,
          sourceCardInstanceId,
          sourceCardId,
          sourceCardName,
          effectKey,
          targetInstanceIds,
          order,
          publicDescription,
          visibility,
          STATUS_PENDING,
          true,
          true,
          TYPE_SPELL,
          null);
    }

    public ChainItem(
        String itemId,
        String controllerPlayerId,
        String sourceCardInstanceId,
        String sourceCardId,
        String sourceCardName,
        String effectKey,
        List<String> targetInstanceIds,
        int order,
        String publicDescription) {
      this(
          itemId,
          controllerPlayerId,
          sourceCardInstanceId,
          sourceCardId,
          sourceCardName,
          effectKey,
          targetInstanceIds,
          order,
          publicDescription,
          VISIBILITY_PUBLIC);
    }

    public ChainItem {
      targetInstanceIds = targetInstanceIds == null ? List.of() : List.copyOf(targetInstanceIds);
      chainTargets = chainTargets == null ? List.of() : List.copyOf(chainTargets);
      visibility = visibility == null || visibility.isBlank() ? VISIBILITY_PUBLIC : visibility;
      status = status == null || status.isBlank() ? STATUS_PENDING : status;
      chainItemType = chainItemType == null || chainItemType.isBlank() ? TYPE_SPELL : chainItemType;
    }

    public boolean isPubliclyVisible() {
      return VISIBILITY_PUBLIC.equalsIgnoreCase(visibility);
    }

    public boolean isPending() {
      return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public ChainItem withStatus(String newStatus) {
      return new ChainItem(
          itemId,
          controllerPlayerId,
          sourceCardInstanceId,
          sourceCardId,
          sourceCardName,
          effectKey,
          targetInstanceIds,
          order,
          publicDescription,
          visibility,
          newStatus,
          counterable,
          targetableOnChain,
          chainItemType,
          sourceZoneBeforeChain,
          chainTargets);
    }
  }

  public record ChainTarget(
      String role,
      String targetInstanceId,
      String targetChainItemId,
      String targetControllerPlayerId,
      String targetKind,
      ZoneName targetZone,
      String publicLabel,
      boolean publicSafe) {}

  public String getRoomCode() { return roomCode; }
  public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
  public Phase getCurrentPhase() { return currentPhase; }
  public void setCurrentPhase(Phase currentPhase) { this.currentPhase = currentPhase; }
  public String getActivePlayerId() { return activePlayerId; }
  public void setActivePlayerId(String activePlayerId) { this.activePlayerId = activePlayerId; }
  public String getFirstPlayerId() { return firstPlayerId; }
  public void setFirstPlayerId(String firstPlayerId) { this.firstPlayerId = firstPlayerId; }
  public int getTurnNumber() { return turnNumber; }
  public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
  public List<CardInstance> getCards() { return cards; }
  public void setCards(List<CardInstance> cards) { this.cards = cards; }
  public List<PlayerState> getPlayers() { return players; }
  public void setPlayers(List<PlayerState> players) { this.players = players; }
  public List<RuneState> getRunes() { return runes; }
  public void setRunes(List<RuneState> runes) { this.runes = runes; }
  public List<LogEntry> getLog() { return log; }
  public void setLog(List<LogEntry> log) { this.log = log; }
  public String getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
  public String getWinnerId() { return winnerId; }
  public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
  public Set<String> getMulligansDone() { return mulligansDone; }
  public void setMulligansDone(Set<String> mulligansDone) { this.mulligansDone = mulligansDone; }
  public boolean isCardPlayedThisTurn() { return cardPlayedThisTurn; }
  public void setCardPlayedThisTurn(boolean cardPlayedThisTurn) { this.cardPlayedThisTurn = cardPlayedThisTurn; }
  public Map<String, String> getBattlefieldController() { return battlefieldController; }
  public void setBattlefieldController(Map<String, String> battlefieldController) { this.battlefieldController = battlefieldController; }
  public Set<String> getScoredBattlefieldsThisTurn() { return scoredBattlefieldsThisTurn; }
  public void setScoredBattlefieldsThisTurn(Set<String> scoredBattlefieldsThisTurn) { this.scoredBattlefieldsThisTurn = scoredBattlefieldsThisTurn; }
  public List<RevealedHandSnapshot> getRevealedHands() { return revealedHands; }
  public void setRevealedHands(List<RevealedHandSnapshot> revealedHands) { this.revealedHands = revealedHands; }
  public ShowdownState getActiveShowdown() { return activeShowdown; }
  public void setActiveShowdown(ShowdownState activeShowdown) { this.activeShowdown = activeShowdown; }
  public GameMode getGameMode() { return gameMode; }
  public void setGameMode(GameMode gameMode) { this.gameMode = gameMode == null ? GameMode.ENFORCED : gameMode; }
  public Set<LegalAction> getLegalActions() { return legalActions; }
  public void setLegalActions(Set<LegalAction> legalActions) { this.legalActions = legalActions == null ? new HashSet<>() : legalActions; }
  public PendingChoice getPendingChoice() { return pendingChoice; }
  public void setPendingChoice(PendingChoice pendingChoice) { this.pendingChoice = pendingChoice; }
  public ChainState getChainState() { return chainState; }
  public void setChainState(ChainState chainState) { this.chainState = chainState; }
  public CombatAssignmentState getCombatAssignmentState() { return combatAssignmentState; }
  public void setCombatAssignmentState(CombatAssignmentState combatAssignmentState) { this.combatAssignmentState = combatAssignmentState; }
}
