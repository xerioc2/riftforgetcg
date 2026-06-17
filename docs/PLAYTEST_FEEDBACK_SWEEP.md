# Playtester Feedback Sweep

This file tracks concrete external alpha feedback before it becomes bug-fix
work. Do not add speculative issues here; every entry should come from tester
debug info, a screenshot/video, or a locally reproduced report.

## Intake Checklist

For every report, capture:

- Build/release version and client build tag.
- Server build SHA/date from Copy Debug Info.
- Room code.
- Human vs human or human vs RiftBot.
- Card(s) involved.
- Phase, chain, showdown, combat, or pending-choice state.
- Expected behavior.
- Actual behavior.
- Screenshot/video if available.
- Whether supported-cards-only mode was enabled.

## Priority Order

1. Privacy/security leak.
2. Freeze, crash, bot stall, or client soft-lock.
3. Wrong game-state mutation.
4. Wrong support status or ready-gate behavior.
5. UI clarity issue.
6. Documentation/support badge mismatch.

## Current Sweep

| Priority | Issue | Category | Evidence | Fix | Regression coverage | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P5 | Disarming Rake frontend badge could show Partial while backend/docs mark it Supported. | Docs/support badge mismatch | `docs/REACTION_EQUIPMENT_AUDIT.md` recorded backend/docs Supported and frontend Partial. | Added Disarming Rake to the frontend supported-card map and updated the audit row. | `deckSupport` frontend test verifies Disarming Rake is Supported. | Fixed |
| P5 | Lonely Poro was backend/docs Supported but absent from the frontend supported-card map. | Docs/support badge mismatch | Backend `CardSupportService` and `SUPPORTED_CARDS.md` mark Lonely Poro Supported. | Added Lonely Poro to the frontend supported-card map. | `deckSupport` frontend test verifies Lonely Poro is Supported. | Fixed |
| P5 | Scuttle Crab used generic frontend Partial text instead of the backend/docs reason. | UI clarity | Backend support reason explains implemented hand reveal and deferred XP/facedown viewing. | Added a Scuttle Crab-specific frontend Partial reason. | `deckSupport` frontend test verifies the specific reason. | Fixed |

## No Concrete Reports Supplied

This sweep did not include external debug payloads, room codes, screenshots, or
videos. No privacy, freeze, bot-stall, or wrong-rules bug was changed without a
repo-backed reproduction.
