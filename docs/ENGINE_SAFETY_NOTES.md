# Engine Safety Notes

RiftForge currently mutates the authoritative `LiveGameState` object in place while a
room lock is held. This keeps the alpha engine simple, but it means move handlers must
avoid throw-after-partial-mutation bugs: if a handler mutates live state and then throws,
`GameService` may keep a reference to the partially changed object even though the move
was rejected.

## Mutation Policy

1. Validate all player-controlled preconditions before mutating state.
2. Treat mutation as a commit phase. Once mutation starts, the code should not throw for
   expected validation failures.
3. If a mutation phase can still fail because state may become stale, work on local
   copies or restore the changed data before throwing.
4. Keep `RulesValidator` as the first line of defense for normal illegal moves.
5. Keep server-side enforcement authoritative; client `legalActions` are advisory.
6. Future combat, showdown, trigger, prompt, and payment work must add regression tests
   proving failed moves do not partially mutate live state.

## Current Boundaries

- `GameService.processMove` serializes moves per room and calls `GameEngine.applyMove`
  while holding the room lock.
- `RulesValidator.validate` runs before the engine handler and should reject expected
  illegal moves before mutation.
- `GameEngine` handlers mutate the existing state object, not a defensive copy.
- Match history snapshots are captured while the room lock is held, then recorded after
  unlock.

## Known Risk Areas To Watch

- Payment helpers tap or recycle runes and adjust available energy.
- Champion deployment pays before moving the Champion.
- Equipment attach/detach mutates both gear and host-adjacent state.
- Pending choice resolution can inspect and reorder private deck pools.
- Top-deck choice resolution temporarily removes looked-at cards and must restore them
  before throwing on stale selections.
- Showdown/combat resolution will soon mutate damage, zones, score, controller, and
  trigger state in one operation.
- Trigger handlers can chain additional draws, token creation, movement, or pending
  choices after a primary move.

## Test Expectations

When adding or changing a handler, include at least one failed-move regression for the
most likely validation failure. The test should assert the important state did not
change: zones, attachments, deck order, energy, runes, pending choices, showdown step,
score, and public/private logs as applicable.

