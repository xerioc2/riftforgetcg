import type { RiftCard } from '../types';

export type TargetScope = 'friendly' | 'enemy' | 'any';

export function cardTargetScope(card: RiftCard | undefined): TargetScope | null {
  if (!card || !['spell', 'gear'].includes(card.type?.toLowerCase())) return null;
  const text = (card.rulesText ?? '').toLowerCase();
  if (card.type?.toLowerCase() === 'gear' && text.includes('[equip]')) return 'friendly';
  if (text.includes('friendly unit') || text.includes('unit you control')) return 'friendly';
  if (text.includes('enemy unit') || text.includes('opponent unit')) return 'enemy';
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
  return requiresTarget ? 'any' : null;
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
