# Fiora, Grand Duelist Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/fiora_wins_s3_beijing_city_challenge.txt`
Normalized deck: `decks/meta/normalized/fiora_wins_s3_beijing_city_challenge.json`
Event/result: fiora wins s3 beijing city challenge

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
- Supported: 2
- Partial: 20
- Unsupported: 2
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Hidden Blade (OGN-213): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Challenge (OGN-128): UNSUPPORTED - Damage / destroy. Blocked in enforced play: this card's effect is not supported yet.
- Fiora - Grand Duelist (SFD-205): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Punch First (SFD-097): PARTIAL - Basic or descriptor-only. Playable for alpha testing, but card-specific behavior may be incomplete.
- Sacrifice (UNL-173): PARTIAL - Reaction / chain / counter. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Fiora - Grand Duelist (SFD-205) |
| Main Deck | 2x Punch First (SFD-097), 2x Sacrifice (UNL-173), 2x Shepherd's Heirloom (UNL-158), 2x Kinkou Initiate (UNL-097), 2x Grim Resolve (UNL-095), 3x Riposte (SFD-206), 3x Unsung Hero (SFD-167), 3x B.F. Sword (SFD-161), 1x Sea Monkey (SFD-098), 2x Spectral Matron (OGN-226), 2x Hidden Blade (OGN-213), 3x Pit Rookie (OGN-136), 3x First Mate (OGN-132), 3x Challenge (OGN-128) |
| Champion / MainDeck candidate | 2x Ashe - Focused (UNL-169), 1x Akshan - Mischievous (SFD-109), 1x Fiora - Victorious (OGN-232), 3x Sett - Brawler (OGN-164) |
| Battlefields | 1x Sunken Temple (SFD-218), 1x Trifarian War Camp (OGN-294), 1x Monastery of Hirana (OGN-282) |
| Rune Deck | 5x Body Rune (OGN-126), 7x Order Rune (OGN-214) |

## Recommended Implementation Order

- Treat as additional data for equipment/combat work; blockers include Challenge, Hidden Blade, and several Partial Gear/combat texts.
