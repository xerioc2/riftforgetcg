import type { PlayerGameState, RiftCard } from '../types';
import type { ZoneRect, ZonePlacement } from '../components/board/BoardLayout';
import {
  BATTLEFIELD_LOCATIONS,
  battlefieldLocationLabel,
  battlefieldZonesForLocation,
  normalizeBattlefieldLocationId,
  type BattlefieldLocationId,
} from '../components/board/BoardLayout';

export type BattlefieldDisplay = {
  playerId: string;
  cardId: string;
  card?: RiftCard;
  label: string;
  imageUrl?: string;
  locationId?: BattlefieldLocationId;
};

export type BattlefieldDisplayFrame = ZonePlacement & {
  width: number;
  height: number;
};

export type BattlefieldDisplaySide = 'left' | 'right';

export type BattlefieldLaneDisplay = {
  locationId: BattlefieldLocationId;
  cardId?: string;
  card?: RiftCard;
  label: string;
  imageUrl?: string;
  frame: BattlefieldDisplayFrame;
};

export function battlefieldDisplayForPlayer(
  player: PlayerGameState,
  cardsById: Map<string, RiftCard>,
): BattlefieldDisplay | null {
  if (!player.selectedBattlefieldId) return null;
  const card = cardsById.get(player.selectedBattlefieldId);
  return {
    playerId: player.userId,
    cardId: player.selectedBattlefieldId,
    card,
    label: card?.name ?? player.selectedBattlefieldId,
    imageUrl: card?.imageUrl,
  };
}

export function battlefieldDisplayFrame(zone: ZoneRect, side: BattlefieldDisplaySide = 'right'): BattlefieldDisplayFrame {
  const maxWidth = Math.max(80, zone.width - 24);
  const width = Math.min(maxWidth, Math.min(180, Math.max(132, zone.width * 0.16)));
  const maxHeight = Math.max(36, zone.height - 18);
  const height = Math.min(maxHeight, Math.min(66, Math.max(50, zone.height * 0.24)));
  const inset = Math.min(18, Math.max(10, zone.width * 0.015));
  return {
    x: side === 'left' ? zone.x + inset : zone.x + zone.width - width - inset,
    y: zone.y + Math.max(8, (zone.height - height) / 2),
    width,
    height,
  };
}

export function battlefieldLaneFrame(zones: ZoneRect[], locationId: string): BattlefieldDisplayFrame | null {
  const laneZones = battlefieldZonesForLocation(zones, locationId);
  if (laneZones.length === 0) return null;
  const minX = Math.min(...laneZones.map((zone) => zone.x));
  const minY = Math.min(...laneZones.map((zone) => zone.y));
  const maxX = Math.max(...laneZones.map((zone) => zone.x + zone.width));
  const maxY = Math.max(...laneZones.map((zone) => zone.y + zone.height));
  const width = Math.min(Math.max(104, maxX - minX - 22), 170);
  const height = Math.min(Math.max(46, (maxY - minY) * 0.22), 62);
  return {
    x: minX + Math.max(10, ((maxX - minX) - width) / 2),
    y: minY + Math.max(26, ((maxY - minY) - height) / 2),
    width,
    height,
  };
}

export function battlefieldLaneDisplays(
  players: PlayerGameState[],
  localPlayerId: string,
  cardsById: Map<string, RiftCard>,
  zones: ZoneRect[],
): BattlefieldLaneDisplay[] {
  const orderedPlayers = [
    ...players.filter((player) => player.userId === localPlayerId),
    ...players.filter((player) => player.userId !== localPlayerId),
  ];
  return BATTLEFIELD_LOCATIONS.flatMap((locationId, index) => {
    const selectedCardId = orderedPlayers[index]?.selectedBattlefieldId ?? undefined;
    const card = selectedCardId ? cardsById.get(selectedCardId) : undefined;
    const frame = battlefieldLaneFrame(zones, locationId);
    if (!frame) return [];
    const display: BattlefieldLaneDisplay = {
      locationId: normalizeBattlefieldLocationId(locationId),
      label: card?.name ?? selectedCardId ?? battlefieldLocationLabel(locationId),
      frame,
    };
    if (selectedCardId) display.cardId = selectedCardId;
    if (card) display.card = card;
    if (card?.imageUrl) display.imageUrl = card.imageUrl;
    return [display];
  });
}

export function selectedBattlefieldIsInteractiveTarget(): false {
  return false;
}

export function selectedBattlefieldSupportsPreview(): true {
  return true;
}
