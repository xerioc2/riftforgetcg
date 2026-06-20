package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PendingChoice;
import com.riftforge.model.Phase;
import com.riftforge.model.PlayerState;
import com.riftforge.model.RevealedHandSnapshot;
import com.riftforge.model.RuneState;
import com.riftforge.model.ShowdownStep;
import com.riftforge.model.ZoneName;
import com.riftforge.model.move.ActivateAbilityMove;
import com.riftforge.model.move.AssignCombatDamageMove;
import com.riftforge.model.move.EquipGearMove;
import com.riftforge.model.move.HideCardMove;
import com.riftforge.model.move.MoveToBaseMove;
import com.riftforge.model.move.MoveToBattlefieldMove;
import com.riftforge.model.move.PassChainFocusMove;
import com.riftforge.model.move.PassPhaseMove;
import com.riftforge.model.move.PassShowdownFocusMove;
import com.riftforge.model.move.PlayCardMove;
import com.riftforge.model.move.RepositionCardMove;
import com.riftforge.model.move.ResolveChainTopMove;
import com.riftforge.model.move.ResolveChoiceMove;
import com.riftforge.model.move.ResolveShowdownMove;
import com.riftforge.rules.LegalActionsService;
import com.riftforge.service.CardDataService;
import com.riftforge.service.GameStateProjectionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameEnginePlayCardTypeTest {
  @Mock CardDataService cardDataService;
  @Mock CardEffectRegistry effects;
  GameEngine engine;

  @BeforeEach
  void setUp() {
    RulesValidator rulesValidator = new RulesValidator(cardDataService);
    CardZoneService cardZoneService = new CardZoneService(cardDataService);
    DeathTriggerService deathTriggerService = new DeathTriggerService(cardDataService);
    TokenFactory tokenFactory = new TokenFactory(cardDataService);
    CombatResolver combatResolver = new CombatResolver(cardDataService, effects, cardZoneService, new CombatStatsService(cardDataService), deathTriggerService);
    engine = new GameEngine(rulesValidator, combatResolver, cardZoneService, cardDataService, effects, deathTriggerService, tokenFactory, 8);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenReturn(false);
    when(cardDataService.isActionCard(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[action]");
    });
    when(cardDataService.isReactionCard(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("[reaction]");
    });
    when(cardDataService.isStackedDeckEffect(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().toLowerCase();
      return def != null && "Stacked Deck".equalsIgnoreCase(def.name())
          && text.contains("look at the top 3")
          && text.contains("put 1")
          && text.contains("hand")
          && text.contains("recycle");
    });
    when(cardDataService.isCharmMoveEffect(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().trim().toLowerCase();
      return def != null
          && "Spell".equalsIgnoreCase(def.type())
          && "Charm".equalsIgnoreCase(def.name())
          && text.equals("move an enemy unit.");
    });
    when(cardDataService.isTheSyrenActivatedAbility(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().trim().toLowerCase();
      return def != null
          && "Gear".equalsIgnoreCase(def.type())
          && "The Syren".equalsIgnoreCase(def.name())
          && text.equals(":rb_energy_1:, :rb_exhaust:: move a friendly unit at a battlefield to its base.");
    });
    when(cardDataService.isZhonyasHourglass(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().trim().toLowerCase();
      return def != null
          && "Gear".equalsIgnoreCase(def.type())
          && "Zhonya's Hourglass".equalsIgnoreCase(def.name())
          && text.contains("[hidden]")
          && text.contains("if a friendly unit would die")
          && text.contains("kill this instead")
          && text.contains("heal that unit")
          && text.contains("exhaust it")
          && text.contains("recall it");
    });
    when(cardDataService.isIreliaBladeDancerLegend(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().trim().toLowerCase();
      return def != null
          && "Legend".equalsIgnoreCase(def.type())
          && "Irelia - Blade Dancer".equalsIgnoreCase(def.name())
          && text.contains("when you choose a friendly unit")
          && text.contains("exhaust me")
          && text.contains("ready it")
          && text.contains("when you conquer")
          && text.contains("ready me");
    });
    when(cardDataService.isIreliaFerventChampion(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      String text = def == null || def.rulesText() == null ? "" : def.rulesText().trim().toLowerCase();
      return def != null
          && "Champion".equalsIgnoreCase(def.type())
          && "Irelia - Fervent".equalsIgnoreCase(def.name())
          && text.contains("[deflect]")
          && text.contains("when you choose or ready me")
          && text.contains("+1")
          && text.contains(":rb_might:");
    });
    when(cardDataService.isHiddenCard(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && (
          def.rulesText() != null && def.rulesText().toLowerCase().contains("[hidden]")
          || def.keywords().stream().anyMatch(keyword -> keyword.equalsIgnoreCase("HIDDEN")));
    });
    when(cardDataService.isAmbushCard(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && (
          def.rulesText() != null && def.rulesText().toLowerCase().contains("[ambush]")
          || def.keywords().stream().anyMatch(keyword -> keyword.equalsIgnoreCase("AMBUSH")));
    });
    when(cardDataService.hasUnsupportedAdditionalCost(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && def.rulesText() != null && def.rulesText().toLowerCase().contains("additional cost");
    });
    when(cardDataService.isUnsupportedAction(anyString())).thenReturn(false);
    when(cardDataService.requiresBattlefieldTarget(anyString())).thenReturn(false);
    when(cardDataService.requiresFriendlyTarget(anyString())).thenReturn(false);
    when(cardDataService.requiresEnemyTarget(anyString())).thenReturn(false);
    when(cardDataService.isEquip(any(CardDefinition.class))).thenAnswer(invocation -> {
      CardDefinition def = invocation.getArgument(0);
      return def != null && "Gear".equalsIgnoreCase(def.type()) && def.rulesText() != null && def.rulesText().toLowerCase().contains("[equip]");
    });
  }

  @Test
  void unitCanBePlayedToBase() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, play("unit", ZoneName.BASE));

    CardInstance unit = state.getCards().getFirst();
    assertThat(unit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(unit.isTapped()).isTrue();
    assertThat(unit.isHasSummoningSickness()).isTrue();
  }

  @Test
  void vanguardSergeantBasicStarterUnitCanBePlayedAndMoved() {
    LiveGameState state = state(card("vanguard-sergeant", "p1", ZoneName.HAND));
    stubCard("vanguard-sergeant", "Vanguard Sergeant", "Unit", 0, 2, 3, "");

    engine.applyMove(state, play("vanguard-sergeant", ZoneName.BASE));
    CardInstance sergeant = find(state, "vanguard-sergeant");
    assertThat(sergeant.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(sergeant.isTapped()).isTrue();
    assertThat(sergeant.getCurrentHealth()).isEqualTo(3);

    sergeant.setTapped(false);
    sergeant.setHasSummoningSickness(false);
    engine.applyMove(state, new MoveToBattlefieldMove("p1", "vanguard-sergeant"));

    assertThat(sergeant.getCardId()).isEqualTo("vanguard-sergeant");
    assertThat(sergeant.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Vanguard Sergeant"));
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Moved Vanguard Sergeant to the battlefield."));
  }

  @Test
  void zeroHealthStarterUnitStaysInBaseWhenPlayed() {
    LiveGameState state = state(card("tideturner", "p1", ZoneName.HAND));
    state.getPlayers().getFirst().setAvailableEnergy(2);
    stubCard("tideturner", "Tideturner", "Unit", 2, 2, 0, "[Hidden] When you play me, you may choose a unit you control.");

    engine.applyMove(state, play("tideturner", ZoneName.BASE));

    CardInstance tideturner = state.getCards().getFirst();
    assertThat(tideturner.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(tideturner.getZone()).isNotEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getOwnerId().equals("p1") && card.getZone() == ZoneName.HAND);

    LiveGameState projected = new GameStateProjectionService(new LegalActionsService()).toPublicView(state, "p1");
    assertThat(projected.getCards())
        .anySatisfy(card -> {
          assertThat(card.getCardId()).isEqualTo("tideturner");
          assertThat(card.getZone()).isEqualTo(ZoneName.BASE);
        });
  }

  @Test
  void unitCannotBePlayedDirectlyToBattlefield() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND));
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("unit", ZoneName.BATTLEFIELD)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Non-rune cards must be played to base.");
  }

  @Test
  void ambushUnitCanBePlayedToBattlefieldWhenFriendlyUnitIsPresent() {
    LiveGameState state = state(card("ambusher", "p1", ZoneName.HAND), card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("ambusher", "Ambush Recruit", "Unit", 0, 2, 2, "[Ambush] You may play me to a battlefield.", List.of("AMBUSH"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("ambusher", ZoneName.BATTLEFIELD));

    CardInstance ambusher = find(state, "ambusher");
    assertThat(ambusher.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(ambusher.isTapped()).isFalse();
    assertThat(ambusher.isHasSummoningSickness()).isFalse();
    assertThat(state.getBattlefieldController()).containsEntry(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID, "p1");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Ambushed Ambush Recruit to the battlefield."));
  }

  @Test
  void ambushUnitCannotBePlayedToBattlefieldWithoutFriendlyUnit() {
    LiveGameState state = state(card("ambusher", "p1", ZoneName.HAND));
    stubCard("ambusher", "Ambush Recruit", "Unit", 0, 2, 2, "[Ambush] You may play me to a battlefield.", List.of("AMBUSH"));

    assertThatThrownBy(() -> engine.applyMove(state, play("ambusher", ZoneName.BATTLEFIELD)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Ambush requires a friendly unit at that battlefield.");
  }

  @Test
  void ambushPlayStartsShowdownIfOpponentHasBattlefieldUnit() {
    LiveGameState state = state(
        card("ambusher", "p1", ZoneName.HAND),
        card("friendly", "p1", ZoneName.BATTLEFIELD),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("ambusher", "Ambush Recruit", "Unit", 0, 2, 2, "[Ambush] You may play me to a battlefield.", List.of("AMBUSH"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 1, 1, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("ambusher", ZoneName.BATTLEFIELD));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackingPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("ambusher");
  }

  @Test
  void ambushDoesNotWorkOutsideSupportedTimingWindow() {
    LiveGameState state = state(card("ambusher", "p1", ZoneName.HAND), card("friendly", "p1", ZoneName.BATTLEFIELD));
    state.setCurrentPhase(Phase.DRAW);
    stubCard("ambusher", "Ambush Recruit", "Unit", 0, 2, 2, "[Ambush] You may play me to a battlefield.", List.of("AMBUSH"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 1, 1, null);

    assertThatThrownBy(() -> engine.applyMove(state, play("ambusher", ZoneName.BATTLEFIELD)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Ambush reaction timing is not implemented yet.");
  }

  @Test
  void ambushCardWithUnsupportedAdditionalCostIsBlocked() {
    LiveGameState state = state(card("stalking-wolf", "p1", ZoneName.HAND), card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("stalking-wolf", "Stalking Wolf", "Unit", 0, 2, 2, "[Ambush] As an additional cost to play me, kill a Poro you control.", List.of("AMBUSH"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 1, 1, null);

    assertThatThrownBy(() -> engine.applyMove(state, play("stalking-wolf", ZoneName.BATTLEFIELD)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's additional cost is not supported yet.");
  }

  @Test
  void spellResolvesAndMovesToDiscard() {
    LiveGameState state = state(card("spell", "p1", ZoneName.HAND));
    stubCard("spell", "Spell", 0);

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void drawOneDrawsExactlyOneCard() {
    LiveGameState state = state(card("draw-spell", "p1", ZoneName.HAND));
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one", "drawn-two")));
    stubCard("draw-spell", "Draw Spell", "Spell", 0, 0, 0, "Draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);
    stubCard("drawn-two", "Drawn Two", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("draw-spell", ZoneName.BASE));

    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("drawn-one") && card.getZone() == ZoneName.HAND);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-two"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-two");
    assertThat(state.getCards().stream().filter(card -> card.getOwnerId().equals("p1") && card.getZone() == ZoneName.HAND)).hasSize(1);
  }

  @Test
  void scuttleCrabZeroMightDoesNotAutoTrashAndCanMoveToBattlefield() {
    CardInstance scuttle = card("scuttle", "p1", ZoneName.HAND);
    LiveGameState state = state(scuttle);
    stubCard("scuttle", "Scuttle Crab", "Unit", 0, 0, 2, "When you play me, draw 1. [Deathknell] Choose an opponent.", List.of("DEATHKNELL"));

    engine.applyMove(state, play("scuttle", ZoneName.BASE));

    assertThat(scuttle.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(scuttle.getCurrentHealth()).isEqualTo(2);

    scuttle.setTapped(false);
    engine.applyMove(state, new MoveToBattlefieldMove("p1", "scuttle"));

    assertThat(scuttle.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(scuttle.getCurrentHealth()).isEqualTo(2);
  }

  @Test
  void scuttleCrabOnPlayDrawsOneWithoutPubliclyLoggingDrawnCardName() {
    LiveGameState state = state(card("scuttle", "p1", ZoneName.HAND));
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("secret-drawn", "next-card")));
    stubCard("scuttle", "Scuttle Crab", "Unit", 0, 0, 2, "When you play me, draw 1. [Deathknell] Choose an opponent.", List.of("DEATHKNELL"));
    stubCard("secret-drawn", "Secret Drawn Card", "Unit", 0, 1, 1, null);
    stubCard("next-card", "Next Card", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("scuttle", ZoneName.BASE));

    assertThat(state.getCards()).anyMatch(card -> card.getOwnerId().equals("p1")
        && card.getCardId().equals("secret-drawn")
        && card.getZone() == ZoneName.HAND);
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("next-card");
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains("Drew a card.")
        .doesNotContain("Drew Secret Drawn Card.");
  }

  @Test
  void disarmingRakeEntersPlayWithoutPromptWhenNoGearExists() {
    LiveGameState state = state(card("rake", "p1", ZoneName.HAND));
    stubCard("rake", "Disarming Rake", "Unit", 0, 1, 2, "When you play me, you may kill a gear.");

    engine.applyMove(state, play("rake", ZoneName.BASE));

    assertThat(find(state, "rake").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getPendingChoice()).isNull();
  }

  @Test
  void disarmingRakeCreatesOwnerOnlyGearDestroyPromptWhenGearExists() {
    LiveGameState state = state(card("rake", "p1", ZoneName.HAND), card("gear", "p2", ZoneName.BASE));
    stubCard("rake", "Disarming Rake", "Unit", 0, 1, 2, "When you play me, you may kill a gear.");
    stubCard("gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    engine.applyMove(state, play("rake", ZoneName.BASE));

    PendingChoice choice = state.getPendingChoice();
    assertThat(choice).isNotNull();
    assertThat(choice.getPlayerId()).isEqualTo("p1");
    assertThat(choice.getType()).isEqualTo(PendingChoice.TYPE_YES_NO);
    assertThat(choice.getEffect()).isEqualTo(PendingChoice.EFFECT_CREATE_DESTROY_GEAR_CHOICE);
    assertThat(choice.isPublicChoice()).isFalse();
  }

  @Test
  void disarmingRakeDeclineLeavesGearUnchanged() {
    LiveGameState state = disarmingRakeState(card("gear", "p2", ZoneName.BASE));
    stubCard("gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    engine.applyMove(state, play("rake", ZoneName.BASE));

    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_NO));

    assertThat(state.getPendingChoice()).isNull();
    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.BASE);
  }

  @Test
  void disarmingRakeYesThenLegalFriendlyGearTargetDestroysGear() {
    LiveGameState state = disarmingRakeState(card("gear", "p1", ZoneName.BASE));
    stubCard("gear", "Friendly Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    engine.applyMove(state, play("rake", ZoneName.BASE));
    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));

    PendingChoice targetChoice = state.getPendingChoice();
    assertThat(targetChoice.getType()).isEqualTo(PendingChoice.TYPE_TARGET_GEAR);
    engine.applyMove(state, resolveTarget(targetChoice, "gear"));

    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(find(state, "gear").getAttachedToInstanceId()).isNull();
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Disarming Rake destroyed Friendly Gear."));
  }

  @Test
  void disarmingRakeCanDestroyEnemyGear() {
    LiveGameState state = disarmingRakeState(card("gear", "p2", ZoneName.BASE));
    stubCard("gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    engine.applyMove(state, play("rake", ZoneName.BASE));
    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));

    engine.applyMove(state, resolveTarget(state.getPendingChoice(), "gear"));

    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.DISCARD);
  }

  @Test
  void disarmingRakeDestroysAttachedGearWithoutReturningItToBase() {
    CardInstance gear = card("gear", "p2", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = disarmingRakeState(gear, card("host", "p2", ZoneName.BATTLEFIELD));
    stubCard("gear", "Attached Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("host", "Host Unit", "Unit", 0, 2, 2, null);
    engine.applyMove(state, play("rake", ZoneName.BASE));
    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));

    engine.applyMove(state, resolveTarget(state.getPendingChoice(), "gear"));

    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(find(state, "gear").getAttachedToInstanceId()).isNull();
    assertThat(find(state, "host").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().equals("Attached Gear returned to Base."));
  }

  @Test
  void disarmingRakeRejectsNonGearTargetWithoutPartialMutation() {
    LiveGameState state = disarmingRakeState(card("gear", "p2", ZoneName.BASE), card("unit", "p2", ZoneName.BATTLEFIELD));
    stubCard("gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("unit", "Enemy Unit", "Unit", 0, 2, 2, null);
    engine.applyMove(state, play("rake", ZoneName.BASE));
    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));
    PendingChoice targetChoice = state.getPendingChoice();

    assertThatThrownBy(() -> engine.applyMove(state, resolveTarget(targetChoice, "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Choose a public Gear in play.");

    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getPendingChoice()).isEqualTo(targetChoice);
  }

  @Test
  void disarmingRakeRejectsHiddenHandDeckAndTrashGearTargets() {
    CardInstance hidden = card("hidden-gear", "p2", ZoneName.BASE);
    hidden.setFaceDown(true);
    LiveGameState state = disarmingRakeState(
        card("legal-gear", "p2", ZoneName.BASE),
        hidden,
        card("hand-gear", "p2", ZoneName.HAND),
        card("deck-gear", "p2", ZoneName.DECK),
        card("trash-gear", "p2", ZoneName.DISCARD));
    stubCard("legal-gear", "Legal Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("hidden-gear", "Hidden Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("hand-gear", "Hand Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("deck-gear", "Deck Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("trash-gear", "Trash Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    engine.applyMove(state, play("rake", ZoneName.BASE));
    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));
    PendingChoice targetChoice = state.getPendingChoice();

    for (String invalidTarget : List.of("hidden-gear", "hand-gear", "deck-gear", "trash-gear")) {
      assertThatThrownBy(() -> engine.applyMove(state, resolveTarget(targetChoice, invalidTarget)))
          .isInstanceOf(IllegalMoveException.class)
          .hasMessage("Choose a public Gear in play.");
    }
    assertThat(find(state, "legal-gear").getZone()).isEqualTo(ZoneName.BASE);
  }

  @Test
  void wrongPlayerCannotResolveDisarmingRakePromptOrTarget() {
    LiveGameState state = disarmingRakeState(card("gear", "p2", ZoneName.BASE));
    stubCard("gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    engine.applyMove(state, play("rake", ZoneName.BASE));

    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p2", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That choice belongs to another player.");

    engine.applyMove(state, new ResolveChoiceMove("p1", state.getPendingChoice().getChoiceId(), PendingChoice.OPTION_YES));
    assertThatThrownBy(() -> engine.applyMove(state, new ResolveChoiceMove("p2", state.getPendingChoice().getChoiceId(), null, null, null, "gear", List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That choice belongs to another player.");
    assertThat(find(state, "gear").getZone()).isEqualTo(ZoneName.BASE);
  }

  @Test
  void friendlyTargetAcceptedForFriendlyUnitEffect() {
    CardInstance spell = card("buff", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, friendly, enemy);
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a friendly unit +2 :rb_might: this turn.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("buff")).thenReturn(true);

    engine.applyMove(state, playTarget("buff", "friendly"));

    assertThat(friendly.getTemporaryPowerModifier()).isEqualTo(2);
    assertThat(enemy.getTemporaryPowerModifier()).isZero();
  }

  @Test
  void enemyTargetAcceptedForEnemyUnitEffect() {
    CardInstance spell = card("bounce", "p1", ZoneName.HAND);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, enemy);
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "enemy"));

    assertThat(enemy.getZone()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void charmMovesSelectedEnemyBattlefieldUnitToBase() {
    CardInstance spell = card("charm", "p1", ZoneName.HAND);
    CardInstance enemy = atLocation(card("enemy", "p2", ZoneName.BATTLEFIELD), "bf-1");
    LiveGameState state = state(spell, enemy);
    stubCard("charm", "Charm", "Spell", 0, 0, 0, "Move an enemy unit.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);

    engine.applyMove(state, playTarget("charm", "enemy"));

    assertThat(enemy.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(enemy.getBattlefieldLocationId()).isNull();
    assertThat(spell.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Charm moved Enemy Unit to Base."));
  }

  @Test
  void charmRejectsFriendlyOrNonPublicBattlefieldTargetsWithoutMutation() {
    CardInstance spell = card("charm", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance enemyBase = card("enemy-base", "p2", ZoneName.BASE);
    CardInstance hiddenEnemy = card("hidden-enemy", "p2", ZoneName.HIDDEN);
    CardInstance enemyGear = card("enemy-gear", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, friendly, enemyBase, hiddenEnemy, enemyGear);
    stubCard("charm", "Charm", "Spell", 0, 0, 0, "Move an enemy unit.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy-base", "Enemy Base Unit", "Unit", 0, 2, 2, null);
    stubCard("hidden-enemy", "Hidden Enemy", "Unit", 0, 2, 2, null);
    stubCard("enemy-gear", "Enemy Gear", "Gear", 0, 0, 0, "[Equip]");

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("charm", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Charm can only target an enemy Unit or Champion.");
    assertThatThrownBy(() -> engine.applyMove(state, playTarget("charm", "enemy-base")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Charm can only target a public enemy Unit or Champion at a battlefield.");
    assertThatThrownBy(() -> engine.applyMove(state, playTarget("charm", "hidden-enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Charm can only target a public enemy Unit or Champion at a battlefield.");
    assertThatThrownBy(() -> engine.applyMove(state, playTarget("charm", "enemy-gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Charm can only target a public enemy Unit or Champion at a battlefield.");

    assertThat(spell.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(friendly.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(enemyBase.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(hiddenEnemy.getZone()).isEqualTo(ZoneName.HIDDEN);
    assertThat(enemyGear.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void charmPaymentFailureDoesNotMoveTarget() {
    CardInstance spell = card("charm", "p1", ZoneName.HAND);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, enemy);
    state.getPlayers().getFirst().setAvailableEnergy(1);
    stubCard("charm", "Charm", "Spell", 2, 0, 0, "Move an enemy unit.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("charm", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient energy.");

    assertThat(spell.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(enemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isEqualTo(1);
  }

  @Test
  void returnEffectUsesSelectedTargetInsteadOfFirstValidTarget() {
    CardInstance firstEnemy = card("first-enemy", "p2", ZoneName.BATTLEFIELD);
    CardInstance selectedEnemy = card("selected-enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("bounce", "p1", ZoneName.HAND), firstEnemy, selectedEnemy);
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("first-enemy", "First Enemy", "Unit", 0, 2, 2, null);
    stubCard("selected-enemy", "Selected Enemy", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "selected-enemy"));

    assertThat(firstEnemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(selectedEnemy.getZone()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void championUnitReturnedToHandCanBePlayedAgain() {
    CardInstance gust = card("gust", "p1", ZoneName.HAND);
    CardInstance champion = card("champion", "p2", ZoneName.BATTLEFIELD);
    CardInstance gear = card("gear", "p2", ZoneName.BATTLEFIELD);
    gear.setAttachedToInstanceId("champion");
    LiveGameState state = state(gust, champion, gear);
    stubCard("gust", "Gust", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("champion", "Champion Unit", "Champion", 0, 4, 5, "[DEATHKNELL] Supported death effect.");
    stubCard("gear", "Attached Gear", "Gear", 0, 0, 0, "[Equip]");
    when(cardDataService.requiresBattlefieldTarget("gust")).thenReturn(true);
    state.getPlayers().get(1).getDeckPool().add("drawn-card");

    engine.applyMove(state, playTarget("gust", "champion"));

    assertThat(champion.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getPlayers().get(1).getDeckPool()).containsExactly("drawn-card");
    LiveGameState opponentView = new GameStateProjectionService(new LegalActionsService())
        .toPublicView(state, "p1");
    assertThat(find(opponentView, "champion").getCardId()).isEqualTo(GameStateProjectionService.HIDDEN_CARD_ID);

    state.setActivePlayerId("p2");
    engine.applyMove(state, play("p2", "champion", ZoneName.BASE));

    assertThat(champion.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(champion.getCurrentHealth()).isEqualTo(5);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Champion Unit"));
  }

  @Test
  void multiTargetReturnResolvesFriendlyAndEnemyTargets() {
    CardInstance spell = card("team-bounce", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, friendly, enemy);
    stubCard("team-bounce", "Team Bounce", "Spell", 0, 0, 0, "Return a friendly unit and an enemy unit to their owners' hands.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresFriendlyAndEnemyTargets("team-bounce")).thenReturn(true);

    engine.applyMove(state, playMultiTarget("team-bounce", "friendly", "enemy"));

    assertThat(friendly.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(enemy.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(spell.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Team Bounce returned a friendly unit and an enemy unit to hand."));
  }

  @Test
  void multiTargetReturnRequiresBothRolesAndDoesNotSpendOnFailure() {
    CardInstance spell = card("team-bounce", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, friendly);
    state.getPlayers().getFirst().setAvailableEnergy(2);
    stubCard("team-bounce", "Team Bounce", "Spell", 2, 0, 0, "Return a friendly unit and an enemy unit to their owners' hands.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresFriendlyAndEnemyTargets("team-bounce")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, new PlayCardMove(
        "p1",
        "team-bounce",
        ZoneName.BASE,
        0,
        0,
        null,
        List.of(new PlayCardMove.TargetSelection(PlayCardMove.TargetSelection.FRIENDLY_UNIT, "friendly")),
        false,
        List.of(),
        List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("This card requires a friendly target and an enemy target.");

    assertThat(spell.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(friendly.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isEqualTo(2);
  }

  @Test
  void multiTargetReturnValidatesControllersAndPublicCombatants() {
    CardInstance spell = card("team-bounce", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    CardInstance gear = card("gear", "p2", ZoneName.BATTLEFIELD);
    CardInstance hiddenEnemy = card("hidden-enemy", "p2", ZoneName.HIDDEN);
    LiveGameState state = state(spell, friendly, enemy, gear, hiddenEnemy);
    stubCard("team-bounce", "Team Bounce", "Spell", 0, 0, 0, "Return a friendly unit and an enemy unit to their owners' hands.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    stubCard("gear", "Gear", "Gear", 0, 0, 0, null);
    stubCard("hidden-enemy", "Hidden Enemy", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresFriendlyAndEnemyTargets("team-bounce")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playMultiTarget("team-bounce", "enemy", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Friendly target must be controlled by you.");
    assertThatThrownBy(() -> engine.applyMove(state, playMultiTarget("team-bounce", "friendly", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Targets must be different cards.");
    assertThatThrownBy(() -> engine.applyMove(state, playMultiTarget("team-bounce", "friendly", "gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be a Unit or Champion.");
    assertThatThrownBy(() -> engine.applyMove(state, playMultiTarget("team-bounce", "friendly", "hidden-enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be a public Unit or Champion.");

    assertThat(spell.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(friendly.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(enemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void multiTargetReturnCleansAttachmentsAndDoesNotTriggerDeathknell() {
    CardInstance spell = card("team-bounce", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance enemy = card("loyal", "p2", ZoneName.BATTLEFIELD);
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    gear.setAttachedToInstanceId("friendly");
    LiveGameState state = state(spell, friendly, enemy, gear);
    state.getPlayers().get(1).setDeckPool(new ArrayList<>(List.of("drawn")));
    stubCard("team-bounce", "Team Bounce", "Spell", 0, 0, 0, "Return a friendly unit and an enemy unit to their owners' hands.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("loyal", "Loyal Poro", "Unit", 0, 1, 1, "[Deathknell] Draw 1 if I did not die alone.");
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("drawn", "Drawn Card", "Unit", 0, 1, 1, null);
    when(cardDataService.requiresFriendlyAndEnemyTargets("team-bounce")).thenReturn(true);
    when(cardDataService.hasKeyword("loyal", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, playMultiTarget("team-bounce", "friendly", "loyal"));

    assertThat(friendly.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(enemy.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
  }

  @Test
  void readyEffectUsesSelectedFriendlyTargetInsteadOfFirstValidTarget() {
    CardInstance firstFriendly = card("first-friendly", "p1", ZoneName.BATTLEFIELD);
    CardInstance selectedFriendly = card("selected-friendly", "p1", ZoneName.BATTLEFIELD);
    firstFriendly.setTapped(true);
    selectedFriendly.setTapped(true);
    LiveGameState state = state(card("ready-spell", "p1", ZoneName.HAND), firstFriendly, selectedFriendly);
    stubCard("ready-spell", "Ready Spell", "Spell", 0, 0, 0, "Choose a friendly unit. Ready it.");
    stubCard("first-friendly", "First Friendly", "Unit", 0, 2, 2, null);
    stubCard("selected-friendly", "Selected Friendly", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("ready-spell")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("ready-spell")).thenReturn(true);

    engine.applyMove(state, playTarget("ready-spell", "selected-friendly"));

    assertThat(firstFriendly.isTapped()).isTrue();
    assertThat(selectedFriendly.isTapped()).isFalse();
  }

  @Test
  void readyEffectRejectsEnemyTarget() {
    LiveGameState state = state(
        card("ready-spell", "p1", ZoneName.HAND),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("ready-spell", "Ready Spell", "Spell", 0, 0, 0, "Choose a friendly unit. Ready it.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("ready-spell")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("ready-spell")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("ready-spell", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card requires a friendly unit.");
  }

  @Test
  void deathknellDoesNotFireWhenUnitReturnsToHand() {
    CardInstance spell = card("bounce", "p1", ZoneName.HAND);
    CardInstance loyalPoro = card("loyal", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, loyalPoro);
    state.getPlayers().get(1).setDeckPool(new ArrayList<>(List.of("drawn")));
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("loyal", "Loyal Poro", "Unit", 0, 1, 1, "[Deathknell] Draw 1 if I did not die alone.");
    stubCard("drawn", "Drawn Card", "Unit", 0, 1, 1, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);
    when(cardDataService.hasKeyword("loyal", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "loyal"));

    assertThat(loyalPoro.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
  }

  @Test
  void lonelyPoroDeathknellDoesNotFireWhenReturnedToHand() {
    CardInstance spell = card("bounce", "p1", ZoneName.HAND);
    CardInstance lonelyPoro = card("lonely", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(spell, lonelyPoro);
    state.getPlayers().get(1).setDeckPool(new ArrayList<>(List.of("drawn")));
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("lonely", "Lonely Poro", "Unit", 0, 1, 1, "[Deathknell] Draw 1 if I died alone.");
    stubCard("drawn", "Drawn Card", "Unit", 0, 1, 1, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);
    when(cardDataService.hasKeyword("lonely", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "lonely"));

    assertThat(lonelyPoro.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
  }

  @Test
  void enemyTargetRejectedForFriendlyOnlyEffect() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a friendly unit +2 :rb_might: this turn.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card requires a friendly unit.");
  }

  @Test
  void friendlyTargetRejectedForEnemyOnlyEffect() {
    LiveGameState state = state(
        card("bounce", "p1", ZoneName.HAND),
        card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);
    when(cardDataService.requiresEnemyTarget("bounce")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("bounce", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card requires an enemy unit.");
  }

  @Test
  void nonUnitTargetRejectedWhenUnitRequired() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("gear", "p1", ZoneName.BATTLEFIELD));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +2 :rb_might: this turn.");
    stubCard("gear", "Gear", 0);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be a Unit or Champion.");
  }

  @Test
  void hiddenZonesCannotBeTargeted() {
    LiveGameState state = state(
        card("buff", "p1", ZoneName.HAND),
        card("hidden", "p2", ZoneName.HAND));
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +2 :rb_might: this turn.");
    stubCard("hidden", "Hidden Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("buff", "hidden")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Target must be on the battlefield.");
  }

  @Test
  void hiddenCardCanBeHiddenFromHandByTappingRune() {
    CardInstance tideturner = card("tideturner", "p1", ZoneName.HAND);
    LiveGameState state = state(tideturner);
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    stubCard("tideturner", "Tideturner", "Unit", 3, 2, 2, "[Hidden] Hide now for a rune to react with later.");

    engine.applyMove(state, new HideCardMove("p1", "tideturner", "rune-1"));

    assertThat(tideturner.getZone()).isEqualTo(ZoneName.HIDDEN);
    assertThat(tideturner.isFaceDown()).isTrue();
    assertThat(tideturner.isTapped()).isFalse();
    assertThat(state.getRunes().getFirst().isTapped()).isTrue();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Hid a card."));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Tideturner"));
  }

  @Test
  void onlyHiddenCardsCanBeHidden() {
    LiveGameState state = state(card("unit", "p1", ZoneName.HAND));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    stubCard("unit", "Normal Unit", "Unit", 1, 1, 1, null);

    assertThatThrownBy(() -> engine.applyMove(state, new HideCardMove("p1", "unit", "rune-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only cards with Hidden can be hidden.");
  }

  @Test
  void cannotHideOpponentCard() {
    LiveGameState state = state(card("tideturner", "p2", ZoneName.HAND));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    stubCard("tideturner", "Tideturner", "Unit", 3, 2, 2, "[Hidden] Hide now.");

    assertThatThrownBy(() -> engine.applyMove(state, new HideCardMove("p1", "tideturner", "rune-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You do not own that card.");
  }

  @Test
  void cannotHideCardOutsideHand() {
    LiveGameState state = state(card("tideturner", "p1", ZoneName.BASE));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    stubCard("tideturner", "Tideturner", "Unit", 3, 2, 2, "[Hidden] Hide now.");

    assertThatThrownBy(() -> engine.applyMove(state, new HideCardMove("p1", "tideturner", "rune-1")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only cards in hand can be hidden.");
  }

  @Test
  void hiddenCardRequiresReadyOwnRune() {
    LiveGameState state = state(card("tideturner", "p1", ZoneName.HAND));
    state.setRunes(new ArrayList<>(List.of(rune("enemy-rune", "p2", false), rune("tapped-rune", "p1", true))));
    stubCard("tideturner", "Tideturner", "Unit", 3, 2, 2, "[Hidden] Hide now.");

    assertThatThrownBy(() -> engine.applyMove(state, new HideCardMove("p1", "tideturner", "enemy-rune")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You cannot pay with an opponent's rune.");
    assertThatThrownBy(() -> engine.applyMove(state, new HideCardMove("p1", "tideturner", "tapped-rune")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Payment rune is already tapped.");
  }

  @Test
  void hiddenCardCannotMoveToBattlefieldThroughNormalMovement() {
    LiveGameState state = state(card("tideturner", "p1", ZoneName.HIDDEN));
    stubCard("tideturner", "Tideturner", "Unit", 3, 2, 2, "[Hidden] Hide now.");

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "tideturner")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Hidden cards cannot move to a battlefield lane this way.");
  }

  @Test
  void hiddenCardCannotFightOrTriggerDeathknellWhenHidden() {
    CardInstance hidden = card("loyal", "p1", ZoneName.HIDDEN);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(hidden, enemy);
    state.setActiveShowdown(readyShowdown("p1"));
    stubCard("loyal", "Loyal Poro", "Unit", 0, 1, 1, "[Hidden] [Deathknell] Draw 1.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 1, 1, null);
    when(cardDataService.hasKeyword("loyal", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, new ResolveShowdownMove("p1"));

    assertThat(hidden.getZone()).isEqualTo(ZoneName.HIDDEN);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
  }

  @Test
  void unsupportedMultiTargetCardRemainsBlocked() {
    LiveGameState state = state(card("multi", "p1", ZoneName.HAND), card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("multi", "Multi Spell", "Spell", 0, 0, 0, "Choose a friendly unit and an enemy unit.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.isUnsupportedAction("multi")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("multi", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's effect is not supported yet.");
  }

  @Test
  void counterChainCardRemainsBlocked() {
    LiveGameState state = state(card("counter", "p1", ZoneName.HAND));
    stubCard("counter", "Counter Spell", "Spell", 0, 0, 0, "Counter target spell.");
    when(cardDataService.isUnsupportedAction("counter")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, play("counter", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's effect is not supported yet.");
  }

  @Test
  void reactionCardCannotBePlayedOutsideCurrentLegalActionWindow() {
    LiveGameState state = state(card("reaction-draw", "p1", ZoneName.HAND));
    state.setCurrentPhase(Phase.AWAKEN);
    stubCard("reaction-draw", "Reaction Draw", "Spell", 0, 0, 0, "Reaction: Draw 1.");

    assertThatThrownBy(() -> engine.applyMove(state, play("reaction-draw", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That action can only be taken during MAIN.");
  }

  @Test
  void actionCardCanBePlayedDuringShowdownAndDoesNotResolveIt() {
    CardInstance action = card("ride", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    friendly.setTapped(true);
    LiveGameState state = state(action, friendly, card("enemy", "p2", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(focusedShowdown("p1", 1, false));
    stubCard("ride", "Ride The Wind", "Spell", 0, 0, 0, "[Action] Choose a friendly unit. Ready it.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("ride")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("ride")).thenReturn(true);

    engine.applyMove(state, playTarget("ride", "friendly"));

    assertThat(friendly.isTapped()).isFalse();
    assertThat(action.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getActiveShowdown().consecutivePasses()).isZero();
    assertThat(state.getActiveShowdown().readyToResolve()).isFalse();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Ride The Wind during the showdown."));
  }

  @Test
  void actionCreatedPendingChoiceDuringShowdownBlocksOtherMovesAndKeepsFocusCoherent() {
    CardInstance action = card("stacked", "p1", ZoneName.HAND);
    LiveGameState state = state(action, card("attacker", "p1", ZoneName.BATTLEFIELD), card("defender", "p2", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(focusedShowdown("p1", 1, false));
    state.getPlayers().stream()
        .filter(player -> player.getUserId().equals("p1"))
        .findFirst()
        .orElseThrow()
        .getDeckPool()
        .addAll(List.of("top-1", "top-2", "top-3"));
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, "[Action] Look at the top 3 cards of your Main Deck. Put 1 of them into your hand and recycle the rest.");
    stubCard("attacker", "Attacker Unit", "Unit", 0, 2, 2, null);
    stubCard("defender", "Defender Unit", "Unit", 0, 2, 2, null);
    stubCard("top-1", "Top One", "Unit", 0, 1, 1, null);
    stubCard("top-2", "Top Two", "Unit", 0, 1, 1, null);
    stubCard("top-3", "Top Three", "Unit", 0, 1, 1, null);
    when(cardDataService.isActionCard(cardDataService.getCard("stacked"))).thenReturn(true);
    when(cardDataService.isReactionCard(cardDataService.getCard("stacked"))).thenReturn(false);

    engine.applyMove(state, play("stacked", ZoneName.BASE));
    assertThat(state.getPendingChoice()).isNull();
    assertThat(state.getChainState()).isNotNull();
    assertThat(state.getChainState().focusedPlayerId()).isEqualTo("p2");

    engine.applyMove(state, new PassChainFocusMove("p2"));
    engine.applyMove(state, new PassChainFocusMove("p1"));
    engine.applyMove(state, new ResolveChainTopMove("p1"));

    assertThat(state.getPendingChoice()).isNotNull();
    assertThat(state.getPendingChoice().getPlayerId()).isEqualTo("p1");
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().focusedPlayerId()).isEqualTo("p2");
    assertThat(state.getActiveShowdown().consecutivePasses()).isZero();

    assertThatThrownBy(() -> engine.applyMove(state, new PassShowdownFocusMove("p2")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the pending choice before taking another action.");

    GameStateProjectionService projectionService = new GameStateProjectionService(new LegalActionsService(cardDataService));
    assertThat(projectionService.toPublicView(state, "p2").getPendingChoice()).isNull();
    assertThat(projectionService.toPublicView(state, "p1").getPendingChoice()).isNotNull();
  }

  @Test
  void defenderActionCardCanBePlayedDuringShowdownAndDoesNotResolveIt() {
    CardInstance action = card("ride", "p2", ZoneName.HAND);
    CardInstance defender = card("defender", "p2", ZoneName.BATTLEFIELD);
    defender.setTapped(true);
    LiveGameState state = state(action, defender, card("attacker", "p1", ZoneName.BATTLEFIELD));
    state.setActiveShowdown(focusedShowdown("p2", false));
    stubCard("ride", "Ride The Wind", "Spell", 0, 0, 0, "[Action] Choose a friendly unit. Ready it.");
    stubCard("defender", "Defender Unit", "Unit", 0, 2, 2, null);
    stubCard("attacker", "Attacker Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("ride")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("ride")).thenReturn(true);

    engine.applyMove(state, playTarget("p2", "ride", "defender"));

    assertThat(defender.isTapped()).isFalse();
    assertThat(action.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Ride The Wind during the showdown."));
  }

  @Test
  void nonParticipantCannotPlayActionDuringShowdown() {
    CardInstance action = card("ride", "p3", ZoneName.HAND);
    LiveGameState state = state(action, card("attacker", "p1", ZoneName.BATTLEFIELD), card("defender", "p2", ZoneName.BATTLEFIELD));
    PlayerState p3 = new PlayerState();
    p3.setUserId("p3");
    p3.setName("Player Three");
    state.getPlayers().add(p3);
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    stubCard("ride", "Ride The Wind", "Spell", 0, 0, 0, "[Action] Choose a friendly unit. Ready it.");
    stubCard("attacker", "Attacker Unit", "Unit", 0, 2, 2, null);
    stubCard("defender", "Defender Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, play("p3", "ride", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only showdown participants can play Action cards here.");
  }

  @Test
  void nonActionCardCannotBePlayedDuringShowdown() {
    LiveGameState state = state(card("spell", "p1", ZoneName.HAND));
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    stubCard("spell", "Main Spell", "Spell", 0, 0, 0, "Draw 1.");

    assertThatThrownBy(() -> engine.applyMove(state, play("spell", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only supported Action cards can be played during this showdown window.");
  }

  @Test
  void reactionCardCannotBePlayedDuringShowdown() {
    LiveGameState state = state(card("reaction", "p1", ZoneName.HAND));
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    stubCard("reaction", "Reaction Spell", "Spell", 0, 0, 0, "[Reaction] Draw 1.");

    assertThatThrownBy(() -> engine.applyMove(state, play("reaction", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That Reaction's effect is not supported yet.");
  }

  @Test
  void unsupportedActionCardCannotBePlayedDuringShowdown() {
    LiveGameState state = state(card("stacked", "p1", ZoneName.HAND));
    state.setActiveShowdown(new LiveGameState.ShowdownState("p1", List.of("attacker"), Map.of()));
    stubCard("stacked", "Stacked Deck", "Spell", 0, 0, 0, "[Action] Look at the top 3 cards of your Main Deck.");
    when(cardDataService.isUnsupportedAction("stacked")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, play("stacked", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's effect is not supported yet.");
  }

  @Test
  void equipGearFromHandPlaysToBaseWithoutTarget() {
    CardInstance gear = card("equip", "p1", ZoneName.HAND);
    LiveGameState state = state(gear);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    engine.applyMove(state, play("equip", ZoneName.BASE));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Equip Gear to Base."));
  }

  @Test
  void gearFromHandCannotAttachImmediately() {
    LiveGameState state = state(
        card("equip", "p1", ZoneName.HAND),
        card("friendly", "p1", ZoneName.BATTLEFIELD));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, playTarget("equip", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Play Equipment to Base first, then equip it from Base.");
  }

  @Test
  void equipCanAttachFriendlyChampion() {
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    CardInstance champion = card("champion", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(gear, champion);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("champion", "Friendly Champion", "Champion", 0, 3, 4, null);

    engine.applyMove(state, equip("equip", "champion"));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("champion");
  }

  @Test
  void equipCanAttachFriendlyUnitAtBase() {
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    LiveGameState state = state(gear, friendly);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);

    engine.applyMove(state, equip("equip", "friendly"));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("friendly");
  }

  @Test
  void equipCanAttachFriendlyUnitAtBattlefield() {
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(gear, friendly);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);

    engine.applyMove(state, equip("equip", "friendly"));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("friendly");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Equipped Equip Gear to Friendly Unit."));
  }

  @Test
  void equipRequiresPrintedPremiumPayment() {
    CardInstance gear = card("guardian", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    LiveGameState state = state(gear, friendly);
    stubCard("guardian", "Guardian Angel", "Gear", 0, 0, 0, "[Equip] :rb_rune_calm: (:rb_rune_calm:: Attach this to a unit you control.)", List.of(), List.of("CALM"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("guardian", "friendly")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Insufficient equip payment.");

    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(friendly.getZone()).isEqualTo(ZoneName.BASE);
  }

  @Test
  void equipSpendsPrintedPremiumPaymentExactlyOnce() {
    CardInstance gear = card("boots", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    LiveGameState state = state(gear, friendly);
    state.setRunes(new ArrayList<>(List.of(rune("chaos-rune", "p1", false))));
    stubCard("boots", "Boots of Swiftness", "Gear", 0, 0, 0, "[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit you control.)", List.of(), List.of("CHAOS"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("chaos-rune", "Chaos Rune", "Rune", 0, 0, 0, null, List.of(), List.of("CHAOS"));

    engine.applyMove(state, equip("boots", "friendly", List.of(), List.of("chaos-rune")));

    assertThat(gear.getAttachedToInstanceId()).isEqualTo("friendly");
    assertThat(state.getRunes()).extracting(RuneState::getInstanceId).doesNotContain("chaos-rune");
    assertThat(state.getPlayers().getFirst().getRuneDeckPool()).containsExactly("chaos-rune");
  }

  @Test
  void equipRejectsWrongDomainWithoutMutation() {
    CardInstance gear = card("guardian", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    LiveGameState state = state(gear, friendly);
    state.setRunes(new ArrayList<>(List.of(rune("chaos-rune", "p1", false))));
    stubCard("guardian", "Guardian Angel", "Gear", 0, 0, 0, "[Equip] :rb_rune_calm: (:rb_rune_calm:: Attach this to a unit you control.)", List.of(), List.of("CALM"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("chaos-rune", "Chaos Rune", "Rune", 0, 0, 0, null, List.of(), List.of("CHAOS"));

    assertThatThrownBy(() -> engine.applyMove(state, equip("guardian", "friendly", List.of(), List.of("chaos-rune"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equip payment uses the wrong rune domain.");

    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getRunes()).extracting(RuneState::getInstanceId).containsExactly("chaos-rune");
    assertThat(state.getPlayers().getFirst().getRuneDeckPool()).isEmpty();
  }

  @Test
  void equipCanUseSelectedEnergyAndPremiumPayment() {
    CardInstance gear = card("zero-drive", "p1", ZoneName.BASE);
    CardInstance friendly = card("friendly", "p1", ZoneName.BASE);
    LiveGameState state = state(gear, friendly);
    state.setRunes(new ArrayList<>(List.of(
        rune("energy-rune", "p1", false),
        rune("mind-rune", "p1", false))));
    stubCard("zero-drive", "The Zero Drive", "Gear", 0, 0, 0, "[Equip] :rb_energy_1::rb_rune_mind: (:rb_energy_1::rb_rune_mind:: Attach this to a unit you control.)", List.of(), List.of("MIND"));
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);
    stubCard("energy-rune", "Chaos Rune", "Rune", 0, 0, 0, null, List.of(), List.of("CHAOS"));
    stubCard("mind-rune", "Mind Rune", "Rune", 0, 0, 0, null, List.of(), List.of("MIND"));

    engine.applyMove(state, equip("zero-drive", "friendly", List.of("energy-rune"), List.of("mind-rune")));

    assertThat(gear.getAttachedToInstanceId()).isEqualTo("friendly");
    assertThat(findRune(state, "energy-rune").isTapped()).isTrue();
    assertThat(state.getRunes()).extracting(RuneState::getInstanceId).doesNotContain("mind-rune");
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isZero();
  }

  @Test
  void equipRequiresATarget() {
    LiveGameState state = state(card("equip", "p1", ZoneName.BASE));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    assertThatThrownBy(() -> engine.applyMove(state, new EquipGearMove("p1", "equip", "")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment requires a target.");
  }

  @Test
  void equipCannotAttachEnemyUnit() {
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(
        gear,
        enemy);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment can only attach to a friendly Unit or Champion.");
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(gear.getX()).isZero();
    assertThat(gear.getY()).isZero();
    assertThat(enemy.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
  }

  @Test
  void equipCannotAttachUnitInHiddenOrDiscardZones() {
    CardInstance hidden = card("hidden", "p1", ZoneName.HIDDEN);
    CardInstance trashed = card("trashed", "p1", ZoneName.DISCARD);
    LiveGameState state = state(card("equip", "p1", ZoneName.BASE), hidden, trashed);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("hidden", "Hidden Unit", "Unit", 0, 2, 2, null);
    stubCard("trashed", "Trashed Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "hidden")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment target must be in Base or at a battlefield.");
    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "trashed")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment target must be in Base or at a battlefield.");
  }

  @Test
  void equipCannotAttachCardInHandOrMissingDeckPoolCard() {
    CardInstance handUnit = card("hand-unit", "p1", ZoneName.HAND);
    LiveGameState state = state(card("equip", "p1", ZoneName.BASE), handUnit);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("deck-unit")));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("hand-unit", "Hand Unit", "Unit", 0, 2, 2, null);
    stubCard("deck-unit", "Deck Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "hand-unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment target must be in Base or at a battlefield.");
    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "deck-unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Card not found.");
  }

  @Test
  void equipCannotAttachChampionZoneIdentityCard() {
    LiveGameState state = state(
        card("equip", "p1", ZoneName.BASE),
        card("champion", "p1", ZoneName.CHAMPION));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("champion", "Friendly Champion", "Champion", 0, 3, 4, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "champion")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment target must be in Base or at a battlefield.");
  }

  @Test
  void equipCannotAttachNonCombatant() {
    LiveGameState state = state(
        card("equip", "p1", ZoneName.BASE),
        card("battlefield", "p1", ZoneName.BATTLEFIELD));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("battlefield", "Battlefield", "Battlefield", 0, 0, 0, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "battlefield")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment cannot attach to Battlefields.");
  }

  @Test
  void equipRejectsSpecificIllegalTargetTypesWithoutMutation() {
    CardInstance gearTarget = card("other-gear", "p1", ZoneName.BASE);
    CardInstance runeTarget = card("rune-card", "p1", ZoneName.BASE);
    CardInstance legendTarget = card("legend-card", "p1", ZoneName.BASE);
    CardInstance hiddenTarget = card("hidden-unit", "p1", ZoneName.BASE);
    hiddenTarget.setFaceDown(true);
    LiveGameState state = state(
        card("equip", "p1", ZoneName.BASE),
        gearTarget,
        runeTarget,
        legendTarget,
        hiddenTarget);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("other-gear", "Other Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("rune-card", "Rune Card", "Rune", 0, 0, 0, null);
    stubCard("legend-card", "Legend Card", "Legend", 0, 0, 0, null);
    stubCard("hidden-unit", "Hidden Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "other-gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment cannot attach to another Equipment.");
    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "rune-card")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment cannot attach to Runes.");
    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "legend-card")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment cannot attach to Legends.");
    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "hidden-unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment cannot attach to hidden or face-down cards.");
    assertThat(find(state, "equip").getAttachedToInstanceId()).isNull();
    assertThat(state.getCards()).filteredOn(card -> !"equip".equals(card.getInstanceId()))
        .allSatisfy(card -> assertThat(card.getAttachedToInstanceId()).isNull());
  }

  @Test
  void equipRejectsOpponentTargetWithFriendlyMessage() {
    CardInstance enemy = card("enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("equip", "p1", ZoneName.BASE), enemy);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Equipment can only attach to a friendly Unit or Champion.");
    assertThat(find(state, "equip").getAttachedToInstanceId()).isNull();
  }

  @Test
  void equippedGearDoesNotMoveToTrashAndCannotMoveToBattlefield() {
    CardInstance gear = card("equip", "p1", ZoneName.HAND);
    CardInstance friendly = card("friendly", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(gear, friendly);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("friendly", "Friendly Unit", "Unit", 0, 2, 2, null);

    engine.applyMove(state, play("equip", ZoneName.BASE));
    engine.applyMove(state, equip("equip", "friendly"));

    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getZone()).isNotEqualTo(ZoneName.DISCARD);
    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "equip")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only Units and Champions can move to the battlefield.");
  }

  @Test
  void attachedGearReturnsToBaseWhenHostReturnsToHand() {
    CardInstance bounce = card("bounce", "p1", ZoneName.HAND);
    CardInstance host = card("host", "p2", ZoneName.BATTLEFIELD);
    CardInstance gear = card("equip", "p2", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(bounce, host, gear);
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("host", "Equipped Unit", "Unit", 0, 2, 2, null);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "host"));

    assertThat(host.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Equip Gear returned to Base."));
  }

  @Test
  void attachedGearReturnsToBaseWhenHostIsDestroyedByCleanup() {
    CardInstance spell = card("spell", "p1", ZoneName.HAND);
    CardInstance host = card("host", "p2", ZoneName.BATTLEFIELD);
    host.setCurrentHealth(0);
    CardInstance gear = card("equip", "p2", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(spell, host, gear);
    stubCard("spell", "Simple Spell", "Spell", 0, 0, 0, "Check cleanup.");
    stubCard("host", "Destroyed Unit", "Unit", 0, 2, 2, null);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(host.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
  }

  @Test
  void attachedGearDoesNotTriggerDeathknellWhenHostDies() {
    CardInstance spell = card("spell", "p1", ZoneName.HAND);
    CardInstance host = card("host", "p2", ZoneName.BATTLEFIELD);
    host.setCurrentHealth(0);
    CardInstance gear = card("equip", "p2", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(spell, host, gear);
    stubCard("spell", "Simple Spell", "Spell", 0, 0, 0, "Check cleanup.");
    stubCard("host", "Destroyed Unit", "Unit", 0, 2, 2, null);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] [Deathknell] Attached unit gets +1.");
    when(cardDataService.hasKeyword("equip", "DEATHKNELL")).thenReturn(true);

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(host.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Equip Gear returned to Base."));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Deathknell"));
  }

  @Test
  void attachedGearReturnsToBaseWhenChampionHostReturnsToChampionZoneByCleanup() {
    CardInstance spell = card("spell", "p1", ZoneName.HAND);
    CardInstance champion = card("champion", "p2", ZoneName.BATTLEFIELD);
    champion.setCurrentHealth(0);
    CardInstance gear = card("equip", "p2", ZoneName.BASE);
    gear.setAttachedToInstanceId("champion");
    LiveGameState state = state(spell, champion, gear);
    stubCard("spell", "Simple Spell", "Spell", 0, 0, 0, "Check cleanup.");
    stubCard("champion", "Equipped Champion", "Champion", 0, 3, 4, null);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(champion.getZone()).isEqualTo(ZoneName.CHAMPION);
    assertThat(champion.isHasSummoningSickness()).isTrue();
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isNull();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Equip Gear returned to Base."));
  }

  @Test
  void attachedGearCannotBeReequippedNormally() {
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(
        gear,
        card("host", "p1", ZoneName.BASE),
        card("other", "p1", ZoneName.BASE));
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");
    stubCard("host", "Host Unit", "Unit", 0, 2, 2, null);
    stubCard("other", "Other Unit", "Unit", 0, 2, 2, null);

    assertThatThrownBy(() -> engine.applyMove(state, equip("equip", "other")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Attached Equipment cannot be re-equipped.");
  }

  @Test
  void equippedHostMovesToBattlefieldAndGearRemainsAttachedAtBase() {
    CardInstance host = card("host", "p1", ZoneName.BASE);
    CardInstance gear = card("equip", "p1", ZoneName.BASE);
    gear.setAttachedToInstanceId("host");
    LiveGameState state = state(host, gear);
    stubCard("host", "Host Unit", "Unit", 0, 2, 2, null);
    stubCard("equip", "Equip Gear", "Gear", 0, 0, 0, "[Equip] Attached unit gets +1.");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "host"));

    assertThat(host.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(gear.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(gear.getAttachedToInstanceId()).isEqualTo("host");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Moved Host Unit to the battlefield."));
  }

  @Test
  void unsupportedNonEquipGearRemainsBlocked() {
    LiveGameState state = state(card("unsupported-gear", "p1", ZoneName.HAND));
    stubCard("unsupported-gear", "Unsupported Gear", "Gear", 0, 0, 0, "Action: Do something unsupported.");
    when(cardDataService.isUnsupportedAction("unsupported-gear")).thenReturn(true);

    assertThatThrownBy(() -> engine.applyMove(state, play("unsupported-gear", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card's effect is not supported yet.");
  }

  @Test
  void selectedTargetReceivesEffectInsteadOfFirstValidTarget() {
    CardInstance firstEnemy = card("first-enemy", "p2", ZoneName.BATTLEFIELD);
    CardInstance selectedEnemy = card("selected-enemy", "p2", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("buff", "p1", ZoneName.HAND), firstEnemy, selectedEnemy);
    stubCard("buff", "Helpful Spell", "Spell", 0, 0, 0, "Give a unit +3 :rb_might: this turn.");
    stubCard("first-enemy", "First Enemy", "Unit", 0, 2, 2, null);
    stubCard("selected-enemy", "Selected Enemy", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("buff")).thenReturn(true);

    engine.applyMove(state, playTarget("buff", "selected-enemy"));

    assertThat(firstEnemy.getTemporaryPowerModifier()).isZero();
    assertThat(selectedEnemy.getTemporaryPowerModifier()).isEqualTo(3);
  }

  @Test
  void unknownCardTypeIsRejectedInsteadOfUsingFallbackZoneRouting() {
    LiveGameState state = state(card("mystery", "p1", ZoneName.HAND));
    stubCard("mystery", "Mystery Card", "Mystery", 0, 0, 0, null);

    assertThatThrownBy(() -> engine.applyMove(state, play("mystery", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card type cannot be played from hand.");
  }

  @Test
  void spellCannotMoveToBattlefieldLikeAUnit() {
    LiveGameState state = state(card("spell", "p1", ZoneName.BASE));
    stubCard("spell", "Spell", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "spell")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only Units and Champions can move to the battlefield.");
  }

  @Test
  void gearCanBePlayedButCannotFightAsAUnit() {
    LiveGameState state = state(card("gear", "p1", ZoneName.HAND));
    stubCard("gear", "Gear", 0);

    engine.applyMove(state, play("gear", ZoneName.BASE));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BASE);
    state.getCards().getFirst().setTapped(false);
    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "gear")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Only Units and Champions can move to the battlefield.");
  }

  @Test
  void legendCannotBePlayedFromHand() {
    LiveGameState state = state(card("legend", "p1", ZoneName.HAND));
    stubCard("legend", "Legend", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("legend", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Legend cards cannot be played from hand.");
  }

  @Test
  void battlefieldCannotBePlayedFromHand() {
    LiveGameState state = state(card("battlefield", "p1", ZoneName.HAND));
    stubCard("battlefield", "Battlefield", 0);

    assertThatThrownBy(() -> engine.applyMove(state, play("battlefield", ZoneName.BASE)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Battlefield cards cannot be played from hand.");
  }

  @Test
  void championCanStartShowdownFromChampionZone() {
    LiveGameState state = state(
        card("champion", "p1", ZoneName.CHAMPION),
        card("enemy", "p2", ZoneName.BATTLEFIELD));
    stubCard("champion", "Champion", 0);
    stubCard("enemy", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion"));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("champion");
  }

  @Test
  void championFromChampionZoneRequiresEnoughEnergy() {
    LiveGameState state = state(card("champion", "p1", ZoneName.CHAMPION));
    state.getPlayers().getFirst().setAvailableEnergy(3);
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    stubCard("champion", "Irelia - Fervent", "Champion", 5, 4, 5, "");

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Not enough energy to play Irelia - Fervent.");

    CardInstance champion = find(state, "champion");
    assertThat(champion.getZone()).isEqualTo(ZoneName.CHAMPION);
    assertThat(champion.isTapped()).isFalse();
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isEqualTo(3);
    assertThat(state.getRunes()).singleElement().satisfies(rune -> assertThat(rune.isTapped()).isFalse());
    assertThat(state.getActiveShowdown()).isNull();
  }

  @Test
  void championFromChampionZoneConsumesEnergyWhenPlayed() {
    LiveGameState state = state(card("champion", "p1", ZoneName.CHAMPION));
    state.getPlayers().getFirst().setAvailableEnergy(5);
    stubCard("champion", "Irelia - Fervent", "Champion", 5, 4, 5, "");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion"));

    CardInstance champion = find(state, "champion");
    assertThat(champion.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isZero();
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Irelia - Fervent from the Champion zone."));
  }

  @Test
  void championFromChampionZoneCanBePaidWithSelectedRunes() {
    LiveGameState state = state(card("champion", "p1", ZoneName.CHAMPION));
    state.setRunes(new ArrayList<>(List.of(
        rune("rune-1", "p1", false),
        rune("rune-2", "p1", false))));
    stubCard("champion", "Irelia - Fervent", "Champion", 2, 4, 5, "");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion", List.of("rune-1", "rune-2"), List.of()));

    CardInstance champion = find(state, "champion");
    assertThat(champion.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getRunes()).allMatch(RuneState::isTapped);
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isZero();
  }

  @Test
  void championFromChampionZoneCannotBePlayedOutsideMain() {
    LiveGameState state = state(card("champion", "p1", ZoneName.CHAMPION));
    state.setCurrentPhase(Phase.DRAW);
    state.getPlayers().getFirst().setAvailableEnergy(5);
    stubCard("champion", "Irelia - Fervent", "Champion", 5, 4, 5, "");

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You can only play your Champion during a legal play window.");

    assertThat(find(state, "champion").getZone()).isEqualTo(ZoneName.CHAMPION);
  }

  @Test
  void championCannotBePlayedRepeatedlyForFreeAfterLeavingChampionZone() {
    LiveGameState state = state(card("champion", "p1", ZoneName.CHAMPION));
    state.getPlayers().getFirst().setAvailableEnergy(5);
    stubCard("champion", "Irelia - Fervent", "Champion", 5, 4, 5, "");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion"));

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "champion")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card is already at that battlefield lane.");
  }

  @Test
  void legendCannotMoveToBattlefieldInAlphaModel() {
    LiveGameState state = state(card("legend", "p1", ZoneName.LEGEND));
    stubCard("legend", "Irelia - Blade Dancer", "Legend", 0, 0, 0, "");

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "legend")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Legends cannot be moved to the battlefield in this alpha model.");

    assertThat(find(state, "legend").getZone()).isEqualTo(ZoneName.LEGEND);
  }

  @Test
  void championUnitInHandCanBePlayedNormally() {
    LiveGameState state = state(card("champion", "p1", ZoneName.HAND));
    stubCard("champion", "Champion Unit", "Champion", 0, 4, 5, "");

    engine.applyMove(state, play("champion", ZoneName.BASE));

    CardInstance champion = find(state, "champion");
    assertThat(champion.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(champion.getCurrentHealth()).isEqualTo(5);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Played Champion Unit"));
  }

  @Test
  void baseUnitMovingToEmptyBattlefieldUpdatesController() {
    LiveGameState state = state(card("unit", "p1", ZoneName.BASE));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit"));

    assertThat(state.getCards().getFirst().getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getBattlefieldController()).containsEntry(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID, "p1");
    assertThat(state.getActiveShowdown()).isNull();
  }

  @Test
  void noxianDrummerCreatesRecruitWhenMovedToBattlefield() {
    CardInstance drummer = card("drummer", "p1", ZoneName.BASE);
    LiveGameState state = state(drummer);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("deck-card")));
    stubCard("drummer", "Noxian Drummer", "Unit", 0, 2, 2, "When I move to a battlefield, create a Recruit.");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 0, 1, 1, "Token Unit.");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "drummer"));

    assertThat(state.getCards())
        .filteredOn(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()))
        .singleElement()
        .satisfies(token -> {
          assertThat(token.getOwnerId()).isEqualTo("p1");
          assertThat(token.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
          assertThat(token.getCurrentHealth()).isEqualTo(1);
        });
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("deck-card");
  }

  @Test
  void noxianDrummerDoesNotCreateRecruitWhenRepositioned() {
    CardInstance drummer = card("drummer", "p1", ZoneName.BASE);
    LiveGameState state = state(drummer);
    stubCard("drummer", "Noxian Drummer", "Unit", 0, 2, 2, "When I move to a battlefield, create a Recruit.");

    engine.applyMove(state, new RepositionCardMove("p1", "drummer", 10, 10));

    assertThat(state.getCards()).noneMatch(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()));
  }

  @Test
  void stellacornHerderDrawsOneWhenMovedToBattlefield() {
    CardInstance herder = card("herder", "p1", ZoneName.BASE);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one", "drawn-two")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);
    stubCard("drawn-two", "Drawn Two", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "herder"));

    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("drawn-one") && card.getZone() == ZoneName.HAND);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-two"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-two");
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Stellacorn Herder drew 1 after moving."));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Drawn One"));
  }

  @Test
  void stellacornHerderDrawsOneWhenMovedBetweenBattlefieldLocations() {
    CardInstance herder = card("herder", "p1", ZoneName.BATTLEFIELD);
    herder.setBattlefieldLocationId("bf-0");
    herder.setTapped(false);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("lane-draw", "remaining")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("lane-draw", "Lane Draw", "Unit", 0, 1, 1, null);
    stubCard("remaining", "Remaining", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "herder", "bf-1", List.of(), List.of()));

    assertThat(herder.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(herder.getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("lane-draw") && card.getZone() == ZoneName.HAND);
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("remaining");
    assertThat(state.getLog()).filteredOn(entry -> entry.text().equals("Stellacorn Herder drew 1 after moving.")).hasSize(1);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Lane Draw"));
  }

  @Test
  void moveToBaseClearsBattlefieldLocationAndTriggersStellacornOnce() {
    CardInstance herder = card("herder", "p1", ZoneName.BATTLEFIELD);
    herder.setBattlefieldLocationId("bf-1");
    herder.setTapped(false);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("base-draw", "remaining")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("base-draw", "Base Draw", "Unit", 0, 1, 1, null);
    stubCard("remaining", "Remaining", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new MoveToBaseMove("p1", "herder"));

    assertThat(herder.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(herder.getBattlefieldLocationId()).isNull();
    assertThat(herder.isTapped()).isTrue();
    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("base-draw") && card.getZone() == ZoneName.HAND);
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("remaining");
    assertThat(state.getLog()).filteredOn(entry -> entry.text().equals("Stellacorn Herder drew 1 after moving.")).hasSize(1);
    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Moved Stellacorn Herder to Base."));
  }

  @Test
  void invalidMoveToBaseDoesNotDrawOrClearLocation() {
    CardInstance herder = card("herder", "p1", ZoneName.BATTLEFIELD);
    herder.setBattlefieldLocationId("bf-1");
    herder.setTapped(true);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("base-draw")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("base-draw", "Base Draw", "Unit", 0, 1, 1, null);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBaseMove("p1", "herder")))
        .hasMessage("Only ready cards can move back to Base.");

    assertThat(herder.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(herder.getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("base-draw"));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Stellacorn Herder drew"));
  }

  @Test
  void stellacornHerderDrawsOneWhenReturnedFromBattlefieldToBaseAfterShowdown() {
    CardInstance herder = card("herder", "p1", ZoneName.BASE);
    CardInstance defender = card("defender", "p2", ZoneName.BATTLEFIELD);
    defender.setCurrentHealth(5);
    LiveGameState state = state(herder, defender);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("battlefield-draw", "return-draw", "remaining")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("defender", "Defender", "Unit", 0, 3, 5, null);
    when(cardDataService.hasKeyword(defender, "STUN")).thenReturn(true);
    stubCard("battlefield-draw", "Battlefield Draw", "Unit", 0, 1, 1, null);
    stubCard("return-draw", "Return Draw", "Unit", 0, 1, 1, null);
    stubCard("remaining", "Remaining", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "herder"));
    passShowdownFocusCycle(state);
    engine.applyMove(state, new ResolveShowdownMove("p1"));
    engine.applyMove(state, assign("p1", "herder", "defender", 2));
    engine.applyMove(state, new AssignCombatDamageMove("p2", List.of()));

    assertThat(herder.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("battlefield-draw") && card.getZone() == ZoneName.HAND);
    assertThat(state.getCards()).anyMatch(card -> card.getCardId().equals("return-draw") && card.getZone() == ZoneName.HAND);
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("remaining");
    assertThat(state.getLog()).filteredOn(entry -> entry.text().equals("Stellacorn Herder drew 1 after moving.")).hasSize(2);
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Battlefield Draw") || entry.text().contains("Return Draw"));
  }

  @Test
  void stellacornHerderDoesNotDrawWhenPlayedFromHandToBase() {
    LiveGameState state = state(card("herder", "p1", ZoneName.HAND));
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("herder", ZoneName.BASE));

    assertThat(find(state, "herder").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-one");
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Stellacorn Herder drew"));
  }

  @Test
  void stellacornHerderDoesNotDrawWhenRepositioned() {
    CardInstance herder = card("herder", "p1", ZoneName.BASE);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one")));
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new RepositionCardMove("p1", "herder", 10, 10));

    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-one");
  }

  @Test
  void stellacornHerderDoesNotDrawWhenReturnedToHand() {
    CardInstance herder = card("herder", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(card("bounce", "p1", ZoneName.HAND), herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one")));
    stubCard("bounce", "Bounce Spell", "Spell", 0, 0, 0, "Return a unit to its owner's hand.");
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);
    when(cardDataService.requiresBattlefieldTarget("bounce")).thenReturn(true);

    engine.applyMove(state, playTarget("bounce", "herder"));

    assertThat(herder.getZone()).isEqualTo(ZoneName.HAND);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-one");
  }

  @Test
  void stellacornHerderDoesNotDrawWhenDestroyedOrMovedToTrash() {
    CardInstance herder = card("herder", "p1", ZoneName.BASE);
    herder.setCurrentHealth(0);
    LiveGameState state = state(card("spell", "p1", ZoneName.HAND), herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one")));
    stubCard("spell", "Simple Spell", "Spell", 0, 0, 0, "No effect.");
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);

    engine.applyMove(state, play("spell", ZoneName.BASE));

    assertThat(herder.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-one");
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Stellacorn Herder drew"));
  }

  @Test
  void stellacornHerderEmptyDeckLogsNoDrawWithoutDrawMessage() {
    CardInstance herder = card("herder", "p1", ZoneName.BASE);
    LiveGameState state = state(herder);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>());
    stubCard("herder", "Stellacorn Herder", "Unit", 0, 2, 2, "When I move, draw 1.");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "herder"));

    assertThat(state.getLog()).anyMatch(entry -> entry.text().equals("Player One's deck is empty - no draw."));
    assertThat(state.getLog()).noneMatch(entry -> entry.text().equals("Stellacorn Herder drew 1 after moving."));
  }

  @Test
  void stellacornHerderTriggerDoesNotFireForUnrelatedCards() {
    CardInstance unit = card("unit", "p1", ZoneName.BASE);
    LiveGameState state = state(unit);
    state.getPlayers().getFirst().setDeckPool(new ArrayList<>(List.of("drawn-one")));
    stubCard("unit", "Ordinary Unit", "Unit", 0, 2, 2, "No movement trigger.");
    stubCard("drawn-one", "Drawn One", "Unit", 0, 1, 1, null);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit"));

    assertThat(state.getCards()).noneMatch(card -> card.getCardId().equals("drawn-one"));
    assertThat(state.getPlayers().getFirst().getDeckPool()).containsExactly("drawn-one");
    assertThat(state.getLog()).noneMatch(entry -> entry.text().contains("Stellacorn Herder"));
  }

  @Test
  void vanguardCaptainLegionCreatesTwoRecruitsOnlyAfterAnotherCardWasPlayed() {
    CardInstance first = card("first", "p1", ZoneName.HAND);
    CardInstance captain = card("captain", "p1", ZoneName.HAND);
    LiveGameState state = state(first, captain);
    stubCard("first", "First Unit", "Unit", 0, 1, 1, null);
    stubCard("captain", "Vanguard Captain", "Unit", 0, 2, 2, "[Legion] Create two Recruits.");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 0, 1, 1, "Token Unit.");
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenAnswer(invocation -> {
      CardInstance card = invocation.getArgument(0);
      String keyword = invocation.getArgument(1);
      return "captain".equals(card.getCardId()) && "LEGION".equalsIgnoreCase(keyword);
    });

    engine.applyMove(state, play("first", ZoneName.BASE));
    assertThat(state.getCards()).noneMatch(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()));

    engine.applyMove(state, play("captain", ZoneName.BASE));

    assertThat(state.getCards())
        .filteredOn(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()))
        .hasSize(2)
        .allSatisfy(token -> {
          assertThat(token.getOwnerId()).isEqualTo("p1");
          assertThat(token.getZone()).isEqualTo(ZoneName.BASE);
        });
  }

  @Test
  void vanguardCaptainLegionDoesNotTriggerAsFirstCardPlayed() {
    CardInstance captain = card("captain", "p1", ZoneName.HAND);
    LiveGameState state = state(captain);
    stubCard("captain", "Vanguard Captain", "Unit", 0, 2, 2, "[Legion] Create two Recruits.");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 0, 1, 1, "Token Unit.");
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenAnswer(invocation -> {
      CardInstance card = invocation.getArgument(0);
      String keyword = invocation.getArgument(1);
      return "captain".equals(card.getCardId()) && "LEGION".equalsIgnoreCase(keyword);
    });

    engine.applyMove(state, play("captain", ZoneName.BASE));

    assertThat(state.getCards()).noneMatch(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()));
  }

  @Test
  void playedCardTrackingResetsOnTurnChange() {
    LiveGameState state = state();
    state.setCurrentPhase(Phase.END);
    state.setActivePlayerId("p1");
    state.setCardPlayedThisTurn(true);

    engine.applyMove(state, new PassPhaseMove("p1"));

    assertThat(state.getActivePlayerId()).isEqualTo("p2");
    assertThat(state.isCardPlayedThisTurn()).isFalse();
  }

  @Test
  void vanguardCaptainLegionDoesNotUsePreviousPlayersPlayedCardFlagAfterTurnChange() {
    CardInstance captain = card("captain", "p2", ZoneName.HAND);
    LiveGameState state = state(captain);
    state.setCurrentPhase(Phase.END);
    state.setActivePlayerId("p1");
    state.setCardPlayedThisTurn(true);
    stubCard("captain", "Vanguard Captain", "Unit", 0, 2, 2, "[Legion] Create two Recruits.");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 0, 1, 1, "Token Unit.");
    when(cardDataService.hasKeyword(any(CardInstance.class), anyString())).thenAnswer(invocation -> {
      CardInstance card = invocation.getArgument(0);
      String keyword = invocation.getArgument(1);
      return "captain".equals(card.getCardId()) && "LEGION".equalsIgnoreCase(keyword);
    });

    engine.applyMove(state, new PassPhaseMove("p1"));
    state.setCurrentPhase(Phase.MAIN);
    engine.applyMove(state, play("p2", "captain", ZoneName.BASE));

    assertThat(state.getCards()).noneMatch(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()));
  }

  @Test
  void movingIntoContestedBattlefieldStartsShowdown() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE),
        card("defender", "p2", ZoneName.BATTLEFIELD));
    stubCard("attacker", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("attacker");
  }

  @Test
  void movingUnitToBattlefieldAssignsDefaultLocationId() {
    LiveGameState state = state(card("unit", "p1", ZoneName.BASE));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit"));

    CardInstance unit = find(state, "unit");
    assertThat(unit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(unit.getBattlefieldLocationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void movingUnitToBattlefieldCanAssignCustomLocationId() {
    LiveGameState state = state(card("unit", "p1", ZoneName.BASE));
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit", "bf-1", List.of(), List.of()));

    CardInstance unit = find(state, "unit");
    assertThat(unit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(unit.getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getBattlefieldController()).containsEntry("bf-1", "p1");
  }

  @Test
  void showdownStartedByMoveStoresDefaultLocationId() {
    LiveGameState state = state(
        card("attacker", "p1", ZoneName.BASE),
        card("defender", "p2", ZoneName.BATTLEFIELD));
    stubCard("attacker", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker"));

    assertThat(find(state, "attacker").getBattlefieldLocationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().locationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void movingToSameLocationAsOpponentStartsShowdownAtThatLocation() {
    CardInstance defender = atLocation(card("defender", "p2", ZoneName.BATTLEFIELD), "bf-1");
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), defender);
    stubCard("attacker", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker", "bf-1", List.of(), List.of()));

    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().locationId()).isEqualTo("bf-1");
    assertThat(state.getActiveShowdown().relevantPlayerIds()).containsExactly("p1", "p2");
  }

  @Test
  void movingToDifferentLocationThanOpponentDoesNotStartShowdown() {
    CardInstance defender = atLocation(card("defender", "p2", ZoneName.BATTLEFIELD), "bf-0");
    LiveGameState state = state(card("attacker", "p1", ZoneName.BASE), defender);
    stubCard("attacker", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker", "bf-1", List.of(), List.of()));

    assertThat(state.getActiveShowdown()).isNull();
    assertThat(find(state, "attacker").getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getBattlefieldController()).containsEntry("bf-1", "p1");
  }

  @Test
  void activeDuelBattlefieldLocationsAreBf0AndBf1Only() {
    LiveGameState state = state();

    assertThat(com.riftforge.rules.BattlefieldLocationRules.activeLocationIds(state))
        .containsExactly("bf-0", "bf-1");
  }

  @Test
  void battlefieldToBattlefieldMoveUpdatesLocationAndControllers() {
    CardInstance unit = atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0");
    LiveGameState state = state(unit);
    state.getBattlefieldController().put("bf-0", "p1");
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit", "bf-1", List.of(), List.of()));

    assertThat(unit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(unit.getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(unit.isTapped()).isTrue();
    assertThat(state.getActiveShowdown()).isNull();
    assertThat(state.getBattlefieldController()).doesNotContainKey("bf-0");
    assertThat(state.getBattlefieldController()).containsEntry("bf-1", "p1");
  }

  @Test
  void battlefieldToBattlefieldMoveToSameLocationIsRejected() {
    CardInstance unit = atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0");
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit", "bf-0", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card is already at that battlefield lane.");

    assertThat(unit.getBattlefieldLocationId()).isEqualTo("bf-0");
  }

  @Test
  void battlefieldToBattlefieldMoveToInactiveBf2InDuelIsRejected() {
    CardInstance unit = atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0");
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit", "bf-2", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That battlefield lane is not active in this game.");

    assertThat(unit.getBattlefieldLocationId()).isEqualTo("bf-0");
  }

  @Test
  void battlefieldToBattlefieldMoveDuringActiveShowdownIsRejected() {
    CardInstance unit = atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0");
    LiveGameState state = state(unit);
    state.setActiveShowdown(focusedShowdown("p1", false));
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "unit", "bf-1", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("Resolve the active showdown first.");

    assertThat(unit.getBattlefieldLocationId()).isEqualTo("bf-0");
  }

  @Test
  void battlefieldToBattlefieldMoveIntoOpposingLaneStartsShowdownOnlyThere() {
    CardInstance attacker = atLocation(card("attacker", "p1", ZoneName.BATTLEFIELD), "bf-0");
    CardInstance offLocationFriend = atLocation(card("off-location-friend", "p1", ZoneName.BATTLEFIELD), "bf-0");
    CardInstance defender = atLocation(card("defender", "p2", ZoneName.BATTLEFIELD), "bf-1");
    LiveGameState state = state(attacker, offLocationFriend, defender);
    state.getBattlefieldController().put("bf-0", "p1");
    state.getBattlefieldController().put("bf-1", "p2");
    stubCard("attacker", "Unit", 0);
    stubCard("off-location-friend", "Unit", 0);
    stubCard("defender", "Unit", 0);

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "attacker", "bf-1", List.of(), List.of()));

    assertThat(attacker.getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getActiveShowdown()).isNotNull();
    assertThat(state.getActiveShowdown().locationId()).isEqualTo("bf-1");
    assertThat(state.getActiveShowdown().attackerInstanceIds()).containsExactly("attacker");
    assertThat(state.getActiveShowdown().relevantPlayerIds()).containsExactly("p1", "p2");
    assertThat(offLocationFriend.getBattlefieldLocationId()).isEqualTo("bf-0");
    assertThat(state.getBattlefieldController()).containsEntry("bf-0", "p1");
    assertThat(state.getBattlefieldController()).containsEntry("bf-1", "p2");
  }

  @Test
  void noxianDrummerRecruitTokenUsesDrummersBattlefieldLocation() {
    LiveGameState state = state(card("drummer", "p1", ZoneName.BASE));
    stubCard("drummer", "Noxian Drummer", "Unit", 0, 2, 2, "When I move to battlefield, create a Recruit.");
    stubCard(TokenFactory.RECRUIT_TOKEN_CARD_ID, "Recruit", "Unit", 0, 1, 1, "Token Unit.");

    engine.applyMove(state, new MoveToBattlefieldMove("p1", "drummer", "bf-1", List.of(), List.of()));

    assertThat(state.getCards())
        .filteredOn(card -> TokenFactory.RECRUIT_TOKEN_CARD_ID.equals(card.getCardId()))
        .singleElement()
        .satisfies(token -> assertThat(token.getBattlefieldLocationId()).isEqualTo("bf-1"));
  }

  @Test
  void oldBattlefieldCardsWithoutStoredLocationUseDefaultLocationId() {
    CardInstance unit = card("unit", "p1", ZoneName.BATTLEFIELD);
    unit.setBattlefieldLocationId("");

    assertThat(unit.getBattlefieldLocationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void targetingStillWorksForDefaultLocationBattlefieldCards() {
    CardInstance spell = card("boost", "p1", ZoneName.HAND);
    CardInstance target = card("target", "p1", ZoneName.BATTLEFIELD);
    target.setBattlefieldLocationId("");
    LiveGameState state = state(spell, target);
    stubCard("boost", "Boost", "Spell", 0, 0, 0, "Give a friendly unit +2 :rb_might: this turn.");
    stubCard("target", "Target Unit", "Unit", 0, 2, 2, null);
    when(cardDataService.requiresBattlefieldTarget("boost")).thenReturn(true);
    when(cardDataService.requiresFriendlyTarget("boost")).thenReturn(true);

    engine.applyMove(state, playTarget("boost", "target"));

    assertThat(target.getTemporaryPowerModifier()).isEqualTo(2);
    assertThat(target.getBattlefieldLocationId()).isEqualTo(CardInstance.DEFAULT_BATTLEFIELD_LOCATION_ID);
  }

  @Test
  void cannotMoveOpponentUnitToBattlefield() {
    LiveGameState state = state(card("enemy", "p2", ZoneName.BASE));
    stubCard("enemy", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new MoveToBattlefieldMove("p1", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("You do not own that card.");
  }

  @Test
  void repositionOnlyChangesCoordinatesWithinSameZone() {
    CardInstance unit = card("unit", "p1", ZoneName.BASE);
    unit.setX(10);
    unit.setY(20);
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    engine.applyMove(state, new RepositionCardMove("p1", "unit", 55, 66));

    assertThat(unit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(unit.getX()).isEqualTo(55);
    assertThat(unit.getY()).isEqualTo(66);
  }

  @Test
  void hiddenHandCardsCannotBeRepositioned() {
    CardInstance unit = card("unit", "p1", ZoneName.HAND);
    LiveGameState state = state(unit);
    stubCard("unit", "Unit", 0);

    assertThatThrownBy(() -> engine.applyMove(state, new RepositionCardMove("p1", "unit", 55, 66)))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessage("That card cannot be repositioned.");

    assertThat(unit.getZone()).isEqualTo(ZoneName.HAND);
  }

  @Test
  void revealedHandPermissionClearsAtEndOfViewerTurn() {
    LiveGameState state = state();
    state.setCurrentPhase(Phase.END);
    state.setActivePlayerId("p1");
    state.setRevealedHands(new ArrayList<>(List.of(revealedHand("p1", "p2", "secret-hand"))));

    engine.applyMove(state, new PassPhaseMove("p1"));

    assertThat(state.getRevealedHands()).isEmpty();
  }

  @Test
  void theSyrenActivatesFromBaseToMoveFriendlyBattlefieldUnitToBase() {
    stubCard("syren", "The Syren", "Gear", 2, 0, 0, ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.");
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("syren", "p1", ZoneName.BASE), atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-1"));
    state.getPlayers().getFirst().setAvailableEnergy(1);

    engine.applyMove(state, new ActivateAbilityMove("p1", "syren", "unit"));

    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "unit").getBattlefieldLocationId()).isNull();
    assertThat(find(state, "syren").isTapped()).isTrue();
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isZero();
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains("The Syren moved Friendly Unit to Base.");
  }

  @Test
  void theSyrenCanPayActivationWithSelectedRune() {
    stubCard("syren", "The Syren", "Gear", 2, 0, 0, ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.");
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("syren", "p1", ZoneName.BASE), atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0"));
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));

    engine.applyMove(state, new ActivateAbilityMove("p1", "syren", "unit", List.of("rune-1"), List.of()));

    assertThat(findRune(state, "rune-1").isTapped()).isTrue();
    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "syren").isTapped()).isTrue();
  }

  @Test
  void theSyrenRejectsInvalidTargetWithoutMutation() {
    stubCard("syren", "The Syren", "Gear", 2, 0, 0, ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.");
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("syren", "p1", ZoneName.BASE), atLocation(card("enemy", "p2", ZoneName.BATTLEFIELD), "bf-0"));
    state.getPlayers().getFirst().setAvailableEnergy(1);

    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove("p1", "syren", "enemy")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("friendly public Unit or Champion");

    assertThat(find(state, "enemy").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(find(state, "enemy").getBattlefieldLocationId()).isEqualTo("bf-0");
    assertThat(find(state, "syren").isTapped()).isFalse();
    assertThat(state.getPlayers().getFirst().getAvailableEnergy()).isEqualTo(1);
  }

  @Test
  void theSyrenRejectsInsufficientPaymentWithoutMutation() {
    stubCard("syren", "The Syren", "Gear", 2, 0, 0, ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.");
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("syren", "p1", ZoneName.BASE), atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0"));
    state.setRunes(new ArrayList<>(List.of(rune("tapped-rune", "p1", true))));

    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove("p1", "syren", "unit")))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("Insufficient energy");

    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(find(state, "syren").isTapped()).isFalse();
  }

  @Test
  void zhonyasHourglassActivatesFromBaseToRegisterDeathProtection() {
    stubCard("zhonya", "Zhonya's Hourglass", "Gear", 2, 0, 0, zhonyasText());
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("zhonya", "p1", ZoneName.BASE), atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-1"));

    engine.applyMove(state, new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "unit", List.of(), List.of()));

    assertThat(state.getReplacementEffects()).singleElement().satisfies(effect -> {
      assertThat(effect.getSourceInstanceId()).isEqualTo("zhonya");
      assertThat(effect.getProtectedInstanceId()).isEqualTo("unit");
    });
    assertThat(find(state, "zhonya").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "zhonya").isTapped()).isFalse();
    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(find(state, "unit").getBattlefieldLocationId()).isEqualTo("bf-1");
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains("Zhonya's Hourglass is protecting Friendly Unit.");
  }

  @Test
  void ireliaBladeDancerActivatesFromLegendZoneToReadyFriendlyUnit() {
    stubCard("irelia", "Irelia - Blade Dancer", "Legend", 0, 0, 0, ireliaBladeDancerText());
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    CardInstance irelia = card("irelia", "p1", ZoneName.LEGEND);
    CardInstance unit = card("unit", "p1", ZoneName.BASE);
    unit.setTapped(true);
    LiveGameState state = state(irelia, unit);
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));

    engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "irelia",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "unit",
        List.of(),
        List.of("rune-1")));

    assertThat(find(state, "unit").isTapped()).isFalse();
    assertThat(find(state, "irelia").isTapped()).isTrue();
    assertThat(state.getRunes()).noneMatch(rune -> rune.getInstanceId().equals("rune-1"));
    assertThat(state.getPlayers().getFirst().getRuneDeckPool()).containsExactly("rune-1");
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains("Irelia - Blade Dancer readied Friendly Unit.");
  }

  @Test
  void ireliaFerventGainsMightWhenReadiedBySupportedLegendAbility() {
    stubCard("blade-dancer", "Irelia - Blade Dancer", "Legend", 0, 0, 0, ireliaBladeDancerText());
    stubCard("fervent", "Irelia - Fervent", "Champion", 5, 4, 5, ireliaFerventText());
    CardInstance bladeDancer = card("blade-dancer", "p1", ZoneName.LEGEND);
    CardInstance fervent = card("fervent", "p1", ZoneName.BATTLEFIELD);
    fervent.setTapped(true);
    LiveGameState state = state(bladeDancer, fervent);
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));

    engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "blade-dancer",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "fervent",
        List.of(),
        List.of("rune-1")));

    assertThat(find(state, "fervent").isTapped()).isFalse();
    assertThat(find(state, "fervent").getTemporaryPowerModifier()).isEqualTo(1);
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains(
            "Irelia - Fervent gained +1 Might this turn from being readied.",
            "Irelia - Blade Dancer readied Irelia - Fervent.");
  }

  @Test
  void ireliaFerventReadyTriggerRequiresPublicPlayAndActualReadyTransition() {
    stubCard("blade-dancer", "Irelia - Blade Dancer", "Legend", 0, 0, 0, ireliaBladeDancerText());
    stubCard("fervent", "Irelia - Fervent", "Champion", 5, 4, 5, ireliaFerventText());
    CardInstance bladeDancer = card("blade-dancer", "p1", ZoneName.LEGEND);
    CardInstance fervent = card("fervent", "p1", ZoneName.CHAMPION);
    fervent.setTapped(true);
    LiveGameState state = state(bladeDancer, fervent);
    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));

    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "blade-dancer",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "fervent",
        List.of(),
        List.of("rune-1"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("Base or at a battlefield");
    assertThat(find(state, "fervent").getTemporaryPowerModifier()).isZero();

    fervent.setZone(ZoneName.BASE);
    fervent.setTapped(false);
    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "blade-dancer",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "fervent",
        List.of(),
        List.of("rune-1"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("exhausted friendly public Unit");
    assertThat(find(state, "fervent").getTemporaryPowerModifier()).isZero();
  }

  @Test
  void ireliaBladeDancerRejectsMissingPremiumOrReadyTargetWithoutMutation() {
    stubCard("irelia", "Irelia - Blade Dancer", "Legend", 0, 0, 0, ireliaBladeDancerText());
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    CardInstance irelia = card("irelia", "p1", ZoneName.LEGEND);
    CardInstance unit = card("unit", "p1", ZoneName.BASE);
    unit.setTapped(true);
    LiveGameState state = state(irelia, unit);

    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "irelia",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "unit",
        List.of(),
        List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("premium rune payment");
    assertThat(find(state, "unit").isTapped()).isTrue();
    assertThat(find(state, "irelia").isTapped()).isFalse();

    state.setRunes(new ArrayList<>(List.of(rune("rune-1", "p1", false))));
    unit.setTapped(false);
    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove(
        "p1",
        "irelia",
        ActivatedAbilityService.IRELIA_BLADE_DANCER_READY_UNIT,
        "unit",
        List.of(),
        List.of("rune-1"))))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("exhausted friendly public Unit");
    assertThat(find(state, "irelia").isTapped()).isFalse();
    assertThat(state.getRunes()).anyMatch(rune -> rune.getInstanceId().equals("rune-1"));
  }

  @Test
  void zhonyasHourglassRejectsEnemyTargetWithoutMutation() {
    stubCard("zhonya", "Zhonya's Hourglass", "Gear", 2, 0, 0, zhonyasText());
    stubCard("enemy", "Enemy Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(card("zhonya", "p1", ZoneName.BASE), atLocation(card("enemy", "p2", ZoneName.BATTLEFIELD), "bf-0"));

    assertThatThrownBy(() -> engine.applyMove(state, new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "enemy", List.of(), List.of())))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("friendly public Unit or Champion");

    assertThat(state.getReplacementEffects()).isEmpty();
    assertThat(find(state, "zhonya").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "enemy").getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(find(state, "enemy").getBattlefieldLocationId()).isEqualTo("bf-0");
  }

  @Test
  void zhonyasHourglassReplacementDestroysSourceAndHealsExhaustsRecallsProtectedUnit() {
    stubCard("zhonya", "Zhonya's Hourglass", "Gear", 2, 0, 0, zhonyasText());
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 3, "");
    stubCard("trigger", "Trigger Unit", "Unit", 0, 1, 1, "");
    CardInstance unit = atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-1");
    unit.setCurrentHealth(0);
    LiveGameState state = state(card("zhonya", "p1", ZoneName.BASE), unit, card("trigger", "p1", ZoneName.HAND));

    engine.applyMove(state, new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "unit", List.of(), List.of()));
    engine.applyMove(state, play("trigger", ZoneName.BASE));

    assertThat(find(state, "zhonya").getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "unit").getBattlefieldLocationId()).isNull();
    assertThat(find(state, "unit").getCurrentHealth()).isEqualTo(3);
    assertThat(find(state, "unit").isTapped()).isTrue();
    assertThat(state.getReplacementEffects()).isEmpty();
    assertThat(state.getLog()).extracting(LiveGameState.LogEntry::text)
        .contains("Zhonya's Hourglass was destroyed by a replacement effect.");
  }

  @Test
  void nonDeathRecallDoesNotConsumeZhonyasHourglassReplacement() {
    stubCard("zhonya", "Zhonya's Hourglass", "Gear", 2, 0, 0, zhonyasText());
    stubCard("syren", "The Syren", "Gear", 2, 0, 0, ":rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.");
    stubCard("unit", "Friendly Unit", "Unit", 0, 2, 2, "");
    LiveGameState state = state(
        card("zhonya", "p1", ZoneName.BASE),
        card("syren", "p1", ZoneName.BASE),
        atLocation(card("unit", "p1", ZoneName.BATTLEFIELD), "bf-0"));
    state.getPlayers().getFirst().setAvailableEnergy(1);

    engine.applyMove(state, new ActivateAbilityMove("p1", "zhonya", ActivatedAbilityService.ZHONYAS_HOURGLASS_PROTECT, "unit", List.of(), List.of()));
    engine.applyMove(state, new ActivateAbilityMove("p1", "syren", ActivatedAbilityService.THE_SYREN_RECALL, "unit", List.of(), List.of()));

    assertThat(find(state, "unit").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(find(state, "zhonya").getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getReplacementEffects()).singleElement()
        .extracting(effect -> effect.getProtectedInstanceId())
        .isEqualTo("unit");
  }

  private LiveGameState state(CardInstance... cards) {
    PlayerState p1 = new PlayerState();
    p1.setUserId("p1");
    p1.setName("Player One");
    PlayerState p2 = new PlayerState();
    p2.setUserId("p2");
    p2.setName("Player Two");
    LiveGameState state = new LiveGameState();
    state.setCurrentPhase(Phase.MAIN);
    state.setActivePlayerId("p1");
    state.setPlayers(new ArrayList<>(List.of(p1, p2)));
    state.setCards(new ArrayList<>(List.of(cards)));
    state.setLog(new ArrayList<>());
    return state;
  }

  private PlayCardMove play(String instanceId, ZoneName targetZone) {
    return new PlayCardMove("p1", instanceId, targetZone, 0, 0, null, false, List.of(), List.of());
  }

  private PlayCardMove play(String playerId, String instanceId, ZoneName targetZone) {
    return new PlayCardMove(playerId, instanceId, targetZone, 0, 0, null, false, List.of(), List.of());
  }

  private PlayCardMove playTarget(String instanceId, String targetInstanceId) {
    return new PlayCardMove("p1", instanceId, ZoneName.BASE, 0, 0, targetInstanceId, false, List.of(), List.of());
  }

  private PlayCardMove playTarget(String playerId, String instanceId, String targetInstanceId) {
    return new PlayCardMove(playerId, instanceId, ZoneName.BASE, 0, 0, targetInstanceId, false, List.of(), List.of());
  }

  private PlayCardMove playMultiTarget(String instanceId, String friendlyTargetId, String enemyTargetId) {
    return new PlayCardMove(
        "p1",
        instanceId,
        ZoneName.BASE,
        0,
        0,
        null,
        List.of(
            new PlayCardMove.TargetSelection(PlayCardMove.TargetSelection.FRIENDLY_UNIT, friendlyTargetId),
            new PlayCardMove.TargetSelection(PlayCardMove.TargetSelection.ENEMY_UNIT, enemyTargetId)),
        false,
        List.of(),
        List.of());
  }

  private EquipGearMove equip(String gearInstanceId, String targetInstanceId) {
    return new EquipGearMove("p1", gearInstanceId, targetInstanceId);
  }

  private EquipGearMove equip(String gearInstanceId, String targetInstanceId, List<String> paymentRuneIds, List<String> premiumRuneIds) {
    return new EquipGearMove("p1", gearInstanceId, targetInstanceId, paymentRuneIds, premiumRuneIds);
  }

  private LiveGameState disarmingRakeState(CardInstance... extraCards) {
    stubCard("rake", "Disarming Rake", "Unit", 0, 1, 2, "When you play me, you may kill a gear.");
    List<CardInstance> cards = new ArrayList<>();
    cards.add(card("rake", "p1", ZoneName.HAND));
    cards.addAll(List.of(extraCards));
    return state(cards.toArray(CardInstance[]::new));
  }

  private ResolveChoiceMove resolveTarget(PendingChoice choice, String targetInstanceId) {
    return new ResolveChoiceMove("p1", choice.getChoiceId(), null, null, null, targetInstanceId, List.of());
  }

  private LiveGameState.ShowdownState focusedShowdown(String focusedPlayerId, boolean readyToResolve) {
    return focusedShowdown(focusedPlayerId, readyToResolve ? 2 : 0, readyToResolve);
  }

  private LiveGameState.ShowdownState focusedShowdown(String focusedPlayerId, int consecutivePasses, boolean readyToResolve) {
    return new LiveGameState.ShowdownState(
        "p1",
        List.of("attacker"),
        Map.of(),
        ShowdownStep.ACTION_WINDOW,
        List.of("p1", "p2"),
        focusedPlayerId,
        consecutivePasses,
        readyToResolve);
  }

  private LiveGameState.ShowdownState readyShowdown(String focusedPlayerId) {
    return focusedShowdown(focusedPlayerId, true);
  }

  private void passShowdownFocusCycle(LiveGameState state) {
    engine.applyMove(state, new PassShowdownFocusMove("p1"));
    engine.applyMove(state, new PassShowdownFocusMove("p2"));
  }

  private AssignCombatDamageMove assign(String playerId, String sourceId, String targetId, int amount) {
    return new AssignCombatDamageMove(playerId, List.of(new LiveGameState.CombatDamageAssignment(sourceId, targetId, amount)));
  }

  private CardInstance card(String id, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(id);
    card.setCardId(id);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(1);
    card.setTempKeywords(new ArrayList<>());
    return card;
  }

  private CardInstance find(LiveGameState state, String instanceId) {
    return state.getCards().stream()
        .filter(card -> card.getInstanceId().equals(instanceId))
        .findFirst()
        .orElseThrow();
  }

  private CardInstance atLocation(CardInstance card, String locationId) {
    card.setBattlefieldLocationId(locationId);
    return card;
  }

  private String ireliaBladeDancerText() {
    return "When you choose a friendly unit, you may exhaust me and pay :rb_rune_rainbow: to ready it."
        + "When you conquer, you may pay :rb_energy_1: to ready me.";
  }

  private String ireliaFerventText() {
    return "[Deflect] (Opponents must pay :rb_rune_rainbow: to choose me with a spell or ability.)"
        + "When you choose or ready me, give me +1 :rb_might: this turn.";
  }

  private RuneState findRune(LiveGameState state, String instanceId) {
    return state.getRunes().stream()
        .filter(rune -> rune.getInstanceId().equals(instanceId))
        .findFirst()
        .orElseThrow();
  }

  private RuneState rune(String id, String ownerId, boolean tapped) {
    RuneState rune = new RuneState();
    rune.setInstanceId(id);
    rune.setCardId(id);
    rune.setOwnerId(ownerId);
    rune.setTapped(tapped);
    rune.setNormalEnergy(1);
    rune.setPremiumEnergy(2);
    return rune;
  }

  private void stubCard(String id, String type, int cost) {
    stubCard(id, id, type, cost, 1, 1, null);
  }

  private void stubCard(String id, String name, String type, int cost, int power, int health, String rulesText) {
    stubCard(id, name, type, cost, power, health, rulesText, List.of());
  }

  private void stubCard(String id, String name, String type, int cost, int power, int health, String rulesText, List<String> keywords) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, name, type, null, List.of(), cost, 0, null, null, null, rulesText, power, health, keywords));
  }

  private void stubCard(String id, String name, String type, int cost, int power, int health, String rulesText, List<String> keywords, List<String> domains) {
    when(cardDataService.getCard(id)).thenReturn(
        new CardDefinition(id, name, type, null, domains, cost, 0, null, null, null, rulesText, power, health, keywords));
  }

  private String zhonyasText() {
    return "[Hidden] (Hide now for :rb_rune_rainbow: to react with later for :rb_energy_0:.)"
        + "If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it. (Send it to base. This isn't a move.)";
  }

  private RevealedHandSnapshot revealedHand(String toPlayerId, String ownerId, String... instanceIds) {
    RevealedHandSnapshot snapshot = new RevealedHandSnapshot();
    snapshot.setRevealedToPlayerId(toPlayerId);
    snapshot.setRevealedOwnerId(ownerId);
    snapshot.setInstanceIds(List.of(instanceIds));
    return snapshot;
  }
}
