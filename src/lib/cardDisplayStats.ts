import type { CardInstance, RiftCard } from '../types';

export type CardDisplayStats = {
  hasCombatStats: boolean;
  baseMight: number;
  effectiveMight: number;
  mightModified: boolean;
  maxHealth: number;
  currentHealth: number;
  markedDamage: number;
  mightLabel: string;
  healthLabel: string;
  damageLabel?: string;
};

export function cardDisplayStats(card: RiftCard | undefined, instance?: CardInstance): CardDisplayStats {
  const baseMight = card?.power ?? 0;
  const mightBonus = instance?.mightBonus ?? 0;
  const temporaryMight = instance?.temporaryPowerModifier ?? 0;
  const effectiveMight = Math.max(0, baseMight + mightBonus + temporaryMight);
  const maxHealth = card?.health ?? 0;
  const currentHealth = maxHealth > 0
    ? Math.max(0, Math.min(maxHealth, instance?.currentHealth ?? maxHealth))
    : 0;
  const markedDamage = Math.max(0, maxHealth - currentHealth);
  const mightModified = effectiveMight !== baseMight;
  const hasCombatStats = card?.power != null || card?.health != null;

  return {
    hasCombatStats,
    baseMight,
    effectiveMight,
    mightModified,
    maxHealth,
    currentHealth,
    markedDamage,
    mightLabel: mightModified ? `Might ${baseMight} -> ${effectiveMight}` : `Might ${baseMight}`,
    healthLabel: maxHealth > 0 ? `${currentHealth}/${maxHealth} HP` : 'No HP',
    damageLabel: markedDamage > 0 ? `${markedDamage} damage marked` : undefined,
  };
}
