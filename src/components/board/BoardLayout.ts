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
    const leftCol = 200;
    const rightCol = 100;
    const center = width - leftCol - rightCol - 8;
    const divider = 14;
    const half = (height - divider) / 2;
    const runeStrip = 56;
    const baseHeight = 76;
    const battlefieldHeight = half - runeStrip - baseHeight - 8;
    const opponent = others[0] ?? 'opponent';

    // Opponent (top half): Base at very top closest to them, Battlefield in middle, Runes near center
    zones.push(zone('p1-champion', 'Champion', 0, 0, 95, half, opponent, 'champion'));
    zones.push(zone('p1-legend', 'Legend', 97, 0, 95, half, opponent, 'legend'));
    zones.push(zone('p1-base', 'Base', leftCol + 4, 0, center, baseHeight, opponent, 'base'));
    zones.push(zone('p1-battlefield', 'Battlefield', leftCol + 4, baseHeight + 4, center, battlefieldHeight, opponent, 'battlefield'));
    zones.push(zone('p1-rune-deck', 'Rune Deck', leftCol + 4, baseHeight + battlefieldHeight + 8, 80, runeStrip, opponent, 'rune-deck'));
    zones.push(zone('p1-rune', 'Runes', leftCol + 88, baseHeight + battlefieldHeight + 8, center - 88, runeStrip, opponent, 'rune'));
    zones.push(zone('p1-deck', 'Deck', width - rightCol, 0, 95, 120, opponent, 'deck'));
    zones.push(zone('p1-discard', 'Trash', width - rightCol, 124, 95, 80, opponent, 'discard'));
    zones.push(zone('center', '', 0, half, width, divider, null, 'limbo'));

    // Local player (bottom half): Runes near center, Battlefield in middle, Base at very bottom closest to player
    const localY = half + divider;
    zones.push(zone('p0-champion', 'Champion', 0, localY, 95, half, local, 'champion'));
    zones.push(zone('p0-legend', 'Legend', 97, localY, 95, half, local, 'legend'));
    zones.push(zone('p0-rune-deck', 'Rune Deck', leftCol + 4, localY, 80, runeStrip, local, 'rune-deck'));
    zones.push(zone('p0-rune', 'Runes', leftCol + 88, localY, center - 88, runeStrip, local, 'rune'));
    zones.push(zone('p0-battlefield', 'Battlefield', leftCol + 4, localY + runeStrip + 4, center, battlefieldHeight, local, 'battlefield'));
    zones.push(zone('p0-base', 'Base', leftCol + 4, localY + runeStrip + battlefieldHeight + 8, center, baseHeight, local, 'base'));
    zones.push(zone('p0-deck', 'Deck', width - rightCol, localY, 95, 120, local, 'deck'));
    zones.push(zone('p0-discard', 'Trash', width - rightCol, localY + 124, 95, 80, local, 'discard'));
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
