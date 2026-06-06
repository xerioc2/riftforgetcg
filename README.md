# RiftForge

RiftForge is an unofficial, cross-platform digital client for **Riftbound**, the
League of Legends trading card game. The goal is to make it easy to browse
cards, build and share decks, and play complete matches online with an
authoritative rules engine.

The project is a **functional multiplayer prototype**. Deck building, lobbies,
real-time two-player games, a full phase machine, combat declaration flows,
rune pooling, Champion and Legend zones, a rules-validating game server, and
two AI bots are all working end-to-end.

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
- Host can assign a saved deck to RiftBot or let it use an auto-generated deck
- Real-time game state, game log, and spectator view
- **Watch AI vs AI** — launch a RiftBot vs Codex game and spectate the full match

### Digital Tabletop

- Konva-powered interactive board with hand rack, Base, Battlefield, Champion,
  Legend, rune strip, deck, and discard zones
- Drag cards between zones with clamped bounds; tap/untap, flip, hover previews
- Adjustable hand height (drag the divider to resize)
- Draggable card preview panel
- Phase bar showing current phase, active player, and pass controls

### Turn and Phase Flow

- Full six-phase machine: **CHANNEL → MAIN → ATTACK_DECLARE → BLOCK_DECLARE →
  COMBAT_RESOLVE → END**
- CHANNEL and COMBAT_RESOLVE auto-advance (no manual pass required)
- Active player draws a card and gains two runes at the start of each turn
- Rune pool: tap runes to queue energy, commit the pool before spending — undo
  by clicking a pending rune before playing a card
- Summoning sickness: units played or deployed from Base must wait one full cycle
  before attacking (unless they have RUSH)

### Combat

- Click units to declare attackers (ATTACK_DECLARE phase)
- Assign blockers by clicking an attacker then a blocker (BLOCK_DECLARE phase)
- Full combat resolution: damage exchange, health tracking, TOUGH damage
  reduction, OVERWHELM excess-damage scoring, destroy and discard
- Attackers and surviving blockers return to Base after combat
- Unblocked attackers score 1 point; first to reach the target score wins

### Rules Engine

- Spring Boot server owns and validates all game state
- Turn order, card ownership, zones, energy costs, deployment, and combat checks
- Keywords: **RUSH**, **TOUGH**, **OVERWHELM**, **ELUSIVE** (cannot be blocked),
  **LIFESTEAL** (score on lethal blocker kill)
- Temp-keyword grants (e.g. TOUGH granted by a card effect) respected in combat
  and cleared at end of turn
- Spell and gear cards auto-discard to the discard pile after their effect fires
- Targeted-spell validation: checks that a valid enemy unit is on the battlefield
  before allowing the play
- Card effect lifecycle hooks: `onPlay`, `onDestroy`, `onAttack`, `onTurnStart`

### AI

- **RiftBot**: channels runes, deploys champion and base units, plays hand cards,
  attacks with all eligible units, and assigns blocks
- **Codex**: second AI with the same strategy — can be paired against RiftBot
  for a full spectated test game
- Bot vs Bot games run at 350 ms per action; human vs bot games at 700 ms

## Architecture

| Area | Technology |
| --- | --- |
| Web client | React 18, TypeScript, Vite |
| Styling | Tailwind CSS |
| Tabletop | Konva.js + react-konva |
| Client state | Zustand |
| Game server | Spring Boot 3, Java 21 |
| Real-time transport | STOMP over WebSocket (`@stomp/stompjs`) |
| Card data | Riftcodex API |
| Persistence groundwork | Supabase schema and client dependency |
| Future wrappers | Capacitor (mobile), Tauri (desktop) |

The Spring Boot server is authoritative during games. Clients send proposed
moves; the server validates, applies, and broadcasts the resulting state via
STOMP topics. A `ConcurrentHashMap` of `LiveGameState` holds all active games
in memory.

## Run Locally

### Prerequisites

- Node.js 20 or newer
- Java 21
- Maven

### Frontend

```bash
npm install
npm run dev
```

The Vite client runs at `http://localhost:5173`.

### Game Server

In a second terminal:

```bash
cd server
mvn spring-boot:run
```

The Spring Boot server runs at `http://localhost:8080`.

### Environment

Copy `.env.example` to `.env.local` to override local defaults:

```bash
VITE_RIFTCODEX_API_BASE=https://riftcodex.com
VITE_GAME_SERVER_URL=http://localhost:8080
```

Deck and card data remain in browser local storage. The included Supabase
schema is groundwork for future account-backed persistence.

## Validation

```bash
npm run lint
npm run build

cd server
mvn -q -DskipTests compile
```

## Roadmap

### In Progress

- Full card-effect coverage: remaining Origins card scripting, targeted spell
  resolution with explicit target selection
- STOMP reconnect and mid-game rejoin by room code

### Near Term

- Tauri desktop wrapper (.exe / .dmg) — no exposed IP, no browser required
- Match history and replay: store full move log, add a `/history` page
- Automated tests for turn flow, combat, deck legality, and move validation
- Animations for card play, combat, and zone transitions

### Rules Engine

- Formal target-selection flow (choose a target before playing a spell)
- Gear attachment mechanic
- Complete Riftbound keyword set
- Enforce legal deck construction and match formats server-side
- Persist active games and recover them after server restarts

### Platform

- Supabase Auth and account-backed deck persistence
- Invitations, private rooms, and player profiles
- Match history, improved spectator tools, and replays
- Capacitor mobile packaging

### Polish

- Sound effects and settings panel
- Better card inspection and rules explanations
- Production deployment, observability, and abuse protection

## Known Limitations

- Live games are stored in server memory and lost when the server restarts
- Spell targeting uses a presence check (any enemy unit exists) rather than
  explicit target selection — actual targeted effects are not yet resolved
  against a specific card
- Gear cards play to Base like units; an attach-to-unit mechanic is not yet
  implemented
- Mobile and desktop wrappers have not been packaged yet
- Multiplayer assumes the local development server unless the server URL is
  overridden in the environment

## Project Structure

```text
src/        React client, deck builder, lobby, and tabletop
server/     Spring Boot game server and rules engine
supabase/   Optional database schema
public/     Static client assets
```

## Data and Artwork

RiftForge uses the Riftcodex API for card metadata during development. Card art
and game assets must not be hotlinked or redistributed in production without the
appropriate permissions and licenses from Riot Games.
