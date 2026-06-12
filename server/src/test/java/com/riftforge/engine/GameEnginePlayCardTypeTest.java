package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.RepositionCardMove;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameStateProjectionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameEnginePlayCardTypeTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  GameEngine engine;

  @BeforeEach
  void setUp() {
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService));
    engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenReturn(false);
    when(cardDataService.isUnsupportedAction(anyString())).thenReturn(false);
    when(cardDataService.requiresBattlefieldTarget(anyString())).thenReturn(false);
    when(cardDataService.requiresFriendlyTarget(anyString())).thenReturn(false);
    when(cardDataService.requiresEnemyTarget(anyString())).thenReturn(false);
    when(cardDataService.isEquip(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Gear".equalsIgnoreCase(def.type()) && def.rulesText() != null && def.rulesText().toLowerCase().contains("[equip]");
    });
  }

  @Test
  void unitCanBePlayedToBase() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, play("unit", ZoneName.BASE));

    CardInstance unit = state.getCards().getFirst();
    assertThat(unit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(unit.isTapped()).isTrue();
    assertThat(unit.isHasSummoningSickness()).isTrue();
  }

  @Test
  void zeroHealthStarterUnitStaysInBaseWhenPlayed() {
    LiveGameState state = state(card("tideturner", "p1", ZoneName.HAND));
    state.getPlayers().getFirst().setAvailableEnergy(2);
    stubCard("tideturner", "Tideturner", "Unit", 2, 2, 0, "[Hidden] When you play me, you may choose a unit you control.");

    engine.applyMove(state, play("tideturner", ZoneName.BASE));

    CardInstance tideturner = state.getCards().getFirst();
    assertThat(tideturner.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(tideturner.getZone()).isNotEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getOwnerId().equals("p1") && card.getZone() == ZoneName.HAND);

    LiveGameState projected = new GameStateProjectionService(new LegalActionsService()).toPublicView(state, "p1");
    assertThat(projected.getCards())
        .anySatisfy(card -> {
          assertThat(card.getCardId()).isEqualTo("tideturner");
          assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
        });
  }

  @Test
  void unitCannotBePlayedDirectlyToBattlefield() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND));
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("unit", ZoneName.BATTLEFIELD)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Non-rune cards must be played to base.");
  }

  @Test
  void spellResolvesAndMovesToDiscard() {
    LiveGameState state = state(card("spell", "p1", ZoneName.HAND));
    stubCard("spell", "Spell", 0);

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void friendlyTargetAcceptedForFriendlyUnitEffect() {
    CardInstance spell = card("buff", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, friendly, enemy);
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a friendly unit +2 :rb_might: this turn.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("buff")).thenReturn(true);

    engine.applyMove(state, playTarget("buff", "friendly"));

    assertThat(friendly.getTemporaryPowerModifier()).isEqualTo(2);
    assertThat(enemy.getTemporaryPowerModifier()).isZero();
  }

  @Test
  void enemyTargetAcceptedForEnemyUnitEffect() {
    CardInstance spell = card("bounce", "p1", ZoneName.HAND);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, enemy);
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "enemy"));

    assertThat(enemy.getZone()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void enemyTargetRejectedForFriendlyOnlyEffect() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a friendly unit +2 :rb_might: this turn.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card requires a friendly unit.");
  }

  @Test
  void friendlyTargetRejectedForEnemyOnlyEffect() {
    LiveGameState state = state(
        card("bounce", "p1", ZoneName.HAND),
        card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("bounce", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card requires an enemy unit.");
  }

  @Test
  void nonUnitTargetRejectedWhenUnitRequired() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("gear", "p1", ZoneName.BATTLEFIELD));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +2 :rb_might: this turn.");
    stubCard("gear", "Gear", 0);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be a Unit or Champion.");
  }

  @Test
  void hiddenZonesCannotBeTargeted() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("hidden", "p2", ZoneName.HAND));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +2 :rb_might: this turn.");
    stubCard("hidden", "Hidden Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "hidden")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be on the battlefield.");
  }

  @Test
  void unsupportedMultiTargetCardRemainsBlocked() {
    LiveGameState state = state(card("multi", "p1", ZoneName.HAND), card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("multi", "Multi Spell", "Spell", 0, 0, 0, "Choose a friendly unit and an enemy unit.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.isUnsupportedAction("multi")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("multi", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's effect is not supported yet.");
  }

  @Test
  void equipTargetMustBeFriendlyUnit() {
    CardInstance gear = card("equip", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(gear, friendly);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("equip")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("equip")).thenReturn(true);

    engine.applyMove(state, playTarget("equip", "friendly"));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("friendly");
  }

  @Test
  void selectedTargetReceivesEffectInsteadOfFirstValidTarget() {
    CardInstance firstEnemy = card("first-enemy", "p2", ZoneName.BATTLEFIELD);
    CardInstance selectedEnemy = card("selected-enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("buff", "p1", ZoneName.HAND), firstEnemy, selectedEnemy);
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +3 :rb_might: this turn.");
    stubCard("first-enemy", "First Enemy", "Unit", 0, 2, 2, null);
    stubCard("selected-enemy", "Selected Enemy", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    engine.applyMove(state, playTarget("buff", "selected-enemy"));

    assertThat(firstEnemy.getTemporaryPowerModifier()).isZero();
    assertThat(selectedEnemy.getTemporaryPowerModifier()).isEqualTo(3);
  }

  @Test
  void unknownCardTypeIsRejectedInsteadOfUsingFallbackZoneRouting() {
    LiveGameState state = state(card("mystery", "p1", ZoneName.HAND));
    stubCard("mystery", "Mystery Card", "Mystery", 0, 0, 0, null);

    assertThatThrownBy(() -> engine.applyMove(state, play("mystery", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card type cannot be played from hand.");
  }

  @Test
  void spellCannotMoveToBattlefieldLikeAUnit() {
    LiveGameState state = state(card("spell", "p1", ZoneName.BASE));
    stubCard("spell", "Spell", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "spell")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only Units and Champions can move to the battlefield.");
  }

  @Test
  void gearCanBePlayedButCannotFightAsAUnit() {
    LiveGameState state = state(card("gear", "p1", ZoneName.HAND));
    stubCard("gear", "Gear", 0);

    engine.applyMove(state, play("gear", ZoneName.BASE));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BASE);
    state.getCards().getFirst().setTapped(false);
    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only Units and Champions can move to the battlefield.");
  }

  @Test
  void legendCannotBePlayedFromHand() {
    LiveGameState state = state(card("legend", "p1", ZoneName.HAND));
    stubCard("legend", "Legend", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("legend", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Legend cards cannot be played from hand.");
  }

  @Test
  void battlefieldCannotBePlayedFromHand() {
    LiveGameState state = state(card("battlefield", "p1", ZoneName.HAND));
    stubCard("battlefield", "Battlefield", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("battlefield", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Battlefield cards cannot be played from hand.");
  }

  @Test
  void championCanStartShowdownFromChampionZone() {
    LiveGameState state = state(
        card("champion", "p1", ZoneName.CHAMPION),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("champion", "Champion", 0);
    stubCard("enemy", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion"));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("champion");
  }

  @Test
  void championCannotBePlayedFromHand() {
    LiveGameState state = state(card("champion", "p1", ZoneName.HAND));
    stubCard("champion", "Champion", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("champion", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Champion cards cannot be played from hand.");
  }

  @Test
  void baseUnitMovingToEmptyBattlefieldUpdatesController() {
    LiveGameState state = state(card("unit", "p1", ZoneName.BASE));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit"));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getBattlefieldController()).containsEntry("BATTLEFIELD", "p1");
    assertThat(state.getActiveShowdown()).isNull();
  }

  @Test
  void movingIntoContestedBattlefieldStartsShowdown() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE),
        card("defender", "p2", ZoneName.BATTLEFIELD));
    stubCard("attacker", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("attacker");
  }

  @Test
  void cannotMoveOpponentUnitToBattlefield() {
    LiveGameState state = state(card("enemy", "p2", ZoneName.BASE));
    stubCard("enemy", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You do not own that card.");
  }

  @Test
  void repositionOnlyChangesCoordinatesWithinSameZone() {
    CardInstance unit = card("unit", "p1", ZoneName.BASE);
    unit.setX(10);
    unit.setY(20);
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new RepositionCardMove("p1", "unit", 55, 66));

    assertThat(unit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(unit.getX()).isEqualTo(55);
    assertThat(unit.getY()).isEqualTo(66);
  }

  @Test
  void hiddenHandCardsCannotBeRepositioned() {
    CardInstance unit = card("unit", "p1", ZoneName.HAND);
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new RepositionCardMove("p1", "unit", 55, 66)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card cannot be repositioned.");

    assertThat(unit.getZone()).isEqualTo(ZoneName.HAND);
  }

  private LiveGameState state(CardInstance... cards) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    p1.setName("Player One");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    p2.setName("Player Two");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private PlayCardMove play(String instanceId, ZoneName targetZone) {
    return new PlayCardMove("p1", instanceId, targetZone, 0, 0, null, false, List.of(), List.of());
  }

  private PlayCardMove playTarget(String instanceId, String targetInstanceId) {
    return new PlayCardMove("p1", instanceId, ZoneName.BASE, 0, 0, targetInstanceId, false, List.of(), List.of());
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(1);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private void stubCard(String id, String type, int cost) {
    stubCard(id, id, type, cost, 1, 1, null);
  }

  private void stubCard(String id, String name, String type, int cost, int power, int health, String rulesText) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, name, type, null, List.of(), cost, 0, null, null, null, rulesText, power, health, List.of()));
  }
}
