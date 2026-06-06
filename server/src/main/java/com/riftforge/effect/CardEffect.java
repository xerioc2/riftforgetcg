package com.riftforge.effect;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;

public interface CardEffect {
  String cardId();
  default void onPlay(CardInstance card, LiveGameState state) {}
  default void onDestroy(CardInstance card, LiveGameState state) {}
  default void onAttack(CardInstance card, LiveGameState state) {}
  default void onTurnStart(CardInstance card, LiveGameState state) {}
}
