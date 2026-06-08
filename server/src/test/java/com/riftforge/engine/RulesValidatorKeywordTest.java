package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RulesValidatorKeywordTest {
  @Mock CardDataService cardDataService;
  RulesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RulesValidator(cardDataService);
  }

  @Test
  void readyBaseUnitCanMoveToBattlefieldDuringMain() {
    CardInstance card = card("unit", "p1", ZoneName.BASE, false);
    assertThatNoException().isThrownBy(() -> validator.validate(state(Phase.MAIN, card), new MoveToBattlefieldMove("p1", "unit")));
  }

  @Test
  void exhaustedBaseUnitCannotMoveToBattlefield() {
    CardInstance card = card("unit", "p1", ZoneName.BASE, true);
    assertThatThrownBy(() -> validator.validate(state(Phase.MAIN, card), new MoveToBattlefieldMove("p1", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("ready");
  }

  @Test
  void mulliganRejectsMoreThanTwoCards() {
    LiveGameState state = state(Phase.MULLIGAN,
        card("one", "p1", ZoneName.HAND, false),
        card("two", "p1", ZoneName.HAND, false),
        card("three", "p1", ZoneName.HAND, false));
    assertThatThrownBy(() -> validator.validate(state, new MulliganMove("p1", List.of("one", "two", "three"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("up to 2");
  }

  private CardInstance card(String id, String ownerId, ZoneName zone, boolean tapped) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setTapped(tapped);
    return card;
  }

  private LiveGameState state(Phase phase, CardInstance... cards) {
    PlayerState player = new PlayerState();
    player.setUserId("p1");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(phase);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(player)));
    state.setCards(new ArrayList<>(List.of(cards)));
    return state;
  }
}
