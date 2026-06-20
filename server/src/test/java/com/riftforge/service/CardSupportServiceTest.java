package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardSupportServiceTest {
  private final CardDataService cardDataService = mock(CardDataService.class);
  private final CardSupportService service = new CardSupportService(cardDataService);

  @Test
  void auditedStarterUnitsAreSupported() {
    assertThat(service.statusFor(card("vanguard-sergeant", "Vanguard Sergeant", "Unit", "")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("daring-poro", "Daring Poro", "Unit", "[Assault] (+1 Might while attacking.)")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("laurent-duelist", "Laurent Duelist", "Unit", "[Assault 2] (+2 Might while attacking.)")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("noxian-drummer", "Noxian Drummer", "Unit", "When I move to a battlefield, play a 1 Might Recruit unit token here.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("loyal-poro", "Loyal Poro", "Unit", "[Deathknell] If I didn't die alone, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("lonely-poro", "Lonely Poro", "Unit", "[Deathknell] If I died alone, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("vanguard-captain", "Vanguard Captain", "Unit", "[Legion] When you play me, play two Recruit unit tokens here.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("stellacorn-herder", "Stellacorn Herder", "Unit", "When I move, draw 1.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
    assertThat(service.statusFor(card("disarming-rake", "Disarming Rake", "Unit", "When you play me, you may kill a gear.")))
        .isEqualTo(CardSupportStatus.SUPPORTED);
  }

  @Test
  void unauditedGenericUnitsRemainPartial() {
    assertThat(service.statusFor(card("some-unit", "Some Unit", "Unit", "")))
        .isEqualTo(CardSupportStatus.PARTIAL);
  }

  @Test
  void unsupportedSpellsRemainBlockedEvenIfNamedDifferently() {
    CardDefinition spell = card("counter-trick", "Counter Trick", "Spell", "Counter an enemy spell.");
    when(cardDataService.isUnsupportedAction("counter-trick")).thenReturn(true);

    assertThat(service.statusFor(spell)).isEqualTo(CardSupportStatus.UNSUPPORTED);
  }

  @Test
  void ireliaPartialCardsExposeSpecificReasons() {
    assertThat(service.summarize(card("tideturner", "Tideturner", "Unit", "[Hidden]")).reason())
        .contains("Hidden foundation").contains("location swap");
    assertThat(service.summarize(card("adaptatron", "Adaptatron", "Unit", "When I conquer, you may kill a gear. If you do, buff me.")).reason())
        .contains("conquer trigger").contains("Buff state");
    assertThat(service.summarize(card("gust", "Gust", "Spell", "[Reaction] Return a unit at a battlefield with 3 Might or less.")).reason())
        .contains("3 Might or less")
        .contains("full official any-time Reaction timing");
    assertThat(service.summarize(card("discipline", "Discipline", "Spell", "[Reaction] Give a unit +2 Might this turn. Draw 1.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("+2 Might")
        .contains("drawing 1");
    assertThat(service.summarize(card("en-garde", "En Garde", "Spell", "[Reaction] Give a friendly unit +1 Might this turn, then an additional +1 Might this turn if it is the only unit you control there.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("+1 Might")
        .contains("only unit there");
    assertThat(service.summarize(card("defiant-dance", "Defiant Dance", "Spell", "[Reaction] Give a unit +2 Might this turn and another unit -2 Might this turn.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("+2 Might")
        .contains("-2 Might");
    assertThat(service.summarize(card("flash", "Flash", "Spell", "[Reaction] Move up to 2 friendly units to base.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("up to two friendly")
        .contains("Base");
    assertThat(service.summarize(card("star-crossed", "Star-Crossed", "Spell", "[Reaction] Return a friendly unit and an enemy unit to their owners' hands.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("friendly public")
        .contains("enemy public")
        .contains("owners' hands");
    assertThat(service.summarize(card("eclipse", "Eclipse", "Spell", "[Reaction] Give a unit -4 :rb_might: this turn. [Predict].")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("-4 Might")
        .contains("private owner-only Predict choice");
    assertThat(service.summarize(card("stupefy", "Stupefy", "Spell", "[Reaction] Give a unit -1 :rb_might: this turn, to a minimum of 1 :rb_might:. Draw 1.")).reason())
        .contains("alpha chain-window Reaction support")
        .contains("-1 Might")
        .contains("minimum of 1")
        .contains("drawing 1 privately");
    assertThat(service.summarize(card("moonfall", "Moonfall", "Spell", "[Action] Choose a battlefield where you have units. You may move up to one enemy unit to that battlefield. Then give enemy units there -2 :rb_might: this turn.")).reason())
        .contains("active battlefield")
        .contains("may move one enemy")
        .contains("-2 Might");
    assertThat(service.summarize(card("charm", "Charm", "Spell", "Move an enemy unit.")).reason())
        .contains("enemy public battlefield Unit/Champion")
        .contains("Base")
        .contains("movement choices");
    assertThat(service.summarize(card("sunken-temple", "Sunken Temple", "Battlefield", "When you conquer here with one or more [Mighty] units, you may pay 1 to draw 1.")).reason())
        .contains("conquer-with-Mighty")
        .contains("optional pay-1 draw")
        .contains("active Battlefield lanes");
    assertThat(service.summarize(card("targons-peak", "Targon's Peak", "Battlefield", "When you conquer here, ready up to 2 runes at the end of this turn.")).reason())
        .contains("queues end-turn readying")
        .contains("up to two tapped friendly runes")
        .contains("Player-selected rune choice");
    assertThat(service.summarize(card("the-syren", "The Syren", "Gear", ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.")).reason())
        .contains("paying 1 energy")
        .contains("friendly public battlefield Unit/Champion")
        .contains("activated ability timing");
    assertThat(service.summarize(card("last-rites", "Last Rites", "Gear", "[Equip] — :rb_rune_chaos:, Recycle 2 cards from your trash (Pay the cost: Attach this to a unit you control.)")).reason())
        .contains("Chaos rune plus recycle-2-from-Trash equip cost")
        .contains("bottom of the owner's Main Deck pool")
        .contains("full official Equip timing");
    assertThat(service.summarize(card("zhonya", "Zhonya's Hourglass", "Gear", "[Hidden] If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it.")).reason())
        .contains("protect a friendly public Unit/Champion")
        .contains("Hidden Reaction-for-0 timing")
        .contains("competing replacement choices");
    assertThat(service.summarize(card("irelia-fervent", "Irelia - Fervent", "Champion", "[Deflect] When you choose or ready me, give me +1 :rb_might: this turn.")).reason())
        .contains("explicit-ready trigger")
        .contains("+1 Might this turn")
        .contains("choose-trigger coverage is incomplete")
        .contains("automatic ready-step trigger timing remains deferred");
    assertThat(service.summarize(card("diana-scorn", "Diana - Scorn of the Moon", "Legend", "[Reaction] :rb_exhaust:: [Add] :rb_energy_1:. Spend this Energy only during showdowns.")).reason())
        .contains("focused showdown player")
        .contains("showdown-only Energy")
        .contains("Broader official Reaction/resource timing");
    assertThat(service.summarize(card("defy", "Defy", "Spell", "[Reaction] Counter a spell.")).reason())
        .contains("no more than 4 energy")
        .contains("no more than 1 premium rune")
        .contains("countering counters remain deferred");
    assertThat(service.summarize(card(
            "not-so-fast",
            "Not So Fast",
            "Spell",
            "[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.")).reason())
        .contains("friendly Unit/Champion Unit or Gear")
        .contains("Ability-chain targets")
        .contains("countering counters remain deferred");
    assertThat(service.summarize(card("abandon", "Abandon", "Spell", "[Reaction] Counter a spell. Return it to its owner's hand instead of putting it in their trash. [Predict].")).reason())
        .contains("return that spell card to hand")
        .contains("private Predict choice")
        .contains("ability targets remain deferred");
    assertThat(service.summarize(card("hard-bargain", "Hard Bargain", "Spell", "[Reaction] [Repeat] :rb_energy_2: Counter a spell unless its controller pays :rb_energy_2:.")).reason())
        .contains("controller pays 2 energy")
        .contains("owner-only prompt")
        .contains("Repeat")
        .contains("countering counters remain deferred");
    assertThat(service.summarize(card("stacked-deck", "Stacked Deck", "Spell", "Look at the top 3 cards of your deck. Put 1 into hand and recycle the rest.")).reason())
        .contains("narrow alpha chain")
        .contains("private top-3 choice");
  }

  private CardDefinition card(String id, String name, String type, String rulesText) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, rulesText, 1, 1, List.of());
  }
}
