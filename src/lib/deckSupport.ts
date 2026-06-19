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
  'NOXIAN DRUMMER',
  'LOYAL PORO',
  'LONELY PORO',
  'VANGUARD CAPTAIN',
  'STELLACORN HERDER',
  'DISARMING RAKE',
]);

const PARTIAL_REASONS = new Map<string, string>([
  ['IRELIA - FERVENT', 'Partial: Deflect targeting tax is heuristic, and the choose/ready +1 Might trigger is not implemented yet.'],
  ['DISCIPLINE', 'Partial: alpha chain-window Reaction support exists for giving a public battlefield Unit/Champion +2 Might this turn and drawing 1. Full official any-time Reaction timing remains incomplete.'],
  ['TIDETURNER', 'Partial: Hidden foundation exists, but later hidden play timing and the on-play location swap are not implemented yet.'],
  ['GUARDIAN ANGEL', 'Partial: alpha Equip lifecycle and printed Calm equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred.'],
  ['BOOTS OF SWIFTNESS', 'Partial: alpha Equip lifecycle and printed Chaos equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred.'],
  ['ABANDONED HALL', 'Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.'],
  ['ADAPTATRON', 'Partial: conquer trigger, optional gear kill, and official Buff state are not implemented yet.'],
  ['SCUTTLE CRAB', 'Partial: on-play draw and 1v1 private hand reveal Deathknell are implemented, but XP and facedown-card viewing are deferred.'],
  ['EN GARDE', 'Partial: alpha chain-window Reaction support exists for giving a friendly battlefield Unit/Champion +1 Might, or +2 if it is your only unit there. Full official any-time Reaction timing remains incomplete.'],
  ['DEFIANT DANCE', 'Partial: alpha chain-window Reaction support exists for giving one public battlefield Unit/Champion +2 Might and another public battlefield Unit/Champion -2 Might this turn. Full official any-time Reaction timing remains incomplete.'],
  ['FLASH', 'Partial: alpha chain-window Reaction support exists for moving up to two friendly battlefield Unit/Champion cards to Base. Full official any-time Reaction timing remains incomplete.'],
  ['CHARM', 'Partial: alpha support moves one enemy public battlefield Unit/Champion to Base. Broader official movement choices, control/location edge cases, and non-battlefield destinations remain deferred.'],
  ['THE SYREN', 'Partial: The Syren can be played to Base and activated during your Main Phase by paying 1 energy and exhausting it to move a friendly public battlefield Unit/Champion to Base. Broader activated ability timing and ability-chain support remain deferred.'],
  ['ZHONYA\'S HOURGLASS', 'Partial: Zhonya\'s Hourglass can be played to Base and armed during your Main Phase to protect a friendly public Unit/Champion from the next supported death, destroying Zhonya instead, then healing, exhausting, and recalling that unit. Hidden Reaction-for-0 timing, competing replacement choices, and broad replacement timing remain deferred.'],
  ['DEFY', 'Partial: Defy can counter supported public pending spell chain items that cost no more than 4 energy and no more than 1 premium rune during the current alpha chain window. Full official Reaction timing, broad spell/ability targets, and countering counters remain deferred.'],
  ['NOT SO FAST', 'Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred.'],
  ['GUST', 'Partial: alpha chain-window Reaction support exists through Stacked Deck for returning a battlefield Unit/Champion with 3 Might or less, but full official any-time Reaction timing remains incomplete.'],
  ['STACKED DECK', 'Partial: opens the narrow alpha chain, then resolves into a private top-3 choice; official ordering and broader timing remain incomplete.'],
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
  const normalizedName = normalizeCardName(card.name);
  if (SUPPORTED_CARD_NAMES.has(normalizedName)) {
    return { status: 'SUPPORTED', reason: 'Implemented and covered by current support policy.' };
  }
  return { status: 'PARTIAL', reason: PARTIAL_REASONS.get(normalizedName) ?? 'Playable for alpha testing, but rules may be incomplete.' };
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

function normalizeCardName(name: string) {
  return name.trim().toUpperCase().replace('’', "'");
}
