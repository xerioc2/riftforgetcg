import type { Deck, RiftCard } from '../types';

export function getDeckLegendCardId(deck: Deck): string | undefined {
  if (deck.legendCardId) return deck.legendCardId;
  const championIdIsInDeck = deck.championCardId
    ? deck.cards.some((entry) => entry.cardId === deck.championCardId)
    : false;
  return championIdIsInDeck ? undefined : deck.championCardId;
}

export function deckToGameCardIds(deck: Deck, cardsById?: Map<string, RiftCard>): string[] {
  const legendCardId = getDeckLegendCardId(deck);
  const championCardId = deck.championCardId;
  const ids = deck.cards.flatMap((entry) => {
    const card = cardsById?.get(entry.cardId);
    if (card?.type === 'Legend') return [];
    if (entry.cardId === legendCardId) return [];
    return Array.from({ length: entry.quantity }, () => entry.cardId);
  });
  if (championCardId) ids.unshift(championCardId);
  if (legendCardId) ids.unshift(legendCardId);
  return ids;
}
