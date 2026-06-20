# RiftForge Alpha Release Notes Draft

## Version

`v0.1.0-alpha`

## Date

TBD

## Status

Public alpha playtest build.

## Summary

This build is intended for focused external playtesting of RiftForge's
active-lane alpha. The headline addition is the first enforced-playable uploaded
meta deck — uploaded **Irelia** — which is now the default (and selectable)
RiftBot playtest deck. It is enforced-playable but **Partial-heavy**: many of its
cards run conservative alpha approximations, not full official rules. This build
also adds several exact-card effects, a small activated-ability and would-die
replacement foundation, and the usual privacy, layout, and playtest-guidance
polish.

RiftForge is **not** rules-complete and the uploaded Irelia deck is **not** a
golden/reference deck. It is a playtest tool for surfacing issues through real
games.

## Highlights

### Decks and the default playtest bot

- Uploaded competitive meta decklists are imported, normalized, and audited for
  support status. Exact uploaded exports are preserved; per-deck support audits
  are generated.
- Uploaded **Irelia** is the first deck with no Unsupported/Not Audited cards and
  is now the **default RiftBot playtest deck** (also selectable in the Lobby). It
  remains Partial-heavy — several cards use alpha approximations.
- Constructed deck validation: 1 Legend, 1 chosen Champion role card, exactly 39
  MainDeck cards after the chosen Champion, 12 Runes, 3 unique Battlefields, and a
  3-copy limit. The Rune deck now shuffles at setup.

### Newly supported cards (alpha slices — see caveats)

- **Defiant Dance** — chain-backed Reaction giving one public unit +2 Might and
  another -2 Might this turn.
- **Flash** — chain-backed Reaction recalling one or two friendly battlefield
  units to Base.
- **Charm** — Spell that recalls a targeted enemy battlefield Unit/Champion to its
  Base (alpha "to Base" interpretation of "move").
- **The Syren** — Gear with a Main-Phase activated ability: pay 1 energy and
  exhaust to recall a friendly battlefield Unit/Champion to Base.
- **Zhonya's Hourglass** — Main-Phase activated arming: protect a chosen friendly
  Unit/Champion; the next time it would die, Zhonya is destroyed instead and the
  unit heals, exhausts, and recalls to Base.
- **Abandon** — chain-backed counter that returns the countered spell to its
  owner's hand and creates a private Predict choice.
- **Irelia – Fervent** gains +1 Might this turn when readied through a supported
  effect.

All of the above are marked **Partial**: official "any-time" Reaction timing,
hidden timing, competing-replacement choice, and various card-specific edge cases
remain deferred.

### Board, movement, and combat

- Current 1v1 Duel/bot games use two active shared Battlefield lanes, `bf-0` and
  `bf-1`. Deck construction still requires three unique Battlefields, but that
  does not mean Duel games use three active lanes.
- Units/Champions can move into active lanes, between active lanes, and back to
  Base during supported Main-Phase movement (no active showdown). Same-lane
  opposition starts a showdown; different-lane opposition does not. Leaving a lane
  recomputes that lane's control.
- Showdowns stay inside Main Phase and track their active Battlefield lane.
- Combat damage assignment is server-planned and deterministic for the alpha
  UI/RiftBot flow. Full manual damage splitting remains deferred.
- Public Unit/Champion cards use server-projected printed/effective stats for
  display. No production Gear stat modifiers are enabled (the audited starter Gear
  have no stat-modifier text).

### Equipment, runes, and reactions

- Gear lifecycle: play Gear to Base, equip from Base as a separate paid move, keep
  attached Gear out of combat, and return it to Base when its host leaves play.
- Correction: equipping requires paying the printed `[Equip]` cost (energy and/or
  premium rune domains); the server rejects underpaid equips.
- In-play Rune cards render as card-backed resource plaques when card data/art is
  available. Hidden Rune Deck contents remain private.
- Narrow chain/Reaction support: Stacked Deck and simple public `Draw 1` spells
  open the chain; Gust, Defy, Not So Fast, Discipline, En Garde, Defiant Dance,
  Flash, and Abandon are the supported chain-backed Reactions. Supported targeted
  Reactions can be played on your own turn and open a response chain before
  resolving. Human priority windows are bluff-safe; RiftBot auto-passes empty
  windows.

### Privacy and UX

- WebSocket and REST projections mask opponent hands, hidden/face-down cards,
  private choices, deck and rune-deck contents, and private logs.
- Stacked Deck / Predict-style private choices are owner-only.
- Playtester UI includes phase guidance, visible errors, support badges, Copy
  Debug Info (public-safe), Report Issue, Alpha limits, and a responsive board
  with wider Rune rows.

## Card Support Notes

- **Supported** cards are directly implemented and tested.
- **Partial** cards are playable for alpha testing but run conservative
  approximations and may be inaccurate.
- **Unsupported** cards are blocked in supported-cards-only enforced play.
- **Banned** cards and battlefields are rejected for constructed decks.
- **Not Audited** cards are not available in supported-cards-only mode yet.
- The uploaded Irelia bot deck contains many Partial cards by design.

## Known Limitations

- The uploaded Irelia deck is enforced-playable but **Partial-heavy and not
  rules-complete**; treat its behavior as approximate.
- Active-lane alpha is intentional. Battlefield effects, hidden Battlefield slots,
  official "here" targeting, non-Duel active-lane counts, and full official
  location rules remain deferred.
- Reaction, chain, counterspell, and priority timing are narrow and
  card/pattern-specific. Full official Reaction timing, hidden Reaction-for-0, and
  competing-replacement player choice are deferred.
- Replacement/prevention support exists only as an exact-card would-die hook
  (used by Zhonya's Hourglass); generic replacement/prevention is deferred.
- Hidden play-from-hidden timing, Ambush-as-Reaction, and additional-cost Ambush
  cards are incomplete.
- Quick-Draw, Weaponmaster, XP, Hunt, Level, Buff, production Gear stat modifiers,
  +Health attach/detach semantics, and most Battlefield abilities are deferred.
- Predict/top-deck ordering and broader private choice prompts are partial.
- Printed Equip rune payments are supported for audited Equip gear, but broader
  domain/power payment precision is not tournament-accurate.
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
- Human vs human or human vs RiftBot (and which bot deck).
- Supported-cards-only mode setting.
- Phase or showdown state.
- Card(s) involved.
- Expected behavior and actual behavior.
- Screenshot or video if possible.
- Copied debug info.

## Validation Checklist

- `npm run build`
- `npm run test:frontend`
- `cd server && mvn -q test`
- `cd server && mvn -q -DskipTests compile`
- `npm run import:uploaded-meta-decks` (no diff after)
- `npm run audit:meta-decks` (no diff after)
- `git diff --check`
- Desktop installer smoke test (human vs RiftBot full game on the uploaded Irelia
  deck: rune shuffle, draw/play/pay/pass, lane and back-to-Base movement,
  Stellacorn draw, Zhonya/The Syren activation, Charm/Flash/Defiant Dance, a
  showdown, and Copy Debug Info safety)
- Server `/api/health` check (confirm `serverFullGitSha` / `serverJarSha256` match
  the intended build)

At time of writing: 671 backend tests and 107 frontend tests passing.

## Fan Project Notice

RiftForge is an unofficial fan-made project. It is not affiliated with,
endorsed, sponsored, or approved by Riot Games, League of Legends, Riftbound,
or UVS Games. Official rules, card text, tournament documents, banlists, and
errata remain the source of truth.
