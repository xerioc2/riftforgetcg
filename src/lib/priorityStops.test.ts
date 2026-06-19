import { describe, expect, it } from 'vitest';
import type { ChainState } from '../types';
import {
  DEFAULT_PRIORITY_STOP_SETTINGS,
  hasOnlyPassChainFocus,
  normalizePriorityStopSettings,
  shouldLocalAutoPassPriority,
} from './priorityStops';

describe('priority stop settings', () => {
  it('defaults to bluff-safe manual priority', () => {
    expect(DEFAULT_PRIORITY_STOP_SETTINGS.autoPassEmptyWindows).toBe(false);
    expect(DEFAULT_PRIORITY_STOP_SETTINGS.holdOpponentTurn).toBe(true);
    expect(DEFAULT_PRIORITY_STOP_SETTINGS.holdShowdowns).toBe(true);
  });

  it('normalizes missing settings to conservative defaults', () => {
    expect(normalizePriorityStopSettings({ autoPassEmptyWindows: true })).toEqual({
      ...DEFAULT_PRIORITY_STOP_SETTINGS,
      autoPassEmptyWindows: true,
    });
  });

  it('detects empty local chain windows without exposing opponent legal actions', () => {
    expect(hasOnlyPassChainFocus(['PASS_CHAIN_FOCUS'])).toBe(true);
    expect(hasOnlyPassChainFocus(['PASS_CHAIN_FOCUS', 'PLAY_CARD'])).toBe(false);
    expect(hasOnlyPassChainFocus([])).toBe(false);
  });

  it('does not auto-pass unless the local player explicitly enables it', () => {
    expect(shouldLocalAutoPassPriority({
      settings: DEFAULT_PRIORITY_STOP_SETTINGS,
      legalActions: ['PASS_CHAIN_FOCUS'],
      chainState: chain('p1', 'p2', 'MAIN_ACTION'),
      playerId: 'p1',
      activePlayerId: 'p1',
    })).toBe(false);
  });

  it('auto-passes only local empty windows when no stop blocks it', () => {
    expect(shouldLocalAutoPassPriority({
      settings: {
        autoPassEmptyWindows: true,
        holdOpponentTurn: false,
        holdShowdowns: false,
        holdBeforeMySpells: false,
        holdBeforeOpponentSpells: false,
      },
      legalActions: ['PASS_CHAIN_FOCUS'],
      chainState: chain('p1', 'p2', 'MAIN_ACTION'),
      playerId: 'p1',
      activePlayerId: 'p1',
    })).toBe(true);
  });

  it('respects hold stops for opponent spells and showdown windows', () => {
    expect(shouldLocalAutoPassPriority({
      settings: {
        autoPassEmptyWindows: true,
        holdOpponentTurn: false,
        holdShowdowns: false,
        holdBeforeMySpells: false,
        holdBeforeOpponentSpells: true,
      },
      legalActions: ['PASS_CHAIN_FOCUS'],
      chainState: chain('p1', 'p2', 'MAIN_ACTION'),
      playerId: 'p1',
      activePlayerId: 'p1',
    })).toBe(false);

    expect(shouldLocalAutoPassPriority({
      settings: {
        autoPassEmptyWindows: true,
        holdOpponentTurn: false,
        holdShowdowns: true,
        holdBeforeMySpells: false,
        holdBeforeOpponentSpells: false,
      },
      legalActions: ['PASS_CHAIN_FOCUS'],
      chainState: chain('p1', 'p1', 'SHOWDOWN_ACTION'),
      playerId: 'p1',
      activePlayerId: 'p1',
    })).toBe(false);
  });
});

function chain(focusedPlayerId: string, topController: string, sourceContext: string): ChainState {
  return {
    chainId: 'chain-1',
    focusedPlayerId,
    readyToResolveTop: false,
    consecutivePasses: 0,
    relevantPlayerIds: ['p1', 'p2'],
    sourceContext,
    chainItems: [{
      itemId: 'item-1',
      controllerPlayerId: topController,
      publicDescription: 'Test spell',
      status: 'PENDING',
    }],
  };
}
