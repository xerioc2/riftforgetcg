import { describe, expect, it } from 'vitest';
import { phaseGuidance } from './gameGuidance';

describe('gameGuidance', () => {
  it('explains that Stacked Deck creates its private choice after chain responses resolve', () => {
    expect(phaseGuidance('MAIN', false, undefined, true, false, {
      itemId: 'item-1',
      controllerPlayerId: 'p1',
      sourceCardName: 'Stacked Deck',
      effectKey: 'STACKED_DECK_PICK_ONE',
    })).toBe('Stacked Deck is waiting on chain responses. Resolve the chain to choose from the top cards.');
  });

  it('explains ready Stacked Deck chain resolution without implying the choice exists yet', () => {
    expect(phaseGuidance('MAIN', false, undefined, true, true, {
      itemId: 'item-1',
      controllerPlayerId: 'p1',
      publicDescription: 'Stacked Deck',
      effectKey: 'STACKED_DECK_PICK_ONE',
    })).toBe('Resolve Stacked Deck on the chain to choose from the top cards.');
  });
});
