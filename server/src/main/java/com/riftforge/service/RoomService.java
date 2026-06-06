package com.riftforge.service;

import static com.riftforge.bot.BotConstants.BOT_ID;
import static com.riftforge.bot.BotConstants.BOT_NAME;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LobbyPlayer;
import com.riftforge.model.RoomState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    String code = generateCode();
    RoomState room = new RoomState();
    room.setCode(code);
    room.setHostId(hostId);
    room.setBotEnabled(withBot);
    room.getPlayers().add(new LobbyPlayer(hostId, hostName, false, List.of()));
    if (withBot) room.getPlayers().add(new LobbyPlayer(BOT_ID, BOT_NAME, true, generateBotDeck()));
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
    room.getPlayers().stream()
        .filter(p -> p.getId().equals(playerId))
        .findFirst()
        .ifPresent(p -> {
          p.setReady(!p.isReady());
          p.setDeckCardIds(deckCardIds);
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

  private List<String> generateBotDeck() {
    Set<String> playable = Set.of("unit", "spell", "gear");
    List<CardDefinition> all = new ArrayList<>(cardDataService.getAll().values());
    List<String> deck = new ArrayList<>();
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
    return deck.stream().limit(20).toList();
  }
}
