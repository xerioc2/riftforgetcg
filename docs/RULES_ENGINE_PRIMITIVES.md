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

Privacy and projection rules:

- Legal activation is exposed only through the owning/controller player's legal actions.
- Hidden or face-down sources cannot activate the current v1 ability.
- Opponents and spectators do not receive private legal-action information.
- Debug info must remain safe and must not include hidden hand/deck/rune contents.

Deferred:

- Replacement/prevention effects, including Zhonya's Hourglass.
- Ability-chain / reactable activated abilities.
- Full official priority windows for activated abilities.
- Generic rules-text parsing for arbitrary activated abilities.
- Hidden Reaction-for-0 activated ability timing.
