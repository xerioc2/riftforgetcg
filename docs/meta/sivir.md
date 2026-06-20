# Sivir, Battle Mistress Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/sivir-battle-mistress-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| sivir-2nd-at-sydney-regional-qualifier | Sivir 2nd at Sydney Regional Qualifier | 22 | 2 | 13 | 7 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Dazzling Aurora: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Flurry of Blades: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Lunar Boon: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Pack of Wonders: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Sabotage: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.

## Decklists

### Sivir 2nd at Sydney Regional Qualifier

- Source deck slug: `sivir-2nd-at-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=sivir-2nd-at-sydney-regional-qualifier
- Extracted file: `decks/meta/sivir/sivir-2nd-at-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Aspirant's Climb, 1x Forgotten Monument, 1x Sigil of the Storm |
| Champion / MainDeck candidate | 1x Sivir - Mercenary |
| Legend | 1x Sivir - Battle Mistress |
| Main Deck | 3x Catalyst of Aeons, 3x Dazzling Aurora, 1x Disposal Order, 3x Elder Dragon, 3x Flurry of Blades, 3x Gust, 2x Last Rites, 3x Lunar Boon, 1x Mindsplitter, 3x Mobilize, 2x Pack of Wonders, 3x Sabotage, 3x Scryer's Bloom, 3x Stacked Deck, 3x Treasure Trove |
| Rune Deck | 6x Body Rune, 6x Chaos Rune |

Top blockers:

- Dazzling Aurora: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Flurry of Blades: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Lunar Boon: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Pack of Wonders: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Sabotage: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.

## Recommended Implementation Order

- Keep Sivir as an additional guide-sourced archetype; prioritize only if reviewers request it.
