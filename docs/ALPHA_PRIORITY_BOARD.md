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

The reason is simple: the current single-battlefield alpha is now broad enough
to generate useful feedback. More rules systems before the first external pass
will raise regression risk faster than playtest value.

## High Impact + Low/Medium Risk: Do Soon

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Constructed deck correction | High | Medium | Pre-playtest | Done | Role-separated Legend, chosen Champion, main deck, runes, and battlefields unblock real deck testing. |
| Pre-mulligan Battlefield selection | High | Low | Pre-playtest | Done | Constructed games now reveal/lock one selected Battlefield per player before mulligans while keeping full multi-Battlefield play deferred. |
| Champion deployment payment | High | Medium | Pre-playtest | Done | Makes chosen Champions usable without treating them as normal hand cards. |
| Equipment lifecycle | High | Medium | Pre-playtest | Done | Gear can be played to Base, equipped, cleaned up from hosts, and tested with starter decks. |
| Illegal drag snapback | High | Low | Pre-playtest | Done | Prevents UI confusion when server rejects a move. |
| Pending choice prompts | High | Medium | Pre-playtest | Done | Owner-only yes/no, optional payment, Stacked Deck top-card choice, and Predict-style ordering prompts exist without a full chain system. |
| Target selection | High | Medium | Pre-playtest | Done | Required for simple spells, Equip, and many bug reports to be meaningful. |
| Action / Defender Action windows | High | Medium | Pre-playtest | Done | Provides a limited showdown action window while keeping Reaction/chain deferred. |
| Hidden foundation | Medium | Medium | Pre-playtest | Done | Establishes masking and zones without later play-from-hidden timing. |
| Ambush foundation | Medium | Medium | Pre-playtest | Done | Narrow Main-phase battlefield play path; Ambush-as-Reaction remains deferred. |
| Deathknell / tokens | Medium | Medium | Pre-playtest | Done | Supports safe starter-unit scripts, token plumbing, and Scuttle Crab's 1v1 private hand reveal while XP/facedown remain deferred. |
| Responsive board | High | Low | Pre-playtest | Done | Makes fullscreen playtests readable and reduces layout complaints. |
| Support badges | High | Low | Pre-playtest | Done | Sets tester expectations for Partial, Unsupported, Banned, and Not Audited cards. |
| Privacy regression suite | High | Medium | Pre-playtest | Done | Protects hand, hidden, private-choice, projection, and debug-info surfaces. |
| Issue/report flow | High | Low | Pre-playtest | Done | Gives testers a consistent path to submit useful bug reports. |
| Final alpha stabilization sweep | High | Low | Pre-playtest | Done | Run validation, cross-check support policy, update release notes, and fix only small bugs. |
| Optional payment prompts | Medium | Medium | Post-playtest | Done | Generic prompts can offer Pay/Decline, validate available alpha energy at resolution time, spend that energy, and clear safely. Real trigger hookups remain conservative. |
| Stellacorn Herder move trigger | Medium | Medium | Post-playtest | Done | Migrated through the alpha trigger dispatcher; movement draws 1 and same-zone reposition stays inert. |
| Abandoned Hall spell trigger | Medium | Medium | Post-playtest | Next | Useful battlefield trigger candidate, but depends on clearer prompt/payment policy. |

## High Impact + High Risk: Plan Carefully After Playtest

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Trigger framework v1 | High | High | Post-playtest | Done | Small deterministic event/handler dispatcher exists for alpha movement triggers. Chain, priority, optional ordering, and complex trigger windows remain deferred. |
| Charm / enemy movement | High | High | Post-playtest | Deferred | Needs precise legal destinations, ownership, battlefield/control rules, and UI affordances. |
| Hidden play-from-hidden | High | High | Post-playtest | Deferred | Requires timing windows, payment, reveal/masking transitions, and target legality. |
| Ambush-as-Reaction | High | High | Post-playtest | Deferred | Depends on chain/reaction windows and correct response priority. |
| Reaction / chain / counterspells | High | High | Post-playtest | Deferred | Required for many cards but likely the largest rules-system risk. |
| Multiple battlefields | High | High | Later | Deferred | Officially important, intentionally post-alpha because it touches movement, targeting, showdown, scoring, bot decisions, and layout. |
| Sideboard / tournament match structure | Medium | High | Later | Deferred | Valuable for organized play after single-game alpha flow is stable. |

## Low Impact + Low Risk: Polish / Backlog

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Copy and wording polish | Low | Low | Pre-playtest | In progress | Keep improving logs, hints, and limitation text as testers stumble. |
| Keyword glossary wording | Low | Low | Pre-playtest | Done | Helps explain card text without implying unsupported rules are complete. |
| Release note cleanup | Low | Low | Pre-playtest | Done | Keep draft notes aligned with what the installer actually ships. |
| Manual playtest checklist updates | Low | Low | Pre-playtest | Done | Useful for repeatable smoke tests and external tester instructions. |
| More support-audit notes | Low | Low | Post-playtest | Next | Add concrete buckets when new tester decks or bug reports reveal patterns. |

## Low Impact + High Risk: Avoid For Alpha

| Work item | Impact | Risk | Timing | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| XP / Hunt / Level / Buff | Low | High | Later | Deferred | Important for broader card pool, but not needed for first alpha stability. |
| Full tournament procedure | Low | High | Later | Deferred | Best-of-three, sideboarding, and match structure should wait until core games are stable. |
| Broad card-pool scripting | Low | High | Later | Deferred | Implement starter/playtest cards first; avoid chasing the whole database before feedback. |
| Advanced replacement/prevention effects | Low | High | Later | Deferred | Needs chain/effect-layer work and many edge-case tests. |

## Current Alpha Feature Posture

The alpha should be described as:

- Single-battlefield playtest model.
- Constructed deck setup enforced, with role-separated Legend, chosen Champion, and pre-mulligan Battlefield selection.
- Starter-deck-oriented card support, with badges and warnings.
- Partial Action/showdown support, not full Reaction/chain.
- Strong privacy regression coverage for current hidden-information surfaces.
- Installer/release flow through GitHub Releases rather than tracked binaries.

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
4. Keep multiple battlefields, Reaction/chain, and XP/Hunt/Level/Buff out of
   scope until the single-battlefield alpha is stable.
