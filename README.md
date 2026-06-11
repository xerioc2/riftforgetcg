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

RiftForge currently has a server-authoritative playtest foundation:

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
- Online player presence count
- Per-room move locking so same-room moves serialize safely
- Server-computed legal actions included in player-specific projected state
- Client uses `legalActions` to enable or hide major game actions such as
  mulligan, pass phase, play card, move to battlefield, rune actions, and
  showdown resolution
- `RulesValidator` remains the final server enforcement layer for every move
- Atomic rune payment for `PLAY_CARD` moves, including selected normal energy
  runes and premium/domain rune recycling
- Type-aware play validation for common enforced games: units, spells, gear, and
  runes use explicit play destinations, while Legend, Champion, and Battlefield
  setup cards are blocked from hand play
- Movement legality for the current single-battlefield model: ready units and
  Champions can move to battle, contested movement starts a showdown, and
  same-zone visual repositioning uses a dedicated server move
- Constructed deck setup with 1 Legend, 1 Champion, 39 non-Champion main cards,
  12 runes, and 3 unique battlefields
- Opening hand draws only from the main deck, not runes, battlefields, Legend,
  or Champion
- Showdown modeled as `activeShowdown` inside Main Phase
- Showdown status includes a server step, and deterministic combat resolves
  simultaneous damage, Tank priority, Assault, Shield, and Stun for supported
  paths
- Hold and Conquer scoring are server-owned, with target-score winner checks,
  Conquer final-point restrictions, and completed-match snapshots for public
  history
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
- In-game debug-info copy button for playtest bug reports

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
  Champion text, gear attachment details, and many card-specific effects remain
  partial.
- Movement legality is still modeled for one battlefield; multiple battlefield
  selection, assignment, and advanced movement effects remain future work.
- Combat uses a deterministic resolver; interactive damage assignment,
  prevention/replacement, and full timing windows are still future work.
- Scoring supports the current battlefield-controller model, but full official
  multiplayer/tie/burnout cleanup remains future work.
- The keyword/effect handler registry is only a scaffold. Tank and Vision have
  initial handler coverage, but many keywords and card-specific effects still
  need explicit scripts and tests.
- Card support labels are intentionally conservative. Partial cards can be used
  for alpha testing, while unsupported/not-audited cards can be blocked by the
  supported-cards-only lobby option.
- `LegalActionsService` is conservative and does not yet model every timing or
  reaction window.
- Reaction/action windows are future work.
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
- Starter deck cards fully supported
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
