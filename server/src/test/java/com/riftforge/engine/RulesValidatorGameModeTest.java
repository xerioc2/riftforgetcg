package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.GameMode;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.AdjustScoreMove;
import com.riftforge.model.move.DealCardMove;
import com.riftforge.model.move.FlipCardMove;
import com.riftforge.model.move.MoveCardMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.TapCardMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RulesValidatorGameModeTest {
  @Mock CardDataService cardDataService;
  RulesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RulesValidator(cardDataService);
  }

  @Test
  void enforcedGameRejectsDealCard() {
    assertThatThrownBy(() -> validator.validate(state(GameMode.ENFORCED), new DealCardMove("p1", "unit", "HAND", 0, 0)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Deal Card is only available in sandbox games.");
  }

  @Test
  void enforcedGameRejectsAdjustScore() {
    assertThatThrownBy(() -> validator.validate(state(GameMode.ENFORCED), new AdjustScoreMove("p1", "p1", 1)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Adjust Score is only available in sandbox games.");
  }

  @Test
  void enforcedGameRejectsTapCard() {
    assertThatThrownBy(() -> validator.validate(state(GameMode.ENFORCED), new TapCardMove("p1", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Tap Card is only available in sandbox games.");
  }

  @Test
  void enforcedGameRejectsFlipCard() {
    assertThatThrownBy(() -> validator.validate(state(GameMode.ENFORCED), new FlipCardMove("p1", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Flip Card is only available in sandbox games.");
  }

  @Test
  void enforcedGameRejectsMoveCard() {
    assertThatThrownBy(() -> validator.validate(state(GameMode.ENFORCED), new MoveCardMove("p1", "unit", ZoneName.DISCARD, 0, 0)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Move Card is only available in sandbox games.");
  }

  @Test
  void sandboxGameAllowsDealCard() {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state(GameMode.SANDBOX), new DealCardMove("p1", "unit", "HAND", 0, 0)));
  }

  @Test
  void sandboxGameAllowsAdjustScore() {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state(GameMode.SANDBOX), new AdjustScoreMove("p1", "p1", 1)));
  }

  @Test
  void sandboxGameAllowsTapCard() {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state(GameMode.SANDBOX), new TapCardMove("p1", "unit")));
  }

  @Test
  void sandboxGameAllowsFlipCard() {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state(GameMode.SANDBOX), new FlipCardMove("p1", "unit")));
  }

  @Test
  void sandboxGameAllowsMoveCard() {
    assertThatNoException().isThrownBy(() ->
        validator.validate(state(GameMode.SANDBOX), new MoveCardMove("p1", "unit", ZoneName.DISCARD, 0, 0)));
  }

  @Test
  void normalEnforcedMoveStillPasses() {
    LiveGameState state = state(GameMode.ENFORCED);
    state.getCards().add(card("unit", "p1", ZoneName.BASE));
    stubCard("unit", "Unit");

    assertThatNoException().isThrownBy(() ->
        validator.validate(state, new MoveToBattlefieldMove("p1", "unit")));
  }

  private LiveGameState state(GameMode gameMode) {
    PlayerState player = new PlayerState();
    player.setUserId("p1");
    LiveGameState state = new LiveGameState();
    state.setGameMode(gameMode);
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(player)));
    state.setCards(new ArrayList<>());
    return state;
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setTapped(false);
    return card;
  }

  private void stubCard(String id, String type) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, id, type, null, List.of(), 0, 0, null, null, null, null, 1, 1, List.of()));
  }
}
