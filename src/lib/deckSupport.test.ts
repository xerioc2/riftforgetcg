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

describe('deckSupport', () => {
  it('uses a Defy-specific Partial reason for the alpha counter path', () => {
    const status = cardSupportStatus(spell('Defy', '[Reaction] Counter a spell that costs no more than 4 and no more than 1.'));

    expect(status.status).toBe('PARTIAL');
    expect(status.reason).toContain('no more than 4 energy');
    expect(status.reason).toContain('no more than 1 premium rune');
    expect(status.reason).toContain('countering counters remain deferred');
  });

  it('keeps Not So Fast unsupported', () => {
    const status = cardSupportStatus(spell('Not So Fast', '[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.'));

    expect(status.status).toBe('UNSUPPORTED');
    expect(status.reason).toContain('Counter spells');
  });
});
