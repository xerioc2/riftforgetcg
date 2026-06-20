package com.riftforge.model;

public class ReplacementEffect {
  private String replacementEffectId;
  private String effectType;
  private String sourceInstanceId;
  private String protectedInstanceId;
  private String controllerId;
  private int createdSequence;
  private boolean consumed;

  public static final String WOULD_DIE = "WOULD_DIE";
  public static final String WOULD_DIE_DESTROY_SOURCE_INSTEAD = "WOULD_DIE_DESTROY_SOURCE_INSTEAD";

  public String getReplacementEffectId() { return replacementEffectId; }
  public void setReplacementEffectId(String replacementEffectId) { this.replacementEffectId = replacementEffectId; }
  public String getEffectType() { return effectType; }
  public void setEffectType(String effectType) { this.effectType = effectType; }
  public String getSourceInstanceId() { return sourceInstanceId; }
  public void setSourceInstanceId(String sourceInstanceId) { this.sourceInstanceId = sourceInstanceId; }
  public String getProtectedInstanceId() { return protectedInstanceId; }
  public void setProtectedInstanceId(String protectedInstanceId) { this.protectedInstanceId = protectedInstanceId; }
  public String getControllerId() { return controllerId; }
  public void setControllerId(String controllerId) { this.controllerId = controllerId; }
  public int getCreatedSequence() { return createdSequence; }
  public void setCreatedSequence(int createdSequence) { this.createdSequence = createdSequence; }
  public boolean isConsumed() { return consumed; }
  public void setConsumed(boolean consumed) { this.consumed = consumed; }
}
