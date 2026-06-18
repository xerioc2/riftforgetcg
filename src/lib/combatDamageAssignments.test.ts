import { describe, expect, it } from 'vitest';
import { buildCombatDamageAssignments } from './combatDamageAssignments';
import type { CardInstance, RiftCard } from '../types';

describe('buildCombatDamageAssignments', () => {
  it('does not spread partial damage when total might cannot make the next target lethal', () => {
    const cardsById = cardMap([
      unitDef('source-one', 2, 2),
      unitDef('source-two', 2, 2),
      unitDef('target-one', 1, 3),
      unitDef('target-two', 1, 5),
    ]);
    const assignments = buildCombatDamageAssignments(
      [
        card('source-one', 'p1'),
        card('source-two', 'p1'),
        card('target-one', 'p2'),
        card('target-two', 'p2'),
      ],
      cardsById,
      'p1',
      true,
    );

    expect(assignments).toEqual([
      { sourceInstanceId: 'source-one', targetInstanceId: 'target-two', amount: 2 },
      { sourceInstanceId: 'source-two', targetInstanceId: 'target-two', amount: 2 },
    ]);
  });

  it('assigns lethal to Tank before non-Tank when total damage can satisfy both', () => {
    const cardsById = cardMap([
      unitDef('source-one', 2, 2),
      unitDef('source-two', 5, 5),
      unitDef('tank', 1, 2, ['TANK']),
      unitDef('non-tank', 1, 5),
    ]);
    const assignments = buildCombatDamageAssignments(
      [
        card('source-one', 'p1'),
        card('source-two', 'p1'),
        card('non-tank', 'p2'),
        card('tank', 'p2'),
      ],
      cardsById,
      'p1',
      true,
    );

    expect(assignments).toEqual([
      { sourceInstanceId: 'source-one', targetInstanceId: 'tank', amount: 2 },
      { sourceInstanceId: 'source-two', targetInstanceId: 'non-tank', amount: 5 },
    ]);
  });

  it('returns null when the strict alpha policy has no valid assignment', () => {
    const cardsById = cardMap([
      unitDef('source-one', 2, 2),
      unitDef('source-two', 2, 2),
      unitDef('tank', 1, 2, ['TANK']),
      unitDef('non-tank', 1, 5),
    ]);
    const assignments = buildCombatDamageAssignments(
      [
        card('source-one', 'p1'),
        card('source-two', 'p1'),
        card('tank', 'p2'),
        card('non-tank', 'p2'),
      ],
      cardsById,
      'p1',
      true,
    );

    expect(assignments).toBeNull();
  });
});

function card(instanceId: string, ownerId: string): CardInstance {
  return {
    instanceId,
    cardId: instanceId,
    ownerId,
    zone: 'battlefield',
    tapped: false,
    faceDown: false,
    zIndex: 0,
    x: 0,
    y: 0,
    currentHealth: undefined,
  };
}

function unitDef(id: string, power: number, health: number, keywords: string[] = []): RiftCard {
  return {
    id,
    name: id,
    type: 'Unit',
    domains: [],
    power,
    health,
    keywords,
  };
}

function cardMap(cards: RiftCard[]) {
  return new Map(cards.map((card) => [card.id, card]));
}
