package com.riftforge.engine.keyword;

import com.riftforge.effect.EffectCategory;
import com.riftforge.effect.EffectHandler;
import com.riftforge.effect.EffectSupportStatus;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface KeywordHandler extends EffectHandler {
  String keyword();

  @Override
  default String id() { return keyword(); }

  @Override
  default EffectCategory category() { return EffectCategory.KEYWORD; }

  @Override
  default EffectSupportStatus supportStatus() { return EffectSupportStatus.supported(); }
  default void onEnterPlay(CardInstance card, LiveGameState state) {}
  default void modifyDamageDealt(CardInstance attacker, CardInstance defender, int[] damage) {}
  default void modifyDamageTaken(CardInstance defender, CardInstance attacker, int[] damage) {}
}
