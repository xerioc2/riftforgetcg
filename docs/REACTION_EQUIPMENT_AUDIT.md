# Playtest Reaction and Equipment Audit

Last audit: 2026-06-17

Source of truth for this audit:

- `src/lib/starterDecks.ts`
- local normalized card cache `~/.riftforge/cards-cache.json` version 2, last written 2026-06-15 00:39 local time
- `CardSupportService`
- `src/lib/deckSupport.ts`
- `docs/SUPPORTED_CARDS.md`
- `docs/CARD_RULES_BACKLOG.md`

This is an exact-text planning audit, not a gameplay implementation sprint. A
card is recommended as Supported only when its full printed text is implemented
and directly tested. Partial means an alpha path exists but official timing,
targeting, replacement, or card-specific edge cases remain missing.

## Reaction Cards

| Card | In current playtest source | Type | Cost | Domains / premium | Exact local rules text | Backend status | Frontend status | Docs status | Bucket | Recommended status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Gust | Irelia Tempo | Spell | 1 | CHAOS / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Return a unit at a battlefield with 3 :rb_might: or less to its owner's hand.` | Partial | Partial | Partial | Simple chain-backed Reaction effect | Partial | Alpha chain response works only while focused in the Stacked Deck chain window. Target must be a public battlefield Unit/Champion with 3 Might or less. Broad any-time Reaction timing remains missing. |
| Defy | Irelia Tempo | Spell | 1 | CALM / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Counter a spell that costs no more than :rb_energy_4: and no more than :rb_rune_rainbow:.` | Partial | Partial | Partial | Counterspell / chain-item target | Partial | Can counter supported public pending spell chain items within the alpha chain window when cost restrictions pass. Broad spell targets, ability targets, countering counters, and official any-time timing remain deferred. |
| Not So Fast | Irelia Tempo | Spell | 2 | CALM / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Counter an enemy spell or ability that chooses a friendly unit or gear.` | Partial | Partial | Partial | Counterspell / chain-item target | Partial | Can counter supported public pending enemy spell chain items only when the chain target is your friendly Unit/Champion Unit or Gear. Ability-chain targets and countering counters remain deferred. |
| Discipline | Irelia Tempo | Spell | 2 | CALM / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Give a unit +2 :rb_might: this turn. Draw 1.` | Partial | Partial | Partial | Simple chain-backed Reaction effect | Partial | Alpha chain response works only while focused in the Stacked Deck chain window. Target must be a public battlefield Unit/Champion. On resolution it gives +2 Might this turn and draws 1 privately. Broad any-time Reaction timing remains missing. |
| En Garde | Irelia Tempo | Spell | 1 | CALM / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Give a friendly unit +1 :rb_might: this turn, then an additional +1 :rb_might: this turn if it is the only unit you control there.` | Partial | Partial | Partial | Simple chain-backed Reaction effect | Partial | Alpha chain response works only while focused in the Stacked Deck chain window. Target must be a friendly public battlefield Unit/Champion. The lone-friendly-unit-at-location bonus is implemented for the current single-battlefield alpha. Broad any-time Reaction timing remains missing. |
| Defiant Dance | Irelia Tempo | Spell | 1 | CALM, CHAOS / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Give a unit +2 :rb_might: this turn and another unit -2 :rb_might: this turn.` | Unsupported | Unsupported | Unsupported | Multi-target staged targeting | Unsupported | Needs two separate target roles and two stat modifiers, then Reaction chain hookup. Keep blocked until both effect and timing are tested. |
| Star-Crossed | Irelia Tempo | Spell | 3 | CHAOS / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Return a friendly unit and an enemy unit to their owners' hands.` | Partial | Partial | Partial | Multi-target staged targeting | Partial | Paired friendly/enemy return is scripted with staged targets, but Reaction timing is not connected to chain. |
| Riposte | Fiora Vanguard | Spell | 2 | BODY, ORDER / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Choose a friendly unit and a spell. Counter that spell and give that unit +:rb_might: equal to that spell's Energy cost this turn.` | Partial | Partial | Partial | Counterspell plus multi-target staged targeting | Partial | Needs a friendly Unit target, spell chain-item target, variable Might amount, and official Reaction window. Not safe to implement until Defy/Not So Fast patterns are stable. |
| Stalking Wolf | Fiora Vanguard | Unit | 4 | ORDER / premium 0 | `[Ambush] (You may play me as a [Reaction] to a battlefield where you have units.)As an additional cost to play me, kill a Bird, Cat, Dog, or Poro you control. You may play me to its battlefield (even if you don't have other units there).` | Partial | Unsupported | Partial | Needs broad official timing | Unsupported for enforced play | Backend marks Unit text Partial because support gates focus on Spell/Gear effects; frontend blocks additional-cost text. Keep effectively blocked until Ambush-as-Reaction and additional-cost sacrifice are implemented. |
| Flash | Explicit audit/test pool, not starter main | Spell | 2 | CHAOS / premium 0 | `[Reaction] (Play any time, even before spells and abilities resolve.)Move up to 2 friendly units to base.` | Unsupported | Unsupported | Not listed | Broad official timing plus multi-select movement | Unsupported | Needs optional count, up-to-two target selection, movement legality, and any-time Reaction timing. |
| Back Off | Explicit audit/test pool, not starter main | Spell | 3 | CALM / premium 0 | `[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)[Action] (Play on your turn or in showdowns.)[Stun] a unit. (It doesn't deal combat damage this turn.)If you played this from your hand, draw 1.` | Partial | Partial | Not listed | Hidden/Action with Stun | Partial | Current heuristics see draw 1, but full text needs Hidden play-from-hidden, Stun target scripting, and from-hand conditional draw. Do not promote. |
| Edge of Night | Explicit audit/test pool, not starter main | Gear | 3 | CHAOS / premium 0 | `[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)When you play this from face down, attach it to a unit you control (here).[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)` | Partial | Partial | Not listed | Equipment lifecycle plus Hidden play-from-hidden | Partial | Basic Equip text fits the alpha equipment lifecycle, but face-down play and automatic same-location attach are deferred. |
| Switcheroo | Explicit audit/test pool, not starter main | Spell | 2 | CHAOS / premium 0 | `[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)[Action] (Play on your turn or in showdowns.)Swap the Might of two units at the same battlefield this turn.` | Unsupported | Unsupported | Not listed | Hidden/Action with same-location multi-target modifier | Unsupported | Needs Hidden play, two same-battlefield targets, temporary Might swapping, and cleanup. |
| Charm | Irelia Tempo | Spell | 1 | CALM / premium 0 | `Move an enemy unit.` | Unsupported | Unsupported | Unsupported | Movement effect | Unsupported | Not a Reaction, but included because it is a high-impact Irelia spell. Needs enemy movement target rules and legal destination handling. |

## Equipment and Gear Cards

| Card | In current playtest source | Type | Cost | Domains / premium | Exact local rules text | Backend status | Frontend status | Docs status | Bucket | Recommended status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Guardian Angel | Irelia Tempo | Gear | 2 | CALM / premium 0 | `[Equip] :rb_rune_calm: (:rb_rune_calm:: Attach this to a unit you control.)` | Partial | Partial | Partial | Equipment lifecycle only | Partial | Alpha lifecycle works: play Gear to Base, equip from Base, attach to friendly Unit/Champion, return to Base when host leaves. Exact Equip payment/domain and official timing remain incomplete. |
| Boots of Swiftness | Irelia Tempo | Gear | 3 | CHAOS / premium 0 | `[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)` | Partial | Partial | Partial | Equipment lifecycle only | Partial | Same alpha lifecycle as Guardian Angel. Exact Chaos payment and official timing remain incomplete. |
| Zhonya's Hourglass | Explicit audit/test pool, not starter main | Gear | 2 | CALM / premium 0 | `[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it. (Send it to base. This isn't a move.)` | Unsupported | Unsupported | Not listed | Equipment effect / replacement / prevention | Unsupported | This is not an Equip lifecycle card. It needs Hidden, replacement/prevention timing, death prevention, heal, exhaust, and recall. Keep blocked. |
| Edge of Night | Explicit audit/test pool, not starter main | Gear | 3 | CHAOS / premium 0 | `[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)When you play this from face down, attach it to a unit you control (here).[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)` | Partial | Partial | Not listed | Equipment lifecycle plus Hidden play-from-hidden | Partial | Basic Equip lifecycle applies only after the card is in Base. Face-down attach remains deferred. |
| Disarming Rake | Irelia sideboard / playtest pool | Unit | 3 | CALM / premium 0 | `When you play me, you may kill a gear.` | Supported | Supported | Supported | Trigger plus optional prompt plus Gear destroy | Supported | Not Gear itself, but it is the main tested Gear-interaction card. Frontend/backend/docs support status now agree. |

## Implementation Buckets

### Already Implemented Correctly

- Disarming Rake: enter-play optional prompt and Gear destroy are implemented and tested.
- Narrow alpha counter paths: Defy and Not So Fast are implemented only for their exact supported public chain-item shapes.
- Gust, Discipline, and En Garde: implemented as narrow public chain-backed
  board-target Reaction responses.

### Simple Chain-Backed Reaction Effect

- Discipline (implemented for the current alpha chain window)
- En Garde (implemented for the current alpha chain window)

These use existing target selection, temporary Might, and draw helpers through
public chain items. They stay Partial until official any-time Reaction timing is
implemented.

### Counterspell / Chain-Item Target

- Defy
- Not So Fast
- Riposte

Defy and Not So Fast are narrow and should stay Partial. Riposte should wait
until one card can target both a friendly unit and a spell chain item, then
apply variable Might from the countered spell cost.

### Multi-Target Staged Targeting

- Star-Crossed
- Defiant Dance
- Riposte
- Switcheroo

Star-Crossed has direct effect support but no Reaction timing. Defiant Dance and
Switcheroo should stay blocked until two-target stat modifier logic and cleanup
are tested.

### Equipment Lifecycle Only

- Guardian Angel
- Boots of Swiftness
- Edge of Night's plain `[Equip]` clause

The lifecycle is alpha-ready, but official Equip payment/timing remains Partial.

### Equipment Effect / Replacement / Prevention

- Zhonya's Hourglass

Keep Unsupported. It is a replacement/prevention card, not a basic Equip card.

### Needs Optional Prompt

- Disarming Rake already uses the prompt framework.
- Zhonya's Hourglass may eventually need prevention/replacement prompt timing if
the effect is optional or has competing replacement effects.

### Needs Trigger Framework

- Disarming Rake is covered as an enter-play trigger/prompt.
- Edge of Night's face-down play attach depends on later Hidden play-from-hidden
trigger timing.

### Needs Ability-Chain Support

- Not So Fast's printed text includes "or ability"; current alpha support only
handles public spell chain items.
- Riposte and future counters should not broaden into abilities until ability
chain items exist.

### Needs Broad Official Timing

- All `[Reaction]` cards except the narrow Gust/Discipline/En Garde/Defy/Not So Fast chain responses.
- Stalking Wolf's Ambush-as-Reaction path.
- Hidden play-from-hidden cards such as Back Off, Edge of Night, and Switcheroo.

### Keep Unsupported For Now

- Defiant Dance
- Flash
- Switcheroo
- Zhonya's Hourglass
- Charm
- Stalking Wolf in enforced play

## Recommended Implementation Order

1. Star-Crossed chain hookup
   - Effect is already staged and scripted.
   - Add chain-backed Reaction entry point, then keep Partial for official timing.
2. Defiant Dance
   - Requires two-target staged stat modifiers and cleanup.
   - Medium risk because negative Might and "another unit" duplicate prevention
     must be exact.
3. Riposte
   - Higher risk because it combines a unit target, spell chain target, counter,
     and variable Might based on Energy cost.
4. Edge of Night Hidden attach
   - Depends on Hidden play-from-hidden timing and same-location checks.
5. Zhonya's Hourglass
   - Defer. Replacement/prevention effects are a larger timing system.

## Support-Map Mismatches To Resolve Later

- `Disarming Rake`: backend/docs are Supported, frontend `deckSupport.ts`
  currently reports generic Partial. This is UI/status metadata only.
- `Stalking Wolf`: backend/docs are Partial because Unit support is not gated by
  `isUnsupportedAction`; frontend blocks additional-cost text. Treat as blocked
  in enforced play until additional-cost Ambush is implemented.
- `Back Off`: support heuristics see `draw 1`, but full printed text depends on
  Hidden play-from-hidden and Stun. Keep Partial at most until card-specific
  support exists.
