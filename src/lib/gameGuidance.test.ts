import { describe, expect, it } from 'vitest';
import { legalActionHint, phaseGuidance } from './gameGuidance';

describe('gameGuidance', () => {
  it('explains that Stacked Deck creates its private choice after chain responses resolve', () => {
    expect(phaseGuidance('MAIN', false, undefined, true, false, {
      itemId: 'item-1',
      controllerPlayerId: 'p1',
      sourceCardName: 'Stacked Deck',
      effectKey: 'STACKED_DECK_PICK_ONE',
    })).toBe('Stacked Deck is waiting on priority/chain responses. Resolve the chain to choose from the top cards.');
  });

  it('explains ready Stacked Deck chain resolution without implying the choice exists yet', () => {
    expect(phaseGuidance('MAIN', false, undefined, true, true, {
      itemId: 'item-1',
      controllerPlayerId: 'p1',
      publicDescription: 'Stacked Deck',
      effectKey: 'STACKED_DECK_PICK_ONE',
    })).toBe('Resolve Stacked Deck on the chain to choose from the top cards.');
  });

  it('explains that countered and fizzled items are cleared from the chain', () => {
    expect(phaseGuidance('MAIN', false, undefined, true, true, {
      itemId: 'item-1',
      controllerPlayerId: 'p1',
      publicDescription: 'Stacked Deck',
      status: 'COUNTERED',
    })).toBe('Countered items are skipped when they reach the top of the chain.');
    expect(phaseGuidance('MAIN', false, undefined, true, true, {
      itemId: 'item-2',
      controllerPlayerId: 'p1',
      publicDescription: 'Gust',
      status: 'FIZZLED',
    })).toBe('Fizzled items are cleared when they reach the top of the chain.');
  });

  it('uses server legal actions for chain pass and resolve hints', () => {
    const playerOptions = { isPlayer: true, isMyTurn: true };

    expect(legalActionHint(['PASS_CHAIN_FOCUS'], playerOptions)).toBe('You can pass priority.');
    expect(legalActionHint(['PASS_CHAIN_FOCUS', 'PLAY_CARD'], playerOptions)).toBe('You may respond or pass priority.');
    expect(legalActionHint(['RESOLVE_CHAIN_TOP'], playerOptions)).toBe('You can resolve the top chain item.');
    expect(legalActionHint([], playerOptions)).toBe('No server-approved actions are available right now.');
  });

  it('describes showdown play-card windows as supporting Actions and Reactions', () => {
    const playerOptions = { isPlayer: true, isMyTurn: true };

    expect(phaseGuidance('MAIN', true, 'ACTION_WINDOW', false, false)).toBe(
      'Showdown focus window. The focused player may play supported Action or Reaction cards, or pass focus.',
    );
    expect(legalActionHint(['PASS_SHOWDOWN_FOCUS', 'PLAY_CARD'], playerOptions)).toBe(
      'You may play a supported Action or Reaction card, or pass showdown focus.',
    );
  });
});
