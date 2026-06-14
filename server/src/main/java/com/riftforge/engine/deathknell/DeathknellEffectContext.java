package com.riftforge.engine.deathknell;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class DeathknellEffectContext {
  private final CardDataService cardDataService;

  public DeathknellEffectContext(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public void autoDraw(LiveGameState state, String playerId, String emptyDeckMessage) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getDeckPool().isEmpty()) {
      log(state, playerId, emptyDeckMessage == null ? player.getName() + "'s deck is empty." : emptyDeckMessage);
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

  public void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        userId,
        text));
  }
}
