# RiftForge

RiftForge is an unofficial, cross-platform digital client for **Riftbound**, the
League of Legends trading card game. The goal is to make it easy to browse
cards, build and share decks, and play complete matches — either in a browser or
as a native desktop app that ships as a single installer with no runtime
dependencies.

> RiftForge is a fan-made project and is not affiliated with or endorsed by
> Riot Games. Riftbound and League of Legends are trademarks of Riot Games.

## Current State

### Card Database and Deck Builder

- Fetches and caches card data through the Riftcodex API
- Search and filtering by champion, domain, type, and cost
- Create, edit, save, export, delete, and import text decklists
- Legend selection, domain checks, copy limits, and deck legality feedback
- Browser-local deck and card caching

### Lobby and Multiplayer

- Create shareable game rooms and join by room code
- **Find Match** queue that pairs two waiting human players directly into a game
- Choose a deck and ready up before starting
- Play against another human player or **RiftBot** (AI opponent)
- Host can assign a saved deck to RiftBot or use an auto-generated deck
- Real-time game state, game log, and spectator view
- **Watch AI vs AI** — launch a RiftBot vs Codex game and spectate the full match

### Digital Tabletop

- Konva-powered interactive board with hand rack, Base, Battlefield, Champion,
  Legend, rune strip, deck, and discard zones
- Drag cards between zones with clamped bounds; tap/untap, flip, hover previews
- Adjustable hand height; draggable card preview panel
- Human-readable phase labels; connection-lost banner with auto-reconnect

### Turn and Phase Flow

- Six-phase machine: **MAIN → ATTACK_DECLARE → BLOCK_DECLARE →
  COMBAT_RESOLVE → END → CHANNEL** (CHANNEL and COMBAT_RESOLVE are invisible —
  they auto-advance in the same server tick)
- Active player draws a card and gains two runes at the start of each turn
- Rune pool: tap runes to queue energy; un-tap pending runes before committing
- Summoning sickness: units wait one full cycle before attacking (unless RUSH)

### Combat and Card Effects

- Declare attackers by clicking units; assign blockers in BLOCK_DECLARE
- Full damage exchange with health tracking and end-of-combat healing
- Keywords: **RUSH**, **TOUGH**, **OVERWHELM**, **ELUSIVE**, **LIFESTEAL**
- Temporary keyword grants (e.g. TOUGH from a card effect) respected in combat
  and cleared at end of turn
- Spell and gear cards auto-discard after their `onPlay` effect fires
- `onPlay`, `onDestroy`, `onAttack`, and `onTurnStart` lifecycle hooks wired
- Targeted-spell validation: requires at least one enemy unit on the battlefield

### Rules Engine

- Spring Boot server owns and validates all game state
- Turn order, card ownership, zones, energy costs, deployment, and combat checks
- Finished games evicted from memory hourly; duplicate `initGame` calls ignored
- STOMP reconnect: client re-fetches state and shows a banner on connection loss

### AI

- **RiftBot** and **Codex**: two independent bots — channel runes, deploy
  champion and base units, play affordable hand cards, attack, and block
- Bot vs Bot games run at 350 ms/action; human vs bot games at 700 ms/action

### Desktop App (scaffold complete)

- Tauri 2 desktop wrapper — native window, no browser required
- Spring Boot server compiled to a GraalVM native binary (~50 ms startup,
  no JRE bundled)
- Sidecar lifecycle: Tauri spawns the server on launch, kills it on close
- Single build script produces a platform installer (.exe / .dmg / .AppImage)
- See [BUILDING.md](BUILDING.md) for build prerequisites and instructions

## Architecture

| Area | Technology |
| --- | --- |
| Web client | React 18, TypeScript, Vite |
| Styling | Tailwind CSS |
| Tabletop | Konva.js + react-konva |
| Client state | Zustand |
| Game server | Spring Boot 3.3, Java 21 |
| Real-time transport | STOMP over WebSocket (`@stomp/stompjs`) |
| Card data | Riftcodex API |
| Desktop shell | Tauri 2 (Rust) |
| Native server | GraalVM native-image |
| Persistence groundwork | Supabase schema and client dependency |
| Future mobile | Capacitor |

The Spring Boot server is authoritative during games. Clients send proposed
moves; the server validates, applies, and broadcasts the resulting state.
In the desktop app the server runs as a bundled sidecar on `localhost:8080`.

## Run Locally (browser dev)

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

See [BUILDING.md](BUILDING.md). Prerequisites: GraalVM 21+, Rust, and (on
Windows) Visual Studio C++ Build Tools.

```powershell
# Windows
./scripts/build-desktop.ps1

# macOS / Linux
./scripts/build-desktop.sh
```

Output lands in `src-tauri/target/release/bundle/`.

## Validation

```bash
npm run lint
npm run build

cd server
mvn -q -DskipTests compile
```

## Roadmap

### Highest Impact Next Steps

- **Targeted spell UI** — players pick which enemy unit a spell targets before
  playing it; currently the server validates a target exists but the client
  doesn't let you choose one
- **Card and combat animations** — card fly-in, zone transitions, hit/destroy
  effects; the biggest gap between "playable prototype" and "feels like a game"
- **Sound effects** — wire the existing `sfx.ts` stub to actual audio files
- **Match history and replay** — persist the full move log, add a `/history`
  page with replayable games

### Platform

- Supabase Auth and account-backed deck persistence
- Invitations, private rooms, and player profiles
- Hosted multiplayer server so players can connect without a LAN or shared code
- Capacitor mobile packaging (iOS / Android)

### Rules and Cards

- Gear attachment mechanic (currently gears play to Base like units)
- Complete Riftbound keyword set beyond the current five
- Full card-specific effect coverage beyond the Origins placeholder set
- Server-side deck legality enforcement and match formats

### Polish

- Better card inspection: zoom, full rules text, keyword glossary
- Mobile and touch ergonomics
- Production deployment, observability, and abuse protection

## Known Limitations

- Live games are stored in server memory and are lost when the server restarts
- Spell targeting checks presence only — the client cannot choose a specific
  target unit; area/self effects work, but targeted removal does not
- Gear cards play to Base like units; an attach-to-unit mechanic is not yet
  implemented
- The desktop installer requires GraalVM, Rust, and platform build tools to
  compile; pre-built binaries are not yet distributed
- Chat messages are local only and are not synced to opponents

## Project Structure

```text
src/            React client, deck builder, lobby, and tabletop
src-tauri/      Tauri 2 desktop shell (Rust)
server/         Spring Boot game server and rules engine
scripts/        Desktop build scripts
supabase/       Optional database schema
public/         Static client assets
BUILDING.md     Desktop build instructions
```

## Data and Artwork

RiftForge uses the Riftcodex API for card metadata during development. Card art
and game assets must not be hotlinked or redistributed in production without the
appropriate permissions and licenses from Riot Games.
