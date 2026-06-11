# RiftForge Rules Roadmap

Last updated: 2026-06-10

This roadmap tracks rules-engine work for the alpha playtest client. It does not
claim official rules authority; Riot/UVS rules documents, card text, banlists,
errata, and tournament policy remain the source of truth.

## Completed / Partial-Completed

- Constructed deck setup partitioning: Legend, Champion, main deck, runes, and
  battlefields are separated during game setup.
- Opening hand draws only from the main deck.
- Room-scoped STOMP session tokens and REST token validation protect room/game
  actions.
- Player-specific hidden-information projection masks opponent hand cards and
  keeps private Vision logs private.
- Per-room move locking serializes same-room state updates.
- Showdown is modeled as `activeShowdown` inside Main Phase.
- `LegalActionsService` is projected to player-specific state and consumed by
  the client for major action visibility.
- Spectator/public projections receive no legal actions.
- `RulesValidator` remains the final enforcement layer.
- Rune payment is atomic with `PLAY_CARD` for selected normal energy runes and
  basic premium/domain rune recycling.
- Common card-type play legality is enforced for units, spells, gear, runes,
  Champions, Legends, and Battlefields.
- Movement legality is enforced for the current single-battlefield flow, with a
  dedicated same-zone `REPOSITION_CARD` move for visual organization.
- Keyword/effect handler registry scaffold exists for keyword, on-play,
  triggered, activated, static modifier, and replacement-effect handler shapes,
  with explicit unsupported-status reporting for tracked missing handlers.
- Card support metadata and optional supported-cards-only ready gates are in
  place for alpha deck validation.
- Tournament-style deck import/export and deck validation reports are available
  in the Deck Builder.

## v0.2-alpha: Supported Deck Playtesting

Status: Partial

- LegalActionsService projection/UI integration: partial-complete.
- Online presence count: complete for lightweight unique-player presence.
- Starter/sample supported decks: partial-complete with experimental starter lists.
- Visible client errors: partial-complete with global toasts and readable REST/WebSocket failures.
- Supported-card warnings before readying: partial-complete for client-side unsupported effect warnings.
- Supported-cards-only ready gate: partial-complete for blocking unsupported or
  not-audited cards while warning on partial cards.
- Deck import/export workflow: partial-complete for common pasted constructed
  decklists, unresolved-card reporting, and full-section export.

## v0.3-alpha: Core Rules Correctness

Status: Partial

- Rune payment correctness: partial-complete for selected energy and premium/domain rune payment.
- Unit, spell, and gear play legality: partial-complete for hand play,
  spell-to-discard cleanup, and battlefield/showdown entry restrictions.
- Movement legality: partial-complete for source/destination validation,
  ownership, readiness, type restrictions, controller assignment, contested
  showdown starts, and same-zone repositioning.
- Showdown timing and combat resolution: partial-complete for active showdown
  steps, deterministic simultaneous damage, Tank priority, Assault, Shield, and
  Stun.
- Battlefield control.
- Scoring and winning rules: partial-complete for Hold, Conquer, target-score
  winner checks, Conquer final-point replacement, and completed-match history
  snapshots.

## v0.4-beta: Combat and Card Support

Status: Planned / Partial

- Showdown timing precision edge cases.
- Interactive combat damage assignment.
- Keyword/effect registry expansion beyond the initial scaffold.
- Starter deck cards fully supported.
- Popular tournament cards implemented.

## v0.5-beta: Public Playtest Polish

Status: Planned

- Deck import/export polish.
- Release builds through GitHub Releases.
- Release notes template and generated-artifact ignore rules.
- CI.
- Issue templates.
- Reconnect hardening.
- Better spectator and match review tools.

## Current Next Priorities

1. Online presence polish, including live broadcast re-enable after broker startup hardening.
2. Starter/sample supported deck expansion.
3. Import/export polish for sideboards and alternate printing choices.
4. Broader card-by-card audits for support metadata.
5. Bug-report/debug-info polish.
6. Rune payment edge cases and official timing polish.
7. Unit/spell/gear edge cases and card-specific legality.
8. Multi-battlefield movement and control modeling.
9. Combat/showdown precision.
10. Keyword/effect handler expansion for supported starter-deck cards.
