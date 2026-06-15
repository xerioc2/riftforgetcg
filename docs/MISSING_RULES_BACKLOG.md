# RiftForge Missing Rules Backlog

Last audit: 2026-06-11

This backlog lists missing or partial Riftbound support in implementation order,
not official rules order. It is intentionally conservative. "Supported" means
implemented and tested in this repository. "Partial" means the common playtest
path exists, but official edge cases or card-specific scripts are missing.

RiftForge's alpha playtest target intentionally uses a simplified
single-battlefield flow. Full official multiple-battlefield location support is
post-alpha work because it touches movement, target selection, showdowns,
control, scoring, bot decisions, and the board UI all at once.

Sources checked:
- `docs/RULES_COVERAGE.md`
- `docs/SUPPORTED_CARDS.md`
- `docs/CARD_RULES_BACKLOG.md`
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
| Full action/reaction window model | Partial | Starter decks contain many `[Action]` and `[Reaction]` spells. The alpha supports active-player Main actions, focused participant supported Action cards during active showdowns, showdown focus/pass, Stacked Deck as the first narrow chain opener, Gust as the first chain-backed Reaction, foundational Hidden, and a narrow Main-phase Ambush battlefield-play path, but counterspells, Ambush-as-Reaction, later Hidden play, formal priority, and most reaction tricks are still not rules-correct. | `GameEngine`, `RulesValidator`, `LegalActionsService`, `MoveRequest`, `GameBoard.tsx`, `stompGame.ts` | A Reaction spell is legal only while a chain item/window exists and is rejected in ordinary Main without a legal window. |
| Chain / pending spell or ability objects | Partial foundation | `LiveGameState.chainState` can hold chain items with focused player, pass count, ready-to-resolve state, and viewer-aware projection masking for private/controller-only sources. Cards such as Defy, Not So Fast, and Riposte still need real spell/ability objects and counter targets before this becomes card support. | `LiveGameState`, `GameEngine`, `RulesValidator`, `LegalActionsService`, `GameStateProjectionService`, `BotService`, `GameBoard.tsx` | Playing Defy can counter a legal pending spell and cannot be played when no pending spell exists. |
| Card-specific legal actions | Partial | `legalActions` are high-level and phase-based, not card/target/payment-specific. The UI can still offer a card that the server rejects. | `LegalActionsService`, `GameStateProjectionService`, frontend action controls | Projection for a hand card includes playable/unplayable reason based on current phase and available targets. |

### Rune and Payment Rules

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Domain/power payment validation | Partial | Constructed decks use domain runes and many card costs include more than generic energy. Current payment is not complete enough for competitive play. | `RulesValidator`, `GameEngine`, `CardDataService`, `MoveRequest`, payment UI | A card requiring Order power cannot be played using only Body runes. |
| Atomic selected-rune payment | Partial | Tapping/discarding runes separately is playable, but true card payment should validate and consume selected runes atomically with `PLAY_CARD`. | `PlayCardMove`, `RulesValidator`, `GameEngine`, `GameBoard.tsx` | Failed play leaves all selected runes unchanged; successful play exhausts/recycles exactly selected runes. |
| Cost modifiers and alternate/additional costs | Partial | Hidden now uses a narrow "tap one ready own rune" foundation cost, and Ambush cards with unsupported additional costs are explicitly blocked. Later Hidden play, Spectral Matron, counterspells, equipment, and Stalking Wolf's sacrifice cost still need full extra/alternate cost support. | `RulesValidator`, `GameEngine`, card effect handlers | Stalking Wolf requires its additional kill cost and rejects if no legal sacrifice exists. |

### Targeting and Choice Prompts

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Explicit target model | Partial | Current target validation is mostly heuristic. Spells need target type, ownership, location, and count rules. | `MoveRequest`, `RulesValidator`, `GameEngine`, `CardDataService`, frontend target UI | Gust can target only a unit at a battlefield with 3 or less Might. |
| Multi-target and paired choices | Partial | A narrow required two-target foundation supports one friendly Unit/Champion plus one enemy Unit/Champion for paired return-to-hand effects. Defiant Dance, Facebreaker, optional targets, and broader multi-target scripting remain unsupported. | `MoveRequest`, `RulesValidator`, `GameEngine`, target UI | Defiant Dance rejects selecting the same target for both effects unless card text allows it. |
| Private choice prompts | Partial | A generic `pendingChoice` model now supports owner-only yes/no, optional-payment, Stacked Deck-style top-3 card selection, and Predict-style top/bottom ordering. Vision still uses a narrow private keep/recycle flow. Keeper's Verdict, Deathknell reveal, multi-target decisions, and linked choices still need richer prompts. | `LiveGameState`, `GameEngine`, `GameStateProjectionService`, `stompGame.ts`, modal UI | Keeper's Verdict privately asks the affected owner to put a selected unit on top or bottom without leaking hidden deck information. |

### Movement and Battlefields

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Movement permissions by card/effect | Partial | Generic movement is sandbox-only, but effect-driven movement and location swaps are not card-accurate even within the single-battlefield alpha model. | `RulesValidator`, `GameEngine`, effect handlers | Tideturner swaps only with a friendly unit at another legal location and preserves both legal zones. |
| Single-battlefield contested state | Partial | Showdown, control, conquer, and hold need reliable state for the current alpha battlefield before expanding to multiple locations. | `GameEngine`, `LiveGameState`, `CombatResolver` | The alpha battlefield becomes contested when both players have units there and returns to controlled after showdown cleanup. |

### Showdown and Combat

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Showdown focus/pass v1 | Partial | `activeShowdown` now tracks relevant players, focused player, consecutive passes, and a `readyToResolve` gate. Focused participants can play supported `[Action]` cards or pass; the attacker cannot resolve until both relevant players pass in succession. Official priority/reaction opportunities during showdowns are not fully modeled. | `GameEngine`, `LegalActionsService`, `RulesValidator`, `GameBoard.tsx` | Attacker focus starts a contested showdown, attacker pass gives defender focus, defender pass marks the showdown ready to resolve, and early resolve is rejected. |
| Combat damage assignment UI precision | Partial | Server-side assignment now exists after showdown focus/pass and enforces all-damage, Tank, lethal, duplicate, and excess policies. The current client and RiftBot use deterministic Tank-first assignment, so fully manual player damage splitting remains future work. | `CombatResolver`, `RulesValidator`, `MoveRequest`, `GameBoard.tsx` | Player manually assigns legal lethal damage to one Tank and chooses where allowed excess damage goes. |
| Combat cleanup and modifiers | Partial | Temporary Might, Stun, damage, and battlefield cleanup need exact timing for many cards. | `CombatResolver`, `GameEngine`, `CardZoneService` | A temporary combat modifier expires after combat, while permanent Buff remains. |

### Scoring and Winning

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Official winning check timing | Partial | Conquer final-point restriction exists, but official cleanup/tie/multiplayer win timing is not complete. | `GameEngine`, `GameEngineScoringTest`, match history | Player reaches target from legal Hold point and wins only at the correct check timing. |

## P1: Needed for Starter Decks to Feel Correct

### Attachments / Gear

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Equip target/payment rules | Partial | Guardian Angel and Boots of Swiftness are starter deck cards. Basic play-to-Base/equip-from-Base lifecycle exists, but official timing, payment precision, and replacement edge cases are still generic. | `RulesValidator`, `GameEngine`, `CardZoneService`, payment UI | Boots requires the correct Chaos power payment and cannot attach outside the official Equip window. |
| Attachment lifecycle | Partial | Gear now follows host death and return-to-hand cleanup deterministically, but voluntary detach, replacement, and reattachment edge cases are not fully modeled. | `CardZoneService`, `CombatResolver`, `GameEngine` | Reattaching a second gear handles the official replacement rule and clears stale attachment IDs. |

### Tokens

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Recruit token creation | Partial | Noxian Drummer and Vanguard Captain are card-specific Supported for simple 1 Might Recruit Unit tokens, but the broader token system still only covers narrow Recruit scripts. | `TokenFactory`, `GameEngine`, `CardDefinition`, projection | A future non-Recruit token card creates the correct token stats without special-casing the card name. |
| Token lifecycle and visibility | Partial | Recruit tokens can fight and are public cards outside deck pools, but official cleanup/disappear policy outside combat is not complete. | `CardInstance`, `CardZoneService`, serialization | A Recruit token dies in combat and follows the official token cleanup policy for trash/removed zones. |

### Card-Specific Scripting

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Irelia Tempo spell scripts | Partial/Unsupported | Stacked Deck opens the narrow alpha chain and resolves into a private top-3 choice. Star-Crossed has a narrow paired friendly/enemy return script. Gust has a narrow chain-backed Reaction return script through that alpha window. Defy, Defiant Dance, and Not So Fast are still blocked or heuristic because they need counterspell or broader multi-target support. | `CardEffectRegistry`, effect handlers, target/choice UI | Defy counters a legal pending spell and rejects illegal pending targets. |
| Fiora Vanguard unit triggers | Partial | Noxian Drummer's move-to-battlefield trigger and Stellacorn Herder's move trigger now dispatch through the alpha trigger framework. Loyal Poro and Vanguard Captain are card-specific Supported through their current services/scripts, while Stalking Wolf, Crowd Favorite, and Dune Drake define unsupported/partial deck identity. | `TriggerDispatcher`, trigger handlers, `GameEngine`, token/XP systems | Dune Drake gains Might only when attacking into a ready enemy unit at the same battlefield. |
| Legend and Champion text | Partial | Starter legends/champions are visible and important but their text is mostly unscripted. Chosen Champions can deploy from the Champion zone only in supported Main timing and must spend available energy; Legends remain pinned identity/reference cards in the alpha model. | `GameEngine`, `RulesValidator`, activated/triggered handlers, payment UI | Fiora - Worthy readies a unit when a controlled unit becomes Mighty and payment is legal. |

### Keywords in Starter Decks

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| "Becomes Mighty" triggers | Partial | `CombatStatsService` can identify Mighty Unit/Champion cards, but Fiora deck and Sunken Temple still need threshold-crossing trigger timing. | `CombatStatsService`, `GameEngine`, trigger handlers | A unit with 4 Might receiving +1 becomes Mighty exactly once and triggers Fiora. |
| Deathknell | Partial | Deathknell is now a keyword-driven death trigger path with card-specific handlers. Loyal Poro's full printed draw condition is card-specific Supported; Scuttle Crab has a 1v1 alpha handler for private opponent-hand reveal but still needs multiplayer opponent choice, facedown viewing, and XP. | `DeathTriggerService`, `DeathknellEffectHandler`, `CombatResolver`, `GameEngine`, projection/XP systems | Scuttle Crab Deathknell chooses among multiple opponents, allows facedown inspection only as allowed, and grants XP. |
| Buff | Unsupported as official action/state | Adaptatron and Crowd Favorite need persistent buff markers. | `CardInstance`, `GameEngine`, effect handlers | Buffing an unbuffed unit adds +1 Might; a second Buff does not stack if official rule says one buff. |
| Hidden/Ambush | Partial | Hidden cards can be moved from hand to a dedicated hidden zone, masked from non-owners, and kept out of targeting/combat/movement. Clean Ambush Units can be played from hand directly to the battlefield in Main when a friendly Unit/Champion is already there. Tideturner, Facebreaker, and Stalking Wolf still need later hidden play, Ambush-as-Reaction, additional-cost handling, and real reaction timing. | `RulesValidator`, `GameEngine`, `GameStateProjectionService`, chain/timing model | A Hidden card can be played later only in a legal reaction window. |

## P2: Needed for Broader Card Pool

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
| Priority / response order | Partial foundation | Chain focus/pass/resolve state exists and masks private chain item source/effect/target data by viewer. Stacked Deck can create the first public chain item and Gust can respond/resolves above it, but this is not official priority and counterspell/ability responses remain deferred. | `LiveGameState`, `GameEngine`, `RulesValidator`, `LegalActionsService`, websocket prompts | Active player casts a spell, opponent may respond with a legal Reaction before resolution. |
| Formal participant priority during showdown | Partial | Showdown focus/pass v1 alternates the current focused relevant player and blocks early attacker resolution, but it is not a full official priority, invitation, trigger-chain, or Reaction/chain model. | `LegalActionsService`, `RulesValidator`, `GameBoard.tsx`, chain/timing service | Participants alternate or pass priority according to official timing, and only the exact legal response actions are exposed. |
| Countering abilities | Unsupported | Not So Fast counters an enemy spell or ability that chooses a friendly unit or gear. | chain model, target model | Countering an ability removes it from the chain and prevents its effect. |

### Hidden Information

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Facedown cards and reveal permissions | Partial | Hidden cards now have owner/opponent/spectator projections, but Deathknell reveal, reveal-hand effects, private deck choices, and effect-granted reveal permissions need exact modeling. | `GameStateProjectionService`, `CardInstance`, `GameEngine` | Opponent cannot inspect facedown cards unless an effect grants permission for that turn. |
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
| Replacement/prevention effects | Not started | Prevent, replace, and copy effects require effect-layer timing. | effect registry, combat resolver, chain model | A prevention effect reduces combat damage before destruction is checked. |
| Unique/copy/linked instructions | Not started | Official Unleashed updates mention systems not currently represented. | card text parser, effect handlers | Copy effect copies only official copyable values and expires at the correct time. |
| Full battlefield abilities | Partial | Battlefields are selected and counted, but most abilities are unscripted. | battlefield model, effect handlers | Hall of Legends triggers on conquer and readies the legend after legal payment. |
| Official battlefield setup details | Partial | Alpha now has pre-mulligan player Battlefield selection and reveal, but starting player, ownership, and future multi-Battlefield placement details are simplified. | `GameService`, `GameEngine`, `RulesValidator`, `LegalActionsService`, `GameBoard.tsx` | Both players choose from their submitted Battlefields before mulligan and the chosen cards remain public. |

### Post-Alpha Multiple Battlefield Model

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Multiple battlefield location model | Deferred / Post-alpha | Current alpha intentionally treats play as a simplified single-battlefield experience. Official support needs selected battlefield instances and per-location units, targets, showdowns, control, scoring, bot decisions, and UI layout. | `LiveGameState`, `CardInstance`, `GameEngine`, `CombatResolver`, `LegalActionsService`, `BotService`, `GameBoard.tsx` | Moving into Battlefield A starts a showdown only with opposing units at Battlefield A, not Battlefield B. |
| Multi-battlefield score tracking | Deferred / Post-alpha | Current scoring should remain understandable for alpha; official play later needs per-battlefield scoring without duplicate scoring. | `GameEngine`, `LiveGameState`, setup tests, scoreboard UI | Player scores two different battlefields in one turn and cannot score the same battlefield twice. |
| Per-battlefield target and movement UI | Deferred / Post-alpha | Target prompts and movement highlights need location awareness before multiple battlefields are readable for playtesters. | target UI, `cardActions.ts`, `GameBoard.tsx`, `RulesValidator` | A targeted effect can choose only units at the named battlefield required by the effect. |

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
4. Connect real card scripts to the chain/reaction model.
   - The v1 chain state exists, but Defy, Not So Fast, Riposte, Ambush-as-Reaction, and Hidden play still need real timing permissions and counter/response targets.
