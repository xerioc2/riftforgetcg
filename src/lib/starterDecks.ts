import type { Deck, DeckCard, RiftCard } from '../types';

export type StarterDeckStatus = 'Fully supported' | 'Mostly supported' | 'Experimental';

export type StarterDeckSpec = {
  id: string;
  name: string;
  status: StarterDeckStatus;
  description: string;
  warnings: string[];
  legend: string;
  champion: string;
  main: Array<{ name: string; quantity: number }>;
  runes: Array<{ name: string; quantity: number }>;
  battlefields: string[];
};

export type ResolvedStarterDeck = {
  spec: StarterDeckSpec;
  deck?: Deck;
  missingCards: string[];
};

export const STARTER_DECKS: StarterDeckSpec[] = [
  {
    id: 'irelia-calm-chaos',
    name: 'Irelia Tempo',
    status: 'Experimental',
    description: 'A CALM/CHAOS starter list built around cheap tricks, evasive units, and the Irelia package.',
    warnings: ['Some spell and gear effects are still heuristic or partial in this alpha build.'],
    legend: 'Irelia - Blade Dancer',
    champion: 'Irelia - Fervent',
    main: [
      { name: 'Defy', quantity: 3 },
      { name: 'Discipline', quantity: 3 },
      { name: 'Tideturner', quantity: 3 },
      { name: 'Stellacorn Herder', quantity: 3 },
      { name: 'Guardian Angel', quantity: 3 },
      { name: 'Boots of Swiftness', quantity: 3 },
      { name: 'Defiant Dance', quantity: 3 },
      { name: 'Scuttle Crab', quantity: 3 },
      { name: 'Charm', quantity: 2 },
      { name: 'En Garde', quantity: 2 },
      { name: 'Gust', quantity: 2 },
      { name: 'Ride The Wind', quantity: 2 },
      { name: 'Stacked Deck', quantity: 2 },
      { name: 'Not So Fast', quantity: 2 },
      { name: 'Star-Crossed', quantity: 2 },
      { name: 'Adaptatron', quantity: 2 },
    ],
    runes: [
      { name: 'Calm Rune', quantity: 6 },
      { name: 'Chaos Rune', quantity: 6 },
    ],
    battlefields: ["Targon's Peak", 'Sunken Temple', 'Abandoned Hall'],
  },
  {
    id: 'fiora-body-order',
    name: 'Fiora Vanguard',
    status: 'Experimental',
    description: 'A BODY/ORDER starter list with sturdy units, efficient gear, and straightforward battlefield pressure.',
    warnings: ['Combat, gear timing, and several spell details are still under active rules work.'],
    legend: 'Fiora - Grand Duelist',
    champion: 'Fiora - Worthy',
    main: [
      { name: 'Daring Poro', quantity: 3 },
      { name: "Keeper's Verdict", quantity: 3 },
      { name: 'Spectral Matron', quantity: 3 },
      { name: 'Stalking Wolf', quantity: 3 },
      { name: 'Noxian Drummer', quantity: 3 },
      { name: 'Loyal Poro', quantity: 3 },
      { name: 'Vanguard Captain', quantity: 3 },
      { name: 'Facebreaker', quantity: 3 },
      { name: 'Vanguard Sergeant', quantity: 3 },
      { name: 'Laurent Duelist', quantity: 3 },
      { name: 'Crowd Favorite', quantity: 3 },
      { name: 'Riposte', quantity: 3 },
      { name: 'Dune Drake', quantity: 3 },
      { name: 'Dauntless Vanguard', quantity: 1 },
    ],
    runes: [
      { name: 'Body Rune', quantity: 6 },
      { name: 'Order Rune', quantity: 6 },
    ],
    battlefields: ["Aspirant's Climb", 'Hall of Legends', 'Fortified Position'],
  },
];

export function resolveStarterDeck(spec: StarterDeckSpec, cards: RiftCard[]): ResolvedStarterDeck {
  const byName = new Map<string, RiftCard>();
  for (const card of cards) {
    if (!byName.has(card.name)) byName.set(card.name, card);
  }

  const missingCards: string[] = [];
  const find = (name: string) => {
    const card = byName.get(name);
    if (!card) missingCards.push(name);
    return card;
  };

  const legend = find(spec.legend);
  const champion = find(spec.champion);
  const entries: DeckCard[] = [];

  const addEntry = (name: string, quantity: number) => {
    const card = find(name);
    if (card) entries.push({ cardId: card.id, quantity });
  };

  spec.main.forEach((entry) => addEntry(entry.name, entry.quantity));
  spec.runes.forEach((entry) => addEntry(entry.name, entry.quantity));
  spec.battlefields.forEach((name) => addEntry(name, 1));

  if (!legend || !champion || missingCards.length > 0) {
    return { spec, missingCards: [...new Set(missingCards)] };
  }

  return {
    spec,
    missingCards: [],
    deck: {
      id: `starter-${spec.id}-${Date.now()}`,
      name: spec.name,
      legendCardId: legend.id,
      championCardId: champion.id,
      cards: entries,
      updatedAt: new Date().toISOString(),
    },
  };
}
