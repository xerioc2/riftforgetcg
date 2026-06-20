# RiftForge Missing Rules Backlog

Last audit: 2026-06-11

This backlog lists missing or partial Riftbound support in implementation order,
not official rules order. It is intentionally conservative. "Supported" means
implemented and tested in this repository. "Partial" means the common playtest
path exists, but official edge cases or card-specific scripts are missing.

RiftForge's alpha playtest target now uses a narrow active-lane Battlefield
foundation for 1v1 games. The remaining deferred official-style work is the full
multi-location Battlefield model: Battlefield effects, hidden slots, richer
"here" targeting, scoring nuance, bot strategy, and card-specific location
rules. That is separate from 3+ player multiplayer support.

Sources checked:
- `docs/RULES_COVERAGE.md`
- `docs/SUPPORTED_CARDS.md`
- `docs/CARD_RULES_BACKLOG.md`
- `docs/REACTION_EQUIPMENT_AUDIT.md`
- `docs/KEYWORD_BACKLOG.md`
- `src/lib/starterDecks.ts`
- `RulesValidator`
- `LegalActionsService`
- `CombatResolver`
- `GameEngine`
- `CardDataService`
- `CardSupportService`
- `EffectHandlerRegistry`
- keyword handlers and existing tests
- Official Rules Hub / Core Rules references cited in `docs/RULES_COVERAGE.md`
- Official Unleashed patch notes and FAQ already cited in repo docs

## P0: Blocks Normal 1v1 Playtest Games

### Turn Structure and Timing

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Full action/reaction window model | Partial | Starter decks contain many `[Action]` and `[Reaction]` spells. The alpha supports active-player Main actions, focused participant supported Action cards during active showdowns, focused-showdown supported targeted Reactions, showdown focus/pass, Stacked Deck and simple public `Draw 1` spells as narrow chain openers, Gust/Discipline/En Garde/Defiant Dance/Flash as narrow board-target Reactions, Defy/Not So Fast/Hard Bargain as narrow chain-target Reactions, foundational Hidden display/projection, and a narrow Main-phase Ambush battlefield-play path. Broad counterspells, Ambush-as-Reaction, later Hidden play, formal priority, and most reaction tricks are still not rules-correct. | `GameEngine`, `RulesValidator`, `LegalActionsService`, `MoveRequest`, `GameBoard.tsx`, `stompGame.ts` | Targeted supported Reactions can use own-turn Main, focused showdown, or focused chain windows; counter-only Reactions still require an active legal chain target. |
| Chain / pending spell or ability objects | Partial foundation | `LiveGameState.chainState` can hold chain items with focused player, pass count, ready-to-resolve state, lifecycle status, counter-ready metadata, public-safe target summaries, and viewer-aware projection masking for private/controller-only sources and targets. Human chain focus is bluff-safe: empty response windows hold until the focused player passes or locally opts into auto-pass, and only bot players may be server auto-passed. `PriorityWindowService` now centralizes the narrow alpha opt-in for opening chain windows and chain item metadata. The UI can show a public-safe chain panel plus local priority-stop toggles. Defy has a narrow public-spell counter path, Not So Fast has a narrow public targeted-spell counter path, and rune innate Energy/Power actions do not enter or open the chain. Choice-based/private spells, Riposte, hidden reactions, and broad ability/spell counters remain deferred. | `LiveGameState`, `PriorityWindowService`, `GameEngine`, `RulesValidator`, `LegalActionsService`, `GameStateProjectionService`, `BotService`, `GameBoard.tsx` | Playing Not So Fast can counter an enemy Gust only when it targets a friendly Unit/Gear and cannot counter untargeted Stacked Deck. |
| Card-specific legal actions | Partial | `legalActions` are high-level and phase-based, not card/target/payment-specific. The UI can still offer a card that the server rejects. | `LegalActionsService`, `GameStateProjectionService`, frontend action controls | Projection for a hand card includes playable/unplayable reason based on current phase and available targets. |

### Rune and Payment Rules

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Domain/power payment validation | Partial | Constructed decks use domain runes and many card costs include more than generic energy. Current payment is not complete enough for competitive play. | `RulesValidator`, `GameEngine`, `CardDataService`, `MoveRequest`, payment UI | A card requiring Order power cannot be played using only Body runes. |
| Public in-play Rune display/debugging | Partial | Channeled Rune card identity is now retained on public `RuneState`, projected safely, rendered in the board resource row, and included in copied debug info. Rune deck pools are shuffled during setup/reset, while private rune deck contents remain hidden. Full official rune payment and recycling nuance remains incomplete. | `RuneState`, `GameEngine`, `GameStateProjectionService`, `GameBoard.tsx`, `debugInfo.ts` | A channeled Calm Rune renders as that card for both players while `runeDeckPool` stays absent from projection/debug info. |
| Atomic selected-rune payment | Partial | Tapping/discarding runes separately is playable, but true card payment should validate and consume selected runes atomically with `PLAY_CARD`. | `PlayCardMove`, `RulesValidator`, `GameEngine`, `GameBoard.tsx` | Failed play leaves all selected runes unchanged; successful play exhausts/recycles exactly selected runes. |
| Cost modifiers and alternate/additional costs | Partial | Hidden now uses a narrow "tap one ready own rune" foundation cost, and Ambush cards with unsupported additional costs are explicitly blocked. Later Hidden play, Spectral Matron, counterspells, equipment, and Stalking Wolf's sacrifice cost still need full extra/alternate cost support. | `RulesValidator`, `GameEngine`, card effect handlers | Stalking Wolf requires its additional kill cost and rejects if no legal sacrifice exists. |

### Targeting and Choice Prompts

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Explicit target model | Partial | Current target validation is mostly heuristic. Spells need target type, ownership, location, and count rules. | `MoveRequest`, `RulesValidator`, `GameEngine`, `CardDataService`, frontend target UI | Gust can target only a unit at a battlefield with 3 or less Might. |
| Multi-target and paired choices | Partial | Narrow staged targeting supports one friendly Unit/Champion plus one enemy Unit/Champion for paired return-to-hand effects, Defiant Dance's boosted/weakened Unit/Champion pair, and Flash's one-or-two friendly Unit/Champion recall targets. Facebreaker, Switcheroo, optional choice prompts beyond Flash's second target, and broader multi-target scripting remain unsupported. | `MoveRequest`, `RulesValidator`, `GameEngine`, target UI | Defiant Dance rejects selecting the same target for both effects unless card text allows it. |
| Private choice prompts | Partial | A generic `pendingChoice` model now supports owner-only yes/no, optional-payment, Stacked Deck-style top-3 card selection, and Predict-style top/bottom ordering. Vision still uses a narrow private keep/recycle flow. Keeper's Verdict, Deathknell reveal, multi-target decisions, and linked choices still need richer prompts. | `LiveGameState`, `GameEngine`, `GameStateProjectionService`, `stompGame.ts`, modal UI | Keeper's Verdict privately asks the affected owner to put a selected unit on top or bottom without leaking hidden deck information. |

### Movement and Battlefields

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Movement permissions by card/effect | Partial | Generic free movement is sandbox-only. The alpha now has explicit normal Unit/Champion movement from Base to a visible battlefield lane and from battlefield back to Base, plus effect-driven movement, but many card-specific movement and location-swap permissions are still not card-accurate. | `RulesValidator`, `GameEngine`, effect handlers | Tideturner swaps only with a friendly unit at another legal location and preserves both legal zones. |
| Active-lane contested state | Partial | Showdown, control, conquer, and hold are keyed by active Battlefield location ids for the current 1v1 lane foundation. Battlefield card effects, hidden slots, and official location nuance remain deferred. | `GameEngine`, `LiveGameState`, `CombatResolver` | A lane becomes contested only when both players have units at that same location and returns to controlled after showdown cleanup. |

### Showdown and Combat

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Showdown focus/pass v1 | Partial | `activeShowdown` now tracks relevant players, focused player, consecutive passes, and a `readyToResolve` gate. Focused participants can play supported `[Action]` cards, play supported targeted Reactions, or pass; the attacker cannot resolve until both relevant players pass in succession. Official priority/reaction opportunities during showdowns are not fully modeled. | `GameEngine`, `LegalActionsService`, `RulesValidator`, `GameBoard.tsx` | Attacker focus starts a contested showdown, attacker pass gives defender focus, defender pass marks the showdown ready to resolve, and early resolve is rejected. |
| Combat damage assignment UI precision | Partial | Server-side assignment now exists after showdown focus/pass and uses a player damage pool from all eligible units at the active location. Validation enforces all-damage, Tank-first, lethal-before-spread, duplicate-target, and excess policies, and combat death uses Might threshold instead of health. The current client and RiftBot use a projected server-planned deterministic Tank-first assignment, so fully manual player damage splitting remains future work. | `CombatResolver`, `RulesValidator`, `MoveRequest`, `GameBoard.tsx` | Player manually assigns legal lethal damage to one Tank and chooses where allowed excess damage goes. |
| Combat cleanup and modifiers | Partial | Temporary Might, Stun, damage, and battlefield cleanup need exact timing for many cards. | `CombatResolver`, `GameEngine`, `CardZoneService` | A temporary combat modifier expires after combat, while permanent Buff remains. |

### Scoring and Winning

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Official winning check timing | Partial | Conquer final-point restriction exists, but official cleanup/tie/multiplayer win timing is not complete. | `GameEngine`, `GameEngineScoringTest`, match history | Player reaches target from legal Hold point and wins only at the correct check timing. |

## P1: Needed for Starter Decks to Feel Correct

### Attachments / Gear

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Equip target/payment rules | Partial | Guardian Angel and Boots of Swiftness are starter deck cards. Basic play-to-Base/equip-from-Base lifecycle and printed Equip rune payments exist with strict friendly public Unit/Champion target validation. Gear play cost and Equip cost are separate, but official timing and replacement edge cases are still generic. | `RulesValidator`, `GameEngine`, `CardZoneService`, payment UI | Boots requires the correct Chaos power payment and cannot attach outside the official Equip window. |
| Attachment lifecycle | Partial | Gear now follows host movement/death/return-to-hand cleanup deterministically and the board displays host attachment labels, but voluntary detach, replacement, and reattachment edge cases are not fully modeled. | `CardZoneService`, `CombatResolver`, `GameEngine`, `GameBoard.tsx` | Reattaching a second gear handles the official replacement rule and clears stale attachment IDs. |
| Equipment stat modifiers | Partial foundation | Effective stat calculation can now include attached Gear modifiers from explicit support metadata, and server projection exposes effective public Unit/Champion stats for UI/debug display. No current audited starter Gear has an enabled stat modifier entry, so unsupported Gear still grants no fake bonus. | `EquipmentStatModifierRegistry`, `CombatStatsService`, `GameStateProjectionService`, `CombatResolver`, `cardDisplayStats.ts` | An explicitly registered +Might Gear modifies host combat damage, projects only on public hosts, and stops modifying it after detach. |
| Would-die replacement hook | Partial foundation | `DeathService` and `ReplacementEffectService` provide a narrow reusable hook before real Unit/Champion death events are captured. v1 can consume an explicit `WOULD_DIE_DESTROY_SOURCE_INSTEAD` registration so a protected public Unit/Champion survives that death cleanup pass while the replacement source is destroyed. Zhonya's Hourglass now registers this through a narrow Main Phase activated-ability alpha path and applies its printed heal/exhaust/recall result. | `DeathService`, `ReplacementEffectService`, `ReplacementEffect`, `CombatResolver`, `GameEngine` | Add official Hidden Reaction-for-0 timing and player choice among competing replacement/prevention effects. |

### Tokens

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Recruit token creation | Partial | Noxian Drummer and Vanguard Captain are card-specific Supported for simple 1 Might Recruit Unit tokens, but the broader token system still only covers narrow Recruit scripts. | `TokenFactory`, `GameEngine`, `CardDefinition`, projection | A future non-Recruit token card creates the correct token stats without special-casing the card name. |
| Token lifecycle and visibility | Partial | Recruit tokens can fight and are public cards outside deck pools, but official cleanup/disappear policy outside combat is not complete. | `CardInstance`, `CardZoneService`, serialization | A Recruit token dies in combat and follows the official token cleanup policy for trash/removed zones. |

### Card-Specific Scripting

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Irelia/Diana spell scripts | Partial/Unsupported | Stacked Deck opens the narrow alpha chain and resolves into a private top-3 choice. Star-Crossed has a narrow chain-backed paired friendly/enemy return script. Gust, Discipline, En Garde, Defiant Dance, Flash, Eclipse, Stupefy, and Hard Bargain have narrow chain-backed Reaction scripts through current alpha windows. Eclipse now creates a private Predict choice after its -4 Might effect resolves. Defy has a narrow alpha counter path for supported public spell chain items. Not So Fast has a narrow alpha counter path for enemy public spell chain items that choose a friendly Unit/Champion Unit or Gear. Charm has narrow alpha support for moving one enemy public battlefield Unit/Champion to Base. The Syren and Zhonya's Hourglass have narrow Main Phase activated Gear support. Ability-chain targets, Hidden Reaction-for-0 timing, broader movement/destination control, and replacement-choice timing remain deferred. | `CardEffectRegistry`, effect handlers, target/choice UI | Charm rejects friendly, hidden, non-battlefield, Gear, and other non-Unit targets without spending resources or moving the target. |
| Activated ability framework | Partial foundation | `ActivateAbilityMove` and `ActivatedAbilityService` now provide exact-card ability definitions, source legality, target legality, payment validation, legal-action gating, and immediate Main Phase resolution for v1 abilities. The Syren uses this path. | `ActivatedAbilityService`, `RulesValidator`, `GameEngine`, `LegalActionsService`, `GameBoard.tsx` | A future immediate activated ability registers through the service without adding card-specific validation branches to validator/legal-actions. |
| Fiora Vanguard unit triggers | Partial | Noxian Drummer's move-to-battlefield trigger and Stellacorn Herder's Base/battlefield lane movement trigger now dispatch through the alpha trigger framework. Loyal Poro and Vanguard Captain are card-specific Supported through their current services/scripts, while Stalking Wolf, Crowd Favorite, and Dune Drake define unsupported/partial deck identity. | `TriggerDispatcher`, trigger handlers, `GameEngine`, token/XP systems | Dune Drake gains Might only when attacking into a ready enemy unit at the same battlefield. |
| Legend and Champion text | Partial | Starter legends/champions are visible and important but their text is mostly exact-card scripted. Chosen Champions can deploy from the Champion zone to Base or to a controlled active Battlefield lane in supported Main timing and must spend available energy. Once deployed, that physical Champion card follows normal Unit lifecycle and goes to Trash if defeated; it does not return to the Champion zone without a future explicit card/rule. Legends remain pinned identity/reference cards, with a narrow exact-card Irelia - Blade Dancer activated ready ability now supported from the Legend zone. Irelia - Fervent's supported explicit-ready trigger gives her +1 Might this turn when her controller readies her through a registered effect. Irelia's conquer-ready trigger, Fervent's broad choose-trigger coverage, exact Deflect targeting tax, and automatic ready-step trigger timing remain partial. | `GameEngine`, `RulesValidator`, activated/triggered handlers, payment UI | Fiora - Worthy readies a unit when a controlled unit becomes Mighty and payment is legal. |

### Keywords in Starter Decks

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| "Becomes Mighty" triggers | Partial | `CombatStatsService` can identify Mighty Unit/Champion cards, and Sunken Temple checks Mighty at conquer time. Fiora deck and broader "becomes Mighty" threshold-crossing trigger timing remain deferred. | `CombatStatsService`, `GameEngine`, trigger handlers | A unit with 4 Might receiving +1 becomes Mighty exactly once and triggers Fiora. |
| Deathknell | Partial | Deathknell is now a keyword-driven death trigger path with card-specific handlers. Loyal Poro's full printed draw condition is card-specific Supported; Scuttle Crab has a 1v1 alpha handler for private opponent-hand reveal but still needs multiplayer opponent choice, facedown viewing, and XP. | `DeathTriggerService`, `DeathknellEffectHandler`, `CombatResolver`, `GameEngine`, projection/XP systems | Scuttle Crab Deathknell chooses among multiple opponents, allows facedown inspection only as allowed, and grants XP. |
| Buff | Unsupported as official action/state | Adaptatron and Crowd Favorite need persistent buff markers. | `CardInstance`, `GameEngine`, effect handlers | Buffing an unbuffed unit adds +1 Might; a second Buff does not stack if official rule says one buff. |
| Hidden/Ambush | Partial | Hidden cards can be moved from hand to a dedicated hidden zone, masked from non-owners, inspected by their owner, and kept out of targeting/combat/movement. Public logs and copied debug info do not include hidden card identities. Clean Ambush Units can be played from hand directly to the battlefield in Main when a friendly Unit/Champion is already there. Tideturner, Facebreaker, and Stalking Wolf still need later hidden play, Ambush-as-Reaction, additional-cost handling, and real reaction timing. | `RulesValidator`, `GameEngine`, `GameStateProjectionService`, chain/timing model | A Hidden card can be played later only in a legal reaction window. |

## P2: Needed for Broader Card Pool

### Meta Deck Audits

These are support-audit tasks, not card-effect implementation claims. Do not
invent decklists or mark an archetype supported until an extracted/supplied list
is audited through the existing support pipeline.

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Diana interaction audit | Done / implementation next | Three Diana Riftbound.gg guide lists plus the uploaded Suzhou list are extracted. Uploaded Suzhou is enforced-playable after the Hard Bargain slice, but remains Partial because Diana Legend text, Battlefield effects, Hard Bargain Repeat, The Syren, Moonfall, and other interaction-heavy exact rules are not complete. | `docs/META_DECK_SUPPORT.md`, `docs/meta/diana.md`, `docs/meta/diana-uploaded.md`, `decks/meta/normalized/`, `scripts/import-uploaded-meta-decks.mjs` | Pick one remaining repeated Diana Partial blocker and add exact card support/tests without broadening timing globally. |
| Uploaded Irelia playtest bot readiness | Done / Partial caveats | Uploaded Irelia is the first enforced-playable uploaded meta deck and the default playtest RiftBot deck. It has 0 Unsupported and 0 Not Audited cards, but it remains Partial alpha behavior with known caveats around exact Legend/Champion text, Battlefield effects, Hidden timing, broad Reaction timing, and replacement edge cases. | `RoomService`, `docs/META_DECK_SUPPORT.md`, `docs/meta/irelia-uploaded.md`, `decks/meta/normalized/irelia_wins_s3_shanghai_city_challenge.json` | Human vs RiftBot using the default Irelia Uploaded Meta - Playtest deck can start, select Battlefields, draw/play/pay/move/pass, and continue after a showdown. |
| Aurora deck mechanics audit | Partial | Annie has guide and uploaded lists, both Blocked; uploaded Annie also needs local cache support for `OGS-017 Annie - Dark Child - Starter`. Miss Fortune still needs a real list/URL before shared Aurora blockers can be trusted. | `docs/META_DECK_SUPPORT.md`, `docs/meta/annie.md`, `docs/meta/annie-uploaded.md`, future Miss Fortune list | Add Miss Fortune list, then identify shared Annie/MF blockers before implementation. |
| Master Yi representative-list audit | Partial | Master Yi guide and uploaded lists exist, but uploaded `OGS-019 Master Yi - Wuju Bladesman - Starter` is missing from local cache and gameplay notes are still needed before choosing support work. | `docs/META_DECK_SUPPORT.md`, `docs/meta/master-yi.md`, `docs/meta/master-yi-uploaded.md`, `decks/meta/normalized/` | Review gameplay notes against extracted/uploaded lists before any implementation sprint. |
| LeBlanc/Vex/Azir/Sivir/Fiora/Draven uploaded audits | Done / deferred | Uploaded lists are parsed and audited. All are Blocked; Vex also needs shape review and `UNL-041 Allay - Eager Admirer` local card-cache resolution. | `docs/meta/UPLOADED_META_DECKS.md`, `decks/meta/raw/`, `decks/meta/normalized/` | Use generated blocker lists when playtester demand rises. |

### XP / Hunt / Level / Buff

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Player XP resource | Not started | Hunt, Level, and XP-spending cards require a new public resource. | `PlayerState`, `LiveGameState`, projection, UI | Player gains 1 XP from Hunt and the projected state shows the updated XP count. |
| Hunt / Hunt X | Unsupported | Crowd Favorite uses Hunt and future sets will likely lean on XP. | `GameEngine`, scoring/conquer/hold triggers | Hunt grants XP on conquer or hold, not on ordinary movement. |
| Level / Level X and dependent text | Unsupported | Official Unleashed rules introduce Level as a dependent keyword tied to XP thresholds. | card text parser, effect registry, support metadata | A Level card below threshold has inactive Level text and above threshold applies its effect. |
| Official Buff action | Unsupported | Buff appears on starter cards and wider card pool. | `CardInstance`, effect handlers, projection | Buff marker persists across turns and interacts correctly with Might calculations. |

### Action / Reaction / Chain

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Priority / response order | Partial foundation | Chain focus/pass/resolve state exists and masks private chain item source/effect/target data by viewer. Chain items now have `PENDING`/`RESOLVED`/`COUNTERED`/`FIZZLED` status plus counter-ready metadata and public-safe target summaries. `PriorityWindowService` decides the current alpha opener/metadata policy. Stacked Deck and simple public `Draw 1` spells can create public chain items; Gust/Discipline/En Garde/Defiant Dance/Flash can be played as narrow own-turn, focused showdown, or focused chain Reactions; Defy can counter supported public spell chain items; Not So Fast can counter supported public enemy spell chain items that choose a friendly Unit/Gear. Human priority windows are based on public timing opportunities, not hidden hand contents, so empty windows wait for manual pass unless a local auto-pass setting is enabled. Bot players may still auto-pass empty windows. This is still not official priority and broader spell/ability responses remain deferred. | `LiveGameState`, `PriorityWindowService`, `GameEngine`, `RulesValidator`, `LegalActionsService`, websocket prompts | Active player casts a supported priority-window spell, opponent may pass with or without a legal Reaction before resolution. |
| Formal participant priority during showdown | Partial | Showdown focus/pass v1 alternates the current focused relevant player and blocks early attacker resolution, but it is not a full official priority, invitation, trigger-chain, or Reaction/chain model. | `LegalActionsService`, `RulesValidator`, `GameBoard.tsx`, chain/timing service | Participants alternate or pass priority according to official timing, and only the exact legal response actions are exposed. |
| Countering abilities | Unsupported | Not So Fast's narrow alpha support covers only enemy public spell chain items that choose a friendly Unit/Champion Unit or Gear. Ability chain items do not exist yet. | chain model, target model | Countering an ability removes it from the chain and prevents its effect. |

### Hidden Information

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Facedown cards and reveal permissions | Partial | Hidden cards now have owner/opponent/spectator projections and owner-only preview/inspect UI, but Deathknell reveal, reveal-hand effects, private deck choices, and effect-granted reveal permissions need exact modeling. | `GameStateProjectionService`, `CardInstance`, `GameEngine` | Opponent cannot inspect facedown cards unless an effect grants permission for that turn. |
| Revealed-hand duration | Partial | Revealed hands have filtering, but duration/permission by source effect needs more coverage. | `LiveGameState`, projection, effect handlers | Scuttle Crab Deathknell reveals opponent hand only to the controller and only for the intended duration. |
| Privacy regression coverage | Partial | Projection, REST, shared WebSocket broadcasts, match history, and debug-info surfaces now have focused leak checks. Future private choices, top-deck prompts, and reveal windows need the same tests as they are added. | `GameStateProjectionService`, REST/WebSocket controllers, frontend debug helpers | A new private choice flow can be serialized for every viewer without exposing hidden IDs, deck contents, or private logs. |

### Tournament Legality

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Set legality and rotation | Not started | Public playtests and tournaments need format validation beyond the current banlist. | `TournamentLegality`, `RoomService`, card metadata | A card outside the selected format is rejected from FULL_CONSTRUCTED. |
| Champion identity and signature legality | Partial | Constructed validation now separates the chosen Champion role from Champion-type MainDeck Units, and exact-card copy counting includes the chosen Champion. Champion-tag matching to the Legend, full Champion Unit identity rules, and Signature-card restrictions still need reliable structured Riftcodex fields or a curated metadata layer. | `CardDataService`, `RoomService`, deck builder validation, card metadata overrides | Irelia - Blade Dancer accepts only an Irelia chosen Champion, rejects a Signature card as the chosen Champion, and still allows legal non-chosen Champion Units in the MainDeck. |
| Sideboards and match procedure | Not started | Imported decklists include sideboards, but they are skipped today. | deck model, `RoomService`, deck import/export | Importing a sideboard stores it separately and does not shuffle it into game setup. |

## P3: Polish / Tournament / Multiplayer / Edge Cases

### Multiplayer and Match Flow

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| 2-4 player rules completeness | Not started | Rooms allow multiple players in spirit, but engine assumptions are effectively 1v1. | setup, targeting, scoring, turn order | A 3-player room starts with correct turn order and legal target restrictions. |
| Best-of-three and match history | Partial | Tournament testing needs match-level records, not only single completed games. | match history, lobby, deck snapshots | Match history records game 1/game 2 winners without hidden deck contents. |

### Advanced Rules Systems

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Replacement/prevention effects | Partial foundation | A narrow server-side would-die replacement hook exists for explicit registrations before Unit/Champion death cleanup. Zhonya's Hourglass uses it through a conservative Main Phase alpha activation, but full prevention, damage replacement, player choice among competing replacement effects, Hidden Reaction timing, and broad production card support remain deferred. | `DeathService`, `ReplacementEffectService`, effect registry, combat resolver, chain model | A prevention effect reduces combat damage before destruction is checked. |
| Unique/copy/linked instructions | Not started | Official Unleashed updates mention systems not currently represented. | card text parser, effect handlers | Copy effect copies only official copyable values and expires at the correct time. |
| Full battlefield abilities | Partial | Battlefields are selected, revealed, rendered, and hover-readable. Sunken Temple, Targon's Peak, and Abandoned Hall have exact active-lane alpha hooks for conquer-triggered payment/draw, end-turn rune readying, and spell-play unit buff choices. Aspirant's Climb, Hall of Legends, Fortified Position, trigger stacking, player-selected multi-trigger choices, and full official location rules still need explicit handling before promotion. | battlefield model, effect handlers | Hall of Legends triggers on conquer and readies the legend after legal payment. |
| Official battlefield setup details | Partial | Alpha now has pre-mulligan player Battlefield selection and reveal plus stable Battlefield location IDs (`bf-0`/`bf-1`/`bf-2`) on battlefield cards/showdowns, but starting player, ownership, and official selected-Battlefield placement details are simplified. | `GameService`, `GameEngine`, `RulesValidator`, `LegalActionsService`, `GameBoard.tsx` | Both players choose from their submitted Battlefields before mulligan and the chosen cards remain public. |

### Post-Alpha Multi-Location Battlefield Model

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Multi-location Battlefield model | Partial foundation / Post-alpha | Current 1v1 Duel/bot alpha renders two active shared Battlefield lanes and sends drag-to-lane move destinations, including ready Unit/Champion movement between active lanes during Main Phase when no showdown is active. Server-side `battlefieldLocationId` still supports `bf-0`, `bf-1`, and reserved future id `bf-2`; active UI lanes are format-aware rather than derived from the three Battlefield cards in a constructed deck. Movement, showdowns, combat resolution, controller keys, scoring, and narrow Sunken Temple/Targon's Peak/Abandoned Hall exact-card hooks are scoped by location, but official-style support still needs selected Battlefield instances per objective, hidden slots, bot/UI strategy, most Battlefield effects, and full "here" text. This is not the same as 3+ player multiplayer. | `LiveGameState`, `CardInstance`, `GameEngine`, `CombatResolver`, `RulesValidator`, `BotService`, `GameBoard.tsx` | Moving into Battlefield A starts a showdown only with opposing units at Battlefield A, not Battlefield B. |
| Per-location Battlefield score tracking | Partial foundation / Post-alpha | Server scoring keys are location ids and prevent duplicate scoring per tracked location, and the UI now shows controller indicators per lane. Official control/scoring timing and Battlefield card effects remain incomplete. | `GameEngine`, `LiveGameState`, setup tests, scoreboard UI | Player scores two different Battlefield locations in one turn and cannot score the same Battlefield twice. |
| Per-location target and movement UI | Partial / Post-alpha | Dragging a Unit/Champion onto a visible lane sends `battlefieldLocationId`; target prompts still need richer location-specific wording and official card-text constraints before every "here/there" effect is accurate. | target UI, `cardActions.ts`, `GameBoard.tsx`, `RulesValidator` | A targeted effect can choose only units at the named Battlefield required by the effect. |

## Recommended Next Sprints

Use `docs/ALPHA_PRIORITY_BOARD.md` for the current pre-playtest decision
order. The list below is a post-stabilization implementation backlog, not a
reason to keep adding rules systems before the external alpha stop line.

1. Harden low-risk combat descriptors.
   - Promote simple descriptor-only cards after direct card review.
   - Wire Mighty threshold-crossing events and source-specific triggers.
   - Add direct real-card tests for Daring Poro, Laurent Duelist, Fortified Position, and Sunken Temple.
2. Expand the target/choice prompt model.
   - Build on explicit targets and the private pending-choice framework with multi-target and linked result prompts before broad spell scripting.
   - Start with Gust and En Garde because they are simpler than counterspells.
3. Expand starter-deck trigger primitives.
   - Build on `TriggerDispatcher` for additional simple movement, attack,
     spell-play, conquer, and Mighty-threshold triggers.
   - Keep Deathknell in `DeathTriggerService` until migration is worth the risk.
   - Add Buff marker support before promoting Buff/Hunt cards.
4. Connect more real card scripts to the chain/reaction model.
   - The v1 chain state and priority-window service exist with Stacked Deck, Gust, narrow Defy support, and narrow Not So Fast targeted-spell support, but Riposte, Ambush-as-Reaction, Hidden play, and broad spell/ability counters still need real timing permissions and target models.
