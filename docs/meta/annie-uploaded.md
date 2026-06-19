# Annie, Dark Child Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/annie_4th_at_lille_regional_qualifier.txt`
Normalized deck: `decks/meta/normalized/annie_4th_at_lille_regional_qualifier.json`
Event/result: annie 4th at lille regional qualifier

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 0
- Champion candidate count: 10
- Constructed shape check: Needs review

- Expected exactly 1 Legend, found 0.

## Support Summary

- Status: Blocked
- Supported: 1
- Partial: 19
- Unsupported: 2
- Not Audited: 1
- Enforced playable: No

## Unresolved Cards

- Annie - Dark Child - Starter (OGS-017): No local RiftForge card matched Annie - Dark Child - Starter in Proving Grounds.

## Top Blockers

- Annie - Dark Child - Starter (OGS-017): NOT_AUDITED - Unresolved / missing card data. No local RiftForge card matched Annie - Dark Child - Starter in Proving Grounds.
- Hard Bargain (SFD-136): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Flash (OGS-011): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- The Arena's Greatest (OGN-290): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Seat of Power (SFD-217): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Unresolved | 1x Annie - Dark Child - Starter (OGS-017) |
| Battlefields | 1x The Arena's Greatest (OGN-290), 1x Seat of Power (SFD-217), 1x Zaun Warrens (OGN-298) |
| Main Deck | 2x Hard Bargain (SFD-136), 1x Long Sword (SFD-022), 3x Ferrous Forerunner (SFD-021), 3x Flash (OGS-011), 2x Cleave (OGN-004), 2x Tideturner (OGN-199), 3x Noxus Hopeful (OGN-012), 3x Pouty Poro (OGN-013), 1x Mindsplitter (OGN-192), 2x Rebuke (OGN-172), 2x Ride the Wind (OGN-173), 3x Stacked Deck (OGN-183), 3x Traveling Merchant (OGN-185) |
| Champion / MainDeck candidate | 3x Rek'Sai - Breacher (SFD-029), 3x Rengar - Pouncing (SFD-025), 1x Annie - Stubborn (OGS-010), 3x Kai'Sa - Survivor (OGN-039) |
| Rune Deck | 6x Fury Rune (OGN-007), 6x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Use Annie as the first Aurora-shell list while waiting for Miss Fortune; focus on shared Reaction/bounce/damage blockers only after MF is available.
