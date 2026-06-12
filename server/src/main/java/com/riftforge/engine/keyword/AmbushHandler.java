package com.riftforge.engine.keyword;

import org.springframework.stereotype.Component;

@Component
public class AmbushHandler implements KeywordHandler {
  @Override
  public String keyword() {
    return "AMBUSH";
  }
}
