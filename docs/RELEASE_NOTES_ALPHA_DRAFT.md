# RiftForge Alpha Release Notes Draft

## Version

`v0.1.0-alpha`

## Date

TBD

## Status

Public alpha playtest build.

## Summary

This build is intended for focused external playtesting of RiftForge's
single-battlefield alpha. It includes the current starter-deck flow, improved
privacy protections, visible support badges, clearer playtest guidance, and a
safer issue-reporting path.

## Highlights

- Constructed deck setup now separates Legend, chosen Champion, main deck,
  runes, and battlefields.
- Chosen Champions deploy from the Champion zone through the supported Main
  Phase path and require available energy.
- Units play to Base and stay visible on the responsive board.
- Basic Gear lifecycle is supported: play Gear to Base, equip from Base, keep
  attached Gear out of combat, and return it to Base when its host leaves public
  play.
- Explicit target selection handles supported targeted effects and shows clear
  feedback for invalid targets.
- Stacked Deck-style private choices are owner-only and do not reveal top-deck
  options to opponents or spectators.
- Hidden and Ambush foundations are present for alpha testing, with later
  timing windows intentionally deferred.
- Supported Action cards can be played in Main Phase and during simplified
  showdown participant windows.
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

- Single-battlefield alpha is intentional; official multiple-battlefield rules
  are post-alpha work.
- Reaction, chain, counterspell timing, and formal priority are not complete.
- Hidden play-from-hidden timing is incomplete.
- Ambush-as-Reaction and additional-cost Ambush cards are incomplete.
- Quick-Draw, Weaponmaster, XP, Hunt, Level, Buff, and most battlefield
  abilities are deferred.
- Predict/top-deck ordering and broader private choice prompts are partial.
- Domain/power payment precision is still not tournament-accurate.

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
