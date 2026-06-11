package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class ShieldHandler implements KeywordHandler {
  public String keyword() { return "SHIELD"; }
}
