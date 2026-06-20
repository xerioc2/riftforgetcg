package com.riftforge.service;

import static com.riftforge.bot.BotConstants.BOT_ID;
import static com.riftforge.bot.BotConstants.BOT_NAME;
import static com.riftforge.bot.BotConstants.BOT2_ID;
import static com.riftforge.bot.BotConstants.BOT2_NAME;
import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import com.riftforge.model.CardSupportSummary;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
  private final ConcurrentHashMap<String, RoomState> rooms = new ConcurrentHashMap<>();
  private final SimpMessagingTemplate messaging;
  private final CardDataService cardDataService;
  private final CardSupportService cardSupportService;

  public RoomService(SimpMessagingTemplate messaging, CardDataService cardDataService) {
    this(messaging, cardDataService, null);
  }

  @Autowired
  public RoomService(SimpMessagingTemplate messaging, CardDataService cardDataService, CardSupportService cardSupportService) {
    this.messaging = messaging;
    this.cardDataService = cardDataService;
    this.cardSupportService = cardSupportService;
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
    return ready(code, playerId, deckCardIds, false);
  }

  public RoomState ready(String code, String playerId, List<String> deckCardIds, boolean supportedCardsOnly) {
    RoomState room = get(code);
    List<String> submittedDeck = deckCardIds == null ? List.of() : deckCardIds;
    room.getPlayers().stream()
        .filter(p -> p.getId().equals(playerId))
        .findFirst()
        .ifPresent(p -> {
          if (!p.isReady()) {
            DeckValidationResult result = validateDeck(submittedDeck, deckFormatFor(p.getId()), supportedCardsOnly);
            p.setDeckWarnings(result.warnings());
            p.setDeckSupport(result.support());
          } else {
            p.setDeckWarnings(List.of());
            p.setDeckSupport(List.of());
          }
          p.setReady(!p.isReady());
          p.setDeckCardIds(submittedDeck);
        });
    broadcast(code, room);
    return room;
  }

  private void validateDeck(List<String> cardIds, DeckFormat format) {
    validateDeck(cardIds, format, false);
  }

  private DeckValidationResult validateDeck(List<String> cardIds, DeckFormat format, boolean supportedCardsOnly) {
    List<CardDefinition> submittedCards = new ArrayList<>();
    for (String cardId : cardIds) {
      CardDefinition def = resolveCard(cardId);
      if (def == null) throw new IllegalArgumentException("Unknown card ID: " + cardId);
      if (format == DeckFormat.FULL_CONSTRUCTED && TournamentLegality.isBannedInConstructed(def)) {
        throw new IllegalArgumentException(def.name() + " is banned in Constructed.");
      }
      submittedCards.add(def);
    }

    long legendCount = submittedCards.stream()
        .filter(card -> isType(card, "Legend"))
        .count();
    int legendIndex = firstIndexOfType(submittedCards, "Legend");
    if (legendCount == 0) throw new IllegalArgumentException("Deck must include a Legend card.");
    if (format == DeckFormat.FULL_CONSTRUCTED && legendCount != 1) {
      throw new IllegalArgumentException("Deck must include exactly 1 Legend card.");
    }

    int chosenChampionIndex = firstIndexOfType(submittedCards, "Champion");
    if (format == DeckFormat.FULL_CONSTRUCTED && chosenChampionIndex != legendIndex + 1) {
      throw new IllegalArgumentException("Deck must include exactly 1 Chosen Champion card.");
    }

    long mainDeckCount = 0;
    for (int i = 0; i < submittedCards.size(); i++) {
      CardDefinition card = submittedCards.get(i);
      if (isType(card, "Rune") || isType(card, "Battlefield") || isType(card, "Legend")) continue;
      if (i == chosenChampionIndex) continue;
      mainDeckCount++;
    }
    if (format == DeckFormat.FULL_CONSTRUCTED && mainDeckCount != 39) {
      throw new IllegalArgumentException("Main Deck must contain exactly 39 cards. Current count: " + mainDeckCount + ".");
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
      if (isType(card, "Legend") || isType(card, "Rune") || isType(card, "Battlefield")) continue;
      int copies = copiesById.merge(card.id(), 1, Integer::sum);
      if (copies > 3) throw new IllegalArgumentException("Cannot include more than 3 copies of " + card.name() + ".");
    }

    List<CardSupportSummary> support = cardSupportService == null ? List.of() : cardSupportService.summarizeDeck(submittedCards);
    List<String> warnings = support.stream()
        .filter(summary -> summary.status() == CardSupportStatus.PARTIAL)
        .map(summary -> summary.name() + " is partially supported.")
        .toList();
    if (format == DeckFormat.FULL_CONSTRUCTED && supportedCardsOnly) {
      List<CardSupportSummary> blocked = support.stream()
          .filter(summary -> summary.status() == CardSupportStatus.UNSUPPORTED || summary.status() == CardSupportStatus.NOT_AUDITED || summary.status() == CardSupportStatus.BANNED)
          .toList();
      if (!blocked.isEmpty()) {
        String names = blocked.stream()
            .limit(5)
            .map(summary -> summary.name() + " (" + summary.status() + ")")
            .collect(Collectors.joining(", "));
        if (blocked.size() > 5) names += ", +" + (blocked.size() - 5) + " more";
        throw new IllegalArgumentException("Deck contains unsupported or not-audited cards: " + names + ".");
      }
    }
    return new DeckValidationResult(support, warnings);
  }

  private record DeckValidationResult(List<CardSupportSummary> support, List<String> warnings) {}

  private boolean isType(CardDefinition card, String type) {
    return card.type() != null && type.equalsIgnoreCase(card.type().trim());
  }

  private int firstIndexOfType(List<CardDefinition> cards, String type) {
    for (int i = 0; i < cards.size(); i++) {
      if (isType(cards.get(i), type)) return i;
    }
    return -1;
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
    List<String> uploadedIrelia = generateUploadedIreliaPlaytestDeck();
    if (!uploadedIrelia.isEmpty()) return uploadedIrelia;

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

  private List<String> generateUploadedIreliaPlaytestDeck() {
    Map<String, CardDefinition> byName = cardDataService.getAll().values().stream()
        .collect(Collectors.toMap(CardDefinition::name, card -> card, (first, ignored) -> first));
    List<String> requiredNames = List.of(
        "Irelia - Blade Dancer",
        "Irelia - Fervent",
        "Not So Fast",
        "Abandoned Hall",
        "Vex - Apathetic",
        "Scuttle Crab",
        "Back Off",
        "Sunken Temple",
        "Defiant Dance",
        "Edge of Night",
        "Boots of Swiftness",
        "Guardian Angel",
        "Stellacorn Herder",
        "Calm Rune",
        "Lonely Poro",
        "Flash",
        "Charm",
        "Defy",
        "En Garde",
        "Discipline",
        "Zhonya's Hourglass",
        "Chaos Rune",
        "Ride The Wind",
        "The Syren",
        "Mindsplitter",
        "Tideturner",
        "Targon's Peak",
        "Ride The Wind");
    if (requiredNames.stream().anyMatch(name -> !byName.containsKey(name))) return List.of();

    List<String> deck = new ArrayList<>();
    addCopies(deck, byName, "Irelia - Blade Dancer", 1);
    addCopies(deck, byName, "Irelia - Fervent", 1);
    addCopies(deck, byName, "Not So Fast", 1);
    addCopies(deck, byName, "Vex - Apathetic", 2);
    addCopies(deck, byName, "Scuttle Crab", 3);
    addCopies(deck, byName, "Back Off", 1);
    addCopies(deck, byName, "Defiant Dance", 3);
    addCopies(deck, byName, "Edge of Night", 1);
    addCopies(deck, byName, "Boots of Swiftness", 2);
    addCopies(deck, byName, "Guardian Angel", 2);
    addCopies(deck, byName, "Stellacorn Herder", 2);
    addCopies(deck, byName, "Lonely Poro", 3);
    addCopies(deck, byName, "Flash", 2);
    addCopies(deck, byName, "Charm", 3);
    addCopies(deck, byName, "Defy", 3);
    addCopies(deck, byName, "En Garde", 2);
    addCopies(deck, byName, "Discipline", 3);
    addCopies(deck, byName, "Zhonya's Hourglass", 1);
    addCopies(deck, byName, "Ride The Wind", 1);
    addCopies(deck, byName, "The Syren", 1);
    addCopies(deck, byName, "Mindsplitter", 2);
    addCopies(deck, byName, "Tideturner", 1);
    addCopies(deck, byName, "Calm Rune", 6);
    addCopies(deck, byName, "Chaos Rune", 6);
    addCopies(deck, byName, "Abandoned Hall", 1);
    addCopies(deck, byName, "Sunken Temple", 1);
    addCopies(deck, byName, "Targon's Peak", 1);
    return deck;
  }

  private void addCopies(List<String> deck, Map<String, CardDefinition> byName, String name, int quantity) {
    CardDefinition card = byName.get(name);
    for (int i = 0; i < quantity; i++) deck.add(card.id());
  }
}
