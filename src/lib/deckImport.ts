import type { DeckCard, RiftCard } from '../types';

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
    .replace(/[’‘]/g, "'")
    .replace(/\s*[,–—-]\s*/g, ' - ')
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
      /^(legend|champions?|main\s*deck|deck|cards?|units?|spells?|gear|rune\s*pool|runes?|battlefields?|sideboard)\s*:?\s*$/i,
    );
    if (sectionMatch) {
      section = sectionMatch[1].replace(/\s+/g, '').toLowerCase();
      continue;
    }

    const legacyLeaderMatch = line.match(/^(?:legend|champion)\s*:\s*(.+)$/i);
    if (legacyLeaderMatch) {
      const legend = choosePrinting(cardsByName.get(normalizeName(legacyLeaderMatch[1])) ?? [], 'Legend');
      if (legend) {
        legendCardId = legend.id;
        matchedLines++;
      } else {
        unmatched.push(line);
      }
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

    const card = choosePrinting(printings, section === 'champion' ? 'Champion' : undefined);
    if (!card) {
      unmatched.push(line);
      continue;
    }

    if (card.type === 'Champion') championCardId = card.id;
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
