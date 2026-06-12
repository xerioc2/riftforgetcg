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
- `CardSupportService` assigns conservative support metadata: Supported,
  Partial, Unsupported, Banned, or Not Audited.
- Ready validation can optionally enforce supported-cards-only mode, blocking
  Unsupported or Not Audited cards while surfacing Partial cards as warnings.
- The deck builder imports and exports tournament-style sections: Legend,
  Champion, MainDeck, Battlefields, and Rune Pool.
- The deck builder validation report surfaces legality errors, banned cards,
  unsupported cards, partial cards, and missing card data before lobby ready.

Known gaps:
- Domain identity and signature-card legality are not fully enforced.
- Sideboards, match deck registration, set legality, and rotation are not implemented.
- Copy limits are by card ID, not normalized card name or all official identity rules.
- Support metadata is intentionally conservative and still needs card-by-card
  audits for most of the card pool.
- Imported sideboards are currently skipped rather than modeled as match
  sideboards.

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
- Server-computed legal actions are included in player-specific projected state so the client can hide or disable actions using the same conservative action matrix.
- Current action windows include mulligan, basic phase pass, active-player Main Phase actions, a lightweight active-showdown Action window, active showdown resolution, and sandbox actions only in SANDBOX mode.

Known gaps:
- Official cleanup/HOT FEPR sequencing is not fully modeled.
- The engine still uses `END` while current official terminology uses Ending/expiration details.
- Trigger and chain timing is highly simplified.
- Full Reaction timing and priority/chain timing are not represented in the legal-action projection yet.

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
- Units are blocked from direct hand-to-battlefield play unless they are using
  the current alpha Ambush path.
- Alpha Ambush supports only Main-phase battlefield play for clean Ambush Units
  when the player already has a friendly Unit/Champion at the battlefield.
  Additional costs and Ambush-as-Reaction timing are explicitly rejected.
- Only Units and Champions can move to the battlefield to start a showdown.
- ACCELERATE is supported with the extra-energy flag.
- Some keywords modify entry/combat behavior.

Known gaps:
- Chain permissions, Ambush-as-Reaction windows, and full target-location
  permissions are incomplete.
- Domain and power-cost validation are partial.

Test coverage:
- `RulesValidatorGameModeTest`
- `LegalActionsServiceTest`
- `GameEnginePlayCardTypeTest`

Priority: P0.

## Playing Spells

Status: Partial

Current implementation notes:
- Spells can be played during MAIN.
- Spells resolve through the supported effect path and move to discard afterward.
- Spells cannot move to the battlefield as units.
- Targeted-spell heuristics require a valid battlefield target.
- Simple helper-backed effect scripts currently cover draw 1, selected
  temporary Might boosts, selected unit/champion return-to-hand, and selected
  friendly unit/champion readying.
- Unsupported spell shapes are blocked by `CardDataService.isUnsupportedAction`.
- VISION/Predict-like peeking has a basic private choice flow.

Known gaps:
- Chain timing, Reaction timing, countering spells, multi-target spells, replacement/prevention, and many spell-specific effects are not complete.
- Active-showdown `[Action]` play is lightweight: showdown participants can play supported Action cards, the attacker can resolve, and no chain/response system exists yet.

Test coverage:
- Rules validator keyword/target tests.
- Projection tests for private VISION logs.
- `GameEnginePlayCardTypeTest`

Priority: P0.

## Playing Gear

Status: Partial

Current implementation notes:
- Basic `[Equip]` gear requires a friendly battlefield Unit/Champion target,
  can attach to that target, and can apply some keyword hooks.
- Gear can be played to base when supported.
- Gear cannot move to the battlefield or fight as a unit.
- Non-equip gear is treated as unsupported.
- Gear attached to a unit/champion is moved to trash and detached when its
  host is destroyed or returned to hand.

Known gaps:
- Official equipment timing, replacement/reattachment edge cases, payment-domain
  precision, voluntary detach rules, and many card-specific gear effects are
  incomplete.

Test coverage:
- `GameEnginePlayCardTypeTest`

Priority: P1.

## Movement

Status: Partial

Current implementation notes:
- `MoveToBattlefieldMove` is the enforced movement path.
- Free-form `MoveCardMove` is sandbox-only.
- `RepositionCardMove` changes x/y only and cannot change zones.
- Repositioning is limited to owned cards in public zones.
- Movement validates ownership, active player, readiness, source zone, and card type.
- Showdowns are staged when movement creates battlefield opposition.
- Moving to an empty battlefield updates `battlefieldController`.

Known gaps:
- Multiple battlefields remain an official-rules gap, but they are
  intentionally deferred until after the single-battlefield alpha is stable.
  The full model affects movement, target selection, showdown, control,
  scoring, bot decisions, and UI layout.
- Movement costs, readiness/exhaustion edge cases, Ganking exceptions, and
  effect-driven movement still need more precision in the current simplified
  battlefield flow.

Test coverage:
- `GameEngineShowdownTest`
- `GameEnginePlayCardTypeTest`
- `RulesValidatorGameModeTest`
- `LegalActionsServiceTest`

Priority: P0.

## Battlefield Control

Status: Partial

Current implementation notes:
- Battlefield control is tracked in `battlefieldController`.
- Conquer and hold can award points in simplified single-battlefield flow.
- Moving an unopposed unit or Champion to the battlefield sets that player as
  the current controller.
- Moving into an opposed battlefield starts `activeShowdown`.

Known gaps:
- Multiple battlefields and official contested/control cleanup are not fully
  modeled. This is deliberate post-alpha scope; current playtests focus on a
  readable single-battlefield control flow.
- Control locking during showdowns/combat and chain items is incomplete.

Test coverage:
- Showdown/combat tests cover basic conquer flow indirectly.
- `GameEnginePlayCardTypeTest` covers unopposed controller assignment and
  contested showdown start.

Priority: P0.

## Showdown Opening / Timing

Status: Partial

Current implementation notes:
- Showdown is modeled as `activeShowdown` while `currentPhase` remains MAIN.
- `activeShowdown.step` exposes the current simplified showdown step to the
  client; contested movement opens at ACTION_WINDOW.
- Nested showdowns are blocked.
- Resolving clears `activeShowdown` and returns to normal MAIN actions.
- Legal-action visibility pauses normal main actions during an active showdown, but may expose `PLAY_CARD` for a showdown participant when a supported `[Action]` card is in hand.

Known gaps:
- Non-combat showdowns, formal priority between participants, staged combat conversion,
  and chain/reaction timing permissions are simplified.
- Multiple battlefield showdowns are deferred until the post-alpha location
  model.

Test coverage:
- `GameEngineShowdownTest`
- `LegalActionsServiceTest`

Priority: P1.

## Combat Damage Assignment

Status: Partial

Current implementation notes:
- `CombatResolver` resolves simplified attacker/defender combat.
- Damage is assigned deterministically with Tank-priority lethal assignment.
- Damage is simultaneous; killed units move to trash after both sides assign
  damage.
- Survivors heal during combat cleanup.
- ASSAULT, SHIELD, TANK priority, and STUN have partial support.

Known gaps:
- Player-chosen damage assignment, prevention/replacement, combat designation
  cleanup, and many multi-unit edge cases are incomplete.
- Multiple battlefield combat is intentionally post-alpha work.

Test coverage:
- `CombatResolverTest`
- `RulesValidatorKeywordTest`

Priority: P0.

## Conquer / Hold Scoring

Status: Partial

Current implementation notes:
- Hold scoring runs during Beginning for each battlefield controlled by the
  active player in `battlefieldController`.
- Conquer scoring runs after showdown resolution when attackers survive and
  defenders are eliminated.
- `scoredBattlefieldsThisTurn` prevents duplicate scoring for the same
  battlefield in one turn.

Known gaps:
- Multiple named battlefields are represented by controller keys, but full
  official battlefield selection, assignment, and cleanup timing are
  intentionally deferred until after the single-battlefield alpha stabilizes.

Test coverage:
- `GameEngineScoringTest`
- `GameEngineShowdownTest`

Priority: P1.

## Winning Point Rule

Status: Partial

Current implementation notes:
- Conquer has a guard for the target-score-minus-one winning point rule. If a
  Conquer point would be an illegal final point because not every battlefield has
  been scored this turn, it draws instead of scoring.
- Hold can still award the winning point.
- Winner is set only when a player legally reaches the configured target score.
- Match history records completed-match snapshots with public player summaries.

Known gaps:
- Official "win during cleanup if score is greater than/equal to victory score and greater than opponents" is not fully modeled.
- Multiplayer/tie/burnout edge cases are not modeled.

Test coverage:
- `GameEngineScoringTest`
- `MatchHistoryServiceTest`
- `GameServiceMoveSerializationTest`

Priority: P1.

## Keywords

Status: Partial

Current implementation notes:
- Keyword parsing exists in `CardDataService`.
- `EffectHandlerRegistry` provides a central support-status lookup for tracked
  keywords and reports missing handlers as explicit unsupported behavior instead
  of silent no-ops.
- Initial keyword handlers include Assault, Shield, Tank, and Vision, with
  existing combat/rules code still handling some keyword behavior directly.
- ASSAULT and SHIELD are handler-backed and combat-tested as deterministic
  situational Might modifiers through `CombatStatsService`. Plain keywords
  default to +1, and valued keyword parsing is case-insensitive and accepts
  both spaced and compact forms such as `ASSAULT 2` and `ASSAULT2`.
- MIGHTY has a central `CombatStatsService` helper for Unit/Champion cards with
  effective Might 5 or greater in the requested context, including temporary
  and permanent Might modifiers plus Assault/Shield while attacking or
  defending. "Becomes Mighty" triggers are not wired yet.
- `docs/KEYWORD_BACKLOG.md` tracks supported, partial, unsupported, and
  confirmation-needed keywords. `docs/MISSING_RULES_BACKLOG.md` tracks the
  broader P0-P3 rules backlog.
- Several keywords have direct or heuristic handling: ACCELERATE, AMBUSH,
  DEATHKNELL, DEFLECT, GANKING, HIDDEN, LEGION, TANK, TEMPORARY, VISION,
  WEAPONMASTER.
- HIDDEN has a conservative foundation: `[Hidden]` cards can move from hand to
  a dedicated hidden zone by tapping a ready own rune; owner projections keep
  identity visible while opponent/spectator projections mask the card id and
  facedown state. Hidden cards are excluded from normal targeting, movement,
  and combat. Later hidden play/reaction timing is not implemented.
- AMBUSH has a conservative alpha foundation: a clean Ambush Unit can be played
  directly from hand to the battlefield during supported Main-phase play if its
  controller already has a friendly Unit/Champion at the battlefield. Reaction
  timing and additional costs, including Stalking Wolf's kill cost, remain
  unsupported.
- DEATHKNELL has basic trigger plumbing through `DeathTriggerService`: real
  deaths fire after graveyard movement, bounce/return-to-hand does not fire,
  simultaneous combat deaths are batched deterministically, and Loyal Poro's
  "didn't die alone" draw is covered.
- Simple Recruit token creation exists through `TokenFactory` for starter-deck
  scripts. Noxian Drummer creates one Recruit when moved to battlefield, and
  Vanguard Captain creates two Recruits when its current simple Legion condition
  is active.

Known gaps:
- The handler registry is a scaffold; several tracked keywords still need
  dedicated handlers before they can be called fully supported.
- The complete official keyword list, dependent keywords, inactive text, conditional permissions, XP/Hunt/Level, and full action/reaction behavior are incomplete.
- Some legacy placeholder keywords remain in early hard-coded effects and should be audited against current official names.
- Scuttle Crab's Deathknell reveal/facedown/XP text, general token definitions,
  official token cleanup, and broad token creation effects remain incomplete.

Test coverage:
- `RulesValidatorKeywordTest`
- `CombatResolverTest`
- `EffectHandlerRegistryTest`

Priority: P0 for expanding keyword handlers and connecting card-specific script metadata.

## Card-Specific Effects

Status: Partial

Current implementation notes:
- `CardEffectRegistry` contains a small number of hard-coded effects.
- Effect architecture scaffolding exists for keyword, on-play, triggered,
  activated, static modifier, and replacement handlers.
- `EffectHandlerRegistry` centralizes support-status decisions for tracked
  keywords and unsupported generic spell/gear shapes.
- `CardSupportService` is the current card-support metadata source for deck
  warnings and supported-only gates.
- `GameEngine.applyRulesTextEffect` routes the safe generic patterns through
  focused helpers: `applyDraw`, `applyTemporaryMight`,
  `returnUnitToOwnerHand`, and `readyUnit`.
- Unsupported spell/gear patterns are rejected instead of silently pretending to work.

Known gaps:
- Most real cards have no precise scripted effect.
- The declarative/scripted card registry is still early and not wired for most
  real cards.
- Supported status should not be promoted until the whole card has explicit
  behavior and tests; helper-backed simple effects still leave cards Partial
  when timing, choices, or extra clauses are incomplete.
- Optional triggers, may choices, targeting decisions, and chain items are incomplete.

Test coverage:
- Scattered validator/engine tests.
- `EffectHandlerRegistryTest`

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
- Player-specific `GameStateProjectionService` output now includes `legalActions` for the viewer.
- Spectator/public projections receive an empty legal-action set.
- The frontend consumes `state.legalActions` to gate mulligan/keep, pass phase, play card, move to battlefield, rune actions, active showdown resolution, and sandbox-only controls.
- `RulesValidator` remains the source of enforcement.
- The service intentionally does not claim support for card-specific or reaction windows that are not implemented.
- Currently modeled windows: mulligan, basic phase pass, Main Phase active-player actions, participant supported Action play during active showdowns, active showdown resolution, and SANDBOX-only developer actions.

Known gaps:
- Actions are not card-instance-specific.
- Full Reaction windows and chain priority are future work.
- Card-specific legal action prompts are not generated.
- Target-specific and payment-specific legal action generation is incomplete.
- Chain/timing permissions and full priority handling are future work.

Test coverage:
- `LegalActionsServiceTest`
- `GameStateProjectionServiceTest`

Priority: P1.

## UI Guidance

Status: Partial

Current implementation notes:
- The client uses projected `legalActions` for major game-action affordances instead of relying only on local phase guesses.
- Unavailable phase/showdown controls are hidden.
- Mulligan and Keep buttons require `MULLIGAN` or `KEEP_HAND`.
- Normal Main Phase controls require `PLAY_CARD`, `MOVE_TO_BATTLEFIELD`, rune actions, or sandbox-specific actions as appropriate.
- Same-zone card organization uses `REPOSITION_CARD`; cross-zone `MOVE_CARD` is
  sandbox-only.

Known gaps:
- UI does not yet show card-specific legality explanations before a server rejection.
- Action prompts are not generated per card, target, payment mode, or reaction window.
- Client error presentation still needs a clearer visible notification path.

Test coverage:
- Frontend build/type checking.
- Server projection tests cover the data contract that the UI consumes.

Priority: P1.

## Next Rules Sprints

1. Rune payment validation: domain/power costs, recycling, cost modifiers.
2. Play-card legality edge cases: action/reaction permissions, gear attachment detail, card-specific prompts.
3. Movement legality edge cases: Ganking, effect-driven movement, and current
   single-battlefield readability. Full multiple battlefields are post-alpha.
4. Showdown timing edge cases: interactive action windows, combat conversion, open states.
5. Combat damage assignment edge cases: player assignment, multi-unit combat, prevention/replacement.
6. Winning point edge cases: official cleanup timing, multiplayer/tie/burnout cases.
7. Keyword/effect registry expansion: official keyword inventory and per-card script metadata.
8. Tournament legality: sideboards, set legality, rotation, match procedure, errata tracking.
