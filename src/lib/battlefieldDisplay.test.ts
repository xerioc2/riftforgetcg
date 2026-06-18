import { describe, expect, it } from 'vitest';
import { battlefieldDisplayForPlayer, battlefieldDisplayFrame, selectedBattlefieldIsInteractiveTarget, selectedBattlefieldSupportsPreview } from './battlefieldDisplay';
import type { RiftCard } from '../types';
import { computeLayout, sharedBattlefieldSides, type ZoneRect } from '../components/board/BoardLayout';

const sunkenTemple: RiftCard = {
  id: 'sunken-temple',
  name: 'Sunken Temple',
  type: 'Battlefield',
  domains: [],
  imageUrl: 'https://example.test/sunken-temple.png',
  rulesText: 'When you conquer here with one or more [Mighty] units, you may pay 1 to draw 1.',
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

  it('separates opponent and player battlefield plaques on opposite sides', () => {
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

    const opponentFrame = battlefieldDisplayFrame(zone, 'left');
    const playerFrame = battlefieldDisplayFrame(zone, 'right');

    expect(opponentFrame.x).toBeGreaterThanOrEqual(zone.x);
    expect(playerFrame.x + playerFrame.width).toBeLessThanOrEqual(zone.x + zone.width);
    expect(opponentFrame.x + opponentFrame.width).toBeLessThan(playerFrame.x);
    expect(opponentFrame.y).toBe(playerFrame.y);
  });

  it('keeps plaques compact so the center lane remains available', () => {
    const zone: ZoneRect = {
      id: 'wide-battlefield',
      label: 'Battlefield',
      ownerId: 'p1',
      zoneName: 'battlefield',
      x: 400,
      y: 160,
      width: 1200,
      height: 240,
    };

    const frame = battlefieldDisplayFrame(zone, 'left');

    expect(frame.width).toBeLessThanOrEqual(180);
    expect(frame.width).toBeLessThan(zone.width * 0.2);
  });

  it('positions selected battlefield plaques near the shared divider on split battlefield sides', () => {
    const sides = sharedBattlefieldSides(computeLayout(1600, 720, ['player', 'opponent']))!;
    const opponentFrame = battlefieldDisplayFrame(sides.opponentSide, 'right');
    const playerFrame = battlefieldDisplayFrame(sides.playerSide, 'left');
    const dividerCenter = (sides.opponentSide.x + sides.opponentSide.width + sides.playerSide.x) / 2;

    expect(opponentFrame.x + opponentFrame.width).toBeLessThanOrEqual(dividerCenter);
    expect(playerFrame.x).toBeGreaterThanOrEqual(dividerCenter);
    expect(playerFrame.x - (opponentFrame.x + opponentFrame.width)).toBeLessThan(80);
  });

  it('marks selected battlefield visuals as non-targetable and non-interactive', () => {
    expect(selectedBattlefieldIsInteractiveTarget()).toBe(false);
  });

  it('keeps selected battlefield card text available for hover preview', () => {
    const display = battlefieldDisplayForPlayer(
      { userId: 'p1', name: 'Player', score: 0, selectedBattlefieldId: 'sunken-temple' },
      new Map([[sunkenTemple.id, sunkenTemple]]),
    );

    expect(selectedBattlefieldSupportsPreview()).toBe(true);
    expect(display?.card?.rulesText).toContain('Mighty');
  });
});
