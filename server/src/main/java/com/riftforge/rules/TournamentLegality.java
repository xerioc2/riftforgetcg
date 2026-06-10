package com.riftforge.rules;

import com.riftforge.model.CardDefinition;
import java.util.Locale;
import java.util.Set;

public final class TournamentLegality {
  public static final Set<String> BANNED_CONSTRUCTED_CARDS = Set.of(
      "called shot",
      "draven, vanquisher",
      "fight or flight",
      "scrapheap");

  public static final Set<String> BANNED_CONSTRUCTED_BATTLEFIELDS = Set.of(
      "dreaming tree",
      "obelisk of power",
      "reaver's row");

  private TournamentLegality() {}

  public static boolean isBannedInConstructed(CardDefinition card) {
    if (card == null || card.name() == null) return false;
    String normalized = normalize(card.name());
    if ("Battlefield".equalsIgnoreCase(card.type())) {
      return BANNED_CONSTRUCTED_BATTLEFIELDS.contains(normalized);
    }
    return BANNED_CONSTRUCTED_CARDS.contains(normalized);
  }

  public static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT)
        .replace('\u2019', '\'')
        .trim();
  }
}
