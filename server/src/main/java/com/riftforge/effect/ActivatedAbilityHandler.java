package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface ActivatedAbilityHandler extends EffectHandler {
  @Override
  default EffectCategory category() {
    return EffectCategory.ACTIVATED_ABILITY;
  }

  void activate(CardInstance card, LiveGameState state);
}
