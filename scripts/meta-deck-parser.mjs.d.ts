export type ParsedDeckLine =
  | null
  | {
      malformed: true;
      lineNumber: number;
      raw: string;
      reason: string;
    }
  | {
      malformed: false;
      lineNumber: number;
      raw: string;
      quantity: number;
      sourceName: string;
      sourceCardId: string;
      sourceSetCode: string;
    };

export function normalizeName(value: unknown): string;
export function parseDeckLine(line: string, lineNumber?: number): ParsedDeckLine;
export function parseDeckText(text: string): { entries: Array<Exclude<ParsedDeckLine, null | { malformed: true }>>; malformed: Array<Extract<ParsedDeckLine, { malformed: true }>> };
export function classifyCard(card: { type?: string } | null | undefined): string;
export function normalizeDeck(rawText: string, sourceFilename: string, cards: unknown[]): {
  sourceFilename: string;
  entries: Array<{
    originalName: string;
    sourceCardId: string;
    section: string;
    resolved: { status: string; reason?: string };
    support: { status: string; reason: string; blocked: boolean };
  }>;
  supportSummary: {
    unresolvedCards: Array<{ sourceCardId: string; originalName: string; reason?: string }>;
  };
};
export function loadLocalCards(cachePath?: string): unknown[];
