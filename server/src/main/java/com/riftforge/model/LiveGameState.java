package com.riftforge.model;

import java.util.ArrayList;
import java.util.List;

public class LiveGameState {
  private String roomCode;
  private Phase currentPhase;
  private String activePlayerId;
  private int turnNumber;
  private List<CardInstance> cards = new ArrayList<>();
  private List<PlayerState> players = new ArrayList<>();
  private List<RuneState> runes = new ArrayList<>();
  private List<LogEntry> log = new ArrayList<>();
  private String updatedAt;
  private String winnerId;
  private List<String> declaredAttackers = new ArrayList<>();
  private java.util.Map<String, String> blockerToAttacker = new java.util.HashMap<>();

  public record LogEntry(String id, String timestamp, String userId, String text) {}

  public String getRoomCode() { return roomCode; }
  public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
  public Phase getCurrentPhase() { return currentPhase; }
  public void setCurrentPhase(Phase currentPhase) { this.currentPhase = currentPhase; }
  public String getActivePlayerId() { return activePlayerId; }
  public void setActivePlayerId(String activePlayerId) { this.activePlayerId = activePlayerId; }
  public int getTurnNumber() { return turnNumber; }
  public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
  public List<CardInstance> getCards() { return cards; }
  public void setCards(List<CardInstance> cards) { this.cards = cards; }
  public List<PlayerState> getPlayers() { return players; }
  public void setPlayers(List<PlayerState> players) { this.players = players; }
  public List<RuneState> getRunes() { return runes; }
  public void setRunes(List<RuneState> runes) { this.runes = runes; }
  public List<LogEntry> getLog() { return log; }
  public void setLog(List<LogEntry> log) { this.log = log; }
  public String getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
  public String getWinnerId() { return winnerId; }
  public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
  public List<String> getDeclaredAttackers() { return declaredAttackers; }
  public void setDeclaredAttackers(List<String> declaredAttackers) { this.declaredAttackers = declaredAttackers; }
  public java.util.Map<String, String> getBlockerToAttacker() { return blockerToAttacker; }
  public void setBlockerToAttacker(java.util.Map<String, String> blockerToAttacker) { this.blockerToAttacker = blockerToAttacker; }
}
