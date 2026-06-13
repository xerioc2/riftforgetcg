package com.riftforge.service;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardSupportStatus;
import com.riftforge.model.CardSupportSummary;
import com.riftforge.rules.TournamentLegality;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
      "VANGUARD CAPTAIN");

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
      case PARTIAL -> "Playable for alpha testing, but card-specific behavior may be incomplete.";
      case UNSUPPORTED -> "This card's effect is not supported in enforced play yet.";
      case BANNED -> "This card is banned in the current Constructed format.";
      case NOT_AUDITED -> "This card has not been audited for RiftForge support.";
    };
  }

  private String normalize(String value) {
    return value.trim().toUpperCase(Locale.ROOT).replace('’', '\'');
  }
}
