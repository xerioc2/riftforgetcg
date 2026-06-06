# RiftForge

RiftForge is an unofficial, cross-platform digital client for **Riftbound**, the
League of Legends trading card game. The goal is to make it easy to browse
cards, build and share decks, and play complete matches online with an
authoritative rules engine.

The project is currently an **early playable prototype**. Deck building,
lobbies, real-time games, a digital tabletop, basic combat, turn phases, runes,
and a simple bot are working. Full rules enforcement and card-effect coverage
are still in development.

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
- Choose a deck and ready up before starting
- Play against another player or RiftBot
- Real-time game state, game log, and spectator view
- Client-side chat interface ready for server-backed messaging

### Digital Tabletop

- Konva-powered interactive board with hand, Base, Battlefield, rune, and
  discard zones
- Drag and drop cards, tap/untap, flip, hover previews, and adjustable card size
- Channel, Main, Attack, Block, Combat Resolve, and End phases
- Rune channeling, energy spending, Base deployment, combat, scoring, and win
  condition

### Rules Engine

- Spring Boot server owns and validates live game state
- Turn order, card ownership, zones, energy costs, deployment, and combat checks
- Keyword foundations for Rush, Overwhelm, and Tough
- Basic targeted-spell validation
- Simple RiftBot that channels, deploys, plays cards, attacks, and blocks

## Architecture

| Area | Technology |
| --- | --- |
| Web client | React, TypeScript, Vite |
| Styling | Tailwind CSS |
| Tabletop | Konva.js |
| Client state | Zustand |
| Game server | Spring Boot, Java 21 |
| Real-time transport | STOMP over WebSocket |
| Card data | Riftcodex API |
| Persistence groundwork | Supabase schema and client dependency |
| Future wrappers | Capacitor and Tauri |

The Spring Boot server is authoritative during games. Clients send proposed
moves, and the server validates, applies, and broadcasts the resulting state.

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

Copy `.env.example` to `.env.local` when you need to override local defaults:

```bash
VITE_RIFTCODEX_API_BASE=https://riftcodex.com
VITE_GAME_SERVER_URL=http://localhost:8080
```

Deck and card data currently remain in browser local storage. The included
Supabase schema is groundwork for account-backed persistence.

## Validation

```bash
npm run lint
npm run build

cd server
mvn -q -DskipTests compile
```

## Roadmap

### Near Term

- Expand automated tests for turn flow, combat, deck legality, and move validation
- Improve Battlefield targeting and combat declaration interactions
- Add clearer move errors and reconnect/resume behavior
- Refine board layout, animations, mobile ergonomics, and accessibility
- Expand RiftBot decision-making

### Rules Engine

- Implement the complete Riftbound turn and combat rules
- Add a formal target-selection flow and effect resolution system
- Expand keyword support and Origins card scripting
- Enforce legal deck construction and match formats server-side
- Persist active games and recover them after server restarts

### Platform and Online Play

- Supabase Auth and account-backed deck persistence
- Matchmaking, invitations, private rooms, and player profiles
- Match history, replays, and improved spectator tools
- Package mobile clients with Capacitor
- Package desktop clients with Tauri

### Polish

- Card and combat animations
- Sound effects and settings
- Better card inspection and rules explanations
- Production deployment, observability, moderation, and abuse protection

## Known Limitations

- Many individual card effects are not implemented yet
- Targeting uses temporary heuristics rather than a complete targeting system
- Live games are stored in server memory and are lost when the server restarts
- Multiplayer currently assumes the local development server setup
- Mobile and desktop wrappers have not been packaged yet

## Project Structure

```text
src/        React client, deck builder, lobby, and tabletop
server/     Spring Boot game server and rules engine
supabase/   Optional database schema
public/     Static client assets
```

## Data and Artwork

RiftForge currently uses Riftcodex for card metadata during development. Card
art and game assets must not be hotlinked or redistributed in production
without the appropriate permissions and licenses.
