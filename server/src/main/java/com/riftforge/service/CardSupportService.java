package com.riftforge.service;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import com.riftforge.model.CardSupportSummary;
import com.riftforge.rules.TournamentLegality;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CardSupportService {
  private static final Set<String> SUPPORTED_CARD_NAMES = Set.of(
      "CALM RUNE",
      "CHAOS RUNE",
      "BODY RUNE",
      "ORDER RUNE",
      "VANGUARD SERGEANT",
      "DARING PORO",
      "LAURENT DUELIST",
      "NOXIAN DRUMMER",
      "LOYAL PORO",
      "LONELY PORO",
      "VANGUARD CAPTAIN",
      "STELLACORN HERDER",
      "DISARMING RAKE");

  private static final Map<String, String> PARTIAL_REASONS = Map.ofEntries(
      Map.entry("IRELIA - BLADE DANCER", "Partial: the Legend-zone activated ready ability is implemented for Main Phase alpha play: exhaust Irelia, pay one rainbow/premium rune, and ready an exhausted friendly public Unit/Champion in Base or at a battlefield. The conquer trigger that may pay 1 energy to ready Irelia remains deferred."),
      Map.entry("IRELIA - FERVENT", "Partial: the supported explicit-ready trigger gives Irelia +1 Might this turn when her controller readies her through a registered effect. Deflect targeting tax remains heuristic, choose-trigger coverage is incomplete, and automatic ready-step trigger timing remains deferred."),
      Map.entry("DISCIPLINE", "Partial: alpha chain-window Reaction support exists for giving a public battlefield Unit/Champion +2 Might this turn and drawing 1. Full official any-time Reaction timing remains incomplete."),
      Map.entry("TIDETURNER", "Partial: Hidden foundation exists, but later hidden play timing and the on-play location swap are not implemented yet."),
      Map.entry("GUARDIAN ANGEL", "Partial: alpha Equip lifecycle and printed Calm equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred."),
      Map.entry("BOOTS OF SWIFTNESS", "Partial: alpha Equip lifecycle and printed Chaos equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred."),
      Map.entry("ABANDONED HALL", "Partial: exact spell-play optional trigger is implemented for active Battlefield lanes. The spell's controller may choose a friendly public Unit/Champion here to get +1 Might this turn; full official trigger stacking and broader Battlefield rules remain deferred."),
      Map.entry("SUNKEN TEMPLE", "Partial: exact conquer-with-Mighty optional pay-1 draw trigger is implemented for active Battlefield lanes. Broader Mighty threshold timing and full official location rules remain deferred."),
      Map.entry("TARGON'S PEAK", "Partial: exact conquer trigger queues end-turn readying for up to two tapped friendly runes in active Battlefield lanes. Player-selected rune choice and full official location rules remain deferred."),
      Map.entry("ADAPTATRON", "Partial: conquer trigger, optional gear kill, and official Buff state are not implemented yet."),
      Map.entry("SCUTTLE CRAB", "Partial: on-play draw and 1v1 private hand reveal Deathknell are implemented, but XP and facedown-card viewing are deferred."),
      Map.entry("DEFY", "Partial: Defy can counter supported public pending spell chain items that cost no more than 4 energy and no more than 1 premium rune during the current alpha chain window. Full official Reaction timing, broad spell/ability targets, and countering counters remain deferred."),
      Map.entry("NOT SO FAST", "Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred."),
      Map.entry("ABANDON", "Partial: Abandon can counter a supported public pending spell chain item in the current alpha chain window, return that spell card to hand, then create a private Predict choice. Repeat, broad official Reaction timing, hidden/private chain targets, and ability targets remain deferred."),
      Map.entry("HARD BARGAIN", "Partial: Hard Bargain can counter a supported public pending spell chain item unless that spell's controller pays 2 energy through an owner-only prompt. Repeat, broad official Reaction timing, ability targets, hidden/private chain targets, and countering counters remain deferred."),
      Map.entry("EN GARDE", "Partial: alpha chain-window Reaction support exists for giving a friendly battlefield Unit/Champion +1 Might, or +2 if it is your only unit there. Full official any-time Reaction timing remains incomplete."),
      Map.entry("DEFIANT DANCE", "Partial: alpha chain-window Reaction support exists for giving one public battlefield Unit/Champion +2 Might and another public battlefield Unit/Champion -2 Might this turn. Full official any-time Reaction timing remains incomplete."),
      Map.entry("FLASH", "Partial: alpha chain-window Reaction support exists for moving up to two friendly battlefield Unit/Champion cards to Base. Full official any-time Reaction timing remains incomplete."),
      Map.entry("CHARM", "Partial: alpha support moves one enemy public battlefield Unit/Champion to Base. Broader official movement choices, control/location edge cases, and non-battlefield destinations remain deferred."),
      Map.entry("THE SYREN", "Partial: The Syren can be played to Base and activated during your Main Phase by paying 1 energy and exhausting it to move a friendly public battlefield Unit/Champion to Base. Broader activated ability timing and ability-chain support remain deferred."),
      Map.entry("ZHONYA'S HOURGLASS", "Partial: Zhonya's Hourglass can be played to Base and armed during your Main Phase to protect a friendly public Unit/Champion from the next supported death, destroying Zhonya instead, then healing, exhausting, and recalling that unit. Hidden Reaction-for-0 timing, competing replacement choices, and broad replacement timing remain deferred."),
      Map.entry("GUST", "Partial: alpha chain-window Reaction support exists through Stacked Deck for returning a battlefield Unit/Champion with 3 Might or less, but full official any-time Reaction timing remains incomplete."),
      Map.entry("STACKED DECK", "Partial: opens the narrow alpha chain, then resolves into a private top-3 choice; official ordering and broader timing remain incomplete."));

  private final CardDataService cardDataService;

  public CardSupportService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public CardSupportSummary summarize(CardDefinition card) {
    if (card == null) {
      return new CardSupportSummary(null, "Unknown card", CardSupportStatus.NOT_AUDITED, "Card data is missing.");
    }
    CardSupportStatus status = statusFor(card);
    return new CardSupportSummary(card.id(), card.name(), status, reasonFor(card, status));
  }

  public List<CardSupportSummary> summarizeDeck(List<CardDefinition> cards) {
    return cards.stream()
        .collect(java.util.stream.Collectors.toMap(CardDefinition::id, this::summarize, (first, ignored) -> first))
        .values()
        .stream()
        .sorted(Comparator.comparing(CardSupportSummary::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public CardSupportStatus statusFor(CardDefinition card) {
    if (card == null || card.id() == null || card.id().isBlank() || card.name() == null || card.name().isBlank()) {
      return CardSupportStatus.NOT_AUDITED;
    }
    if (TournamentLegality.isBannedInConstructed(card)) return CardSupportStatus.BANNED;
    if (hasEffectSupportGate(card) && cardDataService.isUnsupportedAction(card.id())) return CardSupportStatus.UNSUPPORTED;
    if (SUPPORTED_CARD_NAMES.contains(normalize(card.name()))) return CardSupportStatus.SUPPORTED;
    return CardSupportStatus.PARTIAL;
  }

  private boolean hasEffectSupportGate(CardDefinition card) {
    return "Spell".equalsIgnoreCase(card.type()) || "Gear".equalsIgnoreCase(card.type());
  }

  private String reasonFor(CardDefinition card, CardSupportStatus status) {
    return switch (status) {
      case SUPPORTED -> "Implemented and covered by current support policy.";
      case PARTIAL -> PARTIAL_REASONS.getOrDefault(normalize(card.name()), "Playable for alpha testing, but card-specific behavior may be incomplete.");
      case UNSUPPORTED -> "This card's effect is not supported in enforced play yet.";
      case BANNED -> "This card is banned in the current Constructed format.";
      case NOT_AUDITED -> "This card has not been audited for RiftForge support.";
    };
  }

  private String normalize(String value) {
    return value.trim().toUpperCase(Locale.ROOT).replace('’', '\'');
  }
}
