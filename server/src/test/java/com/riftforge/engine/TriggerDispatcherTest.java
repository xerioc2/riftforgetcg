package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriggerDispatcherTest {
  @Test
  void dispatchesSupportedHandlersInDeterministicClassNameOrder() {
    List<String> calls = new ArrayList<>();
    TriggerDispatcher dispatcher = new TriggerDispatcher(List.of(
        new ZHandler(calls),
        new AHandler(calls),
        new IgnoredHandler(calls)));
    LiveGameState state = new LiveGameState();
    CardInstance card = new CardInstance();
    card.setInstanceId("unit");
    card.setOwnerId("p1");

    dispatcher.dispatch(state, TriggerEvent.cardMoved(card, ZoneName.BASE, ZoneName.BATTLEFIELD, "BATTLEFIELD", "TEST"));

    assertThat(calls).containsExactly("A", "Z");
  }

  @Test
  void cardMovedEventCarriesMovementContext() {
    CardInstance card = new CardInstance();
    card.setInstanceId("unit");
    card.setOwnerId("p1");

    TriggerEvent event = TriggerEvent.cardMoved(card, ZoneName.BASE, ZoneName.BATTLEFIELD, "BATTLEFIELD", "MOVE_TO_BATTLEFIELD");

    assertThat(event.type()).isEqualTo(TriggerType.CARD_MOVED);
    assertThat(event.sourceCard()).isSameAs(card);
    assertThat(event.controllerId()).isEqualTo("p1");
    assertThat(event.oldZone()).isEqualTo(ZoneName.BASE);
    assertThat(event.newZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(event.locationKey()).isEqualTo("BATTLEFIELD");
    assertThat(event.cause()).isEqualTo("MOVE_TO_BATTLEFIELD");
  }

  private static class AHandler implements TriggerHandler {
    private final List<String> calls;

    AHandler(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public boolean supports(TriggerEvent event) {
      return true;
    }

    @Override
    public void handle(LiveGameState state, TriggerEvent event) {
      calls.add("A");
    }
  }

  private static class ZHandler implements TriggerHandler {
    private final List<String> calls;

    ZHandler(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public boolean supports(TriggerEvent event) {
      return true;
    }

    @Override
    public void handle(LiveGameState state, TriggerEvent event) {
      calls.add("Z");
    }
  }

  private static class IgnoredHandler implements TriggerHandler {
    private final List<String> calls;

    IgnoredHandler(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public boolean supports(TriggerEvent event) {
      return false;
    }

    @Override
    public void handle(LiveGameState state, TriggerEvent event) {
      calls.add("ignored");
    }
  }
}
