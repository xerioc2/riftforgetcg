package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface StaticModifierHandler extends EffectHandler {
  @Override
  default EffectCategory category() {
    return EffectCategory.STATIC_MODIFIER;
  }

  void apply(CardInstance card, LiveGameState state);
}
