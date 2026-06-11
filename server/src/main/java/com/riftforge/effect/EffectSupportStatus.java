package com.riftforge.effect;

public record EffectSupportStatus(boolean implemented, String reason) {
  public static EffectSupportStatus supported() {
    return new EffectSupportStatus(true, null);
  }

  public static EffectSupportStatus unsupported(String reason) {
    return new EffectSupportStatus(false, reason);
  }
}
