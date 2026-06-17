import { describe, expect, it } from 'vitest';
import { canUseSupportedChainResponse, isDefyCounterCard, isDisciplineReactionCard, isEnGardeReactionCard, isGustReactionCard, isNotSoFastCounterCard, isReactionCard, isSupportedChainReactionCard, unsupportedCardReason } from './cardActions';
import type { RiftCard } from '../types';

describe('cardActions', () => {
  it('recognizes Gust as a supported chain-backed Reaction helper', () => {
    const gust: RiftCard = {
      id: 'gust',
      name: 'Gust',
      type: 'Spell',
      domains: [],
      rulesText: "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.",
    };

    expect(isReactionCard(gust)).toBe(true);
    expect(isGustReactionCard(gust)).toBe(true);
  });

  it('recognizes Discipline and En Garde as supported alpha chain-backed Reaction helpers', () => {
    const discipline: RiftCard = {
      id: 'discipline',
      name: 'Discipline',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a unit +2 Might this turn. Draw 1.',
    };
    const enGarde: RiftCard = {
      id: 'en-garde',
      name: 'En Garde',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a friendly unit +1 Might this turn, then an additional +1 Might this turn if it is the only unit you control there.',
    };

    expect(isDisciplineReactionCard(discipline)).toBe(true);
    expect(isSupportedChainReactionCard(discipline)).toBe(true);
    expect(isEnGardeReactionCard(enGarde)).toBe(true);
    expect(isSupportedChainReactionCard(enGarde)).toBe(true);
  });

  it('does not treat unsupported Reaction cards as Gust-playable responses', () => {
    const defy: RiftCard = {
      id: 'defy',
      name: 'Defy',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter an enemy spell.',
    };

    expect(isReactionCard(defy)).toBe(true);
    expect(isGustReactionCard(defy)).toBe(false);
  });

  it('recognizes Defy and Not So Fast as the current supported counterspell response helpers', () => {
    const defy: RiftCard = {
      id: 'defy',
      name: 'Defy',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter a spell that costs no more than 4 and no more than 1.',
    };
    const notSoFast: RiftCard = {
      id: 'not-so-fast',
      name: 'Not So Fast',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.',
    };

    expect(isDefyCounterCard(defy)).toBe(true);
    expect(isSupportedChainReactionCard(defy)).toBe(true);
    expect(unsupportedCardReason(defy)).toBeNull();
    expect(isDefyCounterCard(notSoFast)).toBe(false);
    expect(isNotSoFastCounterCard(notSoFast)).toBe(true);
    expect(isSupportedChainReactionCard(notSoFast)).toBe(true);
    expect(unsupportedCardReason(notSoFast)).toBeNull();
  });

  it('marks Gust playable only when chain play and a legal Gust target are available', () => {
    const gust: RiftCard = {
      id: 'gust',
      name: 'Gust',
      type: 'Spell',
      domains: [],
      rulesText: "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.",
    };

    expect(canUseSupportedChainResponse(gust, {
      canPlayCard: true,
      hasLegalGustTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(true);
    expect(canUseSupportedChainResponse(gust, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefyTarget: true,
    })).toBe(false);
    expect(canUseSupportedChainResponse(gust, {
      canPlayCard: false,
      hasLegalGustTarget: true,
      hasLegalDefyTarget: true,
    })).toBe(false);
  });

  it('marks Defy playable only when chain play and a legal chain target are available', () => {
    const defy: RiftCard = {
      id: 'defy',
      name: 'Defy',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter a spell that costs no more than 4 and no more than 1.',
    };
    const notSoFast: RiftCard = {
      id: 'not-so-fast',
      name: 'Not So Fast',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.',
    };

    expect(canUseSupportedChainResponse(defy, {
      canPlayCard: true,
      hasLegalGustTarget: true,
      hasLegalDefyTarget: true,
    })).toBe(true);
    expect(canUseSupportedChainResponse(defy, {
      canPlayCard: true,
      hasLegalGustTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(false);
    expect(canUseSupportedChainResponse(notSoFast, {
      canPlayCard: true,
      hasLegalGustTarget: true,
      hasLegalDefyTarget: true,
      hasLegalNotSoFastTarget: false,
    })).toBe(false);
  });

  it('marks Not So Fast playable only when chain play and a friendly-targeted enemy spell exist', () => {
    const notSoFast: RiftCard = {
      id: 'not-so-fast',
      name: 'Not So Fast',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Counter an enemy spell or ability that chooses a friendly unit or gear.',
    };

    expect(canUseSupportedChainResponse(notSoFast, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefyTarget: false,
      hasLegalNotSoFastTarget: true,
    })).toBe(true);
    expect(canUseSupportedChainResponse(notSoFast, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefyTarget: false,
      hasLegalNotSoFastTarget: false,
    })).toBe(false);
  });

  it('marks Discipline and En Garde playable only when chain play and legal board targets exist', () => {
    const discipline: RiftCard = {
      id: 'discipline',
      name: 'Discipline',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a unit +2 Might this turn. Draw 1.',
    };
    const enGarde: RiftCard = {
      id: 'en-garde',
      name: 'En Garde',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a friendly unit +1 Might this turn, then an additional +1 Might this turn if it is the only unit you control there.',
    };

    expect(canUseSupportedChainResponse(discipline, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDisciplineTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(true);
    expect(canUseSupportedChainResponse(discipline, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDisciplineTarget: false,
      hasLegalDefyTarget: false,
    })).toBe(false);
    expect(canUseSupportedChainResponse(enGarde, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalEnGardeTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(true);
    expect(canUseSupportedChainResponse(enGarde, {
      canPlayCard: false,
      hasLegalGustTarget: false,
      hasLegalEnGardeTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(false);
  });
});
