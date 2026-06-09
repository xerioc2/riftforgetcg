import type { Deck } from '../types';

export function deckToGameCardIds(deck: Deck): string[] {
  const ids = deck.cards.flatMap((entry) =>
    Array.from({ length: entry.quantity }, () => entry.cardId)
  );
  if (deck.championCardId) ids.unshift(deck.championCardId);
  return ids;
}
