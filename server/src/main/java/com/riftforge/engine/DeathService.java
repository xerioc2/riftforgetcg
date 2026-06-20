package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeathService {
  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final CardZoneService cardZoneService;
  private final DeathTriggerService deathTriggerService;
  private final ReplacementEffectService replacementEffectService;

  public DeathService(
      CardDataService cardDataService,
      CardEffectRegistry effects,
      CardZoneService cardZoneService,
      DeathTriggerService deathTriggerService,
      ReplacementEffectService replacementEffectService) {
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.cardZoneService = cardZoneService;
    this.deathTriggerService = deathTriggerService;
    this.replacementEffectService = replacementEffectService;
  }

  public void resolveDeaths(LiveGameState state, List<CardInstance> candidates, DeathEvent.DeathCause cause, String logSuffix) {
    DeathResolutionContext context = new DeathResolutionContext();
    List<DeathEvent> deaths = new ArrayList<>();
    for (CardInstance candidate : candidates) {
      if (!isDeathCandidate(candidate)) continue;
      if (context.wasReplacedThisPass(candidate.getInstanceId())) continue;
      if (replacementEffectService.replaceWouldDie(state, candidate, context)) continue;
      DeathEvent death = deathTriggerService.capture(candidate, state, cause);
      destroy(state, candidate, logSuffix);
      deaths.add(death);
    }
    deathTriggerService.process(state, deaths);
  }

  public ReplacementEffectService replacementEffects() {
    return replacementEffectService;
  }

  private void destroy(LiveGameState state, CardInstance card, String logSuffix) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    List<CardInstance> returnedAttachments = cardZoneService.returnAttachmentsToBase(state, card);
    if (returnedAttachments != null) {
      for (CardInstance attachment : returnedAttachments) {
        GameEngine.log(state, attachment.getOwnerId(), cardName(attachment) + " returned to Base.");
      }
    }
    cardZoneService.moveToGraveyard(card);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
    if (logSuffix != null && !logSuffix.isBlank()) {
      GameEngine.log(state, card.getOwnerId(), def.name() + " " + logSuffix);
    }
  }

  private boolean isDeathCandidate(CardInstance card) {
    if (card == null || card.isFaceDown()) return false;
    if (card.getZone() != ZoneName.BASE && card.getZone() != ZoneName.BATTLEFIELD) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private String cardName(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def == null ? card.getCardId() : def.name();
  }
}
