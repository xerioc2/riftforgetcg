# Annie, Dark Child Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/annie-dark-child-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| annie-4th-at-lille-regional-qualifier | Annie 4th at Lille Regional Qualifier | 27 | 1 | 21 | 5 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Factory Recall: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Flash: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hard Bargain: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Thermo Beam: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.

## Decklists

### Annie 4th at Lille Regional Qualifier

- Source deck slug: `annie-4th-at-lille-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=annie-4th-at-lille-regional-qualifier
- Extracted file: `decks/meta/annie/annie-4th-at-lille-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Seat of Power, 1x The Arena's Greatest, 1x Zaun Warrens |
| Champion / MainDeck candidate | 1x Annie - Dark Child - Starter, 1x Annie - Stubborn, 3x Kai'Sa - Survivor, 3x Rek'Sai - Breacher, 3x Rengar - Pouncing |
| Main Deck | 2x Cleave, 1x Factory Recall, 3x Ferrous Forerunner, 3x Flash, 2x Gust, 1x Hard Bargain, 1x Long Sword, 1x Mindsplitter, 3x Noxus Hopeful, 3x Pouty Poro, 1x Rebuke, 2x Ride the Wind, 3x Stacked Deck, 1x Switcheroo, 1x Thermo Beam, 2x Tideturner, 3x Traveling Merchant |
| Rune Deck | 6x Chaos Rune, 6x Fury Rune |

Top blockers:

- Factory Recall: UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Flash: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hard Bargain: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Thermo Beam: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.

## Recommended Implementation Order

- Use Annie as the first Aurora-shell input while waiting for a Miss Fortune guide/list; audit shared Aurora cards before implementing one-off pieces.
