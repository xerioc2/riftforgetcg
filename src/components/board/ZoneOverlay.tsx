import { Group, Rect, Text } from 'react-konva';
import { BATTLEFIELD_LOCATIONS, battlefieldLocationLabel, battlefieldZonesForLocation, normalizeBattlefieldLocationId, type ZoneRect } from './BoardLayout';

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

export function ZoneOverlay({
  zones,
  activeShowdownLocationId,
  battlefieldController = {},
  playerNames = {},
}: {
  zones: ZoneRect[];
  activeShowdownLocationId?: string | null;
  battlefieldController?: Record<string, string>;
  playerNames?: Record<string, string>;
}) {
  const activeLocation = activeShowdownLocationId ? normalizeBattlefieldLocationId(activeShowdownLocationId) : undefined;
  const locationFrames = BATTLEFIELD_LOCATIONS.map((locationId) => {
    const laneZones = battlefieldZonesForLocation(zones, locationId);
    if (laneZones.length === 0) return null;
    const minX = Math.min(...laneZones.map((zone) => zone.x));
    const minY = Math.min(...laneZones.map((zone) => zone.y));
    const maxX = Math.max(...laneZones.map((zone) => zone.x + zone.width));
    const maxY = Math.max(...laneZones.map((zone) => zone.y + zone.height));
    const dividerY = laneZones.length > 1 ? (laneZones[0].y + laneZones[0].height + laneZones[1].y) / 2 : undefined;
    return {
      locationId,
      x: minX,
      y: minY,
      width: maxX - minX,
      height: maxY - minY,
      dividerY,
    };
  }).filter((frame): frame is NonNullable<typeof frame> => frame != null);

  return (
    <>
      {zones.map((zone) => (
        <Group key={zone.id} listening={false}>
          <Rect x={zone.x} y={zone.y} width={zone.width} height={zone.height} fill={zoneFill(zone.zoneName)} stroke="#2b333d" cornerRadius={4} />
          {zone.zoneName !== 'limbo' && zone.zoneName !== 'battlefield' ? <Text x={zone.x} y={zone.y + zone.height / 2 - 6} width={zone.width} text={zone.label} align="center" fontSize={11} fill="#64748b" /> : null}
        </Group>
      ))}
      {locationFrames.map((frame) => {
        const controllerId = battlefieldController[frame.locationId];
        const controllerName = controllerId ? playerNames[controllerId] ?? controllerId : undefined;
        const active = activeLocation === frame.locationId;
        return (
        <Group key={`battlefield-lane-${frame.locationId}`} listening={false}>
          <Rect
            x={frame.x}
            y={frame.y}
            width={frame.width}
            height={frame.height}
            fill="rgba(0,0,0,0)"
            stroke={active ? 'rgba(216,176,93,0.88)' : 'rgba(216,176,93,0.24)'}
            strokeWidth={active ? 2.5 : 1.5}
            cornerRadius={5}
            shadowColor={active ? '#d8b05d' : undefined}
            shadowBlur={active ? 16 : 0}
            shadowOpacity={active ? 0.35 : 0}
          />
          {frame.dividerY ? (
            <Rect
              x={frame.x + 10}
              y={frame.dividerY - 1}
              width={Math.max(20, frame.width - 20)}
              height={2}
              fill="rgba(216,176,93,0.22)"
              cornerRadius={1}
            />
          ) : null}
          <Text
            x={frame.x + 8}
            y={frame.y + 6}
            width={frame.width - 16}
            text={battlefieldLocationLabel(frame.locationId)}
            align="center"
            fontSize={11}
            fontStyle="bold"
            fill={active ? 'rgba(255,228,164,0.95)' : 'rgba(216,176,93,0.76)'}
          />
          {controllerName ? (
            <Text
              x={frame.x + 8}
              y={frame.y + frame.height - 18}
              width={frame.width - 16}
              text={`Controlled by ${controllerName}`}
              align="center"
              fontSize={9}
              fill="rgba(148,163,184,0.86)"
            />
          ) : null}
        </Group>
      );})}
    </>
  );
}
