import { describe, expect, it } from 'vitest';
import type { RiftCard, RuneInstance } from '../types';
import { primaryRuneDomain, runeDisplayState } from './runeDisplay';

function rune(overrides: Partial<RuneInstance> = {}): RuneInstance {
  return {
    instanceId: 'rune-1',
    cardId: 'calm-rune',
    ownerId: 'p1',
    tapped: false,
    normalEnergy: 1,
    premiumEnergy: 2,
    ...overrides,
  };
}

function card(overrides: Partial<RiftCard> = {}): RiftCard {
  return {
    id: 'calm-rune',
    name: 'Calm Rune',
    type: 'Rune',
    domains: ['CALM'],
    imageUrl: 'https://example.invalid/calm.png',
    ...overrides,
  };
}

describe('runeDisplayState', () => {
  it('uses card-backed display when rune card data exists', () => {
    const display = runeDisplayState(rune(), card());

    expect(display.usesCardArt).toBe(true);
    expect(display.label).toBe('Calm Rune');
    expect(display.domainLabel).toBe('CALM');
    expect(display.title).toContain('Calm Rune');
  });

  it('falls back to generic rune display when card data is missing', () => {
    const display = runeDisplayState(rune(), undefined);

    expect(display.usesCardArt).toBe(false);
    expect(display.label).toBe('Unknown Rune');
    expect(display.domainLabel).toBe('Rune');
  });

  it('makes tapped and pending runes visually distinct', () => {
    const tapped = runeDisplayState(rune({ tapped: true }), card());
    const pending = runeDisplayState(rune({ tapped: true }), card(), true);

    expect(tapped.opacity).toBeLessThan(1);
    expect(pending.opacity).toBe(1);
    expect(pending.borderColor).not.toBe(tapped.borderColor);
    expect(pending.fillColor).not.toBe(tapped.fillColor);
  });

  it('ignores colorless when choosing a compact domain badge', () => {
    expect(primaryRuneDomain(card({ domains: ['COLORLESS', 'CHAOS'] }))).toBe('CHAOS');
  });
});
