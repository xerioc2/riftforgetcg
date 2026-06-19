package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EquipmentStatModifierRegistry {
  public record StatModifier(int mightBonus, int maxHealthBonus) {
    public boolean isEmpty() {
      return mightBonus == 0 && maxHealthBonus == 0;
    }
  }

  private final Map<String, StatModifier> modifiersByCardId;

  public EquipmentStatModifierRegistry() {
    this(Map.of());
  }

  public EquipmentStatModifierRegistry(Map<String, StatModifier> modifiersByCardId) {
    this.modifiersByCardId = Map.copyOf(modifiersByCardId);
  }

  public StatModifier modifierFor(CardDefinition gear) {
    if (gear == null) return new StatModifier(0, 0);
    return modifiersByCardId.getOrDefault(gear.id(), new StatModifier(0, 0));
  }
}
