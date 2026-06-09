package com.riftforge.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LiveGameState {
  private String roomCode;
  private Phase currentPhase;
  private String activePlayerId;
  private String firstPlayerId;
  private int turnNumber;
  private List<CardInstance> cards = new ArrayList<>();
  private List<PlayerState> players = new ArrayList<>();
  private List<RuneState> runes = new ArrayList<>();
  private List<LogEntry> log = new ArrayList<>();
  private String updatedAt;
  private String winnerId;
  private Set<String> mulligansDone = new HashSet<>();
  private boolean cardPlayedThisTurn;
  private Map<String, String> battlefieldController = new HashMap<>();
  private Set<String> scoredBattlefieldsThisTurn = new HashSet<>();
  private List<RevealedHandSnapshot> revealedHands = new ArrayList<>();
  private ShowdownState activeShowdown;
  private GameMode gameMode = GameMode.ENFORCED;

  public record LogEntry(String id, String timestamp, String userId, String text) {}
  public record ShowdownState(String attackingPlayerId, List<String> attackerInstanceIds, Map<String, Integer> gankingBonuses) {}

  public String getRoomCode() { return roomCode; }
  public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
  public Phase getCurrentPhase() { return currentPhase; }
  public void setCurrentPhase(Phase currentPhase) { this.currentPhase = currentPhase; }
  public String getActivePlayerId() { return activePlayerId; }
  public void setActivePlayerId(String activePlayerId) { this.activePlayerId = activePlayerId; }
  public String getFirstPlayerId() { return firstPlayerId; }
  public void setFirstPlayerId(String firstPlayerId) { this.firstPlayerId = firstPlayerId; }
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
  public Set<String> getMulligansDone() { return mulligansDone; }
  public void setMulligansDone(Set<String> mulligansDone) { this.mulligansDone = mulligansDone; }
  public boolean isCardPlayedThisTurn() { return cardPlayedThisTurn; }
  public void setCardPlayedThisTurn(boolean cardPlayedThisTurn) { this.cardPlayedThisTurn = cardPlayedThisTurn; }
  public Map<String, String> getBattlefieldController() { return battlefieldController; }
  public void setBattlefieldController(Map<String, String> battlefieldController) { this.battlefieldController = battlefieldController; }
  public Set<String> getScoredBattlefieldsThisTurn() { return scoredBattlefieldsThisTurn; }
  public void setScoredBattlefieldsThisTurn(Set<String> scoredBattlefieldsThisTurn) { this.scoredBattlefieldsThisTurn = scoredBattlefieldsThisTurn; }
  public List<RevealedHandSnapshot> getRevealedHands() { return revealedHands; }
  public void setRevealedHands(List<RevealedHandSnapshot> revealedHands) { this.revealedHands = revealedHands; }
  public ShowdownState getActiveShowdown() { return activeShowdown; }
  public void setActiveShowdown(ShowdownState activeShowdown) { this.activeShowdown = activeShowdown; }
  public GameMode getGameMode() { return gameMode; }
  public void setGameMode(GameMode gameMode) { this.gameMode = gameMode == null ? GameMode.ENFORCED : gameMode; }
}
