import type { PlayerGameState, RiftCard } from '../types';
import type { ZoneRect, ZonePlacement } from '../components/board/BoardLayout';

export type BattlefieldDisplay = {
  playerId: string;
  cardId: string;
  card?: RiftCard;
  label: string;
  imageUrl?: string;
};

export type BattlefieldDisplayFrame = ZonePlacement & {
  width: number;
  height: number;
};

export type BattlefieldDisplaySide = 'left' | 'right';

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

export function selectedBattlefieldIsInteractiveTarget(): false {
  return false;
}

export function selectedBattlefieldSupportsPreview(): true {
  return true;
}
