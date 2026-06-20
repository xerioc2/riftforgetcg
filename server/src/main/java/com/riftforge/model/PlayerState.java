package com.riftforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class PlayerState {
  private String userId;
  private String name;
  private int score;
  private int availableEnergy;
  private int showdownOnlyEnergy;
  private int runePoolRemaining;
  @JsonIgnore private List<String> deckPool = new ArrayList<>();
  @JsonIgnore private List<String> runeDeckPool = new ArrayList<>();
  @JsonIgnore private List<String> selectedBattlefields = new ArrayList<>();
  private List<String> battlefieldChoices = new ArrayList<>();
  private String selectedBattlefieldId;

  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getScore() { return score; }
  public void setScore(int score) { this.score = score; }
  public int getAvailableEnergy() { return availableEnergy; }
  public void setAvailableEnergy(int availableEnergy) { this.availableEnergy = availableEnergy; }
  public int getShowdownOnlyEnergy() { return showdownOnlyEnergy; }
  public void setShowdownOnlyEnergy(int showdownOnlyEnergy) { this.showdownOnlyEnergy = showdownOnlyEnergy; }
  public int getRunePoolRemaining() { return runePoolRemaining; }
  public void setRunePoolRemaining(int runePoolRemaining) { this.runePoolRemaining = runePoolRemaining; }
  @JsonIgnore
  public List<String> getDeckPool() { return deckPool; }
  public void setDeckPool(List<String> deckPool) { this.deckPool = deckPool; }
  @JsonIgnore
  public List<String> getRuneDeckPool() { return runeDeckPool; }
  public void setRuneDeckPool(List<String> runeDeckPool) { this.runeDeckPool = runeDeckPool; }
  @JsonIgnore
  public List<String> getSelectedBattlefields() { return selectedBattlefields; }
  public void setSelectedBattlefields(List<String> selectedBattlefields) { this.selectedBattlefields = selectedBattlefields; }
  public List<String> getBattlefieldChoices() { return battlefieldChoices; }
  public void setBattlefieldChoices(List<String> battlefieldChoices) { this.battlefieldChoices = battlefieldChoices == null ? new ArrayList<>() : battlefieldChoices; }
  public String getSelectedBattlefieldId() { return selectedBattlefieldId; }
  public void setSelectedBattlefieldId(String selectedBattlefieldId) { this.selectedBattlefieldId = selectedBattlefieldId; }
  @JsonProperty("deckCount")
  public int getDeckCount() { return deckPool == null ? 0 : deckPool.size(); }
}
