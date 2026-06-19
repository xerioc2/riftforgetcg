import { describe, expect, it } from 'vitest';
import { hiddenCardDisplayForViewer } from './hiddenCards';
import type { CardInstance, RiftCard } from '../types';

const hiddenInstance: CardInstance = {
  instanceId: 'hidden-1',
  cardId: 'tideturner',
  ownerId: 'p1',
  zone: 'hidden',
  tapped: false,
  faceDown: true,
  zIndex: 0,
  x: 0,
  y: 0,
};

const tideturner: RiftCard = {
  id: 'tideturner',
  name: 'Tideturner',
  type: 'Unit',
  rarity: 'COMMON',
  domains: ['CALM'],
  cost: 3,
  power: 2,
  health: 2,
  rulesText: '[Hidden] Hide now.',
  keywords: ['HIDDEN'],
};

describe('hiddenCardDisplayForViewer', () => {
  it('lets the owner preview and inspect their hidden card identity', () => {
    const display = hiddenCardDisplayForViewer(hiddenInstance, tideturner, 'p1');

    expect(display).toMatchObject({
      label: 'Tideturner',
      subtitle: 'Hidden - only you can see this',
      masked: false,
      canInspect: true,
      previewCard: tideturner,
    });
  });

  it('masks hidden card identity from opponents', () => {
    const display = hiddenCardDisplayForViewer(hiddenInstance, tideturner, 'p2');

    expect(display).toMatchObject({
      label: 'Hidden card',
      subtitle: 'Face-down hidden card',
      masked: true,
      canInspect: false,
      previewCard: null,
    });
    expect(JSON.stringify(display)).not.toContain('Tideturner');
  });

  it('masks hidden card identity when projection already replaced the card id', () => {
    const display = hiddenCardDisplayForViewer({ ...hiddenInstance, cardId: 'hidden' }, undefined, 'p2');

    expect(display.masked).toBe(true);
    expect(display.label).toBe('Hidden card');
    expect(display.previewCard).toBeNull();
  });

  it('shows public cards normally', () => {
    const display = hiddenCardDisplayForViewer({ ...hiddenInstance, zone: 'battlefield', faceDown: false }, tideturner, 'p2');

    expect(display.label).toBe('Tideturner');
    expect(display.masked).toBe(false);
    expect(display.canInspect).toBe(true);
  });
});
