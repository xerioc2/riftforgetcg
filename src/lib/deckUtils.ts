import type { Deck } from '../types';

export function getDeckLegendCardId(deck: Deck): string | undefined {
  if (deck.legendCardId) return deck.legendCardId;
  const championIdIsInDeck = deck.championCardId
    ? deck.cards.some((entry) => entry.cardId === deck.championCardId)
    : false;
  return championIdIsInDeck ? undefined : deck.championCardId;
}

export function deckToGameCardIds(deck: Deck): string[] {
  const ids = deck.cards.flatMap((entry) =>
    Array.from({ length: entry.quantity }, () => entry.cardId)
  );
  if (deck.championCardId && !ids.includes(deck.championCardId)) ids.unshift(deck.championCardId);
  const legendCardId = getDeckLegendCardId(deck);
  if (legendCardId) ids.unshift(legendCardId);
  return ids;
}
