import type { CardInstance, RiftCard } from '../types';

export type EquipCost = {
  energyCost: number;
  premiumDomains: string[];
  recycleTrashCount: number;
};

export type EquipmentStatModifier = {
  mightBonus: number;
  maxHealthBonus: number;
};

// Server-projected effective stats are authoritative. This client registry stays
// empty in production and exists only as a display fallback for unprojected/test data.
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
  const recycleTrashCount = Number(header.match(/recycle\s+(\d+)\s+cards?\s+from\s+your\s+trash/i)?.[1] ?? 0);
  return { energyCost, premiumDomains, recycleTrashCount };
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
  if (cost.recycleTrashCount > 0) {
    parts.push(`recycle ${cost.recycleTrashCount} from Trash`);
  }
  return parts.length > 0 ? parts.join(' + ') : 'No printed Equip cost';
}

export function selectEquipRecycleCardInstanceIds(
  cards: CardInstance[] | undefined,
  playerId: string,
  cost: EquipCost,
) {
  const required = Math.max(0, cost.recycleTrashCount);
  if (required === 0) return [];
  const candidates = (cards ?? []).filter((card) =>
    card.ownerId === playerId
    && card.zone.toLowerCase() === 'discard'
    && !card.faceDown);
  if (candidates.length < required) return null;
  return candidates.slice(0, required).map((card) => card.instanceId);
}

export function equipmentStatModifierForCard(
  card: RiftCard | undefined,
  modifiers: Record<string, EquipmentStatModifier> = EQUIPMENT_STAT_MODIFIERS,
) {
  if (!card) return undefined;
  return modifiers[card.id];
}
