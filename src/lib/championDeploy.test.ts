import { describe, expect, it } from 'vitest';
import { championDeployDestinations } from './championDeploy';

describe('championDeployDestinations', () => {
  it('offers Base plus each active battlefield lane once', () => {
    expect(championDeployDestinations([
      { zoneName: 'battlefield', battlefieldLocationId: 'bf-0' },
      { zoneName: 'battlefield', battlefieldLocationId: 'bf-0' },
      { zoneName: 'battlefield', battlefieldLocationId: 'bf-1' },
      { zoneName: 'base' },
    ])).toEqual([
      { targetZone: 'BASE', label: 'Base' },
      { targetZone: 'BATTLEFIELD', battlefieldLocationId: 'bf-0', label: 'Battlefield 1' },
      { targetZone: 'BATTLEFIELD', battlefieldLocationId: 'bf-1', label: 'Battlefield 2' },
    ]);
  });

  it('defaults missing battlefield ids to bf-0 for legacy layouts', () => {
    expect(championDeployDestinations([{ zoneName: 'battlefield' }])).toEqual([
      { targetZone: 'BASE', label: 'Base' },
      { targetZone: 'BATTLEFIELD', battlefieldLocationId: 'bf-0', label: 'Battlefield 1' },
    ]);
  });
});
