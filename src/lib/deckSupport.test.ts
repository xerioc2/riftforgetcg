import { describe, expect, it } from 'vitest';
import { cardSupportStatus } from './deckSupport';
import type { RiftCard } from '../types';

function spell(name: string, rulesText: string): RiftCard {
  return {
    id: name.toLowerCase().replace(/\s+/g, '-'),
    name,
    type: 'Spell',
    domains: [],
    rulesText,
  };
}

function unit(name: string, rulesText: string): RiftCard {
  return {
    id: name.toLowerCase().replace(/\s+/g, '-'),
    name,
    type: 'Unit',
    domains: [],
    rulesText,
  };
}

function gear(name: string, rulesText: string): RiftCard {
  return {
    id: name.toLowerCase().replace(/\s+/g, '-'),
    name,
    type: 'Gear',
    domains: [],
    rulesText,
  };
}

function legend(name: string, rulesText: string): RiftCard {
  return {
    id: name.toLowerCase().replace(/\s+/g, '-'),
    name,
    type: 'Legend',
    domains: [],
    rulesText,
  };
}

function champion(name: string, rulesText: string): RiftCard {
  return {
    id: name.toLowerCase().replace(/\s+/g, '-'),
    name,
    type: 'Champion',
    domains: [],
    rulesText,
  };
}

describe('deckSupport', () => {
  it('matches backend/docs support for promoted playtest units', () => {
    expect(cardSupportStatus(unit('Lonely Poro', '[Deathknell] If I died alone, draw 1.')).status).toBe('SUPPORTED');
    expect(cardSupportStatus(unit('Disarming Rake', 'When you play me, you may kill a gear.')).status).toBe('SUPPORTED');
  });

  it('uses a Scuttle Crab-specific Partial reason for deferred XP and facedown viewing', () => {
    const status = cardSupportStatus(unit('Scuttle Crab', 'When you play me, draw 1. Deathknell: Choose an opponent. They reveal their hand. You can look at their facedown cards this turn. Gain 1 XP.'));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain('private hand reveal');
    expect(status.reason).toContain('XP');
    expect(status.reason).toContain('facedown-card viewing');
  });

  it('uses a Defy-specific Partial reason for the alpha counter path', () => {
    const status = cardSupportStatus(spell('Defy', '[Reaction] Counter a spell that costs no more than 4 and no more than 1.'));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain('no more than 4 energy');
    expect(status.reason).toContain('no more than 1 premium rune');
    expect(status.reason).toContain('countering counters remain deferred');
  });

  it('uses a Not So Fast-specific Partial reason for the narrow alpha counter path', () => {
    const status = cardSupportStatus(spell('Not So Fast', '[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.'));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain('chooses your friendly Unit');
    expect(status.reason).toContain('Ability-chain targets');
    expect(status.reason).toContain('countering counters remain deferred');
  });

  it('uses an Abandon-specific Partial reason for the narrow alpha counter path', () => {
    const status = cardSupportStatus(spell('Abandon', "[Reaction] Counter a spell. Return it to its owner's hand instead of putting it in their trash. [Predict]."));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain('return that spell card to hand');
    expect(status.reason).toContain('private Predict choice');
    expect(status.reason).toContain('ability targets remain deferred');
  });

  it('uses a Hard Bargain-specific Partial reason for the tax counter path', () => {
    const status = cardSupportStatus(spell('Hard Bargain', "[Reaction] [Repeat] :rb_energy_2: Counter a spell unless its controller pays :rb_energy_2:."));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain("controller pays 2 energy");
    expect(status.reason).toContain('owner-only prompt');
    expect(status.reason).toContain('Repeat');
    expect(status.reason).toContain('countering counters remain deferred');
  });

  it('uses alpha chain-window Partial reasons for Discipline and En Garde', () => {
    const discipline = cardSupportStatus(spell('Discipline', '[Reaction] Give a unit +2 Might this turn. Draw 1.'));
    const enGarde = cardSupportStatus(spell('En Garde', '[Reaction] Give a friendly unit +1 Might this turn, then an additional +1 Might this turn if it is the only unit you control there.'));

    expect(discipline.status).toBe('PARTIAL');
    expect(discipline.reason).toContain('alpha chain-window Reaction support');
    expect(discipline.reason).toContain('+2 Might');
    expect(enGarde.status).toBe('PARTIAL');
    expect(enGarde.reason).toContain('alpha chain-window Reaction support');
    expect(enGarde.reason).toContain('only unit there');
  });

  it('uses alpha chain-window Partial reasons for Defiant Dance and Flash', () => {
    const defiantDance = cardSupportStatus(spell('Defiant Dance', '[Reaction] Give a unit +2 Might this turn and another unit -2 Might this turn.'));
    const flash = cardSupportStatus(spell('Flash', '[Reaction] Move up to 2 friendly units to base.'));

    expect(defiantDance.status).toBe('PARTIAL');
    expect(defiantDance.reason).toContain('+2 Might');
    expect(defiantDance.reason).toContain('-2 Might');
    expect(flash.status).toBe('PARTIAL');
    expect(flash.reason).toContain('up to two friendly');
    expect(flash.reason).toContain('Base');
  });

  it('uses a Charm-specific Partial reason for alpha enemy battlefield movement', () => {
    const charm = cardSupportStatus(spell('Charm', 'Move an enemy unit.'));

    expect(charm.status).toBe('PARTIAL');
    expect(charm.reason).toContain('enemy public battlefield Unit/Champion');
    expect(charm.reason).toContain('Base');
    expect(charm.reason).toContain('movement choices');
  });

  it('uses Battlefield-specific Partial reasons for Irelia Battlefield hooks', () => {
    const sunken = cardSupportStatus({
      id: 'sunken-temple',
      name: 'Sunken Temple',
      type: 'Battlefield',
      domains: [],
      rulesText: 'When you conquer here with one or more [Mighty] units, you may pay 1 to draw 1.',
    });
    const targon = cardSupportStatus({
      id: 'targons-peak',
      name: "Targon's Peak",
      type: 'Battlefield',
      domains: [],
      rulesText: 'When you conquer here, ready up to 2 runes at the end of this turn.',
    });
    const abandoned = cardSupportStatus({
      id: 'abandoned-hall',
      name: 'Abandoned Hall',
      type: 'Battlefield',
      domains: [],
      rulesText: 'When a player plays a spell, they may give a unit they control here +1 Might this turn.',
    });

    expect(sunken.status).toBe('PARTIAL');
    expect(sunken.reason).toContain('conquer-with-Mighty');
    expect(sunken.reason).toContain('optional pay-1 draw');
    expect(targon.status).toBe('PARTIAL');
    expect(targon.reason).toContain('queues end-turn readying');
    expect(targon.reason).toContain('Player-selected rune choice');
    expect(abandoned.status).toBe('PARTIAL');
    expect(abandoned.reason).toContain('spell-play optional trigger');
    expect(abandoned.reason).toContain('friendly public Unit/Champion here');
  });

  it('uses a The Syren-specific Partial reason for the alpha activated ability', () => {
    const syren = cardSupportStatus(gear('The Syren', ':rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.'));

    expect(syren.status).toBe('PARTIAL');
    expect(syren.reason).toContain('paying 1 energy');
    expect(syren.reason).toContain('friendly public battlefield Unit/Champion');
    expect(syren.reason).toContain('activated ability timing');
  });

  it('uses a Zhonyas Hourglass-specific Partial reason for the alpha would-die replacement', () => {
    const zhonya = cardSupportStatus(gear("Zhonya's Hourglass", "[Hidden] If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it."));

    expect(zhonya.status).toBe('PARTIAL');
    expect(zhonya.reason).toContain('protect a friendly public Unit/Champion');
    expect(zhonya.reason).toContain('Hidden Reaction-for-0 timing');
    expect(zhonya.reason).toContain('competing replacement choices');
  });

  it('uses an Irelia Blade Dancer-specific Partial reason for the alpha Legend ability', () => {
    const irelia = cardSupportStatus(legend('Irelia - Blade Dancer', 'When you choose a friendly unit, you may exhaust me and pay :rb_rune_rainbow: to ready it.When you conquer, you may pay :rb_energy_1: to ready me.'));

    expect(irelia.status).toBe('PARTIAL');
    expect(irelia.reason).toContain('Legend-zone activated ready ability');
    expect(irelia.reason).toContain('rainbow/premium rune');
    expect(irelia.reason).toContain('conquer trigger');
  });

  it('uses an Irelia Fervent-specific Partial reason for the alpha Champion trigger slice', () => {
    const irelia = cardSupportStatus(champion('Irelia - Fervent', '[Deflect] When you choose or ready me, give me +1 :rb_might: this turn.'));

    expect(irelia.status).toBe('PARTIAL');
    expect(irelia.reason).toContain('explicit-ready trigger');
    expect(irelia.reason).toContain('+1 Might this turn');
    expect(irelia.reason).toContain('choose-trigger coverage is incomplete');
    expect(irelia.reason).toContain('automatic ready-step trigger timing remains deferred');
  });
});
