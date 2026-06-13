# RiftForge Alpha Known Limitations

RiftForge is an unofficial fan-made alpha client. It is useful for focused
playtesting, but it is not a complete Riftbound rules implementation yet.

## What Works Best

- Creating rooms, selecting decks, readying, and starting games.
- Human vs RiftBot smoke tests.
- Mulligan, basic phase flow, and visible action guidance.
- Playing supported Units to Base and moving them to the single alpha battlefield.
- Basic `[Equip]` flow: Gear plays to Base first, then equips from Base to a
  friendly Unit or Champion in Base or at the battlefield.
- Private Stacked Deck-style card selection prompts.
- Hidden card masking and opponent-hand privacy projections.
- Support badges and ready warnings for Supported, Partial, Unsupported,
  Banned, and Not Audited cards.

## Partial Or Incomplete

- Single-battlefield alpha is intentional; official multiple battlefield
  location logic is post-alpha work.
- Reaction, chain, and counterspell timing are not rules-complete.
- Hidden cards can be hidden, but later play-from-hidden timing is incomplete.
- Ambush has a narrow Main-phase foundation; Ambush-as-Reaction and additional
  Ambush costs are incomplete.
- Predict/top-deck ordering and private choice prompts are partial.
- Gear payment precision, Quick-Draw, Weaponmaster, replacement, and voluntary
  detach rules are deferred.
- Domain/power payment and selected-rune payment are not complete enough for
  tournament-accurate testing.

## Intentionally Blocked

- Unsupported cards in supported-cards-only mode.
- Banned constructed cards and battlefields.
- Non-Equip Gear in enforced play.
- Counterspell/chain cards that need a formal stack or reaction window.
- Multi-target cards that need paired target prompts.

## Deferred Mechanics

- XP, Hunt, Level, and Buff.
- Full battlefield abilities.
- Full Legend and Champion text.
- Official scoring edge cases beyond the current alpha implementation.
- Tournament match structure and best-of-three flow.

## Reporting Issues

Use the in-game **Copy debug info** button before filing a report. The copied
debug info is designed to be safe to paste publicly: it omits session tokens,
hand contents, deck contents, private choice options, and hidden opponent card
identities.
