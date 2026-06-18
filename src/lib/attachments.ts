import type { CardInstance, RiftCard } from '../types';

export type AttachedGearDisplay = {
  gear: CardInstance;
  name: string;
};

export function attachedGearForHost(
  cards: CardInstance[],
  cardsById: Map<string, RiftCard>,
  hostInstanceId: string,
): AttachedGearDisplay[] {
  return cards
    .filter((card) => card.attachedToInstanceId === hostInstanceId)
    .map((gear) => ({
      gear,
      name: cardsById.get(gear.cardId)?.name ?? 'Equipment',
    }));
}

export function attachedGearNamesForHost(
  cards: CardInstance[],
  cardsById: Map<string, RiftCard>,
  hostInstanceId: string,
): string[] {
  return attachedGearForHost(cards, cardsById, hostInstanceId).map((item) => item.name);
}

export function attachedGearDisplayPosition(hostPosition: { x: number; y: number }) {
  return {
    x: hostPosition.x + 46,
    y: hostPosition.y + 42,
  };
}

export function attachedGearDisplayScale(cardScale: number): number {
  return Math.max(0.62, Math.min(cardScale * 0.72, 0.86));
}

export function attachedGearIsIndependentBoardPiece(instance: CardInstance): boolean {
  return !instance.attachedToInstanceId;
}
