# RiftForge

RiftForge is an unofficial, fan-made playtest client for **Riftbound**, the
League of Legends trading card game. It is built to help the community test
games, decks, and rules interactions while official digital support is
unavailable.

RiftForge is **not** affiliated with, endorsed, sponsored, or approved by Riot
Games, League of Legends, Riftbound, or UVS Games. Official Riot/UVS rules
documents, card text, tournament documents, banlists, and errata are always the
source of truth. RiftForge is an alpha community tool, not an official
tournament authority.

> RiftForge was created under Riot Games' "Legal Jibber Jabber" policy using
> assets owned by Riot Games. Riot Games does not endorse or sponsor this
> project.

## Fan Project Disclaimer

RiftForge is an unofficial fan-made project. It makes no claim of ownership over
Riot Games, League of Legends, Riftbound, UVS Games, or any related names,
characters, card images, rules text, trademarks, logos, or other intellectual
property.

This is a free, noncommercial community project for playtesting and development
feedback. It does not use Riot logos or trademarks to imply official status. If
Riot Games, UVS Games, or another rights holder requests changes, removal of
assets, or takedown, the project will comply.

See [docs/FAN_PROJECT_NOTICE.md](docs/FAN_PROJECT_NOTICE.md) for the longer
notice.

## Current Status

RiftForge currently has a server-authoritative alpha playtest foundation:

- React, TypeScript, and Vite frontend
- Tailwind UI styling
- Konva board rendering
- Spring Boot backend
- STOMP/WebSocket multiplayer
- Room lobby, room codes, matchmaking, spectator view, and bot games
- Server-authoritative game state and move validation
- Room-scoped STOMP session tokens
- REST token validation for protected room and game actions
- Player-specific hidden-information projection
- Hidden deck, rune deck, private choice, hand, and face-down card contents are
  masked from opponents and spectators
- Online player presence count
- Per-room move locking so same-room moves serialize safely
- Server-computed legal actions included in player-specific projected state
- Client uses `legalActions` to enable or hide major game actions such as
  Battlefield selection, mulligan, pass phase, play card, move to battlefield,
  rune actions, chain priority, combat assignment, and showdown resolution
- `RulesValidator` remains the final server enforcement layer for every move
- Atomic rune payment for `PLAY_CARD` moves, including selected normal energy
  runes and premium/domain rune recycling
- Type-aware play validation for common enforced games: units, spells, gear, and
  runes use explicit play destinations, while Legend, Champion, and Battlefield
  setup cards are blocked from hand play
- Pre-mulligan Battlefield selection with player-specific setup privacy and
  public selected Battlefield plaques after setup
- Current Duel/bot games use two active shared Battlefield lanes, `bf-0` and
  `bf-1`; `bf-2` remains a reserved model location for future/non-Duel formats
- Movement legality for the current active-lane model: ready Units and
  Champions can move to an active Battlefield lane or between active lanes,
  contested same-lane movement starts a showdown, and same-zone visual
  repositioning uses a dedicated server move
- Constructed deck setup with 1 Legend, 1 Chosen Champion, 40 MainDeck cards,
  12 runes, and 3 unique battlefields
- Opening hand draws only from the main deck, not runes, battlefields, Legend,
  or Champion
- Showdown modeled as `activeShowdown` inside Main Phase
- Showdown location tracking so focus, combat assignment, resolution, control,
  and scoring are scoped to the active lane
- Deterministic server-planned combat damage assignment for the current alpha
  UI and RiftBot flow; full manual damage splitting is still deferred
- Hold and Conquer scoring are server-owned, with target-score winner checks,
  Conquer final-point restrictions, and completed-match snapshots for public
  history
- In-play Rune cards render as card-backed resource plaques while hidden Rune
  Deck contents remain private
- Gear plays to Base first, Equip is a separate move from Base, and audited
  Gear pays printed Equip costs before attaching
- Attached Gear is non-combatant, non-draggable, follows its host visually, and
  returns to Base when the host leaves public play in supported paths
- Server-projected effective stats are the client display source for public
  Unit/Champion cards; production Gear stat modifiers are not enabled unless
  explicitly listed in support metadata
- Narrow Reaction/chain support using `ChainState`, focus/pass/LIFO resolution,
  local priority stops, and viewer-safe projection
- Stacked Deck and simple public `Draw 1` spells can open narrow response
  windows; Gust, Defy, Not So Fast, Discipline, and En Garde are the current
  supported chain-backed Reactions
- Rune innate Energy/Power actions remain ordinary resource actions and do not
  open or enter the chain
- Keyword/effect handler registry scaffold, with explicit unsupported-status
  reporting for tracked keywords and unsupported spell/gear shapes
- Card support metadata for Supported, Partial, Unsupported, Banned, and Not
  Audited states, with optional supported-cards-only ready gates
- Tournament-style deck import/export with Legend, Champion, MainDeck, Rune
  Pool, and Battlefields sections plus unresolved-card reporting
- Deck Builder validation report for legality, banned cards, unsupported cards,
  partial cards, and missing card data
- Rules coverage and supported-card tracking documents
- Starter decks for safer alpha playtesting with visible support-status warnings
- Player-facing error toasts for failed moves, invalid decks, session problems,
  reconnects, and unsupported card effects
- In-game debug-info copy button for playtest bug reports, including public
  lane, chain, combat assignment, effective stat, Rune, and attachment state
  without hidden hand/deck/rune deck identities

This does **not** mean RiftForge has complete rules or complete card support.
Many card effects, timing windows, combat details, and tournament procedures are
partial or not implemented yet.

## Rules Support

Rules/card implementation is tracked here:

- [docs/RULES_COVERAGE.md](docs/RULES_COVERAGE.md)
- [docs/SUPPORTED_CARDS.md](docs/SUPPORTED_CARDS.md)
- [docs/RULES_ROADMAP.md](docs/RULES_ROADMAP.md)
- [docs/CARD_IMPLEMENTATION_CHECKLIST.md](docs/CARD_IMPLEMENTATION_CHECKLIST.md)

Status labels:

- **Supported**: implemented and tested in this codebase.
- **Partial**: playable for common cases, but missing known interactions or edge
  cases.
- **Unsupported**: not implemented or blocked in enforced play.
- **Banned**: not legal in the current constructed format tracked by RiftForge.
- **Not Audited**: card support has not been reviewed yet.

RiftForge may allow testing of supported or partial features, but official
tournament rulings and official documents always take precedence.

## Known Limitations

- Not all Riftbound rules are implemented.
- Not all cards are implemented.
- Some card effects are heuristic or partial.
- Some timing windows and combat details are incomplete.
- Rune payment is implemented for selected normal energy runes and basic
  premium/domain recycling, but full official payment timing is still evolving.
- Unit/spell/gear legality is enforced for common play and showdown entry, but
  Champion text, many Equipment effects, and many card-specific effects remain
  partial.
- Current 1v1 Duel/bot games use two active shared Battlefield lanes, but
  printed Battlefield effects, hidden Battlefield slots, official "here"
  targeting, richer location rules, and non-Duel active-lane counts remain
  future work.
- Constructed decks still include three unique Battlefield cards; that deck
  requirement does not mean Duel games display or use three active lanes.
- Combat uses deterministic server-planned alpha assignments; player-authored
  manual damage splitting, prevention/replacement, and full official combat
  edge cases are still future work.
- Scoring supports the current per-lane battlefield-controller model, but full
  official multiplayer/tie/burnout cleanup remains future work.
- Equip cost/payment and attachment lifecycle are supported for audited Gear,
  but full Equipment effects, production Gear stat modifiers, Quick-Draw,
  Weaponmaster, +Health attach/detach semantics, replacement effects, and broad
  Equip timing remain deferred unless a card is explicitly listed otherwise.
- The keyword/effect handler registry is only a scaffold. Tank and Vision have
  initial handler coverage, but many keywords and card-specific effects still
  need explicit scripts and tests.
- Narrow Reaction windows exist for current alpha response patterns, but full
  official Reaction timing, hidden Reaction-for-0, broad 3+ invite behavior,
  broad spell/ability counters, countering counters, and broad priority policy
  remain deferred.
- Card support labels are intentionally conservative. Partial cards can be used
  for alpha testing, while unsupported/not-audited cards can be blocked by the
  supported-cards-only lobby option.
- `LegalActionsService` is conservative and does not yet model every timing or
  reaction window.
- Card effects and full rules coverage remain incomplete.
- UI/UX is alpha quality.
- Bugs and rules mismatches are expected.
- Starter decks are recommended for alpha testing because they avoid known banned
  cards and use curated constructed deck shapes.
- Real tournament decklists can be imported for playtesting, but unsupported or
  partial cards should be checked in the validation report before readying.
- Use [docs/SUPPORTED_CARDS.md](docs/SUPPORTED_CARDS.md) before assuming a deck
  is fully playable.
- Official rules, banlists, errata, and tournament policy may change and must be
  rechecked.
- The app should be treated as a community playtest tool, not a tournament
  simulator or rules authority.

## Downloads

Packaged builds are distributed through
[GitHub Releases](https://github.com/xerioc2/riftforgetcg/releases).

- The latest Windows installer is attached to the latest GitHub Release.
- Source code remains in this repository.
- Generated installers should not be committed to git.
- Local developers can build from source using [BUILDING.md](BUILDING.md).
- Release notes can be drafted from [docs/RELEASE_TEMPLATE.md](docs/RELEASE_TEMPLATE.md).

RiftForge installers and releases are unofficial fan-project builds. They are not
Riot, League of Legends, Riftbound, or UVS Games products.

## Roadmap

### v0.2-alpha: Supported Deck Playtesting

- LegalActionsService integrated into UI
- Online presence count
- Starter/sample supported decks
- Visible error messages
- Supported-card warnings before readying
- Optional supported-cards-only ready gate
- Tournament deck import/export and validation report

### v0.3-alpha: Core Rules Correctness

- Rune payment correctness (partial)
- Unit, spell, and gear play legality (partial)
- Movement legality (partial)
- Battlefield control
- Scoring and winning rules (partial)

### v0.4-beta: Combat and Card Support

- Showdown timing precision
- Combat damage assignment
- Keyword/effect registry expansion
- More starter deck cards audited and promoted only when their full printed text
  is implemented and directly tested
- Popular tournament cards implemented

### v0.5-beta: Public Playtest Polish

- Deck import/export polish
- Release builds through GitHub Releases
- CI
- Issue templates
- Reconnect hardening
- Better spectator and match review tools

### v1.0 Goal

Full current constructed rules and card support for normal playtest use, subject
to official rules changes.

## Contributing / Reporting Issues

Useful issue categories:

- Rules bug
- Card behavior bug
- UI bug
- Deck import or validation bug
- Multiplayer/session bug

GitHub issue templates are available for rules bugs, card behavior bugs, UI
bugs, and multiplayer/session bugs.

Please include:

- App version or commit hash
- Decklist
- Game phase
- Expected official rule or card-text behavior
- Actual RiftForge behavior
- Screenshot, browser console output, server log, or reproduction steps if
  available

## Architecture

| Area | Technology |
| --- | --- |
| Web client | React 18, TypeScript, Vite |
| Styling | Tailwind CSS |
| Tabletop | Konva.js + react-konva |
| Client state | Zustand |
| Game server | Spring Boot 3.3, Java 21 |
| Real-time transport | STOMP over WebSocket |
| Card data | Riftcodex API during development |
| Desktop shell | Tauri 2 |
| Desktop server | Spring Boot fat JAR + bundled Java runtime |
| Future mobile | Capacitor |

The Spring Boot server is authoritative during games. Clients send proposed
moves; the server validates, applies, and broadcasts projected state back to the
correct viewers.

## Run Locally

### Prerequisites

- Node.js 20+
- Java 21
- Maven

### Frontend

```bash
npm install
npm run dev
```

Vite runs at `http://localhost:5173`.

### Game Server

```bash
cd server
mvn spring-boot:run
```

Spring Boot runs at `http://localhost:8080`.

### Environment

Copy `.env.example` to `.env.local` to override defaults:

```env
VITE_GAME_SERVER_URL=http://localhost:8080
```

## Build the Desktop App

See [BUILDING.md](BUILDING.md). Prerequisites: Java 21, Maven, Rust, and on
Windows, Visual Studio C++ Build Tools.

```powershell
./scripts/build-desktop.ps1
```

```bash
./scripts/build-desktop.sh
```

Output lands under `src-tauri/target/release/bundle/`.

## Validation

```bash
npm run build

cd server
mvn -q test
mvn -q -DskipTests compile
```

## Data and Artwork

RiftForge uses the Riftcodex API for card metadata during development. Card art,
rules text, names, and game assets belong to their respective owners and must not
be redistributed or used in a way that implies official status or endorsement.
