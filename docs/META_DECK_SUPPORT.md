# Meta Deck Support Roadmap

Last updated: 2026-06-19

This roadmap combines reviewer meta priorities with actual guide decklists extracted from Riftbound.gg and exact exported decklists uploaded by playtesters/reviewers. Decklists are captured as reviewable JSON under `decks/meta/`; support summaries are generated from the current local card cache and the same conservative support rules used by RiftForge frontend/backend support gates.

Do not treat an archetype as supported just because a list is present. `Supported` means a card is implemented and directly covered by policy/tests; `Partial` means playable alpha behavior may exist but exact rules can be incomplete; `Unsupported` and `Not Audited` still block supported-only enforced play.

## Extraction Method

- Riftbound.gg guide pages contain embedded deck widgets with `data-deck` slugs rather than complete static card lists.
- `scripts/meta-deck-audit.mjs` fetches each guide, extracts those slugs, loads each list through `https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=<slug>`, and resolves DotGG card ids through the DotGG card catalog.
- Each source card is matched by exact/base name against `~/.riftforge/cards-cache.json`. Unresolved or ambiguous names are recorded instead of guessed.
- Uploaded text exports are preserved exactly in `decks/meta/raw/`.
- `scripts/import-uploaded-meta-decks.mjs` parses uploaded lines like `3 Stacked Deck (OGN-183)`, resolves by name and source set code against the local card cache, and writes normalized files to `decks/meta/normalized/`.

## Uploaded Exact Decklist Summary

The uploaded decklists are the current authoritative source for exact card counts. Guide-widget API captures remain useful secondary references.

See `docs/meta/UPLOADED_META_DECKS.md` for the complete table.

| Archetype | Uploaded source | Shape check | Support status | S/P/U/NA | Top blockers |
| --- | --- | --- | --- | --- | --- |
| Irelia, Blade Dancer | decks/meta/raw/irelia_wins_s3_shanghai_city_challenge.txt | Pass | Playable | 4/23/0/0 | Irelia - Blade Dancer, Not So Fast, Abandoned Hall, Vex - Apathetic, Scuttle Crab |
| Diana, Scorn of the Moon | decks/meta/raw/diana_wins_s3_suzhou_city_challenge.txt | Pass | Playable | 1/24/0/0 | Diana - Scorn of the Moon, Hard Bargain, Star Spring, Abandoned Hall, Moonfall |
| Annie, Dark Child | decks/meta/raw/annie_4th_at_lille_regional_qualifier.txt | Needs review | Blocked | 1/21/0/1 | Annie - Dark Child - Starter, The Arena's Greatest, Seat of Power, Hard Bargain, Rek'Sai - Breacher |
| Master Yi, Wuju Bladesman | decks/meta/raw/master_yi_wins_s3_guangzhou_city_challenge.txt | Needs review | Blocked | 3/19/0/1 | Master Yi - Wuju Bladesman - Starter, Amateur Recital, Rengar - Trophy Hunter, Master Yi - Tempered, Dragonsoul Sage |
| LeBlanc, Deceiver | decks/meta/raw/leblanc_wins_s3_zhongshan_city_challenge.txt | Pass | Blocked | 1/16/4/0 | Mirror Image, Deadly Flourish, Hidden Blade, Cull the Weak, LeBlanc - Deceiver |
| Vex, Gloomist | decks/meta/raw/vex_top_8_at_s3_zhongshan_city_challenge.txt | Needs review | Blocked | 2/21/3/1 | Existential Dread, Blast Cone, Allay - Eager Admirer, Switcheroo, Vex - Gloomist |
| Azir, Emperor of the Sands | decks/meta/raw/azir_wins_lille_regional_qualifier.txt | Pass | Blocked | 2/19/2/0 | Facebreaker, Hidden Blade, Azir - Emperor of the Sands, Trifarian War Camp, Seat of Power |
| Sivir, Battle Mistress | decks/meta/raw/sivir_2nd_at_sydney_regional_qualifier.txt | Pass | Blocked | 2/13/7/0 | Scryer's Bloom, Lunar Boon, Treasure Trove, Pack of Wonders, Dazzling Aurora |
| Fiora, Grand Duelist | decks/meta/raw/fiora_wins_s3_beijing_city_challenge.txt | Pass | Blocked | 2/20/2/0 | Hidden Blade, Challenge, Fiora - Grand Duelist, Punch First, Sacrifice |
| Draven, Glorious Executioner | decks/meta/raw/draven_wins_new_zealand_10k_open.txt | Pass | Blocked | 1/21/2/0 | Switcheroo, Falling Star, Draven - Glorious Executioner, Fury Rune, Treasure Hoard |

## Meta Priority Table

| Legend / archetype | Meta share | Win rate | Deck count | Current support status | Guide list status | Top blockers | Next implementation target |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Irelia, Blade Dancer | 8% | 54% | 1637 | Blocked | 2 list(s) extracted | Heart of Dark Ice (UNSUPPORTED), Switcheroo (UNSUPPORTED), Abandoned Hall (PARTIAL), Sunken Temple (PARTIAL), Targon's Peak (PARTIAL) | Finish current/Irelia support slice |
| Diana, Scorn of the Moon | 7% | 54% | 1438 | Blocked | 3 list(s) extracted | Invert Timelines (UNSUPPORTED), Turn to Dust (UNSUPPORTED), Unchecked Power (UNSUPPORTED), Abandoned Hall (PARTIAL), Star Spring (PARTIAL) | Reviewer-prioritized next interaction deck |
| Master Yi, Wuju Bladesman | 9% | 60% | 1710 | Blocked | 3 list(s) extracted | Repulse (UNSUPPORTED), Amateur Recital (PARTIAL), Grove of the God-Willow (PARTIAL), Startipped Peak (PARTIAL), Janna - Savior (PARTIAL) | Raw meta leader; audit after gameplay notes |
| Azir, Emperor of the Sands | 4% | 51% | 728 | Blocked | 1 list(s) extracted | Cull the Weak (UNSUPPORTED), Facebreaker (UNSUPPORTED), Hidden Blade (UNSUPPORTED), Wind Wall (UNSUPPORTED), Hall of Legends (PARTIAL) | Later raw-meta audit |
| Sivir, Battle Mistress | n/a | n/a | n/a | Blocked | 1 list(s) extracted | Dazzling Aurora (UNSUPPORTED), Flurry of Blades (UNSUPPORTED), Lunar Boon (UNSUPPORTED), Pack of Wonders (UNSUPPORTED), Sabotage (UNSUPPORTED) | Additional guide-sourced archetype |
| Vex, Gloomist | 5% | 46% | 971 | Blocked | 3 list(s) extracted | Allay - Eager Admirer (NOT_AUDITED), Blast Cone (UNSUPPORTED), Emperor's Divide (UNSUPPORTED), Existential Dread (UNSUPPORTED), Switcheroo (UNSUPPORTED) | Later raw-meta audit |
| LeBlanc, Deceiver | 7% | 52% | 1376 | Blocked | 3 list(s) extracted | Cull the Weak (UNSUPPORTED), Deadly Flourish (UNSUPPORTED), Hidden Blade (UNSUPPORTED), Imperial Decree (UNSUPPORTED), Mirror Image (UNSUPPORTED) | Later raw-meta audit |
| Annie, Dark Child | n/a | n/a | n/a | Blocked | 1 list(s) extracted | Factory Recall (UNSUPPORTED), Switcheroo (UNSUPPORTED), Thermo Beam (UNSUPPORTED), Seat of Power (PARTIAL), The Arena's Greatest (PARTIAL) | Aurora shell candidate with Miss Fortune |

## Reviewer-Prioritized Support Order

1. Finish the current/Irelia support slice.
2. Diana interaction deck.
3. Aurora shell: Annie now has a guide-sourced list; Miss Fortune still needs a representative list.
4. Master Yi audit pending representative gameplay notes, despite the raw meta lead.
5. LeBlanc.
6. Vex.
7. Azir.
8. Sivir as an additional guide-sourced archetype if reviewer demand rises.

Diana remains the recommended next fidelity target because reviewer signal says it is interaction-heavy and growing. The uploaded Suzhou list is now enforced-playable by gate and selectable as `Diana Uploaded Meta - Playtest`, but it is still Partial-heavy and should stress Reaction/chain/priority, targeting, Battlefield effects, and combat timing more usefully than picking solely by raw meta share.

## Extracted Audit Files

- Irelia, Blade Dancer: `docs/meta/irelia.md`, `decks/meta/irelia/`
- Diana, Scorn of the Moon: `docs/meta/diana.md`, `decks/meta/diana/`
- Master Yi, Wuju Bladesman: `docs/meta/master-yi.md`, `decks/meta/master-yi/`
- Azir, Emperor of the Sands: `docs/meta/azir.md`, `decks/meta/azir/`
- Sivir, Battle Mistress: `docs/meta/sivir.md`, `decks/meta/sivir/`
- Vex, Gloomist: `docs/meta/vex.md`, `decks/meta/vex/`
- LeBlanc, Deceiver: `docs/meta/leblanc.md`, `decks/meta/leblanc/`
- Annie, Dark Child: `docs/meta/annie.md`, `decks/meta/annie/`
- Miss Fortune / Aurora: no guide URL was supplied in this sprint, so it remains manual-list-needed.

## Needed From Reviewers

- Miss Fortune Aurora deck list.
- Gameplay notes for Master Yi, especially mandatory interaction patterns and must-work cards.
- Notes on which Diana/Annie lists are preferred for playtesting if multiple tournament lists are present.
- Cards that must be exact before external testers should use supported-cards-only mode.

## Recommended Next Sprint

Use the extracted Diana lists to choose one narrow fidelity slice, preferably `Moonfall` or Diana Legend/Champion text, then Star Spring. Keep Miss Fortune in the Aurora bucket until a real list is provided, and do not implement from archetype names alone.
