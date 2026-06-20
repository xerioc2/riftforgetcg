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
  battlefieldLocationId?: string;
};

export type ZonePlacement = {
  x: number;
  y: number;
};

export type SharedBattlefieldSides = {
  opponentSide: ZoneRect;
  playerSide: ZoneRect;
};

export const BATTLEFIELD_LOCATIONS = ['bf-0', 'bf-1', 'bf-2'] as const;
export type BattlefieldLocationId = (typeof BATTLEFIELD_LOCATIONS)[number];
export const DEFAULT_BATTLEFIELD_LOCATION_ID: BattlefieldLocationId = 'bf-0';
export const DUEL_BATTLEFIELD_LOCATIONS = ['bf-0', 'bf-1'] as const satisfies readonly BattlefieldLocationId[];

export function normalizeBattlefieldLocationId(locationId?: string | null): BattlefieldLocationId {
  const normalized = locationId?.trim();
  return BATTLEFIELD_LOCATIONS.includes(normalized as BattlefieldLocationId)
    ? normalized as BattlefieldLocationId
    : DEFAULT_BATTLEFIELD_LOCATION_ID;
}

export function activeBattlefieldLocationsForPlayerCount(playerCount: number): readonly BattlefieldLocationId[] {
  return playerCount <= 2 ? DUEL_BATTLEFIELD_LOCATIONS : BATTLEFIELD_LOCATIONS;
}

export function battlefieldLocationLabel(locationId?: string | null): string {
  const normalized = normalizeBattlefieldLocationId(locationId);
  const index = BATTLEFIELD_LOCATIONS.indexOf(normalized);
  return `Battlefield ${index + 1}`;
}

const zone = (
  id: string,
  label: string,
  x: number,
  y: number,
  width: number,
  height: number,
  ownerId: string | null,
  zoneName: ZoneName,
  battlefieldLocationId?: string,
): ZoneRect => ({
  id,
  label,
  x,
  y,
  width,
  height,
  ownerId,
  zoneName,
  battlefieldLocationId,
});

export function computeLayout(width: number, height: number, playerIds: string[]): ZoneRect[] {
  const [local, ...others] = playerIds;
  if (!local) return [];
  const zones: ZoneRect[] = [];
  const activeBattlefieldLocations = activeBattlefieldLocationsForPlayerCount(playerIds.length);

  if (playerIds.length <= 2) {
    const playerColumnWidth = 200;
    const identityWidth = 88;
    const identityHeight = 126;
    const identityGap = 6;
    const identityStart = playerColumnWidth + 4;
    const actionX = identityStart + identityWidth * 2 + identityGap + 8;
    const rightPadding = Math.max(12, Math.min(24, width * 0.015));
    const actionWidth = Math.max(420, width - actionX - rightPadding);
    const runeStrip = 78;
    const runeWidth = actionWidth;
    const baseHeight = Math.max(100, Math.min(124, height * 0.145));
    const rowGap = 14;
    const utilityHeight = runeStrip + baseHeight + 12;
    const maxBattlefieldHeight = Math.max(150, height - utilityHeight * 2 - rowGap * 2);
    const battlefieldHeight = Math.min(230, Math.max(150, Math.min(maxBattlefieldHeight, height * 0.31)));
    const topUtilityHeight = Math.max(utilityHeight, (height - battlefieldHeight - rowGap) / 2);
    const battlefieldY = topUtilityHeight;
    const localUtilityY = battlefieldY + battlefieldHeight + rowGap;
    const locationGap = Math.max(10, Math.min(18, actionWidth * 0.012));
    const locationWidth = (actionWidth - locationGap * (activeBattlefieldLocations.length - 1)) / activeBattlefieldLocations.length;
    const sideGap = 8;
    const battlefieldSideHeight = (battlefieldHeight - sideGap) / 2;
    const opponent = others[0] ?? 'opponent';
    const opponentIdentityY = Math.max(0, topUtilityHeight - identityHeight - 4);

    // Opponent setup zones stay separate. The Battlefield itself is a shared row split into two controller sides.
    zones.push(zone('p1-legend', 'Legend', identityStart, opponentIdentityY, identityWidth, identityHeight, opponent, 'legend'));
    zones.push(zone('p1-champion', 'Champion', identityStart + identityWidth + identityGap, opponentIdentityY, identityWidth, identityHeight, opponent, 'champion'));
    zones.push(zone('p1-rune', 'Runes', actionX, 0, runeWidth, runeStrip, opponent, 'rune'));
    zones.push(zone('p1-base', 'Base', actionX, runeStrip + 8, actionWidth, baseHeight, opponent, 'base'));
    activeBattlefieldLocations.forEach((locationId, index) => {
      const x = actionX + index * (locationWidth + locationGap);
      const label = battlefieldLocationLabel(locationId);
      zones.push(zone(`p1-battlefield-${locationId}`, `${label}: opponent`, x, battlefieldY, locationWidth, battlefieldSideHeight, opponent, 'battlefield', locationId));
      zones.push(zone(`p0-battlefield-${locationId}`, `${label}: you`, x, battlefieldY + battlefieldSideHeight + sideGap, locationWidth, battlefieldSideHeight, local, 'battlefield', locationId));
    });

    // Local setup zones stay separate below the shared Battlefield row.
    zones.push(zone('p0-legend', 'Legend', identityStart, localUtilityY, identityWidth, Math.min(identityHeight, height - localUtilityY), local, 'legend'));
    zones.push(zone('p0-champion', 'Champion', identityStart + identityWidth + identityGap, localUtilityY, identityWidth, Math.min(identityHeight, height - localUtilityY), local, 'champion'));
    zones.push(zone('p0-base', 'Base', actionX, localUtilityY, actionWidth, baseHeight, local, 'base'));
    zones.push(zone('p0-rune', 'Runes', actionX, Math.min(localUtilityY + baseHeight + 8, height - runeStrip), runeWidth, runeStrip, local, 'rune'));
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
  const rows = Math.max(1, Math.ceil(totalCount / cols));
  const rowWidth = itemsInRow * cardW + Math.max(0, itemsInRow - 1) * gapX;
  const rowStartX = zone.x + paddingX + Math.max(0, (availableWidth - rowWidth) / 2);
  const contentHeight = cardH + Math.max(0, rows - 1) * stepY;
  const rowStartY = zone.y + Math.max(paddingY, (zone.height - contentHeight) / 2);

  return {
    x: rowStartX + col * stepX + cardW / 2,
    y: rowStartY + row * stepY + cardH / 2,
  };
}

export function clampPlacementToZone(position: ZonePlacement, zone: ZoneRect, margin = 12): ZonePlacement {
  return {
    x: Math.max(zone.x + margin, Math.min(zone.x + zone.width - margin, position.x)),
    y: Math.max(zone.y + margin, Math.min(zone.y + zone.height - margin, position.y)),
  };
}

export function sharedBattlefieldSides(zones: ZoneRect[]): SharedBattlefieldSides | null {
  const battlefieldZones = zones.filter((candidate) => candidate.zoneName === 'battlefield' && candidate.ownerId);
  const opponentSide = battlefieldZones.find((candidate) => candidate.id === `p1-battlefield-${DEFAULT_BATTLEFIELD_LOCATION_ID}`);
  const playerSide = battlefieldZones.find((candidate) => candidate.id === `p0-battlefield-${DEFAULT_BATTLEFIELD_LOCATION_ID}`);
  if (!opponentSide || !playerSide) return null;
  return { opponentSide, playerSide };
}

export function battlefieldZonesForLocation(zones: ZoneRect[], locationId?: string | null): ZoneRect[] {
  const normalized = normalizeBattlefieldLocationId(locationId);
  return zones.filter((zone) => zone.zoneName === 'battlefield' && normalizeBattlefieldLocationId(zone.battlefieldLocationId) === normalized);
}

export function battlefieldZoneForCard(zones: ZoneRect[], ownerId: string, locationId?: string | null): ZoneRect | undefined {
  const normalized = normalizeBattlefieldLocationId(locationId);
  return zones.find((zone) =>
    zone.zoneName === 'battlefield'
    && zone.ownerId === ownerId
    && normalizeBattlefieldLocationId(zone.battlefieldLocationId) === normalized);
}

export function battlefieldLocationForPoint(x: number, y: number, zones: ZoneRect[]): BattlefieldLocationId | undefined {
  return zones.find((zone) =>
    zone.zoneName === 'battlefield'
    && x >= zone.x
    && x <= zone.x + zone.width
    && y >= zone.y
    && y <= zone.y + zone.height)
    ?.battlefieldLocationId as BattlefieldLocationId | undefined;
}

export function runeSlotPositions(zone: ZoneRect, slotCount: number, minSpacing = 34, maxSpacing = 128): ZonePlacement[] {
  if (slotCount <= 0) return [];
  const paddingX = 20;
  const availableWidth = Math.max(0, zone.width - paddingX * 2);
  const naturalSpacing = slotCount === 1 ? 0 : availableWidth / (slotCount - 1);
  const spacing = slotCount === 1 ? 0 : naturalSpacing >= minSpacing ? Math.min(maxSpacing, naturalSpacing) : naturalSpacing;
  const usedWidth = spacing * Math.max(0, slotCount - 1);
  const startX = zone.x + paddingX + Math.max(0, (availableWidth - usedWidth) / 2);
  const y = zone.y + zone.height / 2;

  return Array.from({ length: slotCount }, (_, index) => ({
    x: startX + index * spacing,
    y,
  }));
}
