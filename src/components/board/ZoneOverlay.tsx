import { Group, Rect, Text } from 'react-konva';
import type { ZoneRect } from './BoardLayout';

function zoneFill(zoneName: string): string {
  switch (zoneName) {
    case 'champion':
      return 'rgba(80,50,10,0.18)';
    case 'legend':
      return 'rgba(60,40,80,0.18)';
    case 'base':
      return 'rgba(20,40,30,0.25)';
    case 'battlefield':
      return 'rgba(10,25,20,0.22)';
    case 'rune':
      return 'rgba(50,40,10,0.22)';
    case 'rune-deck':
      return 'rgba(40,30,5,0.25)';
    case 'deck':
      return 'rgba(20,20,40,0.22)';
    case 'discard':
      return 'rgba(40,15,10,0.22)';
    default:
      return 'rgba(255,255,255,0.03)';
  }
}

export function ZoneOverlay({ zones }: { zones: ZoneRect[] }) {
  return (
    <>
      {zones.map((zone) => (
        <Group key={zone.id} listening={false}>
          <Rect x={zone.x} y={zone.y} width={zone.width} height={zone.height} fill={zoneFill(zone.zoneName)} stroke="#2b333d" cornerRadius={4} />
          {zone.zoneName !== 'limbo' ? <Text x={zone.x} y={zone.y + zone.height / 2 - 6} width={zone.width} text={zone.label} align="center" fontSize={11} fill="#64748b" /> : null}
        </Group>
      ))}
    </>
  );
}
