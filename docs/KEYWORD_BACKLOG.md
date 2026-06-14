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
| ASSAULT / ASSAULT X | Supported for descriptor-only cards. `CombatStatsService` adds Assault only while attacking. Plain `Assault` defaults to +1; valued parsing is case-insensitive and accepts compact and spaced numeric forms such as `Assault 2` and `ASSAULT2`. | Daring Poro, Laurent Duelist | Cards with additional Assault-related text still need individual scripts/tests. | Low | P1 | Direct real-card combat tests exist for Daring Poro and Laurent Duelist, including no-bonus defending coverage; both descriptor-only starter cards are now marked Supported. |
| SHIELD / SHIELD X | Partial. `CombatStatsService` adds Shield only while defending. Plain `Shield` defaults to +1; `Shield 2` grants +2 through temporary keyword grants or printed keywords. | Fortified Position grants Shield 2 | Fortified Position's defend trigger, target choice, and battlefield timing are not implemented yet. | Low | P1 | Direct combat tests exist for a Shield 2 temporary grant, including no-bonus attacking coverage; add Fortified Position trigger tests before card promotion. |
| TANK | Partial. Combat damage assignment prioritizes Tank before non-Tank units. | None in current starter decks | Full damage assignment choice model and multiple-attacker/multiple-defender edge cases. | Medium | P2 | Lethal assignment tests with Tank plus non-Tank, multiple Tanks, insufficient damage, and simultaneous death. |
| STUN / STUNNED | Partial. Combat resolver treats Stunned cards as dealing 0 combat damage. | Facebreaker mentions Stun | Model Stun as an action word, duration, source targeting, cleanup, and UI feedback. | Medium | P1 | Spell/effect applies Stunned; Stunned card deals 0; Stun expires at correct cleanup; invalid targets rejected. |
| VISION | Partial. Basic private top-main-deck peek and keep/recycle choice exists, with private projection/log handling. The newer pending-choice framework covers simple private yes/no and pay-1 prompts but does not yet replace Vision's special flow. | No current starter deck cards directly use Vision | Full Predict-style multi-card look/recycle/order flow if card text requires it. | Medium | P2 | Private projection tests, keep/recycle top card tests, opponent leak tests, multi-card Predict tests when implemented. |
| HIDDEN | Partial. Cards with `[Hidden]` can be hidden from hand during Main by tapping a ready own rune. Hidden cards move to a dedicated hidden zone, are masked to opponents/spectators, are not targetable, and do not fight or move through normal board actions. | Tideturner, Facebreaker | Official later play/react timing, reveal permissions, priority windows, hidden-card payment edge cases, and card-specific scripts. | High | P1 | Hidden card leaves hand, owner sees identity, opponent/spectator see only a masked hidden card/count, hidden card cannot be targeted/moved/fight, and later play remains blocked until timing exists. |
| DEATHKNELL | Partial overall. A keyword-driven death-event pipeline detects real Unit/Champion deaths, skips bounce/return-to-hand, batches simultaneous combat deaths deterministically, and dispatches card-specific effects through Deathknell handlers. Loyal Poro's full printed draw trigger and Lonely Poro's inverse "died alone" draw trigger are card-specific Supported; Scuttle Crab has a safe 1v1 alpha handler that privately reveals the opponent hand to the Crab controller. | Scuttle Crab, Loyal Poro, Lonely Poro | Multiplayer opponent choice, facedown access, XP, optional/mandatory choice handling, and broader card-specific scripts. | High | P1 | Deathknell fires when a unit dies; does not fire on bounce, Champion-zone return, reposition, or Gear cleanup; Loyal Poro draws exactly once when it did not die alone; Lonely Poro draws exactly once when no other friendly Unit/Champion was at the death location; Scuttle Crab reveal is private and clears at end phase while XP/facedown remain deferred. |
| LEGION | Partial overall. `GameEngine` checks whether another card was played earlier this turn, resets the flag on turn change, and Vanguard Captain's full printed two-Recruit trigger is card-specific Supported. | Vanguard Captain | Dependent inactive text rules, main-deck-only nuance, and broader source-specific effects. | Medium | P1 | Legion inactive before prior card; active after prior card; turn-change reset; Vanguard Captain creates exactly two Recruit tokens. |
| TEMPORARY | Partial. Temporary cards expire at the start of their controller's next Beginning Phase. | None in current starter decks | Token/created-object integration and direct handler coverage. | Medium | P2 | Temporary unit survives until controller's next Beginning; expires before scoring; opponent turn does not expire it. |
| ACCELERATE | Partial. Server accepts/validates an accelerate flag and client can prompt when enough extra energy exists. | No current starter deck cards directly use Accelerate | Payment correctness and card-specific Accelerate effects. | Medium | P2 | Extra payment required; enters ready only when paid; cannot accelerate without extra energy/domain payment. |
| ACTION | Partial. Supported Action cards can be played by the active player in Main and by showdown participants during the simplified active-showdown action window. Unsupported Action effects remain blocked and no chain/response model exists. | Ride The Wind, Stacked Deck, Keeper's Verdict, Facebreaker | Formal priority, chain item creation, legal response windows, and card-specific Action prompts. | High | P0 | Supported Action spell legal on your turn and participant showdown window; non-Action and Reaction cards rejected during showdown; unsupported Action effects stay blocked. |
| AMBUSH | Partial. Alpha supports clean Ambush Unit cards as a direct Main-phase play to the battlefield when you already have a friendly Unit/Champion there; the unit enters ready and starts the simplified showdown if opposed. | Stalking Wolf | Conditional Reaction window, additional costs, kill requirement, and broader battlefield-location rules. Stalking Wolf remains blocked because its additional kill cost is not implemented. | High | P1 | Ambush battlefield play requires a friendly unit; non-Ambush hand-to-battlefield play rejects; additional-cost Ambush rejects; reaction-timing Ambush remains unsupported. |
| DEFLECT | Partial/heuristic. Some targeting tax/redirection behavior exists. | Irelia - Fervent | Exact opponent payment requirement, spell/ability target policy, choice handling, and UI prompts. | High | P2 | Opponent must pay required rune/power to choose Deflect card; no tax for self; invalid payment rejects. |
| GANKING / GANKING X | Partial/heuristic. Some battlefield entry/combat logic exists. | None in current starter decks | Exact comparison timing, value parsing, and official edge cases. | Medium | P2 | Ganking bonus applies only at correct battlefield/combat timing and only when comparison condition is true. |
| WEAPONMASTER / WEAPONMASTER X | Unsupported/deferred. Previous heuristic Might-on-attach behavior is intentionally disabled until full Gear timing/modifier cleanup is implemented. | No starter deck card has Weaponmaster, but starter decks use Gear | Full equipment modifier model, value parsing, detach/death cleanup, and card tests. | High | P2 | Gear attach applies/removes Weaponmaster modifiers exactly once and only when official timing permits. |
| EQUIP | Partial. The alpha Equip lifecycle is implemented and tested: Gear is played to Base first, then attached with a separate Equip action from Base to a friendly Unit/Champion in Base or at the battlefield. Attached Gear stays in Base with an attachment link, cannot fight or move directly, and returns to Base when its host leaves public play without triggering Gear Deathknell. Champion-zone identity cards are not legal equip targets. Unsupported non-equip gear remains blocked. | Guardian Angel, Boots of Swiftness | Official Equip payment/domain precision, replacement/reattachment edge cases, Quick-Draw, Weaponmaster, voluntary detach rules, and gear card text. | High | P1 | Gear plays to Base, equips only from Base to legal friendly Unit/Champion, returns to Base on host death/bounce/Champion-zone return, unattached gear cannot fight, attached gear cannot be moved directly, unsupported gear stays blocked. |
| MIGHTY | Partial. `CombatStatsService` exposes a central Mighty helper for Unit/Champion cards with effective Might 5 or greater in the requested context, including temporary/permanent Might modifiers and Assault/Shield while attacking/defending. | Sunken Temple, Fiora - Grand Duelist, Fiora - Worthy | Threshold-crossing events, "becomes Mighty" trigger timing, and source-specific choices remain unimplemented. | Medium | P1 | Helper tests cover base Might, temporary modifier crossing, Assault/Shield context crossing, Champion support, and non-combat cards not being Mighty. |

## Unsupported

| Keyword / word | Current implementation status | Starter deck source cards | Behavior needed | Risk | Priority | Minimum tests before Supported |
| --- | --- | --- | --- | --- | --- | --- |
| REACTION | Unsupported as a timing system. Non-active spell play is narrow and should not be treated as official Reaction support. | Defy, Discipline, Defiant Dance, En Garde, Gust, Not So Fast, Star-Crossed, Stalking Wolf, Riposte | Chain, priority, before-resolution timing, counterspell targets, hidden/ambush permissions. | High | P0 | Reaction legal during opponent/chain windows; illegal outside them; counterspell can target spell/ability chain item. |
| HUNT / HUNT X | Unsupported. XP resource is not modeled. | Crowd Favorite | XP resource, conquer/hold trigger, valued Hunt, interaction with Level. | Medium | P2 | Hunt grants XP on conquer/hold; no XP on ordinary combat survival; value X honored. |
| LEVEL / LEVEL X | Unsupported. Dependent keyword text and XP thresholds are not modeled. | No current starter deck card directly uses Level | XP thresholds, inactive text, cards gaining/losing Level active state. | High | P2 | Card below XP threshold lacks Level text/effect; at threshold gains text/effect; XP changes update legality. |
| BUFF | Unsupported as official action/state. The engine has loose permanent Might modifiers, but not official Buff action handling. | Adaptatron, Crowd Favorite | Buff marker/state, "if not buffed" policy, source ownership, and removal rules. | Medium | P1 | Buff adds +1 Might only if no buff; repeated Buff blocked or no-op per rules; buff persists/clears correctly. |
| PREDICT | Partial foundation. Pending choices can now present private top-deck card options and resolve a top/bottom ordering choice with top choices placed LIFO. No broad Predict card scripts are wired yet. | None directly, but Stacked Deck uses the same private top-card selection shape | Official Predict timing, card-specific trigger wiring, optional partial recycling, and exact ordering UX for all Predict cards. | High | P2 | Player privately sees cards; opponent/spectator do not; all revealed cards must be assigned; top choices resolve LIFO; bottom choices do not leak identities. |
| XP | Unsupported player/resource system. | Scuttle Crab, Crowd Favorite | Player XP state, projection, gain/spend, validation, reset/match history. | Medium | P2 | XP gained from supported triggers; XP spending validates amount; projection does not leak hidden choices. |
| UNIQUE | Unsupported. Mentioned in official Unleashed updates, not modeled in current engine. | None in current starter decks | Deck/board uniqueness and exact official scope. | Medium | P3 | Duplicate unique object behavior follows official rule; deck validation if applicable. |
| REPLACE | Unsupported. | None in current starter decks | Swap/replacement action model. | Medium | P3 | Replace action changes objects atomically and respects legality. |
| CREATE | Partial for the broader token system; card-specific Supported for Noxian Drummer and Vanguard Captain. `TokenFactory` can create public 1 Might / 1 health Recruit Unit tokens that can exist at Base/Battlefield, fight, and stay out of deck pools. | Noxian Drummer, Vanguard Captain need Recruit tokens | General token definitions, official cleanup/disappear policy, token card-source metadata, and non-Recruit token support. | Medium | P1 | Token enters correct zone, has correct stats/type, can fight/die, is excluded from deck counts. |
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
| QUICK-DRAW | Unsupported/deferred. No current rules behavior is implemented. | Audit real card data and rules text, then add tests before support claims. |
| REPEAT | GameEngine has an extra-card-play flag path, but official linked-instruction timing needs confirmation. | Audit source cards and write explicit tests before promotion. |
| BACKLINE | Combat priority exists, but it is not tracked in `EffectHandlerRegistry.TRACKED_KEYWORDS`. | Confirm official wording and add handler/tests if it remains current. |

## Implementation Priority

1. P0 timing words:
   - Expand ACTION beyond the current open participant showdown window
   - REACTION
   - chain/priority item model
2. P1 low-risk combat and starter deck keywords:
   - Continue promoting only cards whose full printed text is a supported combat descriptor or no-text basic unit.
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
