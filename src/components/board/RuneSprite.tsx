import { Group, RegularPolygon, Text } from 'react-konva';

interface RuneSpriteProps {
  instanceId: string;
  tapped: boolean;
  normalEnergy: number;
  isOwner: boolean;
  x: number;
  y: number;
  onTap: (id: string) => void;
  onDiscard: (id: string) => void;
}

export function RuneSprite({ instanceId, tapped, normalEnergy, isOwner, x, y, onTap, onDiscard }: RuneSpriteProps) {
  return (
    <Group
      x={x}
      y={y}
      opacity={tapped ? 0.35 : 1}
      onClick={() => {
        if (isOwner && !tapped) onTap(instanceId);
      }}
      onTap={() => {
        if (isOwner && !tapped) onTap(instanceId);
      }}
      onContextMenu={(event) => {
        event.evt.preventDefault();
        if (isOwner && !tapped) onDiscard(instanceId);
      }}
      listening={isOwner}
    >
      <RegularPolygon sides={6} radius={14} fill={tapped ? '#2b333d' : '#d8b05d'} stroke={tapped ? '#404a55' : '#f0cc80'} strokeWidth={1.5} rotation={30} />
      <Text x={-14} y={-6} width={28} text={String(normalEnergy)} align="center" fontSize={11} fontStyle="bold" fill={tapped ? '#4a5560' : '#101418'} listening={false} />
    </Group>
  );
}
