package com.riftforge.service;

import static com.riftforge.bot.BotConstants.BOT_ID;
import static com.riftforge.bot.BotConstants.BOT_NAME;
import static com.riftforge.bot.BotConstants.BOT2_ID;
import static com.riftforge.bot.BotConstants.BOT2_NAME;
import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.DeckFormat;
import com.riftforge.model.GameMode;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import com.riftforge.rules.TournamentLegality;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
  private final ConcurrentHashMap<String, RoomState> rooms = new ConcurrentHashMap<>();
  private final SimpMessagingTemplate messaging;
  private final CardDataService cardDataService;

  public RoomService(SimpMessagingTemplate messaging, CardDataService cardDataService) {
    this.messaging = messaging;
    this.cardDataService = cardDataService;
  }

  public RoomState create(String hostId, String hostName, boolean withBot) {
    return create(hostId, hostName, withBot, GameMode.ENFORCED);
  }

  public RoomState create(String hostId, String hostName, boolean withBot, GameMode gameMode) {
    String code = generateCode();
    RoomState room = new RoomState();
    room.setCode(code);
    room.setHostId(hostId);
    room.setBotEnabled(withBot);
    room.setGameMode(gameMode);
    room.getPlayers().add(new LobbyPlayer(hostId, hostName, false, List.of()));
    if (withBot) room.getPlayers().add(new LobbyPlayer(BOT_ID, BOT_NAME, true, generateBotDeck()));
    rooms.put(code, room);
    return room;
  }

  public RoomState createBotVsBot() {
    String code = generateCode();
    RoomState room = new RoomState();
    room.setCode(code);
    room.setHostId(BOT_ID);
    room.setBotEnabled(true);
    room.setGameMode(GameMode.ENFORCED);
    room.getPlayers().add(new LobbyPlayer(BOT_ID, BOT_NAME, true, generateBotDeck()));
    room.getPlayers().add(new LobbyPlayer(BOT2_ID, BOT2_NAME, true, generateBotDeck()));
    room.setStatus("playing");
    rooms.put(code, room);
    return room;
  }

  public RoomState join(String code, String playerId, String playerName) {
    RoomState room = get(code);
    if (room.getStatus().equals("playing")) throw new IllegalStateException("Game already started.");
    boolean alreadyIn = room.getPlayers().stream().anyMatch(p -> p.getId().equals(playerId));
    if (!alreadyIn) {
      if (room.getPlayers().size() >= 4) throw new IllegalStateException("Room is full.");
      room.getPlayers().add(new LobbyPlayer(playerId, playerName, false, List.of()));
    }
    broadcast(code, room);
    return room;
  }

  public RoomState ready(String code, String playerId, List<String> deckCardIds) {
    RoomState room = get(code);
    List<String> submittedDeck = deckCardIds == null ? List.of() : deckCardIds;
    room.getPlayers().stream()
        .filter(p -> p.getId().equals(playerId))
        .findFirst()
        .ifPresent(p -> {
          if (!p.isReady()) validateDeck(submittedDeck, deckFormatFor(p.getId()));
          p.setReady(!p.isReady());
          p.setDeckCardIds(submittedDeck);
        });
    broadcast(code, room);
    return room;
  }

  private void validateDeck(List<String> cardIds, DeckFormat format) {
    List<CardDefinition> submittedCards = new ArrayList<>();
    for (String cardId : cardIds) {
      CardDefinition def = resolveCard(cardId);
      if (def == null) throw new IllegalArgumentException("Unknown card ID: " + cardId);
      if (format == DeckFormat.FULL_CONSTRUCTED && TournamentLegality.isBannedInConstructed(def)) {
        throw new IllegalArgumentException(def.name() + " is banned in Constructed.");
      }
      submittedCards.add(def);
    }

    boolean hasLegend = submittedCards.stream()
        .anyMatch(card -> "Legend".equalsIgnoreCase(card.type()));
    if (!hasLegend) throw new IllegalArgumentException("Deck must include a Legend card.");

    long championCount = submittedCards.stream()
        .filter(card -> isType(card, "Champion"))
        .count();
    if (format == DeckFormat.FULL_CONSTRUCTED && championCount != 1) {
      throw new IllegalArgumentException("Deck must include exactly 1 Champion card.");
    }

    long mainDeckCount = submittedCards.stream()
        .filter(card -> !isType(card, "Rune"))
        .filter(card -> !isType(card, "Battlefield"))
        .filter(card -> !isType(card, "Champion"))
        .filter(card -> !isType(card, "Legend"))
        .count();
    if (format == DeckFormat.FULL_CONSTRUCTED && mainDeckCount != 39) {
      throw new IllegalArgumentException("Main deck must contain exactly 39 cards (Champion is in addition).");
    }
    if (format == DeckFormat.PLAYTEST_BOT && mainDeckCount < 20) {
      throw new IllegalArgumentException("Main deck must have at least 20 cards.");
    }

    long runeCount = submittedCards.stream().filter(card -> isType(card, "Rune")).count();
    if (format == DeckFormat.FULL_CONSTRUCTED && runeCount != 12) {
      throw new IllegalArgumentException("Rune Pool must contain exactly 12 runes.");
    }

    List<CardDefinition> battlefields = submittedCards.stream()
        .filter(card -> isType(card, "Battlefield"))
        .toList();
    if (format == DeckFormat.FULL_CONSTRUCTED && battlefields.size() != 3) {
      throw new IllegalArgumentException("Choose exactly 3 Battlefields.");
    }
    if (battlefields.stream().map(CardDefinition::name).distinct().count() != battlefields.size()) {
      throw new IllegalArgumentException("Battlefields must have unique names.");
    }

    Map<String, Integer> copiesById = new HashMap<>();
    for (CardDefinition card : submittedCards) {
      if (isType(card, "Champion") || isType(card, "Legend") || isType(card, "Rune") || isType(card, "Battlefield")) continue;
      int copies = copiesById.merge(card.id(), 1, Integer::sum);
      if (copies > 3) throw new IllegalArgumentException("Cannot include more than 3 copies of " + card.name() + ".");
    }
  }

  private boolean isType(CardDefinition card, String type) {
    return type.equalsIgnoreCase(card.type());
  }

  private CardDefinition resolveCard(String cardId) {
    Map<String, CardDefinition> allCards = cardDataService.getAll();
    CardDefinition def = allCards == null ? null : allCards.get(cardId);
    if (def == null) def = cardDataService.getCard(cardId);
    if (def == null || "Unknown".equalsIgnoreCase(def.type())) return null;
    return def;
  }

  public RoomState setBotDeck(String code, List<String> deckCardIds) {
    RoomState room = get(code);
    room.getPlayers().stream()
        .filter(p -> BOT_ID.equals(p.getId()))
        .findFirst()
        .ifPresent(p -> {
          List<String> resolvedDeck = deckCardIds.isEmpty() ? generateBotDeck() : deckCardIds;
          validateDeck(resolvedDeck, DeckFormat.PLAYTEST_BOT);
          p.setDeckCardIds(resolvedDeck);
        });
    broadcast(code, room);
    return room;
  }

  public RoomState start(String code, String requesterId) {
    RoomState room = get(code);
    if (!room.getHostId().equals(requesterId)) throw new IllegalStateException("Only the host can start.");
    boolean humanReady = room.getPlayers().stream()
        .filter(p -> !BOT_ID.equals(p.getId()))
        .allMatch(LobbyPlayer::isReady);
    if (!humanReady) throw new IllegalStateException("Not all players are ready.");
    room.getPlayers().forEach(player -> validateDeck(player.getDeckCardIds(), deckFormatFor(player.getId())));
    room.setStatus("playing");
    broadcast(code, room);
    return room;
  }

  public RoomState get(String code) {
    RoomState room = rooms.get(code.toUpperCase());
    if (room == null) throw new IllegalStateException("Room not found: " + code);
    return room;
  }

  private void broadcast(String code, RoomState room) {
    messaging.convertAndSend("/topic/lobby/" + code.toUpperCase(), room);
  }

  private String generateCode() {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    Random rng = new Random();
    String code;
    do {
      code = IntStream.range(0, 4).mapToObj(i -> String.valueOf(chars.charAt(rng.nextInt(chars.length())))).collect(Collectors.joining());
    } while (rooms.containsKey(code));
    return code;
  }

  private DeckFormat deckFormatFor(String playerId) {
    return ALL_BOT_IDS.contains(playerId) ? DeckFormat.PLAYTEST_BOT : DeckFormat.FULL_CONSTRUCTED;
  }

  private List<String> generateBotDeck() {
    Set<String> playable = Set.of("unit", "spell", "gear");
    List<CardDefinition> all = new ArrayList<>(cardDataService.getAll().values());
    List<String> deck = new ArrayList<>();
    all.stream()
        .filter(card -> "Legend".equalsIgnoreCase(card.type()))
        .findFirst()
        .ifPresent(card -> deck.add(card.id()));
    all.stream()
        .filter(card -> "Champion".equalsIgnoreCase(card.type()))
        .findFirst()
        .ifPresent(card -> deck.add(card.id()));
    all.stream()
        .filter(card -> playable.contains(card.type().toLowerCase()))
        .sorted(Comparator.comparingInt(CardDefinition::cost).thenComparing(CardDefinition::name))
        .limit(10)
        .forEach(card -> {
          deck.add(card.id());
          deck.add(card.id());
        });
    return deck;
  }
}
