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

export type ZonePlacement = {
  x: number;
  y: number;
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
    const playerColumnWidth = 200;
    const identityWidth = 95;
    const identityGap = 4;
    const identityStart = playerColumnWidth;
    const actionX = identityStart + identityWidth * 2 + identityGap * 2 + 8;
    const utilityColumnWidth = Math.max(64, Math.min(96, width * 0.06));
    const actionWidth = Math.max(360, width - actionX - utilityColumnWidth - 8);
    const divider = 14;
    const half = (height - divider) / 2;
    const runeStrip = 56;
    const baseHeight = 76;
    const battlefieldHeight = half - runeStrip - baseHeight - 8;
    const opponent = others[0] ?? 'opponent';

    // Opponent (top half): Runes at very top (back), then Base, then Battlefield near center
    zones.push(zone('p1-legend', 'Legend', identityStart, 0, identityWidth, half, opponent, 'legend'));
    zones.push(zone('p1-champion', 'Champion', identityStart + identityWidth + identityGap, 0, identityWidth, half, opponent, 'champion'));
    zones.push(zone('p1-rune', 'Runes', actionX, 0, actionWidth, runeStrip, opponent, 'rune'));
    zones.push(zone('p1-base', 'Base', actionX, runeStrip + 4, actionWidth, baseHeight, opponent, 'base'));
    zones.push(zone('p1-battlefield', 'Battlefield', actionX, runeStrip + baseHeight + 8, actionWidth, battlefieldHeight, opponent, 'battlefield'));
    zones.push(zone('center', '', 0, half, width, divider, null, 'limbo'));

    // Local player (bottom half): Battlefield near center, then Base, then Runes at very bottom (back)
    const localY = half + divider;
    zones.push(zone('p0-legend', 'Legend', identityStart, localY, identityWidth, half, local, 'legend'));
    zones.push(zone('p0-champion', 'Champion', identityStart + identityWidth + identityGap, localY, identityWidth, half, local, 'champion'));
    zones.push(zone('p0-battlefield', 'Battlefield', actionX, localY, actionWidth, battlefieldHeight, local, 'battlefield'));
    zones.push(zone('p0-base', 'Base', actionX, localY + battlefieldHeight + 4, actionWidth, baseHeight, local, 'base'));
    zones.push(zone('p0-rune', 'Runes', actionX, localY + battlefieldHeight + baseHeight + 8, actionWidth, runeStrip, local, 'rune'));
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

export function autoPlaceInZone(zone: ZoneRect, existingCount: number, totalCount = existingCount + 1, cardW = 88, cardH = 112): ZonePlacement {
  const paddingX = 16;
  const paddingY = 8;
  const gapX = 10;
  const stepX = cardW + gapX;
  const stepY = cardH / 2 + 8;
  const availableWidth = Math.max(cardW, zone.width - paddingX * 2);
  const cols = Math.max(1, Math.floor((availableWidth + gapX) / stepX));
  const row = Math.floor(existingCount / cols);
  const col = existingCount % cols;
  const itemsInRow = Math.max(1, Math.min(cols, totalCount - row * cols));
  const rowWidth = itemsInRow * cardW + Math.max(0, itemsInRow - 1) * gapX;
  const rowStartX = zone.x + paddingX + Math.max(0, (availableWidth - rowWidth) / 2);

  return {
    x: rowStartX + col * stepX + cardW / 2,
    y: zone.y + paddingY + row * stepY + cardH / 4,
  };
}

export function runeSlotPositions(zone: ZoneRect, slotCount: number, minSpacing = 30, maxSpacing = 72): ZonePlacement[] {
  if (slotCount <= 0) return [];
  const paddingX = 20;
  const availableWidth = Math.max(0, zone.width - paddingX * 2);
  const spacing = slotCount === 1 ? 0 : Math.min(maxSpacing, Math.max(minSpacing, availableWidth / (slotCount - 1)));
  const usedWidth = spacing * Math.max(0, slotCount - 1);
  const startX = zone.x + paddingX + Math.max(0, (availableWidth - usedWidth) / 2);
  const y = zone.y + zone.height / 2;

  return Array.from({ length: slotCount }, (_, index) => ({
    x: startX + index * spacing,
    y,
  }));
}
