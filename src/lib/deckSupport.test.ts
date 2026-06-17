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
});
