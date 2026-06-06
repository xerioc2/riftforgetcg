package com.riftforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class CardInstance {
  private String instanceId;
  private String cardId;
  private String ownerId;
  private ZoneName zone;
  private int x;
  private int y;
  private int zIndex;
  private boolean tapped;
  private boolean faceDown;
  @JsonIgnore private int currentHealth;
  @JsonIgnore private boolean hasSummoningSickness;
  @JsonIgnore private List<String> tempKeywords = new ArrayList<>();

  public CardInstance() {}

  public CardInstance(CardInstance other) {
    this.instanceId = other.instanceId;
    this.cardId = other.cardId;
    this.ownerId = other.ownerId;
    this.zone = other.zone;
    this.x = other.x;
    this.y = other.y;
    this.zIndex = other.zIndex;
    this.tapped = other.tapped;
    this.faceDown = other.faceDown;
    this.currentHealth = other.currentHealth;
    this.hasSummoningSickness = other.hasSummoningSickness;
    this.tempKeywords = new ArrayList<>(other.tempKeywords);
  }

  public String getInstanceId() { return instanceId; }
  public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
  public String getCardId() { return cardId; }
  public void setCardId(String cardId) { this.cardId = cardId; }
  public String getOwnerId() { return ownerId; }
  public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
  public ZoneName getZone() { return zone; }
  public void setZone(ZoneName zone) { this.zone = zone; }
  public int getX() { return x; }
  public void setX(int x) { this.x = x; }
  public int getY() { return y; }
  public void setY(int y) { this.y = y; }
  public int getZIndex() { return zIndex; }
  public void setZIndex(int zIndex) { this.zIndex = zIndex; }
  public boolean isTapped() { return tapped; }
  public void setTapped(boolean tapped) { this.tapped = tapped; }
  public boolean isFaceDown() { return faceDown; }
  public void setFaceDown(boolean faceDown) { this.faceDown = faceDown; }
  public int getCurrentHealth() { return currentHealth; }
  public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
  public boolean isHasSummoningSickness() { return hasSummoningSickness; }
  public void setHasSummoningSickness(boolean hasSummoningSickness) { this.hasSummoningSickness = hasSummoningSickness; }
  public List<String> getTempKeywords() { return tempKeywords; }
  public void setTempKeywords(List<String> tempKeywords) { this.tempKeywords = tempKeywords; }
}
