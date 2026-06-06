package com.riftforge.effect;

import com.riftforge.model.ZoneName;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CardEffectRegistry {
  private final Map<String, CardEffect> effects = new HashMap<>();

  @PostConstruct
  void init() {
    register(new CardEffect() {
      public String cardId() { return "origins-001"; }
      public void onPlay(com.riftforge.model.CardInstance card, com.riftforge.model.LiveGameState state) {
        state.getCards().stream()
            .filter(c -> c.getOwnerId().equals(card.getOwnerId()) && c.getZone() == ZoneName.BATTLEFIELD && !c.getInstanceId().equals(card.getInstanceId()))
            .forEach(c -> c.getTempKeywords().add("TOUGH"));
      }
    });
    register(stub("origins-002")); // Stub: agile champion with Rush support.
    register(stub("origins-003")); // Stub: overwhelm finisher.
    register(stub("origins-004")); // Stub: rune acceleration effect.
    register(stub("origins-005")); // Stub: battlefield capture payoff.
  }

  public void register(CardEffect effect) { effects.put(effect.cardId(), effect); }
  public Optional<CardEffect> getEffect(String cardId) { return Optional.ofNullable(effects.get(cardId)); }
  private CardEffect stub(String id) { return () -> id; }
}
