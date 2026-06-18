import { Group, Rect, Text } from 'react-konva';
import { sharedBattlefieldSides, type ZoneRect } from './BoardLayout';

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
  const sharedBattlefield = sharedBattlefieldSides(zones);
  const sharedFrame = sharedBattlefield
    ? {
        x: sharedBattlefield.opponentSide.x,
        y: sharedBattlefield.opponentSide.y,
        width: sharedBattlefield.playerSide.x + sharedBattlefield.playerSide.width - sharedBattlefield.opponentSide.x,
        height: sharedBattlefield.opponentSide.height,
        dividerX: (sharedBattlefield.opponentSide.x + sharedBattlefield.opponentSide.width + sharedBattlefield.playerSide.x) / 2,
      }
    : null;

  return (
    <>
      {zones.map((zone) => (
        <Group key={zone.id} listening={false}>
          <Rect x={zone.x} y={zone.y} width={zone.width} height={zone.height} fill={zoneFill(zone.zoneName)} stroke="#2b333d" cornerRadius={4} />
          {zone.zoneName !== 'limbo' ? <Text x={zone.x} y={zone.y + zone.height / 2 - 6} width={zone.width} text={zone.label} align="center" fontSize={11} fill="#64748b" /> : null}
        </Group>
      ))}
      {sharedFrame ? (
        <Group listening={false}>
          <Rect
            x={sharedFrame.x}
            y={sharedFrame.y}
            width={sharedFrame.width}
            height={sharedFrame.height}
            fill="rgba(0,0,0,0)"
            stroke="rgba(216,176,93,0.24)"
            strokeWidth={1.5}
            cornerRadius={5}
          />
          <Rect
            x={sharedFrame.dividerX - 1}
            y={sharedFrame.y + 12}
            width={2}
            height={Math.max(20, sharedFrame.height - 24)}
            fill="rgba(216,176,93,0.28)"
            cornerRadius={1}
          />
          <Text
            x={sharedFrame.dividerX - 80}
            y={sharedFrame.y + 8}
            width={160}
            text="Shared Battlefield"
            align="center"
            fontSize={11}
            fill="rgba(216,176,93,0.76)"
          />
        </Group>
      ) : null}
    </>
  );
}
