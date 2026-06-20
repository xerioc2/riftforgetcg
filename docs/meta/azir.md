# Azir, Emperor of the Sands Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/azir-emperor-of-the-sands-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| azir-wins-lille-regional-qualifier | Azir Wins Lille Regional Qualifier | 26 | 2 | 20 | 4 | 0 | No |

## Unresolved Cards

- None.

## Shared Top Blockers

- Cull the Weak: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Facebreaker: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Wind Wall: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hall of Legends: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Decklists

### Azir Wins Lille Regional Qualifier

- Source deck slug: `azir-wins-lille-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=azir-wins-lille-regional-qualifier
- Extracted file: `decks/meta/azir/azir-wins-lille-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Hall of Legends, 1x Seat of Power, 1x Trifarian War Camp |
| Champion / MainDeck candidate | 1x Azir - Sovereign |
| Legend | 1x Azir - Emperor of the Sands |
| Main Deck | 3x Arise!, 3x B.F. Sword, 3x Brutalizer, 2x Charm, 1x Cull the Weak, 3x Deathgrip, 1x Defy, 3x Desert's Call, 3x Discipline, 3x Doran's Shield, 2x En Garde, 3x Eye of the Herald, 1x Facebreaker, 1x Guardian Angel, 3x Guards!, 3x Hidden Blade, 2x Sacred Shears, 1x Salvage, 2x Wind Wall |
| Rune Deck | 6x Calm Rune, 6x Order Rune |

Top blockers:

- Cull the Weak: UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Facebreaker: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Wind Wall: UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hall of Legends: PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Recommended Implementation Order

- Audit Azir token/location/scoring needs after higher-priority interaction decks.
