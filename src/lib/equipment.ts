import type { RiftCard } from '../types';

export type EquipCost = {
  energyCost: number;
  premiumDomains: string[];
};

export type EquipmentStatModifier = {
  mightBonus: number;
  maxHealthBonus: number;
};

export const EQUIPMENT_STAT_MODIFIERS: Record<string, EquipmentStatModifier> = {};

export function normalizeEquipDomain(domain: string) {
  return domain.trim().replace(/-/g, '_').toUpperCase();
}

export function equipCostForCard(card: RiftCard | undefined): EquipCost {
  const text = card?.rulesText ?? '';
  const header = text.match(/\[equip\]\s*([^\(\[]*)/i)?.[1] ?? '';
  const energyCost = [...header.matchAll(/:rb_energy_(\d+):/gi)]
    .reduce((total, match) => total + Number(match[1] ?? 0), 0);
  const premiumDomains = [...header.matchAll(/:rb_rune_([a-z_]+):/gi)]
    .map((match) => normalizeEquipDomain(match[1] ?? ''));
  return { energyCost, premiumDomains };
}

export function isEquipRulesText(card: RiftCard | undefined) {
  return /\[equip\]/i.test(card?.rulesText ?? '');
}

export function runeMatchesEquipDomain(requiredDomain: string, rune: RiftCard | undefined) {
  if (!rune) return false;
  const required = normalizeEquipDomain(requiredDomain);
  return rune.domains
    .filter((domain) => normalizeEquipDomain(domain) !== 'COLORLESS')
    .some((domain) => required === 'RAINBOW' || normalizeEquipDomain(domain) === required);
}

export function equipCostLabel(cost: EquipCost) {
  const parts: string[] = [];
  if (cost.energyCost > 0) parts.push(`${cost.energyCost} energy`);
  parts.push(...cost.premiumDomains.map((domain) => `${domain.toLowerCase()} rune`));
  return parts.length > 0 ? parts.join(' + ') : 'No printed Equip cost';
}

export function equipmentStatModifierForCard(
  card: RiftCard | undefined,
  modifiers: Record<string, EquipmentStatModifier> = EQUIPMENT_STAT_MODIFIERS,
) {
  if (!card) return undefined;
  return modifiers[card.id];
}
