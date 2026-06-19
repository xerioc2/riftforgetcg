# Rules Engine Primitives

This document tracks reusable rules-engine building blocks. It is intentionally narrower than card support status: a primitive can exist before every card that needs it is fully supported.

## Activated Ability Framework v1

Status: Partial foundation.

RiftForge has a small server-authoritative activated ability model:

- `ActivateAbilityMove` carries the activating player, source card instance, optional `abilityKey`, target instance, and selected payment runes.
- `ActivatedAbilityService` owns exact-card ability definitions, source legality, target legality, timing, and payment validation.
- `RulesValidator`, `GameEngine`, and `LegalActionsService` share that service instead of hardcoding each activated ability separately.
- v1 supports immediate Main Phase activated abilities only when explicitly registered by exact card/text.

Current production ability:

- `THE_SYREN_RECALL`: The Syren may be activated from Base during its controller's Main Phase by paying 1 energy and exhausting it to move a friendly public battlefield Unit/Champion to Base.
- `ZHONYAS_HOURGLASS_PROTECT`: Zhonya's Hourglass may be activated from Base during its controller's Main Phase for 0 energy to protect a friendly public Unit/Champion in Base or at a battlefield from the next supported death.

Privacy and projection rules:

- Legal activation is exposed only through the owning/controller player's legal actions.
- Hidden or face-down sources cannot activate the current v1 ability.
- Opponents and spectators do not receive private legal-action information.
- Debug info must remain safe and must not include hidden hand/deck/rune contents.

Deferred:

- Ability-chain / reactable activated abilities.
- Full official priority windows for activated abilities.
- Generic rules-text parsing for arbitrary activated abilities.
- Hidden Reaction-for-0 activated ability timing.

## Would-Die Replacement Hook v1

Status: Partial foundation.

RiftForge has a small server-authoritative death/replacement primitive for
explicit replacement cards:

- `DeathService` is the shared path for real Unit/Champion death cleanup from
  combat damage and explicit destroy effects.
- `ReplacementEffectService` owns registered replacement effects before the
  death event is captured or Deathknell is processed.
- `ReplacementEffect` currently supports one narrow behavior:
  `WOULD_DIE_DESTROY_SOURCE_INSTEAD`.
- Registrations are explicit server state, not generic card-text parsing.
- Zhonya's Hourglass registers the first production v1 replacement through its
  narrow activated-ability alpha path.

Current v1 behavior:

- If the protected public Unit/Champion would die through a routed death path,
  the replacement source is destroyed instead.
- The protected card is healed, exhausted, and recalled to Base for the
  current Zhonya v1 result.
- The replacement is consumed after one use.
- If the replacement source is missing, invalid, face-down, or no longer in a
  public play zone, the protected card dies normally.
- If multiple matching replacements exist, the oldest registration is used
  deterministically. Player choice among competing replacements is deferred.

Routed paths:

- Combat damage destruction.
- Explicit public board-card destroy cleanup.

Not routed in v1:

- Spell/action source cleanup to Trash.
- Discard, mill, or ordinary non-death zone movement.
- Gear destroyed directly by Disarming Rake or similar non-unit effects.
- Return-to-hand, recall, reposition, or Champion-zone return.

Deferred:

- Player choice among multiple replacement effects.
- Damage prevention/reduction layers.
- Generic "instead" parsing.
- Hidden Reaction-for-0 timing.
- Ability-chain or full official priority integration.
