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

- Status: Partial
- Supported: 1
- Partial: 24
- Unsupported: 0
- Not Audited: 0
- Enforced playable: Yes

## Unresolved Cards

- None.

## Top Blockers

- Diana - Scorn of the Moon (UNL-197): PARTIAL - Legend text. Partial: Diana's exact Legend ability can be activated by the focused showdown player: exhaust Diana to add 1 showdown-only Energy. That restricted Energy can be spent only while a showdown is active. Broader official Reaction/resource timing remains deferred.
- Hard Bargain (SFD-136): PARTIAL - Reaction / chain / counter. Partial: Hard Bargain can counter a supported public pending spell chain item unless that spell's controller pays 2 energy through an owner-only prompt. Repeat, broad official Reaction timing, ability targets, hidden/private chain targets, and countering counters remain deferred.
- Star Spring (UNL-215): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Abandoned Hall (UNL-205): PARTIAL - Battlefield effect. Partial: exact spell-play optional trigger is implemented for active Battlefield lanes. The spell's controller may choose a friendly public Unit/Champion here to get +1 Might this turn; full official trigger stacking and broader Battlefield rules remain deferred.
- Moonfall (UNL-198): PARTIAL - Movement / location. Partial: exact Action support chooses an active battlefield where you have a public Unit/Champion, may move one enemy public battlefield Unit/Champion there, then gives enemy units there -2 Might this turn. Full official timing and broader movement edge cases remain incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Diana - Scorn of the Moon (UNL-197) |
| Main Deck | 1x Hard Bargain (SFD-136), 3x Moonfall (UNL-198), 1x Abandon (UNL-131), 2x Star-Crossed (UNL-128), 2x Eclipse (UNL-063), 1x Last Rites (SFD-150), 2x Flash (OGS-011), 2x Tideturner (OGN-199), 2x Traveling Merchant (OGN-185), 1x The Syren (OGN-184), 3x Stacked Deck (OGN-183), 3x Ride the Wind (OGN-173), 3x Gust (OGN-169), 2x Thousand-Tailed Watcher (OGN-116), 3x Ravenbloom Student (OGN-103), 3x Stupefy (OGN-095) |
| Battlefields | 1x Star Spring (UNL-215), 1x Abandoned Hall (UNL-205), 1x Targon's Peak (OGN-289) |
| Champion / MainDeck candidate | 3x Hwei - Brooding Painter (UNL-080), 1x Diana - Lunari (UNL-079), 2x Fizz - Trickster (SFD-140) |
| Rune Deck | 5x Mind Rune (OGN-089), 7x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Diana is enforced-playable by gate and selectable as a playtest bot deck, but remains Partial-heavy. Next fidelity slice should focus on Star Spring/Battlefield effects, remaining Champion text, or another repeated Diana Partial blocker before broader timing work.
