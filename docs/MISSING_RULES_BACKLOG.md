# RiftForge Missing Rules Backlog

Last audit: 2026-06-11

This backlog lists missing or partial Riftbound support in implementation order,
not official rules order. It is intentionally conservative. "Supported" means
implemented and tested in this repository. "Partial" means the common playtest
path exists, but official edge cases or card-specific scripts are missing.

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
| Full action/reaction window model | Unsupported | Starter decks contain many `[Action]` and `[Reaction]` spells. Without chain/timing windows, counterspells, Ambush, Hidden, and reaction tricks cannot be rules-correct. | `GameEngine`, `RulesValidator`, `LegalActionsService`, `MoveRequest`, `GameBoard.tsx`, `stompGame.ts` | A Reaction spell is legal only while a chain item/window exists and is rejected in ordinary Main without a legal window. |
| Chain / pending spell or ability objects | Not started | Cards such as Defy, Not So Fast, and Riposte need a spell or ability object to target before it resolves. Current spells apply immediately. | `LiveGameState`, `GameEngine`, `RulesValidator`, new chain model/tests | Playing Defy can counter a pending spell and cannot be played when no pending spell exists. |
| Card-specific legal actions | Partial | `legalActions` are high-level and phase-based, not card/target/payment-specific. The UI can still offer a card that the server rejects. | `LegalActionsService`, `GameStateProjectionService`, frontend action controls | Projection for a hand card includes playable/unplayable reason based on current phase and available targets. |

### Rune and Payment Rules

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Domain/power payment validation | Partial | Constructed decks use domain runes and many card costs include more than generic energy. Current payment is not complete enough for competitive play. | `RulesValidator`, `GameEngine`, `CardDataService`, `MoveRequest`, payment UI | A card requiring Order power cannot be played using only Body runes. |
| Atomic selected-rune payment | Partial | Tapping/discarding runes separately is playable, but true card payment should validate and consume selected runes atomically with `PLAY_CARD`. | `PlayCardMove`, `RulesValidator`, `GameEngine`, `GameBoard.tsx` | Failed play leaves all selected runes unchanged; successful play exhausts/recycles exactly selected runes. |
| Cost modifiers and alternate/additional costs | Unsupported | Ambush, Hidden, Spectral Matron, counterspells, and equipment all need extra/alternate cost support. | `RulesValidator`, `GameEngine`, card effect handlers | Stalking Wolf requires its additional kill cost and rejects if no legal sacrifice exists. |

### Targeting and Choice Prompts

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Explicit target model | Partial | Current target validation is mostly heuristic. Spells need target type, ownership, location, and count rules. | `MoveRequest`, `RulesValidator`, `GameEngine`, `CardDataService`, frontend target UI | Gust can target only a unit at a battlefield with 3 or less Might. |
| Multi-target and paired choices | Unsupported | Defiant Dance and Star-Crossed require more than one target with different constraints. | `MoveRequest`, `RulesValidator`, `GameEngine`, target UI | Defiant Dance rejects selecting the same target for both effects unless card text allows it. |
| Private choice prompts | Partial | Vision exists narrowly, but Stacked Deck, Keeper's Verdict, Deathknell reveal, and Predict-like effects need private ordered choices. | `GameEngine`, `GameStateProjectionService`, `stompGame.ts`, modal UI | Stacked Deck shows top 3 only to the controller and recycles the unchosen cards without leaking them. |

### Movement and Battlefields

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Multiple battlefield location model | Partial | Current battlefield logic has controller keys but many paths still behave like a simplified shared battlefield. Official play needs selected battlefields and units at each location. | `LiveGameState`, `CardInstance`, `GameEngine`, `CombatResolver`, board layout | Moving into Battlefield A starts a showdown only with opposing units at Battlefield A, not Battlefield B. |
| Movement permissions by card/effect | Partial | Generic movement is sandbox-only, but effect-driven movement and location swaps are not card-accurate. | `RulesValidator`, `GameEngine`, effect handlers | Tideturner swaps only with a friendly unit at another location and preserves both legal zones. |
| Contested battlefield state | Partial | Showdown, control, conquer, and hold depend on exact contested state per battlefield. | `GameEngine`, `LiveGameState`, `CombatResolver` | A battlefield becomes contested when both players have units there and returns to controlled after showdown cleanup. |

### Showdown and Combat

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Interactive showdown action windows | Partial | `activeShowdown` exists, but official action/reaction opportunities during showdowns are not fully modeled. | `GameEngine`, `LegalActionsService`, `RulesValidator`, UI | During active showdown, only legal Action/Reaction windows expose the correct moves. |
| Player-chosen damage assignment | Unsupported | Current combat damage is deterministic. Tank/lethal rules need player assignment for official precision. | `CombatResolver`, `MoveRequest`, `GameBoard.tsx` | Attacker assigns lethal to Tank first, then chooses remaining damage among legal defenders. |
| Combat cleanup and modifiers | Partial | Temporary Might, Stun, damage, and battlefield cleanup need exact timing for many cards. | `CombatResolver`, `GameEngine`, `CardZoneService` | A temporary combat modifier expires after combat, while permanent Buff remains. |

### Scoring and Winning

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Official winning check timing | Partial | Conquer final-point restriction exists, but official cleanup/tie/multiplayer win timing is not complete. | `GameEngine`, `GameEngineScoringTest`, match history | Player reaches target from legal Hold point and wins only at the correct check timing. |
| Multi-battlefield score tracking | Partial | Current scoring must scale to every selected battlefield and avoid duplicate scoring. | `GameEngine`, `LiveGameState`, setup tests | Player scores two different battlefields in one turn and cannot score the same battlefield twice. |

## P1: Needed for Starter Decks to Feel Correct

### Attachments / Gear

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Equip target/payment rules | Partial | Guardian Angel and Boots of Swiftness are starter deck cards. Basic friendly battlefield Unit/Champion target validation exists, but official timing and payment precision are still generic. | `RulesValidator`, `GameEngine`, `CardZoneService`, payment UI | Boots requires the correct Chaos power payment and cannot attach outside the official Equip window. |
| Attachment lifecycle | Partial | Gear follows host combat death, but bounce, detach, replacement, and reattachment edge cases are not fully modeled. | `CardZoneService`, `CombatResolver`, `GameEngine` | Returning a unit to hand moves or trashes attached gear according to the official rule and clears attachment IDs. |

### Tokens

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Recruit token creation | Unsupported | Noxian Drummer and Vanguard Captain both need Recruit tokens. | token definition service, `GameEngine`, `CardDefinition`, projection | Vanguard Captain with Legion active creates two 1 Might Recruit Unit tokens at the correct location. |
| Token lifecycle and visibility | Not started | Tokens should fight, die, move, and disappear from non-game zones correctly without entering decks. | `CardInstance`, `CardZoneService`, serialization | A Recruit token dies in combat and does not appear in deck/trash counts if official rules require token cleanup. |

### Card-Specific Scripting

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Irelia Tempo spell scripts | Partial/Unsupported | Defy, Defiant Dance, Not So Fast, Star-Crossed, and Stacked Deck are currently blocked or heuristic. | `CardEffectRegistry`, effect handlers, target/choice UI | Defy counters a legal pending spell and rejects illegal pending targets. |
| Fiora Vanguard unit triggers | Partial | Stalking Wolf, Noxian Drummer, Loyal Poro, Vanguard Captain, Crowd Favorite, and Dune Drake define much of the deck's identity. | `GameEngine`, triggered handlers, token/XP systems | Loyal Poro Deathknell draws only when it did not die alone. |
| Legend and Champion text | Partial | Starter legends/champions are visible and important but their text is mostly unscripted. | `GameEngine`, activated/triggered handlers, payment UI | Fiora - Worthy readies a unit when a controlled unit becomes Mighty and payment is legal. |

### Keywords in Starter Decks

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| "Becomes Mighty" triggers | Partial | `CombatStatsService` can identify Mighty Unit/Champion cards, but Fiora deck and Sunken Temple still need threshold-crossing trigger timing. | `CombatStatsService`, `GameEngine`, trigger handlers | A unit with 4 Might receiving +1 becomes Mighty exactly once and triggers Fiora. |
| Deathknell | Unsupported | Scuttle Crab and Loyal Poro rely on death-trigger effects. | `EffectHandlerRegistry`, `CombatResolver`, `GameEngine` | Deathknell fires after combat destruction and can draw/gain XP without leaking hidden info. |
| Buff | Unsupported as official action/state | Adaptatron and Crowd Favorite need persistent buff markers. | `CardInstance`, `GameEngine`, effect handlers | Buffing an unbuffed unit adds +1 Might; a second Buff does not stack if official rule says one buff. |
| Hidden/Ambush | Partial | Tideturner, Facebreaker, and Stalking Wolf need these for real timing. | `RulesValidator`, `GameEngine`, chain/timing model | A Hidden card can be played later only in a legal reaction window. |

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
| Priority / response order | Not started | Any broad spell pool needs players to respond before spells and abilities resolve. | new chain service, `GameEngine`, websocket prompts | Active player casts a spell, opponent may respond with a legal Reaction before resolution. |
| Countering abilities | Unsupported | Not So Fast counters an enemy spell or ability that chooses a friendly unit or gear. | chain model, target model | Countering an ability removes it from the chain and prevents its effect. |

### Hidden Information

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Facedown cards and reveal permissions | Partial | Hidden, Deathknell, reveal-hand effects, and private deck choices need exact projections. | `GameStateProjectionService`, `CardInstance`, `GameEngine` | Opponent cannot inspect facedown cards unless an effect grants permission for that turn. |
| Revealed-hand duration | Partial | Revealed hands have filtering, but duration/permission by source effect needs more coverage. | `LiveGameState`, projection, effect handlers | Scuttle Crab Deathknell reveals opponent hand only to the controller and only for the intended duration. |

### Tournament Legality

| Item | Status | Why it matters | Likely files | Suggested first test |
| --- | --- | --- | --- | --- |
| Set legality and rotation | Not started | Public playtests and tournaments need format validation beyond the current banlist. | `TournamentLegality`, `RoomService`, card metadata | A card outside the selected format is rejected from FULL_CONSTRUCTED. |
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

## Recommended Next Sprints

1. Harden low-risk combat descriptors.
   - Promote simple descriptor-only cards after direct card review.
   - Wire Mighty threshold-crossing events and source-specific triggers.
   - Add direct real-card tests for Daring Poro, Laurent Duelist, Fortified Position, and Sunken Temple.
2. Build the target/choice prompt model.
   - Add explicit target payloads and server prompts before broad spell scripting.
   - Start with Gust and En Garde because they are simpler than counterspells.
3. Implement starter-deck trigger primitives.
   - Deathknell trigger queue.
   - Recruit token creation.
   - Legion token effect.
   - Buff marker.
4. Start the chain/reaction model.
   - Needed for Defy, Not So Fast, Riposte, Ambush, and Hidden to become real.
