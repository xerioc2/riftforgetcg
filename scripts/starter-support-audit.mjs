import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import ts from 'typescript';

const ROOT = path.resolve(import.meta.dirname, '..');
const STARTER_DECKS_PATH = path.join(ROOT, 'src', 'lib', 'starterDecks.ts');
const CACHE_PATH = path.join(os.homedir(), '.riftforge', 'cards-cache.json');

const SUPPORTED_CARD_NAMES = new Set(['CALM RUNE', 'CHAOS RUNE', 'BODY RUNE', 'ORDER RUNE']);
const BANNED_NAMES = new Set([
  'CALLED SHOT',
  'DRAVEN, VANQUISHER',
  'FIGHT OR FLIGHT',
  'SCRAPHEAP',
  'DREAMING TREE',
  'OBELISK OF POWER',
  "REAVER'S ROW",
]);

function normalize(value) {
  return String(value ?? '').trim().toUpperCase().replace(/[’]/g, "'");
}

async function loadStarterDecks() {
  const source = fs.readFileSync(STARTER_DECKS_PATH, 'utf8');
  const js = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.ES2022,
      target: ts.ScriptTarget.ES2022,
      verbatimModuleSyntax: false,
    },
  }).outputText;
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(js).toString('base64')}`;
  const mod = await import(moduleUrl);
  return mod.STARTER_DECKS;
}

function loadCards() {
  if (!fs.existsSync(CACHE_PATH)) {
    throw new Error(`Card cache not found at ${CACHE_PATH}. Start the server once to populate it.`);
  }
  return Object.values(JSON.parse(fs.readFileSync(CACHE_PATH, 'utf8')));
}

function detectedKeywords(card) {
  const values = new Set(card?.keywords ?? []);
  const text = card?.rulesText ?? '';
  for (const match of text.matchAll(/\[([A-Za-z][A-Za-z ]*(?:\s+\d+)?)\]/g)) {
    values.add(match[1].replace(/\s+/g, ' ').trim());
  }
  return [...values].filter(Boolean).sort((a, b) => a.localeCompare(b));
}

function isUnsupportedAction(card) {
  if (!card) return false;
  const type = String(card.type ?? '').toLowerCase();
  const text = String(card.rulesText ?? '').toLowerCase();
  if (type === 'gear') return !text.includes('[equip]');
  if (type !== 'spell') return false;
  const isCharm = String(card.name ?? '').trim().toUpperCase() === 'CHARM'
    && text.trim() === 'move an enemy unit.';
  const requiresMultipleTargets = text.includes('another unit') || text.includes('a friendly unit and an enemy unit');
  const supportedEffect = text.includes(':rb_might:')
    || text.includes('return a unit')
    || isCharm
    || text.includes('ready it')
    || text.includes('draw 1');
  return text.includes('counter a spell')
    || text.includes('counter an enemy spell')
    || requiresMultipleTargets
    || !supportedEffect;
}

function supportSummary(card) {
  if (!card) {
    return {
      status: 'NOT_AUDITED',
      reason: 'Card data is missing.',
      blocked: false,
    };
  }
  if (BANNED_NAMES.has(normalize(card.name))) {
    return {
      status: 'BANNED',
      reason: 'This card is banned in the current Constructed format.',
      blocked: true,
    };
  }
  const blocked = isUnsupportedAction(card);
  if (blocked) {
    return {
      status: 'UNSUPPORTED',
      reason: "This card's effect is not supported in enforced play yet.",
      blocked,
    };
  }
  if (SUPPORTED_CARD_NAMES.has(normalize(card.name))) {
    return {
      status: 'SUPPORTED',
      reason: 'Implemented and covered by current support policy.',
      blocked,
    };
  }
  return {
    status: 'PARTIAL',
    reason: 'Playable for alpha testing, but card-specific behavior may be incomplete.',
    blocked,
  };
}

function bucket(card, keywords) {
  if (!card) return 'Missing card data / unresolved name';
  const type = String(card.type ?? '').toLowerCase();
  const text = String(card.rulesText ?? '').toLowerCase();
  const meaningfulKeywords = keywords.filter((keyword) => !['action', 'reaction'].includes(keyword.toLowerCase()));
  if (type === 'rune') return 'Rune/payment rules';
  if (type === 'legend') return 'Legend text';
  if (type === 'champion') return 'Champion text';
  if (type === 'battlefield') return 'Battlefield ability';
  if (type === 'gear') return 'Gear/equipment';
  if (type === 'spell') {
    if (text.includes('draw') || text.includes('look at') || text.includes('reveal') || text.includes('top') || text.includes('deck')) return 'Spell: draw/card selection';
    if (text.includes('return') || text.includes('recall')) return 'Spell: bounce/return';
    if (text.trim() === 'move an enemy unit.') return 'Spell: enemy movement';
    if (text.includes(':rb_might:') || text.includes('give a unit') || /[+-]\d/.test(text)) return 'Spell: stat/might modifier';
    if (text.includes('ready') || text.includes('exhaust')) return 'Spell: ready/exhaust';
    return 'Unsupported/unknown text pattern';
  }
  if (type === 'unit') {
    if (!text.trim()) return 'Basic unit with no special text';
    if (text.includes('when ') || text.includes('if ') || text.includes('while ') || text.includes('as ') || text.includes('at the start')) return 'Unit with triggered ability';
    if (meaningfulKeywords.length > 0) return 'Unit with keyword only';
    return 'Basic unit with no special text';
  }
  return 'Unsupported/unknown text pattern';
}

function starterRows(deck, cardsByName) {
  const entries = [
    { section: 'Legend', name: deck.legend, quantity: 1 },
    { section: 'Champion', name: deck.champion, quantity: 1 },
    ...deck.main.map((entry) => ({ section: 'Main', ...entry })),
    ...deck.runes.map((entry) => ({ section: 'Runes', ...entry })),
    ...deck.battlefields.map((name) => ({ section: 'Battlefields', name, quantity: 1 })),
  ];

  return entries.map((entry) => {
    const card = cardsByName.get(entry.name);
    const keywords = detectedKeywords(card);
    const support = supportSummary(card);
    return {
      deckId: deck.id,
      deckName: deck.name,
      section: entry.section,
      quantity: entry.quantity,
      cardName: entry.name,
      cardId: card?.id ?? 'missing',
      type: card?.type ?? 'Missing',
      status: support.status,
      reason: support.reason,
      keywords,
      rulesText: card?.rulesText ?? '',
      unsupportedActionBlocked: support.blocked,
      bucket: bucket(card, keywords),
    };
  });
}

function markdownTable(rows) {
  const escape = (value) => String(value ?? '').replaceAll('|', '\\|').replace(/\s+/g, ' ').trim();
  const columns = ['Deck', 'Card', 'Qty', 'Type', 'Status', 'Blocked', 'Bucket', 'Keywords', 'Reason', 'Rules text'];
  const body = rows.map((row) => [
    row.deckName,
    `${row.cardName} (${row.cardId})`,
    row.quantity,
    row.type,
    row.status,
    row.unsupportedActionBlocked ? 'Yes' : 'No',
    row.bucket,
    row.keywords.join(', ') || '-',
    row.reason,
    row.rulesText || '-',
  ]);
  return [
    `| ${columns.join(' | ')} |`,
    `| ${columns.map(() => '---').join(' | ')} |`,
    ...body.map((cells) => `| ${cells.map(escape).join(' | ')} |`),
  ].join('\n');
}

function bucketSummary(rows) {
  const grouped = new Map();
  for (const row of rows) {
    const bucketRows = grouped.get(row.bucket) ?? [];
    bucketRows.push(row);
    grouped.set(row.bucket, bucketRows);
  }
  return [...grouped.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([name, bucketRows]) => {
      const cards = bucketRows.map((row) => `${row.cardName} (${row.deckName})`).join(', ');
      return `- ${name}: ${bucketRows.length} entries - ${cards}`;
    })
    .join('\n');
}

const cards = loadCards();
const cardsByName = new Map();
for (const card of cards) {
  if (!cardsByName.has(card.name)) cardsByName.set(card.name, card);
}

const starterDecks = await loadStarterDecks();
const rows = starterDecks.flatMap((deck) => starterRows(deck, cardsByName));

console.log('# Starter Deck Support Audit');
console.log('');
console.log(`Generated from \`${path.relative(ROOT, STARTER_DECKS_PATH)}\` and \`~/.riftforge/cards-cache.json\`.`);
console.log('');
console.log('## Bucket Summary');
console.log('');
console.log(bucketSummary(rows));
console.log('');
console.log('## Card Audit');
console.log('');
console.log(markdownTable(rows));
