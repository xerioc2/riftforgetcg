import { describe, expect, it } from 'vitest';
import {
  chainItemDisplayLabel,
  chainItemStatusLabel,
  chainItemTargetSummary,
  chainItemsTopToBottom,
} from './PhaseBar';
import type { ChainItem } from '../types';

describe('PhaseBar chain helpers', () => {
  it('orders chain items top-to-bottom', () => {
    const bottom = item('item-1', 'Stacked Deck', 'STACKED_DECK_PICK_ONE');
    const top = item('item-2', 'Gust', 'GUST_RETURN');

    expect(chainItemsTopToBottom([bottom, top]).map((chainItem) => chainItem.itemId)).toEqual(['item-2', 'item-1']);
  });

  it('uses public-safe labels and masks private items without source info', () => {
    expect(chainItemDisplayLabel(item('item-1', 'Stacked Deck', 'STACKED_DECK_PICK_ONE'))).toBe('Stacked Deck');
    expect(chainItemDisplayLabel({ itemId: 'masked', controllerPlayerId: 'p1' })).toBe('Hidden chain item');
  });

  it('summarizes Stacked Deck, Gust, and Defy targets without needing private fields', () => {
    const stacked = item('item-1', 'Stacked Deck', 'STACKED_DECK_PICK_ONE');
    const gust = { ...item('item-2', 'Gust', 'GUST_RETURN'), targetInstanceIds: ['unit-1'] };
    const defy = { ...item('item-3', 'Defy', 'DEFY_COUNTER'), targetInstanceIds: ['item-1'] };

    expect(chainItemTargetSummary(stacked, [stacked], {})).toBe('Target: none');
    expect(chainItemTargetSummary(gust, [stacked, gust], { 'unit-1': 'Target Unit' })).toBe('Target: Target Unit');
    expect(chainItemTargetSummary(defy, [stacked, gust, defy], {})).toBe('Target: Stacked Deck');
  });

  it('shows lifecycle status only for non-pending items', () => {
    expect(chainItemStatusLabel(item('item-1', 'Stacked Deck', 'STACKED_DECK_PICK_ONE'))).toBeUndefined();
    expect(chainItemStatusLabel({ ...item('item-2', 'Stacked Deck', 'STACKED_DECK_PICK_ONE'), status: 'COUNTERED' })).toBe('countered');
  });
});

function item(itemId: string, publicDescription: string, effectKey: string): ChainItem {
  return {
    itemId,
    controllerPlayerId: 'p1',
    publicDescription,
    effectKey,
    status: 'PENDING',
  };
}
