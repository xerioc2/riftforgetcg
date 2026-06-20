package com.riftforge.engine;

import java.util.HashSet;
import java.util.Set;

public class DeathResolutionContext {
  private final Set<String> replacedThisPass = new HashSet<>();

  public boolean wasReplacedThisPass(String instanceId) {
    return instanceId != null && replacedThisPass.contains(instanceId);
  }

  public void markReplaced(String instanceId) {
    if (instanceId != null && !instanceId.isBlank()) replacedThisPass.add(instanceId);
  }
}
