# RiftForge Supported Cards Matrix

Last audit: 2026-06-10

This is a scaffold for tracking card-specific support. "Heuristic" means the engine may support a text pattern, but the individual card has not been scripted and tested as a tournament-accurate implementation.

| Card name | Set | Type | Status | Supported effects | Unsupported effects | Tests | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| origins-001 | Origins | Unknown from local ID | Partial | Hard-coded `onPlay` grants temporary TOUGH to allied battlefield cards. | Verify real card name, current wording, and official keyword names. | Indirect only | Registry ID should be replaced with canonical Riftcodex ID/name if it differs. |
| origins-002 | Origins | Unknown from local ID | Partial | Hard-coded `onPlay` grants temporary RUSH to allied champions in champion/battlefield zones. | Verify real card name, current wording, and official keyword names. | Indirect only | May use legacy keyword language. |
| origins-003 | Origins | Unknown from local ID | Partial | Hard-coded `onPlay` grants temporary OVERWHELM to itself. | Verify real card name, current wording, and official keyword names. | Indirect only | May use legacy keyword language. |
| origins-004 | Origins | Unknown from local ID | Partial | Hard-coded `onPlay` adds 1 available energy. | Full cost/timing interaction. | Indirect only | Needs card-name mapping. |
| origins-005 | Origins | Unknown from local ID | Partial | Hard-coded `onTurnStart` clears summoning sickness from allied base cards while source is at battlefield. | Full trigger timing and current wording. | Indirect only | Needs card-name mapping. |
| Any Spell with `draw 1` text | Any | Spell | Heuristic partial | Generic rules-text path draws 1 card. | Additional conditions, costs, targets, may choices, chain timing. | Validator/engine indirect | Should become explicit scripted effects for tournament use. |
| Any Spell/Gear with `:rb_might:` boost text | Any | Spell/Gear | Heuristic partial | Generic rules-text path adds temporary might modifier to a target. | Duration nuances, target restrictions, multi-target, replacement/prevention. | Validator/engine indirect | Depends on target validator heuristics. |
| Any text with `return a unit` / `return target unit` | Any | Spell | Heuristic partial | Generic rules-text path returns target to hand and trashes attachments. | Ownership, destination, replacement effects, non-unit filtering. | Validator/engine indirect | Needs explicit scripts. |
| Any text with `ready it` | Any | Spell/Gear | Heuristic partial | Generic rules-text path readies target. | Full target requirements and timing windows. | Validator/engine indirect | Needs explicit scripts. |
| VISION keyword cards | Any | Any | Partial | Peeks top main-deck card privately and supports keep/recycle choice. | Full Predict rules and multiple-card ordering. | Projection and legal-action indirect | Uses private logs and `VISION_CHOICE`. |
| Called Shot | Current Constructed | Card | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | `RoomServiceDeckValidationTest` | Banlist entry only. |
| Draven, Vanquisher | Current Constructed | Card | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | Banlist utility via same path | Add direct test if card fixture is introduced. |
| Fight or Flight | Current Constructed | Card | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | Banlist utility via same path | Add direct test if card fixture is introduced. |
| Scrapheap | Current Constructed | Card | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | Banlist utility via same path | Add direct test if card fixture is introduced. |
| Dreaming Tree | Current Constructed | Battlefield | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | `RoomServiceDeckValidationTest` | Banlist entry only. |
| Obelisk of Power | Current Constructed | Battlefield | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | Banlist utility via same path | Add direct test if card fixture is introduced. |
| Reaver's Row | Current Constructed | Battlefield | Banned | Rejected in `FULL_CONSTRUCTED`. | N/A | Banlist utility via same path | Add direct test if card fixture is introduced. |

## Next Matrix Work

- Replace placeholder IDs with canonical Riftcodex IDs and card names.
- Add starter/bot deck rows after pinning the generated deck fixture.
- Split "heuristic partial" rows into explicit card scripts as effects are implemented.
- Add a test column value only when a card has a direct unit/integration test.
