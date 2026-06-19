import { describe, expect, it } from 'vitest';
import {
  equipmentStatModifierForCard,
  equipCostForCard,
  equipCostLabel,
  isEquipRulesText,
  normalizeEquipDomain,
  runeMatchesEquipDomain,
} from './equipment';
import type { RiftCard } from '../types';

function card(rulesText: string): RiftCard {
  return {
    id: 'gear',
    name: 'Test Gear',
    type: 'Gear',
    domains: [],
    rulesText,
  };
}

function rune(domains: string[]): RiftCard {
  return {
    id: 'rune',
    name: 'Test Rune',
    type: 'Rune',
    domains,
  };
}

describe('equipment helpers', () => {
  it('parses printed Equip energy and premium rune costs', () => {
    expect(equipCostForCard(card('[Equip] :rb_energy_2::rb_rune_calm: Attach this to a unit.'))).toEqual({
      energyCost: 2,
      premiumDomains: ['CALM'],
    });
  });

  it('does not count reminder text cost icons after the Equip header', () => {
    expect(equipCostForCard(card('[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit.)'))).toEqual({
      energyCost: 0,
      premiumDomains: ['CHAOS'],
    });
  });

  it('normalizes equip domains case-insensitively', () => {
    expect(normalizeEquipDomain('mind-body')).toBe('MIND_BODY');
    expect(runeMatchesEquipDomain('calm', rune(['CALM']))).toBe(true);
    expect(runeMatchesEquipDomain('RAINBOW', rune(['FURY']))).toBe(true);
    expect(runeMatchesEquipDomain('CHAOS', rune(['COLORLESS']))).toBe(false);
  });

  it('formats readable equip cost labels', () => {
    expect(equipCostLabel({ energyCost: 1, premiumDomains: ['CHAOS'] })).toBe('1 energy + chaos rune');
    expect(equipCostLabel({ energyCost: 0, premiumDomains: [] })).toBe('No printed Equip cost');
  });

  it('detects Equip rules text', () => {
    expect(isEquipRulesText(card('[Equip] :rb_rune_calm:'))).toBe(true);
    expect(isEquipRulesText(card('Quick-Draw'))).toBe(false);
  });

  it('only reports stat modifiers from explicit support metadata', () => {
    const gear = card('[Equip] :rb_rune_calm:');

    expect(equipmentStatModifierForCard(gear)).toBeUndefined();
    expect(equipmentStatModifierForCard(gear, { gear: { mightBonus: 2, maxHealthBonus: 0 } })).toEqual({
      mightBonus: 2,
      maxHealthBonus: 0,
    });
  });
});
