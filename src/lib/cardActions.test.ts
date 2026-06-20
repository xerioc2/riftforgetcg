import { describe, expect, it } from 'vitest';
import { canUseSupportedChainResponse, isAbandonCounterCard, isCharmMoveCard, isDefiantDanceReactionCard, isDefyCounterCard, isDisciplineReactionCard, isEnGardeReactionCard, isFlashReactionCard, isGustReactionCard, isIreliaBladeDancerActivatedAbility, isLegalTargetForMode, isNotSoFastCounterCard, isReactionCard, isSupportedChainReactionCard, isTheSyrenActivatedAbility, isZhonyasHourglassActivatedAbility, multiTargetRequirementsForCard, shouldShowRespondAction, targetModeForCard, unsupportedCardReason } from './cardActions';
import type { CardInstance, RiftCard } from '../types';

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

  it('recognizes Defiant Dance and Flash as supported alpha chain-backed Reaction helpers', () => {
    const defiantDance: RiftCard = {
      id: 'defiant-dance',
      name: 'Defiant Dance',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a unit +2 Might this turn and another unit -2 Might this turn.',
    };
    const flash: RiftCard = {
      id: 'flash',
      name: 'Flash',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Move up to 2 friendly units to base.',
    };

    expect(isDefiantDanceReactionCard(defiantDance)).toBe(true);
    expect(isSupportedChainReactionCard(defiantDance)).toBe(true);
    expect(isFlashReactionCard(flash)).toBe(true);
    expect(isSupportedChainReactionCard(flash)).toBe(true);
    expect(unsupportedCardReason(defiantDance)).toBeNull();
    expect(unsupportedCardReason(flash)).toBeNull();
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

  it('recognizes Defy, Not So Fast, and Abandon as the current supported counterspell response helpers', () => {
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
    const abandon: RiftCard = {
      id: 'abandon',
      name: 'Abandon',
      type: 'Spell',
      domains: [],
      rulesText: "[Reaction] Counter a spell. Return it to its owner's hand instead of putting it in their trash. [Predict].",
    };

    expect(isDefyCounterCard(defy)).toBe(true);
    expect(isSupportedChainReactionCard(defy)).toBe(true);
    expect(unsupportedCardReason(defy)).toBeNull();
    expect(isDefyCounterCard(notSoFast)).toBe(false);
    expect(isNotSoFastCounterCard(notSoFast)).toBe(true);
    expect(isSupportedChainReactionCard(notSoFast)).toBe(true);
    expect(unsupportedCardReason(notSoFast)).toBeNull();
    expect(isAbandonCounterCard(abandon)).toBe(true);
    expect(isSupportedChainReactionCard(abandon)).toBe(true);
    expect(unsupportedCardReason(abandon)).toBeNull();
  });

  it('recognizes Charm as the narrow alpha enemy battlefield movement helper', () => {
    const charm: RiftCard = {
      id: 'charm',
      name: 'Charm',
      type: 'Spell',
      domains: [],
      rulesText: 'Move an enemy unit.',
    };
    const enemyUnit: RiftCard = {
      id: 'enemy-unit',
      name: 'Enemy Unit',
      type: 'Unit',
      domains: [],
      power: 2,
      health: 2,
      rulesText: '',
    };
    const enemyBattlefield: CardInstance = {
      instanceId: 'enemy-battlefield',
      cardId: 'enemy-unit',
      ownerId: 'opponent',
      zone: 'battlefield',
      x: 0,
      y: 0,
      tapped: false,
      faceDown: false,
      zIndex: 0,
    };
    const enemyBase: CardInstance = { ...enemyBattlefield, instanceId: 'enemy-base', zone: 'base' };
    const friendlyBattlefield: CardInstance = { ...enemyBattlefield, instanceId: 'friendly', ownerId: 'player' };
    const faceDownEnemy: CardInstance = { ...enemyBattlefield, instanceId: 'face-down', faceDown: true };

    expect(isCharmMoveCard(charm)).toBe(true);
    expect(targetModeForCard(charm)).toBe('ENEMY_UNIT');
    expect(unsupportedCardReason(charm)).toBeNull();
    expect(isLegalTargetForMode(enemyBattlefield, enemyUnit, 'ENEMY_UNIT', 'player')).toBe(true);
    expect(isLegalTargetForMode(enemyBase, enemyUnit, 'ENEMY_UNIT', 'player')).toBe(false);
    expect(isLegalTargetForMode(friendlyBattlefield, enemyUnit, 'ENEMY_UNIT', 'player')).toBe(false);
    expect(isLegalTargetForMode(faceDownEnemy, enemyUnit, 'ENEMY_UNIT', 'player')).toBe(false);
  });

  it('recognizes The Syren as the narrow alpha activated Gear helper', () => {
    const syren: RiftCard = {
      id: 'syren',
      name: 'The Syren',
      type: 'Gear',
      domains: [],
      cost: 2,
      rulesText: ':rb_energy_1:, :rb_exhaust:: Move a friendly unit at a battlefield to its base.',
    };

    expect(isTheSyrenActivatedAbility(syren)).toBe(true);
    expect(unsupportedCardReason(syren)).toBeNull();
    expect(targetModeForCard(syren)).toBe('NONE');
  });

  it('recognizes Zhonyas Hourglass as the narrow alpha activated Gear helper', () => {
    const zhonya: RiftCard = {
      id: 'zhonya',
      name: "Zhonya's Hourglass",
      type: 'Gear',
      domains: ['CALM'],
      cost: 2,
      rulesText: "[Hidden] If a friendly unit would die, kill this instead. Heal that unit, exhaust it, and recall it.",
    };

    expect(isZhonyasHourglassActivatedAbility(zhonya)).toBe(true);
    expect(unsupportedCardReason(zhonya)).toBeNull();
    expect(targetModeForCard(zhonya)).toBe('NONE');
  });

  it('recognizes Irelia Blade Dancer as the narrow alpha Legend activated helper', () => {
    const irelia: RiftCard = {
      id: 'irelia',
      name: 'Irelia - Blade Dancer',
      type: 'Legend',
      domains: ['CALM'],
      rulesText: 'When you choose a friendly unit, you may exhaust me and pay :rb_rune_rainbow: to ready it.When you conquer, you may pay :rb_energy_1: to ready me.',
    };

    expect(isIreliaBladeDancerActivatedAbility(irelia)).toBe(true);
    expect(unsupportedCardReason(irelia)).toBeNull();
    expect(targetModeForCard(irelia)).toBe('NONE');
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

  it('marks Abandon playable only when chain play and a public spell target exist', () => {
    const abandon: RiftCard = {
      id: 'abandon',
      name: 'Abandon',
      type: 'Spell',
      domains: [],
      rulesText: "[Reaction] Counter a spell. Return it to its owner's hand instead of putting it in their trash. [Predict].",
    };

    expect(canUseSupportedChainResponse(abandon, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefyTarget: false,
      hasLegalAbandonTarget: true,
    })).toBe(true);
    expect(canUseSupportedChainResponse(abandon, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefyTarget: false,
      hasLegalAbandonTarget: false,
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

  it('marks Defiant Dance and Flash playable only with their legal target patterns', () => {
    const defiantDance: RiftCard = {
      id: 'defiant-dance',
      name: 'Defiant Dance',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a unit +2 Might this turn and another unit -2 Might this turn.',
    };
    const flash: RiftCard = {
      id: 'flash',
      name: 'Flash',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Move up to 2 friendly units to base.',
    };

    expect(canUseSupportedChainResponse(defiantDance, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefiantDanceTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(true);
    expect(canUseSupportedChainResponse(defiantDance, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalDefiantDanceTarget: false,
      hasLegalDefyTarget: false,
    })).toBe(false);
    expect(canUseSupportedChainResponse(flash, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalFlashTarget: true,
      hasLegalDefyTarget: false,
    })).toBe(true);
    expect(canUseSupportedChainResponse(flash, {
      canPlayCard: true,
      hasLegalGustTarget: false,
      hasLegalFlashTarget: false,
      hasLegalDefyTarget: false,
    })).toBe(false);
  });

  it('builds staged target requirements for Defiant Dance and Flash', () => {
    const defiantDance: RiftCard = {
      id: 'defiant-dance',
      name: 'Defiant Dance',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Give a unit +2 Might this turn and another unit -2 Might this turn.',
    };
    const flash: RiftCard = {
      id: 'flash',
      name: 'Flash',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Move up to 2 friendly units to base.',
    };

    expect(multiTargetRequirementsForCard(defiantDance).map((target) => target.role)).toEqual(['boostUnit', 'weakenUnit']);
    const flashTargets = multiTargetRequirementsForCard(flash);
    expect(flashTargets.map((target) => target.role)).toEqual(['firstFriendlyUnit', 'secondFriendlyUnit']);
    expect(flashTargets[0].optional).toBeUndefined();
    expect(flashTargets[1].optional).toBe(true);
  });

  it('shows Respond only for supported Reactions when server legal actions allow chain play', () => {
    const gust: RiftCard = {
      id: 'gust',
      name: 'Gust',
      type: 'Spell',
      domains: [],
      rulesText: "[Reaction] Return a unit at a battlefield with 3 Might or less to its owner's hand.",
    };
    const unsupportedReaction: RiftCard = {
      id: 'unsupported-reaction',
      name: 'Mystery Response',
      type: 'Spell',
      domains: [],
      rulesText: '[Reaction] Do a future thing.',
    };

    expect(shouldShowRespondAction(gust, {
      canPlayReactions: true,
      canPlayReactionCard: true,
    })).toBe(true);
    expect(shouldShowRespondAction(gust, {
      canPlayReactions: false,
      canPlayReactionCard: true,
    })).toBe(false);
    expect(shouldShowRespondAction(gust, {
      canPlayReactions: true,
      canPlayReactionCard: false,
    })).toBe(false);
    expect(shouldShowRespondAction(unsupportedReaction, {
      canPlayReactions: true,
      canPlayReactionCard: true,
    })).toBe(false);
  });

  it('treats both visual battlefield sides as the same battlefield target zone', () => {
    const unit: RiftCard = {
      id: 'unit',
      name: 'Battlefield Unit',
      type: 'Unit',
      domains: [],
      power: 2,
      health: 2,
      rulesText: '',
    };
    const playerSideUnit: CardInstance = {
      instanceId: 'player-unit',
      cardId: 'unit',
      ownerId: 'player',
      zone: 'battlefield',
      x: 900,
      y: 360,
      tapped: false,
      faceDown: false,
      zIndex: 0,
      currentHealth: 2,
    };
    const opponentSideUnit: CardInstance = {
      ...playerSideUnit,
      instanceId: 'opponent-unit',
      ownerId: 'opponent',
      x: 500,
    };

    expect(isLegalTargetForMode(playerSideUnit, unit, 'ANY_BATTLEFIELD_UNIT', 'player')).toBe(true);
    expect(isLegalTargetForMode(opponentSideUnit, unit, 'ANY_BATTLEFIELD_UNIT', 'player')).toBe(true);
    expect(isLegalTargetForMode(playerSideUnit, unit, 'FRIENDLY_UNIT', 'player')).toBe(true);
    expect(isLegalTargetForMode(opponentSideUnit, unit, 'ENEMY_UNIT', 'player')).toBe(true);
  });
});
