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
- **Targeted spell UI** — click a spell to enter targeting mode; eligible
  units highlight based on targeting type (friendly, enemy, or any); confirmed
  target is transmitted to the server
- Block assignment supports attacker-first and blocker-first selection with
  threat, selected, covered, and eligible-blocker highlights
- Card hover preview appears after a 2-second intent delay; moving away cancels
  it before it opens

### Turn and Phase Flow

- Turn start follows the official A-B-C-D sequence: **Awaken** (untap) →
  **Begin** (Hold scoring + Start-of-Turn effects) → **Channel** (2 runes) →
  **Draw** (1 card)
- **Then in any order**: play cards, move units to/from battlefield (triggering
  combat), activate abilities
- **End of turn**: End-of-Turn effects fire, all units return to full health
- Attacking units are exhausted (tapped) when declared; untapped at Awaken
- Rune pool: tap a rune for 1 energy; permanently discard for 2 premium energy
- Each player has a 10-rune reserve; exhausted rune decks skip grants
- Deck-out handled gracefully — draw is skipped with a log, game continues
- Summoning sickness: units wait one full cycle before attacking (unless RUSH)
- Starting player is randomized; first player receives 1 rune, second gets 2

### Combat and Card Effects

- Declare attackers by clicking units; assign blockers in BLOCK_DECLARE
- Full damage exchange with health tracking and end-of-combat healing
- Keywords: **RUSH**, **TOUGH**, **OVERWHELM**, **ELUSIVE**, **LIFESTEAL**
- Temporary keyword grants (e.g. TOUGH from a card effect) respected in combat
  and cleared at end of turn
- Gear attachment: equip cards target a friendly unit, stay attached, and
  discard automatically when their host leaves play
- Spell effects: temporary might bonuses, return-to-hand, ready-unit, draw-one
- Temporary might bonuses apply in combat and clear at end of turn
- Unsupported cards (counter, complex, multi-target) are rejected at validation
  before runes are spent; card previews label each effect as supported or not
- `onPlay`, `onDestroy`, `onAttack`, `onTurnStart`, and `onTurnEnd` lifecycle hooks wired
- Targeted-spell validation: friendly, enemy, or any-unit targeting enforced
- Bots avoid unsupported cards and choose appropriate targets automatically
- **Scoring**: +1 when an attacker defeats a blocker and survives; Hold scoring
  (+1 per uncontested battlefield) fires at the start of your turn, not on
  attack declaration; 8 points wins
- **Champion resilience**: destroyed champions return to their Champion zone
  with summoning sickness rather than going to discard
- **Mulligan**: opening-hand mulligan phase before turn 1 — keep any cards,
  redraw the rest; bots keep all automatically

### Rules Engine

- Spring Boot server owns and validates all game state
- Turn order, card ownership, zones, energy costs, deployment, and combat checks
- Finished games evicted from memory hourly; duplicate `initGame` calls ignored
- STOMP reconnect: client re-fetches state and shows a banner on connection loss

### AI

- **RiftBot** and **Codex**: two independent bots — channel runes, deploy
  champion and base units, play affordable hand cards, attack, and block
- Bot vs Bot games run at 350 ms/action; human vs bot games at 700 ms/action

### Match History

- `/history` page listing completed PvP matches with winner, scores, and date
- Bot-only games are excluded from history
- Server caps history at 100 matches in memory

### Desktop App

- Tauri 2 desktop wrapper — native window, no browser required
- Spring Boot server bundled as a fat JAR alongside a stripped jlink JRE
  (~1-2 s startup; no JDK installation required on the end-user machine)
- Sidecar lifecycle: Tauri spawns the server on launch, kills it on close
- Windows NSIS installer builds to ~63 MB; macOS and Linux packaging also supported
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
| Desktop server | Spring Boot fat JAR + jlink JRE |
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

See [BUILDING.md](BUILDING.md). Prerequisites: Java 21, Maven, Rust, and (on
Windows) Visual Studio C++ Build Tools. GraalVM is not required.

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

- **Card and combat animations** — card fly-in, zone transitions, hit/destroy
  effects; the biggest gap between "playable prototype" and "feels like a game"
- **Sound effects** — wire the existing `sfx.ts` stub to actual audio files
- **Match replay** — persist the full move log alongside match history and allow
  step-through replay from the `/history` page

### Platform

- Supabase Auth and account-backed deck persistence
- Invitations, private rooms, and player profiles
- Hosted multiplayer server so players can connect without a LAN or shared code
- Capacitor mobile packaging (iOS / Android)

### Rules and Cards

- Broader card-effect coverage (counter spells, multi-target, on-destroy triggers)
- Complete Riftbound keyword set beyond the current five
- Server-side deck legality enforcement and match formats

### Polish

- Better card inspection: zoom, full rules text, keyword glossary
- Mobile and touch ergonomics
- Production deployment, observability, and abuse protection

## Known Limitations

- Live games are stored in server memory and are lost when the server restarts
- Counter spells, complex multi-target effects, and cards with no implemented
  effect are rejected; full card-effect coverage is still in progress
- The desktop installer must be built from source; pre-built binaries are not
  yet distributed (see [BUILDING.md](BUILDING.md))
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
