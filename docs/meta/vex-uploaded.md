# Vex, Gloomist Uploaded Meta Deck Audit

Source raw file: `decks/meta/raw/vex_top_8_at_s3_zhongshan_city_challenge.txt`
Normalized deck: `decks/meta/normalized/vex_top_8_at_s3_zhongshan_city_challenge.json`
Event/result: vex top 8 at s3 zhongshan city challenge

## Validation

- Total cards: 56
- Inferred main deck count: 37
- Rune count: 12
- Battlefield count: 3
- Legend count: 1
- Champion candidate count: 11
- Constructed shape check: Needs review

- Expected 39 Main Deck cards after one chosen Champion candidate, found 37.

## Support Summary

- Status: Blocked
- Supported: 2
- Partial: 21
- Unsupported: 3
- Not Audited: 1
- Enforced playable: No

## Unresolved Cards

- Allay - Eager Admirer (UNL-041): No local RiftForge card matched Allay - Eager Admirer in Unleashed.

## Top Blockers

- Existential Dread (UNL-134): UNSUPPORTED - Bounce / return. Blocked in enforced play: this card's effect is not supported yet.
- Blast Cone (UNL-133): UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Allay - Eager Admirer (UNL-041): NOT_AUDITED - Unresolved / missing card data. No local RiftForge card matched Allay - Eager Admirer in Unleashed.
- Switcheroo (SFD-145): UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Vex - Gloomist (UNL-193): PARTIAL - Legend text. Playable for alpha testing, but card-specific behavior may be incomplete.

## Normalized Sections

| Section | Cards |
| --- | --- |
| Legend | 1x Vex - Gloomist (UNL-193) |
| Main Deck | 2x Charm (OGN-043), 2x Existential Dread (UNL-134), 1x Blast Cone (UNL-133), 1x Back Off (UNL-042), 2x Mutated Mouser (UNL-036), 1x Switcheroo (SFD-145), 2x Edge of Night (SFD-139), 3x Overzealous Fan (SFD-128), 1x Not So Fast (SFD-045), 2x Defy (OGN-045), 2x Gust (OGN-169), 2x Ember Monk (OGN-167), 3x Zhonya's Hourglass (OGN-077), 3x Discipline (OGN-058) |
| Champion / MainDeck candidate | 1x Vex - Apathetic (UNL-150), 3x Evelynn - Entrancing (UNL-141), 2x Draven - Audacious (SFD-148), 1x Janna - Savior (SFD-053), 2x Teemo - Scout (OGN-197), 2x Sona - Harmonious (OGN-073) |
| Unresolved | 2x Allay - Eager Admirer (UNL-041) |
| Battlefields | 1x The Papertree (SFD-219), 1x Trifarian War Camp (OGN-294), 1x Bandle Tree (OGN-278) |
| Rune Deck | 6x Calm Rune (OGN-042), 6x Chaos Rune (OGN-166) |

## Recommended Implementation Order

- Defer until Diana/Aurora unless tester demand rises; blockers include Blast Cone, Emperor's Divide, Switcheroo, and hidden/facedown pieces. Charm now has narrow Partial support.
