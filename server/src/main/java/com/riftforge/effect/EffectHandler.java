package com.riftforge.effect;

public interface EffectHandler {
  String id();
  EffectCategory category();

  default EffectSupportStatus supportStatus() {
    return EffectSupportStatus.supported();
  }
}
