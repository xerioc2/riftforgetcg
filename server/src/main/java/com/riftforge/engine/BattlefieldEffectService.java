package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.RuneState;
import com.riftforge.model.ZoneName;
import com.riftforge.rules.BattlefieldLocationRules;
import com.riftforge.service.CardDataService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BattlefieldEffectService {
  private final CardDataService cardDataService;
  private final CombatStatsService combatStatsService;

  public BattlefieldEffectService(CardDataService cardDataService) {
    this(cardDataService, new CombatStatsService(cardDataService));
  }

  @Autowired
  public BattlefieldEffectService(CardDataService cardDataService, CombatStatsService combatStatsService) {
    this.cardDataService = cardDataService;
    this.combatStatsService = combatStatsService;
  }

  public void onConquer(LiveGameState state, String playerId, String locationId) {
    CardDefinition battlefield = battlefieldAtLocation(state, locationId);
    if (battlefield == null) return;
    String normalizedLocation = BattlefieldLocationRules.normalize(locationId);
    if (cardDataService.isSunkenTempleBattlefield(battlefield)) {
      offerSunkenTempleChoice(state, playerId, normalizedLocation, battlefield);
    }
    if (cardDataService.isTargonsPeakBattlefield(battlefield)) {
      state.getPendingEndTurnRuneReadying().merge(playerId, 2, Integer::sum);
      log(state, playerId, "Targon's Peak will ready up to 2 runes at the end of this turn.");
    }
  }

  public void resolveEndTurnRuneReadying(LiveGameState state, String playerId) {
    Integer amount = state.getPendingEndTurnRuneReadying().remove(playerId);
    if (amount == null || amount <= 0) return;
    List<RuneState> readied = state.getRunes().stream()
        .filter(rune -> playerId.equals(rune.getOwnerId()))
        .filter(RuneState::isTapped)
        .sorted(Comparator.comparing(RuneState::getInstanceId, Comparator.nullsLast(String::compareTo)))
        .limit(amount)
        .toList();
    readied.forEach(rune -> rune.setTapped(false));
    if (!readied.isEmpty()) {
      log(state, playerId, "Targon's Peak readied " + readied.size() + " rune(s).");
    }
  }

  private void offerSunkenTempleChoice(LiveGameState state, String playerId, String locationId, CardDefinition battlefield) {
    if (state.getPendingChoice() != null) return;
    boolean hasMightyConqueringUnit = state.getCards().stream()
        .filter(card -> card.getZone() == ZoneName.BATTLEFIELD)
        .filter(card -> playerId.equals(card.getOwnerId()))
        .filter(card -> BattlefieldLocationRules.isAtLocation(card, locationId))
        .anyMatch(card -> combatStatsService.isMighty(state, card, CombatStatsService.CombatContext.IDLE));
    if (!hasMightyConqueringUnit) return;
    state.setPendingChoice(PendingChoice.optionalPayOneDrawOne(
        UUID.randomUUID().toString(),
        playerId,
        battlefield.id(),
        "Pay 1 to draw 1 with Sunken Temple?"));
    log(state, playerId, "Sunken Temple is waiting for an optional payment choice.");
  }

  private CardDefinition battlefieldAtLocation(LiveGameState state, String locationId) {
    List<String> activeLocations = BattlefieldLocationRules.activeLocationIds(state);
    int index = activeLocations.indexOf(BattlefieldLocationRules.normalize(locationId));
    if (index < 0 || index >= state.getPlayers().size()) return null;
    String cardId = state.getPlayers().get(index).getSelectedBattlefieldId();
    if (cardId == null || cardId.isBlank()) return null;
    return cardDataService.getCard(cardId);
  }

  private void log(LiveGameState state, String userId, String text) {
    state.getLog().add(new LiveGameState.LogEntry(
        UUID.randomUUID().toString(),
        Instant.now().toString(),
        userId,
        text));
  }
}
