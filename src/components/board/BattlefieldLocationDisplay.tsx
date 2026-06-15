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
        fill="rgba(5, 10, 14, 0.88)"
        stroke="rgba(216, 176, 93, 0.55)"
        strokeWidth={1.5}
        cornerRadius={6}
        shadowColor="black"
        shadowBlur={10}
        shadowOpacity={0.35}
        listening={false}
      />
      {image ? (
        <Image
          image={image}
          x={6}
          y={6}
          width={Math.min(62, frame.width * 0.32)}
          height={frame.height - 12}
          cornerRadius={4}
          opacity={0.92}
          listening={false}
        />
      ) : (
        <Rect
          x={6}
          y={6}
          width={Math.min(62, frame.width * 0.32)}
          height={frame.height - 12}
          fill="rgba(21, 30, 38, 0.95)"
          stroke="rgba(148, 163, 184, 0.45)"
          strokeWidth={1}
          cornerRadius={4}
          listening={false}
        />
      )}
      <Text
        x={Math.min(76, frame.width * 0.38)}
        y={10}
        width={frame.width - Math.min(86, frame.width * 0.42)}
        text="Selected Battlefield"
        fontSize={9}
        fill="#d8b05d"
        fontStyle="bold"
        listening={false}
      />
      <Text
        x={Math.min(76, frame.width * 0.38)}
        y={26}
        width={frame.width - Math.min(86, frame.width * 0.42)}
        height={frame.height - 30}
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
