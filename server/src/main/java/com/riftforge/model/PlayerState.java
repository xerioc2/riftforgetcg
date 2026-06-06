package com.riftforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class PlayerState {
  private String userId;
  private String name;
  private int score;
  private int availableEnergy;
  @JsonIgnore private List<String> deckPool = new ArrayList<>();

  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getScore() { return score; }
  public void setScore(int score) { this.score = score; }
  public int getAvailableEnergy() { return availableEnergy; }
  public void setAvailableEnergy(int availableEnergy) { this.availableEnergy = availableEnergy; }
  public List<String> getDeckPool() { return deckPool; }
  public void setDeckPool(List<String> deckPool) { this.deckPool = deckPool; }
}
