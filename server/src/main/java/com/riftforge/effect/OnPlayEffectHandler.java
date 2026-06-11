package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface OnPlayEffectHandler extends EffectHandler {
  @Override
  default EffectCategory category() {
    return EffectCategory.ON_PLAY;
  }

  void onPlay(CardInstance card, CardInstance target, LiveGameState state);
}
