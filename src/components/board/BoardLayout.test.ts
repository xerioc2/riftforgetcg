import { describe, expect, it } from 'vitest';
import {
  autoPlaceInZone,
  battlefieldLocationForPoint,
  battlefieldLocationLabel,
  battlefieldZoneForCard,
  BATTLEFIELD_LOCATIONS,
  clampPlacementToZone,
  computeLayout,
  normalizeBattlefieldLocationId,
  runeSlotPositions,
} from './BoardLayout';

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

  it('splits the shared battlefield row into three visible locations with player sides', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const battlefieldZones = zones.filter((zone) => zone.zoneName === 'battlefield');

    expect(battlefieldZones).toHaveLength(6);
    for (const locationId of BATTLEFIELD_LOCATIONS) {
      const laneZones = battlefieldZones.filter((zone) => zone.battlefieldLocationId === locationId);
      expect(laneZones.map((zone) => zone.ownerId).sort()).toEqual(['opponent', 'player']);
      expect(laneZones[0].x).toBe(laneZones[1].x);
      expect(laneZones[0].width).toBe(laneZones[1].width);
    }
  });

  it('keeps Base, Rune, Legend, and Champion zones per-player around the shared battlefield', () => {
    const zones = computeLayout(1366, 600, ['player', 'opponent']);
    const opponentBase = zones.find((zone) => zone.id === 'p1-base');
    const playerBase = zones.find((zone) => zone.id === 'p0-base');
    const opponentRune = zones.find((zone) => zone.id === 'p1-rune');
    const playerRune = zones.find((zone) => zone.id === 'p0-rune');
    const battlefieldZones = zones.filter((zone) => zone.zoneName === 'battlefield');
    const firstOpponentLane = battlefieldZones.find((zone) => zone.ownerId === 'opponent' && zone.battlefieldLocationId === 'bf-0')!;
    const lastPlayerLane = battlefieldZones.find((zone) => zone.ownerId === 'player' && zone.battlefieldLocationId === 'bf-2')!;

    expect(opponentBase?.ownerId).toBe('opponent');
    expect(playerBase?.ownerId).toBe('player');
    expect(opponentRune?.ownerId).toBe('opponent');
    expect(playerRune?.ownerId).toBe('player');
    expect(opponentBase!.y + opponentBase!.height).toBeLessThanOrEqual(firstOpponentLane.y);
    expect(playerBase!.y).toBeGreaterThanOrEqual(lastPlayerLane.y + lastPlayerLane.height);
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

  it('auto-places battlefield cards inside the correct location and controller side', () => {
    const zones = computeLayout(1920, 780, ['player', 'opponent']);
    const opponentLane = battlefieldZoneForCard(zones, 'opponent', 'bf-1')!;
    const playerLane = battlefieldZoneForCard(zones, 'player', 'bf-1')!;

    const opponentPosition = autoPlaceInZone(opponentLane, 0, 3);
    const playerPosition = autoPlaceInZone(playerLane, 0, 3);

    expect(opponentPosition.x).toBeGreaterThanOrEqual(opponentLane.x);
    expect(opponentPosition.x).toBeLessThanOrEqual(opponentLane.x + opponentLane.width);
    expect(playerPosition.x).toBeGreaterThanOrEqual(playerLane.x);
    expect(playerPosition.x).toBeLessThanOrEqual(playerLane.x + playerLane.width);
    expect(opponentPosition.y).toBeLessThan(playerPosition.y);
  });

  it('clamps legacy or manual battlefield positions back to their controller location', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const playerLane = battlefieldZoneForCard(zones, 'player', 'bf-2')!;
    const oldFarLeftPosition = { x: playerLane.x - 200, y: playerLane.y - 40 };

    const clamped = clampPlacementToZone(oldFarLeftPosition, playerLane);

    expect(clamped.x).toBeGreaterThanOrEqual(playerLane.x + 12);
    expect(clamped.x).toBeLessThanOrEqual(playerLane.x + playerLane.width - 12);
    expect(clamped.y).toBeGreaterThanOrEqual(playerLane.y + 12);
    expect(clamped.y).toBeLessThanOrEqual(playerLane.y + playerLane.height - 12);
  });

  it('keeps wide layouts readable by giving all three locations room', () => {
    const zones = computeLayout(2560, 900, ['player', 'opponent']);
    const lanes = BATTLEFIELD_LOCATIONS.map((locationId) => battlefieldZoneForCard(zones, 'player', locationId)!);

    expect(lanes).toHaveLength(3);
    lanes.forEach((lane) => expect(lane.width).toBeGreaterThan(450));
    expect(lanes[0].x + lanes[0].width).toBeLessThan(lanes[1].x);
    expect(lanes[1].x + lanes[1].width).toBeLessThan(lanes[2].x);
  });

  it('maps pointer drops and missing card locations to stable battlefield ids', () => {
    const zones = computeLayout(1600, 720, ['player', 'opponent']);
    const lane = battlefieldZoneForCard(zones, 'player', 'bf-1')!;

    expect(battlefieldLocationForPoint(lane.x + lane.width / 2, lane.y + lane.height / 2, zones)).toBe('bf-1');
    expect(normalizeBattlefieldLocationId(null)).toBe('bf-0');
    expect(normalizeBattlefieldLocationId('unknown')).toBe('bf-0');
    expect(battlefieldLocationLabel('bf-2')).toBe('Battlefield 3');
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
