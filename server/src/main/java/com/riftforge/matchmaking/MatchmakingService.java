package com.riftforge.matchmaking;

import com.riftforge.model.RoomState;
import com.riftforge.service.GameService;
import com.riftforge.service.RoomService;
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
  private final SimpMessagingTemplate messaging;

  public MatchmakingService(RoomService roomService, @Lazy GameService gameService, SimpMessagingTemplate messaging) {
    this.roomService = roomService;
    this.gameService = gameService;
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
        Map.of(p1.playerId(), p1.deckCardIds(), p2.playerId(), p2.deckCardIds())
    );

    MatchNotification notification = new MatchNotification(room.getCode());
    notifyPlayer(p1.playerId(), notification);
    notifyPlayer(p2.playerId(), notification);
  }

  private void notifyPlayer(String playerId, MatchNotification notification) {
    messaging.convertAndSendToUser(playerId, "/queue/matchmaking", notification);
    messaging.convertAndSend("/topic/matchmaking/" + playerId, notification);
  }
}
