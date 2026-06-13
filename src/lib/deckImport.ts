import type { Deck, DeckCard, RiftCard } from '../types';
import { getDeckLegendCardId } from './deckUtils';

export type DeckImportResult = {
  legendCardId?: string;
  championCardId?: string;
  cards: DeckCard[];
  matchedLines: number;
  unmatched: string[];
  skippedSideboard: number;
};

const normalizeName = (value: string) =>
  value
    .trim()
    .replace(/[\u2018\u2019]/g, "'")
    .replace(/\s*[,/\u2013\u2014]\s*/g, ' - ')
    .replace(/\s+/g, ' ')
    .toLowerCase();

const choosePrinting = (cards: RiftCard[], type?: RiftCard['type']) => {
  const candidates = type ? cards.filter((card) => card.type === type) : cards;
  return candidates.find((card) => !/\((alternate art|metal|overnumbered|signature)\)/i.test(card.name)) ?? candidates[0];
};

export function importDeckText(text: string, catalog: RiftCard[]): DeckImportResult {
  const cardsByName = new Map<string, RiftCard[]>();
  for (const card of catalog) {
    const name = normalizeName(card.name);
    cardsByName.set(name, [...(cardsByName.get(name) ?? []), card]);
  }

  let legendCardId: string | undefined;
  let championCardId: string | undefined;
  let matchedLines = 0;
  let skippedSideboard = 0;
  let section = '';
  const quantities = new Map<string, number>();
  const unmatched: string[] = [];

  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#') || line.startsWith('//')) continue;

    const sectionMatch = line.match(
      /^(legend|champions?|main\s*deck|maindeck|deck|cards?|units?|spells?|gear|rune\s*pool|runes?|battlefields?|sideboard)\s*:?\s*$/i,
    );
    if (sectionMatch) {
      section = sectionMatch[1].replace(/\s+/g, '').toLowerCase();
      continue;
    }

    const leaderMatch = line.match(/^(legend|champion)\s*:\s*(.+)$/i);
    if (leaderMatch) {
      const targetType = /^champion$/i.test(leaderMatch[1]) ? 'Champion' : 'Legend';
      const card = choosePrinting(cardsByName.get(normalizeName(leaderMatch[2])) ?? [], targetType);
      if (!card) {
        unmatched.push(line);
        continue;
      }
      if (targetType === 'Legend') legendCardId = card.id;
      if (targetType === 'Champion') championCardId = card.id;
      matchedLines++;
      continue;
    }

    const cardMatch = line.match(/^(\d+)\s*x?\s+(.+)$/i);
    if (!cardMatch) {
      unmatched.push(line);
      continue;
    }

    const quantity = Number(cardMatch[1]);
    const name = cardMatch[2].trim();
    if (quantity < 1) {
      unmatched.push(line);
      continue;
    }
    if (section === 'sideboard') {
      skippedSideboard += quantity;
      continue;
    }

    const printings = cardsByName.get(normalizeName(name)) ?? [];
    const legend = choosePrinting(printings, 'Legend');
    if (section === 'legend' && legend) {
      legendCardId = legend.id;
      matchedLines++;
      continue;
    }

    const targetType = section === 'champion' || section === 'champions' ? 'Champion' : undefined;
    const card = choosePrinting(printings, targetType);
    if (!card) {
      unmatched.push(line);
      continue;
    }

    if (targetType === 'Champion') {
      championCardId = card.id;
      matchedLines++;
      continue;
    }
    quantities.set(card.id, (quantities.get(card.id) ?? 0) + quantity);
    matchedLines++;
  }

  return {
    legendCardId,
    championCardId,
    cards: [...quantities].map(([cardId, quantity]) => ({ cardId, quantity })),
    matchedLines,
    unmatched,
    skippedSideboard,
  };
}

export function exportDeckText(deck: Deck, cardsById: Map<string, RiftCard>): string {
  const legend = getDeckLegendCardId(deck) ? cardsById.get(getDeckLegendCardId(deck) ?? '') : undefined;
  const entries = deck.cards
    .map((entry) => ({ ...entry, card: cardsById.get(entry.cardId) }))
    .filter((entry): entry is typeof entry & { card: RiftCard } => Boolean(entry.card));
  const champion = deck.championCardId ? cardsById.get(deck.championCardId) : undefined;
  const main = entries
    .filter(({ card }) => card.type !== 'Legend' && card.type !== 'Rune' && card.type !== 'Battlefield')
    .sort(compareDeckEntries);
  const runes = entries.filter(({ card }) => card.type === 'Rune').sort(compareDeckEntries);
  const battlefields = entries.filter(({ card }) => card.type === 'Battlefield').sort(compareDeckEntries);

  const lines: string[] = [];
  lines.push('Legend:');
  if (legend) lines.push(`1 ${legend.name}`);
  lines.push('', 'Champion:');
  if (champion) lines.push(`1 ${champion.name}`);
  lines.push('', 'MainDeck:');
  main.forEach((entry) => lines.push(`${entry.quantity} ${entry.card.name}`));
  lines.push('', 'Battlefields:');
  battlefields.forEach((entry) => lines.push(`${entry.quantity} ${entry.card.name}`));
  lines.push('', 'Rune Pool:');
  runes.forEach((entry) => lines.push(`${entry.quantity} ${entry.card.name}`));
  return lines.join('\n');
}

function compareDeckEntries(a: DeckCard & { card: RiftCard }, b: DeckCard & { card: RiftCard }) {
  return (a.card.cost ?? 99) - (b.card.cost ?? 99) || a.card.name.localeCompare(b.card.name);
}
