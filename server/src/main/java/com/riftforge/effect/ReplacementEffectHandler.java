package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface ReplacementEffectHandler extends EffectHandler {
  @Override
  default EffectCategory category() {
    return EffectCategory.REPLACEMENT_EFFECT;
  }

  boolean replace(CardInstance card, LiveGameState state);
}
