import type { CardInstance, CombatDamageAssignment, RiftCard } from '../types';

function sameZone(zone: string, expected: string) {
  return zone.toLowerCase() === expected.toLowerCase();
}

function hasKeyword(card: RiftCard | undefined, keyword: string) {
  return card?.keywords?.some((value) => value.toUpperCase().startsWith(keyword.toUpperCase())) ?? false;
}

function keywordValue(card: RiftCard | undefined, keyword: string) {
  const match = card?.keywords?.find((value) => value.toUpperCase().startsWith(keyword.toUpperCase()));
  if (!match) return 0;
  const numeric = match.match(/\d+/);
  return numeric ? Number(numeric[0]) : 1;
}

function isCombatant(instance: CardInstance, cardsById: Map<string, RiftCard>) {
  const def = cardsById.get(instance.cardId);
  const type = def?.type?.toLowerCase();
  return sameZone(instance.zone, 'battlefield') && !instance.faceDown && (type === 'unit' || type === 'champion');
}

function combatMight(instance: CardInstance, cardsById: Map<string, RiftCard>, attacking: boolean) {
  const def = cardsById.get(instance.cardId);
  if (!def) return 0;
  if (hasKeyword(def, 'STUN') || hasKeyword(def, 'STUNNED')) return 0;
  const situational = attacking ? keywordValue(def, 'ASSAULT') : keywordValue(def, 'SHIELD');
  return Math.max(0, (def.power ?? 0) + (instance.mightBonus ?? 0) + (instance.temporaryPowerModifier ?? 0) + situational);
}

function lethalDamageFor(instance: CardInstance, cardsById: Map<string, RiftCard>) {
  const def = cardsById.get(instance.cardId);
  return Math.max(1, instance.currentHealth && instance.currentHealth > 0 ? instance.currentHealth : def?.health ?? 0);
}

function targetOrder(cardsById: Map<string, RiftCard>) {
  return (a: CardInstance, b: CardInstance) => {
    const aTank = hasKeyword(cardsById.get(a.cardId), 'TANK');
    const bTank = hasKeyword(cardsById.get(b.cardId), 'TANK');
    if (aTank !== bTank) return aTank ? -1 : 1;
    return a.instanceId.localeCompare(b.instanceId);
  };
}

function targetQuotas(targets: CardInstance[], cardsById: Map<string, RiftCard>, totalMight: number) {
  if (targets.length === 1) return new Map([[targets[0].instanceId, totalMight]]);
  const quotas = new Map<string, number>();
  let prefixLethal = 0;
  for (const target of targets) {
    const lethal = lethalDamageFor(target, cardsById);
    prefixLethal += lethal;
    quotas.set(target.instanceId, lethal);
    if (totalMight === prefixLethal) return quotas;
    if (totalMight < prefixLethal) return singleTargetQuota(targets, cardsById, totalMight);
  }
  const excess = totalMight - prefixLethal;
  if (excess > 0) {
    const finalTarget = targets[targets.length - 1];
    quotas.set(finalTarget.instanceId, (quotas.get(finalTarget.instanceId) ?? 0) + excess);
  }
  return quotas;
}

function singleTargetQuota(targets: CardInstance[], cardsById: Map<string, RiftCard>, totalMight: number) {
  const hasTank = targets.some((target) => hasKeyword(cardsById.get(target.cardId), 'TANK'));
  const target = targets.find((candidate) => {
    if (hasTank && !hasKeyword(cardsById.get(candidate.cardId), 'TANK')) return false;
    return lethalDamageFor(candidate, cardsById) >= totalMight;
  });
  return target ? new Map([[target.instanceId, totalMight]]) : undefined;
}

export function buildCombatDamageAssignments(
  cards: CardInstance[],
  cardsById: Map<string, RiftCard>,
  playerId: string,
  attacking: boolean,
): CombatDamageAssignment[] | null {
  const sources = cards
    .filter((instance) => instance.ownerId === playerId && isCombatant(instance, cardsById))
    .sort((a, b) => a.instanceId.localeCompare(b.instanceId));
  const targets = cards
    .filter((instance) => instance.ownerId !== playerId && isCombatant(instance, cardsById))
    .sort(targetOrder(cardsById));
  if (sources.length === 0 || targets.length === 0) return null;

  const sourceMight = new Map<string, number>();
  let totalMight = 0;
  for (const source of sources) {
    const might = combatMight(source, cardsById, attacking);
    sourceMight.set(source.instanceId, might);
    totalMight += might;
  }
  if (totalMight === 0) return [];

  const quotas = targetQuotas(targets, cardsById, totalMight);
  if (!quotas) return null;

  const assignments: CombatDamageAssignment[] = [];
  const assignedByTarget = new Map<string, number>();
  let targetIndex = 0;
  for (const source of sources) {
    let remaining = sourceMight.get(source.instanceId) ?? 0;
    while (remaining > 0 && targetIndex < targets.length) {
      const target = targets[targetIndex];
      const quota = quotas.get(target.instanceId) ?? 0;
      const assigned = assignedByTarget.get(target.instanceId) ?? 0;
      const needed = quota - assigned;
      if (needed <= 0) {
        targetIndex += 1;
        continue;
      }
      const amount = Math.min(remaining, needed);
      assignments.push({ sourceInstanceId: source.instanceId, targetInstanceId: target.instanceId, amount });
      remaining -= amount;
      assignedByTarget.set(target.instanceId, assigned + amount);
    }
  }
  return assignments;
}
