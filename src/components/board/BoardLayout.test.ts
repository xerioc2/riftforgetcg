import { describe, expect, it } from 'vitest';
import { autoPlaceInZone, clampPlacementToZone, computeLayout, runeSlotPositions, sharedBattlefieldSides } from './BoardLayout';

describe('BoardLayout shared battlefield row', () => {
  it('expands main board zones close to the available right edge', () => {
    const zones = computeLayout(1640, 780, ['player', 'opponent']);
    const playerBase = zones.find((zone) => zone.id === 'p0-base')!;
    const opponentRune = zones.find((zone) => zone.id === 'p1-rune')!;

    expect(playerBase.x).toBeLessThanOrEqual(400);
    expect(playerBase.x + playerBase.width).toBeGreaterThanOrEqual(1600);
    expect(opponentRune.width).toBe(playerBase.width);
  });

  it('gives Base rows enough height for denser card placement', () => {
    const zones = computeLayout(1366, 600, ['player', 'opponent']);
    const playerBase = zones.find((zone) => zone.id === 'p0-base')!;
    const opponentBase = zones.find((zone) => zone.id === 'p1-base')!;
    const firstCard = autoPlaceInZone(playerBase, 0, 4);

    expect(playerBase.height).toBeGreaterThanOrEqual(100);
    expect(opponentBase.height).toBe(playerBase.height);
    expect(firstCard.y).toBeGreaterThan(playerBase.y + playerBase.height * 0.4);
    expect(firstCard.y).toBeLessThan(playerBase.y + playerBase.height);
  });

  it('splits the single alpha battlefield row into opponent and player sides', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const sides = sharedBattlefieldSides(zones);

    expect(sides).not.toBeNull();
    if (!sides) throw new Error('Expected shared battlefield sides');
    expect(sides.opponentSide.ownerId).toBe('opponent');
    expect(sides.playerSide.ownerId).toBe('player');
    expect(sides.opponentSide.y).toBe(sides.playerSide.y);
    expect(sides.opponentSide.height).toBe(sides.playerSide.height);
    expect(sides.opponentSide.x + sides.opponentSide.width).toBeLessThan(sides.playerSide.x);
  });

  it('keeps Base, Rune, Legend, and Champion zones per-player around the shared battlefield', () => {
    const zones = computeLayout(1366, 600, ['player', 'opponent']);
    const opponentBase = zones.find((zone) => zone.id === 'p1-base');
    const playerBase = zones.find((zone) => zone.id === 'p0-base');
    const opponentRune = zones.find((zone) => zone.id === 'p1-rune');
    const playerRune = zones.find((zone) => zone.id === 'p0-rune');
    const sides = sharedBattlefieldSides(zones);

    if (!sides) throw new Error('Expected shared battlefield sides');
    expect(opponentBase?.ownerId).toBe('opponent');
    expect(playerBase?.ownerId).toBe('player');
    expect(opponentRune?.ownerId).toBe('opponent');
    expect(playerRune?.ownerId).toBe('player');
    expect(opponentBase!.y + opponentBase!.height).toBeLessThanOrEqual(sides.opponentSide.y);
    expect(playerBase!.y).toBeGreaterThanOrEqual(sides.playerSide.y + sides.playerSide.height);
  });

  it('keeps identity zones compact and outside the main board lanes', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const playerLegend = zones.find((zone) => zone.id === 'p0-legend')!;
    const playerChampion = zones.find((zone) => zone.id === 'p0-champion')!;
    const playerBase = zones.find((zone) => zone.id === 'p0-base')!;

    expect(playerLegend.height).toBeLessThanOrEqual(126);
    expect(playerChampion.height).toBe(playerLegend.height);
    expect(playerChampion.x + playerChampion.width).toBeLessThan(playerBase.x);
  });

  it('auto-places battlefield cards inside the correct controller side', () => {
    const zones = computeLayout(1920, 780, ['player', 'opponent']);
    const sides = sharedBattlefieldSides(zones)!;

    const opponentPosition = autoPlaceInZone(sides.opponentSide, 0, 3);
    const playerPosition = autoPlaceInZone(sides.playerSide, 0, 3);

    expect(opponentPosition.x).toBeGreaterThanOrEqual(sides.opponentSide.x);
    expect(opponentPosition.x).toBeLessThanOrEqual(sides.opponentSide.x + sides.opponentSide.width);
    expect(playerPosition.x).toBeGreaterThanOrEqual(sides.playerSide.x);
    expect(playerPosition.x).toBeLessThanOrEqual(sides.playerSide.x + sides.playerSide.width);
    expect(opponentPosition.x).toBeLessThan(playerPosition.x);
  });

  it('clamps legacy or manual battlefield positions back to their controller side', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const sides = sharedBattlefieldSides(zones)!;
    const oldFarLeftPosition = { x: sides.opponentSide.x + 20, y: sides.opponentSide.y - 40 };

    const clamped = clampPlacementToZone(oldFarLeftPosition, sides.playerSide);

    expect(clamped.x).toBeGreaterThanOrEqual(sides.playerSide.x + 12);
    expect(clamped.x).toBeLessThanOrEqual(sides.playerSide.x + sides.playerSide.width - 12);
    expect(clamped.y).toBeGreaterThanOrEqual(sides.playerSide.y + 12);
    expect(clamped.y).toBeLessThanOrEqual(sides.playerSide.y + sides.playerSide.height - 12);
  });

  it('keeps wide layouts readable by leaving a center divider gap', () => {
    const zones = computeLayout(2560, 900, ['player', 'opponent']);
    const sides = sharedBattlefieldSides(zones)!;
    const dividerGap = sides.playerSide.x - (sides.opponentSide.x + sides.opponentSide.width);

    expect(sides.opponentSide.width).toBeGreaterThan(600);
    expect(sides.playerSide.width).toBeGreaterThan(600);
    expect(dividerGap).toBeGreaterThanOrEqual(12);
    expect(dividerGap).toBeLessThanOrEqual(32);
  });

  it('keeps rune slots centered and capped inside widened rune rows', () => {
    const zones = computeLayout(2200, 820, ['player', 'opponent']);
    const runeZone = zones.find((zone) => zone.id === 'p0-rune')!;
    const positions = runeSlotPositions(runeZone, 11);
    const spacings = positions.slice(1).map((position, index) => position.x - positions[index].x);

    expect(positions[0].x).toBeGreaterThan(runeZone.x);
    expect(positions[positions.length - 1].x).toBeLessThan(runeZone.x + runeZone.width);
    expect(Math.max(...spacings)).toBeLessThanOrEqual(72);
    expect(Math.min(...spacings)).toBeGreaterThanOrEqual(30);
  });

  it('clamps legacy Base positions inside the expanded Base row', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const playerBase = zones.find((zone) => zone.id === 'p0-base')!;
    const offBoardPosition = { x: playerBase.x + playerBase.width + 200, y: playerBase.y - 200 };

    const clamped = clampPlacementToZone(offBoardPosition, playerBase);

    expect(clamped.x).toBeLessThanOrEqual(playerBase.x + playerBase.width - 12);
    expect(clamped.y).toBeGreaterThanOrEqual(playerBase.y + 12);
  });
});
