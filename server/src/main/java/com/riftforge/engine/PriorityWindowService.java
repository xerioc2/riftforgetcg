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

  public enum ChainOpenReason {
    STACKED_DECK_ACTION,
    SIMPLE_DRAW_ONE_SPELL
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
    Optional<ChainOpenReason> reason = chainOpenReasonForPlayedCard(def);
    if (reason.isEmpty()) return Optional.empty();
    String effectKey = switch (reason.get()) {
      case STACKED_DECK_ACTION -> LiveGameState.ChainItem.EFFECT_STACKED_DECK_PICK_ONE;
      case SIMPLE_DRAW_ONE_SPELL -> LiveGameState.ChainItem.EFFECT_DRAW_1;
    };
    return Optional.of(new PriorityWindow(
        playedDuringShowdown ? PriorityWindowType.SHOWDOWN_ACTION : PriorityWindowType.SPELL_PLAYED,
        effectKey,
        LiveGameState.ChainItem.TYPE_SPELL,
        LiveGameState.ChainItem.VISIBILITY_PUBLIC,
        true,
        true,
        ZoneName.HAND,
        def.name(),
        playedDuringShowdown ? "SHOWDOWN_ACTION" : "MAIN_ACTION"));
  }

  public Optional<ChainOpenReason> chainOpenReasonForPlayedCard(CardDefinition def) {
    if (def == null) return Optional.empty();
    if (cardDataService.isStackedDeckEffect(def)) return Optional.of(ChainOpenReason.STACKED_DECK_ACTION);
    if (isSimplePublicDrawOneSpell(def)) return Optional.of(ChainOpenReason.SIMPLE_DRAW_ONE_SPELL);
    return Optional.empty();
  }

  public boolean shouldOpenReactionWindow(LiveGameState state, CardDefinition def, boolean playedDuringShowdown) {
    return openingWindowForPlayedCard(state, def, playedDuringShowdown).isPresent();
  }

  private boolean isSimplePublicDrawOneSpell(CardDefinition def) {
    if (!"Spell".equalsIgnoreCase(def.type())) return false;
    if (cardDataService.isReactionCard(def)) return false;
    if (cardDataService.isUnsupportedAction(def.id())) return false;
    String text = def.rulesText() == null ? "" : def.rulesText().toLowerCase();
    String normalized = text
        .replace("[action]", "")
        .replaceAll("\\s+", " ")
        .trim();
    return normalized.equals("draw 1") || normalized.equals("draw 1.");
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
    if (cardDataService.isDisciplineReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_DISCIPLINE_BOOST_DRAW,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          true,
          true,
          ZoneName.HAND,
          def.name(),
          null));
    }
    if (cardDataService.isEnGardeReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_EN_GARDE_BOOST,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          true,
          true,
          ZoneName.HAND,
          def.name(),
          null));
    }
    if (cardDataService.isDefiantDanceReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_DEFIANT_DANCE_MODIFIERS,
          LiveGameState.ChainItem.TYPE_SPELL,
          LiveGameState.ChainItem.VISIBILITY_PUBLIC,
          true,
          true,
          ZoneName.HAND,
          def.name(),
          null));
    }
    if (cardDataService.isFlashReaction(def)) {
      return Optional.of(new PriorityWindow(
          PriorityWindowType.REACTION_PLAYED,
          LiveGameState.ChainItem.EFFECT_FLASH_RECALL,
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
