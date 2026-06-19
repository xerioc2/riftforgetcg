# LeBlanc, Deceiver Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/leblanc-deceiver-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| leblanc-wins-s3-zhongshan-city-challenge | LeBlanc Wins S3 Zhongshan City Challenge | 24 | 1 | 18 | 5 | 0 | No |
| leblanc-wins-city-challenge-changzhou | LeBlanc wins City Challenge — Changzhou | 25 | 1 | 18 | 6 | 0 | No |
| leblanc-top-8-at-sydney-regional-qualifier | LeBlanc Top 8 at Sydney Regional Qualifier | 26 | 1 | 21 | 4 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Cull the Weak: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Deadly Flourish: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Imperial Decree: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Mirror Image: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Facebreaker: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Shadow's Call: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.
- Baited Hook: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Turn to Dust: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Aspirant's Climb: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Decklists

### LeBlanc Wins S3 Zhongshan City Challenge

- Source deck slug: `leblanc-wins-s3-zhongshan-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=leblanc-wins-s3-zhongshan-city-challenge
- Extracted file: `decks/meta/leblanc/leblanc-wins-s3-zhongshan-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Dusk Rose Lab, 1x Star Spring, 1x Windswept Hillock |
| Champion / MainDeck candidate | 3x Karthus - Eternal, 3x LeBlanc - Fragmented, 2x Vi - Peacekeeper |
| Legend | 1x LeBlanc - Deceiver |
| Main Deck | 3x Black Rose Dignitary, 3x Carrion Dredger, 3x Cull the Weak, 2x Deadly Flourish, 3x Deathgrip, 3x Glasc Mixologist, 2x Hidden Blade, 2x Imperial Decree, 3x Mirror Image, 2x Rift Herald, 3x Ruined Rex, 2x Sacrifice, 3x Soaring Scout, 2x Thousand-Tailed Watcher, 3x Watchful Sentry |
| Rune Deck | 5x Mind Rune, 7x Order Rune |

Top blockers:

- Cull the Weak: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Deadly Flourish: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Imperial Decree: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Mirror Image: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.

### LeBlanc wins City Challenge — Changzhou

- Source deck slug: `leblanc-wins-city-challenge-changzhou`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=leblanc-wins-city-challenge-changzhou
- Extracted file: `decks/meta/leblanc/leblanc-wins-city-challenge-changzhou.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Dusk Rose Lab, 1x Forbidding Waste, 1x Windswept Hillock |
| Champion / MainDeck candidate | 3x Karthus - Eternal, 1x LeBlanc - Fragmented, 2x Vi - Peacekeeper |
| Legend | 1x LeBlanc - Deceiver |
| Main Deck | 3x Black Rose Dignitary, 3x Cull the Weak, 3x Deathgrip, 1x Facebreaker, 3x Glasc Mixologist, 2x Hidden Blade, 3x Honest Broker, 3x Mirror Image, 3x Ruined Rex, 3x Sacrifice, 3x Salvage, 2x Shadow's Call, 3x Soaring Scout, 2x Thousand-Tailed Watcher, 2x Unchecked Power, 3x Watchful Sentry |
| Rune Deck | 4x Mind Rune, 8x Order Rune |

Top blockers:

- Cull the Weak: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Facebreaker: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Mirror Image: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Shadow's Call: UNSUPPORTED - Draw / reveal / deck selection. Blocked in enforced play: this card's effect is not supported yet.

### LeBlanc Top 8 at Sydney Regional Qualifier

- Source deck slug: `leblanc-top-8-at-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=leblanc-top-8-at-sydney-regional-qualifier
- Extracted file: `decks/meta/leblanc/leblanc-top-8-at-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Aspirant's Climb, 1x Star Spring, 1x Windswept Hillock |
| Champion / MainDeck candidate | 3x Ashe - Focused, 2x Galio - Indefatigable, 3x Karthus - Eternal, 1x LeBlanc - Everywhere at Once, 1x LeBlanc - Fragmented, 2x Vi - Peacekeeper |
| Legend | 1x LeBlanc - Deceiver |
| Main Deck | 1x Atakhan, 3x Baited Hook, 3x Black Rose Dignitary, 3x Glasc Mixologist, 3x Harnessed Dragon, 2x Hidden Blade, 3x Mirror Image, 3x Rift Herald, 3x Ruined Rex, 3x Sacrifice, 3x Soaring Scout, 1x Thousand-Tailed Watcher, 1x Turn to Dust, 3x Watchful Sentry |
| Rune Deck | 4x Mind Rune, 8x Order Rune |

Top blockers:

- Baited Hook: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Mirror Image: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Turn to Dust: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Aspirant's Climb: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Recommended Implementation Order

- Audit LeBlanc after Diana/Aurora unless playtester reports make it urgent.
