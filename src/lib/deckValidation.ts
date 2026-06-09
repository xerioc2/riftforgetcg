import type { Deck, DeckValidation, RiftCard } from '../types';
import { getDeckLegendCardId } from './deckUtils';

const MAX_COPIES = 3;

export function validateDeck(deck: Deck, cardsById: Map<string, RiftCard>): DeckValidation {
  const champion = getDeckLegendCardId(deck) ? cardsById.get(getDeckLegendCardId(deck) ?? '') : undefined;
  const domains = champion?.domains ?? [];
  const messages: string[] = [];
  const entries = deck.cards
    .map((entry) => ({ ...entry, card: cardsById.get(entry.cardId) }))
    .filter((entry): entry is typeof entry & { card: RiftCard } => Boolean(entry.card));
  const totalCards = entries
    .filter(({ card }) => card.type !== 'Rune' && card.type !== 'Battlefield' && card.type !== 'Legend')
    .reduce((sum, card) => sum + card.quantity, 0);
  const runeCards = entries.filter(({ card }) => card.type === 'Rune').reduce((sum, card) => sum + card.quantity, 0);
  const battlefieldEntries = entries.filter(({ card }) => card.type === 'Battlefield');
  const battlefieldCards = battlefieldEntries.reduce((sum, card) => sum + card.quantity, 0);

  if (!deck.name.trim()) messages.push('Deck needs a name.');
  if (!champion) messages.push('Choose a legend to set the deck domains.');
  if (totalCards !== 40) messages.push(`Main Deck must contain exactly 40 cards (${totalCards}/40).`);
  if (runeCards !== 12) messages.push(`Rune Pool must contain exactly 12 runes (${runeCards}/12).`);
  if (battlefieldCards !== 3) messages.push(`Choose exactly 3 Battlefields (${battlefieldCards}/3).`);
  if (new Set(battlefieldEntries.map(({ card }) => card.name)).size !== battlefieldCards) {
    messages.push('Battlefields must have unique names.');
  }

  for (const entry of entries) {
    const { card } = entry;
    if (entry.quantity > MAX_COPIES && card.type !== 'Rune') {
      messages.push(`${card.name} exceeds ${MAX_COPIES} copies.`);
    }
    const relevantDomains = card.domains.filter((domain) => domain !== 'COLORLESS');
    const offDomain = relevantDomains.some((domain) => domains.length > 0 && !domains.includes(domain));
    if (offDomain && card.type !== 'Legend' && card.type !== 'Battlefield') {
      messages.push(`${card.name} is outside ${champion?.name ?? 'the legend'} domains.`);
    }
  }

  return {
    valid: messages.length === 0,
    messages,
    champion,
    domains,
    totalCards,
    runeCards,
    battlefieldCards,
  };
}
