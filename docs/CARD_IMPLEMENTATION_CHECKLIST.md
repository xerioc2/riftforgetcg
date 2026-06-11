# RiftForge Card Implementation Checklist

Use this checklist before promoting any card to Supported.

## 1. Audit

- Identify card type: Legend, Champion, Unit, Spell, Gear, Rune, Battlefield, or Token.
- Confirm current official text, costs, domains, keywords, and any errata.
- Check tournament legality and current banlist.
- Identify whether the card is setup-only, drawable, playable, attached, triggered, activated, static, or replacement-based.

## 2. Cost And Timing

- Confirm normal energy cost.
- Confirm premium/domain cost, if any.
- Identify legal play phase and reaction/action timing.
- Identify legal source and destination zones.
- Identify target requirements and hidden-information constraints.

## 3. Behavior

- Map each keyword to a `KeywordHandler` or existing engine path.
- Map on-play text to an `OnPlayEffectHandler` or explicit engine behavior.
- Map triggered text to a `TriggeredAbilityHandler`.
- Map activated text to an `ActivatedAbilityHandler`.
- Map static modifiers to a `StaticModifierHandler`.
- Map prevention/replacement text to a `ReplacementEffectHandler`.
- If any required behavior is missing, leave the card Partial or Unsupported.

## 4. Tests

- Add focused unit tests for the handler or engine path.
- Add integration coverage when the card changes phase flow, hidden information, payment, scoring, movement, or showdown behavior.
- Test invalid uses as well as valid uses.
- Verify hidden hand/deck information is not exposed in projected state.

## 5. Support Metadata

- Update `CardSupportService` only after behavior and tests justify a status.
- Update `docs/SUPPORTED_CARDS.md` with status, supported effects, unsupported effects, and tests.
- Use conservative labels:
  - Supported: implemented and tested.
  - Partial: common path works, known gaps remain.
  - Unsupported: blocked or not implemented.
  - Banned: not legal in current constructed format.
  - Not Audited: card has not been reviewed.

## 6. Validation

Run:

```bash
cd server
mvn -q test
mvn -q -DskipTests compile

cd ..
npm run build
git diff --check
```
