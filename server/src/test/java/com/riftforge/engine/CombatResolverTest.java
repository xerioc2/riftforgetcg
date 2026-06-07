package com.riftforge.engine;

import com.riftforge.effect.CardEffectRegistry;
import com.riftforge.model.CardDefinition;
import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.PlayerState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CombatResolverTest {

    @Mock CardDataService cardDataService;
    @Mock CardEffectRegistry effects;
    @Mock CardZoneService cardZoneService;

    CombatResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CombatResolver(cardDataService, effects, cardZoneService);
        when(effects.getEffect(anyString())).thenReturn(Optional.empty());
        // simulate unit going to DISCARD (non-champion behaviour)
        doAnswer(inv -> {
            ((CardInstance) inv.getArgument(0)).setZone(ZoneName.DISCARD);
            return null;
        }).when(cardZoneService).moveToGraveyard(any(CardInstance.class));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CardDefinition def(String id, String name, int power, int health, String... keywords) {
        return new CardDefinition(id, name, "Unit", null, List.of(), 0, 0, null, null, null, null, power, health, Arrays.asList(keywords));
    }

    private CardInstance card(String instanceId, String cardId, String ownerId, int currentHealth) {
        CardInstance c = new CardInstance();
        c.setInstanceId(instanceId);
        c.setCardId(cardId);
        c.setOwnerId(ownerId);
        c.setZone(ZoneName.BATTLEFIELD);
        c.setCurrentHealth(currentHealth);
        c.setTempKeywords(new ArrayList<>());
        return c;
    }

    private PlayerState player(String id) {
        PlayerState p = new PlayerState();
        p.setUserId(id);
        p.setScore(0);
        return p;
    }

    /** One attacker vs one blocker. */
    private LiveGameState blockedState(CardInstance atk, CardInstance blk) {
        LiveGameState s = new LiveGameState();
        s.setCards(new ArrayList<>(List.of(atk, blk)));
        s.setPlayers(new ArrayList<>(List.of(player(atk.getOwnerId()), player(blk.getOwnerId()))));
        s.setDeclaredAttackers(new ArrayList<>(List.of(atk.getInstanceId())));
        s.setBlockerToAttacker(new HashMap<>(Map.of(blk.getInstanceId(), atk.getInstanceId())));
        s.setLog(new ArrayList<>());
        return s;
    }

    /** One unblocked attacker. */
    private LiveGameState unblockedState(CardInstance atk) {
        LiveGameState s = new LiveGameState();
        s.setCards(new ArrayList<>(List.of(atk)));
        s.setPlayers(new ArrayList<>(List.of(player(atk.getOwnerId()), player("p2"))));
        s.setDeclaredAttackers(new ArrayList<>(List.of(atk.getInstanceId())));
        s.setBlockerToAttacker(new HashMap<>());
        s.setLog(new ArrayList<>());
        return s;
    }

    private PlayerState scoreOf(LiveGameState s, String playerId) {
        return s.getPlayers().stream().filter(p -> p.getUserId().equals(playerId)).findFirst().orElseThrow();
    }

    // ── OVERWHELM ────────────────────────────────────────────────────────────

    @Test
    void overwhelm_scoresEvenWhenAttackerDiesInTrade() {
        // 2/2 OVERWHELM vs 2/2: mutual death → attacker should still score
        CardInstance atk = card("a1", "c-overwhelm", "p1", 2);
        CardInstance blk = card("b1", "c-normal",    "p2", 2);
        when(cardDataService.getCard("c-overwhelm")).thenReturn(def("c-overwhelm", "Overwhelm Unit", 2, 2, "OVERWHELM"));
        when(cardDataService.getCard("c-normal")).thenReturn(def("c-normal", "Normal Unit", 2, 2));
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(true);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(atk.getCurrentHealth()).isLessThanOrEqualTo(0);
        assertThat(blk.getZone()).isEqualTo(ZoneName.DISCARD);
        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(1);
    }

    @Test
    void noOverwhelm_noScoreWhenAttackerDiesInTrade() {
        // 2/2 vs 2/2 (no OVERWHELM): mutual death → no score
        CardInstance atk = card("a1", "c-a", "p1", 2);
        CardInstance blk = card("b1", "c-b", "p2", 2);
        when(cardDataService.getCard("c-a")).thenReturn(def("c-a", "Unit A", 2, 2));
        when(cardDataService.getCard("c-b")).thenReturn(def("c-b", "Unit B", 2, 2));
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(0);
    }

    @Test
    void overwhelm_scoresNormallyWhenAttackerAlsoSurvives() {
        // 4/4 OVERWHELM vs 2/2: attacker survives AND scores (same as normal win)
        CardInstance atk = card("a1", "c-overwhelm", "p1", 4);
        CardInstance blk = card("b1", "c-normal",    "p2", 2);
        when(cardDataService.getCard("c-overwhelm")).thenReturn(def("c-overwhelm", "Overwhelm Unit", 4, 4, "OVERWHELM"));
        when(cardDataService.getCard("c-normal")).thenReturn(def("c-normal", "Normal Unit", 2, 2));
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(true);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(atk.getCurrentHealth()).isGreaterThan(0);
        assertThat(blk.getZone()).isEqualTo(ZoneName.DISCARD);
        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(1);
    }

    // ── LIFESTEAL ─────────────────────────────────────────────────────────────

    @Test
    void lifesteal_attackerHealsForDamageDealt() {
        // 3/4 LIFESTEAL vs 1/2: attacker takes 1 dmg, heals 3 → ends at full health (4)
        CardInstance atk = card("a1", "c-lifesteal", "p1", 4);
        CardInstance blk = card("b1", "c-weak",      "p2", 2);
        when(cardDataService.getCard("c-lifesteal")).thenReturn(def("c-lifesteal", "Lifesteal Unit", 3, 4, "LIFESTEAL"));
        when(cardDataService.getCard("c-weak")).thenReturn(def("c-weak", "Weak Unit", 1, 2));
        when(cardDataService.hasKeyword(atk, "LIFESTEAL")).thenReturn(true);
        when(cardDataService.hasKeyword(blk, "LIFESTEAL")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        // 4 - 1 + 3 = 6, capped at max health 4
        assertThat(atk.getCurrentHealth()).isEqualTo(4);
        assertThat(blk.getZone()).isEqualTo(ZoneName.DISCARD);
    }

    @Test
    void lifesteal_blockerHealsForDamageDealt() {
        // 1/4 attacker vs 2/3 LIFESTEAL blocker: blocker takes 1 dmg, heals 2 → full health (3)
        CardInstance atk = card("a1", "c-weak",      "p1", 4);
        CardInstance blk = card("b1", "c-lifesteal", "p2", 3);
        when(cardDataService.getCard("c-weak")).thenReturn(def("c-weak", "Weak Unit", 1, 4));
        when(cardDataService.getCard("c-lifesteal")).thenReturn(def("c-lifesteal", "Lifesteal Unit", 2, 3, "LIFESTEAL"));
        when(cardDataService.hasKeyword(atk, "LIFESTEAL")).thenReturn(false);
        when(cardDataService.hasKeyword(blk, "LIFESTEAL")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        // blocker: 3 - 1 + 2 = 4, capped at max health 3
        assertThat(blk.getCurrentHealth()).isEqualTo(3);
    }

    @Test
    void lifesteal_doesNotExceedMaxHealth() {
        // LIFESTEAL unit at full health — healing cannot push above max
        CardInstance atk = card("a1", "c-lifesteal", "p1", 4);
        CardInstance blk = card("b1", "c-0pwr",      "p2", 2);
        when(cardDataService.getCard("c-lifesteal")).thenReturn(def("c-lifesteal", "Lifesteal Unit", 3, 4, "LIFESTEAL"));
        when(cardDataService.getCard("c-0pwr")).thenReturn(def("c-0pwr", "Zero Power Unit", 0, 2));
        when(cardDataService.hasKeyword(atk, "LIFESTEAL")).thenReturn(true);
        when(cardDataService.hasKeyword(blk, "LIFESTEAL")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        // attacker: 4 - 0 + 3 = 7, capped at 4
        assertThat(atk.getCurrentHealth()).isEqualTo(4);
    }

    // ── TOUGH ─────────────────────────────────────────────────────────────────

    @Test
    void tough_onBlocker_reducesIncomingDamageByOne() {
        // 3/4 attacker vs 2/3 TOUGH blocker: attacker deals 3-1=2, blocker survives with 1 hp
        CardInstance atk = card("a1", "c-strong", "p1", 4);
        CardInstance blk = card("b1", "c-tough",  "p2", 3);
        when(cardDataService.getCard("c-strong")).thenReturn(def("c-strong", "Strong Unit", 3, 4));
        when(cardDataService.getCard("c-tough")).thenReturn(def("c-tough", "Tough Unit", 2, 3, "TOUGH"));
        when(cardDataService.hasKeyword(blk, "TOUGH")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "TOUGH")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(blk.getCurrentHealth()).isEqualTo(1);
        assertThat(blk.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
        assertThat(atk.getCurrentHealth()).isEqualTo(2);
    }

    @Test
    void tough_onAttacker_reducesIncomingDamageByOne() {
        // 2/4 TOUGH attacker vs 3/3 blocker: blocker deals 3-1=2 to attacker
        CardInstance atk = card("a1", "c-tough",   "p1", 4);
        CardInstance blk = card("b1", "c-strong",  "p2", 3);
        when(cardDataService.getCard("c-tough")).thenReturn(def("c-tough", "Tough Unit", 2, 4, "TOUGH"));
        when(cardDataService.getCard("c-strong")).thenReturn(def("c-strong", "Strong Unit", 3, 3));
        when(cardDataService.hasKeyword(blk, "TOUGH")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "TOUGH")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        // attacker: 4 - (3-1)=2 → 2 hp remaining
        assertThat(atk.getCurrentHealth()).isEqualTo(2);
        // blocker: 3 - 2 = 1 hp remaining
        assertThat(blk.getCurrentHealth()).isEqualTo(1);
    }

    @Test
    void tough_cannotReduceDamageBelow0() {
        // 1-might attacker vs TOUGH blocker: effective damage is max(0, 1-1)=0, blocker unhurt
        CardInstance atk = card("a1", "c-weak",  "p1", 2);
        CardInstance blk = card("b1", "c-tough", "p2", 3);
        when(cardDataService.getCard("c-weak")).thenReturn(def("c-weak", "Weak Unit", 1, 2));
        when(cardDataService.getCard("c-tough")).thenReturn(def("c-tough", "Tough Unit", 1, 3, "TOUGH"));
        when(cardDataService.hasKeyword(blk, "TOUGH")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "TOUGH")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(blk.getCurrentHealth()).isEqualTo(3); // no damage
    }

    // ── MIGHTY ────────────────────────────────────────────────────────────────

    @Test
    void mighty_addsTwoMightWhenAttackingAlone() {
        // 2/4 MIGHTY solo attacker vs 3/3 blocker: effective might 4, kills blocker, attacker survives
        CardInstance atk = card("a1", "c-mighty",  "p1", 4);
        CardInstance blk = card("b1", "c-normal",  "p2", 3);
        when(cardDataService.getCard("c-mighty")).thenReturn(def("c-mighty", "Mighty Unit", 2, 4, "MIGHTY"));
        when(cardDataService.getCard("c-normal")).thenReturn(def("c-normal", "Normal Unit", 3, 3));
        when(cardDataService.hasKeyword(atk, "MIGHTY")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "OVERWHELMING")).thenReturn(false);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(blk.getZone()).isEqualTo(ZoneName.DISCARD);    // blocker killed (took 4 dmg)
        assertThat(atk.getCurrentHealth()).isEqualTo(1);          // atk: 4 - 3 = 1
        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(1);
    }

    @Test
    void mighty_noBonusWithMultipleAttackers() {
        // MIGHTY unit attacks alongside an ally — no +2 bonus, blocker survives
        CardInstance atk  = card("a1", "c-mighty", "p1", 4);
        CardInstance ally = card("a2", "c-ally",   "p1", 3);
        CardInstance blk  = card("b1", "c-normal", "p2", 3);
        when(cardDataService.getCard("c-mighty")).thenReturn(def("c-mighty", "Mighty Unit", 2, 4, "MIGHTY"));
        when(cardDataService.getCard("c-ally")).thenReturn(def("c-ally",   "Ally Unit",   1, 3));
        when(cardDataService.getCard("c-normal")).thenReturn(def("c-normal", "Normal Unit", 3, 3));
        when(cardDataService.hasKeyword(atk, "MIGHTY")).thenReturn(true);
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);

        LiveGameState s = new LiveGameState();
        s.setCards(new ArrayList<>(List.of(atk, ally, blk)));
        s.setPlayers(new ArrayList<>(List.of(player("p1"), player("p2"))));
        s.setDeclaredAttackers(new ArrayList<>(List.of("a1", "a2"))); // two attackers
        s.setBlockerToAttacker(new HashMap<>(Map.of("b1", "a1")));
        s.setLog(new ArrayList<>());

        resolver.resolve(s);

        // atk deals 2 (no bonus) — blocker 3-2=1, survives
        assertThat(blk.getCurrentHealth()).isEqualTo(1);
        assertThat(blk.getZone()).isEqualTo(ZoneName.BATTLEFIELD);
    }

    // ── unblocked scoring (baseline) ─────────────────────────────────────────

    @Test
    void unblocked_attackerAlwaysScores() {
        CardInstance atk = card("a1", "c-normal", "p1", 2);
        when(cardDataService.getCard("c-normal")).thenReturn(def("c-normal", "Normal Unit", 2, 2));
        LiveGameState s = unblockedState(atk);

        resolver.resolve(s);

        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(1);
    }

    @Test
    void normalWin_attackerScoresAndBlockerDies() {
        // 3/3 attacker vs 2/2 blocker: attacker wins cleanly
        CardInstance atk = card("a1", "c-strong", "p1", 3);
        CardInstance blk = card("b1", "c-weak",   "p2", 2);
        when(cardDataService.getCard("c-strong")).thenReturn(def("c-strong", "Strong Unit", 3, 3));
        when(cardDataService.getCard("c-weak")).thenReturn(def("c-weak",   "Weak Unit",   2, 2));
        when(cardDataService.hasKeyword(atk, "OVERWHELM")).thenReturn(false);
        LiveGameState s = blockedState(atk, blk);

        resolver.resolve(s);

        assertThat(blk.getZone()).isEqualTo(ZoneName.DISCARD);
        assertThat(atk.getCurrentHealth()).isEqualTo(1);
        assertThat(scoreOf(s, "p1").getScore()).isEqualTo(1);
    }
}
