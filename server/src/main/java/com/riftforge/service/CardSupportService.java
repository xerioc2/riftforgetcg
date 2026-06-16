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
      Map.entry("IRELIA - FERVENT", "Partial: Deflect targeting tax is heuristic, and the choose/ready +1 Might trigger is not implemented yet."),
      Map.entry("DISCIPLINE", "Partial: draw 1 and selected +2 Might are helper-backed, but Reaction timing is missing."),
      Map.entry("TIDETURNER", "Partial: Hidden foundation exists, but later hidden play timing and the on-play location swap are not implemented yet."),
      Map.entry("GUARDIAN ANGEL", "Partial: alpha Equip lifecycle is implemented and tested, but exact Calm power payment and official Equip timing remain incomplete."),
      Map.entry("BOOTS OF SWIFTNESS", "Partial: alpha Equip lifecycle is implemented and tested, but exact Chaos power payment and official Equip timing remain incomplete."),
      Map.entry("ABANDONED HALL", "Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely."),
      Map.entry("ADAPTATRON", "Partial: conquer trigger, optional gear kill, and official Buff state are not implemented yet."),
      Map.entry("SCUTTLE CRAB", "Partial: on-play draw and 1v1 private hand reveal Deathknell are implemented, but XP and facedown-card viewing are deferred."),
      Map.entry("DEFY", "Partial: Defy can counter supported public pending spell chain items that cost no more than 4 energy and no more than 1 premium rune during the current alpha chain window. Full official Reaction timing, broad spell/ability targets, and countering counters remain deferred."),
      Map.entry("NOT SO FAST", "Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred."),
      Map.entry("EN GARDE", "Partial: selected friendly +Might is helper-backed, but Reaction timing remains incomplete."),
      Map.entry("GUST", "Partial: selected return-to-hand is helper-backed, but Reaction timing and the 3-or-less-Might target filter remain incomplete."));

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
