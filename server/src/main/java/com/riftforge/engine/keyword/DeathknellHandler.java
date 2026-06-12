package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class DeathknellHandler implements KeywordHandler {
  public String keyword() { return "DEATHKNELL"; }
}
