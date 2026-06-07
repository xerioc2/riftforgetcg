import { useEffect, useRef } from 'react';
import Konva from 'konva';
import type { Group as KonvaGroup } from 'konva/lib/Group';
import { Tween } from 'konva/lib/Tween';
import { Group, Image, Rect, Text } from 'react-konva';
import useImage from 'use-image';
import type { CardInstance, RiftCard } from '../../types';

const CARD_BACK_URL = 'data:image/gif;base64,R0lGODlhAQABAIABACIyMgAAACwAAAAAAQABAAACAkQBADs=';
const CARD_WIDTH = 80;
const CARD_HEIGHT = 112;

function typeFill(type?: string): string {
  switch (type?.toLowerCase()) {
    case 'champion':
      return '#2a1f0a';
    case 'unit':
      return '#0d1f1a';
    case 'spell':
      return '#0d0f1f';
    case 'gear':
      return '#1a1a1a';
    case 'rune':
      return '#1a0d1f';
    default:
      return '#16191e';
  }
}

export function CardSprite({
  instance,
  cardDef,
  isOwner,
  onDragEnd,
  onClick,
  onDoubleClick,
  onContextMenu,
  selected,
  animate,
  scale = 1,
  onHover,
}: {
  instance: CardInstance;
  cardDef: RiftCard;
  isOwner: boolean;
  onDragEnd: (id: string, x: number, y: number) => void;
  onClick: (id: string) => void;
  onDoubleClick: (id: string) => void;
  onContextMenu: (id: string) => void;
  selected: boolean;
  animate?: boolean;
  scale?: number;
  onHover?: (card: RiftCard | null) => void;
}) {
  const [image] = useImage(instance.faceDown ? CARD_BACK_URL : cardDef.imageUrl ?? '');
  const groupRef = useRef<KonvaGroup | null>(null);
  const isDiscarded = instance.zone.toLowerCase() === 'discard';
  const isBoardUnit = ['base', 'battlefield'].includes(instance.zone.toLowerCase());
  const maxHealth = cardDef.health ?? 0;
  const currentHealth = instance.currentHealth ?? maxHealth;
  const isDamaged = isBoardUnit && maxHealth > 0 && instance.currentHealth != null && currentHealth < maxHealth;
  const healthRatio = maxHealth > 0 ? Math.max(0, Math.min(1, currentHealth / maxHealth)) : 0;
  const isSick = instance.zone.toLowerCase() === 'battlefield' && instance.hasSummoningSickness === true;
  const effectivePower = (cardDef.power ?? 0) + (instance.temporaryPowerModifier ?? 0);

  useEffect(() => {
    if (!groupRef.current) return;
    new Tween({
      node: groupRef.current,
      rotation: instance.tapped ? 90 : 0,
      duration: 0.15,
      easing: Konva.Easings.EaseOut,
    }).play();
  }, [instance.tapped]);

  useEffect(() => {
    if (instance.zone.toLowerCase() !== 'discard' || !groupRef.current) return;
    new Tween({
      node: groupRef.current,
      opacity: 0,
      duration: 0.3,
      easing: Konva.Easings.EaseIn,
    }).play();
  }, [instance.zone]);

  useEffect(() => {
    if (!animate || !groupRef.current) return;
    groupRef.current.y(instance.y + 80);
    new Tween({
      node: groupRef.current,
      y: instance.y,
      duration: 0.2,
      easing: Konva.Easings.EaseOut,
    }).play();
  }, [animate, instance.y]);

  return (
    <Group
      ref={groupRef}
      x={instance.x}
      y={instance.y}
      offsetX={CARD_WIDTH / 2}
      offsetY={CARD_HEIGHT / 2}
      scaleX={scale}
      scaleY={scale}
      rotation={instance.tapped ? 90 : 0}
      visible={!isDiscarded}
      draggable={isOwner}
      onDragEnd={(event) => onDragEnd(instance.instanceId, event.target.x(), event.target.y())}
      onClick={() => onClick(instance.instanceId)}
      onTap={() => onClick(instance.instanceId)}
      onMouseEnter={() => onHover?.(instance.faceDown ? null : cardDef)}
      onMouseLeave={() => onHover?.(null)}
      onDblClick={() => onDoubleClick(instance.instanceId)}
      onDblTap={() => onDoubleClick(instance.instanceId)}
      onContextMenu={(event) => {
        event.evt.preventDefault();
        onContextMenu(instance.instanceId);
      }}
    >
      {image && !instance.faceDown ? (
        <Image image={image} width={CARD_WIDTH} height={CARD_HEIGHT} cornerRadius={4} />
      ) : (
        <>
          <Rect width={CARD_WIDTH} height={CARD_HEIGHT} fill={typeFill(cardDef.type)} stroke="#2b333d" strokeWidth={1} cornerRadius={4} />
          <Rect x={2} y={2} width={16} height={16} fill="rgba(0,0,0,0.5)" cornerRadius={3} />
          <Text x={2} y={4} width={16} text={String(cardDef.cost ?? 0)} align="center" fontSize={10} fontStyle="bold" fill="#d8b05d" />
          <Text x={2} y={24} width={CARD_WIDTH - 4} text={instance.faceDown ? '?' : cardDef.name} align="center" fontSize={9} fontStyle="bold" fill="#ffffff" wrap="word" />
          <Text x={2} y={CARD_HEIGHT / 2 - 6} width={CARD_WIDTH - 4} text={instance.faceDown ? '' : cardDef.type ?? ''} align="center" fontSize={8} fill="rgba(255,255,255,0.5)" />
          {!instance.faceDown && cardDef.power != null && cardDef.health != null ? (
            <>
              <Rect x={CARD_WIDTH - 28} y={CARD_HEIGHT - 18} width={26} height={14} fill="rgba(0,0,0,0.5)" cornerRadius={3} />
              <Text x={CARD_WIDTH - 28} y={CARD_HEIGHT - 16} width={26} text={`${effectivePower}/${instance.currentHealth ?? cardDef.health}`} align="center" fontSize={9} fontStyle="bold" fill="#6fd3b6" />
            </>
          ) : null}
        </>
      )}
      {isSick ? (
        <>
          <Rect width={CARD_WIDTH} height={CARD_HEIGHT} fill="rgba(0,0,0,0.35)" cornerRadius={4} listening={false} />
          <Text y={CARD_HEIGHT / 2 - 5} width={CARD_WIDTH} text="SICK" align="center" fontSize={9} fill="rgba(255,255,255,0.5)" listening={false} />
        </>
      ) : null}
      {isDamaged ? (
        <>
          <Text x={2} y={CARD_HEIGHT - 16} width={CARD_WIDTH - 4} text={`${currentHealth}/${maxHealth}`} align="right" fontSize={7} fill="#ffffff" listening={false} />
          <Rect y={CARD_HEIGHT - 7} width={CARD_WIDTH} height={5} fill="#4b5563" listening={false} />
          <Rect y={CARD_HEIGHT - 7} width={CARD_WIDTH * healthRatio} height={5} fill={healthRatio > 0.5 ? '#6fd3b6' : '#e56c4f'} listening={false} />
        </>
      ) : null}
      {selected ? <Rect width={CARD_WIDTH} height={CARD_HEIGHT} stroke="#d8b05d" strokeWidth={3} cornerRadius={4} listening={false} /> : null}
      {instance.attachedToInstanceId ? <Text x={2} y={CARD_HEIGHT - 14} width={CARD_WIDTH - 4} text="EQUIPPED" align="center" fontSize={7} fontStyle="bold" fill="#d8b05d" listening={false} /> : null}
    </Group>
  );
}
