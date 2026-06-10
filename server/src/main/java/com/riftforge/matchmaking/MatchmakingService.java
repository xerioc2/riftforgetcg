package com.riftforge.matchmaking;

import com.riftforge.model.RoomState;
import com.riftforge.service.GameService;
import com.riftforge.service.RoomService;
import com.riftforge.service.RoomTokenService;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchmakingService {
  private final Queue<MatchmakingEntry> queue = new ConcurrentLinkedQueue<>();
  private final Set<String> queued = ConcurrentHashMap.newKeySet();
  private final RoomService roomService;
  private final GameService gameService;
  private final RoomTokenService roomTokenService;
  private final SimpMessagingTemplate messaging;

  public MatchmakingService(RoomService roomService, @Lazy GameService gameService, RoomTokenService roomTokenService, SimpMessagingTemplate messaging) {
    this.roomService = roomService;
    this.gameService = gameService;
    this.roomTokenService = roomTokenService;
    this.messaging = messaging;
  }

  public synchronized void enqueue(MatchmakingEntry entry) {
    if (!queued.add(entry.playerId())) return;
    queue.offer(entry);
    tryMatch();
  }

  public synchronized void dequeue(String playerId) {
    queue.removeIf(entry -> entry.playerId().equals(playerId));
    queued.remove(playerId);
  }

  public int queueSize() {
    return queue.size();
  }

  private void tryMatch() {
    if (queue.size() < 2) return;
    MatchmakingEntry p1 = queue.poll();
    MatchmakingEntry p2 = queue.poll();
    if (p1 == null || p2 == null) return;
    queued.remove(p1.playerId());
    queued.remove(p2.playerId());

    RoomState room = roomService.create(p1.playerId(), p1.playerName(), false);
    roomService.join(room.getCode(), p2.playerId(), p2.playerName());
    roomService.ready(room.getCode(), p1.playerId(), p1.deckCardIds());
    roomService.ready(room.getCode(), p2.playerId(), p2.deckCardIds());
    roomService.start(room.getCode(), p1.playerId());
    gameService.initGame(
        room.getCode().toUpperCase(),
        List.of(p1.playerId(), p2.playerId()),
        Map.of(p1.playerId(), p1.deckCardIds(), p2.playerId(), p2.deckCardIds()),
        Map.of(p1.playerId(), p1.playerName(), p2.playerId(), p2.playerName()),
        room.getGameMode()
    );

    String p1Token = roomTokenService.issue(room.getCode(), p1.playerId(), "PLAYER");
    String p2Token = roomTokenService.issue(room.getCode(), p2.playerId(), "PLAYER");
    notifyPlayer(p1.playerId(), new MatchNotification(room.getCode(), p1Token));
    notifyPlayer(p2.playerId(), new MatchNotification(room.getCode(), p2Token));
  }

  private void notifyPlayer(String playerId, MatchNotification notification) {
    messaging.convertAndSendToUser(playerId, "/queue/matchmaking", notification);
    messaging.convertAndSend("/topic/matchmaking/" + playerId, notification);
  }
}
