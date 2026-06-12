package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.engine.CombatStatsService.CombatContext;
import com.riftforge.service.CardDataService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CombatResolver {
  public record CombatResult(boolean attackersRemain, boolean defendersEliminated) {}

  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final CardZoneService cardZoneService;
  private final CombatStatsService combatStatsService;

  public CombatResolver(CardDataService cardDataService, CardEffectRegistry effects, CardZoneService cardZoneService, CombatStatsService combatStatsService) {
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.cardZoneService = cardZoneService;
    this.combatStatsService = combatStatsService;
  }

  public CombatResult resolve(LiveGameState state, String attackingPlayerId) {
    List<CardInstance> attackers = battlefieldCards(state, attackingPlayerId);
    List<CardInstance> defenders = state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && !attackingPlayerId.equals(card.getOwnerId()))
        .toList();
    if (attackers.isEmpty() || defenders.isEmpty()) return new CombatResult(!attackers.isEmpty(), defenders.isEmpty());

    Map<String, Integer> damage = new HashMap<>();
    assignDamage(attackers, defenders, damage, true);
    assignDamage(defenders, attackers, damage, false);

    applyDamage(state, damage);

    boolean attackersRemain = !battlefieldCards(state, attackingPlayerId).isEmpty();
    boolean defendersEliminated = state.getCards().stream()
        .noneMatch(card -> card.getZone() == ZoneName.BATTLEFIELD && !attackingPlayerId.equals(card.getOwnerId()));
    healSurvivors(state);
    return new CombatResult(attackersRemain, defendersEliminated);
  }

  private void assignDamage(List<CardInstance> sources, List<CardInstance> targets, Map<String, Integer> damage, boolean attacking) {
    CombatContext context = attacking ? CombatContext.ATTACKING : CombatContext.DEFENDING;
    int pool = sources.stream().mapToInt(card -> combatStatsService.effectiveMight(card, context)).sum();
    List<CardInstance> ordered = targets.stream()
        .sorted(Comparator.comparingInt(this::assignmentPriority))
        .toList();
    for (CardInstance target : ordered) {
      if (pool <= 0) break;
      int lethal = lethalDamage(target);
      int assigned = Math.min(pool, lethal);
      damage.merge(target.getInstanceId(), assigned, Integer::sum);
      pool -= assigned;
    }
  }

  private void applyDamage(LiveGameState state, Map<String, Integer> damage) {
    for (CardInstance card : state.getCards().stream().filter(c -> c.getZone() == ZoneName.BATTLEFIELD).toList()) {
      int assigned = damage.getOrDefault(card.getInstanceId(), 0);
      if (assigned <= 0) continue;
      int currentHealth = card.getCurrentHealth() > 0 ? card.getCurrentHealth() : cardDataService.getCard(card.getCardId()).health();
      card.setCurrentHealth(Math.max(0, currentHealth - assigned));
    }
    for (CardInstance card : state.getCards().stream().filter(c -> c.getZone() == ZoneName.BATTLEFIELD).toList()) {
      if (damage.getOrDefault(card.getInstanceId(), 0) <= 0) continue;
      if (card.getCurrentHealth() > 0) continue;
      destroy(state, card);
    }
  }

  private int assignmentPriority(CardInstance card) {
    if (cardDataService.hasKeyword(card, "TANK")) return 0;
    if (cardDataService.hasKeyword(card, "BACKLINE")) return 2;
    return 1;
  }

  private int lethalDamage(CardInstance card) {
    int currentHealth = card.getCurrentHealth();
    if (currentHealth <= 0) currentHealth = cardDataService.getCard(card.getCardId()).health();
    return Math.max(1, currentHealth);
  }

  private void destroy(LiveGameState state, CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    state.getCards().stream()
        .filter(attachment -> card.getInstanceId().equals(attachment.getAttachedToInstanceId()))
        .toList()
        .forEach(attachment -> {
          cardZoneService.moveToGraveyard(attachment);
          attachment.setAttachedToInstanceId(null);
        });
    cardZoneService.moveToGraveyard(card);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
    GameEngine.log(state, card.getOwnerId(), def.name() + " was destroyed in combat.");
  }

  private void healSurvivors(LiveGameState state) {
    state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD)
        .forEach(card -> card.setCurrentHealth(cardDataService.getCard(card.getCardId()).health()));
  }

  private List<CardInstance> battlefieldCards(LiveGameState state, String playerId) {
    return state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && playerId.equals(card.getOwnerId()))
        .toList();
  }
}
