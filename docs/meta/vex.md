# Vex, Gloomist Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/vex-gloomist-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| vex-top-8-at-s3-zhongshan-city-challenge | Vex Top 8 at S3 Zhongshan City Challenge | 33 | 3 | 21 | 8 | 1 | No |
| vex-top-8-at-city-challenge-wuhan | Vex Top 8 at City Challenge — Wuhan | 28 | 3 | 21 | 4 | 0 | No |
| vex-top-4-at-sydney-regional-qualifier | Vex Top 4 at Sydney Regional Qualifier | 25 | 2 | 20 | 3 | 0 | No |

## Unresolved Cards

- UNL-041: Allay - Eager Admirer

## Shared Top Blockers

- Abandon: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Allay - Eager Admirer: NOT_AUDITED - Triggered/static card text. No local RiftForge card matched "Allay - Eager Admirer".
- Blast Cone: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Charm: PARTIAL - Movement / location. Alpha support moves one enemy public battlefield Unit/Champion to Base; broader movement choices and destination control remain deferred.
- Emperor's Divide: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Seal of Focus: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Wind Wall: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Zhonya's Hourglass: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Existential Dread: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Bandle Tree: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Decklists

### Vex Top 8 at S3 Zhongshan City Challenge

- Source deck slug: `vex-top-8-at-s3-zhongshan-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=vex-top-8-at-s3-zhongshan-city-challenge
- Extracted file: `decks/meta/vex/vex-top-8-at-s3-zhongshan-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Bandle Tree, 1x The Papertree, 1x Trifarian War Camp |
| Champion / MainDeck candidate | 1x Ahri - Alluring, 2x Draven - Audacious, 3x Evelynn - Entrancing, 1x Janna - Savior, 2x Sona - Harmonious, 2x Teemo - Scout, 1x Vex - Apathetic |
| Legend | 1x Vex - Gloomist |
| Main Deck | 1x Abandon, 2x Allay - Eager Admirer, 1x Back Off, 1x Blast Cone, 2x Charm, 2x Defy, 2x Disarming Rake, 3x Discipline, 2x Edge of Night, 2x Ember Monk, 2x Emperor's Divide, 2x Existential Dread, 2x Gust, 1x Hard Bargain, 2x Mutated Mouser, 1x Not So Fast, 3x Overzealous Fan, 1x Shadow, 1x Switcheroo, 3x Zhonya's Hourglass |
| Rune Deck | 6x Calm Rune, 6x Chaos Rune |

Top blockers:

- Abandon: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Allay - Eager Admirer: NOT_AUDITED - Triggered/static card text. No local RiftForge card matched "Allay - Eager Admirer".
- Blast Cone: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Charm: PARTIAL - Movement / location. Alpha support moves one enemy public battlefield Unit/Champion to Base; broader movement choices and destination control remain deferred.
- Emperor's Divide: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.

### Vex Top 8 at City Challenge — Wuhan

- Source deck slug: `vex-top-8-at-city-challenge-wuhan`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=vex-top-8-at-city-challenge-wuhan
- Extracted file: `decks/meta/vex/vex-top-8-at-city-challenge-wuhan.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Aspirant's Climb, 1x The Papertree |
| Champion / MainDeck candidate | 2x Evelynn - Entrancing, 1x Fizz - Trickster, 3x Sona - Harmonious, 1x Vex - Apathetic |
| Legend | 1x Vex - Gloomist |
| Main Deck | 3x Back Off, 1x Baron Nashor, 3x Defy, 3x Disarming Rake, 3x Discipline, 1x Emperor's Divide, 3x Mutated Mouser, 1x Not So Fast, 3x Scuttle Crab, 3x Seal of Focus, 3x Stacked Deck, 3x Star-Crossed, 1x Tianna Crownguard, 1x Tideturner, 2x Vilemaw, 2x Wind Wall, 2x Windsinger, 3x Zhonya's Hourglass |
| Rune Deck | 7x Calm Rune, 5x Chaos Rune |

Top blockers:

- Emperor's Divide: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Seal of Focus: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Wind Wall: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Zhonya's Hourglass: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.

### Vex Top 4 at Sydney Regional Qualifier

- Source deck slug: `vex-top-4-at-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=vex-top-4-at-sydney-regional-qualifier
- Extracted file: `decks/meta/vex/vex-top-4-at-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Bandle Tree, 1x Star Spring, 1x Startipped Peak |
| Champion / MainDeck candidate | 3x Evelynn - Entrancing, 2x Kha'Zix - Mutating Horror, 2x Pyke - Returned, 2x Sona - Harmonious, 3x Teemo - Scout, 1x Vex - Apathetic |
| Legend | 1x Vex - Gloomist |
| Main Deck | 2x Back Off, 2x Boots of Swiftness, 2x Defy, 3x Discipline, 2x Edge of Night, 2x Ember Monk, 3x Emperor's Divide, 2x Existential Dread, 2x Gust, 3x Mutated Mouser, 1x Star-Crossed, 1x Switcheroo, 2x Treasure Hunter |
| Rune Deck | 5x Calm Rune, 7x Chaos Rune |

Top blockers:

- Emperor's Divide: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Existential Dread: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Bandle Tree: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Star Spring: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Recommended Implementation Order

- Audit Vex after Diana/Aurora unless playtester reports make it urgent.
