import { describe, expect, it } from 'vitest';
import type { CardInstance, RiftCard } from '../types';
import { cardDisplayStats } from './cardDisplayStats';

function unit(overrides: Partial<RiftCard> = {}): RiftCard {
  return {
    id: 'unit',
    name: 'Unit',
    type: 'Unit',
    domains: [],
    cost: 1,
    power: 3,
    health: 4,
    ...overrides,
  };
}

function instance(overrides: Partial<CardInstance> = {}): CardInstance {
  return {
    instanceId: 'unit-1',
    cardId: 'unit',
    ownerId: 'p1',
    zone: 'battlefield',
    x: 0,
    y: 0,
    tapped: false,
    faceDown: false,
    zIndex: 0,
    ...overrides,
  };
}

describe('cardDisplayStats', () => {
  it('shows base Might and full health when unmodified and undamaged', () => {
    const stats = cardDisplayStats(unit(), instance());

    expect(stats.mightLabel).toBe('Might 3');
    expect(stats.healthLabel).toBe('4/4 HP');
    expect(stats.damageLabel).toBeUndefined();
    expect(stats.mightModified).toBe(false);
  });

  it('shows effective Might when real modifiers exist', () => {
    const stats = cardDisplayStats(unit(), instance({ mightBonus: 1, temporaryPowerModifier: 1 }));

    expect(stats.effectiveMight).toBe(5);
    expect(stats.mightLabel).toBe('Might 3 -> 5');
    expect(stats.mightModified).toBe(true);
  });

  it('shows marked damage from current health', () => {
    const stats = cardDisplayStats(unit(), instance({ currentHealth: 2 }));

    expect(stats.healthLabel).toBe('2/4 HP');
    expect(stats.markedDamage).toBe(2);
    expect(stats.damageLabel).toBe('2 damage marked');
  });

  it('clamps displayed health safely', () => {
    expect(cardDisplayStats(unit(), instance({ currentHealth: -3 })).healthLabel).toBe('0/4 HP');
    expect(cardDisplayStats(unit(), instance({ currentHealth: 9 })).healthLabel).toBe('4/4 HP');
  });
});
