package com.riftforge.model;

import java.util.ArrayList;
import java.util.List;

public class RoomState {
  private String code;
  private String hostId;
  private List<LobbyPlayer> players = new ArrayList<>();
  private String status = "waiting";
  private boolean botEnabled;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getHostId() { return hostId; }
  public void setHostId(String hostId) { this.hostId = hostId; }
  public List<LobbyPlayer> getPlayers() { return players; }
  public void setPlayers(List<LobbyPlayer> players) { this.players = players; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public boolean isBotEnabled() { return botEnabled; }
  public void setBotEnabled(boolean botEnabled) { this.botEnabled = botEnabled; }
}
