package com.riftforge.service;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
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
    view.setChainState(copyChain(state.getChainState(), viewerPlayerId));
    view.setGameMode(state.getGameMode());
    view.setPendingChoice(copyChoiceForViewer(state.getPendingChoice(), viewerPlayerId));
    view.setLegalActions(viewerPlayerId == null ? Set.of() : legalActionsService.legalActionsFor(state, viewerPlayerId));
    boolean battlefieldsPublic = state.getCurrentPhase() != Phase.SELECT_BATTLEFIELD;
    view.setPlayers(state.getPlayers().stream().map(player -> copyPlayer(player, viewerPlayerId, battlefieldsPublic)).toList());
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
    if ((card.getZone() == ZoneName.HAND || card.getZone() == ZoneName.HIDDEN)
        && !Objects.equals(card.getOwnerId(), viewerPlayerId)) {
      copy.setCardId(HIDDEN_CARD_ID);
      copy.setCurrentHealth(0);
      copy.setFaceDown(true);
    }
    if (card.isFaceDown() && !Objects.equals(card.getOwnerId(), viewerPlayerId)) {
      copy.setBattlefieldLocationId(null);
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

  private PlayerState copyPlayer(PlayerState player, String viewerPlayerId, boolean battlefieldsPublic) {
    PlayerState copy = new PlayerState();
    boolean isViewer = Objects.equals(player.getUserId(), viewerPlayerId);
    copy.setUserId(player.getUserId());
    copy.setName(player.getName());
    copy.setScore(player.getScore());
    copy.setAvailableEnergy(player.getAvailableEnergy());
    copy.setRunePoolRemaining(player.getRunePoolRemaining());
    copy.setDeckPool(player.getDeckPool() == null ? new ArrayList<>() : new ArrayList<>(player.getDeckPool()));
    copy.setSelectedBattlefields(new ArrayList<>());
    copy.setBattlefieldChoices(isViewer
        ? new ArrayList<>(player.getSelectedBattlefields())
        : new ArrayList<>());
    copy.setSelectedBattlefieldId(isViewer || battlefieldsPublic ? player.getSelectedBattlefieldId() : null);
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
        showdown.step(),
        new ArrayList<>(showdown.relevantPlayerIds()),
        showdown.focusedPlayerId(),
        showdown.consecutivePasses(),
        showdown.readyToResolve(),
        showdown.assigningPlayerId(),
        new ArrayList<>(showdown.attackerAssignments()),
        new ArrayList<>(showdown.defenderAssignments()),
        showdown.locationId());
  }

  private LiveGameState.ChainState copyChain(LiveGameState.ChainState chain, String viewerPlayerId) {
    if (chain == null) return null;
    return new LiveGameState.ChainState(
        chain.chainId(),
        chain.chainItems().stream().map(item -> copyChainItem(item, viewerPlayerId)).toList(),
        new ArrayList<>(chain.relevantPlayerIds()),
        chain.focusedPlayerId(),
        chain.consecutivePasses(),
        chain.readyToResolveTop(),
        chain.sourceContext());
  }

  private LiveGameState.ChainItem copyChainItem(LiveGameState.ChainItem item, String viewerPlayerId) {
    boolean canSeePrivateSource = item.isPubliclyVisible() || Objects.equals(item.controllerPlayerId(), viewerPlayerId);
    return new LiveGameState.ChainItem(
        item.itemId(),
        item.controllerPlayerId(),
        canSeePrivateSource ? item.sourceCardInstanceId() : null,
        canSeePrivateSource ? item.sourceCardId() : HIDDEN_CARD_ID,
        canSeePrivateSource ? item.sourceCardName() : "Hidden chain item",
        canSeePrivateSource ? item.effectKey() : null,
        copyChainTargetIds(item, canSeePrivateSource, Objects.equals(item.controllerPlayerId(), viewerPlayerId)),
        item.order(),
        item.publicDescription(),
        item.visibility(),
        item.status(),
        canSeePrivateSource && item.counterable(),
        canSeePrivateSource && item.targetableOnChain(),
        canSeePrivateSource ? item.chainItemType() : "MASKED",
        canSeePrivateSource ? item.sourceZoneBeforeChain() : null,
        item.chainTargets().stream()
            .map(target -> copyChainTarget(target, canSeePrivateSource, Objects.equals(item.controllerPlayerId(), viewerPlayerId)))
            .toList());
  }

  private LiveGameState.ChainTarget copyChainTarget(LiveGameState.ChainTarget target, boolean canSeePrivateSource, boolean isController) {
    boolean canSeeTarget = canSeePrivateSource && (target.publicSafe() || isController);
    return new LiveGameState.ChainTarget(
        target.role(),
        canSeeTarget ? target.targetInstanceId() : null,
        canSeeTarget ? target.targetChainItemId() : null,
        canSeeTarget ? target.targetControllerPlayerId() : null,
        canSeeTarget ? target.targetKind() : "MASKED",
        canSeeTarget ? target.targetZone() : null,
        canSeeTarget ? target.publicLabel() : "Hidden target",
        canSeeTarget);
  }

  private ArrayList<String> copyChainTargetIds(LiveGameState.ChainItem item, boolean canSeePrivateSource, boolean isController) {
    if (!canSeePrivateSource) return new ArrayList<>();
    if (item.chainTargets().isEmpty()) return new ArrayList<>(item.targetInstanceIds());
    return item.chainTargets().stream()
        .filter(target -> target.publicSafe() || isController)
        .map(target -> target.targetInstanceId() != null ? target.targetInstanceId() : target.targetChainItemId())
        .filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }

  private PendingChoice copyChoiceForViewer(PendingChoice choice, String viewerPlayerId) {
    if (choice == null) return null;
    if (!choice.isPublicChoice() && !Objects.equals(choice.getPlayerId(), viewerPlayerId)) return null;
    PendingChoice copy = new PendingChoice();
    copy.setChoiceId(choice.getChoiceId());
    copy.setPlayerId(choice.getPlayerId());
    copy.setType(choice.getType());
    copy.setPrompt(choice.getPrompt());
    copy.setOptions(new ArrayList<>(choice.getOptions()));
    boolean isOwner = Objects.equals(choice.getPlayerId(), viewerPlayerId);
    copy.setCardOptions(isOwner ? new ArrayList<>(choice.getCardOptions()) : new ArrayList<>());
    copy.setAssignments(isOwner ? new ArrayList<>(choice.getAssignments()) : new ArrayList<>());
    copy.setSourceCardInstanceId(choice.getSourceCardInstanceId());
    copy.setSourceCardId(choice.getSourceCardId());
    copy.setPublicChoice(choice.isPublicChoice());
    copy.setRequiredSelections(choice.getRequiredSelections());
    copy.setPaymentAmount(choice.getPaymentAmount());
    copy.setEffect(choice.getEffect());
    copy.setAllowPartialResolve(choice.isAllowPartialResolve());
    copy.setContext(new HashMap<>(choice.getContext()));
    return copy;
  }
}
