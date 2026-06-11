package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class TankHandler implements KeywordHandler {
  public String keyword() { return "TANK"; }
}
