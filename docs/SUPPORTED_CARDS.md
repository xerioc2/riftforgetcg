# RiftForge Supported Cards Matrix

Last audit: 2026-06-10

This is a scaffold for tracking card-specific support. "Heuristic" means the engine may support a text pattern, but the individual card has not been scripted and tested as a tournament-accurate implementation.

Keyword/effect handler coverage is now tracked through the backend
`EffectHandlerRegistry`. A handler-backed keyword does not automatically make a
whole card Supported; the card still needs card-specific rules coverage and
tests before its status should be promoted.

Deck validation support gates use `CardSupportService` as the conservative
metadata source. Supported-only mode blocks Unsupported and Not Audited cards,
surfaces Partial cards as warnings, and always rejects Banned constructed cards.

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

## Starter Deck Card Status

Starter decks are curated for legal constructed shape and banned-card avoidance,
not complete rules coverage. Both starter decks are currently **Experimental**.

| Card name | Starter deck | Type | Status | Notes |
| --- | --- | --- | --- | --- |
| Irelia - Blade Dancer | Irelia Tempo | Legend | Experimental | Identity/setup card; full Legend text support may be partial. |
| Irelia - Fervent | Irelia Tempo | Champion | Experimental | Starts in Champion zone; full card-specific effects may be partial. |
| Defy | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Discipline | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Tideturner | Irelia Tempo | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Stellacorn Herder | Irelia Tempo | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Guardian Angel | Irelia Tempo | Gear | Heuristic partial | Gear/equip timing remains partial. |
| Boots of Swiftness | Irelia Tempo | Gear | Heuristic partial | Gear/equip timing remains partial. |
| Defiant Dance | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Scuttle Crab | Irelia Tempo | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Charm | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| En Garde | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Gust | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Ride The Wind | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Stacked Deck | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Not So Fast | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Star-Crossed | Irelia Tempo | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Adaptatron | Irelia Tempo | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Calm Rune | Irelia Tempo | Rune | Partial | Rune deck setup and basic rune actions are implemented; payment nuance remains incomplete. |
| Chaos Rune | Irelia Tempo | Rune | Partial | Rune deck setup and basic rune actions are implemented; payment nuance remains incomplete. |
| Targon's Peak | Irelia Tempo | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |
| Sunken Temple | Irelia Tempo | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |
| Abandoned Hall | Irelia Tempo | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |
| Fiora - Grand Duelist | Fiora Vanguard | Legend | Experimental | Identity/setup card; full Legend text support may be partial. |
| Fiora - Worthy | Fiora Vanguard | Champion | Experimental | Starts in Champion zone; full card-specific effects may be partial. |
| Daring Poro | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Keeper's Verdict | Fiora Vanguard | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Spectral Matron | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Stalking Wolf | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Noxian Drummer | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Loyal Poro | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Vanguard Captain | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Facebreaker | Fiora Vanguard | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Vanguard Sergeant | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Laurent Duelist | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Crowd Favorite | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Riposte | Fiora Vanguard | Spell | Heuristic partial | Spell behavior depends on generic text handling. |
| Dune Drake | Fiora Vanguard | Unit | Experimental | Unit play/combat supported for common playtest flow. |
| Body Rune | Fiora Vanguard | Rune | Partial | Rune deck setup and basic rune actions are implemented; payment nuance remains incomplete. |
| Order Rune | Fiora Vanguard | Rune | Partial | Rune deck setup and basic rune actions are implemented; payment nuance remains incomplete. |
| Aspirant's Climb | Fiora Vanguard | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |
| Hall of Legends | Fiora Vanguard | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |
| Fortified Position | Fiora Vanguard | Battlefield | Experimental | Battlefield selection/setup is tracked; full battlefield text may be partial. |

## Next Matrix Work

- Replace placeholder IDs with canonical Riftcodex IDs and card names.
- Promote starter deck cards from experimental to partial/supported only after
  card-specific scripts and tests exist.
- Split "heuristic partial" rows into explicit card scripts as effects are implemented.
- Add a test column value only when a card has a direct unit/integration test.
