import { describe, expect, it } from 'vitest';
import { buildDebugInfo } from './debugInfo';
import type { LiveGameState } from '../types';

describe('buildDebugInfo', () => {
  it('includes safe chain, choice, legal-action, and client waiting diagnostics', () => {
    const state: LiveGameState = {
      roomCode: 'ROOM',
      currentPhase: 'MAIN',
      gameMode: 'ENFORCED',
      activePlayerId: 'bot-player-riftbot',
      turnNumber: 3,
      activeShowdown: null,
      battlefieldController: { 'bf-1': 'p1' },
      chainState: {
        chainId: 'chain-1',
        chainItems: [{ itemId: 'item-1', controllerPlayerId: 'p1' }],
        relevantPlayerIds: ['p1', 'bot-player-riftbot'],
        focusedPlayerId: 'bot-player-riftbot',
        readyToResolveTop: false,
      },
      pendingChoice: {
        choiceId: 'choice-1',
        playerId: 'p1',
        type: 'TOP_DECK_PICK_ONE',
        prompt: 'Private prompt',
        options: [],
        cardOptions: [{ optionId: 'secret-option', cardId: 'private-card', name: 'Private Card', originalIndex: 0 }],
      },
      legalActions: ['PLAY_CARD', 'PASS_PHASE'],
      cards: [
        {
          instanceId: 'public-unit',
          cardId: 'unit-card',
          ownerId: 'p1',
          zone: 'battlefield',
          battlefieldLocationId: 'bf-1',
          attachedToInstanceId: 'gear-1',
          printedMight: 3,
          printedHealth: 4,
          effectiveMight: 5,
          effectiveMaxHealth: 6,
          currentHealth: 4,
          markedDamage: 2,
          statModifierLabels: ['Test Gear: +2 Might', 'Test Gear: +2 max HP'],
          tapped: false,
          faceDown: false,
          zIndex: 0,
          x: 0,
          y: 0,
        },
        {
          instanceId: 'hidden-card',
          cardId: 'secret-card',
          ownerId: 'p1',
          zone: 'hidden',
          tapped: false,
          faceDown: true,
          zIndex: 0,
          x: 0,
          y: 0,
        },
        {
          instanceId: 'hand-card',
          cardId: 'private-hand-card',
          ownerId: 'p1',
          zone: 'hand',
          tapped: false,
          faceDown: false,
          zIndex: 0,
          x: 0,
          y: 0,
        },
      ],
      runes: [
        {
          instanceId: 'rune-1',
          cardId: 'calm-rune',
          ownerId: 'p1',
          tapped: true,
          normalEnergy: 1,
          premiumEnergy: 1,
        },
      ],
      players: [
        { userId: 'p1', name: 'Human', score: 0, selectedBattlefieldId: 'sunken-temple' },
        { userId: 'bot-player-riftbot', name: 'RiftBot', score: 0, selectedBattlefieldId: 'training-grounds' },
      ],
      log: [],
      updatedAt: 'now',
    };

    const debug = buildDebugInfo({
      roomCode: 'ROOM',
      state,
      player: { id: 'p1', name: 'Human' },
      lastError: 'Action failed',
      serverUrl: 'http://localhost:8080',
      appVersion: '0.1.0',
      buildTag: 'abc123',
      buildDate: '2026-06-15T00:00:00Z',
      awaitingServerUpdate: true,
      lastSubmittedActionType: 'PLAY_CARD',
      lastActionFailureMessage: 'Reaction timing is not implemented yet.',
    });

    expect(debug.chainState).toEqual({
      active: true,
      itemCount: 1,
      focusedPlayerId: 'bot-player-riftbot',
      readyToResolveTop: false,
    });
    expect(debug.pendingChoice).toEqual({
      active: true,
      type: 'TOP_DECK_PICK_ONE',
      playerId: 'p1',
    });
    expect(debug.legalActions).toEqual(['PLAY_CARD', 'PASS_PHASE']);
    expect(debug.activeShowdownDetails).toEqual({
      locationId: null,
      step: null,
      attackingPlayerId: null,
      focusedPlayerId: null,
      readyToResolve: false,
    });
    expect(debug.battlefieldController).toEqual({ 'bf-1': 'p1' });
    expect(debug.selectedBattlefields).toEqual([
      { playerId: 'p1', selectedBattlefieldId: 'sunken-temple' },
      { playerId: 'bot-player-riftbot', selectedBattlefieldId: 'training-grounds' },
    ]);
    expect(debug.publicRunes).toEqual([
      {
        instanceId: 'rune-1',
        cardId: 'calm-rune',
        ownerId: 'p1',
        tapped: true,
        normalEnergy: 1,
        premiumEnergy: 1,
      },
    ]);
    expect(debug.publicCards).toEqual([
      {
        instanceId: 'public-unit',
        cardId: 'unit-card',
        ownerId: 'p1',
        zone: 'battlefield',
        battlefieldLocationId: 'bf-1',
        attachedToInstanceId: 'gear-1',
        faceDown: false,
        printedMight: 3,
        printedHealth: 4,
        effectiveMight: 5,
        effectiveMaxHealth: 6,
        currentHealth: 4,
        markedDamage: 2,
        statModifierLabels: ['Test Gear: +2 Might', 'Test Gear: +2 max HP'],
      },
    ]);
    expect(debug.cardZoneCounts).toEqual({
      'p1:battlefield': 1,
      'p1:hidden': 1,
      'p1:hand': 1,
    });
    expect(debug.awaitingServerUpdate).toBe(true);
    expect(debug.lastSubmittedActionType).toBe('PLAY_CARD');
    expect(debug.lastActionFailureMessage).toBe('Reaction timing is not implemented yet.');
    expect(JSON.stringify(debug)).not.toContain('Private Card');
    expect(JSON.stringify(debug)).not.toContain('private-card');
    expect(JSON.stringify(debug)).not.toContain('secret-card');
    expect(JSON.stringify(debug)).not.toContain('private-hand-card');
    expect(JSON.stringify(debug)).not.toContain('runeDeckPool');
  });
});
