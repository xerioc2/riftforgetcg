# RiftForge Rules Coverage

Last audit: 2026-06-10

This document tracks RiftForge's rules-engine coverage against the current Riftbound rules direction. It is intentionally conservative: "Supported" means the behavior is implemented and covered by tests in this codebase, not that every edge case in the official rules is complete.

Reference baseline:
- Official Riftbound site and Rules Hub entry point: https://riftbound.leagueoflegends.com/en-us/
- Core Rules article notes that the Core Rules document is the technical rules source and that the Rules Hub has the most up-to-date references.
- How to Play / Core Rules quick guide confirms the deck parts, 12-card rune deck, 40-card MainDeck, separate chosen Champion, battlefield setup, scoring, and rune payment basics.
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
- `RoomService.validateDeck` validates `FULL_CONSTRUCTED` as exactly 1 Legend, exactly 1 chosen Champion role card, exactly 40 MainDeck cards, exactly 12 runes, exactly 3 unique battlefields, and a 3-copy limit for exact card IDs excluding only Legend, Rune, and Battlefield cards.
- The chosen Champion copy is separate from the 40-card MainDeck but is included in exact-card copy counting. Champion-type Units in the MainDeck are allowed and count as MainDeck cards; only the role-selected chosen Champion starts in the Champion zone.
- `PLAYTEST_BOT` stays looser so bot games can run with generated test decks.
- Constructed banlist names are centralized in `TournamentLegality` and rejected during `FULL_CONSTRUCTED` validation.
- `CardSupportService` assigns conservative support metadata: Supported,
  Partial, Unsupported, Banned, or Not Audited.
- Current fully supported starter-deck cards are limited to basic runes,
  Vanguard Sergeant, Daring Poro, Laurent Duelist, Noxian Drummer,
  Loyal Poro, Lonely Poro, Vanguard Captain, and Stellacorn Herder.
- Ready validation can optionally enforce supported-cards-only mode, blocking
  Unsupported or Not Audited cards while surfacing Partial cards as warnings.
- The deck builder imports and exports tournament-style sections: Legend,
  Champion, MainDeck, Battlefields, and Rune Pool.
- The deck builder validation report surfaces legality errors, banned cards,
  unsupported cards, partial cards, and missing card data before lobby ready.

Known gaps:
- Domain identity and signature-card legality are not fully enforced.
- Sideboards, match deck registration, set legality, and rotation are not implemented.
- Champion-tag matching to Legend and signature-card restrictions are not fully enforced because the normalized card model does not yet expose reliable structured fields for those rules.
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
- Constructed games enter `SELECT_BATTLEFIELD` before mulligan. Each player sees their own three submitted Battlefields, chooses one, and selected Battlefield IDs are revealed/locked before mulligans begin.
- Champion-zone identity cards are not legal equip targets while they remain in the Champion zone. When destroyed in supported combat/cleanup paths, Champions return to the Champion zone rather than Trash, and attached Gear returns to Base.
- Chosen Champions can be deployed from the Champion zone only during supported Main play and must spend their real energy cost from currently available energy.
- Legends are identity/reference cards in the alpha model and cannot be moved to the battlefield.
- Opening hand draws from the main deck pool only.
- Deck/rune counts are projected without exposing hidden deck contents.

Known gaps:
- Battlefield play/control is still simplified to a single battlefield key in several engine paths after the pre-mulligan selection step.
- Starting player selection is not fully derived from battlefield ownership/randomization.
- 2-4 player setup is not fully rules-complete.

Test coverage:
- `GameServiceDeckStartTest`
- `BotServicePhaseFlowTest`

Priority: P0 for battlefield model before tournament-accurate games.

## Battlefield Selection And Mulligan

Status: Partial

Current implementation notes:
- `SELECT_BATTLEFIELD` phase exists before `MULLIGAN` for constructed decks with Battlefield choices.
- Player-specific projections include only that player's own Battlefield choices. Selected Battlefields are public once chosen.
- Legal-action visibility exposes `SELECT_BATTLEFIELD` only to players who still need to choose.
- MULLIGAN phase exists.
- Players may keep or recycle up to 2 cards, then draw replacements.
- Legal-action visibility exposes only `KEEP_HAND` and `MULLIGAN` during mulligan until the player completes it.

Known gaps:
- Multiplayer turn-order mulligan nuance is not deeply modeled.
- UI/engine language should stay aligned with official "recycle up to 2" wording.
- Full official multiple-Battlefield location setup remains post-alpha.

Test coverage:
- `LegalActionsServiceTest`
- `BotServicePhaseFlowTest`
- `LegalActionsFlowIntegrationTest`

Priority: P2.

## Turn Phases

Status: Partial

Current implementation notes:
- Engine phases are `SELECT_BATTLEFIELD`, `MULLIGAN`, `AWAKEN`, `BEGINNING`, `CHANNEL`, `DRAW`, `MAIN`, `END`.
- Phase passing is server-authoritative.
- Early phases do not expose normal main actions through `LegalActionsService`.
- Server-computed legal actions are included in player-specific projected state so the client can hide or disable actions using the same conservative action matrix.
- Current action windows include Battlefield selection, mulligan, basic phase pass, active-player Main Phase actions, a lightweight active-showdown focus/pass Action window, a narrow chain focus/pass/resolve foundation, gated active showdown resolution, and sandbox actions only in SANDBOX mode.
- Pending choices pause normal actions and expose `RESOLVE_CHOICE` only to the prompted player through player-specific projections.
- Private card-selection choices can show top-deck card options only to the owner; opponent and spectator projections omit those identities.

Known gaps:
- Official cleanup/HOT FEPR sequencing is not fully modeled.
- The engine still uses `END` while current official terminology uses Ending/expiration details.
- Trigger and chain timing is highly simplified.
- Full Reaction timing, counterspell targeting, and official priority/chain timing are not represented yet.
- Choice support covers private yes/no, generic optional-payment prompts, Stacked Deck-style top-3 pick-one, and a Predict-style top/bottom ordering foundation; required two-target spell selection has a narrow paired friendly/enemy foundation, while optional targets, linked choices, and full timing windows remain incomplete.

Test coverage:
- `LegalActionsServiceTest`
- `BotServicePhaseFlowTest`

Priority: P1.

## Priority / Chain Foundation

Status: Partial

Current implementation notes:
- `LiveGameState.chainState` can represent a public-safe stack of chain items, relevant players, current focus, consecutive passes, and a `readyToResolveTop` gate.
- Chain items now carry explicit lifecycle status: `PENDING`, `RESOLVED`,
  `COUNTERED`, or `FIZZLED`. The current engine cleans non-pending/fizzled
  items safely and will not resolve the same item twice.
- Chain items also carry counter-ready metadata such as counterability,
  targetability, item type, stable item ID, public description, controller, and
  source zone before the chain. This metadata is for future counterspell work
  only; no countering move or counter card is connected yet.
- `PASS_CHAIN_FOCUS` and `RESOLVE_CHAIN_TOP` are server-validated moves.
- Pending choices still take priority over chain actions. While a chain is active, normal phase/showdown actions are blocked until the chain item is resolved.
- `LegalActionsService` exposes `PASS_CHAIN_FOCUS`, `RESOLVE_CHAIN_TOP`,
  and narrowly supported focused Reaction play to the focused player.
  Spectators and non-focused players receive no chain actions.
- RiftBot can pass chain focus or resolve a ready top item through the same server legal-action contract.
- Current effect resolution is deliberately limited to deterministic test/no-op
  and draw-one harness items, Stacked Deck as the first real chain opener, and
  Gust and Defy as the first real chain-backed Reactions.
- Stacked Deck opens the narrow alpha chain when played in supported gameplay,
  becomes a public chain item, and creates its existing owner-only private
  top-card choice only after that chain item resolves.
- Gust can be played only while the controller is focused during an active
  chain. It creates a public chain item and, when resolved, returns a
  battlefield Unit/Champion with 3 Might or less to its owner's hand. If the
  target is no longer legal at resolution, Gust fizzles safely.
- Defy can be played only while the controller is focused during an active
  chain and can target a pending, public, counterable spell chain item whose
  source card cost is within Defy's alpha limits. Defy itself is not
  counterable in v1. When Defy resolves, the target item is marked
  `COUNTERED`, its source card moves from LIMBO to Trash, and the target item
  later cleans up without resolving.
- Chain item projection is viewer-aware. Public chain items can expose source and
  target metadata, while controller-only/private chain items mask source card
  IDs, source names, effect keys, and target instance IDs from opponents and
  spectator/public views. Private/masked chain items also suppress
  counter-target metadata and source-zone details for non-owners.
- The client shows a compact chain panel when `chainState` exists, ordered
  top-to-bottom with public-safe item descriptions, focus state, and
  non-pending item status. It still exposes chain buttons only through
  server-provided legal actions.

Known gaps:
- Gust and Defy are the only real Reaction cards connected to the chain. Not So
  Fast, Riposte counter behavior, Ambush-as-Reaction, hidden play windows, and
  ability-counter targets are not connected yet.
- No official priority, invitation, trigger-ordering, replacement/prevention, or multiplayer focus policy is implemented.
- Private/hidden chain objects have a projection policy, but real hidden
  Reaction timing and counterspell cards are still deferred until future
  sprints.

Test coverage:
- `GameEngineChainTest`
- `LegalActionsServiceTest`
- `GameStateProjectionServiceTest`
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
- Targeted-spell heuristics require valid server-checked targets. Single-target spells use the legacy `targetInstanceId` path; one narrow multi-target path supports required friendly Unit/Champion plus enemy Unit/Champion roles.
- Simple helper-backed effect scripts currently cover draw 1, selected
  temporary Might boosts, selected unit/champion return-to-hand, paired
  friendly/enemy unit return-to-hand, and selected friendly unit/champion
  readying.
- Unsupported spell shapes are blocked by `CardDataService.isUnsupportedAction`.
- A generic pending-choice framework exists for private yes/no, optional-payment,
  board-target follow-up prompts, Stacked Deck-style top-3 pick-one, and
  Predict-style top/bottom ordering prompts. VISION still uses its narrow
  private keep/recycle flow.
- Disarming Rake uses the narrow enter-play optional prompt path: after it
  successfully enters play, its controller may choose to destroy a public Gear.
  If accepted, a second owner-only Gear target choice destroys the selected
  friendly or enemy Gear to Trash. Attached Gear is detached and trashed, not
  returned to Base, and this does not run Deathknell.

Known gaps:
- Chain timing, Reaction timing, countering spells, optional/three-plus/conditional multi-target spells, full optional trigger ordering, replacement/prevention, and many spell-specific effects are not complete.
- Active-showdown `[Action]` play is lightweight: focused showdown participants can play supported Action cards or pass focus. Once both relevant players pass in succession, the showdown becomes ready for the attacker to resolve. A narrow chain-state foundation exists, with Gust as the only connected Reaction card; counterspell and broader response-card support remain deferred.

Test coverage:
- Rules validator keyword/target tests.
- Projection tests for private VISION logs.
- `GameEnginePlayCardTypeTest`

Priority: P0.

## Playing Gear

Status: Partial

Current implementation notes:
- The alpha Equipment lifecycle is finalized for the current single-battlefield
  model.
- Basic `[Equip]` gear is played from hand to Base first, then attached with a
  separate Equip action from Base to a friendly Unit/Champion in Base or at the
  battlefield.
- Champion-zone identity cards are not legal equip targets until they move into
  Base or the battlefield.
- Attached Gear remains in Base with an attachment link and is shown near its
  host for readability.
- Gear cannot move to the battlefield or fight as a unit.
- Non-equip gear is treated as unsupported.
- Gear attached to a unit/champion returns to Base and detaches when its host
  leaves public play, including death, return-to-hand effects, or a Champion
  returning to the Champion zone.
- Returning Gear to Base is not treated as the Gear dying and does not process
  Deathknell.

Known gaps:
- Official equipment timing, Equip payment/domain precision, Quick-Draw,
  Weaponmaster, replacement/reattachment edge cases, voluntary detach rules, and
  many card-specific gear effects are incomplete.

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
- `activeShowdown` now tracks the two-player alpha relevant players, current
  focused player, consecutive focus passes, and a `readyToResolve` gate.
- Focus starts with the attacker. The focused player may play a supported
  `[Action]` card or pass focus; playing an Action resets consecutive passes
  and advances focus. When both relevant players pass in succession, the
  attacker may resolve the simplified combat.
- Nested showdowns are blocked.
- Resolving clears `activeShowdown` and returns to normal MAIN actions.
- Legal-action visibility pauses normal main actions during an active showdown,
  exposes `PASS_SHOWDOWN_FOCUS` and supported Action `PLAY_CARD` only to the
  focused participant, and exposes `RESOLVE_SHOWDOWN` only after focus/pass is
  complete.

Known gaps:
- Non-combat showdowns, full priority/chain timing, staged combat conversion,
  initial attack/defend trigger chain, invitations, and reaction timing
  permissions remain simplified or deferred.
- Multiple battlefield showdowns are deferred until the post-alpha location
  model.

Test coverage:
- `GameEngineShowdownTest`
- `LegalActionsServiceTest`

Priority: P1.

## Combat Damage Assignment

Status: Partial

Current implementation notes:
- After showdown focus/pass is complete, `RESOLVE_SHOWDOWN` enters an
  `ASSIGN_DAMAGE` step instead of resolving combat immediately.
- The assigning player submits `ASSIGN_COMBAT_DAMAGE`; attacker assignments are
  stored first, then defender assignments resolve simultaneous damage.
- Server validation requires each side to assign all available combat Might,
  enforces Tank-priority lethal assignment, rejects duplicate source-target
  assignments, and allows excess damage on only one target after legal targets
  have lethal.
- The current client and RiftBot use deterministic Tank-first assignment
  helpers; full manual damage-splitting UI is deferred.
- Damage is simultaneous; killed units move to trash after both sides assign
  damage.
- Survivors heal during combat cleanup.
- ASSAULT / ASSAULT X is supported for descriptor-only starter units
  Daring Poro and Laurent Duelist.
- SHIELD, TANK priority, and STUN have partial support.

Known gaps:
- Fine-grained player-chosen damage UI, prevention/replacement, combat
  designation cleanup, and many official multi-unit edge cases are incomplete.
- Multiple battlefield combat is intentionally post-alpha work.

Test coverage:
- `CombatResolverTest`
- `GameEngineShowdownTest`
- `LegalActionsServiceTest`
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
- DEATHKNELL has keyword-driven trigger plumbing through `DeathTriggerService`:
  real Unit/Champion deaths fire after graveyard movement, bounce/return-to-hand
  does not fire, simultaneous combat deaths are batched deterministically, and
  card-specific Deathknell effects dispatch through dedicated handlers. Loyal
  Poro's full printed "didn't die alone" draw text and Lonely Poro's full
  printed "died alone" draw text are card-specific Supported;
  Scuttle Crab has a safe 1v1 alpha handler that privately reveals the
  opponent hand only to the Crab controller and clears that permission at the
  controller's end phase. XP and facedown viewing remain deferred.
- Simple Recruit token creation exists through `TokenFactory` for starter-deck
  scripts. Noxian Drummer's move-to-battlefield trigger and Vanguard Captain's
  Legion token trigger are card-specific Supported in the single-battlefield
  alpha.
- Stellacorn Herder's full printed movement trigger is card-specific
  Supported in the single-battlefield alpha: Base -> battlefield and
  battlefield -> Base/recall movement each draw 1 privately, while play from
  hand, return to hand, trash/death, setup/import, hidden transitions, and
  same-zone repositioning do not trigger it.
- `TriggerEvent`, `TriggerDispatcher`, and `TriggerHandler` provide a small
  deterministic trigger framework for alpha events. Movement triggers for
  Noxian Drummer and Stellacorn Herder now run through this dispatcher.

Known gaps:
- The handler registry is a scaffold; several tracked keywords still need
  dedicated handlers before they can be called fully supported.
- The complete official keyword list, dependent keywords, inactive text, conditional permissions, XP/Hunt/Level, and full action/reaction behavior are incomplete.
- Some legacy placeholder keywords remain in early hard-coded effects and should be audited against current official names.
- The trigger framework does not create official priority windows, optional
  trigger ordering flows, or broad simultaneous chain items yet. A narrow
  chain-state foundation exists for future integration.
- Scuttle Crab's Deathknell reveal/facedown/XP text, general token definitions,
  official token cleanup, and broad non-Recruit token creation effects remain
  incomplete.

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
- Descriptor-only starter Units can be marked Supported only after direct
  card-specific behavior tests.
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
- Optional triggers and may choices are partial: private yes/no,
  optional-payment, and narrow follow-up board-target prompts exist, but complex
  targeting decisions, linked choices, trigger ordering, and real card chain items are
  incomplete.

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
- The frontend consumes `state.legalActions` to gate Battlefield selection, mulligan/keep, pass phase, play card, move to battlefield, rune actions, chain focus passing/resolution, showdown focus passing, gated active showdown resolution, and sandbox-only controls.
- `RulesValidator` remains the source of enforcement.
- The service intentionally does not claim support for card-specific or reaction windows that are not implemented.
- Currently modeled windows: Battlefield selection, mulligan, basic phase pass, Main Phase active-player actions, Stacked Deck's narrow chain opener, chain focus/pass/resolve, focused participant supported Action play/pass during active showdowns, gated active showdown resolution, and SANDBOX-only developer actions.

Known gaps:
- Actions are not card-instance-specific.
- Full Reaction windows and official chain priority remain future work.
- Card-specific legal action prompts are not generated.
- Target-specific and payment-specific legal action generation is incomplete.
- Chain/timing permissions for real card responses and full priority handling are future work.

Test coverage:
- `LegalActionsServiceTest`
- `GameStateProjectionServiceTest`

Priority: P1.

## UI Guidance

Status: Partial

Current implementation notes:
- The client uses projected `legalActions` for major game-action affordances instead of relying only on local phase guesses.
- Unavailable phase/showdown/chain controls are hidden.
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
