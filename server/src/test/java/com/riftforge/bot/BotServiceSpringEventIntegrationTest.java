package com.riftforge.bot;

import static com.riftforge.bot.BotConstants.BOT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "riftforge.riftcodex-api=http://127.0.0.1:9/cards-unreachable-in-tests",
    "spring.main.allow-circular-references=false"
})
class BotServiceSpringEventIntegrationTest {
  private static final String HUMAN_ID = "human-event-test";

  @Autowired GameService gameService;
  @MockBean CardDataService cardDataService;

  private final Map<String, CardDefinition> cards = new LinkedHashMap<>();

  @BeforeEach
  void setUp() {
    cards.clear();
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> cards.get(invocation.getArgument(0)));
    add("legend", "Legend", 0);
    add("champion", "Champion", 0);
    for (int i = 0; i < 10; i++) add("unit-" + i, "Unit", 99);
  }

  @Test
  void finalMulliganEventAdvancesRiftBotFromTurnOneAwaken() throws Exception {
    String roomCode = "EVT" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    gameService.initGame(
        roomCode,
        List.of(BOT_ID, HUMAN_ID),
        Map.of(BOT_ID, playtestDeck(), HUMAN_ID, playtestDeck()),
        Map.of(BOT_ID, "RiftBot", HUMAN_ID, "Human"));
    LiveGameState state = gameService.currentState(roomCode);
    state.setFirstPlayerId(BOT_ID);
    state.setActivePlayerId(BOT_ID);
    state.setCurrentPhase(Phase.MULLIGAN);
    state.setTurnNumber(1);
    state.getMulligansDone().clear();

    gameService.processMove(roomCode, new MulliganMove(BOT_ID, List.of()));
    gameService.processMove(roomCode, new MulliganMove(HUMAN_ID, List.of()));

    LiveGameState afterMulligans = gameService.currentState(roomCode);
    assertThat(afterMulligans.getCurrentPhase()).isEqualTo(Phase.AWAKEN);
    assertThat(afterMulligans.getActivePlayerId()).isEqualTo(BOT_ID);

    LiveGameState latest = waitUntilBotLeavesAwaken(roomCode);
    latest.setWinnerId("test-complete");
    assertThat(latest.getCurrentPhase()).isIn(Phase.BEGINNING, Phase.CHANNEL, Phase.DRAW, Phase.MAIN, Phase.END);
  }

  private LiveGameState waitUntilBotLeavesAwaken(String roomCode) throws Exception {
    long deadline = System.currentTimeMillis() + 5_000;
    LiveGameState latest = gameService.currentState(roomCode);
    while (System.currentTimeMillis() < deadline) {
      latest = gameService.currentState(roomCode);
      if (latest != null
          && !(latest.getCurrentPhase() == Phase.AWAKEN && BOT_ID.equals(latest.getActivePlayerId()))) {
        return latest;
      }
      Thread.sleep(25);
    }
    return latest;
  }

  private void add(String id, String type, int cost) {
    cards.put(id, new CardDefinition(id, id, type, null, List.of(), cost, 0, null, null, null, null, 1, 1, List.of()));
  }

  private List<String> playtestDeck() {
    List<String> deck = new ArrayList<>();
    deck.add("legend");
    deck.add("champion");
    for (int i = 0; i < 10; i++) {
      deck.add("unit-" + i);
      deck.add("unit-" + i);
    }
    return deck;
  }
}
