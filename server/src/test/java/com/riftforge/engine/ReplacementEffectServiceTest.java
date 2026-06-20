package com.riftforge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ReplacementEffect;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplacementEffectServiceTest {
  private CardDataService cardDataService;
  private CardEffectRegistry effects;
  private CardZoneService cardZoneService;
  private DeathTriggerService deathTriggerService;
  private ReplacementEffectService replacementEffectService;
  private DeathService deathService;

  @BeforeEach
  void setUp() {
    cardDataService = mock(CardDataService.class);
    effects = mock(CardEffectRegistry.class);
    cardZoneService = new CardZoneService(cardDataService);
    deathTriggerService = new DeathTriggerService(cardDataService, List.of());
    replacementEffectService = new ReplacementEffectService(cardDataService, cardZoneService);
    deathService = new DeathService(cardDataService, effects, cardZoneService, deathTriggerService, replacementEffectService);
    when(effects.getEffect(anyString())).thenReturn(Optional.empty());
    when(cardDataService.getCard("unit")).thenReturn(card("unit", "Protected Unit", "Unit", 2));
    when(cardDataService.getCard("other-unit")).thenReturn(card("other-unit", "Other Unit", "Unit", 2));
    when(cardDataService.getCard("source-gear")).thenReturn(card("source-gear", "Source Gear", "Gear", 0));
    when(cardDataService.getCard("spell")).thenReturn(card("spell", "Resolved Spell", "Spell", 0));
    when(cardDataService.hasKeyword(anyString(), anyString())).thenReturn(false);
  }

  @Test
  void noReplacementUnitDiesNormallyThroughDeathService() {
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(protectedUnit);

    deathService.resolveDeaths(state, List.of(protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getReplacementEffects()).isEmpty();
  }

  @Test
  void registeredWouldDieReplacementDestroysSourceInsteadAndIsConsumed() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(source, protectedUnit);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    deathService.resolveDeaths(state, List.of(protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(source.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(protectedUnit.getBattlefieldLocationId()).isNull();
    assertThat(protectedUnit.getCurrentHealth()).isEqualTo(2);
    assertThat(protectedUnit.isTapped()).isTrue();
    assertThat(state.getReplacementEffects()).isEmpty();
  }

  @Test
  void samePassDoesNotImmediatelyKillProtectedCardAgain() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    protectedUnit.setCurrentHealth(0);
    LiveGameState state = state(source, protectedUnit);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    deathService.resolveDeaths(state, List.of(protectedUnit, protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(source.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(protectedUnit.getCurrentHealth()).isEqualTo(2);
    assertThat(protectedUnit.isTapped()).isTrue();
  }

  @Test
  void missingSourceMeansReplacementDoesNotApplyAndProtectedDiesNormally() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(source, protectedUnit);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");
    state.getCards().remove(source);

    deathService.resolveDeaths(state, List.of(protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(state.getReplacementEffects()).isEmpty();
  }

  @Test
  void replacementOnlyProtectsRegisteredProtectedCard() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    CardInstance other = instance("other", "other-unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(source, protectedUnit, other);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    deathService.resolveDeaths(state, List.of(other), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(other.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(source.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(state.getReplacementEffects()).hasSize(1);
  }

  @Test
  void attachedGearSourceMovesToTrashAndProtectedAttachmentsRemain() {
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    CardInstance sourceGear = instance("source", "source-gear", "p1", ZoneName.BASE);
    sourceGear.setAttachedToInstanceId("protected");
    CardInstance otherGear = instance("other-gear", "source-gear", "p1", ZoneName.BASE);
    otherGear.setAttachedToInstanceId("protected");
    LiveGameState state = state(sourceGear, protectedUnit, otherGear);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    deathService.resolveDeaths(state, List.of(protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(sourceGear.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(sourceGear.getAttachedToInstanceId()).isNull();
    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(otherGear.getAttachedToInstanceId()).isEqualTo("protected");
    assertThat(otherGear.getZone()).isEqualTo(ZoneName.BASE);
  }

  @Test
  void multipleReplacementsUseOldestDeterministically() {
    CardInstance firstSource = instance("source-1", "source-gear", "p1", ZoneName.BASE);
    CardInstance secondSource = instance("source-2", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(firstSource, secondSource, protectedUnit);
    ReplacementEffect first = replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source-1", "protected");
    ReplacementEffect second = replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source-2", "protected");

    deathService.resolveDeaths(state, List.of(protectedUnit), DeathEvent.DeathCause.EFFECT, "was destroyed.");

    assertThat(firstSource.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(secondSource.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(protectedUnit.isTapped()).isTrue();
    assertThat(state.getReplacementEffects()).singleElement()
        .extracting(ReplacementEffect::getReplacementEffectId)
        .isEqualTo(second.getReplacementEffectId());
    assertThat(first.getCreatedSequence()).isLessThan(second.getCreatedSequence());
  }

  @Test
  void registrationValidatesSourceProtectedAndController() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    LiveGameState state = state(source, protectedUnit);

    assertThatThrownBy(() -> replacementEffectService.registerWouldDieDestroySourceInstead(state, "p2", "source", "protected"))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("control");
    assertThatThrownBy(() -> replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "protected", "protected"))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("cannot protect itself");
  }

  @Test
  void nonDeathTrashMoveDoesNotConsumeReplacement() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance protectedUnit = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    CardInstance spell = instance("spell", "spell", "p1", ZoneName.LIMBO);
    LiveGameState state = state(source, protectedUnit, spell);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    cardZoneService.moveToGraveyard(spell);

    assertThat(spell.getZone()).isEqualTo(ZoneName.DISCARD);
    assertThat(source.getZone()).isEqualTo(ZoneName.BASE);
    assertThat(protectedUnit.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    assertThat(state.getReplacementEffects()).hasSize(1);
  }

  @Test
  void sameSourceCannotRegisterTwoActiveProtections() {
    CardInstance source = instance("source", "source-gear", "p1", ZoneName.BASE);
    CardInstance firstProtected = instance("protected", "unit", "p1", ZoneName.BATTLEFIELD);
    CardInstance secondProtected = instance("other", "other-unit", "p1", ZoneName.BASE);
    LiveGameState state = state(source, firstProtected, secondProtected);
    replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "protected");

    assertThatThrownBy(() -> replacementEffectService.registerWouldDieDestroySourceInstead(state, "p1", "source", "other"))
        .isInstanceOf(IllegalMoveException.class)
        .hasMessageContaining("already protecting");
    assertThat(state.getReplacementEffects()).hasSize(1);
  }

  private LiveGameState state(CardInstance... cards) {
    LiveGameState state = new LiveGameState();
    PlayerState player = new PlayerState();
    player.setUserId("p1");
    PlayerState opponent = new PlayerState();
    opponent.setUserId("p2");
    state.setPlayers(List.of(player, opponent));
    state.setCards(new ArrayList<>(List.of(cards)));
    return state;
  }

  private CardInstance instance(String instanceId, String cardId, String ownerId, ZoneName zone) {
    CardInstance card = new CardInstance();
    card.setInstanceId(instanceId);
    card.setCardId(cardId);
    card.setOwnerId(ownerId);
    card.setZone(zone);
    card.setCurrentHealth(2);
    return card;
  }

  private CardDefinition card(String id, String name, String type, int health) {
    return new CardDefinition(id, name, type, null, List.of(), 0, 0, "COMMON", "Test", null, "", 1, health, List.of());
  }
}
