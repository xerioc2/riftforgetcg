import { describe, expect, it } from 'vitest';

import {
  classifyCard,
  normalizeDeck,
  parseDeckLine,
  parseDeckText,
} from '../../scripts/meta-deck-parser.mjs';

describe('uploaded meta deck parser', () => {
  it('parses quantity, exact card name, and source card id', () => {
    expect(parseDeckLine('3 Stacked Deck (OGN-183)', 1)).toMatchObject({
      malformed: false,
      quantity: 3,
      sourceName: 'Stacked Deck',
      sourceCardId: 'OGN-183',
      sourceSetCode: 'OGN',
    });
  });

  it('preserves names with apostrophes and hyphens', () => {
    expect(parseDeckLine("1 Zhonya's Hourglass (OGN-077)", 2)).toMatchObject({
      sourceName: "Zhonya's Hourglass",
      sourceCardId: 'OGN-077',
    });
    expect(parseDeckLine('1 Irelia - Blade Dancer (SFD-195)', 3)).toMatchObject({
      sourceName: 'Irelia - Blade Dancer',
      sourceCardId: 'SFD-195',
    });
  });

  it('reports malformed lines without dropping them silently', () => {
    const parsed = parseDeckText('3 Stacked Deck (OGN-183)\nnot a deck line');
    expect(parsed.entries).toHaveLength(1);
    expect(parsed.malformed).toEqual([
      expect.objectContaining({
        lineNumber: 2,
        malformed: true,
        raw: 'not a deck line',
      }),
    ]);
  });

  it('classifies sections from resolved card type data', () => {
    expect(classifyCard({ type: 'Legend' })).toBe('Legend');
    expect(classifyCard({ type: 'Champion' })).toBe('Champion / MainDeck candidate');
    expect(classifyCard({ type: 'Rune' })).toBe('Rune Deck');
    expect(classifyCard({ type: 'Battlefield' })).toBe('Battlefields');
    expect(classifyCard({ type: 'Spell' })).toBe('Main Deck');
  });

  it('keeps unresolved cards in normalized output', () => {
    const deck = normalizeDeck('2 Unknown Trick (ABC-001)', 'test.txt', []);
    expect(deck.entries).toHaveLength(1);
    expect(deck.entries[0]).toMatchObject({
      originalName: 'Unknown Trick',
      sourceCardId: 'ABC-001',
      section: 'Unresolved',
      resolved: { status: 'UNRESOLVED' },
      support: { status: 'NOT_AUDITED' },
    });
    expect(deck.supportSummary.unresolvedCards).toEqual([
      expect.objectContaining({ originalName: 'Unknown Trick', sourceCardId: 'ABC-001' }),
    ]);
  });
});
