# RiftForge Rules Coverage

Last audit: 2026-06-10

This document tracks RiftForge's rules-engine coverage against the current Riftbound rules direction. It is intentionally conservative: "Supported" means the behavior is implemented and covered by tests in this codebase, not that every edge case in the official rules is complete.

Reference baseline:
- Official Riftbound site and Rules Hub entry point: https://riftbound.leagueoflegends.com/en-us/
- Core Rules article notes that the Core Rules document is the technical rules source and that the Rules Hub has the most up-to-date references.
- How to Play / Core Rules quick guide confirms the deck parts, 12-card rune deck, 40-card main deck with chosen champion, 39-card shuffled main deck, battlefield setup, scoring, and rune payment basics.
- Unleashed Core Rules patch notes, 2026-03-30, clarify showdowns, combat cleanup, winning the game, dependent keywords, action/reaction timing, and new systems.
- Tournament Rules and organized play articles remain the baseline for match procedure, decklists, penalties, and tournament legality.

## Status Key

- Supported: implemented and covered by focused tests.
- Partial: implemented for common playtest paths, but missing official edge cases.
- Unsupported: deliberately blocked or not rules-correct yet.
- Not started: no meaningful implementation yet.

## Deck Construction

Status: Partial

Current implementation notes:
- `RoomService.validateDeck` validates `FULL_CONSTRUCTED` as 1 Legend, exactly 1 Champion, exactly 39 non-special main cards, exactly 12 runes, exactly 3 unique battlefields, and a 3-copy limit for non-special cards.
- `PLAYTEST_BOT` stays looser so bot games can run with generated test decks.
- Constructed banlist names are centralized in `TournamentLegality` and rejected during `FULL_CONSTRUCTED` validation.

Known gaps:
- Domain identity and signature-card legality are not fully enforced.
- Sideboards, match deck registration, set legality, and rotation are not implemented.
- Copy limits are by card ID, not normalized card name or all official identity rules.

Test coverage:
- `RoomServiceDeckValidationTest`

Priority: P0 for domain/signature legality before tournament playtesting.

## Game Setup

Status: Partial

Current implementation notes:
- Submitted decks are partitioned into Legend, Champion, main deck, runes, and battlefields.
- Legend and Champion start in their correct zones.
- Opening hand draws from the main deck pool only.
- Deck/rune counts are projected without exposing hidden deck contents.

Known gaps:
- Battlefield selection/control is simplified to a single battlefield key in several engine paths.
- Starting player selection is not fully derived from battlefield ownership/randomization.
- 2-4 player setup is not fully rules-complete.

Test coverage:
- `GameServiceDeckStartTest`
- `BotServicePhaseFlowTest`

Priority: P0 for battlefield model before tournament-accurate games.

## Mulligan

Status: Partial

Current implementation notes:
- MULLIGAN phase exists.
- Players may keep or recycle up to 2 cards, then draw replacements.
- Legal-action visibility exposes only `KEEP_HAND` and `MULLIGAN` during mulligan until the player completes it.

Known gaps:
- Multiplayer turn-order mulligan nuance is not deeply modeled.
- UI/engine language should stay aligned with official "recycle up to 2" wording.

Test coverage:
- `LegalActionsServiceTest`
- `BotServicePhaseFlowTest`

Priority: P2.

## Turn Phases

Status: Partial

Current implementation notes:
- Engine phases are `MULLIGAN`, `AWAKEN`, `BEGINNING`, `CHANNEL`, `DRAW`, `MAIN`, `END`.
- Phase passing is server-authoritative.
- Early phases do not expose normal main actions through `LegalActionsService`.

Known gaps:
- Official cleanup/HOT FEPR sequencing is not fully modeled.
- The engine still uses `END` while current official terminology uses Ending/expiration details.
- Trigger and chain timing is highly simplified.

Test coverage:
- `LegalActionsServiceTest`
- `BotServicePhaseFlowTest`

Priority: P1.

## Rune / Channel / Payment Rules

Status: Partial

Current implementation notes:
- Channel grants runes from a remaining rune pool.
- Rune deck count starts from submitted runes for constructed setup.
- Tapping and discarding runes can add energy.
- Energy is cleared for all players on turn transition.

Known gaps:
- Domain-specific power costs are not enforced.
- Rune recycling/exhaustion is simplified.
- Cost reductions, same-rune payment for energy and power, and complex payment components are incomplete.

Test coverage:
- Phase-flow and setup tests cover counts and basic channel flow.

Priority: P0.

## Playing Units

Status: Partial

Current implementation notes:
- Units can be played from hand to base during MAIN if the player has enough available energy.
- ACCELERATE is supported with the extra-energy flag.
- Some keywords modify entry/combat behavior.

Known gaps:
- Play legality by type, chain permissions, Ambush/Reaction windows, and target-location permissions are incomplete.
- Domain and power-cost validation are incomplete.

Test coverage:
- `RulesValidatorGameModeTest`
- `LegalActionsServiceTest`

Priority: P0.

## Playing Spells

Status: Partial

Current implementation notes:
- Spells can be played during MAIN.
- Targeted-spell heuristics require a valid battlefield target.
- Unsupported spell shapes are blocked by `CardDataService.isUnsupportedAction`.
- VISION/Predict-like peeking has a basic private choice flow.

Known gaps:
- Chain, action/reaction timing, countering spells, multi-target spells, replacement/prevention, and many spell-specific effects are not complete.
- Non-active spell play is currently very narrow and should not be treated as official Reaction support.

Test coverage:
- Rules validator keyword/target tests.
- Projection tests for private VISION logs.

Priority: P0.

## Playing Gear

Status: Partial

Current implementation notes:
- Basic `[Equip]` gear can attach to a target and apply some keyword hooks.
- Non-equip gear is treated as unsupported.

Known gaps:
- Equipment timing, replacement, attachment legality, and many card-specific gear effects are incomplete.

Test coverage:
- Indirect validator coverage.

Priority: P1.

## Movement

Status: Partial

Current implementation notes:
- `MoveToBattlefieldMove` is the enforced movement path.
- Free-form `MoveCardMove` is sandbox-only.
- Showdowns are staged when movement creates battlefield opposition.

Known gaps:
- Multiple battlefields, movement costs, readiness/exhaustion edge cases, Ganking exceptions, and effect-driven movement need a richer location model.

Test coverage:
- `GameEngineShowdownTest`
- `RulesValidatorGameModeTest`
- `LegalActionsServiceTest`

Priority: P0.

## Battlefield Control

Status: Partial

Current implementation notes:
- Battlefield control is tracked in `battlefieldController`.
- Conquer and hold can award points in simplified single-battlefield flow.

Known gaps:
- Multiple battlefields and official contested/control cleanup are not fully modeled.
- Control locking during showdowns/combat and chain items is incomplete.

Test coverage:
- Showdown/combat tests cover basic conquer flow indirectly.

Priority: P0.

## Showdown Opening / Timing

Status: Partial

Current implementation notes:
- Showdown is modeled as `activeShowdown` while `currentPhase` remains MAIN.
- Nested showdowns are blocked.
- Resolving clears `activeShowdown` and returns to normal MAIN actions.
- Legal-action visibility pauses normal main actions during an active showdown.

Known gaps:
- Non-combat showdowns, multiple battlefields, staged combat conversion, open states, and chain/timing permissions are simplified.

Test coverage:
- `GameEngineShowdownTest`
- `LegalActionsServiceTest`

Priority: P1.

## Combat Damage Assignment

Status: Partial

Current implementation notes:
- `CombatResolver` resolves simplified attacker/defender combat.
- ASSAULT, SHIELD, TANK-related validation, and some combat keywords have partial support.

Known gaps:
- Official damage assignment order, lethal assignment, combat designation cleanup, replacement/prevention, and multi-unit combat details are incomplete.

Test coverage:
- `CombatResolverTest`
- `RulesValidatorKeywordTest`

Priority: P0.

## Conquer / Hold Scoring

Status: Partial

Current implementation notes:
- Hold scoring exists at beginning.
- Conquer scoring exists after showdown resolution.

Known gaps:
- Multiple battlefield scoring and official cleanup timing are incomplete.

Test coverage:
- Indirect engine tests.

Priority: P1.

## Winning Point Rule

Status: Partial

Current implementation notes:
- Conquer has a guard for the target-score-minus-one winning point rule in the simplified battlefield model.
- Hold can still award the winning point.

Known gaps:
- Official "win during cleanup if score is greater than/equal to victory score and greater than opponents" is not fully modeled.
- Multiplayer/tie/burnout edge cases are not modeled.

Test coverage:
- Existing engine tests cover only simplified paths.

Priority: P1.

## Keywords

Status: Partial

Current implementation notes:
- Keyword parsing exists in `CardDataService`.
- Several keywords have direct or heuristic handling: ACCELERATE, AMBUSH, ASSAULT, DEFLECT, GANKING, HIDDEN, LEGION, SHIELD, TANK, TEMPORARY, VISION, WEAPONMASTER.

Known gaps:
- The complete official keyword list, dependent keywords, inactive text, conditional permissions, XP/Hunt/Level, and full action/reaction behavior are incomplete.
- Some legacy placeholder keywords remain in early hard-coded effects and should be audited against current official names.

Test coverage:
- `RulesValidatorKeywordTest`
- `CombatResolverTest`

Priority: P0 for timing keywords and supported-card registry.

## Card-Specific Effects

Status: Partial

Current implementation notes:
- `CardEffectRegistry` contains a small number of hard-coded effects.
- `GameEngine.applyRulesTextEffect` supports a few generic rules-text patterns.
- Unsupported spell/gear patterns are rejected instead of silently pretending to work.

Known gaps:
- Most real cards have no precise scripted effect.
- No declarative card script registry exists yet.
- Optional triggers, may choices, targeting decisions, and chain items are incomplete.

Test coverage:
- Scattered validator/engine tests.

Priority: P0.

## Tournament Legality / Banlist

Status: Partial

Current implementation notes:
- `TournamentLegality` centralizes the current Constructed banned cards and battlefields:
  - Called Shot
  - Draven, Vanquisher
  - Fight or Flight
  - Scrapheap
  - Dreaming Tree
  - Obelisk of Power
  - Reaver's Row
- `FULL_CONSTRUCTED` validation rejects those names.

Known gaps:
- Set legality, sideboards, best-of-three match rules, tournament procedure, errata tracking, and rotation are not implemented.
- Banlist data should eventually be versioned and sourced from official structured data if Riot exposes it.

Test coverage:
- `RoomServiceDeckValidationTest`

Priority: P1.

## Multiplayer Formats

Status: Not started

Current implementation notes:
- Some room/player data structures support more than two players.
- Primary engine assumptions are still effectively 1v1.

Known gaps:
- Free-for-all/team target score, seating, battlefield count, first-player rules, priority/timing, and team scoring are incomplete.

Test coverage:
- None focused.

Priority: P2.

## Legal Action Matrix

Status: Partial

Current implementation notes:
- `LegalActionsService` returns conservative high-level actions for the current player.
- `RulesValidator` remains the source of enforcement.
- The service intentionally does not claim support for card-specific or reaction windows that are not implemented.

Known gaps:
- No REST endpoint exposes legal actions yet.
- Actions are not card-instance-specific.
- Chain/timing permissions and full target legality are future work.

Test coverage:
- `LegalActionsServiceTest`

Priority: P1.

## Next Rules Sprints

1. Rune payment validation: domain/power costs, recycling, cost modifiers.
2. Play-card legality by type: unit, spell, gear, action/reaction permissions.
3. Movement legality: multiple battlefields, exhaustion, Ganking, effect movement.
4. Showdown timing/cleanup precision: staged showdowns, combat conversion, open states.
5. Combat damage assignment: lethal assignment, multi-unit combat, prevention/replacement.
6. Winning point rule: cleanup win checks, multiplayer/tie/burnout cases.
7. Keyword/effect registry: official keyword inventory and per-card script metadata.
8. Tournament legality: sideboards, set legality, rotation, match procedure, errata tracking.
