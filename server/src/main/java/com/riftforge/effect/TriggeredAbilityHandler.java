package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface TriggeredAbilityHandler extends EffectHandler {
  @Override
  default EffectCategory category() {
    return EffectCategory.TRIGGERED_ABILITY;
  }

  void onTrigger(CardInstance card, LiveGameState state);
}
