package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CardEffectRegistry {
  private final Map<String, CardEffect> effects = new HashMap<>();
  private final CardDataService cardDataService;

  public CardEffectRegistry(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  @PostConstruct
  void init() {
    register(new CardEffect() {
      public String cardId() { return "origins-001"; }
      public void onPlay(CardInstance card, CardInstance target, LiveGameState state) {
        state.getCards().stream()
            .filter(c -> c.getOwnerId().equals(card.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD && !c.getInstanceId().equals(card.getInstanceId()))
            .forEach(c -> { if (!c.getTempKeywords().contains("TOUGH")) c.getTempKeywords().add("TOUGH"); });
      }
    });
    register(new CardEffect() {
      public String cardId() { return "origins-002"; }
      public void onPlay(CardInstance card, CardInstance target, LiveGameState state) {
        state.getCards().stream()
            .filter(c -> c.getOwnerId().equals(card.getOwnerId())
                && (c.getZone() == ZoneName.CHAMPION || c.getZone() == ZoneName.BATTLEFIELD)
                && "champion".equalsIgnoreCase(cardDataService.getCard(c.getCardId()).type()))
            .forEach(c -> { if (!c.getTempKeywords().contains("RUSH")) c.getTempKeywords().add("RUSH"); });
      }
    });
    register(new CardEffect() {
      public String cardId() { return "origins-003"; }
      public void onPlay(CardInstance card, CardInstance target, LiveGameState state) {
        if (!card.getTempKeywords().contains("OVERWHELM")) card.getTempKeywords().add("OVERWHELM");
      }
    });
    register(new CardEffect() {
      public String cardId() { return "origins-004"; }
      public void onPlay(CardInstance card, CardInstance target, LiveGameState state) {
        state.getPlayers().stream()
            .filter(player -> player.getUserId().equals(card.getOwnerId()))
            .findFirst()
            .ifPresent(player -> player.setAvailableEnergy(player.getAvailableEnergy() + 1));
      }
    });
    register(new CardEffect() {
      public String cardId() { return "origins-005"; }
      public void onTurnStart(CardInstance card, LiveGameState state) {
        if (card.getZone() != ZoneName.BATTLEFIELD) return;
        state.getCards().stream()
            .filter(c -> c.getOwnerId().equals(card.getOwnerId()) && c.getZone() == ZoneName.BASE)
            .forEach(c -> c.setHasSummoningSickness(false));
      }
    });
  }

  public void register(CardEffect effect) { effects.put(effect.cardId(), effect); }
  public Optional<CardEffect> getEffect(String cardId) { return Optional.ofNullable(effects.get(cardId)); }
}
