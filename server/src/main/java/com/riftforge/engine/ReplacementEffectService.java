package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ReplacementEffect;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReplacementEffectService {
  private final CardDataService cardDataService;
  private final CardZoneService cardZoneService;

  public ReplacementEffectService(CardDataService cardDataService, CardZoneService cardZoneService) {
    this.cardDataService = cardDataService;
    this.cardZoneService = cardZoneService;
  }

  public ReplacementEffect registerWouldDieDestroySourceInstead(
      LiveGameState state,
      String controllerId,
      String sourceInstanceId,
      String protectedInstanceId) {
    CardInstance source = findCard(state, sourceInstanceId);
    CardInstance protectedCard = findCard(state, protectedInstanceId);
    if (source.getInstanceId().equals(protectedCard.getInstanceId())) {
      throw new IllegalMoveException("A replacement source cannot protect itself in v1.");
    }
    if (!controllerId.equals(source.getOwnerId())) throw new IllegalMoveException("You do not control that replacement source.");
    if (!controllerId.equals(protectedCard.getOwnerId())) throw new IllegalMoveException("You do not control that protected card.");
    if (!isUnitOrChampion(protectedCard)) throw new IllegalMoveException("Replacement target must be a Unit or Champion.");
    boolean sourceAlreadyArmed = state.getReplacementEffects().stream()
        .filter(effect -> !effect.isConsumed())
        .anyMatch(effect -> sourceInstanceId.equals(effect.getSourceInstanceId()));
    if (sourceAlreadyArmed) throw new IllegalMoveException("That replacement source is already protecting a card.");
    ReplacementEffect effect = new ReplacementEffect();
    effect.setReplacementEffectId(UUID.randomUUID().toString());
    effect.setEffectType(ReplacementEffect.WOULD_DIE_DESTROY_SOURCE_INSTEAD);
    effect.setSourceInstanceId(sourceInstanceId);
    effect.setProtectedInstanceId(protectedInstanceId);
    effect.setControllerId(controllerId);
    effect.setCreatedSequence(state.getReplacementEffects().size());
    state.getReplacementEffects().add(effect);
    return effect;
  }

  public boolean replaceWouldDie(LiveGameState state, CardInstance protectedCard, DeathResolutionContext context) {
    if (protectedCard == null || context.wasReplacedThisPass(protectedCard.getInstanceId())) return false;
    Optional<ReplacementEffect> replacement = state.getReplacementEffects().stream()
        .filter(effect -> !effect.isConsumed())
        .filter(effect -> ReplacementEffect.WOULD_DIE_DESTROY_SOURCE_INSTEAD.equals(effect.getEffectType()))
        .filter(effect -> protectedCard.getInstanceId().equals(effect.getProtectedInstanceId()))
        .sorted(Comparator.comparingInt(ReplacementEffect::getCreatedSequence)
            .thenComparing(ReplacementEffect::getReplacementEffectId))
        .findFirst();
    if (replacement.isEmpty()) return false;

    ReplacementEffect effect = replacement.get();
    Optional<CardInstance> source = state.getCards().stream()
        .filter(card -> effect.getSourceInstanceId().equals(card.getInstanceId()))
        .findFirst();
    if (source.isEmpty() || !canDestroySource(source.get())) {
      effect.setConsumed(true);
      removeConsumed(state);
      return false;
    }

    destroySourceInstead(state, source.get());
    applyProtectedReplacementResult(protectedCard);
    effect.setConsumed(true);
    context.markReplaced(protectedCard.getInstanceId());
    removeConsumed(state);
    return true;
  }

  private void destroySourceInstead(LiveGameState state, CardInstance source) {
    List<CardInstance> returnedAttachments = cardZoneService.returnAttachmentsToBase(state, source);
    if (returnedAttachments != null) {
      for (CardInstance attachment : returnedAttachments) {
        GameEngine.log(state, attachment.getOwnerId(), cardName(attachment) + " returned to Base.");
      }
    }
    source.setAttachedToInstanceId(null);
    cardZoneService.moveToGraveyard(source);
    GameEngine.log(state, source.getOwnerId(), cardName(source) + " was destroyed by a replacement effect.");
  }

  private void applyProtectedReplacementResult(CardInstance protectedCard) {
    CardDefinition def = cardDataService.getCard(protectedCard.getCardId());
    protectedCard.setCurrentHealth(Math.max(1, def == null ? protectedCard.getCurrentHealth() : def.health()));
    protectedCard.setTapped(true);
    protectedCard.setZone(ZoneName.BASE);
    protectedCard.setBattlefieldLocationId(null);
    protectedCard.setX(0);
    protectedCard.setY(0);
  }

  private boolean canDestroySource(CardInstance source) {
    if (source.isFaceDown()) return false;
    return source.getZone() == ZoneName.BASE || source.getZone() == ZoneName.BATTLEFIELD;
  }

  private void removeConsumed(LiveGameState state) {
    state.getReplacementEffects().removeIf(ReplacementEffect::isConsumed);
  }

  private CardInstance findCard(LiveGameState state, String instanceId) {
    if (instanceId == null || instanceId.isBlank()) throw new IllegalMoveException("Card instance id is required.");
    return state.getCards().stream()
        .filter(card -> instanceId.equals(card.getInstanceId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("Card instance not found."));
  }

  private boolean isUnitOrChampion(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private String cardName(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def == null ? card.getCardId() : def.name();
  }
}
