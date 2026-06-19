# LeBlanc, Deceiver Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/leblanc_wins_s3_zhongshan_city_challenge.txt`
Normalized deck: `decks/meta/normalized/leblanc_wins_s3_zhongshan_city_challenge.json`
Event/result: leblanc wins s3 zhongshan city challenge

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 8
- Constructed shape check: Pass

- No shape issues found.

## Support Summary

- Status: Blocked
- Supported: 1
- Partial: 16
- Unsupported: 4
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Mirror Image (UNL-200): UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Deadly Flourish (UNL-073): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade (OGN-213): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Cull the Weak (OGN-209): UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- LeBlanc - Deceiver (UNL-199): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x LeBlanc - Deceiver (UNL-199) |
| Main Deck | 3x Ruined Rex (UNL-067), 3x Mirror Image (UNL-200), 2x Sacrifice (UNL-173), 3x Carrion Dredger (UNL-153), 2x Deadly Flourish (UNL-073), 3x Glasc Mixologist (SFD-165), 3x Deathgrip (SFD-163), 3x Soaring Scout (OGN-216), 2x Hidden Blade (OGN-213), 3x Cull the Weak (OGN-209), 2x Thousand-Tailed Watcher (OGN-116), 3x Watchful Sentry (OGN-096) |
| Battlefields | 1x Star Spring (UNL-215), 1x Dusk Rose Lab (UNL-209), 1x Windswept Hillock (OGN-297) |
| Champion / MainDeck candidate | 2x Vi - Peacekeeper (UNL-176), 3x LeBlanc - Fragmented (UNL-172), 3x Karthus - Eternal (OGN-236) |
| Rune Deck | 5x Mind Rune (OGN-089), 7x Order Rune (OGN-214) |

## Recommended Implementation Order

- Defer until Diana/Aurora unless tester demand rises; blockers skew toward destroy/hidden/counter-style spell effects.
