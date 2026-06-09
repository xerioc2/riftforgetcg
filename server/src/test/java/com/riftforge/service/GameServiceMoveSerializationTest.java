package com.riftforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.riftforge.engine.GameEngine;
import com.riftforge.model.LiveGameState;
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
  ExecutorService executor;
  GameService gameService;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService());
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
}
