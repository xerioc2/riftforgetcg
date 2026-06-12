# Manual Playtest Checklist

Use this checklist for quick alpha smoke tests before sharing a build or asking another player to try a room.

## Morning Smoke Test

- Start the Spring server fresh or launch the packaged app from a clean state.
- If card data or stat parsing changed, clear or refresh the local card cache.
- Create a human vs RiftBot room.
- Load or select a constructed deck, then ready and start.
- Keep the opening hand or mulligan at least one card.
- Confirm RiftBot updates live after mulligan and during its turn.
- Play a Unit and confirm it stays in Base.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- Equip Gear to a valid friendly Unit/Champion and confirm the log says what was equipped.
- Move a Unit to the battlefield.
- If you have a clean Ambush Unit and already control a battlefield unit, use the Ambush button and confirm it enters the battlefield ready.
- If a showdown starts and you have a supported `[Action]` card, try it during the showdown window.
- Resolve a showdown if one starts.
- If Deathknell/token cards appear, confirm the log and board state update clearly.
- Check your hand remains visible to you and opponent hands remain masked.
- Trigger one invalid action and confirm the visible error is understandable.
- Use Copy debug info if stuck and confirm it omits hidden hand/deck contents.
- When filing a GitHub issue, include room code, phase, active player, last error, expected behavior, actual behavior, and screenshots/logs if useful.

## Human vs RiftBot

- Start RiftForge and create a human vs RiftBot room.
- Load or select a constructed deck.
- Ready up, start the game, and confirm the opening mulligan appears.
- Keep the opening hand or mulligan at least one card.
- Confirm your hand cards are visible to you after mulligan.
- Confirm RiftBot advances through its turn steps without manual help.
- Play a Unit from hand and confirm it stays in Base.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- Play Equip Gear when available and confirm it attaches to a friendly Unit/Champion.
- Pass through the turn and confirm the phase guidance updates.
- Move a Unit to the battlefield.
- If an Ambush card appears, confirm it either shows a clear Ambush option with a friendly battlefield unit or explains why Ambush/additional cost is unavailable.
- If a showdown starts and you have a supported `[Action]` card, confirm the UI says you may play it or resolve the showdown.
- Resolve a showdown if one starts.
- Confirm attached Gear goes to Trash if its host leaves play in a supported path.
- Confirm the game log shows readable entries for play, move, target, showdown, scoring, and unsupported-effect messages.

## Hidden Information

- Open a second player or spectator view when practical.
- Confirm your own hand card names are visible only to you.
- Confirm opponent hand cards are masked in player/spectator views.
- If you draw a `[Hidden]` card, hide it with a ready rune; confirm you see its name in your hidden area and the opponent/spectator sees only a hidden-card count.
- Confirm Vision/private reveal log entries are not visible to the wrong viewer.
- Confirm the spectator/public room view has no action buttons or `legalActions`.
- Confirm opponent Hidden cards cannot be selected as targets unless a future reveal effect explicitly grants permission.
- After a match ends, confirm History shows public winner/score summary only, not hand/deck/log contents.

## Support Messaging

- In Deck Builder, check that Partial cards say they are playable for alpha testing but may be incomplete.
- Check that Unsupported cards say they are blocked in enforced play.
- Check that Banned cards say they are not legal in constructed.
- Check that Not Audited or missing-data cards are not presented as supported.

## Connection And Error Feedback

- Trigger one invalid action, such as playing during a non-Main phase, and confirm a visible warning appears.
- Disconnect/reconnect the local server if testing locally and confirm the reconnect/offline message is understandable.
- Use the Copy debug info button after an error and confirm it does not include hidden hand or deck contents.
