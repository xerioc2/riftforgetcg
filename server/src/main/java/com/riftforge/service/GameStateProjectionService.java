package com.riftforge.service;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RevealedHandSnapshot;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.LegalActionsService;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GameStateProjectionService {
  public static final String HIDDEN_CARD_ID = "hidden";
  private final LegalActionsService legalActionsService;

  public GameStateProjectionService() {
    this(new LegalActionsService());
  }

  public GameStateProjectionService(LegalActionsService legalActionsService) {
    this.legalActionsService = legalActionsService;
  }

  public LiveGameState toPublicView(LiveGameState state, String viewerPlayerId) {
    LiveGameState view = new LiveGameState();
    view.setRoomCode(state.getRoomCode());
    view.setCurrentPhase(state.getCurrentPhase());
    view.setActivePlayerId(state.getActivePlayerId());
    view.setFirstPlayerId(state.getFirstPlayerId());
    view.setTurnNumber(state.getTurnNumber());
    view.setUpdatedAt(state.getUpdatedAt());
    view.setWinnerId(state.getWinnerId());
    view.setMulligansDone(new HashSet<>(state.getMulligansDone()));
    view.setCardPlayedThisTurn(state.isCardPlayedThisTurn());
    view.setBattlefieldController(new HashMap<>(state.getBattlefieldController()));
    view.setScoredBattlefieldsThisTurn(new HashSet<>(state.getScoredBattlefieldsThisTurn()));
    view.setActiveShowdown(copyShowdown(state.getActiveShowdown()));
    view.setGameMode(state.getGameMode());
    view.setLegalActions(viewerPlayerId == null ? Set.of() : legalActionsService.legalActionsFor(state, viewerPlayerId));
    view.setPlayers(state.getPlayers().stream().map(this::copyPlayer).toList());
    view.setRunes(state.getRunes().stream().map(this::copyRune).toList());
    view.setRevealedHands(state.getRevealedHands().stream()
        .filter(snapshot -> viewerPlayerId != null && viewerPlayerId.equals(snapshot.getRevealedToPlayerId()))
        .map(this::copyRevealedHand)
        .toList());
    view.setCards(state.getCards().stream()
        .map(card -> copyCardForViewer(card, viewerPlayerId))
        .toList());
    view.setLog(state.getLog().stream()
        .filter(entry -> canSeeLogEntry(entry, viewerPlayerId))
        .toList());
    return view;
  }

  private CardInstance copyCardForViewer(CardInstance card, String viewerPlayerId) {
    CardInstance copy = new CardInstance(card);
    if (card.getZone() == ZoneName.HAND && !Objects.equals(card.getOwnerId(), viewerPlayerId)) {
      copy.setCardId(HIDDEN_CARD_ID);
      copy.setCurrentHealth(0);
    }
    return copy;
  }

  private boolean canSeeLogEntry(LiveGameState.LogEntry entry, String viewerPlayerId) {
    if (!isPrivateVisionLog(entry.text())) return true;
    return entry.userId() != null && entry.userId().equals(viewerPlayerId);
  }

  private boolean isPrivateVisionLog(String text) {
    return text != null && (text.startsWith("VISION_PEEK|") || text.startsWith("VISION_RESOLVED|"));
  }

  private PlayerState copyPlayer(PlayerState player) {
    PlayerState copy = new PlayerState();
    copy.setUserId(player.getUserId());
    copy.setName(player.getName());
    copy.setScore(player.getScore());
    copy.setAvailableEnergy(player.getAvailableEnergy());
    copy.setRunePoolRemaining(player.getRunePoolRemaining());
    copy.setDeckPool(player.getDeckPool() == null ? new ArrayList<>() : new ArrayList<>(player.getDeckPool()));
    return copy;
  }

  private RuneState copyRune(RuneState rune) {
    RuneState copy = new RuneState();
    copy.setInstanceId(rune.getInstanceId());
    copy.setCardId(rune.getCardId());
    copy.setOwnerId(rune.getOwnerId());
    copy.setTapped(rune.isTapped());
    copy.setNormalEnergy(rune.getNormalEnergy());
    copy.setPremiumEnergy(rune.getPremiumEnergy());
    return copy;
  }

  private RevealedHandSnapshot copyRevealedHand(RevealedHandSnapshot snapshot) {
    RevealedHandSnapshot copy = new RevealedHandSnapshot();
    copy.setRevealedToPlayerId(snapshot.getRevealedToPlayerId());
    copy.setRevealedOwnerId(snapshot.getRevealedOwnerId());
    copy.setInstanceIds(new ArrayList<>(snapshot.getInstanceIds()));
    copy.setDismissedInstanceIds(new HashSet<>(snapshot.getDismissedInstanceIds()));
    return copy;
  }

  private LiveGameState.ShowdownState copyShowdown(LiveGameState.ShowdownState showdown) {
    if (showdown == null) return null;
    return new LiveGameState.ShowdownState(
        showdown.attackingPlayerId(),
        new ArrayList<>(showdown.attackerInstanceIds()),
        new HashMap<>(showdown.gankingBonuses()),
        showdown.step());
  }
}
