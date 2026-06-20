export type ChampionDeployDestination =
  | { targetZone: 'BASE'; label: string }
  | { targetZone: 'BATTLEFIELD'; battlefieldLocationId: string; label: string };

export function championDeployDestinations(
  zones: Array<{ zoneName: string; battlefieldLocationId?: string | null }>,
): ChampionDeployDestination[] {
  const battlefieldIds = zones
    .filter((zone) => zone.zoneName.toLowerCase() === 'battlefield')
    .map((zone) => normalizeLocationId(zone.battlefieldLocationId))
    .filter((value, index, all) => all.indexOf(value) === index);

  return [
    { targetZone: 'BASE', label: 'Base' },
    ...battlefieldIds.map((battlefieldLocationId) => ({
      targetZone: 'BATTLEFIELD' as const,
      battlefieldLocationId,
      label: battlefieldLabel(battlefieldLocationId),
    })),
  ];
}

function normalizeLocationId(locationId?: string | null) {
  return locationId && /^bf-\d+$/.test(locationId) ? locationId : 'bf-0';
}

function battlefieldLabel(locationId: string) {
  const index = Number.parseInt(locationId.replace('bf-', ''), 10);
  return Number.isFinite(index) ? `Battlefield ${index + 1}` : 'Battlefield';
}
