package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.*;
import com.riftforge.model.move.*;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(GameEngine.class);
  private static final int MAX_RUNES = 11;

  private final RulesValidator rulesValidator;
  private final CombatResolver combatResolver;
  private final CardZoneService cardZoneService;
  private final CardDataService cardDataService;
  private final CardEffectRegistry effects;
  private final DeathTriggerService deathTriggerService;
  private final TokenFactory tokenFactory;
  private final int targetScore;

  public GameEngine(RulesValidator rulesValidator, CombatResolver combatResolver, CardZoneService cardZoneService, CardDataService cardDataService, CardEffectRegistry effects, DeathTriggerService deathTriggerService, TokenFactory tokenFactory, @Value("${riftforge.target-score}") int targetScore) {
    this.rulesValidator = rulesValidator;
    this.combatResolver = combatResolver;
    this.cardZoneService = cardZoneService;
    this.cardDataService = cardDataService;
    this.effects = effects;
    this.deathTriggerService = deathTriggerService;
    this.tokenFactory = tokenFactory;
    this.targetScore = targetScore;
  }

  public LiveGameState applyMove(LiveGameState state, MoveRequest move) {
    rulesValidator.validate(state, move);
    LiveGameState next = switch (move) {
      case DealCardMove m -> applyDealCard(state, m);
      case TapCardMove m -> applyTapCard(state, m);
      case FlipCardMove m -> applyFlipCard(state, m);
      case PlayCardMove m -> applyPlayCard(state, m);
      case MoveCardMove m -> applyMoveCard(state, m);
      case RepositionCardMove m -> applyRepositionCard(state, m);
      case TapRuneMove m -> applyTapRune(state, m);
      case DiscardRuneMove m -> applyDiscardRune(state, m);
      case MoveToBattlefieldMove m -> applyMoveToBattlefield(state, m);
      case ResolveShowdownMove m -> applyResolveShowdown(state, m);
      case MulliganMove m -> applyMulligan(state, m);
      case UndoRunesMove m -> applyUndoRunes(state, m);
      case PassPhaseMove m -> applyPassPhase(state);
      case AdjustScoreMove m -> applyAdjustScore(state, m);
      case VisionChoiceMove m -> applyVisionChoice(state, m);
      case DismissRevealedMove m -> applyDismissRevealed(state, m);
      case HideCardMove m -> applyHideCard(state, m);
      case EquipGearMove m -> applyEquipGear(state, m);
      case ResolveChoiceMove m -> applyResolveChoice(state, m);
    };
    next.setUpdatedAt(Instant.now().toString());
    checkWinCondition(next);
    return next;
  }

  private LiveGameState applyDealCard(LiveGameState state, DealCardMove move) {
    CardDefinition def = cardDataService.getCard(move.cardId());
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance instance = new CardInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setCardId(move.cardId());
    instance.setOwnerId(move.playerId());
    instance.setZone(ZoneName.valueOf(move.targetZone().toUpperCase()));
    instance.setX(move.x());
    instance.setY(move.y());
    instance.setCurrentHealth(def.health());
    instance.setHasSummoningSickness(true);
    instance.setTempKeywords(new ArrayList<>());
    instance.setZIndex(maxZ + 1);
    state.getCards().add(instance);
    log(state, move.playerId(), "Drew " + def.name());
    return state;
  }

  private LiveGameState applyTapCard(LiveGameState state, TapCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    card.setTapped(!card.isTapped());
    String action = card.isTapped() ? "tapped" : "untapped";
    log(state, move.playerId(), "Player " + action + " " + cardDataService.getCard(card.getCardId()).name());
    return state;
  }

  private LiveGameState applyFlipCard(LiveGameState state, FlipCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    card.setFaceDown(!card.isFaceDown());
    log(state, move.playerId(), "Player flipped " + cardDataService.getCard(card.getCardId()).name());
    return state;
  }

  private LiveGameState applyPlayCard(LiveGameState state, PlayCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    CardDefinition def = cardDataService.getCard(card.getCardId());
    String cardTypeLower = normalizeCardType(def.type());
    boolean playedDuringShowdown = state.getActiveShowdown() != null;
    boolean cardPlayedEarlierThisTurn = state.isCardPlayedThisTurn();
    int paidCost = def.cost() + (move.accelerate() ? 1 : 0);
    applyPayment(state, move, paidCost);
    LOGGER.debug(
        "PLAY_CARD routing: player={}, cardId={}, cardName={}, cardType={}, fromZone={}, targetZone={}, x={}, y={}",
        move.playerId(),
        card.getCardId(),
        def.name(),
        def.type(),
        card.getZone(),
        move.targetZone(),
        move.x(),
        move.y());
    card.setZone(move.targetZone());
    card.setX(move.x());
    card.setY(move.y());
    card.setCurrentHealth(def.health());
    card.setAttachedToInstanceId(null);
    card.setHasSummoningSickness(!move.accelerate());
    if (move.targetZone() == ZoneName.BASE) card.setTapped(!move.accelerate());
    if (move.targetZone() == ZoneName.BATTLEFIELD && cardDataService.isAmbushCard(def)) {
      card.setTapped(false);
      card.setHasSummoningSickness(false);
    }
    applyLegion(state, card, cardPlayedEarlierThisTurn);
    applyPlayedCardTokenScripts(state, card, cardPlayedEarlierThisTurn);
    state.setCardPlayedThisTurn(true);
    CardInstance target = move.targetInstanceId() == null ? null : state.getCards().stream()
        .filter(candidate -> candidate.getInstanceId().equals(move.targetInstanceId()))
        .findFirst()
        .orElse(null);
    target = deflectTarget(card, target, state);
    CardInstance resolvedTarget = target;
    if (resolvedTarget != null) {
      log(state, move.playerId(), def.name() + " targeted " + cardDataService.getCard(resolvedTarget.getCardId()).name() + ".");
    }
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onPlay(card, resolvedTarget, state));
    if (move.accelerate()) log(state, move.playerId(), def.name() + " entered ready with Accelerate.");
    if (cardDataService.hasKeyword(card, "VISION")) {
      String topCardId = player(state, move.playerId()).getDeckPool().stream().findFirst().orElse("");
      String topCardName = topCardId.isBlank() ? "No card" : cardDataService.getCard(topCardId).name();
      log(state, move.playerId(), "VISION_PEEK|" + topCardId + "|" + topCardName);
    }
    applyRulesTextEffect(card, target, state, def);
    moveDestroyedBoardCards(state);
    if (cardTypeLower.equals("spell")) {
      cardZoneService.moveToGraveyard(card);
    }
    boolean ambushedToBattlefield = move.targetZone() == ZoneName.BATTLEFIELD && card.getZone() == ZoneName.BATTLEFIELD && cardDataService.isAmbushCard(def);
    if (ambushedToBattlefield) startAmbushBattlefield(state, card, def, move.playerId());
    if (cardDataService.hasKeyword(card, "REPEAT")) state.setCardPlayedThisTurn(false);
    if (!ambushedToBattlefield) {
      String message = cardDataService.isEquip(def)
          ? "Played " + def.name() + " to Base."
          : playedDuringShowdown ? "Played " + def.name() + " during the showdown." : "Played " + def.name();
      log(state, move.playerId(), message);
    }
    return state;
  }

  private LiveGameState applyEquipGear(LiveGameState state, EquipGearMove move) {
    CardInstance gear = findCard(state, move.gearInstanceId());
    CardDefinition gearDef = cardDataService.getCard(gear.getCardId());
    CardInstance target = findCard(state, move.targetInstanceId());
    attachGear(state, gear, gearDef, target, move.playerId());
    return state;
  }

  private void applyPayment(LiveGameState state, PlayCardMove move, int cost) {
    PlayerState player = player(state, move.playerId());
    int selectedEnergy = 0;
    for (String runeId : move.paymentRuneIds()) {
      RuneState rune = findRune(state, runeId);
      rune.setTapped(true);
      selectedEnergy += rune.getNormalEnergy();
    }
    for (String runeId : move.premiumRuneIds()) {
      RuneState rune = findRune(state, runeId);
      state.getRunes().remove(rune);
      if (rune.getCardId() != null && !rune.getCardId().isBlank()) {
        player.getRuneDeckPool().add(rune.getCardId());
        player.setRunePoolRemaining(player.getRuneDeckPool().size());
      }
    }
    player.setAvailableEnergy(Math.max(0, player.getAvailableEnergy() + selectedEnergy - cost));
  }

  private LiveGameState applyMoveCard(LiveGameState state, MoveCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    ZoneName sourceZone = card.getZone();
    if (move.targetZone() == ZoneName.DISCARD) cardZoneService.moveToGraveyard(card);
    else card.setZone(move.targetZone());
    card.setX(move.x());
    card.setY(move.y());
    if (move.targetZone() == ZoneName.BATTLEFIELD
        && (sourceZone == ZoneName.BASE || sourceZone == ZoneName.CHAMPION || sourceZone == ZoneName.LEGEND)) {
      card.setTapped(true);
      card.setHasSummoningSickness(false);
    }
    log(state, move.playerId(), "Moved " + cardDataService.getCard(card.getCardId()).name() + " to " + displayZone(move.targetZone()) + ".");
    return state;
  }

  private LiveGameState applyRepositionCard(LiveGameState state, RepositionCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    card.setX(move.x());
    card.setY(move.y());
    log(state, move.playerId(), "Repositioned " + cardDataService.getCard(card.getCardId()).name() + ".");
    return state;
  }

  private LiveGameState applyTapRune(LiveGameState state, TapRuneMove move) {
    RuneState rune = findRune(state, move.runeInstanceId());
    rune.setTapped(true);
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> p.setAvailableEnergy(p.getAvailableEnergy() + rune.getNormalEnergy()));
    log(state, move.playerId(), "Tapped a rune.");
    return state;
  }

  private LiveGameState applyDiscardRune(LiveGameState state, DiscardRuneMove move) {
    RuneState rune = findRune(state, move.runeInstanceId());
    state.getRunes().remove(rune);
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.playerId())).findFirst().ifPresent(p -> {
      p.setAvailableEnergy(p.getAvailableEnergy() + rune.getPremiumEnergy());
      p.setRunePoolRemaining(Math.max(0, p.getRunePoolRemaining() - 1));
    });
    log(state, move.playerId(), "Discarded a rune.");
    return state;
  }

  private LiveGameState applyHideCard(LiveGameState state, HideCardMove move) {
    CardInstance card = findCard(state, move.instanceId());
    CardDefinition def = cardDataService.getCard(card.getCardId());
    RuneState rune = findRune(state, move.paymentRuneId());
    rune.setTapped(true);
    card.setZone(ZoneName.HIDDEN);
    card.setFaceDown(true);
    card.setTapped(false);
    card.setX(0);
    card.setY(0);
    card.setAttachedToInstanceId(null);
    log(state, move.playerId(), "Hid " + def.name() + ".");
    return state;
  }

  private LiveGameState applyMulligan(LiveGameState state, MulliganMove move) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(move.playerId()))
        .findFirst()
        .orElseThrow();
    List<CardInstance> returned = state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(move.playerId()))
        .filter(card -> card.getZone() == ZoneName.HAND)
        .filter(card -> move.discardInstanceIds().contains(card.getInstanceId()))
        .toList();
    state.getCards().removeAll(returned);
    for (int i = 0; i < returned.size() && !player.getDeckPool().isEmpty(); i++) autoDraw(state, move.playerId());
    returned.forEach(card -> player.getDeckPool().add(card.getCardId()));
    Collections.shuffle(player.getDeckPool());
    state.getMulligansDone().add(move.playerId());
    log(state, move.playerId(), returned.isEmpty() ? "Kept their opening hand." : "Mulliganed " + returned.size() + " card(s).");
    if (state.getMulligansDone().size() == state.getPlayers().size()) {
      state.setCurrentPhase(Phase.AWAKEN);
      log(state, state.getActivePlayerId(), "Mulligans complete. Advanced to AWAKEN.");
    }
    return state;
  }

  private LiveGameState applyMoveToBattlefield(LiveGameState state, MoveToBattlefieldMove move) {
    CardInstance card = findCard(state, move.instanceId());
    CardDefinition def = cardDataService.getCard(card.getCardId());
    ZoneName sourceZone = card.getZone();
    boolean playedFromChampionZone = card.getZone() == ZoneName.CHAMPION;
    if (playedFromChampionZone) {
      PlayerState player = player(state, move.playerId());
      int selectedEnergy = 0;
      for (String runeId : move.paymentRuneIds()) {
        RuneState rune = findRune(state, runeId);
        rune.setTapped(true);
        selectedEnergy += rune.getNormalEnergy();
      }
      for (String runeId : move.premiumRuneIds()) {
        RuneState rune = findRune(state, runeId);
        state.getRunes().remove(rune);
        if (rune.getCardId() != null && !rune.getCardId().isBlank()) {
          player.getRuneDeckPool().add(rune.getCardId());
          player.setRunePoolRemaining(player.getRuneDeckPool().size());
        }
      }
      player.setAvailableEnergy(Math.max(0, player.getAvailableEnergy() + selectedEnergy - Math.max(0, def.cost())));
    }
    card.setZone(ZoneName.BATTLEFIELD);
    card.setX(0);
    card.setY(0);
    card.setTapped(!cardDataService.hasKeyword(card, "AMBUSH"));
    card.setHasSummoningSickness(false);
    int gankingBonus = applyGanking(state, card);
    applyMovementScripts(state, card, sourceZone, ZoneName.BATTLEFIELD);
    applyMoveToBattlefieldTokenScripts(state, card);
    effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onAttack(card, state));
    log(state, move.playerId(), playedFromChampionZone
        ? "Played " + def.name() + " from the Champion zone."
        : "Moved " + def.name() + " to the battlefield.");
    boolean opposed = state.getCards().stream()
        .anyMatch(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD && !move.playerId().equals(candidate.getOwnerId()));
    if (!opposed) {
      state.getBattlefieldController().put("BATTLEFIELD", move.playerId());
      return state;
    }
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        move.playerId(),
        List.of(card.getInstanceId()),
        gankingBonus > 0 ? new HashMap<>(Map.of(card.getInstanceId(), gankingBonus)) : new HashMap<>(),
        ShowdownStep.ACTION_WINDOW));
    log(state, move.playerId(), "Showdown started at the battlefield.");
    return state;
  }

  private void startAmbushBattlefield(LiveGameState state, CardInstance card, CardDefinition def, String playerId) {
    log(state, playerId, "Ambushed " + def.name() + " to the battlefield.");
    boolean opposed = state.getCards().stream()
        .anyMatch(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD && !playerId.equals(candidate.getOwnerId()));
    if (!opposed) {
      state.getBattlefieldController().put("BATTLEFIELD", playerId);
      return;
    }
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        playerId,
        List.of(card.getInstanceId()),
        new HashMap<>(),
        ShowdownStep.ACTION_WINDOW));
    log(state, playerId, "Showdown started at the battlefield.");
  }

  private LiveGameState applyResolveShowdown(LiveGameState state, ResolveShowdownMove move) {
    LiveGameState.ShowdownState showdown = state.getActiveShowdown();
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        showdown.attackingPlayerId(),
        showdown.attackerInstanceIds(),
        showdown.gankingBonuses(),
        ShowdownStep.ASSIGN_DAMAGE));
    CombatResolver.CombatResult result = combatResolver.resolve(state, showdown.attackingPlayerId());
    state.setActiveShowdown(new LiveGameState.ShowdownState(
        showdown.attackingPlayerId(),
        showdown.attackerInstanceIds(),
        showdown.gankingBonuses(),
        ShowdownStep.CLEANUP));
    showdown.gankingBonuses().forEach((instanceId, bonus) -> state.getCards().stream()
        .filter(card -> card.getInstanceId().equals(instanceId))
        .findFirst()
        .ifPresent(card -> card.setTemporaryPowerModifier(Math.max(0, card.getTemporaryPowerModifier() - bonus))));
    if (result.attackersRemain() && result.defendersEliminated()) conquerBattlefield(state, showdown.attackingPlayerId());
    else if (!result.defendersEliminated()) returnBattlefieldCardsToBase(state, showdown.attackingPlayerId());
    state.setActiveShowdown(null);
    state.setCurrentPhase(Phase.MAIN);
    log(state, move.playerId(), "Showdown resolved.");
    return state;
  }

  private LiveGameState applyUndoRunes(LiveGameState state, UndoRunesMove move) {
    state.getRunes().stream()
        .filter(rune -> rune.getOwnerId().equals(move.playerId()) && rune.isTapped())
        .forEach(rune -> rune.setTapped(false));
    state.getPlayers().stream()
        .filter(player -> player.getUserId().equals(move.playerId()))
        .findFirst()
        .ifPresent(player -> player.setAvailableEnergy(0));
    log(state, move.playerId(), "Undid rune taps.");
    return state;
  }

  private LiveGameState applyPassPhase(LiveGameState state) {
    Phase next;
    switch (state.getCurrentPhase()) {
      case MULLIGAN -> next = Phase.AWAKEN;
      case AWAKEN -> { awaken(state); next = Phase.BEGINNING; }
      case BEGINNING -> { applyBeginning(state); next = Phase.CHANNEL; }
      case CHANNEL -> { applyChannel(state); next = Phase.DRAW; }
      case DRAW -> { applyDraw(state); clearEnergy(state); next = Phase.MAIN; }
      case MAIN -> next = Phase.END;
      case END -> {
        finishEndPhase(state);
        clearEnergy(state);
        advanceTurn(state);
        next = Phase.AWAKEN;
      }
      default -> next = Phase.AWAKEN;
    }
    state.setCurrentPhase(next);
    log(state, state.getActivePlayerId(), "Advanced to " + next);
    return state;
  }

  private LiveGameState applyAdjustScore(LiveGameState state, AdjustScoreMove move) {
    state.getPlayers().stream().filter(p -> p.getUserId().equals(move.targetPlayerId())).findFirst().ifPresent(p -> p.setScore(Math.max(0, p.getScore() + move.delta())));
    log(state, move.playerId(), "Adjusted score.");
    return state;
  }

  private void advanceTurn(LiveGameState state) {
    int index = 0;
    for (int i = 0; i < state.getPlayers().size(); i++) if (state.getPlayers().get(i).getUserId().equals(state.getActivePlayerId())) index = i;
    PlayerState next = state.getPlayers().get((index + 1) % state.getPlayers().size());
    state.setActivePlayerId(next.getUserId());
    state.setTurnNumber(state.getTurnNumber() + 1);
    state.setCardPlayedThisTurn(false);
    state.setScoredBattlefieldsThisTurn(new HashSet<>());
  }

  private LiveGameState applyVisionChoice(LiveGameState state, VisionChoiceMove move) {
    PlayerState player = player(state, move.playerId());
    if (player.getDeckPool().isEmpty()) {
      log(state, move.playerId(), "Vision found no card to move.");
      return state;
    }
    String topCardId = player.getDeckPool().get(0);
    if (move.recycle()) {
      player.getDeckPool().remove(0);
      player.getDeckPool().add(topCardId);
      log(state, move.playerId(), "VISION_RESOLVED|Recycled " + cardDataService.getCard(topCardId).name() + " to the bottom of the deck.");
    } else {
      log(state, move.playerId(), "VISION_RESOLVED|Kept " + cardDataService.getCard(topCardId).name() + " on top of the deck.");
    }
    return state;
  }

  private LiveGameState applyResolveChoice(LiveGameState state, ResolveChoiceMove move) {
    PendingChoice choice = state.getPendingChoice();
    if (PendingChoice.TYPE_OPTIONAL_DRAW_ONE.equals(choice.getType())) {
      if (PendingChoice.OPTION_YES.equals(move.selectedOptionId())) {
        applyDraw(state, move.playerId(), 1);
        log(state, move.playerId(), player(state, move.playerId()).getName() + " chose to draw 1.");
      } else {
        log(state, move.playerId(), player(state, move.playerId()).getName() + " declined " + choicePromptLabel(choice) + ".");
      }
    } else if (PendingChoice.TYPE_OPTIONAL_PAY_1_DRAW_ONE.equals(choice.getType())) {
      if (PendingChoice.OPTION_PAY_1.equals(move.selectedOptionId())) {
        PlayerState player = player(state, move.playerId());
        player.setAvailableEnergy(Math.max(0, player.getAvailableEnergy() - 1));
        applyDraw(state, move.playerId(), 1);
        log(state, move.playerId(), player.getName() + " paid 1 to draw 1.");
      } else {
        log(state, move.playerId(), player(state, move.playerId()).getName() + " declined " + choicePromptLabel(choice) + ".");
      }
    } else if (PendingChoice.TYPE_TOP_DECK_PICK_ONE.equals(choice.getType())) {
      resolveTopDeckPickOne(state, move, choice);
    } else if (PendingChoice.TYPE_PREDICT_ORDER.equals(choice.getType())) {
      resolvePredictOrder(state, move, choice);
    }
    state.setPendingChoice(null);
    return state;
  }

  private void resolveTopDeckPickOne(LiveGameState state, ResolveChoiceMove move, PendingChoice choice) {
    PlayerState player = player(state, move.playerId());
    int count = Math.min(choice.getCardOptions().size(), player.getDeckPool().size());
    if (count == 0) {
      log(state, move.playerId(), choicePromptLabel(choice) + " found no cards.");
      return;
    }
    List<String> lookedAt = new ArrayList<>(player.getDeckPool().subList(0, count));
    player.getDeckPool().subList(0, count).clear();
    PendingChoice.CardChoiceOption selected = choice.getCardOptions().stream()
        .filter(option -> option.optionId().equals(move.selectedCardOptionId()))
        .findFirst()
        .orElse(null);
    if (selected == null || selected.originalIndex() >= lookedAt.size()) {
      player.getDeckPool().addAll(0, lookedAt);
      throw new IllegalMoveException("Choose one of the revealed cards.");
    }
    String selectedCardId = lookedAt.get(selected.originalIndex());
    addCardToHand(state, move.playerId(), selectedCardId, false);
    for (int i = 0; i < lookedAt.size(); i++) {
      if (i != selected.originalIndex()) player.getDeckPool().add(lookedAt.get(i));
    }
    log(state, move.playerId(), choicePromptLabel(choice) + " put a card into hand and recycled the rest.");
  }

  private void resolvePredictOrder(LiveGameState state, ResolveChoiceMove move, PendingChoice choice) {
    PlayerState player = player(state, move.playerId());
    int count = Math.min(choice.getCardOptions().size(), player.getDeckPool().size());
    if (count == 0) {
      log(state, move.playerId(), "Predict found no cards.");
      return;
    }
    List<String> lookedAt = new ArrayList<>(player.getDeckPool().subList(0, count));
    player.getDeckPool().subList(0, count).clear();
    Map<String, PendingChoice.CardChoiceOption> options = new HashMap<>();
    choice.getCardOptions().forEach(option -> options.put(option.optionId(), option));
    List<PendingChoice.CardChoiceAssignment> orderedAssignments = new ArrayList<>(move.assignments());
    orderedAssignments.sort((left, right) -> Integer.compare(left.order(), right.order()));
    List<String> topIds = new ArrayList<>();
    List<String> bottomIds = new ArrayList<>();
    for (PendingChoice.CardChoiceAssignment assignment : orderedAssignments) {
      PendingChoice.CardChoiceOption option = options.get(assignment.optionId());
      if (option == null || option.originalIndex() >= lookedAt.size()) continue;
      String cardId = lookedAt.get(option.originalIndex());
      if (PendingChoice.ACTION_TOP.equals(assignment.action())) topIds.add(cardId);
      if (PendingChoice.ACTION_BOTTOM.equals(assignment.action())) bottomIds.add(cardId);
    }
    Collections.reverse(topIds);
    player.getDeckPool().addAll(0, topIds);
    player.getDeckPool().addAll(bottomIds);
    log(state, move.playerId(), "Resolved Predict ordering.");
  }

  private String choicePromptLabel(PendingChoice choice) {
    if (choice.getSourceCardId() != null && !choice.getSourceCardId().isBlank()) {
      CardDefinition source = cardDataService.getCard(choice.getSourceCardId());
      if (source != null && source.name() != null && !source.name().isBlank()) return source.name();
    }
    return "the effect";
  }

  private LiveGameState applyDismissRevealed(LiveGameState state, DismissRevealedMove move) {
    state.getRevealedHands().stream()
        .filter(snapshot -> snapshot.getRevealedToPlayerId().equals(move.playerId()))
        .forEach(snapshot -> snapshot.getDismissedInstanceIds().add(move.instanceId()));
    return state;
  }

  private void awaken(LiveGameState state) {
    PlayerState active = player(state, state.getActivePlayerId());
    active.setAvailableEnergy(0);
    state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(active.getUserId()))
        .forEach(r -> r.setTapped(false));
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(active.getUserId()))
        .forEach(c -> c.setTapped(false));
  }

  private void applyBeginning(LiveGameState state) {
    expireTemporaryCards(state);
    state.getCards().stream()
        .filter(c -> c.getOwnerId().equals(state.getActivePlayerId()) && (c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD))
        .forEach(c -> effects.getEffect(c.getCardId()).ifPresent(effect -> effect.onTurnStart(c, state)));
    scoreHeldBattlefield(state);
  }

  private void applyChannel(LiveGameState state) {
    int amount = !state.getActivePlayerId().equals(state.getFirstPlayerId()) && state.getTurnNumber() == 2 ? 3 : 2;
    grantRunes(state, state.getActivePlayerId(), amount);
  }

  private void applyDraw(LiveGameState state) {
    if (state.getTurnNumber() == 1 && state.getActivePlayerId().equals(state.getFirstPlayerId())) {
      log(state, state.getActivePlayerId(), "First player skips their first draw.");
      return;
    }
    autoDraw(state, state.getActivePlayerId());
  }

  private void clearEnergy(LiveGameState state) {
    state.getPlayers().forEach(player -> player.setAvailableEnergy(0));
  }

  private void grantRunes(LiveGameState state, String playerId, int amount) {
    PlayerState player = state.getPlayers().stream()
        .filter(candidate -> candidate.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getRunePoolRemaining() <= 0) {
      log(state, playerId, player.getName() + "'s rune deck is exhausted.");
      return;
    }
    long currentCount = state.getRunes().stream()
        .filter(r -> r.getOwnerId().equals(playerId))
        .count();
    int granted = 0;
    for (int i = 0; i < amount && currentCount < MAX_RUNES && granted < player.getRunePoolRemaining(); i++, currentCount++, granted++) {
      RuneState rune = new RuneState();
      rune.setInstanceId(UUID.randomUUID().toString());
      if (!player.getRuneDeckPool().isEmpty()) {
        rune.setCardId(player.getRuneDeckPool().remove(0));
      }
      rune.setOwnerId(playerId);
      rune.setTapped(false);
      rune.setNormalEnergy(1);
      rune.setPremiumEnergy(2);
      state.getRunes().add(rune);
    }
    player.setRunePoolRemaining(player.getRuneDeckPool().isEmpty() ? Math.max(0, player.getRunePoolRemaining() - granted) : player.getRuneDeckPool().size());
  }

  private void scoreHeldBattlefield(LiveGameState state) {
    String activePlayerId = state.getActivePlayerId();
    for (String battlefieldId : battlefieldIds(state)) {
      if (!activePlayerId.equals(state.getBattlefieldController().get(battlefieldId))) continue;
      if (!state.getScoredBattlefieldsThisTurn().add(battlefieldId)) continue;
      scorePoint(state, activePlayerId);
      log(state, activePlayerId, "Held " + battlefieldLabel(battlefieldId) + " - score +1.");
    }
  }

  private void conquerBattlefield(LiveGameState state, String playerId) {
    String battlefieldId = "BATTLEFIELD";
    state.getBattlefieldController().put(battlefieldId, playerId);
    if (state.getScoredBattlefieldsThisTurn().contains(battlefieldId)) return;
    PlayerState scorer = player(state, playerId);
    state.getScoredBattlefieldsThisTurn().add(battlefieldId);
    if (scorer.getScore() >= targetScore - 1 && !scoredAllBattlefieldsThisTurn(state)) {
      autoDraw(state, scorer.getUserId());
      log(state, scorer.getUserId(), "Conquers but draws a card (must score all battlefields to win).");
      return;
    }
    scorePoint(state, scorer.getUserId());
    log(state, scorer.getUserId(), scorer.getScore() >= targetScore ? "Conquers for the winning point!" : "Conquers - +1 point.");
  }

  private List<String> battlefieldIds(LiveGameState state) {
    if (state.getBattlefieldController().isEmpty()) return List.of("BATTLEFIELD");
    return state.getBattlefieldController().keySet().stream().sorted().toList();
  }

  private boolean scoredAllBattlefieldsThisTurn(LiveGameState state) {
    return state.getScoredBattlefieldsThisTurn().containsAll(battlefieldIds(state));
  }

  private void scorePoint(LiveGameState state, String playerId) {
    PlayerState scorer = player(state, playerId);
    scorer.setScore(scorer.getScore() + 1);
  }

  private String battlefieldLabel(String battlefieldId) {
    return "BATTLEFIELD".equals(battlefieldId) ? "the battlefield" : battlefieldId;
  }

  private void returnBattlefieldCardsToBase(LiveGameState state, String playerId) {
    List<CardInstance> returned = state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD && playerId.equals(card.getOwnerId()))
        .toList();
    returned.forEach(card -> {
      card.setZone(ZoneName.BASE);
      card.setX(0);
      card.setY(0);
      applyMovementScripts(state, card, ZoneName.BATTLEFIELD, ZoneName.BASE);
    });
  }

  private void healBoardCards(LiveGameState state) {
    state.getCards().stream()
        .filter(c -> c.getZone() == ZoneName.BASE || c.getZone() == ZoneName.BATTLEFIELD)
        .forEach(c -> {
          CardDefinition def = cardDataService.getCard(c.getCardId());
          if (def != null && def.health() > 0) c.setCurrentHealth(def.health());
          c.setHasSummoningSickness(false);
          c.setTemporaryPowerModifier(0);
          c.getTempKeywords().clear();
        });
  }

  private void finishEndPhase(LiveGameState state) {
    state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(state.getActivePlayerId()) && card.getZone() == ZoneName.BATTLEFIELD)
        .forEach(card -> effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onTurnEnd(card, state)));
    healBoardCards(state);
  }

  private PlayerState player(LiveGameState state, String playerId) {
    return state.getPlayers().stream().filter(player -> playerId.equals(player.getUserId())).findFirst().orElseThrow();
  }

  private void applyLegion(LiveGameState state, CardInstance card, boolean cardPlayedEarlierThisTurn) {
    if (!cardPlayedEarlierThisTurn || !cardDataService.hasKeyword(card, "LEGION")) return;
    card.setMightBonus(card.getMightBonus() + 1);
    log(state, card.getOwnerId(), cardDataService.getCard(card.getCardId()).name() + " gains Legion +1 might.");
  }

  private void applyPlayedCardTokenScripts(LiveGameState state, CardInstance card, boolean cardPlayedEarlierThisTurn) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (isNamed(def, "Vanguard Captain") && cardPlayedEarlierThisTurn && cardDataService.hasKeyword(card, "LEGION")) {
      tokenFactory.createRecruit(state, card.getOwnerId(), ZoneName.BASE, card.getX() + 28, card.getY() + 28);
      tokenFactory.createRecruit(state, card.getOwnerId(), ZoneName.BASE, card.getX() + 56, card.getY() + 28);
      log(state, card.getOwnerId(), "Vanguard Captain's Legion created two Recruit tokens.");
    }
  }

  private void applyMoveToBattlefieldTokenScripts(LiveGameState state, CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (!isNamed(def, "Noxian Drummer")) return;
    tokenFactory.createRecruit(state, card.getOwnerId(), ZoneName.BATTLEFIELD, card.getX() + 40, card.getY() + 40);
    log(state, card.getOwnerId(), "Noxian Drummer created a Recruit token.");
  }

  private void applyMovementScripts(LiveGameState state, CardInstance card, ZoneName sourceZone, ZoneName targetZone) {
    if (sourceZone == targetZone) return;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    if (!isNamed(def, "Stellacorn Herder")) return;
    applyDraw(state, card.getOwnerId(), 1);
    log(state, card.getOwnerId(), "Stellacorn Herder drew 1 after moving.");
  }

  private boolean isNamed(CardDefinition def, String name) {
    return def != null && def.name() != null && def.name().trim().equalsIgnoreCase(name);
  }

  private int applyGanking(LiveGameState state, CardInstance card) {
    int value = cardDataService.getKeywordValue(card, "GANKING");
    if (value <= 0) return 0;
    int might = effectiveMight(card);
    boolean facesStrongerUnit = state.getCards().stream()
        .filter(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD)
        .filter(candidate -> !card.getOwnerId().equals(candidate.getOwnerId()))
        .anyMatch(candidate -> effectiveMight(candidate) > might);
    if (!facesStrongerUnit) return 0;
    card.setTemporaryPowerModifier(card.getTemporaryPowerModifier() + value);
    log(state, card.getOwnerId(), cardDataService.getCard(card.getCardId()).name() + " activates Ganking: +" + value + " Might this combat.");
    return value;
  }

  private void attachGear(LiveGameState state, CardInstance gear, CardDefinition gearDef, CardInstance target, String playerId) {
    gear.setZone(ZoneName.BASE);
    gear.setAttachedToInstanceId(target.getInstanceId());
    gear.setX(target.getX() + 34);
    gear.setY(target.getY() + 34);
    gear.setTapped(false);
    gear.setHasSummoningSickness(false);
    log(state, playerId, "Equipped " + gearDef.name() + " to " + cardDataService.getCard(target.getCardId()).name() + ".");
  }

  private CardInstance deflectTarget(CardInstance playedCard, CardInstance target, LiveGameState state) {
    if (target == null
        || playedCard.getOwnerId().equals(target.getOwnerId())
        || !cardDataService.hasKeyword(target, "DEFLECT")) {
      return target;
    }
    List<CardInstance> alternatives = state.getCards().stream()
        .filter(candidate -> candidate.getZone() == ZoneName.BATTLEFIELD)
        .filter(candidate -> !candidate.getInstanceId().equals(target.getInstanceId()))
        .toList();
    if (alternatives.isEmpty()) return target;
    log(state, target.getOwnerId(), cardDataService.getCard(target.getCardId()).name() + " deflects the spell!");
    return alternatives.get(ThreadLocalRandom.current().nextInt(alternatives.size()));
  }

  private int effectiveMight(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return Math.max(0, def.power() + card.getMightBonus() + card.getTemporaryPowerModifier());
  }

  private void expireTemporaryCards(LiveGameState state) {
    List<CardInstance> expired = state.getCards().stream()
        .filter(card -> card.getOwnerId().equals(state.getActivePlayerId()))
        .filter(card -> card.getZone() == ZoneName.BASE || card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> cardDataService.hasKeyword(card, "TEMPORARY"))
        .toList();
    for (CardInstance card : expired) {
      CardDefinition def = cardDataService.getCard(card.getCardId());
      DeathEvent death = deathTriggerService.capture(card, state, DeathEvent.DeathCause.CLEANUP);
      returnAttachmentsToBase(state, card);
      cardZoneService.moveToGraveyard(card);
      effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
      deathTriggerService.process(state, List.of(death));
      log(state, card.getOwnerId(), def.name() + " expired (Temporary).");
    }
  }

  private void moveDestroyedBoardCards(LiveGameState state) {
    List<CardInstance> destroyed = state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BASE || card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> {
          CardDefinition def = cardDataService.getCard(card.getCardId());
          return def != null
              && ("Champion".equalsIgnoreCase(def.type()) || "Unit".equalsIgnoreCase(def.type()))
              && def.health() > 0
              && card.getCurrentHealth() <= 0;
        })
        .toList();
    List<DeathEvent> deaths = destroyed.stream()
        .map(card -> deathTriggerService.capture(card, state, DeathEvent.DeathCause.EFFECT))
        .toList();
    for (CardInstance card : destroyed) {
      returnAttachmentsToBase(state, card);
      cardZoneService.moveToGraveyard(card);
      effects.getEffect(card.getCardId()).ifPresent(effect -> effect.onDestroy(card, state));
    }
    deathTriggerService.process(state, deaths);
  }

  private void applyRulesTextEffect(CardInstance card, CardInstance target, LiveGameState state, CardDefinition def) {
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    if (isStackedDeckEffect(text)) {
      List<CardDefinition> topCards = topDeckDefinitions(state, card.getOwnerId(), 3);
      if (topCards.isEmpty()) {
        log(state, card.getOwnerId(), def.name() + " found no cards.");
      } else {
        state.setPendingChoice(PendingChoice.topDeckPickOne(
            UUID.randomUUID().toString(),
            card.getOwnerId(),
            card.getCardId(),
            card.getInstanceId(),
            topCards));
        log(state, card.getOwnerId(), def.name() + " is waiting for a private card choice.");
      }
      return;
    }
    if (text.contains("reveal") && text.contains("hand")) {
      state.getPlayers().stream()
          .map(PlayerState::getUserId)
          .filter(playerId -> !playerId.equals(card.getOwnerId()))
          .findFirst()
          .ifPresent(opponentId -> CardEffectRegistry.revealHand(state, card.getOwnerId(), opponentId));
    }
    if (target != null) {
      Matcher powerBoost = Pattern.compile("\\+(\\d+)\\s*:rb_might:").matcher(text);
      if (powerBoost.find()) {
        int boost = Integer.parseInt(powerBoost.group(1));
        int totalBoost = boost;
        if (text.contains("additional +1") && state.getCards().stream()
            .filter(candidate -> candidate.getOwnerId().equals(target.getOwnerId()) && candidate.getZone() == target.getZone())
            .count() == 1) {
          totalBoost += 1;
        }
        applyTemporaryMight(state, card, def, target, totalBoost);
      }
      if (text.contains("return a unit") || text.contains("return target unit")) {
        returnUnitToOwnerHand(state, card, def, target);
      }
      if (text.contains("ready it")) {
        readyUnit(state, card, def, target);
      }
    }
    if (text.contains("draw 1")) applyDraw(state, card.getOwnerId(), 1);
  }

  private boolean isStackedDeckEffect(String text) {
    return text.contains("look at the top 3")
        && text.contains("put 1")
        && text.contains("hand")
        && text.contains("recycle");
  }

  private List<CardDefinition> topDeckDefinitions(LiveGameState state, String playerId, int count) {
    PlayerState player = player(state, playerId);
    return player.getDeckPool().stream()
        .limit(count)
        .map(cardDataService::getCard)
        .toList();
  }

  private void applyDraw(LiveGameState state, String playerId, int count) {
    for (int i = 0; i < count; i++) autoDraw(state, playerId);
  }

  private void applyTemporaryMight(LiveGameState state, CardInstance source, CardDefinition sourceDef, CardInstance target, int amount) {
    target.setTemporaryPowerModifier(target.getTemporaryPowerModifier() + amount);
    log(state, source.getOwnerId(), sourceDef.name() + " gave " + cardDataService.getCard(target.getCardId()).name() + " +" + amount + " Might this turn.");
  }

  private void returnUnitToOwnerHand(LiveGameState state, CardInstance source, CardDefinition sourceDef, CardInstance target) {
    returnAttachmentsToBase(state, target);
    target.setZone(ZoneName.HAND);
    target.setTapped(false);
    target.setHasSummoningSickness(false);
    log(state, source.getOwnerId(), sourceDef.name() + " returned " + cardDataService.getCard(target.getCardId()).name() + " to hand.");
  }

  private void readyUnit(LiveGameState state, CardInstance source, CardDefinition sourceDef, CardInstance target) {
    target.setTapped(false);
    log(state, source.getOwnerId(), sourceDef.name() + " readied " + cardDataService.getCard(target.getCardId()).name() + ".");
  }

  private void returnAttachmentsToBase(LiveGameState state, CardInstance host) {
    List<CardInstance> returned = cardZoneService.returnAttachmentsToBase(state, host);
    if (returned == null) return;
    for (CardInstance attachment : returned) {
      log(state, attachment.getOwnerId(), cardName(attachment) + " returned to Base.");
    }
  }

  private String cardName(CardInstance card) {
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def == null ? card.getCardId() : def.name();
  }

  private void autoDraw(LiveGameState state, String playerId) {
    PlayerState player = state.getPlayers().stream()
        .filter(p -> p.getUserId().equals(playerId))
        .findFirst()
        .orElse(null);
    if (player == null) return;
    if (player.getDeckPool().isEmpty()) {
      log(state, playerId, player.getName() + "'s deck is empty \u2014 no draw.");
      return;
    }
    String cardId = player.getDeckPool().remove(0);
    addCardToHand(state, playerId, cardId, true);
  }

  private void addCardToHand(LiveGameState state, String playerId, String cardId, boolean logCardName) {
    CardDefinition def = cardDataService.getCard(cardId);
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance instance = new CardInstance();
    instance.setInstanceId(UUID.randomUUID().toString());
    instance.setCardId(cardId);
    instance.setOwnerId(playerId);
    instance.setZone(ZoneName.HAND);
    instance.setX(0);
    instance.setY(0);
    instance.setCurrentHealth(def.health());
    instance.setHasSummoningSickness(false);
    instance.setTempKeywords(new ArrayList<>());
    instance.setZIndex(maxZ + 1);
    state.getCards().add(instance);
    if (logCardName) log(state, playerId, "Drew " + def.name() + ".");
  }

  private void checkWinCondition(LiveGameState state) {
    if (state.getWinnerId() != null) return;
    state.getPlayers().stream().filter(p -> p.getScore() >= targetScore).findFirst().ifPresent(p -> state.setWinnerId(p.getUserId()));
  }

  private String normalizeCardType(String type) {
    return type == null ? "" : type.trim().toLowerCase();
  }

  private String displayZone(ZoneName zone) {
    return switch (zone) {
      case BASE -> "Base";
      case BATTLEFIELD -> "Battlefield";
      case HAND -> "Hand";
      case DISCARD -> "Trash";
      case CHAMPION -> "Champion";
      case LEGEND -> "Legend";
      case RUNE -> "Runes";
      case RUNE_DECK -> "Rune Deck";
      case DECK -> "Deck";
      case LIMBO -> "Limbo";
      case HIDDEN -> "Hidden";
    };
  }

  private CardInstance findCard(LiveGameState state, String id) {
    return state.getCards().stream().filter(c -> c.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  private RuneState findRune(LiveGameState state, String id) {
    return state.getRunes().stream().filter(r -> r.getInstanceId().equals(id)).findFirst().orElseThrow();
  }

  public static void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(UUID.randomUUID().toString(), Instant.now().toString(), userId, text));
  }
}
