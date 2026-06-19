# RiftForge Alpha Priority Board

Last updated: 2026-06-13

This board ranks remaining alpha work by playtest value and stability risk. It
is intentionally practical rather than exhaustive: the goal is to choose the
next sprint by likely tester impact, not by official-rules completeness.

Scoring:

- Impact: High / Medium / Low
- Risk: High / Medium / Low
- Timing: Pre-playtest / Post-playtest / Later
- Status: Done / In progress / Next / Deferred

## Recommended Pre-Playtest Stop Line

After Equipment lifecycle, final stabilization, and report/issue flow are in
place, stop feature work for the external alpha. From that point forward, take
only blocker bugfixes, privacy fixes, installer/release fixes, and small
readability/documentation updates until playtest feedback lands.

The reason is simple: the current 1v1 active-lane alpha is now broad enough
to generate useful feedback. More rules systems before the first external pass
will raise regression risk faster than playtest value.

## High Impact + Low/Medium Risk: Do Soon

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Constructed deck correction | High | Medium | Pre-playtest | Done | Role-separated Legend, chosen Champion, main deck, runes, and battlefields unblock real deck testing. |
| Pre-mulligan Battlefield selection | High | Low | Pre-playtest | Done | Constructed games now reveal/lock one selected Battlefield per player before mulligans while keeping full multi-location Battlefield play deferred. |
| Champion deployment payment | High | Medium | Pre-playtest | Done | Makes chosen Champions usable without treating them as normal hand cards. |
| Equipment lifecycle | High | Medium | Pre-playtest | Done | Gear can be played to Base, equipped to friendly public Unit/Champion hosts, cleaned up from hosts, displayed with attachment labels, and tested with starter decks. Equip cost/payment is covered for audited Gear, and explicit Gear stat-modifier plumbing exists without guessing unsupported card text. |
| Illegal drag snapback | High | Low | Pre-playtest | Done | Prevents UI confusion when server rejects a move. |
| Pending choice prompts | High | Medium | Pre-playtest | Done | Owner-only yes/no, optional payment, Stacked Deck top-card choice, and Predict-style ordering prompts exist; Stacked Deck now creates its private choice after resolving from the narrow alpha chain. |
| Target selection | High | Medium | Pre-playtest | Done | Required for simple spells, Equip, and many bug reports to be meaningful. |
| Showdown focus/pass Action window | High | Medium | Pre-playtest | Done | Focused participants can play supported Action cards, supported targeted Reactions, or pass; attacker resolution is gated until both relevant players pass in succession. |
| Combat damage assignment v1 | High | Medium | Post-playtest | Done | Showdowns now enter an assignment step after focus/pass; server validates all-damage, Tank, lethal, duplicate, and excess policies. Client/bot use deterministic Tank-first assignment until manual damage UI is worth the risk. |
| Priority / chain foundation | High | Medium | Post-playtest | In progress | Public-safe chain state, lifecycle status, counter-ready metadata, public-safe target summaries, focus passing, ready-to-resolve gating, projection, bot handling, compact chain panel, local priority-stop toggles, and a narrow `PriorityWindowService` opt-in layer exist. Stacked Deck and simple public `Draw 1` spells are opener patterns; Gust, Discipline, En Garde, Defiant Dance, and Flash can be played in own-turn, focused showdown, or focused chain windows; Defy and Not So Fast remain chain-target counters. Human priority is bluff-safe and does not auto-pass based on hidden hand contents; bot players may auto-pass empty windows. Rune innate Energy/Power actions do not open or enter the chain. Broader Reaction/counterspell card support remains deferred. |
| Hidden foundation | Medium | Medium | Pre-playtest | Done | Establishes masking and zones without later play-from-hidden timing. |
| Ambush foundation | Medium | Medium | Pre-playtest | Done | Narrow Main-phase battlefield play path; Ambush-as-Reaction remains deferred. |
| Deathknell / tokens | Medium | Medium | Pre-playtest | Done | Supports safe starter-unit scripts, token plumbing, and Scuttle Crab's 1v1 private hand reveal while XP/facedown remain deferred. |
| Responsive board | High | Low | Pre-playtest | Done | Makes fullscreen playtests readable and reduces layout complaints. |
| Multi-location Battlefield lanes v1 | High | Medium | Post-playtest | Done | The board renders the active Battlefield lanes for the current format: 1v1 Duel/bot games show `bf-0` and `bf-1`, while `bf-2` remains reserved for future formats. Cards are placed by `battlefieldLocationId`, drag-to-lane sends active destinations, active showdown lanes highlight, and per-lane controller indicators show. Battlefield effects and full official location rules remain deferred. |
| Support badges | High | Low | Pre-playtest | Done | Sets tester expectations for Partial, Unsupported, Banned, and Not Audited cards. |
| Privacy regression suite | High | Medium | Pre-playtest | Done | Protects hand, hidden, private-choice, projection, and debug-info surfaces. |
| Issue/report flow | High | Low | Pre-playtest | Done | Gives testers a consistent path to submit useful bug reports. |
| Final alpha stabilization sweep | High | Low | Pre-playtest | Done | Run validation, cross-check support policy, update release notes, and fix only small bugs. |
| Optional payment prompts | Medium | Medium | Post-playtest | Done | Generic prompts can offer Pay/Decline, validate available alpha energy at resolution time, spend that energy, and clear safely. Real trigger hookups remain conservative. |
| Reviewer-guided meta support audit | High | Low | Post-playtest | Done | Riftbound.gg guide lists are extracted, and exact uploaded exports are preserved under `decks/meta/raw/` with normalized support audits in `decks/meta/normalized/` and `docs/meta/`. Diana remains the next requested card/rules support target because it is interaction-heavy and growing. Aurora support has an Annie list, but Miss Fortune still needs a real list. Master Yi stays tracked as the raw meta leader, but implementation should wait for gameplay notes. |
| Stellacorn Herder move trigger | Medium | Medium | Post-playtest | Done | Migrated through the alpha trigger dispatcher; movement draws 1 and same-zone reposition stays inert. |
| Abandoned Hall spell trigger | Medium | Medium | Post-playtest | Next | Useful battlefield trigger candidate, but depends on clearer prompt/payment policy. |

## High Impact + High Risk: Plan Carefully After Playtest

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Trigger framework v1 | High | High | Post-playtest | Done | Small deterministic event/handler dispatcher exists for alpha movement triggers. Chain, priority, optional ordering, and complex trigger windows remain deferred. |
| Charm / enemy movement | High | High | Post-playtest | In progress | Charm has narrow Partial support for selecting one enemy public battlefield Unit/Champion and moving it to Base. Broader official movement choices, control/location rules, and non-battlefield destinations remain deferred. |
| Hidden play-from-hidden | High | High | Post-playtest | Deferred | Requires timing windows, payment, reveal/masking transitions, and target legality. |
| Ambush-as-Reaction | High | High | Post-playtest | Deferred | Depends on chain/reaction windows and correct response priority. |
| Reaction / counterspells on real cards | High | High | Post-playtest | Deferred | Required for many cards but likely the largest rules-system risk; Gust, Discipline, En Garde, Defiant Dance, Flash, Defy, and Not So Fast cover narrow public chain paths, with targeted Reactions now also available on the active player's turn and focused showdown windows. Riposte counter behavior, abilities, hidden reactions, countering counters, private/choice effects, and broad timing should still be added one card/pattern at a time. |
| Full multi-location Battlefield model | High | High | Later | Deferred | A server/UI foundation now supports `bf-0`/`bf-1`/`bf-2` lanes, location-scoped movement, showdowns, combat, controller keys, scoring, and drag-to-lane destinations. Official-style 1v1 play still needs Battlefield effects, true selected Battlefield instances/objectives, "here" targeting, hidden slots, and bot strategy. It remains separate from 3+ player multiplayer support. |
| Sideboard / tournament match structure | Medium | High | Later | Deferred | Valuable for organized play after single-game alpha flow is stable. |

## Low Impact + Low Risk: Polish / Backlog

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Copy and wording polish | Low | Low | Pre-playtest | In progress | Keep improving logs, hints, and limitation text as testers stumble. |
| Keyword glossary wording | Low | Low | Pre-playtest | Done | Helps explain card text without implying unsupported rules are complete. |
| Release note cleanup | Low | Low | Pre-playtest | Done | Keep draft notes aligned with what the installer actually ships. |
| Manual playtest checklist updates | Low | Low | Pre-playtest | Done | Useful for repeatable smoke tests and external tester instructions. |
| More support-audit notes | Low | Low | Post-playtest | Next | Add concrete buckets when new tester decks or bug reports reveal patterns. |
| Meta deck support roadmap | Medium | Low | Post-playtest | Done | `docs/META_DECK_SUPPORT.md` now includes extracted guide-list and uploaded-export support audits for Master Yi, Irelia, Diana, LeBlanc, Vex, Azir, Sivir, Annie, plus extra Fiora/Draven data. Priority remains: finish Irelia, then Diana, then Aurora Miss Fortune / Annie. Miss Fortune remains list-needed and no decklists are invented. |

## Low Impact + High Risk: Avoid For Alpha

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| XP / Hunt / Level / Buff | Low | High | Later | Deferred | Important for broader card pool, but not needed for first alpha stability. |
| Full tournament procedure | Low | High | Later | Deferred | Best-of-three, sideboarding, and match structure should wait until core games are stable. |
| Broad card-pool scripting | Low | High | Later | Deferred | Implement starter/playtest cards first; avoid chasing the whole database before feedback. |
| Advanced replacement/prevention effects | Low | High | Later | Deferred | Needs chain/effect-layer work and many edge-case tests. |

## Current Alpha Feature Posture

The alpha should be described as:

- 1v1 active-lane playtest model with Battlefield effects and full official
  location rules still deferred.
- Constructed deck setup enforced, with role-separated Legend, chosen Champion, and pre-mulligan Battlefield selection.
- Starter-deck-oriented card support, with badges and warnings.
- Partial Action/showdown support with focus/pass v1 and a narrow priority/chain foundation for listed Reaction cards, not full Reaction/counterspell priority.
- Combat damage assignment v1 with server legality checks and deterministic
  alpha UI/bot assignment, not full manual damage splitting.
- Strong privacy regression coverage for current hidden-information surfaces.
- Installer/release flow through GitHub Releases rather than tracked binaries.
- Meta support planning should start from the extracted guide lists and
  reviewer signal. Current roadmap order is: finish the Irelia support slice,
  implement the smallest useful Diana blocker next, audit Aurora shell support
  through the extracted Annie list plus a future Miss Fortune list, keep Master
  Yi tracked as the raw meta leader pending gameplay notes, then audit
  LeBlanc/Vex/Azir/Sivir when playtest demand rises.

## Decision Rule For New Sprints

Before external alpha feedback:

1. Fix blocker bugs.
2. Fix privacy/security leaks.
3. Fix installer/release issues.
4. Improve clarity/docs if the change is low risk.
5. Defer new rules systems.

After feedback:

1. Pick the highest repeated playtest pain point.
2. Prefer one narrow card/rule slice with tests.
3. Update support badges and docs immediately.
4. Keep full Battlefield effects/location rules, broader Reaction/chain, and
   XP/Hunt/Level/Buff out of scope until the active-lane alpha is stable.
5. For meta support, do not implement from archetype names alone. Add the real
   list first, audit its unsupported/partial cards, then choose the highest
   repeated blocker.
