package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeathTriggerService {
  private final CardDataService cardDataService;

  public DeathTriggerService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public DeathEvent capture(CardInstance card, LiveGameState state, DeathEvent.DeathCause cause) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    boolean hadOtherFriendlyUnit = state.getCards().stream()
        .filter(candidate -> !candidate.getInstanceId().equals(card.getInstanceId()))
        .filter(candidate -> candidate.getZone() == card.getZone())
        .filter(candidate -> candidate.getOwnerId().equals(card.getOwnerId()))
        .anyMatch(this::isUnitOrChampion);
    return new DeathEvent(
        card.getInstanceId(),
        card.getCardId(),
        def.name(),
        card.getOwnerId(),
        card.getZone(),
        hadOtherFriendlyUnit,
        cause);
  }

  public void process(LiveGameState state, List<DeathEvent> deaths) {
    List<DeathEvent> ordered = new ArrayList<>(deaths);
    ordered.sort(Comparator.comparing(DeathEvent::cardName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(DeathEvent::instanceId));
    for (DeathEvent death : ordered) {
      CardDefinition def = cardDataService.getCard(death.cardId());
      if (!cardDataService.hasKeyword(death.cardId(), "DEATHKNELL")) continue;
      if (isNamed(def, "Loyal Poro")) {
        resolveLoyalPoro(state, death);
      } else if (isNamed(def, "Scuttle Crab")) {
        log(state, death.ownerId(), "Scuttle Crab's Deathknell is detected, but XP/reveal support is partial.");
      } else {
        log(state, death.ownerId(), def.name() + "'s Deathknell is not fully supported yet.");
      }
    }
  }

  private void resolveLoyalPoro(LiveGameState state, DeathEvent death) {
    if (!death.hadOtherFriendlyUnitAtLocation()) {
      log(state, death.ownerId(), "Loyal Poro died alone. Deathknell did not draw.");
      return;
    }
    autoDraw(state, death.ownerId());
    log(state, death.ownerId(), "Loyal Poro's Deathknell drew 1.");
  }

  private void autoDraw(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getDeckPool().isEmpty()) {
      log(state, playerId, player.getName() + "'s deck is empty - no Deathknell draw.");
      return;
    }
    String cardId = player.getDeckPool().remove(0);
    CardDefinition def = cardDataService.getCard(cardId);
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance instance = new CardInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setCardId(cardId);
    instance.setOwnerId(playerId);
    instance.setZone(ZoneName.HAND);
    instance.setCurrentHealth(def.health());
    instance.setHasSummoningSickness(false);
    instance.setTempKeywords(new ArrayList<>());
    instance.setZIndex(maxZ + 1);
    state.getCards().add(instance);
  }

  private boolean isUnitOrChampion(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private boolean isNamed(CardDefinition def, String name) {
    return def != null && def.name() != null && def.name().trim().equalsIgnoreCase(name);
  }

  private void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        userId,
        text));
  }
}
