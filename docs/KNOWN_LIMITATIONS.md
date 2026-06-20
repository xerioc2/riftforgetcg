# RiftForge Alpha Known Limitations

RiftForge is an unofficial fan-made alpha client. It is useful for focused
playtesting, but it is not a complete Riftbound rules implementation yet.

## What Works Best

- Creating rooms, selecting decks, readying, and starting games.
- Human vs RiftBot smoke tests.
- Mulligan, basic phase flow, and visible action guidance.
- Pre-mulligan Battlefield selection and current 1v1 Duel/bot games with two
  active shared Battlefield lanes.
- Playing supported Units to Base, moving them to active Battlefield lanes, and
  moving ready Units/Champions between active lanes during supported Main Phase
  movement.
- In-play Rune cards display as public card-backed resource plaques while Rune
  Deck contents remain hidden.
- Basic `[Equip]` flow: Gear plays to Base first, then equips from Base to a
- friendly Unit or Champion in Base or at the battlefield after paying the
  printed Equip cost for audited Gear.
- Private Stacked Deck-style card selection prompts.
- Narrow bluff-safe chain windows for Stacked Deck/simple public `Draw 1`
  openers and the listed supported Reactions.
- Hidden card masking and opponent-hand privacy projections.
- Support badges and ready warnings for Supported, Partial, Unsupported,
  Banned, and Not Audited cards.

## Partial Or Incomplete

- Active-lane alpha is intentional; current 1v1 Duel/bot games use `bf-0` and
  `bf-1`, while `bf-2` remains reserved for future/non-Duel formats.
- Sunken Temple, Targon's Peak, and Abandoned Hall have narrow exact-card active-lane hooks, but most
  Battlefield effects, hidden Battlefield slots, official "here" targeting,
  richer location rules, and non-Duel active-lane counts are incomplete.
- Reaction, chain, and counterspell timing are not rules-complete. Current
  supported windows are narrow and card/pattern-specific.
- Hidden cards can be hidden, but later play-from-hidden timing is incomplete.
- Hidden Reaction-for-0 is deferred.
- Ambush has a narrow Main-phase foundation; Ambush-as-Reaction and additional
  Ambush costs are incomplete.
- Combat damage assignment uses deterministic server-planned alpha assignments;
  full manual player damage splitting remains deferred.
- Predict/top-deck ordering and private choice prompts are partial.
- Printed Equip rune payments are supported for alpha Equip gear; full
  Equipment effects, production Gear stat modifiers, +Health attach/detach
  semantics, Quick-Draw, Weaponmaster, replacement, and voluntary detach rules
  are deferred.
- Domain/power payment and selected-rune payment are not complete enough for
  tournament-accurate testing.

## Intentionally Blocked

- Unsupported cards in supported-cards-only mode.
- Banned constructed cards and battlefields.
- Non-Equip Gear in enforced play.
- Counterspell/chain cards outside the current narrow supported Reaction list.
- Multi-target cards outside the current narrow paired-target prompt support.

## Deferred Mechanics

- XP, Hunt, Level, and Buff.
- Full battlefield abilities.
- Full Legend and Champion text.
- Official scoring edge cases beyond the current alpha implementation.
- Broad 3+ player invite/priority behavior.
- Tournament match structure and best-of-three flow.

## Reporting Issues

Use the in-game **Copy debug info** button before filing a report. The copied
debug info is designed to be safe to paste publicly: it omits session tokens,
hand contents, deck contents, private choice options, and hidden opponent card
identities.
