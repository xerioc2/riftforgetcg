package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riftforge.engine.GameEngine;
import com.riftforge.model.CompletedMatchSnapshot;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.MatchRecord;
import com.riftforge.model.Phase;
import com.riftforge.model.move.PassPhaseMove;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class GameServiceMoveSerializationTest {
  @Mock GameEngine engine;
  @Mock CardDataService cardDataService;
  @Mock SimpMessagingTemplate messaging;
  @Mock ApplicationEventPublisher eventPublisher;
  @Mock MatchHistoryService matchHistoryService;
  GameStateProjectionService projectionService;
  ExecutorService executor;
  GameService gameService;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
    projectionService = new GameStateProjectionService();
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, matchHistoryService, projectionService);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void movesForSameRoomAreProcessedSequentially() throws Exception {
    gameService.initGame("ROOM", List.of("p1"), Map.of("p1", List.of()), Map.of("p1", "Player One"));
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maxActive = new AtomicInteger();
    when(engine.applyMove(any(LiveGameState.class), any())).thenAnswer(invocation -> {
      int current = active.incrementAndGet();
      maxActive.accumulateAndGet(current, Math::max);
      Thread.sleep(100);
      active.decrementAndGet();
      return invocation.getArgument(0);
    });

    Future<?> first = executor.submit(() -> gameService.processMove("ROOM", new PassPhaseMove("p1")));
    Future<?> second = executor.submit(() -> gameService.processMove("ROOM", new PassPhaseMove("p1")));

    first.get(2, TimeUnit.SECONDS);
    second.get(2, TimeUnit.SECONDS);
    assertThat(maxActive.get()).isEqualTo(1);
  }

  @Test
  void movesForDifferentRoomsDoNotShareOneGlobalLock() throws Exception {
    gameService.initGame("ONE", List.of("p1"), Map.of("p1", List.of()), Map.of("p1", "Player One"));
    gameService.initGame("TWO", List.of("p2"), Map.of("p2", List.of()), Map.of("p2", "Player Two"));
    CountDownLatch bothEntered = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maxActive = new AtomicInteger();
    when(engine.applyMove(any(LiveGameState.class), any())).thenAnswer(invocation -> {
      int current = active.incrementAndGet();
      maxActive.accumulateAndGet(current, Math::max);
      bothEntered.countDown();
      release.await(2, TimeUnit.SECONDS);
      active.decrementAndGet();
      return invocation.getArgument(0);
    });

    Future<?> first = executor.submit(() -> gameService.processMove("ONE", new PassPhaseMove("p1")));
    Future<?> second = executor.submit(() -> gameService.processMove("TWO", new PassPhaseMove("p2")));

    assertThat(bothEntered.await(1, TimeUnit.SECONDS)).isTrue();
    release.countDown();
    first.get(2, TimeUnit.SECONDS);
    second.get(2, TimeUnit.SECONDS);
    assertThat(maxActive.get()).isEqualTo(2);
  }

  @Test
  void completedMatchHistoryReceivesSnapshotNotLiveState() {
    gameService.initGame("ROOM", List.of("p1"), Map.of("p1", List.of()), Map.of("p1", "Player One"));
    when(engine.applyMove(any(LiveGameState.class), any())).thenAnswer(invocation -> {
      LiveGameState state = invocation.getArgument(0);
      state.setWinnerId("p1");
      state.setTurnNumber(7);
      state.getPlayers().getFirst().setScore(8);
      return state;
    });

    gameService.processMove("ROOM", new PassPhaseMove("p1"));
    gameService.currentState("ROOM").getPlayers().getFirst().setScore(99);

    ArgumentCaptor<CompletedMatchSnapshot> captor = ArgumentCaptor.forClass(CompletedMatchSnapshot.class);
    verify(matchHistoryService).record(captor.capture());
    CompletedMatchSnapshot snapshot = captor.getValue();
    assertThat(snapshot.turnCount()).isEqualTo(7);
    assertThat(snapshot.winnerId()).isEqualTo("p1");
    assertThat(snapshot.players())
        .extracting(MatchRecord.PlayerSummary::score)
        .containsExactly(8);
  }

  @Test
  void resetWaitsForInFlightMoveAndIsNotOverwritten() throws Exception {
    gameService.initGame("ROOM", List.of("p1"), Map.of("p1", List.of()), Map.of("p1", "Player One"));
    CountDownLatch moveEntered = new CountDownLatch(1);
    CountDownLatch releaseMove = new CountDownLatch(1);
    when(engine.applyMove(any(LiveGameState.class), any())).thenAnswer(invocation -> {
      moveEntered.countDown();
      releaseMove.await(2, TimeUnit.SECONDS);
      LiveGameState state = invocation.getArgument(0);
      state.setWinnerId("move-wrote-after-reset");
      return state;
    });

    Future<?> move = executor.submit(() -> gameService.processMove("ROOM", new PassPhaseMove("p1")));
    assertThat(moveEntered.await(1, TimeUnit.SECONDS)).isTrue();

    Future<LiveGameState> reset = executor.submit(() ->
        gameService.reset("ROOM", List.of("p1"), Map.of("p1", List.of()), Map.of("p1", "Player One")));
    Thread.sleep(150);
    assertThat(reset.isDone()).isFalse();

    releaseMove.countDown();
    move.get(2, TimeUnit.SECONDS);
    LiveGameState resetState = reset.get(2, TimeUnit.SECONDS);

    assertThat(resetState.getCurrentPhase()).isEqualTo(Phase.MULLIGAN);
    assertThat(resetState.getWinnerId()).isNull();
    assertThat(gameService.currentState("ROOM").getWinnerId()).isNull();
  }
}
