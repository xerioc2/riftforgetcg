# Irelia, Blade Dancer Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/irelia_wins_s3_shanghai_city_challenge.txt`
Normalized deck: `decks/meta/normalized/irelia_wins_s3_shanghai_city_challenge.json`
Event/result: irelia wins s3 shanghai city challenge

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 3
- Constructed shape check: Pass

- No shape issues found.

## Support Summary

- Status: Blocked
- Supported: 4
- Partial: 20
- Unsupported: 3
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Charm (OGN-043): UNSUPPORTED - Movement / location. Blocked in enforced play: this card's effect is not supported yet.
- Zhonya's Hourglass (OGN-077): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- The Syren (OGN-184): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Irelia - Blade Dancer (SFD-195): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Not So Fast (SFD-045): PARTIAL - Reaction / chain / counter. Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Irelia - Blade Dancer (SFD-195) |
| Main Deck | 1x Not So Fast (SFD-045), 3x Scuttle Crab (UNL-053), 1x Back Off (UNL-042), 3x Defiant Dance (SFD-196), 1x Edge of Night (SFD-139), 2x Boots of Swiftness (SFD-133), 2x Guardian Angel (SFD-051), 2x Stellacorn Herder (SFD-048), 3x Lonely Poro (SFD-036), 2x Flash (OGS-011), 3x Charm (OGN-043), 3x Defy (OGN-045), 2x En Garde (OGN-046), 3x Discipline (OGN-058), 1x Zhonya's Hourglass (OGN-077), 1x Ride the Wind (OGN-173), 1x The Syren (OGN-184), 2x Mindsplitter (OGN-192), 1x Tideturner (OGN-199) |
| Battlefields | 1x Abandoned Hall (UNL-205), 1x Sunken Temple (SFD-218), 1x Targon's Peak (OGN-289) |
| Champion / MainDeck candidate | 2x Vex - Apathetic (UNL-150), 1x Irelia - Fervent (SFD-057) |
| Rune Deck | 6x Calm Rune (OGN-042), 6x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Continue current Irelia polish: Charm movement, Zhonya's Hourglass/The Syren Gear-effect blockers, and remaining Partial Legend/Reaction accuracy from the uploaded list.
