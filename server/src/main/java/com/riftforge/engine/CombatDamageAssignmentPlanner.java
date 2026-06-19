package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.BattlefieldLocationRules;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CombatDamageAssignmentPlanner {
  private final CardDataService cardDataService;
  private final CombatStatsService combatStatsService;
  private final CombatDamageRules combatDamageRules;

  public CombatDamageAssignmentPlanner(
      CardDataService cardDataService,
      CombatStatsService combatStatsService,
      CombatDamageRules combatDamageRules) {
    this.cardDataService = cardDataService;
    this.combatStatsService = combatStatsService;
    this.combatDamageRules = combatDamageRules;
  }

  public Optional<List<LiveGameState.CombatDamageAssignment>> plan(LiveGameState state, String playerId) {
    return assignmentState(state, playerId)
        .filter(LiveGameState.CombatAssignmentState::canAutoAssign)
        .map(LiveGameState.CombatAssignmentState::suggestedAssignments);
  }

  public Optional<LiveGameState.CombatAssignmentState> assignmentState(LiveGameState state) {
    if (state == null || state.getActiveShowdown() == null) return Optional.empty();
    String assigningPlayerId = state.getActiveShowdown().assigningPlayerId();
    if (assigningPlayerId == null || assigningPlayerId.isBlank()) return Optional.empty();
    return assignmentState(state, assigningPlayerId);
  }

  public Optional<LiveGameState.CombatAssignmentState> assignmentState(LiveGameState state, String playerId) {
    if (state == null || state.getActiveShowdown() == null) return Optional.empty();
    String locationId = BattlefieldLocationRules.normalize(state.getActiveShowdown().locationId());
    boolean attacking = playerId.equals(state.getActiveShowdown().attackingPlayerId());
    CombatStatsService.CombatContext context = attacking
        ? CombatStatsService.CombatContext.ATTACKING
        : CombatStatsService.CombatContext.DEFENDING;
    List<CardInstance> sources = combatantsFor(state, playerId, locationId).stream()
        .sorted(Comparator.comparing(CardInstance::getInstanceId))
        .toList();
    List<CardInstance> targets = state.getCards().stream()
        .filter(card -> !playerId.equals(card.getOwnerId()))
        .filter(this::isCombatant)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .sorted(targetOrder(state, attacking))
        .toList();
    if (sources.isEmpty() || targets.isEmpty()) {
      return Optional.of(combatAssignmentState(state, playerId, locationId, sources, targets, List.of(), false));
    }

    Map<String, Integer> sourceMight = new LinkedHashMap<>();
    int totalMight = 0;
    for (CardInstance source : sources) {
      int might = combatStatsService.effectiveMight(state, source, context);
      sourceMight.put(source.getInstanceId(), might);
      totalMight += might;
    }
    if (totalMight == 0) {
      return Optional.of(combatAssignmentState(state, playerId, locationId, sources, targets, List.of(), true, sourceMight));
    }

    Optional<Map<String, Integer>> quotas = targetQuotas(state, targets, totalMight, attacking);
    List<LiveGameState.CombatDamageAssignment> assignments = quotas
        .map(targetQuotas -> poolAssignments(playerId, targets, targetQuotas))
        .orElse(List.of());
    return Optional.of(combatAssignmentState(state, playerId, locationId, sources, targets, assignments, quotas.isPresent(), sourceMight));
  }

  private LiveGameState.CombatAssignmentState combatAssignmentState(
      LiveGameState state,
      String playerId,
      String locationId,
      List<CardInstance> sources,
      List<CardInstance> targets,
      List<LiveGameState.CombatDamageAssignment> assignments,
      boolean canAutoAssign) {
    return combatAssignmentState(state, playerId, locationId, sources, targets, assignments, canAutoAssign, Map.of());
  }

  private LiveGameState.CombatAssignmentState combatAssignmentState(
      LiveGameState state,
      String playerId,
      String locationId,
      List<CardInstance> sources,
      List<CardInstance> targets,
      List<LiveGameState.CombatDamageAssignment> assignments,
      boolean canAutoAssign,
      Map<String, Integer> sourceMight) {
    List<String> validTargetIds = targets.stream()
        .map(CardInstance::getInstanceId)
        .toList();
    boolean attacking = playerId.equals(state.getActiveShowdown().attackingPlayerId());
    CombatStatsService.CombatContext context = attacking
        ? CombatStatsService.CombatContext.ATTACKING
        : CombatStatsService.CombatContext.DEFENDING;
    List<LiveGameState.CombatDamageSourceOption> validSources = sources.stream()
        .map(source -> new LiveGameState.CombatDamageSourceOption(
            source.getInstanceId(),
            sourceMight.containsKey(source.getInstanceId())
                ? sourceMight.get(source.getInstanceId())
                : combatStatsService.effectiveMight(state, source, context),
            validTargetIds))
        .toList();
    int damagePool = validSources.stream()
        .mapToInt(LiveGameState.CombatDamageSourceOption::availableDamage)
        .sum();
    CombatStatsService.CombatContext targetContext = attacking
        ? CombatStatsService.CombatContext.DEFENDING
        : CombatStatsService.CombatContext.ATTACKING;
    List<LiveGameState.CombatDamageTargetOption> validTargets = targets.stream()
        .map(target -> new LiveGameState.CombatDamageTargetOption(
            target.getInstanceId(),
            combatDamageRules.combatLethalDamage(state, target, targetContext),
            hasTank(target)))
        .toList();
    return new LiveGameState.CombatAssignmentState(
        locationId,
        playerId,
        state.getActiveShowdown().step().name(),
        damagePool,
        validSources,
        validTargets,
        validTargetIds,
        assignments,
        canAutoAssign);
  }

  private Optional<Map<String, Integer>> targetQuotas(
      LiveGameState state,
      List<CardInstance> targets,
      int totalMight,
      boolean attackingAssignment) {
    if (targets.size() == 1) {
      return Optional.of(Map.of(targets.getFirst().getInstanceId(), totalMight));
    }

    CombatStatsService.CombatContext targetContext = attackingAssignment
        ? CombatStatsService.CombatContext.DEFENDING
        : CombatStatsService.CombatContext.ATTACKING;
    int prefixLethal = 0;
    Map<String, Integer> quotas = new LinkedHashMap<>();
    for (CardInstance target : targets) {
      int lethal = combatDamageRules.combatLethalDamage(state, target, targetContext);
      prefixLethal += lethal;
      quotas.put(target.getInstanceId(), lethal);
      if (totalMight == prefixLethal) return Optional.of(quotas);
      if (totalMight < prefixLethal) {
        return singleTargetQuota(state, targets, totalMight, targetContext);
      }
    }

    int excess = totalMight - prefixLethal;
    if (excess > 0) {
      CardInstance finalTarget = targets.getLast();
      quotas.merge(finalTarget.getInstanceId(), excess, Integer::sum);
    }
    return Optional.of(quotas);
  }

  private Optional<Map<String, Integer>> singleTargetQuota(
      LiveGameState state,
      List<CardInstance> targets,
      int totalMight,
      CombatStatsService.CombatContext targetContext) {
    boolean hasTank = targets.stream().anyMatch(this::hasTank);
    return targets.stream()
        .filter(target -> !hasTank || hasTank(target))
        .filter(target -> combatDamageRules.combatLethalDamage(state, target, targetContext) >= totalMight)
        .findFirst()
        .map(target -> Map.of(target.getInstanceId(), totalMight));
  }

  private List<LiveGameState.CombatDamageAssignment> poolAssignments(
      String playerId,
      List<CardInstance> targets,
      Map<String, Integer> targetQuotas) {
    List<LiveGameState.CombatDamageAssignment> assignments = new ArrayList<>();
    for (CardInstance target : targets) {
      int amount = targetQuotas.getOrDefault(target.getInstanceId(), 0);
      if (amount <= 0) continue;
      assignments.add(new LiveGameState.CombatDamageAssignment(
          playerId,
          target.getInstanceId(),
          amount));
    }
    return assignments;
  }

  private List<CardInstance> combatantsFor(LiveGameState state, String playerId, String locationId) {
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()))
        .filter(this::isCombatant)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .toList();
  }

  private boolean isCombatant(CardInstance card) {
    if (card.getZone() != ZoneName.BATTLEFIELD || card.isFaceDown()) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private Comparator<CardInstance> targetOrder(LiveGameState state, boolean attackingAssignment) {
    CombatStatsService.CombatContext targetContext = attackingAssignment
        ? CombatStatsService.CombatContext.DEFENDING
        : CombatStatsService.CombatContext.ATTACKING;
    return Comparator
        .comparing((CardInstance card) -> !hasTank(card))
        .thenComparingInt(card -> combatDamageRules.combatLethalDamage(state, card, targetContext))
        .thenComparing(CardInstance::getInstanceId);
  }

  private boolean hasTank(CardInstance card) {
    return cardDataService.hasKeyword(card, "TANK");
  }
}
