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
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PlayCardMove;
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
class GameEnginePaymentTest {
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
  }

  @Test
  void canPaySimpleEnergyCost() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND), rune("rune-1", "calm-rune", "p1"));
    stubCard("unit", "Unit", 1, 0, List.of("CALM"));
    stubCard("calm-rune", "Rune", 0, 0, List.of("CALM"));

    engine.applyMove(state, play("unit", List.of("rune-1"), List.of()));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getRunes().getFirst().isTapped()).isTrue();
    assertThat(player(state).getAvailableEnergy()).isZero();
  }

  @Test
  void cannotPlayWithoutEnoughEnergy() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND), rune("rune-1", "calm-rune", "p1"));
    stubCard("unit", "Unit", 2, 0, List.of("CALM"));
    stubCard("calm-rune", "Rune", 0, 0, List.of("CALM"));

    assertThatThrownBy(() -> engine.applyMove(state, play("unit", List.of("rune-1"), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient energy.");

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.HAND);
    assertThat(state.getRunes().getFirst().isTapped()).isFalse();
  }

  @Test
  void canPayDomainPremiumCost() {
    LiveGameState state = state(
        card("spell", "p1", ZoneName.HAND),
        rune("energy-rune", "calm-rune", "p1"),
        rune("premium-rune", "calm-rune", "p1"));
    stubCard("spell", "Spell", 1, 1, List.of("CALM"));
    stubCard("calm-rune", "Rune", 0, 0, List.of("CALM"));

    engine.applyMove(state, play("spell", List.of("energy-rune"), List.of("premium-rune")));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getRunes()).extracting(RuneState::getInstanceId).containsExactly("energy-rune");
    assertThat(state.getRunes().getFirst().isTapped()).isTrue();
    assertThat(player(state).getRuneDeckPool()).containsExactly("calm-rune");
  }

  @Test
  void cannotPayPremiumWithWrongDomain() {
    LiveGameState state = state(
        card("spell", "p1", ZoneName.HAND),
        rune("energy-rune", "calm-rune", "p1"),
        rune("premium-rune", "chaos-rune", "p1"));
    stubCard("spell", "Spell", 1, 1, List.of("CALM"));
    stubCard("calm-rune", "Rune", 0, 0, List.of("CALM"));
    stubCard("chaos-rune", "Rune", 0, 0, List.of("CHAOS"));

    assertThatThrownBy(() -> engine.applyMove(state, play("spell", List.of("energy-rune"), List.of("premium-rune"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Premium payment uses the wrong rune domain.");

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.HAND);
    assertThat(state.getRunes()).hasSize(2);
    assertThat(state.getRunes()).allMatch(rune -> !rune.isTapped());
  }

  @Test
  void opponentCannotPayWithYourRunes() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND), rune("rune-1", "calm-rune", "p2"));
    stubCard("unit", "Unit", 1, 0, List.of("CALM"));
    stubCard("calm-rune", "Rune", 0, 0, List.of("CALM"));

    assertThatThrownBy(() -> engine.applyMove(state, play("unit", List.of("rune-1"), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You cannot pay with an opponent's rune.");
  }

  private LiveGameState state(CardInstance card, RuneState... runes) {
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
    state.setCards(new ArrayList<>(List.of(card)));
    state.setRunes(new ArrayList<>(List.of(runes)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private PlayerState player(LiveGameState state) {
    return state.getPlayers().stream().filter(candidate -> "p1".equals(candidate.getUserId())).findFirst().orElseThrow();
  }

  private PlayCardMove play(String instanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    return new PlayCardMove("p1", instanceId, ZoneName.BASE, 0, 0, null, false, paymentRuneIds, premiumRuneIds);
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

  private RuneState rune(String id, String cardId, String ownerId) {
    RuneState rune = new RuneState();
    rune.setInstanceId(id);
    rune.setCardId(cardId);
    rune.setOwnerId(ownerId);
    rune.setNormalEnergy(1);
    rune.setPremiumEnergy(2);
    return rune;
  }

  private void stubCard(String id, String type, int cost, int premiumCost, List<String> domains) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, type, null, domains, cost, premiumCost, null, null, null, null, 1, 1, List.of()));
  }
}
