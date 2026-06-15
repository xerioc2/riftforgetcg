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

export function battlefieldDisplayFrame(zone: ZoneRect): BattlefieldDisplayFrame {
  const maxWidth = Math.max(80, zone.width - 24);
  const width = Math.min(maxWidth, Math.min(210, Math.max(150, zone.width * 0.24)));
  const maxHeight = Math.max(36, zone.height - 18);
  const height = Math.min(maxHeight, Math.min(74, Math.max(54, zone.height * 0.28)));
  return {
    x: zone.x + zone.width - width - 18,
    y: zone.y + Math.min(14, Math.max(4, zone.height - height - 4)),
    width,
    height,
  };
}

export function selectedBattlefieldIsInteractiveTarget(): false {
  return false;
}
