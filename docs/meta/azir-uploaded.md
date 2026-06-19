# Azir, Emperor of the Sands Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/azir_wins_lille_regional_qualifier.txt`
Normalized deck: `decks/meta/normalized/azir_wins_lille_regional_qualifier.json`
Event/result: azir wins lille regional qualifier

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
- Partial: 19
- Unsupported: 2
- Not Audited: 0
- Enforced playable: No

## Unresolved Cards

- None.

## Top Blockers

- Facebreaker (OGN-220): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Hidden Blade (OGN-213): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Azir - Emperor of the Sands (SFD-197): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Trifarian War Camp (OGN-294): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.
- Seat of Power (SFD-217): PARTIAL - Battlefield effect. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Azir - Emperor of the Sands (SFD-197) |
| Battlefields | 1x Trifarian War Camp (OGN-294), 1x Seat of Power (SFD-217), 1x Hall of Legends (SFD-210) |
| Main Deck | 3x Arise! (SFD-198), 2x Sacred Shears (SFD-172), 3x Deathgrip (SFD-163), 3x B.F. Sword (SFD-161), 3x Guards! (SFD-154), 3x Eye of the Herald (SFD-153), 1x Guardian Angel (SFD-051), 3x Brutalizer (SFD-042), 3x Doran's Shield (SFD-033), 3x Desert's Call (SFD-031), 1x Salvage (OGN-224), 1x Facebreaker (OGN-220), 3x Hidden Blade (OGN-213), 3x Discipline (OGN-058), 2x En Garde (OGN-046), 2x Defy (OGN-045) |
| Champion / MainDeck candidate | 1x Azir - Sovereign (SFD-177) |
| Rune Deck | 6x Calm Rune (OGN-042), 6x Order Rune (OGN-214) |

## Recommended Implementation Order

- Defer until higher-priority interaction decks; blockers include Charm, Facebreaker, Hidden Blade, and Wind Wall.
