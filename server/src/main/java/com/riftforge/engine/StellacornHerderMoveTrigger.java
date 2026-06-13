package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StellacornHerderMoveTrigger implements TriggerHandler {
  private final CardDataService cardDataService;

  public StellacornHerderMoveTrigger(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  @Override
  public boolean supports(TriggerEvent event) {
    return event.type() == TriggerType.CARD_MOVED
        && event.oldZone() != event.newZone()
        && isNamed(event.sourceCard(), "Stellacorn Herder");
  }

  @Override
  public void handle(LiveGameState state, TriggerEvent event) {
    autoDraw(state, event.controllerId());
    log(state, event.controllerId(), "Stellacorn Herder drew 1 after moving.");
  }

  private void autoDraw(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getDeckPool().isEmpty()) {
      log(state, playerId, player.getName() + "'s deck is empty - no draw.");
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

  private boolean isNamed(CardInstance card, String name) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
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
