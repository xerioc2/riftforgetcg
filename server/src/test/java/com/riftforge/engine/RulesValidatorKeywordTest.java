package com.riftforge.engine;

import com.riftforge.model.*;
import com.riftforge.model.move.DeclareAttackMove;
import com.riftforge.model.move.DeclareBlockMove;
import com.riftforge.service.CardDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RulesValidatorKeywordTest {

    @Mock CardDataService cardDataService;

    RulesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RulesValidator(cardDataService);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CardInstance unit(String instanceId, String cardId, String ownerId, ZoneName zone, boolean tapped, boolean sick) {
        CardInstance c = new CardInstance();
        c.setInstanceId(instanceId);
        c.setCardId(cardId);
        c.setOwnerId(ownerId);
        c.setZone(zone);
        c.setTapped(tapped);
        c.setHasSummoningSickness(sick);
        c.setTempKeywords(new ArrayList<>());
        return c;
    }

    private PlayerState player(String id) {
        PlayerState p = new PlayerState();
        p.setUserId(id);
        p.setScore(0);
        return p;
    }

    private LiveGameState attackState(CardInstance attacker, CardInstance defender) {
        LiveGameState s = new LiveGameState();
        s.setCurrentPhase(Phase.ATTACK_DECLARE);
        s.setActivePlayerId(attacker.getOwnerId());
        s.setCards(new ArrayList<>(List.of(attacker, defender)));
        s.setPlayers(new ArrayList<>(List.of(player(attacker.getOwnerId()), player(defender.getOwnerId()))));
        s.setDeclaredAttackers(new ArrayList<>());
        s.setBlockerToAttacker(new HashMap<>());
        s.setLog(new ArrayList<>());
        return s;
    }

    private LiveGameState blockState(CardInstance attacker, CardInstance blocker) {
        LiveGameState s = new LiveGameState();
        s.setCurrentPhase(Phase.BLOCK_DECLARE);
        s.setActivePlayerId(attacker.getOwnerId());
        s.setCards(new ArrayList<>(List.of(attacker, blocker)));
        s.setPlayers(new ArrayList<>(List.of(player(attacker.getOwnerId()), player(blocker.getOwnerId()))));
        s.setDeclaredAttackers(new ArrayList<>(List.of(attacker.getInstanceId())));
        s.setBlockerToAttacker(new HashMap<>());
        s.setLog(new ArrayList<>());
        return s;
    }

    // ── ELUSIVE ──────────────────────────────────────────────────────────────

    @Test
    void elusive_cannotBeBlocked() {
        CardInstance atk = unit("a1", "c-elusive", "p1", ZoneName.BATTLEFIELD, false, false);
        CardInstance blk = unit("b1", "c-normal",  "p2", ZoneName.BATTLEFIELD, false, false);

        when(cardDataService.hasKeyword(atk, "ELUSIVE")).thenReturn(true);

        LiveGameState s = blockState(atk, blk);
        DeclareBlockMove move = new DeclareBlockMove("p2", Map.of("b1", "a1"));

        assertThatThrownBy(() -> validator.validate(s, move))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("ELUSIVE");
    }

    @Test
    void nonElusive_canBeBlocked() {
        CardInstance atk = unit("a1", "c-normal", "p1", ZoneName.BATTLEFIELD, false, false);
        CardInstance blk = unit("b1", "c-normal", "p2", ZoneName.BATTLEFIELD, false, false);

        when(cardDataService.hasKeyword(atk, "ELUSIVE")).thenReturn(false);

        LiveGameState s = blockState(atk, blk);
        DeclareBlockMove move = new DeclareBlockMove("p2", Map.of("b1", "a1"));

        assertThatNoException().isThrownBy(() -> validator.validate(s, move));
    }

    // ── RUSH ─────────────────────────────────────────────────────────────────

    @Test
    void rush_canAttackWithSummoningSickness() {
        CardInstance atk = unit("a1", "c-rush",  "p1", ZoneName.BATTLEFIELD, false, true); // sick=true
        CardInstance def = unit("d1", "c-normal", "p2", ZoneName.BATTLEFIELD, false, false);

        when(cardDataService.hasKeyword(atk, "RUSH")).thenReturn(true);

        LiveGameState s = attackState(atk, def);
        DeclareAttackMove move = new DeclareAttackMove("p1", List.of("a1"), "p2");

        assertThatNoException().isThrownBy(() -> validator.validate(s, move));
    }

    @Test
    void noRush_cannotAttackWithSummoningSickness() {
        CardInstance atk = unit("a1", "c-normal", "p1", ZoneName.BATTLEFIELD, false, true); // sick=true, no RUSH
        CardInstance def = unit("d1", "c-normal", "p2", ZoneName.BATTLEFIELD, false, false);

        when(cardDataService.hasKeyword(atk, "RUSH")).thenReturn(false);

        LiveGameState s = attackState(atk, def);
        DeclareAttackMove move = new DeclareAttackMove("p1", List.of("a1"), "p2");

        assertThatThrownBy(() -> validator.validate(s, move))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("summoning sickness");
    }

    @Test
    void noRush_readyUnitCanAlwaysAttack() {
        CardInstance atk = unit("a1", "c-normal", "p1", ZoneName.BATTLEFIELD, false, false); // not sick
        CardInstance def = unit("d1", "c-normal", "p2", ZoneName.BATTLEFIELD, false, false);

        when(cardDataService.hasKeyword(atk, "RUSH")).thenReturn(false);

        LiveGameState s = attackState(atk, def);
        DeclareAttackMove move = new DeclareAttackMove("p1", List.of("a1"), "p2");

        assertThatNoException().isThrownBy(() -> validator.validate(s, move));
    }

    @Test
    void tappedUnit_cannotAttackRegardlessOfRush() {
        CardInstance atk = unit("a1", "c-rush", "p1", ZoneName.BATTLEFIELD, true, false); // tapped
        CardInstance def = unit("d1", "c-normal","p2", ZoneName.BATTLEFIELD, false, false);

        LiveGameState s = attackState(atk, def);
        DeclareAttackMove move = new DeclareAttackMove("p1", List.of("a1"), "p2");

        assertThatThrownBy(() -> validator.validate(s, move))
                .isInstanceOf(IllegalMoveException.class)
                .hasMessageContaining("exhausted");
    }
}
