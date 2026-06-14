package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.engine.deathknell.DeathknellEffectContext;
import com.riftforge.engine.deathknell.DeathknellEffectHandler;
import com.riftforge.engine.deathknell.LonelyPoroDeathknellEffectHandler;
import com.riftforge.engine.deathknell.LoyalPoroDeathknellEffectHandler;
import com.riftforge.engine.deathknell.ScuttleCrabDeathknellEffectHandler;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeathTriggerService {
  private final CardDataService cardDataService;
  private final DeathknellEffectContext context;
  private final List<DeathknellEffectHandler> handlers;

  public DeathTriggerService(CardDataService cardDataService) {
    this(cardDataService, List.of(
        new LonelyPoroDeathknellEffectHandler(),
        new LoyalPoroDeathknellEffectHandler(),
        new ScuttleCrabDeathknellEffectHandler()));
  }

  @Autowired
  public DeathTriggerService(CardDataService cardDataService, List<DeathknellEffectHandler> handlers) {
    this.cardDataService = cardDataService;
    this.context = new DeathknellEffectContext(cardDataService);
    this.handlers = List.copyOf(handlers);
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
      if (!isUnitOrChampion(def)) continue;
      handlers.stream()
          .filter(handler -> handler.supports(def))
          .findFirst()
          .ifPresentOrElse(
              handler -> handler.resolve(state, death, context),
              () -> context.log(state, death.ownerId(), def.name() + "'s Deathknell is not fully supported yet."));
    }
  }

  private boolean isUnitOrChampion(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return isUnitOrChampion(def);
  }

  private boolean isUnitOrChampion(CardDefinition def) {
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }
}
