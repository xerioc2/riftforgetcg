package com.riftforge.engine;

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

  public CombatResolver(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
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
        int attackerDamage = attackerDef.power();
        int blockerDamage = blockerDef.power();
        if (cardDataService.hasKeyword(blocker.getCardId(), "TOUGH")) attackerDamage = Math.max(0, attackerDamage - 1);
        if (cardDataService.hasKeyword(attacker.getCardId(), "TOUGH")) blockerDamage = Math.max(0, blockerDamage - 1);
        int originalBlockerHealth = blocker.getCurrentHealth() <= 0 ? blockerDef.health() : blocker.getCurrentHealth();
        blocker.setCurrentHealth(originalBlockerHealth - attackerDamage);
        attacker.setCurrentHealth((attacker.getCurrentHealth() <= 0 ? attackerDef.health() : attacker.getCurrentHealth()) - blockerDamage);
        if (blocker.getCurrentHealth() <= 0) {
          blocker.setZone(ZoneName.DISCARD);
          GameEngine.log(state, blocker.getOwnerId(), blockerDef.name() + " was destroyed");
          if (cardDataService.hasKeyword(attacker.getCardId(), "OVERWHELM") && attackerDamage > originalBlockerHealth) {
            player(state, attacker.getOwnerId()).setScore(player(state, attacker.getOwnerId()).getScore() + 1);
          }
        }
        if (attacker.getCurrentHealth() <= 0) {
          attacker.setZone(ZoneName.DISCARD);
          GameEngine.log(state, attacker.getOwnerId(), attackerDef.name() + " was destroyed in combat");
        }
      }
    }
    state.getDeclaredAttackers().clear();
    state.getBlockerToAttacker().clear();
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  private PlayerState player(LiveGameState state, String id) {
    return state.getPlayers().stream().filter(p -> p.getUserId().equals(id)).findFirst().orElseThrow();
  }
}
