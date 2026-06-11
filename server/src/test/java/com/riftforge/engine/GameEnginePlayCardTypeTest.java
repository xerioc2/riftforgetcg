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
import com.riftforge.service.CardDataService;
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
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService);
    engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenReturn(false);
    when(cardDataService.isUnsupportedAction(anyString())).thenReturn(false);
    when(cardDataService.requiresBattlefieldTarget(anyString())).thenReturn(false);
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
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, type, null, List.of(), cost, 0, null, null, null, null, 1, 1, List.of()));
  }
}
