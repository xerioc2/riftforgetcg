package com.riftforge.engine.keyword;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface KeywordHandler {
  String keyword();
  default void onEnterPlay(CardInstance card, LiveGameState state) {}
  default void modifyDamageDealt(CardInstance attacker, CardInstance defender, int[] damage) {}
  default void modifyDamageTaken(CardInstance defender, CardInstance attacker, int[] damage) {}
}
