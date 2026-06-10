# RiftForge

RiftForge is an unofficial, cross-platform digital client for **Riftbound**, the
League of Legends trading card game. The goal is to make it easy to browse
cards, build and share decks, and play complete matches — either in a browser or
as a native desktop app that ships as a single installer with no runtime
dependencies.

> RiftForge is a fan-made project and is not affiliated with or endorsed by
> Riot Games. Riftbound and League of Legends are trademarks of Riot Games.

## Playing Right Now

**Download:** grab `RiftForgeInstaller.exe` from the root of this repo and run it.
The app bundles its own game server — no Java or other installs required.

**Playing solo or vs bot:** just open the app and go. The bundled server starts
automatically (first launch takes ~30 s while card data loads; faster after that).

**Playing with friends:**
The game server runs at `https://riftforgetcg-production.up.railway.app` — no setup required.
Install the app and you're connected automatically. Share a room code to invite friends.

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
- Choose a deck and ready up before starting; server validates deck legality on ready and start
- Play against another human player or **RiftBot** (AI opponent)
- Host can assign a saved deck to RiftBot or use an auto-generated deck
- Real-time game state, game log, and spectator view
- **Watch AI vs AI** — launch a RiftBot vs Codex game and spectate the full match

### Digital Tabletop

- Konva-powered interactive board with hand rack, Base, Battlefield, Champion,
  Legend, and rune strip zones
- Drag cards between zones with clamped bounds; tap/untap, flip, hover previews
- **Player panel** — shows deck count, rune pool remaining, and inline scrollable
  trash list; local player panel includes card size slider and leave button
- **Revealed hand** — Thoughtseize-style effects snapshot the opponent's hand;
  the panel shows each revealed card with a per-card dismiss button
- Human-readable phase labels; connection-lost banner with auto-reconnect
- **Targeted spell UI** — click a spell to enter targeting mode; eligible
  units highlight based on targeting type (friendly, enemy, or any); confirmed
  target is transmitted to the server
- Card hover preview appears after a 2-second intent delay; moving away cancels
  it before it opens

### Turn and Phase Flow

- Turn start follows the official A-B-C-D sequence: **Awaken** (untap) →
  **Beginning** (Hold scoring + Start-of-Turn effects) → **Channel** (2 runes) →
  **Draw** (1 card)
- **Main Phase**: play cards, move units to the battlefield (may trigger a
  Showdown — see below), activate abilities, in any order
- **End Phase**: End-of-Turn effects fire, all board units return to full health
- Attacking units are tapped when declared; untapped at Awaken
- Rune pool: tap a rune for 1 energy; permanently discard for 2 premium energy
- Each player has a 10-rune reserve; exhausted rune decks skip grants
- Deck-out handled gracefully — draw is skipped with a log, game continues
- Summoning sickness: units wait one full cycle before attacking (unless RUSH)
- Starting player is randomized; first player skips their first draw

### Showdowns

When a unit moves to the battlefield during the Main Phase and there are
opposing units already there, a **Showdown** starts:

- The game enters a suspended Main Phase substate — `activeShowdown` is set on
  the game state; the phase remains **MAIN** throughout
- No other moves are legal until the attacking player resolves the Showdown
- On resolution, combat is evaluated: if attackers remain and all defenders are
  eliminated, the attacker conquers the battlefield; otherwise attackers retreat
  to base
- After resolution `activeShowdown` is cleared and play returns to the normal
  Main Phase — multiple Showdowns can happen in a single Main Phase turn

### Combat and Card Effects

- Keywords: **RUSH**, **TOUGH**, **OVERWHELM**, **ELUSIVE**, **LIFESTEAL**,
  **ACCELERATE**, **AMBUSH**, **HIDDEN**, **VISION**, **LEGION**, **GANKING**,
  **DEFLECT**, **REPEAT**, **TEMPORARY**, **WEAPONMASTER**, **QUICK-DRAW**
- Temporary keyword grants respected in combat and cleared at end of turn
- Gear attachment: equip cards target a friendly unit, stay attached, and
  discard automatically when their host leaves play
- Spell effects: temporary might bonuses, return-to-hand, ready-unit, draw-one,
  reveal-opponent-hand
- Temporary might bonuses apply in combat and clear at end of turn
- Unsupported cards (counter, complex, multi-target) are rejected at validation
  before runes are spent; card previews label each effect as supported or not
- `onPlay`, `onDestroy`, `onAttack`, `onTurnStart`, and `onTurnEnd` lifecycle hooks wired
- Targeted-spell validation: friendly, enemy, or any-unit targeting enforced
- Bots avoid unsupported cards and choose appropriate targets automatically
- **Scoring**: conquer the battlefield to score +1; hold an uncontested
  battlefield at the start of your turn for +1; 8 points wins
- **Champion resilience**: destroyed champions return to their Champion zone
  with summoning sickness rather than going to discard
- **Mulligan**: opening-hand mulligan phase before turn 1 — keep any cards,
  swap up to 2; bots keep all automatically

### Rules Engine

- Spring Boot server owns and validates all game state; clients submit proposed
  moves and receive the server-applied result
- **ENFORCED / SANDBOX game modes** — ENFORCED is the default for all human and
  bot games; SANDBOX gates developer-only moves (`DEAL_CARD`, `ADJUST_SCORE`)
- **Per-room move serialization** — a `ReentrantLock` per room code ensures
  moves for the same game are validated and applied sequentially; different rooms
  run in parallel
- **Completed-match snapshotting** — match history captures a value snapshot
  under the room lock; the live mutable state is never passed to history after unlock
- **Deck validation** on ready and start enforces two profiles:
  - `FULL_CONSTRUCTED` (human players): 1 Legend, 1 Champion, exactly 39
    non-Champion main-deck cards, exactly 12 runes, exactly 3 unique battlefields,
    ≤3 copies per card — violations return 400 with a specific message
  - `PLAYTEST_BOT` (generated bot decks): Legend required, ≥20 main-deck cards,
    Champion/runes/battlefields optional — intentionally loose for dev/test piles
- Finished games evicted from memory hourly; duplicate `initGame` calls ignored
- STOMP reconnect: client re-fetches state and shows a banner on connection loss

### AI

- **RiftBot** and **Codex**: two independent bots — channel runes, deploy
  champion and base units, play affordable hand cards, attack, and resolve showdowns
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
- Dynamic port selection: scans 8080–8099 for an available port so multiple
  instances and stale processes don't collide
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
In the desktop app the server runs as a bundled sidecar on a dynamically
selected localhost port (default 8080).

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
mvn -q test
```

## Roadmap

### P0 — Before Serious Hosted PvP

- **Player-specific game-state projections** — the server currently broadcasts
  full game state on a shared room topic; opponent hand card identities need to be
  masked, private Vision top-card choices need to be player-private, and the
  public game log must not contain private card IDs
- **Room-scoped session tokens** — STOMP identity currently uses a
  client-supplied player ID; the server needs to issue a per-room join token,
  verify it on connect, and tie it to a specific room + player + role

### P1 — Playable Demo Quality

- **Starter decks** — ship two sample valid constructed decks so new players
  can start a game without building one first
- **Supported-card matrix** — document which cards are fully, partially, or not
  yet supported so players know what to expect
- **Client error display** — WebSocket validation errors currently surface only
  in the local game log; they need a visible in-game notification
- **Clean game log** — separate public play-by-play from private prompts;
  replace protocol strings like `VISION_PEEK|cardId|name` with human-readable entries
- **Reconnect hardening** — fetch a player-specific state projection on
  reconnect and safely restore any pending private choices
- **Board layout polish** — swap Champion and Legend zone positions so Champion
  is closer to the Battlefield/play area (Champion is the card you deploy;
  Legend is the identity anchor); keep both zones clear of the player panel
- **Deck format and setup correctness** — explicitly separate Legend and Champion
  as distinct deck roles; validate constructed format as 1 Legend + 1 Champion +
  39-card main remainder + 12-rune deck + 3 battlefields (Champion counts toward
  the 40-card main deck, not separately); ensure `deckCount` reflects only the
  remaining drawable main deck after setup cards are removed; replace random bot
  piles with valid sample decks so reviewers see legal constructed counts

### P2 — Rules Depth

- Broader card-effect coverage (counter spells, multi-target, complex
  on-destroy triggers)
- Complete Riftbound keyword set
- More complete reaction / priority model
- Multiple named battlefield support beyond the current single controller key
- Bot reaction logic on the opponent's turn

### P3 — Production / Portfolio Polish

- **Card and combat animations** — card fly-in, zone transitions, hit/destroy effects
- **Sound effects** — wire the existing `sfx.ts` stub to actual audio files
- **Match replay** — persist the full move log and allow step-through replay
  from the `/history` page
- README screenshots and architecture diagram
- CI workflow for `npm run build` + `mvn test`
- Move installer binary to GitHub Releases
- Tighten Tauri CSP
- Fix NSIS installer to kill only the RiftForge sidecar process, not all `java.exe` instances
- Supabase Auth and account-backed deck persistence
- Capacitor mobile packaging (iOS / Android)

## Known Limitations

- Live games are stored in server memory and are lost when the server restarts
- Game state is currently broadcast on a shared room topic — player-specific
  projection to prevent hidden-information leaks (opponent hand cards, private
  Vision choices) is the next major hardening task
- STOMP identity uses a client-provided player ID; room-scoped session tokens
  for verified identity are planned
- `TapCardMove`, `FlipCardMove`, and `MoveCardMove` are sandbox-only in ENFORCED
  games; they have no legal rules use case yet and are gated rather than
  phase/zone validated
- `FULL_CONSTRUCTED` deck validation requires exactly 39 non-Champion main cards +
  1 Champion; `PLAYTEST_BOT` validation is intentionally loose and does not require
  Champion, runes, or battlefields — this divergence is documented and tested
- Counter spells, complex multi-target effects, and cards with no implemented
  effect are rejected; full card-effect coverage is still in progress
- A pre-built Windows installer (`RiftForgeInstaller.exe`) is at the repository
  root; macOS and Linux require building from source (see [BUILDING.md](BUILDING.md))
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
