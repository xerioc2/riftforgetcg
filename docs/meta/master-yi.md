# Master Yi, Wuju Bladesman Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/master-yi-wuju-bladesman-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| master-yi-wins-s3-guangzhou-city-challenge | Master Yi Wins S3 Guangzhou City Challenge | 27 | 4 | 22 | 1 | 0 | No |
| master-yi-wins-s3-chongqing-city-challenge | Master Yi Wins S3 Chongqing City Challenge | 26 | 3 | 20 | 3 | 0 | No |
| master-yi-top-16-at-sydney-regional-qualifier | Master Yi Top 16 at Sydney Regional Qualifier | 24 | 3 | 20 | 1 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Repulse: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Amateur Recital: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Grove of the God-Willow: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Startipped Peak: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Janna - Savior: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Challenge: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Sabotage: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.
- Altar to Unity: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Alpha Strike: UNSUPPORTED - XP / Hunt / Level / Buff. Blocked in enforced play: this card's effect is not supported yet.
- The Arena's Greatest: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Vilemaw's Lair: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Master Yi - Tempered: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.

## Decklists

### Master Yi Wins S3 Guangzhou City Challenge

- Source deck slug: `master-yi-wins-s3-guangzhou-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=master-yi-wins-s3-guangzhou-city-challenge
- Extracted file: `decks/meta/master-yi/master-yi-wins-s3-guangzhou-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Amateur Recital, 1x Grove of the God-Willow, 1x Startipped Peak |
| Champion / MainDeck candidate | 2x Janna - Savior, 1x Master Yi - Tempered, 1x Master Yi - Wuju Bladesman - Starter, 3x Rengar - Trophy Hunter |
| Main Deck | 2x Back Off, 2x Charm, 3x Defy, 3x Disarming Rake, 3x Discipline, 2x Dragonsoul Sage, 2x En Garde, 3x First Mate, 3x Lonely Poro, 3x Not So Fast, 3x Punch First, 1x Repulse, 2x Ruin Runner, 3x Scuttle Crab, 1x Tasty Faefolk, 2x Trinity Force, 2x Vilemaw, 2x Zhonya's Hourglass |
| Rune Deck | 6x Body Rune, 6x Calm Rune |

Top blockers:

- Repulse: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Amateur Recital: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Grove of the God-Willow: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Startipped Peak: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Janna - Savior: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.

### Master Yi Wins S3 Chongqing City Challenge

- Source deck slug: `master-yi-wins-s3-chongqing-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=master-yi-wins-s3-chongqing-city-challenge
- Extracted file: `decks/meta/master-yi/master-yi-wins-s3-chongqing-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Altar to Unity, 1x Star Spring, 1x Treasure Hoard |
| Champion / MainDeck candidate | 3x Akshan - Mischievous, 1x Master Yi - Tempered, 1x Master Yi - Wuju Bladesman - Starter, 3x Rengar - Trophy Hunter |
| Main Deck | 1x Back Off, 2x Challenge, 3x Charm, 3x Defy, 3x Discipline, 2x En Garde, 3x First Mate, 2x Guardian Angel, 3x Lonely Poro, 2x Punch First, 1x Repulse, 1x Ruin Runner, 3x Sabotage, 3x Scuttle Crab, 3x Tasty Faefolk, 2x Vilemaw, 3x Zhonya's Hourglass |
| Rune Deck | 7x Body Rune, 5x Calm Rune |

Top blockers:

- Challenge: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Repulse: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Sabotage: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.
- Altar to Unity: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

### Master Yi Top 16 at Sydney Regional Qualifier

- Source deck slug: `master-yi-top-16-at-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=master-yi-top-16-at-sydney-regional-qualifier
- Extracted file: `decks/meta/master-yi/master-yi-top-16-at-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Startipped Peak, 1x The Arena's Greatest, 1x Vilemaw's Lair |
| Champion / MainDeck candidate | 1x Master Yi - Tempered, 3x Rengar - Trophy Hunter |
| Main Deck | 1x Alpha Strike, 1x Back Off, 3x Charm, 3x Defy, 3x Discipline, 2x En Garde, 3x Lonely Poro, 1x Not So Fast, 3x Pit Rookie, 2x Punch First, 3x Ruin Runner, 3x Scuttle Crab, 2x Stalwart Poro, 2x Trinity Force, 1x Vilemaw, 1x Whiteflame Protector, 2x Zhonya's Hourglass |
| Rune Deck | 6x Body Rune, 6x Calm Rune |

Top blockers:

- Alpha Strike: UNSUPPORTED - XP / Hunt / Level / Buff. Blocked in enforced play: this card's effect is not supported yet.
- Startipped Peak: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- The Arena's Greatest: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Vilemaw's Lair: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Master Yi - Tempered: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.

## Recommended Implementation Order

- Keep Master Yi tracked as the raw meta leader, but pick implementation work only after reviewing these guide lists with gameplay notes.
