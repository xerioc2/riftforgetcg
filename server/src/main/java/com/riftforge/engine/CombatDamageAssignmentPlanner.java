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
        .sorted(targetOrder())
        .toList();
    if (sources.isEmpty() || targets.isEmpty()) return Optional.empty();

    Map<String, Integer> sourceMight = new LinkedHashMap<>();
    int totalMight = 0;
    for (CardInstance source : sources) {
      int might = combatStatsService.effectiveMight(source, context);
      sourceMight.put(source.getInstanceId(), might);
      totalMight += might;
    }
    if (totalMight == 0) return Optional.of(List.of());

    Optional<Map<String, Integer>> quotas = targetQuotas(targets, totalMight);
    return quotas.map(targetQuotas -> splitSourceDamage(sources, sourceMight, targets, targetQuotas));
  }

  private Optional<Map<String, Integer>> targetQuotas(List<CardInstance> targets, int totalMight) {
    if (targets.size() == 1) {
      return Optional.of(Map.of(targets.getFirst().getInstanceId(), totalMight));
    }

    int prefixLethal = 0;
    Map<String, Integer> quotas = new LinkedHashMap<>();
    for (CardInstance target : targets) {
      int lethal = combatDamageRules.lethalDamage(target);
      prefixLethal += lethal;
      quotas.put(target.getInstanceId(), lethal);
      if (totalMight == prefixLethal) return Optional.of(quotas);
      if (totalMight < prefixLethal) {
        return singleTargetQuota(targets, totalMight);
      }
    }

    int excess = totalMight - prefixLethal;
    if (excess > 0) {
      CardInstance finalTarget = targets.getLast();
      quotas.merge(finalTarget.getInstanceId(), excess, Integer::sum);
    }
    return Optional.of(quotas);
  }

  private Optional<Map<String, Integer>> singleTargetQuota(List<CardInstance> targets, int totalMight) {
    boolean hasTank = targets.stream().anyMatch(this::hasTank);
    return targets.stream()
        .filter(target -> !hasTank || hasTank(target))
        .filter(target -> combatDamageRules.lethalDamage(target) >= totalMight)
        .findFirst()
        .map(target -> Map.of(target.getInstanceId(), totalMight));
  }

  private List<LiveGameState.CombatDamageAssignment> splitSourceDamage(
      List<CardInstance> sources,
      Map<String, Integer> sourceMight,
      List<CardInstance> targets,
      Map<String, Integer> targetQuotas) {
    List<LiveGameState.CombatDamageAssignment> assignments = new ArrayList<>();
    Map<String, Integer> assignedByTarget = new LinkedHashMap<>();
    int targetIndex = 0;
    for (CardInstance source : sources) {
      int remaining = sourceMight.getOrDefault(source.getInstanceId(), 0);
      while (remaining > 0 && targetIndex < targets.size()) {
        CardInstance target = targets.get(targetIndex);
        int quota = targetQuotas.getOrDefault(target.getInstanceId(), 0);
        int assigned = assignedByTarget.getOrDefault(target.getInstanceId(), 0);
        int needed = quota - assigned;
        if (needed <= 0) {
          targetIndex++;
          continue;
        }
        int amount = Math.min(remaining, needed);
        assignments.add(new LiveGameState.CombatDamageAssignment(
            source.getInstanceId(),
            target.getInstanceId(),
            amount));
        remaining -= amount;
        assignedByTarget.merge(target.getInstanceId(), amount, Integer::sum);
      }
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

  private Comparator<CardInstance> targetOrder() {
    return Comparator
        .comparing((CardInstance card) -> !hasTank(card))
        .thenComparing(CardInstance::getInstanceId);
  }

  private boolean hasTank(CardInstance card) {
    return cardDataService.hasKeyword(card, "TANK");
  }
}
