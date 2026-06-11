# RiftForge Supported Cards Matrix

Last audit: 2026-06-11

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
| ASSAULT X keyword units | Any | Unit | Partial | Handler-backed keyword support; adds X Might while attacking in combat resolution. | Card-specific triggered/static text beyond ASSAULT still needs individual scripts/tests. | `CombatResolverTest`, `EffectHandlerRegistryTest`, `CardDataServiceEffectRegistryTest` | Valued parsing supports `ASSAULT 2` and `ASSAULT2`. |
| SHIELD X keyword units | Any | Unit | Partial | Handler-backed keyword support; adds X Might while defending in combat resolution. | Card-specific triggered/static text beyond SHIELD still needs individual scripts/tests. | `CombatResolverTest`, `EffectHandlerRegistryTest`, `CardDataServiceEffectRegistryTest` | Valued parsing supports `SHIELD 2` and `SHIELD2`. |
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
The full code-backed audit, including card IDs, detected keywords, current
support-gate result, and rules text, lives in `docs/CARD_RULES_BACKLOG.md`.

| Card name | Starter deck | Type | Current status | Implementation bucket | Actionable note |
| --- | --- | --- | --- | --- | --- |
| Irelia - Blade Dancer | Irelia Tempo | Legend | Partial | Legend text | Needs legend activated/triggered readying hooks and payment prompts. |
| Irelia - Fervent | Irelia Tempo | Champion | Partial | Champion text | Needs Deflect targeting tax and choose/ready might trigger. |
| Defy | Irelia Tempo | Spell | Unsupported | Unsupported/unknown text pattern | Blocked by `isUnsupportedAction`; needs reaction stack/counter-spell timing. |
| Discipline | Irelia Tempo | Spell | Partial | Spell: draw/card selection | Needs explicit script for reaction timing, +2 might target, and draw 1. |
| Tideturner | Irelia Tempo | Unit | Partial | Unit with triggered ability | Needs Hidden support and play trigger that swaps locations with a friendly unit. |
| Stellacorn Herder | Irelia Tempo | Unit | Partial | Unit with triggered ability | Needs move trigger that draws 1. |
| Guardian Angel | Irelia Tempo | Gear | Partial | Gear/equipment | Needs equipment attachment/payment handler; current equip support is only generic. |
| Boots of Swiftness | Irelia Tempo | Gear | Partial | Gear/equipment | Needs equipment attachment/payment handler; current equip support is only generic. |
| Defiant Dance | Irelia Tempo | Spell | Unsupported | Spell: stat/might modifier | Blocked by multi-target text; needs two-target stat modifier script. |
| Scuttle Crab | Irelia Tempo | Unit | Partial | Unit with triggered ability | Needs on-play draw, Deathknell reveal/XP, and hand/facedown reveal safety. |
| Charm | Irelia Tempo | Spell | Unsupported | Unsupported/unknown text pattern | Blocked by unsupported move effect; needs enemy-unit movement target rules. |
| En Garde | Irelia Tempo | Spell | Partial | Spell: stat/might modifier | Needs explicit target and lone-unit conditional might script. |
| Gust | Irelia Tempo | Spell | Partial | Spell: bounce/return | Needs explicit target filter for battlefield units with 3 or less Might. |
| Ride The Wind | Irelia Tempo | Spell | Partial | Spell: ready/exhaust | Needs friendly-unit move plus ready script. |
| Stacked Deck | Irelia Tempo | Spell | Unsupported | Spell: draw/card selection | Needs look-at-top-3 choice UI and recycle ordering. |
| Not So Fast | Irelia Tempo | Spell | Unsupported | Unsupported/unknown text pattern | Blocked by counter/reaction timing; needs spell/ability stack target model. |
| Star-Crossed | Irelia Tempo | Spell | Unsupported | Spell: bounce/return | Blocked by multi-target text; needs friendly/enemy paired return script. |
| Adaptatron | Irelia Tempo | Unit | Partial | Unit with triggered ability | Needs conquer trigger, gear-kill target, and buff placement. |
| Calm Rune | Irelia Tempo | Rune | Supported | Rune/payment rules | Basic rune setup/actions are covered; deeper payment edge cases remain roadmap work. |
| Chaos Rune | Irelia Tempo | Rune | Supported | Rune/payment rules | Basic rune setup/actions are covered; deeper payment edge cases remain roadmap work. |
| Targon's Peak | Irelia Tempo | Battlefield | Partial | Battlefield ability | Needs conquer-delayed rune-readying battlefield trigger. |
| Sunken Temple | Irelia Tempo | Battlefield | Partial | Battlefield ability | Needs Mighty check, optional payment, and draw trigger on conquer. |
| Abandoned Hall | Irelia Tempo | Battlefield | Partial | Battlefield ability | Needs spell-play trigger and per-controller unit buff target. |
| Fiora - Grand Duelist | Fiora Vanguard | Legend | Partial | Legend text | Needs Mighty-state detection and optional exhausted rune channel trigger. |
| Fiora - Worthy | Fiora Vanguard | Champion | Partial | Champion text | Needs Mighty-state detection, Order rune payment, and ready target trigger. |
| Daring Poro | Fiora Vanguard | Unit | Partial | ASSAULT value is handler-backed; remaining unit text is not fully scripted. | Needs direct card-specific behavior test before promotion. |
| Keeper's Verdict | Fiora Vanguard | Spell | Unsupported | Spell: draw/card selection | Needs enemy-unit target plus owner top/bottom deck choice. |
| Spectral Matron | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs trash-unit selection and free-play cost bypass with power-cost handling. |
| Stalking Wolf | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs Ambush/reaction timing and additional-cost kill validation. |
| Noxian Drummer | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs move-to-battlefield token creation. |
| Loyal Poro | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs Deathknell condition and draw trigger. |
| Vanguard Captain | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs Legion condition plus Recruit token creation. |
| Facebreaker | Fiora Vanguard | Spell | Unsupported | Unsupported/unknown text pattern | Needs Hidden support plus friendly/enemy stun target script. |
| Vanguard Sergeant | Fiora Vanguard | Unit | Partial | Basic unit with no special text | Candidate for first direct "basic unit supported" promotion after tests. |
| Laurent Duelist | Fiora Vanguard | Unit | Partial | ASSAULT 2 value is handler-backed; remaining unit text is not fully scripted. | Needs direct card-specific behavior test before promotion. |
| Crowd Favorite | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs XP/Hunt, activated XP spend, and Buff state support. |
| Riposte | Fiora Vanguard | Spell | Partial | Spell: stat/might modifier | Needs reaction timing, spell target model, counter behavior, and variable might buff. |
| Dune Drake | Fiora Vanguard | Unit | Partial | Unit with triggered ability | Needs attack trigger checking ready enemy units at the battlefield. |
| Body Rune | Fiora Vanguard | Rune | Supported | Rune/payment rules | Basic rune setup/actions are covered; deeper payment edge cases remain roadmap work. |
| Order Rune | Fiora Vanguard | Rune | Supported | Rune/payment rules | Basic rune setup/actions are covered; deeper payment edge cases remain roadmap work. |
| Aspirant's Climb | Fiora Vanguard | Battlefield | Partial | Battlefield ability | Needs target-score modification from selected battlefield setup. |
| Hall of Legends | Fiora Vanguard | Battlefield | Partial | Battlefield ability | Needs conquer optional payment and legend readying trigger. |
| Fortified Position | Fiora Vanguard | Battlefield | Partial | Battlefield ability | Needs defend trigger, target choice, and Shield 2 combat modifier. |

## Next Matrix Work

- Replace placeholder IDs with canonical Riftcodex IDs and card names.
- Promote starter deck cards to Supported only after card-specific scripts and
  tests exist.
- Split "heuristic partial" rows into explicit card scripts as effects are implemented.
- Add a test column value only when a card has a direct unit/integration test.
