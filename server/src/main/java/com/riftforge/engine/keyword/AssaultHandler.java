package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class AssaultHandler implements KeywordHandler {
  public String keyword() { return "ASSAULT"; }
}
