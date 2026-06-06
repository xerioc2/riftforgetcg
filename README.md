# RiftForge

A cross-platform Riftbound digital client built as a side project.

## Stack

- React + TypeScript + Vite
- Tailwind CSS
- Zustand for local card/deck state
- Supabase Auth and deck persistence
- Riftcodex card data fetch and local browser cache
- Konva dependencies installed for the Phase 3 tabletop canvas

## Run Locally

```bash
npm install
npm run dev
```

Create `.env.local` from `.env.example`:

```bash
VITE_SUPABASE_URL=your-project-url
VITE_SUPABASE_ANON_KEY=your-anon-key
VITE_RIFTCODEX_API_BASE=https://riftcodex.com
```

Without Supabase values, the app still runs in local mode and stores decks in `localStorage`.

## Supabase Schema

Start with the tables in [supabase/schema.sql](./supabase/schema.sql). Enable whichever OAuth providers you want in Supabase Auth. The UI currently exposes email magic links and GitHub OAuth.

## Current Phase 1 Surface

- Card browser with search and filters for champion, domain, type, and cost
- Riftcodex fetch with local card cache
- Deck create/edit/delete in local storage
- Champion selection and basic domain/copy-count validation
- Auth-aware Supabase deck load/save path

## Current Phase 2 Surface

- Create a lobby room with a 2, 3, or 4 player cap
- Join a room by shareable six-character room code
- Choose or change a deck before the game starts
- Ready/unready state and automatic `ready` room status once all seated players are ready
- Supabase Realtime subscription per room through `room:{roomId}`
- Local-room fallback when Supabase env vars are not configured

## Notes

Riftcodex documents `GET /cards` and a nested card payload with `classification`, `attributes`, `text`, `set`, and `media` fields. The normalizer accepts those fields and a few common aliases so the browser stays tolerant of minor API shape changes.
