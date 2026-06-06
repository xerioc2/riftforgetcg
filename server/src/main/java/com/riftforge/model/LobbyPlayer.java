package com.riftforge.model;

import java.util.ArrayList;
import java.util.List;

public class LobbyPlayer {
  private String id;
  private String name;
  private boolean ready;
  private List<String> deckCardIds = new ArrayList<>();

  public LobbyPlayer() {}

  public LobbyPlayer(String id, String name, boolean ready, List<String> deckCardIds) {
    this.id = id;
    this.name = name;
    this.ready = ready;
    this.deckCardIds = new ArrayList<>(deckCardIds);
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public boolean isReady() { return ready; }
  public void setReady(boolean ready) { this.ready = ready; }
  public List<String> getDeckCardIds() { return deckCardIds; }
  public void setDeckCardIds(List<String> deckCardIds) { this.deckCardIds = deckCardIds; }
}
