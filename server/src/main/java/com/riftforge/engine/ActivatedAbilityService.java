package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.ActivateAbilityMove;
import com.riftforge.service.CardDataService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ActivatedAbilityService {
  public static final String THE_SYREN_RECALL = "THE_SYREN_RECALL";

  private final CardDataService cardDataService;

  public ActivatedAbilityService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public List<ActivatedAbilityDefinition> definitionsFor(CardDefinition sourceDef) {
    if (cardDataService.isTheSyrenActivatedAbility(sourceDef)) {
      return List.of(new ActivatedAbilityDefinition(
          THE_SYREN_RECALL,
          "Move a friendly unit at a battlefield to its base.",
          ZoneName.BASE,
          1,
          true,
          ActivatedAbilityTiming.MAIN_PHASE_IMMEDIATE,
          ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_BATTLEFIELD_UNIT,
          false,
          "Move a friendly battlefield unit to Base."));
    }
    return List.of();
  }

  public Optional<ActivatedAbilityDefinition> definitionFor(CardDefinition sourceDef, String abilityKey) {
    List<ActivatedAbilityDefinition> definitions = definitionsFor(sourceDef);
    if (abilityKey == null || abilityKey.isBlank()) {
      return definitions.size() == 1 ? Optional.of(definitions.getFirst()) : Optional.empty();
    }
    return definitions.stream()
        .filter(definition -> definition.abilityKey().equals(abilityKey))
        .findFirst();
  }

  public boolean hasSupportedAbility(CardDefinition sourceDef) {
    return !definitionsFor(sourceDef).isEmpty();
  }

  public boolean hasLegalActivation(LiveGameState state, String playerId) {
    if (state == null || playerId == null || state.getCurrentPhase() != Phase.MAIN || state.getActiveShowdown() != null) {
      return false;
    }
    return state.getCards().stream()
        .filter(card -> playerId.equals(card.getOwnerId()))
        .anyMatch(source -> definitionsFor(cardDataService.getCard(source.getCardId())).stream()
            .anyMatch(definition -> canActivate(state, playerId, source, definition)));
  }

  public ActivatedAbilityDefinition validate(LiveGameState state, ActivateAbilityMove move) {
    if (state.getCurrentPhase() != Phase.MAIN) throw new IllegalMoveException("Activated abilities can only be used during your Main Phase.");
    if (state.getActiveShowdown() != null) throw new IllegalMoveException("Resolve the active showdown first.");
    CardInstance source = findCard(state, move.sourceInstanceId());
    if (!move.playerId().equals(source.getOwnerId())) throw new IllegalMoveException("You do not own that card.");
    CardDefinition sourceDef = cardDataService.getCard(source.getCardId());
    ActivatedAbilityDefinition definition = definitionFor(sourceDef, move.abilityKey())
        .orElseThrow(() -> new IllegalMoveException("That activated ability is not supported yet."));
    validateSource(source, definition);
    validateTarget(state, move, definition);
    validatePayment(state, move, definition);
    return definition;
  }

  public CardInstance validateTarget(LiveGameState state, ActivateAbilityMove move, ActivatedAbilityDefinition definition) {
    if (definition.targetKind() == ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_BATTLEFIELD_UNIT) {
      if (move.targetInstanceId() == null || move.targetInstanceId().isBlank()) {
        throw new IllegalMoveException("This ability requires a friendly Unit or Champion at a battlefield.");
      }
      CardInstance target = findCard(state, move.targetInstanceId());
      if (!isFriendlyPublicBattlefieldUnit(target, move.playerId())) {
        throw new IllegalMoveException("This ability can only target a friendly public Unit or Champion at a battlefield.");
      }
      return target;
    }
    throw new IllegalMoveException("That activated ability target is not supported yet.");
  }

  private boolean canActivate(LiveGameState state, String playerId, CardInstance source, ActivatedAbilityDefinition definition) {
    try {
      validateSource(source, definition);
      if (!canPayEnergy(state, playerId, definition.energyCost())) return false;
      return hasLegalTarget(state, playerId, definition);
    } catch (IllegalMoveException ex) {
      return false;
    }
  }

  private boolean hasLegalTarget(LiveGameState state, String playerId, ActivatedAbilityDefinition definition) {
    if (definition.targetKind() == ActivatedAbilityTargetKind.FRIENDLY_PUBLIC_BATTLEFIELD_UNIT) {
      return state.getCards().stream().anyMatch(card -> isFriendlyPublicBattlefieldUnit(card, playerId));
    }
    return false;
  }

  private void validateSource(CardInstance source, ActivatedAbilityDefinition definition) {
    if (source.getZone() != definition.sourceZone()) {
      throw new IllegalMoveException("That ability cannot be activated from this zone.");
    }
    if (source.isFaceDown()) throw new IllegalMoveException("Hidden or face-down cards cannot activate this ability.");
    if (definition.requiresExhaust() && source.isTapped()) throw new IllegalMoveException("That card is already exhausted.");
    if (source.getAttachedToInstanceId() != null && !source.getAttachedToInstanceId().isBlank()) {
      throw new IllegalMoveException("Attached cards cannot activate this ability.");
    }
  }

  private void validatePayment(LiveGameState state, ActivateAbilityMove move, ActivatedAbilityDefinition definition) {
    Set<String> allRuneIds = new HashSet<>();
    for (String runeId : move.paymentRuneIds()) {
      validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
    }
    for (String runeId : move.premiumRuneIds()) {
      validatePaymentRune(state, move.playerId(), requirePaymentRuneId(runeId));
      if (!allRuneIds.add(runeId)) throw new IllegalMoveException("Payment cannot use the same rune twice.");
    }
    if (!move.premiumRuneIds().isEmpty()) {
      throw new IllegalMoveException("This activated ability does not require premium payment.");
    }
    if (playerEnergy(state, move.playerId()) + selectedPaymentEnergy(state, move.paymentRuneIds()) < definition.energyCost()) {
      throw new IllegalMoveException("Insufficient energy for this activated ability.");
    }
  }

  private boolean canPayEnergy(LiveGameState state, String playerId, int energyCost) {
    return playerEnergy(state, playerId) + state.getRunes().stream()
        .filter(rune -> playerId.equals(rune.getOwnerId()))
        .filter(rune -> !rune.isTapped())
        .mapToInt(RuneState::getNormalEnergy)
        .sum() >= energyCost;
  }

  private int selectedPaymentEnergy(LiveGameState state, List<String> paymentRuneIds) {
    return state.getRunes().stream()
        .filter(rune -> paymentRuneIds.contains(rune.getInstanceId()))
        .mapToInt(RuneState::getNormalEnergy)
        .sum();
  }

  private int playerEnergy(LiveGameState state, String playerId) {
    return state.getPlayers().stream()
        .filter(player -> playerId.equals(player.getUserId()))
        .findFirst()
        .map(PlayerState::getAvailableEnergy)
        .orElse(0);
  }

  private RuneState validatePaymentRune(LiveGameState state, String playerId, String runeId) {
    RuneState rune = state.getRunes().stream()
        .filter(candidate -> runeId.equals(candidate.getInstanceId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("Payment rune not found."));
    if (!playerId.equals(rune.getOwnerId())) throw new IllegalMoveException("You cannot spend an opponent's rune.");
    if (rune.isTapped()) throw new IllegalMoveException("Payment rune is already tapped.");
    return rune;
  }

  private String requirePaymentRuneId(String runeId) {
    if (runeId == null || runeId.isBlank()) throw new IllegalMoveException("Payment rune id is required.");
    return runeId;
  }

  private CardInstance findCard(LiveGameState state, String instanceId) {
    if (instanceId == null || instanceId.isBlank()) throw new IllegalMoveException("Card instance id is required.");
    return state.getCards().stream()
        .filter(card -> instanceId.equals(card.getInstanceId()))
        .findFirst()
        .orElseThrow(() -> new IllegalMoveException("Card instance not found."));
  }

  private boolean isFriendlyPublicBattlefieldUnit(CardInstance card, String playerId) {
    if (!playerId.equals(card.getOwnerId())) return false;
    if (card.getZone() != ZoneName.BATTLEFIELD) return false;
    if (card.isFaceDown()) return false;
    CardDefinition def = cardDataService.getCard(card.getCardId());
    return def != null && isUnitOrChampion(def);
  }

  private boolean isUnitOrChampion(CardDefinition def) {
    String type = def.type() == null ? "" : def.type().toLowerCase();
    return type.contains("unit") || type.contains("champion");
  }
}
