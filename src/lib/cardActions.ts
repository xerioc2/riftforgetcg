import type { RiftCard } from '../types';

export type TargetScope = 'friendly' | 'enemy' | 'any';
export type TargetMode = 'NONE' | 'FRIENDLY_UNIT' | 'ENEMY_UNIT' | 'ANY_BATTLEFIELD_UNIT' | 'FRIENDLY_UNIT_FOR_EQUIP' | 'UNSUPPORTED';

export function cardTargetScope(card: RiftCard | undefined): TargetScope | null {
  const mode = targetModeForCard(card);
  if (mode === 'FRIENDLY_UNIT' || mode === 'FRIENDLY_UNIT_FOR_EQUIP') return 'friendly';
  if (mode === 'ENEMY_UNIT') return 'enemy';
  if (mode === 'ANY_BATTLEFIELD_UNIT') return 'any';
  if (mode === 'UNSUPPORTED') return 'any';
  return null;
}

export function targetModeForCard(card: RiftCard | undefined): TargetMode {
  if (!card || !['spell', 'gear'].includes(card.type?.toLowerCase())) return 'NONE';
  const text = (card.rulesText ?? '').toLowerCase();
  if (card.type?.toLowerCase() === 'gear') return text.includes('[equip]') ? 'FRIENDLY_UNIT_FOR_EQUIP' : 'UNSUPPORTED';
  if (text.includes('another unit') || text.includes('a friendly unit and an enemy unit')) return 'UNSUPPORTED';
  if (text.includes('counter a spell')
    || text.includes('counter an enemy spell')
    || text.includes('choose a spell')
    || text.includes('target spell')
    || text.includes('target ability')) {
    return 'UNSUPPORTED';
  }
  if (text.includes('friendly unit') || text.includes('unit you control')) return 'FRIENDLY_UNIT';
  if (text.includes('enemy unit') || text.includes('opponent unit')) return 'ENEMY_UNIT';
  const requiresTarget = text.includes('target unit')
    || text.includes('target champion')
    || text.includes('target creature')
    || text.includes('target card')
    || text.includes('counter target')
    || text.includes('destroy target')
    || text.includes('give a unit')
    || text.includes('return a unit')
    || text.includes('move a unit')
    || (text.includes('deal') && text.includes('damage to target'));
  return requiresTarget ? 'ANY_BATTLEFIELD_UNIT' : 'NONE';
}

export function targetPromptForMode(mode: TargetMode) {
  switch (mode) {
    case 'FRIENDLY_UNIT':
      return 'Choose a friendly unit.';
    case 'ENEMY_UNIT':
      return 'Choose an enemy unit.';
    case 'ANY_BATTLEFIELD_UNIT':
      return 'Choose a unit at a battlefield.';
    case 'FRIENDLY_UNIT_FOR_EQUIP':
      return 'Choose a unit to equip.';
    case 'UNSUPPORTED':
      return 'This targeting pattern is not supported yet.';
    case 'NONE':
    default:
      return '';
  }
}

export function isLegalTargetForMode(card: { ownerId: string; zone: string } | undefined, cardDef: RiftCard | undefined, mode: TargetMode, playerId: string) {
  if (!card || !cardDef) return false;
  const type = cardDef.type?.toLowerCase();
  const isUnitLike = type === 'unit' || type === 'champion';
  if (!isUnitLike || card.zone.toLowerCase() !== 'battlefield') return false;
  if (mode === 'FRIENDLY_UNIT' || mode === 'FRIENDLY_UNIT_FOR_EQUIP') return card.ownerId === playerId;
  if (mode === 'ENEMY_UNIT') return card.ownerId !== playerId;
  if (mode === 'ANY_BATTLEFIELD_UNIT') return true;
  return false;
}

export function unsupportedCardReason(card: RiftCard | undefined): string | null {
  if (!card) return null;
  const type = card.type?.toLowerCase();
  const text = (card.rulesText ?? '').toLowerCase();
  if (type === 'gear') return text.includes('[equip]') ? null : 'That gear ability is not supported yet.';
  if (type !== 'spell') return null;
  const supported = text.includes(':rb_might:') || text.includes('return a unit') || text.includes('ready it') || text.includes('draw 1');
  if (text.includes('counter a spell') || text.includes('counter an enemy spell')) return 'Counter spells need the future reaction stack.';
  if (text.includes('another unit') || text.includes('a friendly unit and an enemy unit')) return 'Multi-target spells are not supported yet.';
  return supported ? null : 'That spell effect is not supported yet.';
}
