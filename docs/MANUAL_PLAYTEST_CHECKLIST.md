# Manual Playtest Checklist

Use this checklist for quick alpha smoke tests before sharing a build or asking another player to try a room.

## Human vs RiftBot

- Start RiftForge and create a human vs RiftBot room.
- Load or select a constructed deck.
- Ready up, start the game, and confirm the opening mulligan appears.
- Keep the opening hand or mulligan at least one card.
- Confirm your hand cards are visible to you after mulligan.
- Confirm RiftBot advances through its turn steps without manual help.
- Play a Unit from hand and confirm it stays in Base.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- Pass through the turn and confirm the phase guidance updates.
- Move a Unit to the battlefield.
- Resolve a showdown if one starts.
- Confirm the game log shows readable entries for play, move, target, showdown, scoring, and unsupported-effect messages.

## Hidden Information

- Open a second player or spectator view when practical.
- Confirm your own hand card names are visible only to you.
- Confirm opponent hand cards are masked in player/spectator views.
- Confirm Vision/private reveal log entries are not visible to the wrong viewer.

## Support Messaging

- In Deck Builder, check that Partial cards say they are playable for alpha testing but may be incomplete.
- Check that Unsupported cards say they are blocked in enforced play.
- Check that Banned cards say they are not legal in constructed.
- Check that Not Audited or missing-data cards are not presented as supported.

## Connection And Error Feedback

- Trigger one invalid action, such as playing during a non-Main phase, and confirm a visible warning appears.
- Disconnect/reconnect the local server if testing locally and confirm the reconnect/offline message is understandable.
- Use the Copy debug info button after an error and confirm it does not include hidden hand or deck contents.
