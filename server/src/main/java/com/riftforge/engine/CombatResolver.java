package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import org.springframework.stereotype.Component;

@Component
public class CombatResolver {
  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final CardZoneService cardZoneService;

  public CombatResolver(CardDataService cardDataService, CardEffectRegistry effects, CardZoneService cardZoneService) {
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.cardZoneService = cardZoneService;
  }

  public void resolve(LiveGameState state) {
    for (String attackerId : state.getDeclaredAttackers()) {
      CardInstance attacker = findCard(state, attackerId);
      CardDefinition attackerDef = cardDataService.getCard(attacker.getCardId());
      String blockerId = state.getBlockerToAttacker().entrySet().stream().filter(e -> e.getValue().equals(attackerId)).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
      if (blockerId == null) {
        player(state, attacker.getOwnerId()).setScore(player(state, attacker.getOwnerId()).getScore() + 1);
        GameEngine.log(state, attacker.getOwnerId(), attackerDef.name() + " attacked unblocked - score +1");
      } else {
        CardInstance blocker = findCard(state, blockerId);
        CardDefinition blockerDef = cardDataService.getCard(blocker.getCardId());
        int attackerDamage = Math.max(0, attackerDef.power() + attacker.getTemporaryPowerModifier());
        int blockerDamage = Math.max(0, blockerDef.power() + blocker.getTemporaryPowerModifier());
        if (cardDataService.hasKeyword(blocker, "TOUGH")) attackerDamage = Math.max(0, attackerDamage - 1);
        if (cardDataService.hasKeyword(attacker, "TOUGH")) blockerDamage = Math.max(0, blockerDamage - 1);
        int originalBlockerHealth = blocker.getCurrentHealth() <= 0 ? blockerDef.health() : blocker.getCurrentHealth();
        blocker.setCurrentHealth(originalBlockerHealth - attackerDamage);
        attacker.setCurrentHealth((attacker.getCurrentHealth() <= 0 ? attackerDef.health() : attacker.getCurrentHealth()) - blockerDamage);
        if (blocker.getCurrentHealth() <= 0) {
          cardZoneService.moveToGraveyard(blocker);
          discardAttachments(state, blocker);
          GameEngine.log(state, blocker.getOwnerId(), blockerDef.name() + " was destroyed");
          effects.getEffect(blocker.getCardId()).ifPresent(effect -> effect.onDestroy(blocker, state));
          if (attacker.getCurrentHealth() > 0) {
            player(state, attacker.getOwnerId()).setScore(player(state, attacker.getOwnerId()).getScore() + 1);
            GameEngine.log(state, attacker.getOwnerId(), attackerDef.name() + " won combat - score +1");
          }
        }
        if (attacker.getCurrentHealth() <= 0) {
          cardZoneService.moveToGraveyard(attacker);
          discardAttachments(state, attacker);
          GameEngine.log(state, attacker.getOwnerId(), attackerDef.name() + " was destroyed in combat");
          effects.getEffect(attacker.getCardId()).ifPresent(effect -> effect.onDestroy(attacker, state));
        }
      }
    }
    state.setDeclaredAttackers(new java.util.ArrayList<>());
    state.setBlockerToAttacker(new java.util.HashMap<>());
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  private PlayerState player(LiveGameState state, String id) {
    return state.getPlayers().stream().filter(p -> p.getUserId().equals(id)).findFirst().orElseThrow();
  }

  private void discardAttachments(LiveGameState state, CardInstance unit) {
    state.getCards().stream()
        .filter(card -> unit.getInstanceId().equals(card.getAttachedToInstanceId()))
        .forEach(card -> {
          cardZoneService.moveToGraveyard(card);
          card.setAttachedToInstanceId(null);
        });
  }
}
