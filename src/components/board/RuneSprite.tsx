import { Group, Image, Rect, RegularPolygon, Text } from 'react-konva';
import useImage from 'use-image';
import { runeDisplayState } from '../../lib/runeDisplay';
import type { RiftCard } from '../../types';

interface RuneSpriteProps {
  instanceId: string;
  cardId?: string | null;
  cardDef?: RiftCard;
  tapped: boolean;
  normalEnergy?: number;
  premiumEnergy?: number;
  isOwner: boolean;
  pending?: boolean;
  x: number;
  y: number;
  onTap: (id: string) => void;
  onDiscard: (id: string) => void;
  onHover?: (card: RiftCard | null) => void;
}

const RUNE_CARD_WIDTH = 42;
const RUNE_CARD_HEIGHT = 58;

export function RuneSprite({
  instanceId,
  cardId,
  cardDef,
  tapped,
  normalEnergy = 1,
  premiumEnergy = 1,
  isOwner,
  pending = false,
  x,
  y,
  onTap,
  onDiscard,
  onHover,
}: RuneSpriteProps) {
  const display = runeDisplayState({ cardId: cardId ?? '', normalEnergy, premiumEnergy, tapped }, cardDef, pending);
  const [image] = useImage(cardDef?.imageUrl ?? '');

  return (
    <Group
      x={x}
      y={y}
      opacity={display.opacity}
      onClick={() => {
        if (isOwner) onTap(instanceId);
      }}
      onTap={() => {
        if (isOwner) onTap(instanceId);
      }}
      onContextMenu={(event) => {
        event.evt.preventDefault();
        if (isOwner && !tapped && !pending) onDiscard(instanceId);
      }}
      onMouseEnter={() => onHover?.(cardDef ?? null)}
      onMouseLeave={() => onHover?.(null)}
      listening
    >
      {cardDef ? (
        <>
          <Rect
            x={-RUNE_CARD_WIDTH / 2}
            y={-RUNE_CARD_HEIGHT / 2}
            width={RUNE_CARD_WIDTH}
            height={RUNE_CARD_HEIGHT}
            fill={display.fillColor}
            stroke={display.borderColor}
            strokeWidth={pending ? 2.25 : 1.25}
            cornerRadius={4}
          />
          {image ? (
            <Image
              image={image}
              x={-RUNE_CARD_WIDTH / 2 + 2}
              y={-RUNE_CARD_HEIGHT / 2 + 2}
              width={RUNE_CARD_WIDTH - 4}
              height={RUNE_CARD_HEIGHT - 4}
              cornerRadius={3}
            />
          ) : (
            <>
              <RegularPolygon sides={6} radius={13} fill="#d8b05d" stroke="#f0cc80" strokeWidth={1} rotation={30} />
              <Text
                x={-RUNE_CARD_WIDTH / 2 + 4}
                y={-RUNE_CARD_HEIGHT / 2 + 7}
                width={RUNE_CARD_WIDTH - 8}
                text={cardDef.name}
                align="center"
                fontSize={7}
                fontStyle="bold"
                fill="#f8fafc"
                wrap="word"
              />
            </>
          )}
          <Rect
            x={-RUNE_CARD_WIDTH / 2 + 3}
            y={RUNE_CARD_HEIGHT / 2 - 15}
            width={RUNE_CARD_WIDTH - 6}
            height={12}
            fill="rgba(5,8,13,0.86)"
            stroke={pending ? '#9eebd8' : 'rgba(216,176,93,0.72)'}
            strokeWidth={0.75}
            cornerRadius={3}
            listening={false}
          />
          <Text
            x={-RUNE_CARD_WIDTH / 2 + 5}
            y={RUNE_CARD_HEIGHT / 2 - 13}
            width={RUNE_CARD_WIDTH - 10}
            text={display.domainLabel.slice(0, 7)}
            align="center"
            fontSize={6.25}
            fontStyle="bold"
            fill={pending ? '#9eebd8' : '#f2d58a'}
            listening={false}
            ellipsis
          />
          {tapped && !pending ? (
            <Rect
              x={-RUNE_CARD_WIDTH / 2}
              y={-RUNE_CARD_HEIGHT / 2}
              width={RUNE_CARD_WIDTH}
              height={RUNE_CARD_HEIGHT}
              fill="rgba(3,7,18,0.36)"
              cornerRadius={4}
              listening={false}
            />
          ) : null}
        </>
      ) : (
        <>
          <RegularPolygon sides={6} radius={14} fill={display.fillColor} stroke={display.borderColor} strokeWidth={1.5} rotation={30} />
          <Text x={-17} y={17} width={34} text={String(normalEnergy)} align="center" fontSize={7} fontStyle="bold" fill="#f2d58a" listening={false} />
        </>
      )}
    </Group>
  );
}
