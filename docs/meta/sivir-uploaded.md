# Sivir, Battle Mistress Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/sivir_2nd_at_sydney_regional_qualifier.txt`
Normalized deck: `decks/meta/normalized/sivir_2nd_at_sydney_regional_qualifier.json`
Event/result: sivir 2nd at sydney regional qualifier

## Validation

- Total cards: 56
- Inferred main deck count: 39
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 1
- Constructed shape check: Pass

- No shape issues found.

## Support Summary

- Status: Blocked
- Supported: 2
- Partial: 13
- Unsupported: 7
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Scryer's Bloom (UNL-136): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Lunar Boon (UNL-125): UNSUPPORTED - Reaction / chain / counter. Blocked in enforced play: this card's effect is not supported yet.
- Treasure Trove (OGN-186): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Pack of Wonders (OGN-181): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Dazzling Aurora (OGN-160): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Sivir - Battle Mistress (SFD-203) |
| Main Deck | 1x Mindsplitter (OGN-192), 3x Scryer's Bloom (UNL-136), 3x Lunar Boon (UNL-125), 3x Elder Dragon (UNL-118), 1x Disposal Order (UNL-103), 2x Last Rites (SFD-150), 3x Treasure Trove (OGN-186), 3x Stacked Deck (OGN-183), 2x Pack of Wonders (OGN-181), 3x Gust (OGN-169), 3x Dazzling Aurora (OGN-160), 3x Sabotage (OGN-156), 3x Catalyst of Aeons (OGN-138), 3x Mobilize (OGN-134), 3x Flurry of Blades (OGN-133) |
| Battlefields | 1x Forgotten Monument (SFD-209), 1x Sigil of the Storm (OGN-287), 1x Aspirant's Climb (OGN-276) |
| Champion / MainDeck candidate | 1x Sivir - Mercenary (SFD-143) |
| Rune Deck | 6x Body Rune (OGN-126), 6x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Keep as additional data; blockers include Dazzling Aurora, Flurry of Blades, Lunar Boon, Pack of Wonders, and Sabotage.
