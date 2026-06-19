# RiftForge Alpha Release Notes Draft

## Version

`v0.1.0-alpha`

## Date

TBD

## Status

Public alpha playtest build.

## Summary

This build is intended for focused external playtesting of RiftForge's
active-lane alpha. It includes the current starter-deck flow, improved
privacy protections, visible support badges, clearer playtest guidance, and a
safer issue-reporting path.

## Highlights

- Constructed deck setup now separates Legend, chosen Champion, main deck,
  runes, and battlefields.
- Constructed games ask each player to select a Battlefield before mulligan.
  The selected Battlefields are hidden during choice, become public after setup,
  and render as inert hoverable location plaques.
- Current 1v1 Duel/bot games use two active shared Battlefield lanes, `bf-0`
  and `bf-1`. Deck construction still requires three unique Battlefields, but
  that does not mean Duel games use three active lanes.
- Chosen Champions deploy from the Champion zone through the supported Main
  Phase path and require available energy.
- Units play to Base and stay visible on the responsive board.
- Units and Champions can move into active Battlefield lanes or between active
  lanes during supported Main Phase movement. Same-lane opposition starts a
  showdown; different-lane opposition does not.
- Basic Gear lifecycle is supported: play Gear to Base, equip from Base, keep
  attached Gear out of combat, and return it to Base when its host leaves public
  play.
- Correction: equipping now requires paying the printed `[Equip]` cost (energy
  and/or premium rune domains). The earlier alpha behavior that let gear equip
  for free was a bug and has been fixed; the server rejects equips that do not
  cover the printed cost.
- Explicit target selection handles supported targeted effects and shows clear
  feedback for invalid targets.
- In-play Rune cards render as card-backed resource plaques when card data/art
  is available. Hidden Rune Deck contents remain private.
- Stacked Deck-style private choices are owner-only and do not reveal top-deck
  options to opponents or spectators.
- Stacked Deck and simple public `Draw 1` spells can open the current narrow
  chain window. Gust, Defy, Not So Fast, Discipline, and En Garde are the
  current supported chain-backed Reactions.
- Human priority windows are bluff-safe: the focused player can pass manually
  even with no legal Reaction, and opponents do not learn whether a response is
  available. RiftBot may still auto-pass empty windows.
- Hidden and Ambush foundations are present for alpha testing, with later
  timing windows intentionally deferred.
- Supported Action cards can be played in Main Phase and during simplified
  showdown participant windows.
- Showdowns stay inside Main Phase and track their active Battlefield lane.
- Combat damage assignment is now server-planned and deterministic for the
  alpha UI/RiftBot flow. Full manual damage splitting remains deferred.
- Public Unit/Champion cards use server-projected printed/effective stats for
  display. Production Gear stat modifiers are not enabled unless an explicit
  support metadata entry exists.
- Deathknell and Recruit token plumbing support the current starter-card scripts
  for Loyal Poro, Noxian Drummer, and Vanguard Captain.
- RiftBot uses server-computed legal actions and resolves supported private
  choices automatically.
- WebSocket and REST projections mask opponent hands, hidden cards, private
  choices, deck contents, and private logs.
- Playtester UI now includes phase guidance, visible errors, support badges,
  Copy debug info, Report issue, Alpha limits, and a responsive board layout.

## Card Support Notes

- Supported starter cards include current directly tested simple units, runes,
  and selected starter-card scripts.
- Partial cards are playable for alpha testing but may be inaccurate.
- Unsupported cards are blocked in supported-cards-only enforced play.
- Banned cards and battlefields are rejected for constructed decks.
- Not Audited cards are not available in supported-cards-only mode yet.

## Known Limitations

- Active-lane alpha is intentional. Battlefield effects, hidden Battlefield
  slots, official "here" targeting, non-Duel active-lane counts, and full
  official location rules remain deferred.
- Reaction, chain, counterspell timing, and formal priority are not complete.
  The current supported path is narrow and card/pattern-specific.
- Hidden play-from-hidden timing is incomplete.
- Hidden Reaction-for-0 is deferred.
- Ambush-as-Reaction and additional-cost Ambush cards are incomplete.
- Quick-Draw, Weaponmaster, XP, Hunt, Level, Buff, production Gear stat
  modifiers, +Health attach/detach semantics, replacement/prevention effects,
  and most Battlefield abilities are deferred.
- Predict/top-deck ordering and broader private choice prompts are partial.
- Printed Equip rune payments are supported for alpha Equip gear, but broader
  domain/power payment precision is still not tournament-accurate.
- Full manual combat damage splitting and many official combat edge cases are
  deferred.
- 3+ player invitation/priority behavior is not implemented.

## Reporting Issues

Use **Copy debug info** or **Report issue** from the game board before filing a
GitHub issue. The copied payload is safe to paste publicly and excludes session
tokens, hand contents, deck contents, hidden opponent card identities, private
choice options, and private card logs.

Please include:

- Build/release version.
- Room code.
- Human vs human or human vs RiftBot.
- Supported-cards-only mode setting.
- Phase or showdown state.
- Card(s) involved.
- Expected behavior and actual behavior.
- Screenshot or video if possible.
- Copied debug info.

## Validation Checklist

- `npm run build`
- `cd server && mvn -q test`
- `cd server && mvn -q -DskipTests compile`
- `git diff --check`
- Desktop installer smoke test
- Server `/api/health` check

## Fan Project Notice

RiftForge is an unofficial fan-made project. It is not affiliated with,
endorsed, sponsored, or approved by Riot Games, League of Legends, Riftbound,
or UVS Games. Official rules, card text, tournament documents, banlists, and
errata remain the source of truth.
