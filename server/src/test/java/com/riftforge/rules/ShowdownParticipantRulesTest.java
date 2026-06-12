package com.riftforge.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShowdownParticipantRulesTest {
  private final ShowdownParticipantRules rules = new ShowdownParticipantRules();

  @Test
  void attackerIsParticipantAndCanResolve() {
    LiveGameState state = showdownState();

    assertThat(rules.isShowdownParticipant(state, "p1")).isTrue();
    assertThat(rules.isShowdownAttacker(state, "p1")).isTrue();
    assertThat(rules.isShowdownDefender(state, "p1")).isFalse();
  }

  @Test
  void battlefieldOpponentIsParticipantButNotAttacker() {
    LiveGameState state = showdownState(card("defender", "p2", ZoneName.BATTLEFIELD));

    assertThat(rules.isShowdownParticipant(state, "p2")).isTrue();
    assertThat(rules.isShowdownAttacker(state, "p2")).isFalse();
    assertThat(rules.isShowdownDefender(state, "p2")).isTrue();
  }

  @Test
  void nonParticipantIsNotIncluded() {
    LiveGameState state = showdownState(card("hand-card", "p2", ZoneName.HAND));

    assertThat(rules.isShowdownParticipant(state, "p2")).isFalse();
    assertThat(rules.isShowdownAttacker(state, "p2")).isFalse();
    assertThat(rules.isShowdownDefender(state, "p2")).isFalse();
  }

  @Test
  void noActiveShowdownHasNoParticipants() {
    LiveGameState state = new LiveGameState();
    state.setCards(new ArrayList<>(List.of(card("unit", "p1", ZoneName.BATTLEFIELD))));

    assertThat(rules.isShowdownParticipant(state, "p1")).isFalse();
    assertThat(rules.isShowdownAttacker(state, "p1")).isFalse();
    assertThat(rules.isShowdownDefender(state, "p1")).isFalse();
  }

  private LiveGameState showdownState(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    return state;
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    return card;
  }
}
