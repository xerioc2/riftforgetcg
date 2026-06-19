import type { CardInstance, RiftCard } from '../types';
import { EQUIPMENT_STAT_MODIFIERS, equipmentStatModifierForCard, type EquipmentStatModifier } from './equipment';

export type CardDisplayStatsOptions = {
  allInstances?: CardInstance[];
  cardsById?: Map<string, RiftCard>;
  equipmentStatModifiers?: Record<string, EquipmentStatModifier>;
};

export type CardDisplayStats = {
  hasCombatStats: boolean;
  baseMight: number;
  effectiveMight: number;
  mightModified: boolean;
  baseHealth: number;
  effectiveMaxHealth: number;
  healthModified: boolean;
  maxHealth: number;
  currentHealth: number;
  markedDamage: number;
  mightLabel: string;
  healthLabel: string;
  damageLabel?: string;
  equipmentModifierLabels: string[];
};

export function cardDisplayStats(
  card: RiftCard | undefined,
  instance?: CardInstance,
  options: CardDisplayStatsOptions = {},
): CardDisplayStats {
  const baseMight = card?.power ?? 0;
  const mightBonus = instance?.mightBonus ?? 0;
  const temporaryMight = instance?.temporaryPowerModifier ?? 0;
  const equipmentModifiers = attachedEquipmentModifiers(instance, options);
  const equipmentMight = equipmentModifiers.reduce((total, item) => total + item.modifier.mightBonus, 0);
  const equipmentMaxHealth = equipmentModifiers.reduce((total, item) => total + item.modifier.maxHealthBonus, 0);
  const effectiveMight = Math.max(0, baseMight + mightBonus + temporaryMight + equipmentMight);
  const baseHealth = card?.health ?? 0;
  const effectiveMaxHealth = Math.max(0, baseHealth + equipmentMaxHealth);
  const currentHealth = effectiveMaxHealth > 0
    ? Math.max(0, Math.min(effectiveMaxHealth, instance?.currentHealth ?? effectiveMaxHealth))
    : 0;
  const markedDamage = Math.max(0, effectiveMaxHealth - currentHealth);
  const mightModified = effectiveMight !== baseMight;
  const healthModified = effectiveMaxHealth !== baseHealth;
  const hasCombatStats = card?.power != null || card?.health != null;
  const equipmentModifierLabels = equipmentModifiers.flatMap(({ name, modifier }) => {
    const labels: string[] = [];
    if (modifier.mightBonus !== 0) labels.push(`${name}: ${modifier.mightBonus > 0 ? '+' : ''}${modifier.mightBonus} Might`);
    if (modifier.maxHealthBonus !== 0) labels.push(`${name}: ${modifier.maxHealthBonus > 0 ? '+' : ''}${modifier.maxHealthBonus} max HP`);
    return labels;
  });

  return {
    hasCombatStats,
    baseMight,
    effectiveMight,
    mightModified,
    baseHealth,
    effectiveMaxHealth,
    healthModified,
    maxHealth: effectiveMaxHealth,
    currentHealth,
    markedDamage,
    mightLabel: mightModified ? `Might ${baseMight} -> ${effectiveMight}` : `Might ${baseMight}`,
    healthLabel: effectiveMaxHealth > 0
      ? healthModified ? `${currentHealth}/${baseHealth}->${effectiveMaxHealth} HP` : `${currentHealth}/${effectiveMaxHealth} HP`
      : 'No HP',
    damageLabel: markedDamage > 0 ? `${markedDamage} damage marked` : undefined,
    equipmentModifierLabels,
  };
}

function attachedEquipmentModifiers(instance: CardInstance | undefined, options: CardDisplayStatsOptions) {
  if (!instance || !options.allInstances || !options.cardsById) return [];
  const registry = options.equipmentStatModifiers ?? EQUIPMENT_STAT_MODIFIERS;
  return options.allInstances
    .filter((candidate) => candidate.attachedToInstanceId === instance.instanceId && !candidate.faceDown)
    .map((candidate) => {
      const card = options.cardsById?.get(candidate.cardId);
      const modifier = equipmentStatModifierForCard(card, registry);
      if (!card || !modifier || (modifier.mightBonus === 0 && modifier.maxHealthBonus === 0)) return null;
      return { name: card.name, modifier };
    })
    .filter((item): item is { name: string; modifier: EquipmentStatModifier } => item != null);
}
