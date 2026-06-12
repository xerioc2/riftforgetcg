package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class HiddenHandler implements KeywordHandler {
  @Override
  public String keyword() {
    return "HIDDEN";
  }
}
