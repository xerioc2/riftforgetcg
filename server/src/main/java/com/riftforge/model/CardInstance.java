package com.riftforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class CardInstance {
  public static final String DEFAULT_BATTLEFIELD_LOCATION_ID = "bf-0";

  private String instanceId;
  private String cardId;
  private String ownerId;
  private ZoneName zone;
  private String battlefieldLocationId;
  private int x;
  private int y;
  private int zIndex;
  private boolean tapped;
  private boolean faceDown;
  private int currentHealth;
  private int mightBonus;
  private int temporaryPowerModifier;
  private Integer printedMight;
  private Integer printedHealth;
  private Integer effectiveMight;
  private Integer effectiveMaxHealth;
  private Integer markedDamage;
  private List<String> statModifierLabels = new ArrayList<>();
  private String attachedToInstanceId;
  private boolean hasSummoningSickness;
  @JsonIgnore private List<String> tempKeywords = new ArrayList<>();

  public CardInstance() {}

  public CardInstance(CardInstance other) {
    this.instanceId = other.instanceId;
    this.cardId = other.cardId;
    this.ownerId = other.ownerId;
    this.zone = other.zone;
    this.battlefieldLocationId = other.battlefieldLocationId;
    this.x = other.x;
    this.y = other.y;
    this.zIndex = other.zIndex;
    this.tapped = other.tapped;
    this.faceDown = other.faceDown;
    this.currentHealth = other.currentHealth;
    this.mightBonus = other.mightBonus;
    this.temporaryPowerModifier = other.temporaryPowerModifier;
    this.printedMight = other.printedMight;
    this.printedHealth = other.printedHealth;
    this.effectiveMight = other.effectiveMight;
    this.effectiveMaxHealth = other.effectiveMaxHealth;
    this.markedDamage = other.markedDamage;
    this.statModifierLabels = other.statModifierLabels == null ? new ArrayList<>() : new ArrayList<>(other.statModifierLabels);
    this.attachedToInstanceId = other.attachedToInstanceId;
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
  public void setZone(ZoneName zone) {
    this.zone = zone;
    if (zone == ZoneName.BATTLEFIELD && !faceDown) {
      if (battlefieldLocationId == null || battlefieldLocationId.isBlank()) {
        battlefieldLocationId = DEFAULT_BATTLEFIELD_LOCATION_ID;
      }
    } else {
      battlefieldLocationId = null;
    }
  }
  public String getBattlefieldLocationId() {
    if (zone == ZoneName.BATTLEFIELD && !faceDown) {
      return battlefieldLocationId == null || battlefieldLocationId.isBlank()
          ? DEFAULT_BATTLEFIELD_LOCATION_ID
          : battlefieldLocationId;
    }
    return null;
  }
  public void setBattlefieldLocationId(String battlefieldLocationId) {
    this.battlefieldLocationId = battlefieldLocationId;
    if (zone == ZoneName.BATTLEFIELD && (this.battlefieldLocationId == null || this.battlefieldLocationId.isBlank())) {
      this.battlefieldLocationId = DEFAULT_BATTLEFIELD_LOCATION_ID;
    }
  }
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
  public int getMightBonus() { return mightBonus; }
  public void setMightBonus(int mightBonus) { this.mightBonus = mightBonus; }
  public int getTemporaryPowerModifier() { return temporaryPowerModifier; }
  public void setTemporaryPowerModifier(int temporaryPowerModifier) { this.temporaryPowerModifier = temporaryPowerModifier; }
  public Integer getPrintedMight() { return printedMight; }
  public void setPrintedMight(Integer printedMight) { this.printedMight = printedMight; }
  public Integer getPrintedHealth() { return printedHealth; }
  public void setPrintedHealth(Integer printedHealth) { this.printedHealth = printedHealth; }
  public Integer getEffectiveMight() { return effectiveMight; }
  public void setEffectiveMight(Integer effectiveMight) { this.effectiveMight = effectiveMight; }
  public Integer getEffectiveMaxHealth() { return effectiveMaxHealth; }
  public void setEffectiveMaxHealth(Integer effectiveMaxHealth) { this.effectiveMaxHealth = effectiveMaxHealth; }
  public Integer getMarkedDamage() { return markedDamage; }
  public void setMarkedDamage(Integer markedDamage) { this.markedDamage = markedDamage; }
  public List<String> getStatModifierLabels() { return statModifierLabels; }
  public void setStatModifierLabels(List<String> statModifierLabels) {
    this.statModifierLabels = statModifierLabels == null ? new ArrayList<>() : statModifierLabels;
  }
  public void clearProjectedStats() {
    printedMight = null;
    printedHealth = null;
    effectiveMight = null;
    effectiveMaxHealth = null;
    markedDamage = null;
    statModifierLabels = new ArrayList<>();
  }
  public String getAttachedToInstanceId() { return attachedToInstanceId; }
  public void setAttachedToInstanceId(String attachedToInstanceId) { this.attachedToInstanceId = attachedToInstanceId; }
  public boolean isHasSummoningSickness() { return hasSummoningSickness; }
  public void setHasSummoningSickness(boolean hasSummoningSickness) { this.hasSummoningSickness = hasSummoningSickness; }
  public List<String> getTempKeywords() { return tempKeywords; }
  public void setTempKeywords(List<String> tempKeywords) { this.tempKeywords = tempKeywords; }
}
