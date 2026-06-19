# Manual Playtest Checklist

Use this checklist for quick alpha smoke tests before sharing a build or asking another player to try a room.

## What Should I Test?

- Install or launch the latest alpha build from GitHub Releases, or start the local dev server if testing from source.
- Create a room or join one with a room code.
- Select a deck in the lobby, review support warnings, then Ready and Start.
- Start a human vs RiftBot or human vs human game.
- Choose one Battlefield before mulligan and confirm the opponent's unchosen Battlefield pool is not visible.
- After both players choose, confirm selected Battlefield plaques appear in the active Battlefield lane area, can be hovered/read like cards, and cannot be targeted, moved, tapped, or fought.
- Remember the current 1v1 alpha has two active shared Battlefield lanes. Deck construction still requires three Battlefield cards, but that does not mean three active lanes are used in Duel/bot games. Battlefield effects, hidden slots, official "here" targeting, and full location rules remain deferred.
- Keep your opening hand or mulligan at least one card.
- During Channel and payment, confirm in-play Rune cards show their card art/name when known, can be hovered like cards, and still tap or recycle with the existing click/right-click controls.
- Play a Unit and confirm it stays visible in Base.
- Confirm your Legend starts in the Legend zone and your chosen Champion starts in the Champion zone.
- Confirm the Legend cannot be dragged to the battlefield, and the chosen Champion only deploys after you have enough available energy.
- If your deck has Champion Units in the MainDeck, confirm they draw/play as main-deck cards instead of starting in the Champion zone.
- Use a targeted effect and confirm the target prompt and error messages make sense.
- If an optional choice prompt appears, choose an option and confirm the game resumes.
- If an optional payment prompt appears, try Pay when you have enough energy and confirm the energy is spent and the effect resolves.
- If an optional payment prompt appears, try Decline and confirm the prompt clears without applying the effect.
- If you play Disarming Rake while Gear is in play, confirm Yes asks you to
  choose a Gear, Cancel/No leaves Gear alone, and a selected Gear goes to Trash.
- If you play Stacked Deck, confirm it opens the narrow priority/chain window
  before the top-card choice appears; after players pass/resolve the chain, only you see the
  top-card options, one card goes to hand, and the rest recycle without
  appearing in the public log.
- If the chain panel appears, confirm it lists public-safe chain items
  top-to-bottom, shows focus/ready status clearly, and does not expose hidden
  or private card names.
- If neither player has a legal Gust/Discipline/En Garde/Defy/Not So Fast response, confirm the chain skips
  dead response windows and leaves the top item ready to resolve instead of
  requiring extra Pass Chain clicks.
- If Defy is in hand during chain focus, confirm it can target a supported
  public spell chain item such as Stacked Deck, counters that item, and prevents
  the Stacked Deck top-card choice from appearing.
- If Not So Fast is in hand during chain focus, confirm it stays unavailable
  against untargeted Stacked Deck, but can counter an enemy Gust only when that
  Gust chooses your friendly Unit/Champion Unit or Gear.
- Play Gear to Base, then pay its printed Equip cost to equip it to a friendly
  Unit or Champion if you draw one.
- Confirm attached Gear shows a host label, can be inspected by clicking/hovering the Gear, and cannot be dragged independently.
- Confirm attached Gear stays with a host that moves from Base to a battlefield lane.
- Confirm attached Gear returns to Base, not Trash, if its host dies or leaves
  public play in a supported path.
- Hide a card if you draw a `[Hidden]` card.
- Try Ambush if the UI says it is available.
- Move a Unit to the battlefield.
- During a showdown, confirm the focused player can either play a supported `[Action]` card or click `Pass Focus`.
- After both relevant players pass focus, resolve the showdown, then use `Assign Damage` when prompted.
- Trigger or inspect an unsupported-card message.
- Check support badges in the deck builder, hand, hover preview, and inspect modal.
- Use Report issue or Copy debug info if anything looks stuck or confusing. Include the build version from Alpha limits or copied debug info.

## Morning Smoke Test

- Start the Spring server fresh or launch the packaged app from a clean state.
- If card data or stat parsing changed, clear or refresh the local card cache.
- Create a human vs RiftBot room.
- Load or select a constructed deck, then ready and start.
- Choose one Battlefield from the three-card setup prompt and confirm it appears on the board after both players choose.
- Confirm in-play Rune cards display as card-backed resource plaques when they are channeled; hidden rune deck contents should not be visible.
- Move one Unit into each visible active lane across a few turns and confirm the card lands in the lane you dropped it on.
- Move opposing Units into the same lane and confirm a same-lane showdown starts with that lane highlighted.
- Move opposing Units into different lanes and confirm no cross-lane showdown starts.
- Keep the opening hand or mulligan at least one card.
- Confirm RiftBot updates live after mulligan and during its turn.
- Play a Unit and confirm it stays in Base.
- If using Fiora Vanguard, play Vanguard Sergeant and confirm it behaves as a normal Unit with no extra prompt.
- If Daring Poro or Laurent Duelist attacks, confirm Assault adds Might only while attacking.
- Inspect those cards in the deck builder and in-game hover/inspect views; their support badge should say Supported.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- If a private choice prompt appears, confirm only the prompted player sees the options.
- If Disarming Rake appears in the sideboard/playtest pool, try both declining
  and destroying a friendly or enemy Gear.
- If Stacked Deck appears, confirm it first waits on the chain, then resolve
  the top-3 private prompt and confirm opponent/spectator views do not reveal
  those card names.
- Play Gear to Base, equip it to a valid friendly Unit/Champion, and confirm the log says what was equipped.
- Confirm illegal Equip targets, such as opponent units, Gear, Battlefields, Runes, Legends, hidden cards, and Champion-zone identity cards, show readable warnings/rejections.
- Confirm Gear cannot target the Champion while it is still in the Champion zone; it can target a Champion only after the Champion has moved into Base or the battlefield.
- Try an illegal Champion/Legend drag and confirm it snaps back to the zone with a readable warning.
- If a Champion is destroyed in combat, confirm it returns to the Champion zone and any attached Gear returns to Base.
- Move a Unit to the battlefield.
- If you have a clean Ambush Unit and already control a battlefield unit, use the Ambush button and confirm it enters the battlefield ready.
- If a showdown starts and you have focus, try a supported `[Action]` card or click `Pass Focus`.
- Confirm the attacker cannot resolve until both relevant players pass focus.
- If a chain prompt/status appears, confirm only the focused player sees `Pass Chain` or `Resolve Chain` and normal game actions stay paused until it clears.
- Confirm the chain panel status updates do not imply counterspells are
  broadly supported yet; Gust, Defy, and narrow Not So Fast are the only real
  alpha Reaction responses.
- Confirm unsupported Reactions such as Defiant Dance do not appear playable,
  even while the chain panel is active.
- During human vs RiftBot, try clicking an unsupported Reaction such as Defiant
  Dance during a no-chain or opponent-turn window; confirm a local warning
  appears, the UI does not stay stuck waiting for a server update, and RiftBot
  continues or ends its turn.
- Resolve a showdown after the focus/pass cycle completes, then assign combat damage when prompted.
- In human vs RiftBot, create a multi-unit combat where RiftBot has more combatants than the opposing side; confirm RiftBot assigns all damage, the showdown resolves, and the game does not freeze.
- If practical, smoke both paths: RiftBot attacking with multiple units and RiftBot defending with multiple units.
- If Deathknell/token cards appear, confirm the log and board state update clearly.
- Check your hand remains visible to you and opponent hands remain masked.
- Trigger one invalid action and confirm the visible error is understandable.
- Use Copy debug info if stuck and confirm it omits hidden hand/deck/rune deck contents while including public in-play rune summaries.
- When filing a GitHub issue, include build version, room code, phase, active player, last error, expected behavior, actual behavior, whether supported-cards-only was enabled, and screenshots/logs if useful.

## Human vs RiftBot

- Start RiftForge and create a human vs RiftBot room.
- Load or select a constructed deck.
- Ready up, start the game, and confirm the Battlefield selection prompt appears before mulligan.
- Choose one Battlefield, then confirm mulligan appears after both players have selected.
- Confirm the selected Battlefield displays appear in the visible Battlefield lanes after setup, show hover previews, and remain non-interactive location displays.
- Confirm the Rune row shows actual in-play Rune cards when known, generic fallback runes when art/card data is missing, and pending tapped/recycled selections remain visually distinct.
- Move Units to `Battlefield 1` and `Battlefield 2` and confirm each card stays in the lane chosen by the drag/drop. In 1v1, confirm no `Battlefield 3` placeholder is shown.
- During Main Phase with no active showdown, drag a ready Unit/Champion from `Battlefield 1` to `Battlefield 2` and confirm it moves lanes instead of snapping back.
- Try dragging a Unit/Champion to the lane it already occupies and confirm it does not create a new move/showdown.
- Move units from both players into the same lane and confirm the lane highlight/phase bar identify the active showdown location.
- Move units from both players into different lanes and confirm they do not start a showdown with each other.
- Keep the opening hand or mulligan at least one card.
- Confirm your hand cards are visible to you after mulligan.
- Confirm RiftBot advances through its turn steps without manual help.
- Play a Unit from hand and confirm it stays in Base.
- With Fiora Vanguard, confirm Vanguard Sergeant is treated as Supported and Daring Poro/Laurent Duelist remain readable Assault examples.
- Play or attempt a targeted effect and confirm target prompts/errors are visible.
- If an optional choice prompt appears, resolve it and confirm normal actions return afterward.
- If an optional payment prompt appears, confirm only the prompted player sees Pay/Decline and the opponent view does not reveal prompt details.
- If you draw Stacked Deck, play it and confirm the private card-selection modal is readable and returns to normal actions after resolving.
- Play Equip Gear when available and confirm it enters Base first.
- Click the Gear in Base, choose a friendly Unit/Champion, and confirm it attaches.
- Confirm the attached Gear appears smaller near its host, the host shows the Gear name, and clicking the attached Gear opens inspect rather than selecting it as a movable combat card.
- Pass through the turn and confirm the phase guidance updates.
- Move a Unit to the battlefield.
- If an Ambush card appears, confirm it either shows a clear Ambush option with a friendly battlefield unit or explains why Ambush/additional cost is unavailable.
- If a showdown starts and you have a supported `[Action]` card, confirm the UI says you may play it or pass focus only while you are focused.
- Confirm `Resolve Showdown` appears only after both relevant players pass focus.
- After combat damage assignment, confirm board cards and hover/inspect show readable Might, current HP, and marked damage until the model clears it.
- Play Stacked Deck to open the narrow alpha chain. If the opponent has Gust in
  hand, confirm Gust shows as a `Respond` option only while that player has
  chain focus, highlights only battlefield Units/Champions with 3 Might or
  less, resolves before Stacked Deck, and returns the target to hand after
  chain passes/resolution.
- If the opponent has Defy in hand, confirm it can select the Stacked Deck
  chain item from the chain panel, resolves above Stacked Deck, marks Stacked
  Deck countered, and no private Stacked Deck choice appears.
- If the opponent has Not So Fast in hand, confirm it cannot select untargeted
  Stacked Deck but can select an enemy Gust chain item that chooses that
  opponent's friendly Unit/Champion Unit or Gear.
- If no player has Gust, Defy, or Not So Fast available, confirm the chain advances directly
  to `Resolve Chain` without making players click through empty response
  windows.
- While RiftBot is active in Main, click an unsupported Reaction card in your
  hand and confirm the warning does not freeze the client; Copy debug info
  should include awaiting update, last submitted action, last failure, active
  showdown location, battlefield controllers, public card locations,
  attachments, chain, choice, and legal-action fields.
- Resolve a showdown after the focus/pass cycle completes, then confirm `Assign Damage` appears before cleanup.
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
- Use the Copy debug info button after an error and confirm it does not include hidden hand, deck, or rune deck contents.
