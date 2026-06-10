import type { Deck, DeckValidation, RiftCard } from '../types';
import { getDeckLegendCardId } from './deckUtils';

const MAX_COPIES = 3;

export function validateDeck(deck: Deck, cardsById: Map<string, RiftCard>): DeckValidation {
  const legend = getDeckLegendCardId(deck) ? cardsById.get(getDeckLegendCardId(deck) ?? '') : undefined;
  const domains = legend?.domains ?? [];
  const messages: string[] = [];
  const entries = deck.cards
    .map((entry) => ({ ...entry, card: cardsById.get(entry.cardId) }))
    .filter((entry): entry is typeof entry & { card: RiftCard } => Boolean(entry.card));
  const mainDeckCards = entries
    .filter(({ card }) => card.type !== 'Rune' && card.type !== 'Battlefield' && card.type !== 'Legend' && card.type !== 'Champion')
    .reduce((sum, card) => sum + card.quantity, 0);
  const championCards = entries.filter(({ card }) => card.type === 'Champion').reduce((sum, card) => sum + card.quantity, 0);
  const totalCards = mainDeckCards + championCards;
  const runeCards = entries.filter(({ card }) => card.type === 'Rune').reduce((sum, card) => sum + card.quantity, 0);
  const battlefieldEntries = entries.filter(({ card }) => card.type === 'Battlefield');
  const battlefieldCards = battlefieldEntries.reduce((sum, card) => sum + card.quantity, 0);

  if (!deck.name.trim()) messages.push('Deck needs a name.');
  if (!legend) messages.push('Choose a Legend to set the deck domains.');
  if (championCards !== 1) messages.push(`Choose exactly 1 Champion (${championCards}/1).`);
  if (mainDeckCards !== 39) messages.push(`Main Deck must contain exactly 39 non-Champion cards (${mainDeckCards}/39).`);
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
      messages.push(`${card.name} is outside ${legend?.name ?? 'the Legend'} domains.`);
    }
  }

  return {
    valid: messages.length === 0,
    messages,
    legend,
    champion: entries.find(({ card }) => card.type === 'Champion')?.card,
    domains,
    totalCards,
    mainDeckCards,
    championCards,
    runeCards,
    battlefieldCards,
  };
}
