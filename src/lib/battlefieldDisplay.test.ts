import { describe, expect, it } from 'vitest';
import { battlefieldDisplayForPlayer, battlefieldDisplayFrame, selectedBattlefieldIsInteractiveTarget } from './battlefieldDisplay';
import type { RiftCard } from '../types';
import type { ZoneRect } from '../components/board/BoardLayout';

const sunkenTemple: RiftCard = {
  id: 'sunken-temple',
  name: 'Sunken Temple',
  type: 'Battlefield',
  domains: [],
  imageUrl: 'https://example.test/sunken-temple.png',
};

describe('battlefieldDisplay', () => {
  it('builds a display item for a selected battlefield', () => {
    const display = battlefieldDisplayForPlayer(
      { userId: 'p1', name: 'Player', score: 0, selectedBattlefieldId: 'sunken-temple' },
      new Map([[sunkenTemple.id, sunkenTemple]]),
    );

    expect(display).toEqual({
      playerId: 'p1',
      cardId: 'sunken-temple',
      card: sunkenTemple,
      label: 'Sunken Temple',
      imageUrl: 'https://example.test/sunken-temple.png',
    });
  });

  it('falls back to the card id when card data or image is missing', () => {
    const display = battlefieldDisplayForPlayer(
      { userId: 'p1', name: 'Player', score: 0, selectedBattlefieldId: 'unknown-battlefield' },
      new Map(),
    );

    expect(display?.label).toBe('unknown-battlefield');
    expect(display?.imageUrl).toBeUndefined();
  });

  it('returns null when no battlefield has been selected', () => {
    expect(battlefieldDisplayForPlayer({ userId: 'p1', name: 'Player', score: 0 }, new Map())).toBeNull();
  });

  it('keeps the selected battlefield display inside the zone', () => {
    const zone: ZoneRect = {
      id: 'p0-battlefield',
      label: 'Battlefield',
      ownerId: 'p1',
      zoneName: 'battlefield',
      x: 240,
      y: 300,
      width: 900,
      height: 210,
    };

    const frame = battlefieldDisplayFrame(zone);

    expect(frame.x).toBeGreaterThanOrEqual(zone.x);
    expect(frame.y).toBeGreaterThanOrEqual(zone.y);
    expect(frame.x + frame.width).toBeLessThanOrEqual(zone.x + zone.width);
    expect(frame.y + frame.height).toBeLessThanOrEqual(zone.y + zone.height);
  });

  it('marks selected battlefield visuals as non-targetable and non-interactive', () => {
    expect(selectedBattlefieldIsInteractiveTarget()).toBe(false);
  });
});
