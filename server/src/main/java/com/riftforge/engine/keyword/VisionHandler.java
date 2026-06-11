package com.riftforge.engine.keyword;

import com.riftforge.effect.EffectCategory;
import org.springframework.stereotype.Component;

@Component
public class VisionHandler implements KeywordHandler {
  public String keyword() { return "VISION"; }
  public EffectCategory category() { return EffectCategory.ON_PLAY; }
}
