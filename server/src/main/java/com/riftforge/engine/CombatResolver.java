package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.engine.CombatStatsService.CombatContext;
import com.riftforge.rules.BattlefieldLocationRules;
import com.riftforge.service.CardDataService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CombatResolver {
  public record CombatResult(boolean attackersRemain, boolean defendersEliminated) {}

  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final CardZoneService cardZoneService;
  private final CombatStatsService combatStatsService;
  private final DeathTriggerService deathTriggerService;
  private final CombatDamageRules combatDamageRules;

  public CombatResolver(CardDataService cardDataService, CardEffectRegistry effects, CardZoneService cardZoneService, CombatStatsService combatStatsService, DeathTriggerService deathTriggerService) {
    this(cardDataService, effects, cardZoneService, combatStatsService, deathTriggerService, new CombatDamageRules(cardDataService));
  }

  @Autowired
  public CombatResolver(CardDataService cardDataService, CardEffectRegistry effects, CardZoneService cardZoneService, CombatStatsService combatStatsService, DeathTriggerService deathTriggerService, CombatDamageRules combatDamageRules) {
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.cardZoneService = cardZoneService;
    this.combatStatsService = combatStatsService;
    this.deathTriggerService = deathTriggerService;
    this.combatDamageRules = combatDamageRules;
  }

  public CombatResult resolve(LiveGameState state, String attackingPlayerId) {
    String locationId = BattlefieldLocationRules.activeShowdownLocation(state);
    List<CardInstance> attackers = battlefieldCards(state, attackingPlayerId, locationId);
    List<CardInstance> defenders = state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && !attackingPlayerId.equals(card.getOwnerId()))
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .toList();
    if (attackers.isEmpty() || defenders.isEmpty()) return new CombatResult(!attackers.isEmpty(), defenders.isEmpty());

    Map<String, Integer> damage = new HashMap<>();
    assignDamage(state, attackers, defenders, damage, true);
    assignDamage(state, defenders, attackers, damage, false);

    applyDamage(state, damage, attackingPlayerId);

    boolean attackersRemain = !battlefieldCards(state, attackingPlayerId, locationId).isEmpty();
    boolean defendersEliminated = state.getCards().stream()
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD && !attackingPlayerId.equals(card.getOwnerId()));
    healSurvivors(state, locationId);
    return new CombatResult(attackersRemain, defendersEliminated);
  }

  public CombatResult resolveAssigned(
      LiveGameState state,
      String attackingPlayerId,
      List<LiveGameState.CombatDamageAssignment> attackerAssignments,
      List<LiveGameState.CombatDamageAssignment> defenderAssignments) {
    String locationId = BattlefieldLocationRules.activeShowdownLocation(state);
    Map<String, Integer> damage = new HashMap<>();
    for (LiveGameState.CombatDamageAssignment assignment : attackerAssignments) {
      damage.merge(assignment.targetInstanceId(), assignment.amount(), Integer::sum);
    }
    for (LiveGameState.CombatDamageAssignment assignment : defenderAssignments) {
      damage.merge(assignment.targetInstanceId(), assignment.amount(), Integer::sum);
    }
    applyDamage(state, damage, attackingPlayerId);

    boolean attackersRemain = !battlefieldCombatants(state, attackingPlayerId, locationId).isEmpty();
    boolean defendersEliminated = state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD && !attackingPlayerId.equals(card.getOwnerId()));
    healSurvivors(state, locationId);
    return new CombatResult(attackersRemain, defendersEliminated);
  }

  public List<CardInstance> battlefieldCombatants(LiveGameState state, String playerId) {
    return battlefieldCombatants(state, playerId, CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  public List<CardInstance> battlefieldCombatants(LiveGameState state, String playerId, String locationId) {
    return state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && playerId.equals(card.getOwnerId()))
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .toList();
  }

  public List<CardInstance> opposingBattlefieldCombatants(LiveGameState state, String playerId) {
    return opposingBattlefieldCombatants(state, playerId, CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  public List<CardInstance> opposingBattlefieldCombatants(LiveGameState state, String playerId, String locationId) {
    return state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && !playerId.equals(card.getOwnerId()))
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .toList();
  }

  public int lethalDamage(CardInstance card) {
    return combatDamageRules.lethalDamage(card);
  }

  public int lethalDamage(LiveGameState state, CardInstance card) {
    return combatDamageRules.lethalDamage(state, card);
  }

  public boolean isCombatant(CardInstance card) {
    if (card == null || card.getZone() != ZoneName.BATTLEFIELD || card.isFaceDown()) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private void assignDamage(LiveGameState state, List<CardInstance> sources, List<CardInstance> targets, Map<String, Integer> damage, boolean attacking) {
    CombatContext context = attacking ? CombatContext.ATTACKING : CombatContext.DEFENDING;
    int pool = sources.stream().mapToInt(card -> combatStatsService.effectiveMight(state, card, context)).sum();
    List<CardInstance> ordered = targets.stream()
        .sorted(Comparator.comparingInt(this::assignmentPriority))
        .toList();
    for (CardInstance target : ordered) {
      if (pool <= 0) break;
      CombatContext targetContext = attacking ? CombatContext.DEFENDING : CombatContext.ATTACKING;
      int lethal = combatDamageRules.combatLethalDamage(state, target, targetContext);
      int assigned = Math.min(pool, lethal);
      damage.merge(target.getInstanceId(), assigned, Integer::sum);
      pool -= assigned;
    }
  }

  private void applyDamage(LiveGameState state, Map<String, Integer> damage, String attackingPlayerId) {
    List<CardInstance> destroyed = new java.util.ArrayList<>();
    for (CardInstance card : state.getCards().stream().filter(c -> c.getZone() == ZoneName.BATTLEFIELD).toList()) {
      int assigned = damage.getOrDefault(card.getInstanceId(), 0);
      if (assigned <= 0) continue;
      CombatContext context = combatContextFor(attackingPlayerId, card);
      int lethal = combatDamageRules.combatLethalDamage(state, card, context);
      if (assigned >= lethal) {
        card.setCurrentHealth(0);
        destroyed.add(card);
      }
    }
    List<DeathEvent> deaths = destroyed.stream()
        .map(card -> deathTriggerService.capture(card, state, DeathEvent.DeathCause.COMBAT))
        .toList();
    destroyed.forEach(card -> destroy(state, card));
    deathTriggerService.process(state, deaths);
  }

  private CombatContext combatContextFor(String attackingPlayerId, CardInstance card) {
    return card.getOwnerId() != null && card.getOwnerId().equals(attackingPlayerId)
        ? CombatContext.ATTACKING
        : CombatContext.DEFENDING;
  }

  private int assignmentPriority(CardInstance card) {
    if (cardDataService.hasKeyword(card, "TANK")) return 0;
    if (cardDataService.hasKeyword(card, "BACKLINE")) return 2;
    return 1;
  }

  private void destroy(LiveGameState state, CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    List<CardInstance> returnedAttachments = cardZoneService.returnAttachmentsToBase(state, card);
    if (returnedAttachments != null) {
      for (CardInstance attachment : returnedAttachments) {
        GameEngine.log(state, attachment.getOwnerId(), cardName(attachment) + " returned to Base.");
      }
    }
    cardZoneService.moveToGraveyard(card);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
    GameEngine.log(state, card.getOwnerId(), def.name() + " was destroyed in combat.");
  }

  private String cardName(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def == null ? card.getCardId() : def.name();
  }

  private void healSurvivors(LiveGameState state, String locationId) {
    state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .forEach(card -> card.setCurrentHealth(combatStatsService.effectiveMaxHealth(state, card)));
  }

  private List<CardInstance> battlefieldCards(LiveGameState state, String playerId, String locationId) {
    return state.getCards().stream()
        .filter(this::isCombatant)
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && playerId.equals(card.getOwnerId()))
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .toList();
  }
}
