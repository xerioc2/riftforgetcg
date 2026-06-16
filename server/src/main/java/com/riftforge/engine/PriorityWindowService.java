package com.riftforge.engine;

import com.riftforge.model.CardDefinition;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PriorityWindowService {
  private final CardDataService cardDataService;

  public PriorityWindowService(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public enum PriorityWindowType {
    SPELL_PLAYED,
    REACTION_PLAYED,
    ABILITY_TRIGGERED,
    SHOWDOWN_ACTION,
    TEST_FIXTURE
  }

  public record PriorityWindow(
      PriorityWindowType type,
      String effectKey,
      String chainItemType,
      String visibility,
      boolean counterable,
      boolean targetableOnChain,
      ZoneName sourceZoneBeforeChain,
      String publicDescription,
      String sourceContext) {}

  public Optional<PriorityWindow> openingWindowForPlayedCard(
      LiveGameState state,
      CardDefinition def,
      boolean playedDuringShowdown) {
    if (state == null || def == null || state.getChainState() != null) return Optional.empty();
    if (!cardDataService.isStackedDeckEffect(def)) return Optional.empty();
    return Optional.of(new PriorityWindow(
        playedDuringShowdown ? PriorityWindowType.SHOWDOWN_ACTION : PriorityWindowType.SPELL_PLAYED,
        LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE,
        LiveGameState.ChainItem.TYPE_SPELL,
        LiveGameState.ChainItem.VISIBILITY_PUBLIC,
        true,
        true,
        ZoneName.HAND,
        def.name(),
        playedDuringShowdown ? "SHOWDOWN_ACTION" : "MAIN_ACTION"));
  }

  public Optional<PriorityWindow> reactionWindowFor(CardDefinition def) {
    if (def == null) return Optional.empty();
    if (cardDataService.isGustReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_GUST_RETURN,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          true,
          true,
          ZoneName.HAND,
          def.name(),
          null));
    }
    if (cardDataService.isDefyCounterReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_DEFY_COUNTER,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          false,
          false,
          ZoneName.HAND,
          def.name(),
          null));
    }
    if (cardDataService.isNotSoFastCounterReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_NOT_SO_FAST_COUNTER,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          false,
          false,
          ZoneName.HAND,
          def.name(),
          null));
    }
    return Optional.empty();
  }

  public List<String> relevantPlayers(LiveGameState state, LiveGameState.ChainState chain) {
    if (chain != null && chain.relevantPlayerIds() != null && !chain.relevantPlayerIds().isEmpty()) {
      return chain.relevantPlayerIds();
    }
    if (state == null || state.getPlayers() == null) return List.of();
    return state.getPlayers().stream()
        .map(PlayerState::getUserId)
        .filter(id -> id != null && !id.isBlank())
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  public String nextFocusedPlayerId(List<String> relevant, String currentFocus) {
    if (relevant == null || relevant.isEmpty()) return currentFocus;
    int index = relevant.indexOf(currentFocus);
    return relevant.get((index < 0 ? 0 : index + 1) % relevant.size());
  }
}
