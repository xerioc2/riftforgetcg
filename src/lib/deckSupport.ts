import type { CardSupportStatus, Deck, RiftCard } from '../types';
import { unsupportedCardReason } from './cardActions';
import { isBannedInConstructed } from './tournamentLegality';

const SUPPORTED_CARD_NAMES = new Set([
  'CALM RUNE',
  'CHAOS RUNE',
  'BODY RUNE',
  'ORDER RUNE',
  'VANGUARD SERGEANT',
  'DARING PORO',
  'LAURENT DUELIST',
]);

export type DeckSupportEntry = {
  card: RiftCard;
  status: CardSupportStatus;
  reason: string;
};

export function cardSupportStatus(card: RiftCard | undefined): { status: CardSupportStatus; reason: string } {
  if (!card) return { status: 'NOT_AUDITED', reason: 'Not available in supported-only mode yet because card data is missing.' };
  if (isBannedInConstructed(card)) return { status: 'BANNED', reason: 'Not legal in constructed.' };
  const unsupportedReason = unsupportedCardReason(card);
  if (unsupportedReason) return { status: 'UNSUPPORTED', reason: `Blocked in enforced play: ${unsupportedReason}` };
  if (SUPPORTED_CARD_NAMES.has(card.name.trim().toUpperCase().replace('’', "'"))) {
    return { status: 'SUPPORTED', reason: 'Implemented and covered by current support policy.' };
  }
  return { status: 'PARTIAL', reason: 'Playable for alpha testing, but rules may be incomplete.' };
}

export function deckSupportEntries(deck: Deck | undefined, cardsById: Map<string, RiftCard>): DeckSupportEntry[] {
  if (!deck) return [];
  const seen = new Set<string>();
  const entries: DeckSupportEntry[] = [];
  for (const entry of deck.cards) {
    if (seen.has(entry.cardId)) continue;
    seen.add(entry.cardId);
    const card = cardsById.get(entry.cardId);
    if (!card) continue;
    entries.push({ card, ...cardSupportStatus(card) });
  }
  return entries;
}

export function unsupportedDeckEntries(deck: Deck | undefined, cardsById: Map<string, RiftCard>): Array<{ card: RiftCard; reason: string }> {
  return deckSupportEntries(deck, cardsById)
    .filter((entry) => entry.status === 'UNSUPPORTED' || entry.status === 'NOT_AUDITED')
    .map(({ card, reason }) => ({ card, reason }));
}
