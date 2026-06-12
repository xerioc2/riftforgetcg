# Alpha Review Checklist

Use this checklist before public alpha builds, release notes, or larger mechanics sprints.

## Game Loop

- Room creation, join, ready, start, and reset still work.
- Constructed setup keeps Legend, Champion, main deck, runes, and battlefields partitioned.
- Opening hands draw only from the main deck.
- Mulligan advances to AWAKEN only after all players keep/mulligan.
- Phase guidance and legal action hints match server-projected `legalActions`.

## Bot Behavior

- RiftBot advances through post-mulligan AWAKEN without manual input.
- Human-first games advance to the bot's next turn without stalling.
- Bot decisions remain gated by `LegalActionsService`.
- Bot does not use sandbox-only moves in ENFORCED games.
- Bot failure logs are warnings/errors, not silent freezes.

## WebSocket And Projection Privacy

- User-specific state shows only that player's hand.
- Opponent and spectator hands are masked.
- Public room broadcasts are safe for spectators.
- REST fallback state fetches use the same projection rules.
- Invalid room/player tokens are rejected.
- Global/no-room presence or matchmaking connections do not grant room privileges.
- Serialized projections do not expose `deckPool`, `runeDeckPool`, or selected hidden setup internals.

## Targeting And Effects

- Target-required cards cannot resolve without a target.
- Friendly, enemy, and any-battlefield target prompts are clear.
- Selected targets receive effects; the engine does not fall back to the first valid target.
- Return-to-hand does not fire Deathknell.
- Simple draw, +Might, return, and ready helpers remain covered by tests.
- Unsupported multi-target, counter, and chain patterns remain blocked.

## Combat

- Assault and Assault X apply only while attacking.
- Shield and Shield X apply only while defending.
- Mighty helper uses the correct effective Might context.
- Tank assignment priority remains enforced.
- Stun/Stunned damage prevention remains covered.
- Units/Champions can move to battlefield; non-combatants cannot fight as units.

## Deathknell And Tokens

- Deathknell fires exactly once on supported death paths.
- Deathknell does not fire on return/bounce.
- Simultaneous deaths stay deterministic.
- Recruit tokens can be created by supported scripts.
- Tokens do not affect deck counts.
- Noxian Drummer triggers only on move to battlefield.
- Vanguard Captain Legion requires another card to have been played earlier this turn.
- Played-card-this-turn tracking resets on turn change.

## Equip

- Basic `[Equip]` Gear requires a friendly battlefield Unit/Champion target.
- Non-Equip Gear remains unsupported in enforced play.
- Equipped Gear stays attached instead of going to Trash immediately.
- Gear cannot move to battlefield or fight as a unit.
- Attached Gear cleans up deterministically when the host dies or returns to hand.
- Guardian Angel and Boots of Swiftness remain Partial until timing/payment/card text are fully modeled.

## UI And Log Readability

- Phase guidance explains the current step.
- Available action hints come from server-projected legal actions.
- Invalid moves show visible, friendly errors.
- Game log entries identify plays, targets, equips, movement, showdowns, scoring, unsupported effects, and bot activity clearly enough for bug reports.
- Copy debug info excludes hidden hand/deck contents.

## Support Messaging

- Partial cards say they are playable for alpha testing but may be incomplete.
- Unsupported cards say they are blocked in enforced play.
- Banned cards say they are not legal in constructed.
- Not Audited cards are not presented as supported.
- Starter deck cards are not promoted to Supported without behavior and tests.

## Intentionally Deferred

- Multiple battlefields remain post-alpha work.
- Reaction/action chain timing remains deferred.
- Full rune domain/payment prompts remain incomplete.
- Full target/choice prompt stack remains incomplete.
- Full equipment replacement/reattachment rules remain incomplete.
- XP, Hunt, Level, Buff, Hidden, Ambush, and Predict remain incomplete unless explicitly implemented in a later sprint.
