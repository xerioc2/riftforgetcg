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

The uploaded decklists are the current authoritative source for exact card
counts. Guide-widget API captures remain useful secondary references.

See `docs/meta/UPLOADED_META_DECKS.md` for the complete table.

| Archetype | Uploaded source | Shape check | Support status | Key blockers / review notes |
| --- | --- | --- | --- | --- |
| Irelia, Blade Dancer | `irelia_wins_s3_shanghai_city_challenge.txt` | Pass | Blocked | Uploaded blockers are now `Charm`, `Zhonya's Hourglass`, and `The Syren`; Defiant Dance and Flash are Partial alpha chain-backed Reactions. |
| Diana, Scorn of the Moon | `diana_wins_s3_suzhou_city_challenge.txt` | Pass | Blocked | `Hard Bargain`, `Abandon`, `The Syren`; Diana remains the recommended next target. Flash is Partial alpha support. |
| Annie, Dark Child | `annie_4th_at_lille_regional_qualifier.txt` | Needs review | Blocked | `Annie - Dark Child - Starter` (`OGS-017`) is missing from local card cache; remaining Unsupported blocker is `Hard Bargain`. Flash is Partial alpha support. |
| Master Yi, Wuju Bladesman | `master_yi_wins_s3_guangzhou_city_challenge.txt` | Needs review | Blocked | `Master Yi - Wuju Bladesman - Starter` (`OGS-019`) is missing from local card cache; blockers include `Zhonya's Hourglass` and `Charm`. |
| LeBlanc, Deceiver | `leblanc_wins_s3_zhongshan_city_challenge.txt` | Pass | Blocked | `Mirror Image`, `Deadly Flourish`, `Hidden Blade`, `Cull the Weak`. |
| Vex, Gloomist | `vex_top_8_at_s3_zhongshan_city_challenge.txt` | Needs review | Blocked | Uploaded list infers 37 main-deck cards after chosen Champion; `Allay - Eager Admirer` (`UNL-041`) is missing from local card cache. |
| Azir, Emperor of the Sands | `azir_wins_lille_regional_qualifier.txt` | Pass | Blocked | `Facebreaker`, `Hidden Blade`; token/location/scoring text remains partial. |
| Sivir, Battle Mistress | `sivir_2nd_at_sydney_regional_qualifier.txt` | Pass | Blocked | `Scryer's Bloom`, `Lunar Boon`, `Treasure Trove`, `Pack of Wonders`, `Dazzling Aurora`. |
| Fiora, Grand Duelist | `fiora_wins_s3_beijing_city_challenge.txt` | Pass | Blocked | Added as extra meta data; blockers include `Hidden Blade`, `Challenge`, and several Partial combat/Gear cards. |
| Draven, Glorious Executioner | `draven_wins_new_zealand_10k_open.txt` | Pass | Blocked | Added as extra meta data; blockers include `Switcheroo`, `Hard Bargain`, and `Falling Star`. |

## Meta Priority Table

| Legend / archetype | Meta share | Win rate | Deck count | Current support status | Guide list status | Top blockers | Next implementation target |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Irelia, Blade Dancer | 8% | 54% | 1637 | Blocked | 2 list(s) extracted | Uploaded list: Charm (UNSUPPORTED), Zhonya's Hourglass (UNSUPPORTED), The Syren (UNSUPPORTED), plus important Partial Legend/Reaction/Gear effects | Finish current/Irelia support slice |
| Diana, Scorn of the Moon | 7% | 54% | 1438 | Blocked | 3 list(s) extracted | Uploaded list: Abandon (UNSUPPORTED), Hard Bargain (UNSUPPORTED), The Syren (UNSUPPORTED), plus interaction-heavy Partial effects | Reviewer-prioritized next interaction deck |
| Master Yi, Wuju Bladesman | 9% | 60% | 1710 | Blocked | 3 list(s) extracted | Charm (UNSUPPORTED), Repulse (UNSUPPORTED), Zhonya's Hourglass (UNSUPPORTED), Amateur Recital (PARTIAL), Grove of the God-Willow (PARTIAL) | Raw meta leader; audit after gameplay notes |
| Azir, Emperor of the Sands | 4% | 51% | 728 | Blocked | 1 list(s) extracted | Charm (UNSUPPORTED), Cull the Weak (UNSUPPORTED), Facebreaker (UNSUPPORTED), Hidden Blade (UNSUPPORTED), Wind Wall (UNSUPPORTED) | Later raw-meta audit |
| Sivir, Battle Mistress | n/a | n/a | n/a | Blocked | 1 list(s) extracted | Dazzling Aurora (UNSUPPORTED), Flurry of Blades (UNSUPPORTED), Lunar Boon (UNSUPPORTED), Pack of Wonders (UNSUPPORTED), Sabotage (UNSUPPORTED) | Additional guide-sourced archetype |
| Vex, Gloomist | 5% | 46% | 971 | Blocked | 3 list(s) extracted | Abandon (UNSUPPORTED), Allay - Eager Admirer (NOT_AUDITED), Blast Cone (UNSUPPORTED), Charm (UNSUPPORTED), Emperor's Divide (UNSUPPORTED) | Later raw-meta audit |
| LeBlanc, Deceiver | 7% | 52% | 1376 | Blocked | 3 list(s) extracted | Cull the Weak (UNSUPPORTED), Deadly Flourish (UNSUPPORTED), Hidden Blade (UNSUPPORTED), Imperial Decree (UNSUPPORTED), Mirror Image (UNSUPPORTED) | Later raw-meta audit |
| Annie, Dark Child | n/a | n/a | n/a | Blocked | 1 list(s) extracted | Uploaded list: Annie - Dark Child - Starter (NOT_AUDITED), Hard Bargain (UNSUPPORTED), plus Partial Aurora/Battlefield/Champion effects | Aurora shell candidate with Miss Fortune |

## Reviewer-Prioritized Support Order

1. Finish the current/Irelia support slice.
2. Diana interaction deck.
3. Aurora shell: Annie now has a guide-sourced list; Miss Fortune still needs a representative list.
4. Master Yi audit pending representative gameplay notes, despite the raw meta lead.
5. LeBlanc.
6. Vex.
7. Azir.
8. Sivir as an additional guide-sourced archetype if reviewer demand rises.

Diana remains the recommended next implementation target because reviewer signal says it is interaction-heavy and growing. It should stress Reaction/chain/priority, targeting, and combat timing more usefully than picking solely by raw meta share.

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

Use the extracted Diana lists to choose one narrow support slice, preferably the highest-repeat unsupported/partial interaction pattern shared across Diana lists. Keep Miss Fortune in the Aurora bucket until a real list is provided, and do not implement from archetype names alone.
