# Manual Playtest Checklist

Use this checklist for quick alpha smoke tests before sharing a build or asking another player to try a room.

## What Should I Test?

- Install or launch the latest alpha build from GitHub Releases, or start the local dev server if testing from source.
- Create a room or join one with a room code.
- Select a deck in the lobby, review support warnings, then Ready and Start.
- Start a human vs RiftBot or human vs human game.
- Keep your opening hand or mulligan at least one card.
- Play a Unit and confirm it stays visible in Base.
- Confirm your Legend starts in the Legend zone and your chosen Champion starts in the Champion zone.
- Confirm the Legend cannot be dragged to the battlefield, and the chosen Champion only deploys after you have enough available energy.
- If your deck has Champion Units in the MainDeck, confirm they draw/play as main-deck cards instead of starting in the Champion zone.
- Use a targeted effect and confirm the target prompt and error messages make sense.
- If an optional choice prompt appears, choose an option and confirm the game resumes.
- If you play Stacked Deck, confirm only you see the top-card options, one card goes to hand, and the rest recycle without appearing in the public log.
- Play Gear to Base, then equip it to a friendly Unit or Champion if you draw one.
- Confirm attached Gear stays with a host that moves from Base to battlefield.
- Confirm attached Gear returns to Base, not Trash, if its host dies or leaves
  public play in a supported path.
- Hide a card if you draw a `[Hidden]` card.
- Try Ambush if the UI says it is available.
- Move a Unit to the battlefield.
- Play a supported `[Action]` card during a showdown if one appears.
- Resolve a showdown.
- Trigger or inspect an unsupported-card message.
- Check support badges in the deck builder, hand, hover preview, and inspect modal.
- Use Report issue or Copy debug info if anything looks stuck or confusing. Include the build version from Alpha limits or copied debug info.

## Morning Smoke Test

- Start the Spring server fresh or launch the packaged app from a clean state.
- If card data or stat parsing changed, clear or refresh the local card cache.
- Create a human vs RiftBot room.
- Load or select a constructed deck, then ready and start.
- Keep the opening hand or mulligan at least one card.
- Confirm RiftBot updates live after mulligan and during its turn.
- Play a Unit and confirm it stays in Base.
- If using Fiora Vanguard, play Vanguard Sergeant and confirm it behaves as a normal Unit with no extra prompt.
- If Daring Poro or Laurent Duelist attacks, confirm Assault adds Might only while attacking.
- Inspect those cards in the deck builder and in-game hover/inspect views; their support badge should say Supported.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- If a private choice prompt appears, confirm only the prompted player sees the options.
- If Stacked Deck appears, resolve the top-3 private prompt and confirm opponent/spectator views do not reveal those card names.
- Play Gear to Base, equip it to a valid friendly Unit/Champion, and confirm the log says what was equipped.
- Confirm Gear cannot target the Champion while it is still in the Champion zone; it can target a Champion only after the Champion has moved into Base or the battlefield.
- Try an illegal Champion/Legend drag and confirm it snaps back to the zone with a readable warning.
- If a Champion is destroyed in combat, confirm it returns to the Champion zone and any attached Gear returns to Base.
- Move a Unit to the battlefield.
- If you have a clean Ambush Unit and already control a battlefield unit, use the Ambush button and confirm it enters the battlefield ready.
- If a showdown starts and you have a supported `[Action]` card, try it during the showdown window.
- Resolve a showdown if one starts.
- If Deathknell/token cards appear, confirm the log and board state update clearly.
- Check your hand remains visible to you and opponent hands remain masked.
- Trigger one invalid action and confirm the visible error is understandable.
- Use Copy debug info if stuck and confirm it omits hidden hand/deck contents.
- When filing a GitHub issue, include build version, room code, phase, active player, last error, expected behavior, actual behavior, whether supported-cards-only was enabled, and screenshots/logs if useful.

## Human vs RiftBot

- Start RiftForge and create a human vs RiftBot room.
- Load or select a constructed deck.
- Ready up, start the game, and confirm the opening mulligan appears.
- Keep the opening hand or mulligan at least one card.
- Confirm your hand cards are visible to you after mulligan.
- Confirm RiftBot advances through its turn steps without manual help.
- Play a Unit from hand and confirm it stays in Base.
- With Fiora Vanguard, confirm Vanguard Sergeant is treated as Supported and Daring Poro/Laurent Duelist remain readable Assault examples.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- If an optional choice prompt appears, resolve it and confirm normal actions return afterward.
- If you draw Stacked Deck, play it and confirm the private card-selection modal is readable and returns to normal actions after resolving.
- Play Equip Gear when available and confirm it enters Base first.
- Click the Gear in Base, choose a friendly Unit/Champion, and confirm it attaches.
- Pass through the turn and confirm the phase guidance updates.
- Move a Unit to the battlefield.
- If an Ambush card appears, confirm it either shows a clear Ambush option with a friendly battlefield unit or explains why Ambush/additional cost is unavailable.
- If a showdown starts and you have a supported `[Action]` card, confirm the UI says you may play it or resolve the showdown.
- Resolve a showdown if one starts.
- Confirm attached Gear returns to Base if its host leaves play in a supported path.
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
