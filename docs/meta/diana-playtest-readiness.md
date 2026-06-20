# Diana Uploaded Meta Playtest Readiness

Source raw file: `decks/meta/raw/diana_wins_s3_suzhou_city_challenge.txt`
Normalized deck: `decks/meta/normalized/diana_wins_s3_suzhou_city_challenge.json`

## Gate Status

- Constructed shape: Pass
- Main deck: 39
- Rune deck: 12
- Battlefields: 3
- Legend: 1 (`Diana - Scorn of the Moon`)
- Chosen Champion: `Diana - Lunari`
- Champion candidates in list: 6
- Supported / Partial / Unsupported / Not Audited: 1 / 24 / 0 / 0
- Unresolved cards: none
- Enforced playable by gate: Yes

Diana is a selectable playtest bot deck because the uploaded Suzhou list has no
Unsupported or Not Audited cards. It is still Partial-heavy alpha behavior, not
rules-complete, golden, or reference-correct.

## Partial Risk Table

| Card | Count | Current support reason | Implemented behavior | Missing/deferred behavior | Risk | Bot readiness |
| --- | ---: | --- | --- | --- | --- | --- |
| Diana - Scorn of the Moon | 1 | Playable for alpha testing, but card-specific behavior may be incomplete. | Deck role and display are valid. | Exact Legend text and Diana-specific persistent effects need audit. | High | OK with caveat |
| Hard Bargain | 1 | Partial: counters a supported public pending spell chain item unless that spell's controller pays 2 energy through an owner-only prompt. | Narrow counter-tax Reaction works on supported public spell chain items. | Repeat, broad Reaction timing, ability targets, hidden/private chain targets, and countering counters. | Medium | OK |
| Star Spring | 1 | Playable for alpha testing, but card-specific behavior may be incomplete. | Battlefield selection/display works. | Exact Battlefield ability is not scripted. | High | OK with caveat |
| Abandoned Hall | 1 | Exact spell-play optional trigger is implemented for active Battlefield lanes. | Spell controller may choose a friendly public Unit/Champion here for +1 Might this turn. | Trigger stacking and broader official Battlefield rules. | Medium | OK |
| Moonfall | 3 | Playable for alpha testing, but card-specific behavior may be incomplete. | Card is playable by alpha support gate. | Exact movement/location text needs audit and scripting. | High | OK with caveat |
| Abandon | 1 | Partial: counters a supported public pending spell chain item, returns it to hand, then creates a private Predict choice. | Narrow public spell-chain counter plus private Predict foundation. | Repeat, broad Reaction timing, hidden/private chain targets, and ability targets. | Medium | OK |
| Star-Crossed | 2 | Partial: alpha chain-window Reaction support returns one friendly public battlefield Unit/Champion and one enemy public battlefield Unit/Champion to their owners' hands. | Staged friendly/enemy return resolves from the chain. | Full official Reaction timing and edge cases. | Medium | OK with caveat |
| Hwei - Brooding Painter | 3 | Playable for alpha testing, but card-specific behavior may be incomplete. | Main-deck Champion card handling works. | Exact Champion Unit text is not audited here. | Medium | OK with caveat |
| Diana - Lunari | 1 | Playable for alpha testing, but card-specific behavior may be incomplete. | Chosen Champion role and deployment flow work. | Exact Champion text needs audit. | High | OK with caveat |
| Eclipse | 2 | Partial: alpha chain-window Reaction support gives one public battlefield Unit/Champion -4 Might this turn, then creates a private owner-only Predict choice. | Exact -4 Might plus Predict flow is implemented. | Full official Reaction timing and edge cases. | Medium | OK with caveat |
| Last Rites | 1 | Playable for alpha testing, but card-specific behavior may be incomplete. | Gear lifecycle foundation can play Gear to Base. | Exact Gear text/effect needs audit. | Medium | OK with caveat |
| Fizz - Trickster | 2 | Playable for alpha testing, but card-specific behavior may be incomplete. | Main-deck Champion card handling works. | Exact Champion Unit text is not audited here. | Medium | OK with caveat |
| Mind Rune | 5 | Playable for alpha testing, but card-specific behavior may be incomplete. | Basic rune setup/payment model accepts the card. | Deeper domain/payment edge cases remain roadmap work. | Low | OK |
| Flash | 2 | Partial: alpha chain-window Reaction support moves up to two friendly battlefield Unit/Champion cards to Base. | Supported chain-backed friendly recall. | Full official any-time Reaction timing. | Medium | OK |
| Targon's Peak | 1 | Exact conquer trigger queues end-turn readying for up to two tapped friendly runes. | Narrow active-lane conquer hook works. | Player-selected rune choice and full official location rules. | Medium | OK |
| Tideturner | 2 | Hidden foundation exists, but later hidden play timing and on-play location swap are not implemented yet. | Hidden display/projection foundation works. | Play-from-hidden timing and Tideturner swap text. | High | OK with caveat |
| Traveling Merchant | 2 | Playable for alpha testing, but card-specific behavior may be incomplete. | Card is playable by alpha support gate. | Exact card text needs audit. | Medium | OK with caveat |
| The Syren | 1 | Partial: Main Phase activated ability pays 1, exhausts, and moves a friendly public battlefield Unit/Champion to Base. | Narrow activated Gear movement works. | Broader activated ability timing and ability-chain support. | Medium | OK |
| Stacked Deck | 3 | Partial: opens the narrow alpha chain, then resolves into a private top-3 choice. | Public chain opener and private top-card choice work. | Official ordering and broader timing. | Medium | OK |
| Ride The Wind | 3 | Playable for alpha testing, but card-specific behavior may be incomplete. | Friendly ready helper is used by existing alpha paths. | Movement choice and full Action/showdown timing details. | Medium | OK with caveat |
| Gust | 3 | Partial: alpha chain-window Reaction returns a battlefield Unit/Champion with 3 Might or less. | Narrow chain-backed bounce works. | Full official any-time Reaction timing. | Medium | OK |
| Thousand-Tailed Watcher | 2 | Playable for alpha testing, but card-specific behavior may be incomplete. | Card is playable by alpha support gate. | Exact unit text needs audit. | Medium | OK with caveat |
| Ravenbloom Student | 3 | Playable for alpha testing, but card-specific behavior may be incomplete. | Card is playable by alpha support gate. | Exact unit text needs audit. | Medium | OK with caveat |
| Stupefy | 3 | Playable for alpha testing, but card-specific behavior may be incomplete. | Card is playable by alpha support gate. | Exact spell text needs audit. | Medium | OK with caveat |

## Top Diana Accuracy Gaps

1. `Moonfall` appears as a 3-of and likely drives Diana's location/interactions; audit exact text first.
2. `Diana - Scorn of the Moon` and `Diana - Lunari` need Legend/Champion text fidelity.
3. `Star Spring` is an unscripted Battlefield effect in the selected Battlefield package.
4. `Tideturner` still lacks play-from-hidden timing and its on-play location swap.
5. `Ride The Wind`, `Stupefy`, `Eclipse`, and the repeated Unit package need exact text audits before calling Diana rules-complete.

## Bot Wiring

`Diana Uploaded Meta - Playtest` is selectable in the RiftBot deck selector. It
uses the real uploaded Suzhou normalized decklist. `Irelia Uploaded Meta -
Playtest` remains the default RiftBot deck when no explicit bot deck is selected.
