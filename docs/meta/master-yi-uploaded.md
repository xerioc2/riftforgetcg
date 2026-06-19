# Master Yi, Wuju Bladesman Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/master_yi_wins_s3_guangzhou_city_challenge.txt`
Normalized deck: `decks/meta/normalized/master_yi_wins_s3_guangzhou_city_challenge.json`
Event/result: master yi wins s3 guangzhou city challenge

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 0
- Champion candidate count: 6
- Constructed shape check: Needs review

- Expected exactly 1 Legend, found 0.

## Support Summary

- Status: Blocked
- Supported: 3
- Partial: 19
- Unsupported: 0
- Not Audited: 1
- Enforced playable: No

## Unresolved Cards

- Master Yi - Wuju Bladesman - Starter (OGS-019): No local RiftForge card matched Master Yi - Wuju Bladesman - Starter in Proving Grounds.

## Top Blockers

- Master Yi - Wuju Bladesman - Starter (OGS-019): NOT_AUDITED - Unresolved / missing card data. No local RiftForge card matched Master Yi - Wuju Bladesman - Starter in Proving Grounds.
- Amateur Recital (UNL-207): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Rengar - Trophy Hunter (UNL-120): PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Master Yi - Tempered (UNL-113): PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Dragonsoul Sage (UNL-093): PARTIAL - Reaction / chain / counter. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Unresolved | 1x Master Yi - Wuju Bladesman - Starter (OGS-019) |
| Main Deck | 3x Lonely Poro (SFD-036), 2x Dragonsoul Sage (UNL-093), 2x Vilemaw (UNL-060), 3x Scuttle Crab (UNL-053), 2x Back Off (UNL-042), 3x Punch First (SFD-097), 3x Not So Fast (SFD-045), 3x First Mate (OGN-132), 2x Zhonya's Hourglass (OGN-077), 1x Tasty Faefolk (OGN-075), 3x Discipline (OGN-058), 2x En Garde (OGN-046), 3x Defy (OGN-045), 2x Charm (OGN-043) |
| Battlefields | 1x Amateur Recital (UNL-207), 1x Startipped Peak (OGN-288), 1x Grove of the God-Willow (OGN-280) |
| Champion / MainDeck candidate | 3x Rengar - Trophy Hunter (UNL-120), 1x Master Yi - Tempered (UNL-113), 2x Janna - Savior (SFD-053) |
| Rune Deck | 6x Calm Rune (OGN-042), 6x Body Rune (OGN-126) |

## Recommended Implementation Order

- Review gameplay notes before implementation; likely blockers are Gear, Champion/Legend text, and remaining movement/location edge cases beyond Charm's narrow alpha support.
