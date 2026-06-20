# Diana, Scorn of the Moon Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/diana-scorn-of-the-moon-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| diana-wins-s3-suzhou-city-challenge | Diana Wins S3 Suzhou City Challenge | 29 | 1 | 25 | 3 | 0 | No |
| diana-wins-s3-shenzhen-city-challenge-tournament | Diana Wins S3 Shenzhen City Challenge tournament | 30 | 1 | 25 | 4 | 0 | No |
| diana-top-4-at-sydney-regional-qualifier | Diana Top 4 at Sydney Regional Qualifier | 26 | 1 | 23 | 2 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Invert Timelines: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.
- Turn to Dust: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Unchecked Power: UNSUPPORTED - Basic or descriptor-only. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Bellows Breath: UNSUPPORTED - Basic or descriptor-only. Blocked in enforced play: this card's effect is not supported yet.
- Downwell: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Existential Dread: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Fading Memories: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Frigid Jewel: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Ravenbloom Conservatory: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Targon's Peak: PARTIAL - Battlefield effect. Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred.

## Decklists

### Diana Wins S3 Suzhou City Challenge

- Source deck slug: `diana-wins-s3-suzhou-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=diana-wins-s3-suzhou-city-challenge
- Extracted file: `decks/meta/diana/diana-wins-s3-suzhou-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Star Spring, 1x Targon's Peak |
| Champion / MainDeck candidate | 1x Diana - Lunari, 2x Fizz - Trickster, 3x Hwei - Brooding Painter, 2x Vex - Apathetic |
| Legend | 1x Diana - Scorn of the Moon |
| Main Deck | 1x Abandon, 2x Eclipse, 2x Flash, 3x Gust, 1x Hard Bargain, 1x Invert Timelines, 1x Last Rites, 3x Moonfall, 3x Ravenbloom Student, 3x Ride the Wind, 3x Stacked Deck, 2x Star-Crossed, 3x Stupefy, 1x The Syren, 2x Thousand-Tailed Watcher, 2x Tideturner, 2x Traveling Merchant, 2x Turn to Dust, 2x Unchecked Power |
| Rune Deck | 7x Chaos Rune, 5x Mind Rune |

Top blockers:

- Invert Timelines: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.
- Turn to Dust: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Unchecked Power: UNSUPPORTED - Basic or descriptor-only. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

### Diana Wins S3 Shenzhen City Challenge tournament

- Source deck slug: `diana-wins-s3-shenzhen-city-challenge-tournament`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=diana-wins-s3-shenzhen-city-challenge-tournament
- Extracted file: `decks/meta/diana/diana-wins-s3-shenzhen-city-challenge-tournament.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Ravenbloom Conservatory, 1x Targon's Peak |
| Champion / MainDeck candidate | 1x Diana - Lunari, 2x Fizz - Trickster, 3x Hwei - Brooding Painter, 2x Vex - Apathetic |
| Legend | 1x Diana - Scorn of the Moon |
| Main Deck | 1x Abandon, 1x Bellows Breath, 1x Downwell, 2x Eclipse, 2x Existential Dread, 2x Flash, 3x Gust, 1x Hard Bargain, 1x Last Rites, 3x Moonfall, 3x Ravenbloom Student, 3x Ride the Wind, 3x Stacked Deck, 2x Star-Crossed, 3x Stupefy, 1x The Syren, 2x Thousand-Tailed Watcher, 2x Tideturner, 2x Traveling Merchant, 2x Turn to Dust |
| Rune Deck | 7x Chaos Rune, 5x Mind Rune |

Top blockers:

- Bellows Breath: UNSUPPORTED - Basic or descriptor-only. Blocked in enforced play: this card's effect is not supported yet.
- Downwell: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Existential Dread: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Turn to Dust: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.

### Diana Top 4 at Sydney Regional Qualifier

- Source deck slug: `diana-top-4-at-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=diana-top-4-at-sydney-regional-qualifier
- Extracted file: `decks/meta/diana/diana-top-4-at-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Ravenbloom Conservatory, 1x Targon's Peak |
| Champion / MainDeck candidate | 1x Diana - Lunari, 2x Fizz - Trickster, 3x Hwei - Brooding Painter, 1x Vex - Apathetic, 1x Vex - Cheerless |
| Legend | 1x Diana - Scorn of the Moon |
| Main Deck | 1x Fading Memories, 2x Flash, 3x Frigid Jewel, 3x Gust, 1x Hard Bargain, 1x Last Rites, 1x Mindsplitter, 2x Moonfall, 3x Ravenbloom Student, 3x Ride the Wind, 1x Smoke Screen, 3x Stacked Deck, 2x Star-Crossed, 3x Stupefy, 3x Tideturner |
| Rune Deck | 6x Chaos Rune, 6x Mind Rune |

Top blockers:

- Fading Memories: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Frigid Jewel: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Ravenbloom Conservatory: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Targon's Peak: PARTIAL - Battlefield effect. Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred.

## Recommended Implementation Order

- Treat Diana as the next implementation target after Irelia because its lists are interaction-heavy and should stress Reaction/chain/priority, targeting, and combat timing.
