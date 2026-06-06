package com.riftforge.engine.keyword;

import com.riftforge.model.CardInstance;
import org.springframework.stereotype.Component;

@Component
public class ToughHandler implements KeywordHandler {
  public String keyword() { return "TOUGH"; }
  public void modifyDamageTaken(CardInstance defender, CardInstance attacker, int[] damage) { damage[0] = Math.max(0, damage[0] - 1); }
}
