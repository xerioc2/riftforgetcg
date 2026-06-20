# Draven, Glorious Executioner Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/draven_wins_new_zealand_10k_open.txt`
Normalized deck: `decks/meta/normalized/draven_wins_new_zealand_10k_open.json`
Event/result: draven wins new zealand 10k open

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 7
- Constructed shape check: Pass

- No shape issues found.

## Support Summary

- Status: Blocked
- Supported: 1
- Partial: 20
- Unsupported: 3
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Switcheroo (SFD-145): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Hard Bargain (SFD-136): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Falling Star (OGN-029): UNSUPPORTED - Basic or descriptor-only. Blocked in enforced play: this card's effect is not supported yet.
- Draven - Glorious Executioner (SFD-185): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Fury Rune (OGN-007): PARTIAL - Rune/payment. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Draven - Glorious Executioner (SFD-185) |
| Rune Deck | 6x Fury Rune (OGN-007), 6x Chaos Rune (OGN-166) |
| Battlefields | 1x Treasure Hoard (SFD-220), 1x Forge of the Fluft (SFD-208), 1x Targon's Peak (OGN-289) |
| Main Deck | 3x Spinning Axe (SFD-186), 1x Switcheroo (SFD-145), 1x Edge of Night (SFD-139), 1x Hard Bargain (SFD-136), 2x Treasure Hunter (SFD-130), 3x Overzealous Fan (SFD-128), 3x Ferrous Forerunner (SFD-021), 3x Tideturner (OGN-199), 3x Stacked Deck (OGN-183), 1x Cleave (OGN-004), 2x Rebuke (OGN-172), 3x Noxus Hopeful (OGN-012), 2x Falling Star (OGN-029), 3x Ride the Wind (OGN-173), 2x Brynhir Thundersong (OGN-026) |
| Champion / MainDeck candidate | 3x Kai'Sa - Survivor (OGN-039), 1x Draven - Showboat (OGN-028), 3x Darius - Trifarian (OGN-027) |

## Recommended Implementation Order

- Treat as additional data for Aurora/Draven shell work; blockers include Switcheroo, Edge of Night, Hard Bargain, and token/hidden pieces.
