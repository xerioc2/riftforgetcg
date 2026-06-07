import { config, getGameServerUrl } from './env';
import type { CardType, RiftCard } from '../types';

const CACHE_KEY = 'riftforge.cards.v1';
const CANDIDATE_PATHS = ['/cards'];

type UnknownRecord = Record<string, unknown>;

const asRecord = (value: unknown): UnknownRecord => (value && typeof value === 'object' ? (value as UnknownRecord) : {});

const asString = (...values: unknown[]) =>
  values.find((value): value is string => typeof value === 'string' && value.trim().length > 0)?.trim();

const asNumber = (...values: unknown[]) => {
  const found = values.find((value) => typeof value === 'number' || typeof value === 'string');
  if (found === undefined) return undefined;
  const number = Number(found);
  return Number.isFinite(number) ? number : undefined;
};

const asStringList = (...values: unknown[]) => {
  for (const value of values) {
    if (Array.isArray(value)) {
      return value.map((item) => String(item).trim()).filter(Boolean);
    }
    if (typeof value === 'string' && value.trim().length > 0) {
      return value
        .split(/[|,/]/)
        .map((item) => item.trim())
        .filter(Boolean);
    }
  }
  return [];
};

const normalizeType = (value?: string): CardType => {
  const key = value?.toLowerCase();
  if (!key) return 'Unknown';
  if (key.includes('legend')) return 'Legend';
  if (key.includes('champion')) return 'Champion';
  if (key.includes('unit') || key.includes('army')) return 'Unit';
  if (key.includes('spell')) return 'Spell';
  if (key.includes('gear') || key.includes('equipment')) return 'Gear';
  if (key.includes('rune')) return 'Rune';
  if (key.includes('battlefield')) return 'Battlefield';
  if (key.includes('token')) return 'Token';
  return 'Unknown';
};

const absolutize = (url?: string) => {
  if (!url) return undefined;
  try {
    return new URL(url, config.riftcodexApiBase).toString();
  } catch {
    return url;
  }
};

const extractCards = (payload: unknown): unknown[] => {
  if (Array.isArray(payload)) return payload;
  const record = asRecord(payload);
  for (const key of ['cards', 'data', 'results', 'items']) {
    const value = record[key];
    if (Array.isArray(value)) return value;
  }
  return [];
};

export const normalizeRiftcodexCard = (raw: unknown): RiftCard => {
  const card = asRecord(raw);
  const images = asRecord(card.images);
  const image = asRecord(card.image);
  const text = asRecord(card.text);
  const classification = asRecord(card.classification);
  const attributes = asRecord(card.attributes);
  const set = asRecord(card.set);
  const media = asRecord(card.media);

  return {
    id: asString(card.id, card.cardId, card.uuid, card.slug, card.collectorNumber, card.name) ?? crypto.randomUUID(),
    name: asString(card.name, card.title) ?? 'Unknown card',
    type: normalizeType(asString(card.supertype, classification.supertype, card.type, card.cardType, classification.type)),
    champion: asString(card.champion, card.championName, card.legend),
    domains: asStringList(card.domains, card.domain, card.colors, card.colorIdentity, classification.domain),
    cost: asNumber(card.cost, card.energyCost, card.normalCost, attributes.energy),
    premiumCost: asNumber(card.premiumCost, card.premium, card.alternateCost),
    rarity: asString(card.rarity, classification.rarity),
    set: asString(card.setName, card.expansion, set.label, set.id),
    imageUrl: absolutize(asString(card.imageUrl, card.image_url, images.full, images.large, image.url, media.image_url)),
    rulesText: asString(card.rulesText, card.oracleText, card.description, text.rules, text.plain),
    flavorText: asString(card.flavorText, text.flavor),
    power: asNumber(card.power, card.attack, attributes.power, attributes.attack),
    health: asNumber(card.health, card.defense, card.toughness, attributes.health),
    keywords: asStringList(card.keywords, card.abilities, card.keywordAbilities, classification.keywords),
  };
};

export const loadCachedCards = (): RiftCard[] => {
  try {
    const cached = localStorage.getItem(CACHE_KEY);
    return cached ? (JSON.parse(cached) as RiftCard[]) : [];
  } catch {
    return [];
  }
};

export const cacheCards = (cards: RiftCard[]) => {
  localStorage.setItem(CACHE_KEY, JSON.stringify(cards));
};

export async function fetchCardsFromRiftcodex(): Promise<RiftCard[]> {
  // Try the local server first — it proxies Riftcodex server-side, avoiding CORS
  const base = config.riftcodexApiBase.replace(/\/$/, '');
  const urls = [`${getGameServerUrl()}/api/cards`, ...CANDIDATE_PATHS.map((path) => `${base}${path}`)];
  let lastError: unknown;

  for (const url of urls) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        lastError = new Error(`${response.status} ${response.statusText}`);
        continue;
      }
      const payload = await response.json();
      const cards = extractCards(payload).map(normalizeRiftcodexCard);
      if (cards.length > 0) {
        cacheCards(cards);
        return cards;
      }
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError instanceof Error ? lastError : new Error('No Riftcodex cards returned from known endpoints.');
}
