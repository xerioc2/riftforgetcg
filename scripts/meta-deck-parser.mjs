import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

export const CACHE_PATH = path.join(os.homedir(), '.riftforge', 'cards-cache.json');

const SET_CODE_NAMES = new Map([
  ['OGN', 'Origins'],
  ['SFD', 'Spiritforged'],
  ['UNL', 'Unleashed'],
  ['OGS', 'Proving Grounds'],
]);

const SUPPORTED_CARD_NAMES = new Set([
  'CALM RUNE',
  'CHAOS RUNE',
  'BODY RUNE',
  'ORDER RUNE',
  'VANGUARD SERGEANT',
  'DARING PORO',
  'LAURENT DUELIST',
  'NOXIAN DRUMMER',
  'LOYAL PORO',
  'LONELY PORO',
  'VANGUARD CAPTAIN',
  'STELLACORN HERDER',
  'DISARMING RAKE',
]);

const BANNED_NAMES = new Set([
  'CALLED SHOT',
  'DRAVEN, VANQUISHER',
  'FIGHT OR FLIGHT',
  'SCRAPHEAP',
  'DREAMING TREE',
  'OBELISK OF POWER',
  "REAVER'S ROW",
]);

const PARTIAL_REASONS = new Map([
  ['IRELIA - FERVENT', 'Partial: Deflect targeting tax is heuristic, and the choose/ready +1 Might trigger is not implemented yet.'],
  ['DISCIPLINE', 'Partial: alpha chain-window Reaction support exists for giving a public battlefield Unit/Champion +2 Might this turn and drawing 1. Full official any-time Reaction timing remains incomplete.'],
  ['TIDETURNER', 'Partial: Hidden foundation exists, but later hidden play timing and the on-play location swap are not implemented yet.'],
  ['GUARDIAN ANGEL', 'Partial: alpha Equip lifecycle and printed Calm equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred.'],
  ['BOOTS OF SWIFTNESS', 'Partial: alpha Equip lifecycle and printed Chaos equip payment are implemented and tested. Full official Equip timing and replacement/reattachment edge cases remain deferred.'],
  ['ABANDONED HALL', 'Partial: spell-play optional trigger needs battlefield-aware target choice before it can be scripted safely.'],
  ['ADAPTATRON', 'Partial: conquer trigger, optional gear kill, and official Buff state are not implemented yet.'],
  ['SCUTTLE CRAB', 'Partial: on-play draw and 1v1 private hand reveal Deathknell are implemented, but XP and facedown-card viewing are deferred.'],
  ['EN GARDE', 'Partial: alpha chain-window Reaction support exists for giving a friendly battlefield Unit/Champion +1 Might, or +2 if it is your only unit there. Full official any-time Reaction timing remains incomplete.'],
  ['DEFY', 'Partial: Defy can counter supported public pending spell chain items that cost no more than 4 energy and no more than 1 premium rune during the current alpha chain window. Full official Reaction timing, broad spell/ability targets, and countering counters remain deferred.'],
  ['NOT SO FAST', 'Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred.'],
  ['GUST', 'Partial: alpha chain-window Reaction support exists through Stacked Deck for returning a battlefield Unit/Champion with 3 Might or less, but full official any-time Reaction timing remains incomplete.'],
  ['STACKED DECK', 'Partial: opens the narrow alpha chain, then resolves into a private top-3 choice; official ordering and broader timing remain incomplete.'],
]);

export function normalizeName(value) {
  return String(value ?? '').trim().replace(/[’â€™]/g, "'").replace(/\s+/g, ' ').toUpperCase();
}

export function parseDeckLine(line, lineNumber = 0) {
  const raw = String(line ?? '').trim();
  if (!raw) return null;
  const match = raw.match(/^(\d+)\s+(.+?)\s+\(([A-Z0-9]+-\d+)\)$/);
  if (!match) {
    return {
      malformed: true,
      lineNumber,
      raw,
      reason: 'Expected "3 Card Name (SET-123)" format.',
    };
  }
  const [, quantity, name, sourceCardId] = match;
  return {
    malformed: false,
    lineNumber,
    raw,
    quantity: Number.parseInt(quantity, 10),
    sourceName: name.trim(),
    sourceCardId,
    sourceSetCode: sourceCardId.split('-')[0],
  };
}

export function parseDeckText(text) {
  const entries = [];
  const malformed = [];
  String(text ?? '').split(/\r?\n/).forEach((line, index) => {
    const parsed = parseDeckLine(line, index + 1);
    if (!parsed) return;
    if (parsed.malformed) malformed.push(parsed);
    else entries.push(parsed);
  });
  return { entries, malformed };
}

export function loadLocalCards(cachePath = CACHE_PATH) {
  if (!fs.existsSync(cachePath)) {
    throw new Error(`Card cache not found at ${cachePath}. Start the server once to populate it.`);
  }
  const root = JSON.parse(fs.readFileSync(cachePath, 'utf8'));
  return Object.values(root.cards ?? root);
}

export function resolveCard(entry, cards) {
  const expectedSet = SET_CODE_NAMES.get(entry.sourceSetCode) ?? null;
  const exactNameMatches = cards.filter((card) => normalizeName(card.name) === normalizeName(entry.sourceName));
  const setMatches = expectedSet
    ? exactNameMatches.filter((card) => normalizeName(card.set) === normalizeName(expectedSet))
    : [];
  const candidates = setMatches.length > 0 ? setMatches : exactNameMatches;
  if (candidates.length === 1) {
    return { status: 'RESOLVED', method: setMatches.length > 0 ? 'name-and-set' : 'name', card: candidates[0] };
  }
  if (candidates.length > 1) {
    return {
      status: 'AMBIGUOUS',
      reason: `Multiple local cards match ${entry.sourceName}${expectedSet ? ` in ${expectedSet}` : ''}.`,
      candidates: candidates.map(minCard),
    };
  }
  return {
    status: 'UNRESOLVED',
    reason: `No local RiftForge card matched ${entry.sourceName}${expectedSet ? ` in ${expectedSet}` : ''}.`,
  };
}

export function classifyCard(card) {
  if (!card) return 'Unresolved';
  const type = String(card.type ?? '').toLowerCase();
  if (type === 'legend') return 'Legend';
  if (type === 'champion') return 'Champion / MainDeck candidate';
  if (type === 'rune') return 'Rune Deck';
  if (type === 'battlefield') return 'Battlefields';
  return 'Main Deck';
}

export function supportSummary(card, resolution) {
  if (resolution.status !== 'RESOLVED') {
    return {
      status: 'NOT_AUDITED',
      reason: resolution.reason ?? 'Card could not be resolved to local RiftForge card data.',
      blocked: true,
    };
  }
  if (BANNED_NAMES.has(normalizeName(card.name))) {
    return { status: 'BANNED', reason: 'This card is banned in the current Constructed format.', blocked: true };
  }
  if (isUnsupportedAction(card)) {
    return { status: 'UNSUPPORTED', reason: "Blocked in enforced play: this card's effect is not supported yet.", blocked: true };
  }
  if (SUPPORTED_CARD_NAMES.has(normalizeName(card.name))) {
    return { status: 'SUPPORTED', reason: 'Implemented and covered by current support policy.', blocked: false };
  }
  return {
    status: 'PARTIAL',
    reason: PARTIAL_REASONS.get(normalizeName(card.name)) ?? 'Playable for alpha testing, but card-specific behavior may be incomplete.',
    blocked: false,
  };
}

export function mechanicBucket(card) {
  if (!card) return 'Unresolved / missing card data';
  const type = String(card.type ?? '').toLowerCase();
  const text = String(card.rulesText ?? '').toLowerCase();
  if (type === 'legend') return 'Legend text';
  if (type === 'champion') return 'Champion text';
  if (type === 'battlefield') return 'Battlefield effect';
  if (type === 'rune') return 'Rune/payment';
  if (type === 'gear') return text.includes('quick-draw') ? 'Equipment: Quick-Draw' : text.includes('weaponmaster') ? 'Equipment: Weaponmaster' : 'Equipment lifecycle/effect';
  if (text.includes('[reaction]') || text.includes('counter')) return 'Reaction / chain / counter';
  if (text.includes('[hidden]') || text.includes('face down') || text.includes('facedown')) return 'Hidden / facedown';
  if (text.includes('[ambush]')) return 'Ambush timing';
  if (text.includes('xp') || text.includes('[hunt') || text.includes('[level') || text.includes('buff')) return 'XP / Hunt / Level / Buff';
  if (text.includes('return') || text.includes('recall')) return 'Bounce / return';
  if (text.includes('move')) return 'Movement / location';
  if (text.includes('draw') || text.includes('look at') || text.includes('reveal') || text.includes('top')) return 'Draw / reveal / deck selection';
  if (text.includes('kill') || text.includes('destroy') || text.includes('damage')) return 'Damage / destroy';
  if (text.includes('token') || text.includes('recruit')) return 'Token creation';
  if (text.includes('when ') || text.includes('if ') || text.includes('while ')) return 'Triggered/static card text';
  return 'Basic or descriptor-only';
}

export function normalizeDeck(rawText, sourceFilename, cards) {
  const parsed = parseDeckText(rawText);
  const entries = parsed.entries.map((entry) => {
    const resolution = resolveCard(entry, cards);
    const card = resolution.card ?? null;
    return {
      sourceLine: entry.lineNumber,
      quantity: entry.quantity,
      originalName: entry.sourceName,
      sourceCardId: entry.sourceCardId,
      sourceSetCode: entry.sourceSetCode,
      section: classifyCard(card),
      resolved: resolution.status === 'RESOLVED'
        ? { status: resolution.status, method: resolution.method, card: minCard(card) }
        : resolution,
      support: supportSummary(card, resolution),
      bucket: mechanicBucket(card),
    };
  });
  return {
    sourceFilename,
    parsedAt: '2026-06-19',
    entries,
    malformedLines: parsed.malformed,
    validation: validateDeckShape(entries, parsed.malformed),
    supportSummary: summarizeSupport(entries),
  };
}

export function validateDeckShape(entries, malformedLines = []) {
  const sum = (section) => entries.filter((entry) => entry.section === section).reduce((total, entry) => total + entry.quantity, 0);
  const legendCount = sum('Legend');
  const championCandidates = sum('Champion / MainDeck candidate');
  const runeCount = sum('Rune Deck');
  const battlefieldCount = sum('Battlefields');
  const mainDeckCount = sum('Main Deck') + Math.max(0, championCandidates - 1);
  const issues = [];
  if (malformedLines.length > 0) issues.push(`${malformedLines.length} malformed line(s) need manual review.`);
  if (legendCount !== 1) issues.push(`Expected exactly 1 Legend, found ${legendCount}.`);
  if (championCandidates < 1) issues.push(`Expected at least 1 Champion candidate, found ${championCandidates}.`);
  if (runeCount !== 12) issues.push(`Expected 12 Runes, found ${runeCount}.`);
  if (battlefieldCount !== 3) issues.push(`Expected 3 Battlefields, found ${battlefieldCount}.`);
  if (mainDeckCount !== 39) issues.push(`Expected 39 Main Deck cards after one chosen Champion candidate, found ${mainDeckCount}.`);
  return {
    totalCards: entries.reduce((total, entry) => total + entry.quantity, 0),
    legendCount,
    championCandidateCount: championCandidates,
    inferredMainDeckCount: mainDeckCount,
    runeCount,
    battlefieldCount,
    validConstructedShape: issues.length === 0,
    issues,
  };
}

export function summarizeSupport(entries) {
  const counts = { SUPPORTED: 0, PARTIAL: 0, UNSUPPORTED: 0, BANNED: 0, NOT_AUDITED: 0 };
  for (const entry of entries) counts[entry.support.status] = (counts[entry.support.status] ?? 0) + 1;
  const blockers = entries.filter((entry) => ['UNSUPPORTED', 'BANNED', 'NOT_AUDITED'].includes(entry.support.status));
  return {
    uniqueCards: entries.length,
    counts,
    canUseInEnforced: blockers.length === 0,
    unresolvedCards: entries.filter((entry) => entry.resolved.status !== 'RESOLVED').map((entry) => ({
      sourceCardId: entry.sourceCardId,
      originalName: entry.originalName,
      reason: entry.resolved.reason,
    })),
    topBlockers: [...blockers, ...entries.filter((entry) => entry.support.status === 'PARTIAL')].slice(0, 5).map((entry) => ({
      cardName: entry.originalName,
      sourceCardId: entry.sourceCardId,
      status: entry.support.status,
      bucket: entry.bucket,
      reason: entry.support.reason,
    })),
  };
}

function isUnsupportedAction(card) {
  if (!card) return false;
  const type = String(card.type ?? '').toLowerCase();
  const text = String(card.rulesText ?? '').toLowerCase();
  if (type === 'gear') return !text.includes('[equip]');
  if (type !== 'spell') return false;
  const supportedFriendlyEnemyReturn = text.includes('return')
    && text.includes('friendly unit')
    && text.includes('enemy unit');
  const requiresMultipleTargets = text.includes('another unit')
    || (text.includes('a friendly unit and an enemy unit') && !supportedFriendlyEnemyReturn);
  const supportedEffect = text.includes(':rb_might:')
    || text.includes('return a unit')
    || supportedFriendlyEnemyReturn
    || text.includes('ready it')
    || text.includes('draw 1')
    || isDefy(card)
    || isNotSoFast(card)
    || isDiscipline(card)
    || isEnGarde(card)
    || isStackedDeck(card);
  return (text.includes('counter a spell') && !isDefy(card))
    || (text.includes('counter an enemy spell') && !isNotSoFast(card))
    || requiresMultipleTargets
    || !supportedEffect;
}

function isDefy(card) {
  return normalizeName(card?.name) === 'DEFY' && String(card?.rulesText ?? '').toLowerCase().includes('counter a spell');
}

function isNotSoFast(card) {
  const text = String(card?.rulesText ?? '').toLowerCase();
  return normalizeName(card?.name) === 'NOT SO FAST'
    && text.includes('counter an enemy spell or ability')
    && text.includes('friendly unit or gear');
}

function isDiscipline(card) {
  const text = String(card?.rulesText ?? '').toLowerCase();
  return normalizeName(card?.name) === 'DISCIPLINE' && text.includes('give a unit') && text.includes('+2') && text.includes('draw 1');
}

function isEnGarde(card) {
  const text = String(card?.rulesText ?? '').toLowerCase();
  return normalizeName(card?.name) === 'EN GARDE' && text.includes('friendly unit') && text.includes('+1') && text.includes('additional +1');
}

function isStackedDeck(card) {
  const text = String(card?.rulesText ?? '').toLowerCase();
  return text.includes('look at the top 3') && text.includes('put 1') && text.includes('hand') && text.includes('recycle');
}

function minCard(card) {
  return {
    id: card.id,
    name: card.name,
    type: card.type,
    set: card.set,
    rulesText: card.rulesText,
  };
}
