package com.riftforge.engine.keyword;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import org.springframework.stereotype.Component;

@Component
public class RushHandler implements KeywordHandler {
  public String keyword() { return "RUSH"; }
  public void onEnterPlay(CardInstance card, LiveGameState state) { card.setHasSummoningSickness(false); }
}
