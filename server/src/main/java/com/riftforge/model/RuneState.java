package com.riftforge.model;

public class RuneState {
  private String instanceId;
  private String cardId;
  private String ownerId;
  private boolean tapped;
  private int normalEnergy;
  private int premiumEnergy;

  public String getInstanceId() { return instanceId; }
  public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
  public String getCardId() { return cardId; }
  public void setCardId(String cardId) { this.cardId = cardId; }
  public String getOwnerId() { return ownerId; }
  public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
  public boolean isTapped() { return tapped; }
  public void setTapped(boolean tapped) { this.tapped = tapped; }
  public int getNormalEnergy() { return normalEnergy; }
  public void setNormalEnergy(int normalEnergy) { this.normalEnergy = normalEnergy; }
  public int getPremiumEnergy() { return premiumEnergy; }
  public void setPremiumEnergy(int premiumEnergy) { this.premiumEnergy = premiumEnergy; }
}
