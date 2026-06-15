package com.riftforge.bot;

import static com.riftforge.bot.BotConstants.ALL_BOT_IDS;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.MulliganMove;
import com.riftforge.model.move.MoveRequest;
import com.riftforge.model.move.ResolveChoiceMove;
import com.riftforge.model.move.ResolveChainTopMove;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.model.move.SelectBattlefieldMove;
import com.riftforge.model.move.TapRuneMove;
import com.riftforge.rules.LegalAction;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BotService {
  private static final Logger log = LoggerFactory.getLogger(BotService.class);
  private final Set<String> actingRooms = ConcurrentHashMap.newKeySet();
  private final GameService gameService;
  private final CardDataService cardDataService;
  private final LegalActionsService legalActionsService;

  public BotService(@Lazy GameService gameService, CardDataService cardDataService, LegalActionsService legalActionsService) {
    this.gameService = gameService;
    this.cardDataService = cardDataService;
    this.legalActionsService = legalActionsService;
  }

  @PostConstruct
  void logStartup() {
    log.info("BotService active. Bot IDs={}", ALL_BOT_IDS);
  }

  @Scheduled(fixedDelay = 1_000, initialDelay = 1_000)
  void recoverMissedBotTurns() {
    for (LiveGameState state : gameService.activeStates()) {
      if (state == null || state.getWinnerId() != null) continue;
      String roomCode = normalizeRoomCode(state.getRoomCode());
      String actingBotId = actingBotId(state);
      if (actingBotId == null || actingRooms.contains(roomCode)) continue;
      log.debug(
          "Bot sweep found pending bot action: room={}, phase={}, activePlayer={}, actingBotId={}",
          roomCode,
          state.getCurrentPhase(),
          state.getActivePlayerId(),
          actingBotId);
      onStateChanged(new GameStateChangedEvent(this, roomCode, state));
    }
  }

  @EventListener
  public void onStateChanged(GameStateChangedEvent event) {
    String roomCode = normalizeRoomCode(event.getRoomCode());
    LiveGameState state = event.getState();
    if (state.getWinnerId() != null) {
      log.debug("Bot event ignored for finished game: rawRoom={}, room={}, winner={}", event.getRoomCode(), roomCode, state.getWinnerId());
      return;
    }

    List<String> playerIds = state.getPlayers().stream()
        .map(PlayerState::getUserId)
        .toList();
    boolean anyBotInGame = playerIds.stream().anyMatch(this::isBotId);

    log.debug(
        "BotService received GameStateChangedEvent: room={}, phase={}, activePlayer={}, turnNumber={}, players={}, anyBotInGame={}",
        roomCode,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getTurnNumber(),
        playerIds,
        anyBotInGame);

    String actingBotId = actingBotId(state);
    boolean alreadyActing = actingRooms.contains(roomCode);
    log.debug(
        "BotService bot selection: rawRoom={}, room={}, botIds={}, actingBotId={}, actingRoomsContainsRoom={}, actingRoomsBeforeAdd={}",
        event.getRoomCode(),
        roomCode,
        ALL_BOT_IDS,
        actingBotId,
        alreadyActing,
        actingRooms);
    if (!anyBotInGame) return;
    if (actingBotId == null) return;
    boolean addedActingRoom = actingRooms.add(roomCode);
    log.debug("BotService acting-room guard: room={}, actingBotId={}, added={}, activeRooms={}", roomCode, actingBotId, addedActingRoom, actingRooms);
    if (!addedActingRoom) return;

    boolean isBotVsBot = state.getPlayers().stream()
        .allMatch(p -> isBotId(p.getUserId()));
    long delay = isBotVsBot ? 350L : 700L;
    final String botId = actingBotId;
    AtomicBoolean acted = new AtomicBoolean(false);
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(delay);
        LiveGameState current = gameService.currentState(roomCode);
        Set<LegalAction> currentLegalActions = current == null ? Set.of() : legalActions(current, botId);
        log.debug(
            "Bot async started: room={}, bot={}, currentState={}, phase={}, activePlayer={}, legalActions={}",
            roomCode,
            botId,
            current == null ? "null" : "present",
            current == null ? null : current.getCurrentPhase(),
            current == null ? null : current.getActivePlayerId(),
            currentLegalActions);
        if (current == null) {
          log.warn("Bot async current state missing: room={}, bot={}", roomCode, botId);
          return;
        }
        if (current.getWinnerId() != null) return;
        log.debug(
            "Bot acting: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}",
            roomCode,
            botId,
            current.getCurrentPhase(),
            current.getActivePlayerId(),
            current.getActiveShowdown() != null,
            current.getGameMode());
        acted.set(doActiveTurn(roomCode, current, botId));
      } catch (Exception e) {
        // A bot failure must never interrupt the game server.
        LiveGameState current = gameService.currentState(roomCode);
        log.warn(
            "Bot action failed: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}",
            roomCode,
            botId,
            current == null ? null : current.getCurrentPhase(),
            current == null ? null : current.getActivePlayerId(),
            current != null && current.getActiveShowdown() != null,
            current == null ? null : current.getGameMode(),
            e);
      } finally {
        actingRooms.remove(roomCode);
        LiveGameState latest = gameService.currentState(roomCode);
        String nextBotId = latest == null ? null : actingBotId(latest);
        if (latest != null && nextBotId != null && (acted.get() || hasRelevantLegalAction(latest, nextBotId))) {
          onStateChanged(new GameStateChangedEvent(this, roomCode, latest));
        }
      }
    });
  }

  private boolean doActiveTurn(String roomCode, LiveGameState state, String botId) {
    Set<LegalAction> legalActions = legalActions(state, botId);
    log.debug(
        "Bot legal actions: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, actions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        legalActions);
    if (legalActions.isEmpty()) {
      logNoLegalAction(roomCode, state, botId, legalActions);
      return false;
    }
    if (state.getPendingChoice() != null) {
      return resolvePendingChoiceIfPossible(roomCode, state, botId, legalActions);
    }
    if (state.getChainState() != null) {
      if (legalActions.contains(LegalAction.RESOLVE_CHAIN_TOP)) {
        return processBotMove(roomCode, state, botId, new ResolveChainTopMove(botId));
      } else if (legalActions.contains(LegalAction.PASS_CHAIN_FOCUS)) {
        return processBotMove(roomCode, state, botId, new PassChainFocusMove(botId));
      } else {
        log.debug("Bot waiting during chain: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
      }
      return false;
    }
    if (state.getActiveShowdown() != null) {
      if (legalActions.contains(LegalAction.ASSIGN_COMBAT_DAMAGE)) {
        return processBotMove(roomCode, state, botId, new AssignCombatDamageMove(botId, botDamageAssignments(state, botId)));
      } else if (legalActions.contains(LegalAction.RESOLVE_SHOWDOWN)) {
        return processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
      } else if (legalActions.contains(LegalAction.PASS_SHOWDOWN_FOCUS)) {
        return processBotMove(roomCode, state, botId, new PassShowdownFocusMove(botId));
      } else {
        log.debug("Bot waiting during showdown: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
      }
      return false;
    }
    return switch (state.getCurrentPhase()) {
      case SELECT_BATTLEFIELD -> {
        if (legalActions.contains(LegalAction.SELECT_BATTLEFIELD)) {
          yield doSelectBattlefield(roomCode, state, botId);
        } else {
          log.warn("Bot cannot choose Battlefield because no selection action is legal: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
          yield false;
        }
      }
      case MULLIGAN -> {
        if (legalActions.contains(LegalAction.MULLIGAN) || legalActions.contains(LegalAction.KEEP_HAND)) {
          yield doMulligan(roomCode, state, botId);
        } else {
          log.warn("Bot cannot mulligan because no mulligan action is legal: room={}, bot={}, legalActions={}", roomCode, botId, legalActions);
          yield false;
        }
      }
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END -> passIfLegal(roomCode, state, botId, legalActions);
      case MAIN -> doMain(roomCode, state, botId);
    };
  }

  private boolean doSelectBattlefield(String roomCode, LiveGameState state, String botId) {
    return state.getPlayers().stream()
        .filter(player -> botId.equals(player.getUserId()))
        .findFirst()
        .flatMap(player -> player.getSelectedBattlefields().stream().findFirst())
        .map(battlefieldId -> processBotMove(roomCode, state, botId, new SelectBattlefieldMove(botId, battlefieldId)))
        .orElseGet(() -> {
          log.warn("Bot has no Battlefield choice available: room={}, bot={}", roomCode, botId);
          return false;
        });
  }

  private boolean doMulligan(String roomCode, LiveGameState state, String botId) {
    return processBotMove(roomCode, state, botId, new MulliganMove(botId, List.of()));
  }

  private boolean resolvePendingChoiceIfPossible(String roomCode, LiveGameState state, String botId, Set<LegalAction> legalActions) {
    PendingChoice choice = state.getPendingChoice();
    if (choice == null) return false;
    if (!botId.equals(choice.getPlayerId())) return false;
    if (!legalActions.contains(LegalAction.RESOLVE_CHOICE)) {
      log.warn(
          "Bot owns pending choice but RESOLVE_CHOICE is not legal: room={}, bot={}, choiceType={}, legalActions={}",
          roomCode,
          botId,
          choice.getType(),
          legalActions);
      return false;
    }
    return botChoiceMove(botId, choice)
        .map(move -> processBotMove(roomCode, state, botId, move))
        .orElseGet(() -> {
          log.warn("Bot cannot resolve unsupported pending choice: room={}, bot={}, choiceType={}", roomCode, botId, choice.getType());
          return false;
        });
  }

  private Optional<ResolveChoiceMove> botChoiceMove(String botId, PendingChoice choice) {
    if (PendingChoice.TYPE_TOP_DECK_PICK_ONE.equals(choice.getType())) {
      return choice.getCardOptions().stream()
          .findFirst()
          .map(option -> new ResolveChoiceMove(
              botId,
              choice.getChoiceId(),
              null,
              option.optionId(),
              PendingChoice.ACTION_HAND,
              List.of()));
    }
    if (PendingChoice.TYPE_PREDICT_ORDER.equals(choice.getType())) {
      List<PendingChoice.CardChoiceAssignment> assignments = new java.util.ArrayList<>();
      for (int i = 0; i < choice.getCardOptions().size(); i++) {
        assignments.add(new PendingChoice.CardChoiceAssignment(
            choice.getCardOptions().get(i).optionId(),
            PendingChoice.ACTION_TOP,
            i));
      }
      return Optional.of(new ResolveChoiceMove(botId, choice.getChoiceId(), null, null, null, assignments));
    }
    if (choice.getOptions().stream().anyMatch(option -> PendingChoice.OPTION_DECLINE.equals(option.id()))) {
      return Optional.of(new ResolveChoiceMove(botId, choice.getChoiceId(), PendingChoice.OPTION_DECLINE));
    }
    if (choice.getOptions().stream().anyMatch(option -> PendingChoice.OPTION_NO.equals(option.id()))) {
      return Optional.of(new ResolveChoiceMove(botId, choice.getChoiceId(), PendingChoice.OPTION_NO));
    }
    if (choice.getOptions().stream().anyMatch(option -> PendingChoice.OPTION_YES.equals(option.id()))) {
      return Optional.of(new ResolveChoiceMove(botId, choice.getChoiceId(), PendingChoice.OPTION_YES));
    }
    return Optional.empty();
  }

  private boolean doMain(String roomCode, LiveGameState state, String botId) {
    boolean acted = false;
    if (legalActions(state, botId).contains(LegalAction.MOVE_TO_BATTLEFIELD)) {
      int availableEnergy = botEnergy(state, botId);
      List<CardInstance> readyChampions = state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.CHAMPION && !c.isTapped())
          .filter(c -> {
            CardDefinition def = cardDataService.getCard(c.getCardId());
            return def != null && def.cost() <= availableEnergy;
          })
          .toList();
      for (CardInstance champion : readyChampions) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)) break;
        acted |= processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, champion.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        state = resolveShowdownIfLegal(roomCode, state, botId);
        if (state == null) return acted;
      }

      List<CardInstance> readyBaseCards = state.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.BASE && !c.isTapped())
          .toList();
      for (CardInstance card : readyBaseCards) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)) break;
        acted |= processBotMove(roomCode, state, botId, new MoveToBattlefieldMove(botId, card.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        state = resolveShowdownIfLegal(roomCode, state, botId);
        if (state == null) return acted;
      }
    }
    if (state == null) return acted;

    if (legalActions(state, botId).contains(LegalAction.TAP_RUNE)) {
      List<RuneState> untappedRunes = state.getRunes().stream()
          .filter(r -> botId.equals(r.getOwnerId()) && !r.isTapped())
          .toList();
      for (RuneState rune : untappedRunes) {
        Set<LegalAction> actions = legalActions(state, botId);
        if (!actions.contains(LegalAction.TAP_RUNE)) break;
        acted |= processBotMove(roomCode, state, botId, new TapRuneMove(botId, rune.getInstanceId()));
        sleepBriefly(200);
        state = gameService.currentState(roomCode);
        if (state == null) return acted;
      }
    }

    LiveGameState playableState = gameService.currentState(roomCode);
    Set<LegalAction> playableActions = legalActions(playableState, botId);
    if (!playableActions.contains(LegalAction.PLAY_CARD)) {
      return passIfLegal(roomCode, playableState, botId, playableActions) || acted;
    }
    boolean anyPlayable = playableState != null && playableState.getCards().stream()
        .anyMatch(c -> botId.equals(c.getOwnerId())
            && c.getZone() == ZoneName.HAND
            && cardDataService.getCard(c.getCardId()).cost() <= botEnergy(playableState, botId));
    if (!anyPlayable) {
      return passIfLegal(roomCode, playableState, botId, playableActions) || acted;
    }

    boolean played = true;
    while (played) {
      played = false;
      LiveGameState current = gameService.currentState(roomCode);
      if (current == null || current.getWinnerId() != null || current.getCurrentPhase() != Phase.MAIN) break;
      if (!legalActions(current, botId).contains(LegalAction.PLAY_CARD)) break;
      int energy = botEnergy(current, botId);
      Optional<CardInstance> pick = current.getCards().stream()
          .filter(c -> botId.equals(c.getOwnerId()) && c.getZone() == ZoneName.HAND)
          .filter(c -> hasValidSpellTarget(current, c, botId))
          .filter(c -> cardDataService.getCard(c.getCardId()).cost() <= energy)
          .max(Comparator.comparingInt(c -> cardDataService.getCard(c.getCardId()).cost()));

      if (pick.isPresent()) {
        CardDefinition pickedDef = cardDataService.getCard(pick.get().getCardId());
        String targetInstanceId = targetForCard(current, pickedDef, botId)
            .map(CardInstance::getInstanceId)
            .orElse(null);
        List<PlayCardMove.TargetSelection> targets = targetsForCard(current, pickedDef, botId);
        int boardCount = (int) current.getCards().stream()
            .filter(c -> botId.equals(c.getOwnerId()) && (c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD))
            .count();
        acted |= processBotMove(roomCode, current, botId, new PlayCardMove(botId, pick.get().getInstanceId(), ZoneName.BASE, 60 + boardCount * 100, 220, targetInstanceId, targets, false, List.of(), List.of()));
        sleepBriefly(300);
        played = true;
      }
    }
    LiveGameState latest = gameService.currentState(roomCode);
    return passIfLegal(roomCode, latest, botId, legalActions(latest, botId)) || acted;
  }

  private LiveGameState resolveShowdownIfLegal(String roomCode, LiveGameState state, String botId) {
    if (state == null || state.getActiveShowdown() == null) return state;
    Set<LegalAction> actions = legalActions(state, botId);
    if (!actions.contains(LegalAction.RESOLVE_SHOWDOWN)) {
      log.debug("Bot cannot resolve showdown yet: room={}, bot={}, legalActions={}", roomCode, botId, actions);
      return state;
    }
    processBotMove(roomCode, state, botId, new ResolveShowdownMove(botId));
    sleepBriefly(200);
    return gameService.currentState(roomCode);
  }

  private boolean passIfLegal(String roomCode, LiveGameState state, String botId, Set<LegalAction> legalActions) {
    if (state == null) return false;
    if (legalActions.contains(LegalAction.PASS_PHASE) || legalActions.contains(LegalAction.END_TURN)) {
      return processBotMove(roomCode, state, botId, new PassPhaseMove(botId));
    }
    log.warn(
        "Bot cannot pass because PASS_PHASE is not legal: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}, legalActions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        state.getGameMode(),
        legalActions);
    return false;
  }

  private boolean processBotMove(String roomCode, LiveGameState state, String botId, MoveRequest move) {
    Phase beforePhase = state == null ? null : state.getCurrentPhase();
    String beforeActivePlayer = state == null ? null : state.getActivePlayerId();
    log.debug(
        "Bot move before: room={}, bot={}, move={}, phaseBefore={}, activePlayerBefore={}, activeShowdown={}, gameMode={}",
        roomCode,
        botId,
        move.getClass().getSimpleName(),
        state == null ? null : state.getCurrentPhase(),
        state == null ? null : state.getActivePlayerId(),
        state != null && state.getActiveShowdown() != null,
        state == null ? null : state.getGameMode());
    gameService.processMove(roomCode, move);
    LiveGameState latest = gameService.currentState(roomCode);
    boolean phaseChanged = latest != null && beforePhase != latest.getCurrentPhase();
    boolean activePlayerChanged = latest != null && beforeActivePlayer != null && !beforeActivePlayer.equals(latest.getActivePlayerId());
    log.debug(
        "Bot move after: room={}, bot={}, move={}, latestPhase={}, latestActivePlayer={}, phaseChanged={}, activePlayerChanged={}, winner={}, activeShowdown={}",
        roomCode,
        botId,
        move.getClass().getSimpleName(),
        latest == null ? null : latest.getCurrentPhase(),
        latest == null ? null : latest.getActivePlayerId(),
        phaseChanged,
        activePlayerChanged,
        latest == null ? null : latest.getWinnerId(),
        latest != null && latest.getActiveShowdown() != null);
    if (move instanceof PassPhaseMove
        && latest != null
        && beforePhase == latest.getCurrentPhase()
        && botId.equals(latest.getActivePlayerId())
        && latest.getWinnerId() == null) {
      log.warn(
          "Bot PassPhaseMove did not advance phase: room={}, bot={}, phase={}, activePlayer={}",
          roomCode,
          botId,
          latest.getCurrentPhase(),
          latest.getActivePlayerId());
    }
    return true;
  }

  private boolean hasValidSpellTarget(LiveGameState state, CardInstance card, String botId) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (cardDataService.isUnsupportedAction(def.id())) return false;
    if (cardDataService.isEquip(def)) return true;
    if (cardDataService.requiresFriendlyAndEnemyTargets(def.id())) return targetsForCard(state, def, botId).size() == 2;
    if (!cardDataService.requiresBattlefieldTarget(def.id())) return true;
    return targetForCard(state, def, botId).isPresent();
  }

  private List<LiveGameState.CombatDamageAssignment> botDamageAssignments(LiveGameState state, String botId) {
    boolean attacking = state.getActiveShowdown() != null && botId.equals(state.getActiveShowdown().attackingPlayerId());
    List<CardInstance> sources = state.getCards().stream()
        .filter(card -> botId.equals(card.getOwnerId()) && card.getZone() == ZoneName.BATTLEFIELD)
        .filter(this::isCombatant)
        .toList();
    List<CardInstance> targets = state.getCards().stream()
        .filter(card -> !botId.equals(card.getOwnerId()) && card.getZone() == ZoneName.BATTLEFIELD)
        .filter(this::isCombatant)
        .sorted(Comparator
            .comparing((CardInstance card) -> !cardDataService.hasKeyword(card, "TANK"))
            .thenComparing(CardInstance::getInstanceId))
        .toList();
    if (sources.isEmpty() || targets.isEmpty()) return List.of();

    List<LiveGameState.CombatDamageAssignment> assignments = new java.util.ArrayList<>();
    int targetIndex = 0;
    int assignedToCurrentTarget = 0;
    for (CardInstance source : sources) {
      int remaining = botMight(source, attacking);
      while (remaining > 0) {
        CardInstance target = targets.get(Math.min(targetIndex, targets.size() - 1));
        int lethal = lethal(target);
        int needed = lethal - assignedToCurrentTarget;
        if (needed <= 0) {
          targetIndex = Math.min(targetIndex + 1, targets.size() - 1);
          assignedToCurrentTarget = 0;
          continue;
        }
        int amount = targetIndex == targets.size() - 1 ? remaining : Math.min(remaining, needed);
        assignments.add(new LiveGameState.CombatDamageAssignment(
            source.getInstanceId(),
            target.getInstanceId(),
            amount));
        remaining -= amount;
        assignedToCurrentTarget += amount;
        if (assignedToCurrentTarget >= lethal) {
          if (targetIndex < targets.size() - 1) {
            targetIndex++;
            assignedToCurrentTarget = 0;
          }
        }
      }
    }
    return assignments;
  }

  private int botMight(CardInstance card, boolean attacking) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (def == null) return 0;
    if (cardDataService.hasKeyword(card, "STUN") || cardDataService.hasKeyword(card, "STUNNED")) return 0;
    int situational = attacking
        ? cardDataService.getKeywordValue(card, "ASSAULT")
        : cardDataService.getKeywordValue(card, "SHIELD");
    return Math.max(0, def.power() + card.getMightBonus() + card.getTemporaryPowerModifier() + situational);
  }

  private int lethal(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    int health = card.getCurrentHealth() > 0 ? card.getCurrentHealth() : def == null ? 0 : def.health();
    return Math.max(1, health);
  }

  private boolean isCombatant(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
  }

  private List<PlayCardMove.TargetSelection> targetsForCard(LiveGameState state, CardDefinition def, String botId) {
    if (!cardDataService.requiresFriendlyAndEnemyTargets(def.id())) return List.of();
    Optional<CardInstance> friendly = publicUnitTarget(state, botId, true);
    Optional<CardInstance> enemy = publicUnitTarget(state, botId, false);
    if (friendly.isEmpty() || enemy.isEmpty()) return List.of();
    return List.of(
        new PlayCardMove.TargetSelection(PlayCardMove.TargetSelection.FRIENDLY_UNIT, friendly.get().getInstanceId()),
        new PlayCardMove.TargetSelection(PlayCardMove.TargetSelection.ENEMY_UNIT, enemy.get().getInstanceId()));
  }

  private Optional<CardInstance> targetForCard(LiveGameState state, CardDefinition def, String botId) {
    if (!cardDataService.requiresBattlefieldTarget(def.id())) return Optional.empty();
    if (cardDataService.isEquip(def)) return Optional.empty();
    if (cardDataService.requiresFriendlyAndEnemyTargets(def.id())) return Optional.empty();
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    boolean preferFriendly = cardDataService.requiresFriendlyTarget(def.id())
        || text.contains("give a unit")
        || text.contains("ready it");
    return publicUnitTarget(state, botId, preferFriendly);
  }

  private Optional<CardInstance> publicUnitTarget(LiveGameState state, String botId, boolean friendly) {
    return state.getCards().stream()
        .filter(candidate -> candidate.getZone() == ZoneName.BASE || candidate.getZone() == ZoneName.BATTLEFIELD)
        .filter(candidate -> !candidate.isFaceDown())
        .filter(candidate -> friendly == botId.equals(candidate.getOwnerId()))
        .filter(candidate -> {
          CardDefinition def = cardDataService.getCard(candidate.getCardId());
          return def != null && ("Unit".equalsIgnoreCase(def.type()) || "Champion".equalsIgnoreCase(def.type()));
        })
        .findFirst();
  }

  private int botEnergy(LiveGameState state, String botId) {
    return state.getPlayers().stream()
        .filter(p -> botId.equals(p.getUserId()))
        .findFirst()
        .map(PlayerState::getAvailableEnergy)
        .orElse(0);
  }

  private void sleepBriefly(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private Set<LegalAction> legalActions(LiveGameState state, String botId) {
    return state == null ? Set.of() : legalActionsService.legalActionsFor(state, botId);
  }

  private boolean hasRelevantLegalAction(LiveGameState state, String botId) {
    Set<LegalAction> actions = legalActions(state, botId);
    if (actions.isEmpty()) return false;
    if (state.getPendingChoice() != null) {
      return botId.equals(state.getPendingChoice().getPlayerId())
          && actions.contains(LegalAction.RESOLVE_CHOICE)
          && botChoiceMove(botId, state.getPendingChoice()).isPresent();
    }
    if (state.getChainState() != null) {
      return actions.contains(LegalAction.RESOLVE_CHAIN_TOP)
          || actions.contains(LegalAction.PASS_CHAIN_FOCUS);
    }
    if (state.getActiveShowdown() != null) {
      return actions.contains(LegalAction.RESOLVE_SHOWDOWN)
          || actions.contains(LegalAction.PASS_SHOWDOWN_FOCUS)
          || actions.contains(LegalAction.ASSIGN_COMBAT_DAMAGE)
          || actions.contains(LegalAction.PLAY_CARD);
    }
    return switch (state.getCurrentPhase()) {
      case SELECT_BATTLEFIELD -> actions.contains(LegalAction.SELECT_BATTLEFIELD);
      case MULLIGAN -> actions.contains(LegalAction.MULLIGAN) || actions.contains(LegalAction.KEEP_HAND);
      case AWAKEN, BEGINNING, CHANNEL, DRAW, END ->
          actions.contains(LegalAction.PASS_PHASE) || actions.contains(LegalAction.END_TURN);
      case MAIN -> actions.contains(LegalAction.PLAY_CARD)
          || actions.contains(LegalAction.MOVE_TO_BATTLEFIELD)
          || actions.contains(LegalAction.TAP_RUNE)
          || actions.contains(LegalAction.DISCARD_RUNE)
          || actions.contains(LegalAction.RESOLVE_SHOWDOWN)
          || actions.contains(LegalAction.PASS_PHASE)
          || actions.contains(LegalAction.END_TURN);
    };
  }

  private void logNoLegalAction(String roomCode, LiveGameState state, String botId, Set<LegalAction> legalActions) {
    log.warn(
        "Bot is active but has no legal actions: room={}, bot={}, phase={}, activePlayer={}, activeShowdown={}, gameMode={}, legalActions={}",
        roomCode,
        botId,
        state.getCurrentPhase(),
        state.getActivePlayerId(),
        state.getActiveShowdown() != null,
        state.getGameMode(),
        legalActions);
  }

  private boolean isBotId(String playerId) {
    return playerId != null && ALL_BOT_IDS.stream().anyMatch(botId -> botId.equalsIgnoreCase(playerId));
  }

  private String actingBotId(LiveGameState state) {
    if (state.getPendingChoice() != null && isBotId(state.getPendingChoice().getPlayerId())) {
      return state.getPendingChoice().getPlayerId();
    }
    if (state.getChainState() != null && isBotId(state.getChainState().focusedPlayerId())) {
      return state.getChainState().focusedPlayerId();
    }
    if (state.getActiveShowdown() != null) {
      if (isBotId(state.getActiveShowdown().assigningPlayerId())) return state.getActiveShowdown().assigningPlayerId();
      if (isBotId(state.getActiveShowdown().focusedPlayerId())) return state.getActiveShowdown().focusedPlayerId();
    }
    if (state.getCurrentPhase() == Phase.MULLIGAN) {
      return state.getPlayers().stream()
          .map(PlayerState::getUserId)
          .filter(this::isBotId)
          .filter(id -> !state.getMulligansDone().contains(id))
          .findFirst()
          .orElse(null);
    }
    if (state.getCurrentPhase() == Phase.SELECT_BATTLEFIELD) {
      return state.getPlayers().stream()
          .filter(player -> isBotId(player.getUserId()))
          .filter(player -> !player.getSelectedBattlefields().isEmpty())
          .filter(player -> player.getSelectedBattlefieldId() == null || player.getSelectedBattlefieldId().isBlank())
          .map(PlayerState::getUserId)
          .findFirst()
          .orElse(null);
    }
    if (isBotId(state.getActivePlayerId())) return state.getActivePlayerId();
    return null;
  }

  private String normalizeRoomCode(String roomCode) {
    return roomCode == null ? "" : roomCode.toUpperCase();
  }
}
