import { describe, expect, it } from 'vitest';
import { attachedGearDisplayPosition, attachedGearDisplayScale, attachedGearForHost, attachedGearIsIndependentBoardPiece, attachedGearNamesForHost } from './attachments';
import type { CardInstance, RiftCard } from '../types';

const host: CardInstance = {
  instanceId: 'host',
  cardId: 'host-card',
  ownerId: 'p1',
  zone: 'base',
  x: 100,
  y: 120,
  tapped: false,
  faceDown: false,
  zIndex: 0,
};

const boots: CardInstance = {
  instanceId: 'boots',
  cardId: 'boots-card',
  ownerId: 'p1',
  zone: 'base',
  x: 0,
  y: 0,
  tapped: false,
  faceDown: false,
  zIndex: 1,
  attachedToInstanceId: 'host',
};

const looseGear: CardInstance = {
  instanceId: 'loose',
  cardId: 'loose-card',
  ownerId: 'p1',
  zone: 'base',
  x: 0,
  y: 0,
  tapped: false,
  faceDown: false,
  zIndex: 2,
};

const cardsById = new Map<string, RiftCard>([
  ['boots-card', { id: 'boots-card', name: 'Boots of Swiftness', type: 'Gear', domains: [] }],
]);

describe('attachment helpers', () => {
  it('finds attached gear names for a host', () => {
    expect(attachedGearNamesForHost([host, boots, looseGear], cardsById, 'host')).toEqual(['Boots of Swiftness']);
  });

  it('falls back to Equipment when attached gear card data is unavailable', () => {
    const unknownGear = { ...boots, cardId: 'unknown-gear' };

    expect(attachedGearForHost([host, unknownGear], cardsById, 'host')).toEqual([
      { gear: unknownGear, name: 'Equipment' },
    ]);
  });

  it('positions attached gear near but offset from its host', () => {
    expect(attachedGearDisplayPosition({ x: 100, y: 120 })).toEqual({ x: 146, y: 162 });
  });

  it('keeps attached gear smaller than normal board cards', () => {
    expect(attachedGearDisplayScale(1)).toBeCloseTo(0.72);
    expect(attachedGearDisplayScale(2)).toBe(0.86);
    expect(attachedGearDisplayScale(0.5)).toBe(0.62);
  });

  it('marks attached gear as non-independent board pieces', () => {
    expect(attachedGearIsIndependentBoardPiece(boots)).toBe(false);
    expect(attachedGearIsIndependentBoardPiece(looseGear)).toBe(true);
  });
});
