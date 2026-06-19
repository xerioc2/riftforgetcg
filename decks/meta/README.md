# Meta Decklist Intake

This directory is for representative competitive decklists used by the meta
support roadmap in `docs/META_DECK_SUPPORT.md`.

Do not invent decklists. Add or regenerate lists here only from reviewer,
playtester, tournament, guide, or explicit user-provided sources.

## Current Source

`scripts/meta-deck-audit.mjs` extracts guide-sourced decklists from Riftbound.gg
embedded deck widgets and writes normalized audit JSON here.

Generated files include:

- `audit-summary.json`
- `annie/*.json`
- `azir/*.json`
- `diana/*.json`
- `irelia/*.json`
- `leblanc/*.json`
- `master-yi/*.json`
- `sivir/*.json`
- `vex/*.json`

Each deck JSON records the source guide URL, DotGG deck slug/API URL, captured
date, source card ids/names/counts, local card-cache resolution, conservative
support status, and top blockers.

Miss Fortune remains manual-list-needed because no Miss Fortune guide URL was
supplied in the extraction sprint.

## Rerun

```powershell
npm.cmd run audit:meta-decks
```

The script uses `~/.riftforge/cards-cache.json` for local RiftForge card
resolution. If the cache is stale, start the server once to refresh card data
before rerunning the audit.

## Manual Intake Format

If a future deck cannot be extracted from a guide/API, add it manually only from
an explicit source and keep the same role separation:

- Legend
- Champion / chosen champion if known
- Main Deck
- Rune Deck
- Battlefields
