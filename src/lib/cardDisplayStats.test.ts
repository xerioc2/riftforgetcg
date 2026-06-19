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

  it('prefers server-projected effective stats over local fallback math', () => {
    const host = instance({
      printedMight: 3,
      printedHealth: 4,
      effectiveMight: 7,
      effectiveMaxHealth: 6,
      currentHealth: 5,
      markedDamage: 1,
      statModifierLabels: ['Server Gear: +4 Might', 'Server Gear: +2 max HP'],
      mightBonus: 1,
      temporaryPowerModifier: 1,
    });

    const stats = cardDisplayStats(unit(), host);

    expect(stats.baseMight).toBe(3);
    expect(stats.effectiveMight).toBe(7);
    expect(stats.effectiveMaxHealth).toBe(6);
    expect(stats.currentHealth).toBe(5);
    expect(stats.markedDamage).toBe(1);
    expect(stats.mightLabel).toBe('Might 3 -> 7');
    expect(stats.healthLabel).toBe('5/4->6 HP');
    expect(stats.equipmentModifierLabels).toEqual(['Server Gear: +4 Might', 'Server Gear: +2 max HP']);
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

  it('does not invent modifiers for attached unsupported gear', () => {
    const host = instance();
    const gear = instance({ instanceId: 'gear-1', cardId: 'gear', attachedToInstanceId: host.instanceId });
    const cardsById = new Map<string, RiftCard>([
      ['unit', unit()],
      ['gear', { id: 'gear', name: 'Unsupported Gear', type: 'Gear', domains: [] }],
    ]);

    const stats = cardDisplayStats(unit(), host, { allInstances: [host, gear], cardsById });

    expect(stats.effectiveMight).toBe(3);
    expect(stats.equipmentModifierLabels).toEqual([]);
  });

  it('shows base to effective Might for explicitly supported attached gear modifiers', () => {
    const host = instance();
    const gear = instance({ instanceId: 'gear-1', cardId: 'might-gear', attachedToInstanceId: host.instanceId });
    const cardsById = new Map<string, RiftCard>([
      ['unit', unit()],
      ['might-gear', { id: 'might-gear', name: 'Might Gear', type: 'Gear', domains: [] }],
    ]);

    const stats = cardDisplayStats(unit(), host, {
      allInstances: [host, gear],
      cardsById,
      equipmentStatModifiers: { 'might-gear': { mightBonus: 2, maxHealthBonus: 0 } },
    });

    expect(stats.effectiveMight).toBe(5);
    expect(stats.mightLabel).toBe('Might 3 -> 5');
    expect(stats.equipmentModifierLabels).toEqual(['Might Gear: +2 Might']);
  });

  it('shows effective max HP for explicitly supported health modifiers', () => {
    const host = instance({ currentHealth: 4 });
    const gear = instance({ instanceId: 'gear-1', cardId: 'health-gear', attachedToInstanceId: host.instanceId });
    const cardsById = new Map<string, RiftCard>([
      ['unit', unit()],
      ['health-gear', { id: 'health-gear', name: 'Health Gear', type: 'Gear', domains: [] }],
    ]);

    const stats = cardDisplayStats(unit(), host, {
      allInstances: [host, gear],
      cardsById,
      equipmentStatModifiers: { 'health-gear': { mightBonus: 0, maxHealthBonus: 2 } },
    });

    expect(stats.healthLabel).toBe('4/4->6 HP');
    expect(stats.markedDamage).toBe(2);
    expect(stats.equipmentModifierLabels).toEqual(['Health Gear: +2 max HP']);
  });
});
