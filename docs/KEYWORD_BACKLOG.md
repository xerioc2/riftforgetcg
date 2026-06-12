# RiftForge Keyword Backlog

Last audit: 2026-06-11

This document tracks keyword and keyword-like rules support. "Supported" is
reserved for behavior that is implemented and tested in RiftForge. A keyword
can have a supported narrow behavior while cards containing that keyword remain
Partial or Unsupported because their full rules text needs more scripting.

Sources checked:
- `docs/RULES_COVERAGE.md`
- `docs/SUPPORTED_CARDS.md`
- `docs/CARD_RULES_BACKLOG.md`
- `src/lib/starterDecks.ts`
- `src/lib/cardKeywords.ts`
- `EffectHandlerRegistry.TRACKED_KEYWORDS`
- `CardDataService` keyword parsing
- `RulesValidator`
- `LegalActionsService`
- `CombatResolver`
- `GameEngine`
- Current tests under `server/src/test`
- Official Riftbound Rules Hub / Core Rules references cited in
  `docs/RULES_COVERAGE.md`
- Official Unleashed patch notes and FAQ already cited in the rules docs

## Supported

No keyword is currently complete enough to call fully Supported across all
official edge cases. The safest current items are narrow, tested behaviors that
still live under Partial support in the broader matrix.

## Partial

| Keyword / word | Current implementation status | Starter deck source cards | Behavior needed | Risk | Priority | Minimum tests before Supported |
| --- | --- | --- | --- | --- | --- | --- |
| ASSAULT / ASSAULT X | Partial. `CombatStatsService` adds Assault only while attacking. Plain `Assault` defaults to +1; valued parsing accepts compact and spaced numeric forms such as `Assault 2` and `ASSAULT2`. | Daring Poro, Laurent Duelist | Card-specific text beyond the combat descriptor still needs individual scripts/tests. | Low | P1 | Direct real-card combat tests exist for Daring Poro and Laurent Duelist; add integration tests when cards are promoted. |
| SHIELD / SHIELD X | Partial. `CombatStatsService` adds Shield only while defending. Plain `Shield` defaults to +1; `Shield 2` grants +2 through temporary keyword grants or printed keywords. | Fortified Position grants Shield 2 | Fortified Position's defend trigger, target choice, and battlefield timing are not implemented yet. | Low | P1 | Direct combat tests exist for a Shield 2 temporary grant; add Fortified Position trigger tests before card promotion. |
| TANK | Partial. Combat damage assignment prioritizes Tank before non-Tank units. | None in current starter decks | Full damage assignment choice model and multiple-attacker/multiple-defender edge cases. | Medium | P2 | Lethal assignment tests with Tank plus non-Tank, multiple Tanks, insufficient damage, and simultaneous death. |
| STUN / STUNNED | Partial. Combat resolver treats Stunned cards as dealing 0 combat damage. | Facebreaker mentions Stun | Model Stun as an action word, duration, source targeting, cleanup, and UI feedback. | Medium | P1 | Spell/effect applies Stunned; Stunned card deals 0; Stun expires at correct cleanup; invalid targets rejected. |
| VISION | Partial. Basic private top-main-deck peek and keep/recycle choice exists, with private projection/log handling. | No current starter deck cards directly use Vision | Full Predict-style multi-card look/recycle/order flow if card text requires it. | Medium | P2 | Private projection tests, keep/recycle top card tests, opponent leak tests, multi-card Predict tests when implemented. |
| HIDDEN | Partial. Validator blocks opponent targeting of enemy Hidden cards at Base. | Tideturner, Facebreaker | Official hide/react timing, facedown state, hidden play permissions, reveal, and payment. | High | P1 | Hidden card cannot be targeted at Base; can be revealed/played only in legal windows; hidden information is not exposed. |
| LEGION | Partial. `GameEngine` checks whether another card was played earlier this turn. | Vanguard Captain | Dependent keyword text, inactive text rules, token creation, and source-specific effect. | Medium | P1 | Legion inactive before prior main-deck card; active after prior main-deck card; Vanguard Captain creates two Recruit tokens. |
| TEMPORARY | Partial. Temporary cards expire at the start of their controller's next Beginning Phase. | None in current starter decks | Token/created-object integration and direct handler coverage. | Medium | P2 | Temporary unit survives until controller's next Beginning; expires before scoring; opponent turn does not expire it. |
| ACCELERATE | Partial. Server accepts/validates an accelerate flag and client can prompt when enough extra energy exists. | No current starter deck cards directly use Accelerate | Payment correctness and card-specific Accelerate effects. | Medium | P2 | Extra payment required; enters ready only when paid; cannot accelerate without extra energy/domain payment. |
| AMBUSH | Partial/heuristic. Some GameEngine behavior exists, but official Ambush is tied to Reaction timing and battlefield permissions. | Stalking Wolf | Conditional Reaction window, additional costs, kill requirement, and legal battlefield play. | High | P1 | Ambush can be played only in legal reaction window; additional cost paid atomically; target battlefield restrictions enforced. |
| DEFLECT | Partial/heuristic. Some targeting tax/redirection behavior exists. | Irelia - Fervent | Exact opponent payment requirement, spell/ability target policy, choice handling, and UI prompts. | High | P2 | Opponent must pay required rune/power to choose Deflect card; no tax for self; invalid payment rejects. |
| GANKING / GANKING X | Partial/heuristic. Some battlefield entry/combat logic exists. | None in current starter decks | Exact comparison timing, value parsing, and official edge cases. | Medium | P2 | Ganking bonus applies only at correct battlefield/combat timing and only when comparison condition is true. |
| WEAPONMASTER / WEAPONMASTER X | Partial/heuristic. Gear attachment can add permanent Might. | No starter deck card has Weaponmaster, but starter decks use Gear | Full equipment model, attach legality, value parsing, detach/death cleanup, and card tests. | High | P2 | Gear attach increases Might; detach/death removes or preserves modifier according to official rules; invalid attach rejects. |
| EQUIP | Partial. Basic `[Equip]` gear is validated as a friendly battlefield Unit/Champion target, attaches through the generic play path, cannot fight as a unit, and attached gear moves to trash when its host is destroyed in combat. Unsupported non-equip gear remains blocked. | Guardian Angel, Boots of Swiftness | Official attach timing, replacement/reattachment edge cases, payment-domain precision, bounce/detach edge cases, and gear card text. | High | P1 | Gear attaches only to legal friendly unit; gear follows host death/bounce/trash; unattached gear cannot fight; unsupported gear stays blocked. |
| MIGHTY | Partial. `CombatStatsService` exposes a central Mighty helper for Unit/Champion cards with effective idle Might 5 or greater, including temporary and permanent Might modifiers. | Sunken Temple, Fiora - Grand Duelist, Fiora - Worthy | Threshold-crossing events, "becomes Mighty" trigger timing, and source-specific choices remain unimplemented. | Medium | P1 | Helper tests cover base Might, temporary modifier crossing, Champion support, and non-combat cards not being Mighty. |

## Unsupported

| Keyword / word | Current implementation status | Starter deck source cards | Behavior needed | Risk | Priority | Minimum tests before Supported |
| --- | --- | --- | --- | --- | --- | --- |
| ACTION | Unsupported as a timing system. Some Action cards are playable through simplified Main/showdown logic, but no official chain/action window exists. | Ride The Wind, Stacked Deck, Keeper's Verdict, Facebreaker | Action timing, showdown windows, chain item creation, legal response windows. | High | P0 | Action spell legal on your turn/showdowns only; resolves through chain; non-active illegal unless rules allow. |
| REACTION | Unsupported as a timing system. Non-active spell play is narrow and should not be treated as official Reaction support. | Defy, Discipline, Defiant Dance, En Garde, Gust, Not So Fast, Star-Crossed, Stalking Wolf, Riposte | Chain, priority, before-resolution timing, counterspell targets, hidden/ambush permissions. | High | P0 | Reaction legal during opponent/chain windows; illegal outside them; counterspell can target spell/ability chain item. |
| DEATHKNELL | Unsupported. Registry explicitly reports no handler. | Scuttle Crab, Loyal Poro | Death trigger queue, optional/mandatory handling, choosing opponent, reveal/facedown access, XP/draw effects. | High | P1 | Deathknell fires when unit dies; does not fire on bounce; Scuttle/Loyal Poro effects resolve with hidden info protected. |
| HUNT / HUNT X | Unsupported. XP resource is not modeled. | Crowd Favorite | XP resource, conquer/hold trigger, valued Hunt, interaction with Level. | Medium | P2 | Hunt grants XP on conquer/hold; no XP on ordinary combat survival; value X honored. |
| LEVEL / LEVEL X | Unsupported. Dependent keyword text and XP thresholds are not modeled. | No current starter deck card directly uses Level | XP thresholds, inactive text, cards gaining/losing Level active state. | High | P2 | Card below XP threshold lacks Level text/effect; at threshold gains text/effect; XP changes update legality. |
| BUFF | Unsupported as official action/state. The engine has loose permanent Might modifiers, but not official Buff action handling. | Adaptatron, Crowd Favorite | Buff marker/state, "if not buffed" policy, source ownership, and removal rules. | Medium | P1 | Buff adds +1 Might only if no buff; repeated Buff blocked or no-op per rules; buff persists/clears correctly. |
| PREDICT | Unsupported. Vision is only a narrow one-card peek/recycle flow. | None directly, but Stacked Deck is a similar top-card selection shape | Look at N cards, recycle any number, order the rest, hidden choice UI. | High | P2 | Player privately sees cards; opponent does not; chosen cards recycle; remaining order preserved. |
| XP | Unsupported player/resource system. | Scuttle Crab, Crowd Favorite | Player XP state, projection, gain/spend, validation, reset/match history. | Medium | P2 | XP gained from supported triggers; XP spending validates amount; projection does not leak hidden choices. |
| UNIQUE | Unsupported. Mentioned in official Unleashed updates, not modeled in current engine. | None in current starter decks | Deck/board uniqueness and exact official scope. | Medium | P3 | Duplicate unique object behavior follows official rule; deck validation if applicable. |
| REPLACE | Unsupported. | None in current starter decks | Swap/replacement action model. | Medium | P3 | Replace action changes objects atomically and respects legality. |
| CREATE | Unsupported as general token creation. | Noxian Drummer, Vanguard Captain need Recruit tokens | Token definitions, token ownership, zones, death cleanup, projection, and card-source metadata. | Medium | P1 | Token enters correct zone, has correct stats/type, can fight/die, is excluded from deck counts. |
| PREVENT | Unsupported. | No current starter deck card directly uses Prevent | Damage prevention/replacement effects and timing. | High | P3 | Prevention effect reduces damage at correct timing and expires correctly. |
| COPY | Unsupported. | No current starter deck card directly uses Copy | Copyable values, duration, hidden/public info, and interactions with tokens/attachments. | High | P3 | Copied object has correct visible characteristics and duration. |
| LINKED INSTRUCTIONS / LINKED ABILITIES | Unsupported. | Repeat/counter/card-selection style texts may need this later | Choice/result linkage and multi-part effect sequencing. | High | P3 | Linked choice affects only the linked instruction and does not leak hidden info. |

## Needs Official Confirmation

These terms appear in local code, old placeholder effects, or broad docs but need
an official wording audit before any support claim.

| Term | Why it needs confirmation | Suggested action |
| --- | --- | --- |
| OVERWHELM | Legacy placeholder handler/effect exists, but current official keyword status is unclear in this repo. | Verify against current Rules Hub before keeping or deleting handler. |
| RUSH | Legacy placeholder handler/effect exists, but current official keyword status is unclear in this repo. | Verify against current Rules Hub before keeping or deleting handler. |
| TOUGH | Legacy placeholder handler/effect exists, but current official keyword status is unclear in this repo. | Verify against current Rules Hub before keeping or deleting handler. |
| QUICK-DRAW | Frontend description and GameEngine heuristics exist, but official keyword/action status needs confirmation. | Audit real card data and rules text, then add tests or remove from support claims. |
| REPEAT | GameEngine has an extra-card-play flag path, but official linked-instruction timing needs confirmation. | Audit source cards and write explicit tests before promotion. |
| BACKLINE | Combat priority exists, but it is not tracked in `EffectHandlerRegistry.TRACKED_KEYWORDS`. | Confirm official wording and add handler/tests if it remains current. |

## Implementation Priority

1. P0 timing words:
   - ACTION
   - REACTION
   - chain/priority item model
2. P1 low-risk combat and starter deck keywords:
   - Add integration tests for cards whose only remaining text is a supported combat descriptor.
   - Wire MIGHTY threshold-crossing events and source-specific triggers.
   - Add STUN action application for Facebreaker-style effects.
3. P1 starter deck triggers:
   - DEATHKNELL
   - CREATE tokens
   - LEGION token effect
   - BUFF marker
4. P1 attachments:
   - EQUIP target/payment legality
   - attachment cleanup
5. P2 resource systems:
   - XP
   - HUNT
   - LEVEL
6. P2 private selection:
   - Promote narrow Vision flow toward full PREDICT/card ordering.
7. P3 broader official systems:
   - UNIQUE
   - REPLACE
   - PREVENT
   - COPY
   - linked instructions/abilities
