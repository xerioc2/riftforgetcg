package com.riftforge.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RevealedHandSnapshot {
  private String revealedToPlayerId;
  private String revealedOwnerId;
  private List<String> instanceIds = new ArrayList<>();
  private Set<String> dismissedInstanceIds = new HashSet<>();

  public String getRevealedToPlayerId() { return revealedToPlayerId; }
  public void setRevealedToPlayerId(String revealedToPlayerId) { this.revealedToPlayerId = revealedToPlayerId; }
  public String getRevealedOwnerId() { return revealedOwnerId; }
  public void setRevealedOwnerId(String revealedOwnerId) { this.revealedOwnerId = revealedOwnerId; }
  public List<String> getInstanceIds() { return instanceIds; }
  public void setInstanceIds(List<String> instanceIds) { this.instanceIds = instanceIds; }
  public Set<String> getDismissedInstanceIds() { return dismissedInstanceIds; }
  public void setDismissedInstanceIds(Set<String> dismissedInstanceIds) { this.dismissedInstanceIds = dismissedInstanceIds; }
}
