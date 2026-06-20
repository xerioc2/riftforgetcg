import type { Deck, DeckCard, RiftCard } from '../types';
import azirUploaded from '../../decks/meta/normalized/azir_wins_lille_regional_qualifier.json';
import dianaUploaded from '../../decks/meta/normalized/diana_wins_s3_suzhou_city_challenge.json';
import dravenUploaded from '../../decks/meta/normalized/draven_wins_new_zealand_10k_open.json';
import fioraUploaded from '../../decks/meta/normalized/fiora_wins_s3_beijing_city_challenge.json';
import ireliaUploaded from '../../decks/meta/normalized/irelia_wins_s3_shanghai_city_challenge.json';
import leblancUploaded from '../../decks/meta/normalized/leblanc_wins_s3_zhongshan_city_challenge.json';
import sivirUploaded from '../../decks/meta/normalized/sivir_2nd_at_sydney_regional_qualifier.json';

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

type UploadedMetaDeck = {
  entries: Array<{
    quantity: number;
    originalName: string;
    section: string;
    resolved?: {
      card?: {
        name: string;
        type: RiftCard['type'];
      };
    };
  }>;
  supportSummary?: {
    canUseInEnforced?: boolean;
  };
};

export type ResolvedStarterDeck = {
  spec: StarterDeckSpec;
  deck?: Deck;
  missingCards: string[];
};

const entryCardName = (entry: UploadedMetaDeck['entries'][number]) => entry.resolved?.card?.name ?? entry.originalName;
const entryCardType = (entry: UploadedMetaDeck['entries'][number]) => entry.resolved?.card?.type ?? 'Unknown';

function uploadedMetaDeckPreset(
  deck: UploadedMetaDeck,
  options: {
    id: string;
    name: string;
    status: StarterDeckStatus;
    description: string;
    champion: string;
    warnings?: string[];
  },
): StarterDeckSpec {
  const legend = deck.entries.find((entry) => entry.section === 'Legend');
  const main: StarterDeckSpec['main'] = [];
  const runes: StarterDeckSpec['runes'] = [];
  const battlefields: string[] = [];

  for (const entry of deck.entries) {
    const name = entryCardName(entry);
    const type = entryCardType(entry);
    let quantity = entry.quantity;

    if (name === entryCardName(legend ?? entry)) continue;
    if (name === options.champion) {
      quantity -= 1;
      if (quantity <= 0) continue;
    }

    if (type === 'Rune') {
      runes.push({ name, quantity });
    } else if (type === 'Battlefield') {
      for (let i = 0; i < quantity; i += 1) battlefields.push(name);
    } else {
      main.push({ name, quantity });
    }
  }

  return {
    id: options.id,
    name: options.name,
    status: options.status,
    description: options.description,
    warnings:
      options.warnings ??
      (deck.supportSummary?.canUseInEnforced
        ? ['Enforced-playable alpha deck; many Partial card behaviors remain.']
        : ['Contains Unsupported/Partial cards; use as an audit/playtest target, not enforced-ready yet.']),
    legend: legend ? entryCardName(legend) : '',
    champion: options.champion,
    main,
    runes,
    battlefields,
  };
}

const UPLOADED_META_DECKS: StarterDeckSpec[] = [
  uploadedMetaDeckPreset(ireliaUploaded as UploadedMetaDeck, {
    id: 'uploaded-irelia-shanghai',
    name: 'Irelia Uploaded Meta - Playtest',
    status: 'Mostly supported',
    description: 'Exact uploaded S3 Shanghai City Challenge list. This is the current default playtest deck and the first enforced-playable uploaded meta deck.',
    champion: 'Irelia - Fervent',
    warnings: ['Enforced-playable alpha deck; many Partial card behaviors remain. Not rules-complete or golden/reference-correct.'],
  }),
  uploadedMetaDeckPreset(dianaUploaded as UploadedMetaDeck, {
    id: 'uploaded-diana-suzhou',
    name: 'Diana Uploaded Meta - Playtest',
    status: 'Mostly supported',
    description: 'Exact uploaded S3 Suzhou City Challenge list. Enforced-playable reviewer-priority interaction deck for alpha bot testing.',
    champion: 'Diana - Lunari',
    warnings: ['Enforced-playable alpha deck; many Partial card behaviors remain. Not rules-complete or golden/reference-correct.'],
  }),
  uploadedMetaDeckPreset(leblancUploaded as UploadedMetaDeck, {
    id: 'uploaded-leblanc-zhongshan',
    name: 'LeBlanc Uploaded Meta - Zhongshan',
    status: 'Experimental',
    description: 'Exact uploaded S3 Zhongshan City Challenge list. Audit preset with unsupported effects still blocked in enforced play.',
    champion: 'LeBlanc - Fragmented',
  }),
  uploadedMetaDeckPreset(azirUploaded as UploadedMetaDeck, {
    id: 'uploaded-azir-lille',
    name: 'Azir Uploaded Meta - Lille',
    status: 'Experimental',
    description: 'Exact uploaded Lille Regional Qualifier winning list. Audit preset with unsupported effects still blocked in enforced play.',
    champion: 'Azir - Sovereign',
  }),
  uploadedMetaDeckPreset(sivirUploaded as UploadedMetaDeck, {
    id: 'uploaded-sivir-sydney',
    name: 'Sivir Uploaded Meta - Sydney',
    status: 'Experimental',
    description: 'Exact uploaded Sydney Regional Qualifier second-place list. Audit preset with unsupported effects still blocked in enforced play.',
    champion: 'Sivir - Mercenary',
  }),
  uploadedMetaDeckPreset(fioraUploaded as UploadedMetaDeck, {
    id: 'uploaded-fiora-beijing',
    name: 'Fiora Uploaded Meta - Beijing',
    status: 'Experimental',
    description: 'Exact uploaded S3 Beijing City Challenge winning list. Audit preset with unsupported effects still blocked in enforced play.',
    champion: 'Fiora - Victorious',
  }),
  uploadedMetaDeckPreset(dravenUploaded as UploadedMetaDeck, {
    id: 'uploaded-draven-new-zealand',
    name: 'Draven Uploaded Meta - New Zealand',
    status: 'Experimental',
    description: 'Exact uploaded New Zealand 10k Open winning list. Audit preset with unsupported effects still blocked in enforced play.',
    champion: 'Draven - Showboat',
  }),
];

const STARTER_PLAYTEST_DECKS: StarterDeckSpec[] = [
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

export const STARTER_DECKS: StarterDeckSpec[] = [...UPLOADED_META_DECKS, ...STARTER_PLAYTEST_DECKS];

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
