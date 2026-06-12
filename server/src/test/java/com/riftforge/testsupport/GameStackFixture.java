package com.riftforge.testsupport;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.engine.CardZoneService;
import com.riftforge.engine.CombatResolver;
import com.riftforge.engine.CombatStatsService;
import com.riftforge.engine.GameEngine;
import com.riftforge.engine.RulesValidator;
import com.riftforge.model.CardDefinition;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import com.riftforge.service.GameStateProjectionService;
import com.riftforge.service.MatchHistoryService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Wires the real rules/game/room stack against an in-memory card pool so
 * integration tests stay deterministic and need no external card API.
 * Only transport-level collaborators (messaging, events) and card data
 * loading are replaced.
 */
public final class GameStackFixture {
  public static final int TARGET_SCORE = 8;

  public final Map<String, CardDefinition> cards = new HashMap<>();
  public final CardDataService cardDataService = mock(CardDataService.class);
  public final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
  public final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
  public final RoomTokenService roomTokenService = new RoomTokenService();
  public final GameStateProjectionService projectionService = new GameStateProjectionService(new LegalActionsService());
  public final RoomService roomService;
  public final GameService gameService;

  public GameStackFixture() {
    when(cardDataService.getAll()).thenReturn(cards);
    when(cardDataService.getCard(anyString())).thenAnswer(invocation -> {
      String id = invocation.getArgument(0);
      CardDefinition def = cards.get(id);
      return def != null ? def
          : new CardDefinition(id, "Unknown Card", "Unknown", null, List.of(), 0, 0, null, null, null, "", 1, 1, List.of());
    });
    CardEffectRegistry effects = new CardEffectRegistry(cardDataService);
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService));
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    GameEngine engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, TARGET_SCORE);
    gameService = new GameService(engine, cardDataService, messaging, eventPublisher, new MatchHistoryService(), projectionService);
    roomService = new RoomService(messaging, cardDataService);
  }

  public void addCard(String id, String name, String type) {
    cards.put(id, new CardDefinition(id, name, type, null, List.of(), 0, 0, null, null, null, "", 1, 1, List.of()));
  }

  /**
   * Registers a tournament-shaped card pool (1 Legend, 1 Champion, 39 main
   * cards, 12 runes, 3 unique battlefields) and returns the matching
   * FULL_CONSTRUCTED deck list.
   */
  public List<String> registerConstructedDeck() {
    List<String> deck = new ArrayList<>();
    addCard("legend", "Test Legend", "Legend");
    addCard("champion", "Test Champion", "Champion");
    deck.add("legend");
    deck.add("champion");
    for (int i = 0; i < 39; i++) {
      addCard("unit-" + i, "Unit " + i, "Unit");
      deck.add("unit-" + i);
    }
    for (int i = 0; i < 12; i++) {
      addCard("rune-" + i, "Rune " + i, "Rune");
      deck.add("rune-" + i);
    }
    for (int i = 0; i < 3; i++) {
      addCard("battlefield-" + i, "Battlefield " + i, "Battlefield");
      deck.add("battlefield-" + i);
    }
    return deck;
  }
}
