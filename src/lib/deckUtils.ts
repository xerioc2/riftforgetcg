import type { Deck } from '../types';

export function getDeckLegendCardId(deck: Deck): string | undefined {
  return deck.legendCardId ?? deck.championCardId;
}

export function deckToGameCardIds(deck: Deck): string[] {
  const ids = deck.cards.flatMap((entry) =>
    Array.from({ length: entry.quantity }, () => entry.cardId)
  );
  const legendCardId = getDeckLegendCardId(deck);
  if (legendCardId) ids.unshift(legendCardId);
  return ids;
}
