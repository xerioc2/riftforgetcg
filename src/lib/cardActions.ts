import type { RiftCard } from '../types';

export type TargetMode = 'NONE' | 'FRIENDLY_UNIT' | 'ENEMY_UNIT' | 'ANY_BATTLEFIELD_UNIT' | 'FRIENDLY_UNIT_FOR_EQUIP' | 'UNSUPPORTED';

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
  if (text.includes('friendly unit') || text.includes('unit you control') || text.includes('ready it')) return 'FRIENDLY_UNIT';
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

export function isActionCard(card: RiftCard | undefined) {
  return bracketedTiming(card, 'action');
}

export function isReactionCard(card: RiftCard | undefined) {
  return bracketedTiming(card, 'reaction');
}

export function isAmbushCard(card: RiftCard | undefined) {
  return bracketedTiming(card, 'ambush')
    || (card?.keywords ?? []).some((keyword) => keyword.toUpperCase().startsWith('AMBUSH'));
}

export function hasUnsupportedAdditionalCost(card: RiftCard | undefined) {
  return (card?.rulesText ?? '').toLowerCase().includes('additional cost');
}

function bracketedTiming(card: RiftCard | undefined, timingWord: string) {
  return (card?.rulesText ?? '').toLowerCase().includes(`[${timingWord.toLowerCase()}]`);
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
      return 'Choose a friendly Unit or Champion in Base or at the battlefield.';
    case 'UNSUPPORTED':
      return 'This targeting pattern is not supported yet.';
    case 'NONE':
    default:
      return '';
  }
}

export function noLegalTargetsMessage(mode: TargetMode) {
  switch (mode) {
    case 'FRIENDLY_UNIT_FOR_EQUIP':
      return 'No legal equip targets. You need a friendly Unit or Champion in Base or at the battlefield.';
    case 'FRIENDLY_UNIT':
      return 'No legal friendly targets.';
    case 'ENEMY_UNIT':
      return 'No legal enemy targets.';
    case 'ANY_BATTLEFIELD_UNIT':
      return 'No legal battlefield targets.';
    case 'UNSUPPORTED':
      return 'That targeting pattern is not supported yet.';
    case 'NONE':
    default:
      return 'No legal targets.';
  }
}

export function isLegalTargetForMode(card: { ownerId: string; zone: string; faceDown?: boolean } | undefined, cardDef: RiftCard | undefined, mode: TargetMode, playerId: string) {
  if (!card || !cardDef) return false;
  const type = cardDef.type?.toLowerCase();
  const isUnitLike = type === 'unit' || type === 'champion';
  if (!isUnitLike || card.faceDown) return false;
  const zone = card.zone.toLowerCase();
  if (mode === 'FRIENDLY_UNIT_FOR_EQUIP') {
    return card.ownerId === playerId && (zone === 'base' || zone === 'battlefield');
  }
  if (zone !== 'battlefield') return false;
  if (mode === 'FRIENDLY_UNIT') return card.ownerId === playerId;
  if (mode === 'ENEMY_UNIT') return card.ownerId !== playerId;
  if (mode === 'ANY_BATTLEFIELD_UNIT') return true;
  return false;
}

export function unsupportedCardReason(card: RiftCard | undefined): string | null {
  if (!card) return null;
  const type = card.type?.toLowerCase();
  const text = (card.rulesText ?? '').toLowerCase();
  if (hasUnsupportedAdditionalCost(card)) return 'Additional-cost cards are not supported yet.';
  if (type === 'gear') return text.includes('[equip]') ? null : 'That gear ability is not supported yet.';
  if (type !== 'spell') return null;
  const supported = text.includes(':rb_might:') || text.includes('return a unit') || text.includes('ready it') || text.includes('draw 1');
  if (text.includes('counter a spell') || text.includes('counter an enemy spell')) return 'Counter spells need the future reaction stack.';
  if (text.includes('another unit') || text.includes('a friendly unit and an enemy unit')) return 'Multi-target spells are not supported yet.';
  return supported ? null : 'That spell effect is not supported yet.';
}
