import { Group, Image, Rect, Text } from 'react-konva';
import useImage from 'use-image';
import type { RiftCard } from '../../types';
import type { BattlefieldDisplayFrame } from '../../lib/battlefieldDisplay';

export function BattlefieldLocationDisplay({
  frame,
  card,
  label,
  imageUrl,
  onHover,
}: {
  frame: BattlefieldDisplayFrame;
  card?: RiftCard;
  label: string;
  imageUrl?: string;
  onHover?: (card: RiftCard | null) => void;
}) {
  const [image] = useImage(imageUrl ?? '');

  return (
    <Group
      x={frame.x}
      y={frame.y}
      onMouseEnter={() => onHover?.(card ?? null)}
      onMouseLeave={() => onHover?.(null)}
      onTap={() => onHover?.(card ?? null)}
    >
      <Rect
        width={frame.width}
        height={frame.height}
        fill="rgba(6, 12, 17, 0.94)"
        stroke="rgba(216, 176, 93, 0.68)"
        strokeWidth={1.5}
        cornerRadius={6}
        shadowColor="black"
        shadowBlur={12}
        shadowOpacity={0.42}
      />
      {image ? (
        <Image
          image={image}
          x={6}
          y={6}
          width={Math.min(48, frame.width * 0.28)}
          height={frame.height - 12}
          cornerRadius={4}
          opacity={0.92}
          listening={false}
        />
      ) : (
        <Rect
          x={6}
          y={6}
          width={Math.min(48, frame.width * 0.28)}
          height={frame.height - 12}
          fill="rgba(21, 30, 38, 0.95)"
          stroke="rgba(148, 163, 184, 0.45)"
          strokeWidth={1}
          cornerRadius={4}
          listening={false}
        />
      )}
      <Text
        x={Math.min(62, frame.width * 0.34)}
        y={8}
        width={frame.width - Math.min(72, frame.width * 0.38)}
        text="Battlefield"
        fontSize={9}
        fill="#d8b05d"
        fontStyle="bold"
        listening={false}
      />
      <Text
        x={Math.min(62, frame.width * 0.34)}
        y={23}
        width={frame.width - Math.min(72, frame.width * 0.38)}
        height={frame.height - 26}
        text={label}
        fontSize={12}
        fill="#f8fafc"
        fontStyle="bold"
        wrap="word"
        listening={false}
      />
    </Group>
  );
}
