import { describe, expect, it } from 'vitest';
import {
  equipmentStatModifierForCard,
  equipCostForCard,
  equipCostLabel,
  isEquipRulesText,
  normalizeEquipDomain,
  runeMatchesEquipDomain,
  selectEquipRecycleCardInstanceIds,
} from './equipment';
import type { CardInstance, RiftCard } from '../types';

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

function instance(instanceId: string, ownerId: string, zone: CardInstance['zone'], faceDown = false): CardInstance {
  return {
    instanceId,
    cardId: instanceId,
    ownerId,
    zone,
    x: 0,
    y: 0,
    zIndex: 0,
    tapped: false,
    faceDown,
    currentHealth: 1,
  };
}

describe('equipment helpers', () => {
  it('parses printed Equip energy and premium rune costs', () => {
    expect(equipCostForCard(card('[Equip] :rb_energy_2::rb_rune_calm: Attach this to a unit.'))).toEqual({
      energyCost: 2,
      premiumDomains: ['CALM'],
      recycleTrashCount: 0,
    });
  });

  it('does not count reminder text cost icons after the Equip header', () => {
    expect(equipCostForCard(card('[Equip] :rb_rune_chaos: (:rb_rune_chaos:: Attach this to a unit.)'))).toEqual({
      energyCost: 0,
      premiumDomains: ['CHAOS'],
      recycleTrashCount: 0,
    });
  });

  it('parses Last Rites recycle-trash equip cost', () => {
    expect(equipCostForCard(card('[Equip] — :rb_rune_chaos:, Recycle 2 cards from your trash (Pay the cost: Attach this to a unit you control.)'))).toEqual({
      energyCost: 0,
      premiumDomains: ['CHAOS'],
      recycleTrashCount: 2,
    });
  });

  it('normalizes equip domains case-insensitively', () => {
    expect(normalizeEquipDomain('mind-body')).toBe('MIND_BODY');
    expect(runeMatchesEquipDomain('calm', rune(['CALM']))).toBe(true);
    expect(runeMatchesEquipDomain('RAINBOW', rune(['FURY']))).toBe(true);
    expect(runeMatchesEquipDomain('CHAOS', rune(['COLORLESS']))).toBe(false);
  });

  it('formats readable equip cost labels', () => {
    expect(equipCostLabel({ energyCost: 1, premiumDomains: ['CHAOS'], recycleTrashCount: 0 })).toBe('1 energy + chaos rune');
    expect(equipCostLabel({ energyCost: 0, premiumDomains: ['CHAOS'], recycleTrashCount: 2 })).toBe('chaos rune + recycle 2 from Trash');
    expect(equipCostLabel({ energyCost: 0, premiumDomains: [], recycleTrashCount: 0 })).toBe('No printed Equip cost');
  });

  it('selects owner Trash cards for recycle-cost Equip without leaking invalid cards', () => {
    const cost = { energyCost: 0, premiumDomains: ['CHAOS'], recycleTrashCount: 2 };
    const cards = [
      instance('trash-1', 'p1', 'discard'),
      instance('opponent-trash', 'p2', 'discard'),
      instance('hidden-trash', 'p1', 'discard', true),
      instance('base-card', 'p1', 'base'),
      instance('trash-2', 'p1', 'discard'),
    ];

    expect(selectEquipRecycleCardInstanceIds(cards, 'p1', cost)).toEqual(['trash-1', 'trash-2']);
    expect(selectEquipRecycleCardInstanceIds(cards.slice(0, 4), 'p1', cost)).toBeNull();
    expect(selectEquipRecycleCardInstanceIds(cards, 'p1', { ...cost, recycleTrashCount: 0 })).toEqual([]);
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
