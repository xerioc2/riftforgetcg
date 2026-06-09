import type { ZoneName } from '../../types';

export type ZoneRect = {
  id: string;
  label: string;
  x: number;
  y: number;
  width: number;
  height: number;
  ownerId: string | null;
  zoneName: ZoneName;
};

const zone = (id: string, label: string, x: number, y: number, width: number, height: number, ownerId: string | null, zoneName: ZoneName): ZoneRect => ({
  id,
  label,
  x,
  y,
  width,
  height,
  ownerId,
  zoneName,
});

export function computeLayout(width: number, height: number, playerIds: string[]): ZoneRect[] {
  const [local, ...others] = playerIds;
  if (!local) return [];
  const zones: ZoneRect[] = [];

  if (playerIds.length <= 2) {
    const leftCol = 400;
    const rightCol = 100;
    const center = width - leftCol - rightCol - 8;
    const divider = 14;
    const half = (height - divider) / 2;
    const runeStrip = 56;
    const baseHeight = 76;
    const battlefieldHeight = half - runeStrip - baseHeight - 8;
    const opponent = others[0] ?? 'opponent';

    // Opponent (top half): Runes at very top (back), then Base, then Battlefield near center
    zones.push(zone('p1-champion', 'Champion', 200, 0, 95, half, opponent, 'champion'));
    zones.push(zone('p1-legend', 'Legend', 298, 0, 95, half, opponent, 'legend'));
    zones.push(zone('p1-rune', 'Runes', leftCol + 4, 0, center, runeStrip, opponent, 'rune'));
    zones.push(zone('p1-base', 'Base', leftCol + 4, runeStrip + 4, center, baseHeight, opponent, 'base'));
    zones.push(zone('p1-battlefield', 'Battlefield', leftCol + 4, runeStrip + baseHeight + 8, center, battlefieldHeight, opponent, 'battlefield'));
    zones.push(zone('center', '', 0, half, width, divider, null, 'limbo'));

    // Local player (bottom half): Battlefield near center, then Base, then Runes at very bottom (back)
    const localY = half + divider;
    zones.push(zone('p0-champion', 'Champion', 200, localY, 95, half, local, 'champion'));
    zones.push(zone('p0-legend', 'Legend', 298, localY, 95, half, local, 'legend'));
    zones.push(zone('p0-battlefield', 'Battlefield', leftCol + 4, localY, center, battlefieldHeight, local, 'battlefield'));
    zones.push(zone('p0-base', 'Base', leftCol + 4, localY + battlefieldHeight + 4, center, baseHeight, local, 'base'));
    zones.push(zone('p0-rune', 'Runes', leftCol + 4, localY + battlefieldHeight + baseHeight + 8, center, runeStrip, local, 'rune'));
    return zones;
  }

  const bottomH = Math.max(170, height * 0.25);
  const sideW = Math.max(150, width * 0.2);
  const topH = Math.max(150, height * 0.22);
  const top = others[0] ?? local;
  const left = others[1] ?? top;
  const right = others[2] ?? top;

  zones.push(zone('top-hand', 'Top hand', sideW, 0, width - sideW * 2, 80, top, 'hand'));
  zones.push(zone('top-battlefield', 'Top battlefield', sideW, 88, width - sideW * 2, topH - 96, top, 'battlefield'));
  zones.push(zone('left-hand', 'Left hand', 0, topH, sideW, 80, left, 'hand'));
  zones.push(zone('left-battlefield', 'Left battlefield', 0, topH + 88, sideW, height - topH - bottomH - 96, left, 'battlefield'));
  zones.push(zone('right-hand', 'Right hand', width - sideW, topH, sideW, 80, right, 'hand'));
  zones.push(zone('right-battlefield', 'Right battlefield', width - sideW, topH + 88, sideW, height - topH - bottomH - 96, right, 'battlefield'));
  zones.push(zone('shared-center', 'Shared battlefield', sideW + 16, topH + 16, width - sideW * 2 - 32, height - topH - bottomH - 32, null, 'battlefield'));
  zones.push(zone('p0-battlefield', 'Your battlefield', 96, height - bottomH, width - 208, bottomH - 108, local, 'battlefield'));
  zones.push(zone('p0-rune', 'Your runes', 0, height - bottomH, 80, bottomH - 108, local, 'rune'));
  zones.push(zone('p0-discard', 'Your discard', width - 96, height - bottomH, 80, 120, local, 'discard'));
  zones.push(zone('p0-hand', 'Your hand', 0, height - 100, width, 100, local, 'hand'));
  return zones;
}
