package com.riftforge.engine;

import com.riftforge.model.ZoneName;

public record ActivatedAbilityDefinition(
    String abilityKey,
    String label,
    ZoneName sourceZone,
    int energyCost,
    int premiumCost,
    boolean requiresExhaust,
    ActivatedAbilityTiming timing,
    ActivatedAbilityTargetKind targetKind,
    boolean reactable,
    String publicDescription
) {}
