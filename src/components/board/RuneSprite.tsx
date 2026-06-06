import { Group, RegularPolygon } from 'react-konva';

interface RuneSpriteProps {
  instanceId: string;
  tapped: boolean;
  normalEnergy?: number;
  isOwner: boolean;
  pending?: boolean;
  x: number;
  y: number;
  onTap: (id: string) => void;
  onDiscard: (id: string) => void;
}

export function RuneSprite({ instanceId, tapped, isOwner, pending = false, x, y, onTap, onDiscard }: RuneSpriteProps) {
  const fill = pending ? '#6fd3b6' : tapped ? '#2b333d' : '#d8b05d';
  const stroke = pending ? '#9eebd8' : tapped ? '#404a55' : '#f0cc80';
  const opacity = tapped && !pending ? 0.35 : 1;

  return (
    <Group
      x={x}
      y={y}
      opacity={opacity}
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
      listening={isOwner}
    >
      <RegularPolygon sides={6} radius={14} fill={fill} stroke={stroke} strokeWidth={1.5} rotation={30} />
    </Group>
  );
}
