# Diana, Scorn of the Moon Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/diana_wins_s3_suzhou_city_challenge.txt`
Normalized deck: `decks/meta/normalized/diana_wins_s3_suzhou_city_challenge.json`
Event/result: diana wins s3 suzhou city challenge

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 6
- Constructed shape check: Pass

- No shape issues found.

## Support Summary

- Status: Blocked
- Supported: 1
- Partial: 20
- Unsupported: 4
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Hard Bargain (SFD-136): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Abandon (UNL-131): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Flash (OGS-011): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- The Syren (OGN-184): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Diana - Scorn of the Moon (UNL-197): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Diana - Scorn of the Moon (UNL-197) |
| Main Deck | 1x Hard Bargain (SFD-136), 3x Moonfall (UNL-198), 1x Abandon (UNL-131), 2x Star-Crossed (UNL-128), 2x Eclipse (UNL-063), 1x Last Rites (SFD-150), 2x Flash (OGS-011), 2x Tideturner (OGN-199), 2x Traveling Merchant (OGN-185), 1x The Syren (OGN-184), 3x Stacked Deck (OGN-183), 3x Ride the Wind (OGN-173), 3x Gust (OGN-169), 2x Thousand-Tailed Watcher (OGN-116), 3x Ravenbloom Student (OGN-103), 3x Stupefy (OGN-095) |
| Battlefields | 1x Star Spring (UNL-215), 1x Abandoned Hall (UNL-205), 1x Targon's Peak (OGN-289) |
| Champion / MainDeck candidate | 3x Hwei - Brooding Painter (UNL-080), 1x Diana - Lunari (UNL-079), 2x Fizz - Trickster (SFD-140) |
| Rune Deck | 5x Mind Rune (OGN-089), 7x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Diana remains next: start with repeated interaction blockers such as Abandon/Flash/Hard Bargain or a shared Gear/effect blocker if it appears in the chosen support slice.
