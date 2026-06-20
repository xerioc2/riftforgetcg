import type { CardInstance, RiftCard } from '../types';

export const HIDDEN_CARD_LABEL = 'Hidden card';

export type HiddenCardDisplay = {
  label: string;
  subtitle: string;
  masked: boolean;
  canInspect: boolean;
  previewCard: RiftCard | null;
};

function isHiddenLike(instance: CardInstance) {
  return instance.zone.toLowerCase() === 'hidden' || instance.faceDown;
}

export function hiddenCardDisplayForViewer(instance: CardInstance, card: RiftCard | undefined, viewerPlayerId: string | undefined): HiddenCardDisplay {
  const ownerCanSeeIdentity = instance.ownerId === viewerPlayerId && Boolean(card) && instance.cardId !== 'hidden';
  if (!isHiddenLike(instance)) {
    return {
      label: card?.name ?? HIDDEN_CARD_LABEL,
      subtitle: card?.type ?? '',
      masked: !card,
      canInspect: Boolean(card),
      previewCard: card ?? null,
    };
  }
  if (ownerCanSeeIdentity) {
    return {
      label: card?.name ?? HIDDEN_CARD_LABEL,
      subtitle: 'Hidden - only you can see this',
      masked: false,
      canInspect: true,
      previewCard: card ?? null,
    };
  }
  return {
    label: HIDDEN_CARD_LABEL,
    subtitle: 'Face-down hidden card',
    masked: true,
    canInspect: false,
    previewCard: null,
  };
}
