package com.riftforge.rules;

import com.riftforge.model.CardDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EquipmentRules {
  private static final Pattern EQUIP_HEADER = Pattern.compile("(?i)\\[equip\\]\\s*([^\\(\\[]*)");
  private static final Pattern ENERGY_ICON = Pattern.compile("(?i):rb_energy_(\\d+):");
  private static final Pattern RUNE_ICON = Pattern.compile("(?i):rb_rune_([a-z_]+):");

  private EquipmentRules() {}

  public record EquipCost(int energyCost, List<String> premiumDomains) {
    public EquipCost {
      energyCost = Math.max(0, energyCost);
      premiumDomains = premiumDomains == null ? List.of() : List.copyOf(premiumDomains);
    }
  }

  public static EquipCost equipCost(CardDefinition def) {
    if (def == null || def.rulesText() == null) return new EquipCost(0, List.of());
    Matcher header = EQUIP_HEADER.matcher(def.rulesText());
    if (!header.find()) return new EquipCost(0, List.of());
    String costText = header.group(1) == null ? "" : header.group(1);
    int energyCost = 0;
    Matcher energy = ENERGY_ICON.matcher(costText);
    while (energy.find()) {
      energyCost += Integer.parseInt(energy.group(1));
    }
    List<String> premiumDomains = new ArrayList<>();
    Matcher rune = RUNE_ICON.matcher(costText);
    while (rune.find()) {
      premiumDomains.add(normalizeDomain(rune.group(1)));
    }
    return new EquipCost(energyCost, premiumDomains);
  }

  public static String normalizeDomain(String domain) {
    if (domain == null) return "";
    return domain.trim().replace('-', '_').toUpperCase(Locale.ROOT);
  }
}
