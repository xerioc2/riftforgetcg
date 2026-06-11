import type { RiftCard } from '../types';

const BANNED_CONSTRUCTED_NAMES = new Set([
  'CALLED SHOT',
  'DRAVEN, VANQUISHER',
  'FIGHT OR FLIGHT',
  'SCRAPHEAP',
  'DREAMING TREE',
  'OBELISK OF POWER',
  "REAVER'S ROW",
]);

export function normalizedCardName(name: string): string {
  return name.trim().toUpperCase().replace(/[’‘]/g, "'").replace(/\s+/g, ' ');
}

export function isBannedInConstructed(card: RiftCard | undefined): boolean {
  return Boolean(card && BANNED_CONSTRUCTED_NAMES.has(normalizedCardName(card.name)));
}
