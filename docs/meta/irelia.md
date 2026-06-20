# Irelia, Blade Dancer Meta Deck Audit

Captured: 2026-06-19

Source guide: https://riftbound.gg/irelia-blade-dancer-guide/

## Extraction Status

The Riftbound.gg guide page embeds one or more deck widgets with `data-deck` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.

Extraction status: Extracted

## Support Summary

| Deck slug | Deck name | Unique cards | Supported | Partial | Unsupported | Not Audited | Enforced playable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| irelia-wins-s3-shanghai-city-challenge | Irelia Wins S3 Shanghai City Challenge | 33 | 5 | 26 | 2 | 0 | No |
| irelia-wins-sydney-regional-qualifier | Irelia wins Sydney Regional Qualifier | 24 | 3 | 21 | 0 | 0 | Yes |

## Unresolved Cards

- None.

## Shared Top Blockers

- Heart of Dark Ice: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Sunken Temple: PARTIAL - Battlefield effect. Partial: exact conquer-with-Mighty optional pay-1 draw trigger is implemented for active Battlefield lanes. Broader Mighty threshold timing and full official location rules remain deferred.
- Targon's Peak: PARTIAL - Battlefield effect. Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred.
- Fizz - Trickster: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Irelia - Fervent: PARTIAL - Champion text. Partial: the supported explicit-ready trigger gives Irelia +1 Might this turn when her controller readies her through a registered effect. Deflect targeting tax remains heuristic, choose-trigger coverage is incomplete, and automatic ready-step trigger timing remains deferred.

## Decklists

### Irelia Wins S3 Shanghai City Challenge

- Source deck slug: `irelia-wins-s3-shanghai-city-challenge`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=irelia-wins-s3-shanghai-city-challenge
- Extracted file: `decks/meta/irelia/irelia-wins-s3-shanghai-city-challenge.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Sunken Temple, 1x Targon's Peak |
| Champion / MainDeck candidate | 1x Irelia - Fervent, 2x Vex - Apathetic |
| Legend | 1x Irelia - Blade Dancer |
| Main Deck | 1x Back Off, 2x Boots of Swiftness, 3x Charm, 3x Defiant Dance, 3x Defy, 2x Disarming Rake, 3x Discipline, 1x Edge of Night, 2x En Garde, 2x Flash, 2x Guardian Angel, 2x Gust, 1x Hard Bargain, 1x Heart of Dark Ice, 3x Lonely Poro, 2x Mindsplitter, 1x Not So Fast, 1x Ride the Wind, 3x Scuttle Crab, 1x Star-Crossed, 2x Stellacorn Herder, 1x Switcheroo, 1x The Syren, 1x Tideturner, 1x Zhonya's Hourglass |
| Rune Deck | 6x Calm Rune, 6x Chaos Rune |

Top blockers:

- Heart of Dark Ice: UNSUPPORTED - Equipment lifecycle/effect. Blocked in enforced play: this card's effect is not supported yet.
- Switcheroo: UNSUPPORTED - Hidden / facedown. Blocked in enforced play: this card's effect is not supported yet.
- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Sunken Temple: PARTIAL - Battlefield effect. Partial: exact conquer-with-Mighty optional pay-1 draw trigger is implemented for active Battlefield lanes. Broader Mighty threshold timing and full official location rules remain deferred.
- Targon's Peak: PARTIAL - Battlefield effect. Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred.

### Irelia wins Sydney Regional Qualifier

- Source deck slug: `irelia-wins-sydney-regional-qualifier`
- Source API: https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=irelia-wins-sydney-regional-qualifier
- Extracted file: `decks/meta/irelia/irelia-wins-sydney-regional-qualifier.json`

| Section | Cards |
| --- | --- |
| Battlefields | 1x Abandoned Hall, 1x Sunken Temple, 1x Targon's Peak |
| Champion / MainDeck candidate | 1x Fizz - Trickster, 1x Irelia - Fervent |
| Legend | 1x Irelia - Blade Dancer |
| Main Deck | 1x Adaptatron, 3x Boots of Swiftness, 2x Charm, 3x Defiant Dance, 3x Defy, 3x Discipline, 2x En Garde, 3x Guardian Angel, 1x Gust, 2x Not So Fast, 2x Ride the Wind, 3x Scuttle Crab, 2x Stacked Deck, 2x Star-Crossed, 3x Stellacorn Herder, 3x Tideturner |
| Rune Deck | 6x Calm Rune, 6x Chaos Rune |

Top blockers:

- Abandoned Hall: PARTIAL - Battlefield effect. Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.
- Sunken Temple: PARTIAL - Battlefield effect. Partial: exact conquer-with-Mighty optional pay-1 draw trigger is implemented for active Battlefield lanes. Broader Mighty threshold timing and full official location rules remain deferred.
- Targon's Peak: PARTIAL - Battlefield effect. Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred.
- Fizz - Trickster: PARTIAL - Champion text. Playable for alpha testing, but card-specific behavior may be incomplete.
- Irelia - Fervent: PARTIAL - Champion text. Partial: the supported explicit-ready trigger gives Irelia +1 Might this turn when her controller readies her through a registered effect. Deflect targeting tax remains heuristic, choose-trigger coverage is incomplete, and automatic ready-step trigger timing remains deferred.

## Recommended Implementation Order

- Continue the current Irelia slice: tighten unsupported Irelia spells, Champion/Legend text, and Battlefield effects that appear in these guide lists.
