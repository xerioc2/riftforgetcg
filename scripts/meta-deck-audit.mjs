import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const CAPTURED_DATE = '2026-06-19';
const CACHE_PATH = path.join(os.homedir(), '.riftforge', 'cards-cache.json');
const DOTGG_DECK_API = 'https://api.dotgg.gg/cgfw/getdeck?game=riftbound';
const DOTGG_CARD_API = 'https://api.dotgg.gg/cgfw/getcards?game=riftbound&mode=indexed';

const GUIDE_SOURCES = [
  {
    key: 'irelia',
    label: 'Irelia',
    legend: 'Irelia, Blade Dancer',
    priority: 'Finish current/Irelia support slice',
    guideUrl: 'https://riftbound.gg/irelia-blade-dancer-guide/',
  },
  {
    key: 'diana',
    label: 'Diana',
    legend: 'Diana, Scorn of the Moon',
    priority: 'Reviewer-prioritized next interaction deck',
    guideUrl: 'https://riftbound.gg/diana-scorn-of-the-moon-guide/',
  },
  {
    key: 'master-yi',
    label: 'Master Yi',
    legend: 'Master Yi, Wuju Bladesman',
    priority: 'Raw meta leader; audit after gameplay notes',
    guideUrl: 'https://riftbound.gg/master-yi-wuju-bladesman-guide/',
  },
  {
    key: 'azir',
    label: 'Azir',
    legend: 'Azir, Emperor of the Sands',
    priority: 'Later raw-meta audit',
    guideUrl: 'https://riftbound.gg/azir-emperor-of-the-sands-guide/',
  },
  {
    key: 'sivir',
    label: 'Sivir',
    legend: 'Sivir, Battle Mistress',
    priority: 'Additional guide-sourced archetype',
    guideUrl: 'https://riftbound.gg/sivir-battle-mistress-guide/',
  },
  {
    key: 'vex',
    label: 'Vex',
    legend: 'Vex, Gloomist',
    priority: 'Later raw-meta audit',
    guideUrl: 'https://riftbound.gg/vex-gloomist-guide/',
  },
  {
    key: 'leblanc',
    label: 'LeBlanc',
    legend: 'LeBlanc, Deceiver',
    priority: 'Later raw-meta audit',
    guideUrl: 'https://riftbound.gg/leblanc-deceiver-guide/',
  },
  {
    key: 'annie',
    label: 'Annie',
    legend: 'Annie, Dark Child',
    priority: 'Aurora shell candidate with Miss Fortune',
    guideUrl: 'https://riftbound.gg/annie-dark-child-guide/',
  },
];

const META_STATS = {
  'master-yi': { share: '9%', winRate: '60%', decks: '1710' },
  irelia: { share: '8%', winRate: '54%', decks: '1637' },
  diana: { share: '7%', winRate: '54%', decks: '1438' },
  leblanc: { share: '7%', winRate: '52%', decks: '1376' },
  vex: { share: '5%', winRate: '46%', decks: '971' },
  azir: { share: '4%', winRate: '51%', decks: '728' },
  sivir: { share: 'n/a', winRate: 'n/a', decks: 'n/a' },
  annie: { share: 'n/a', winRate: 'n/a', decks: 'n/a' },
};

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
  ['DEFIANT DANCE', 'Partial: alpha chain-window Reaction support exists for giving one public battlefield Unit/Champion +2 Might and another public battlefield Unit/Champion -2 Might this turn. Full official any-time Reaction timing remains incomplete.'],
  ['FLASH', 'Partial: alpha chain-window Reaction support exists for moving up to two friendly battlefield Unit/Champion cards to Base. Full official any-time Reaction timing remains incomplete.'],
  ['CHARM', 'Partial: alpha support moves one enemy public battlefield Unit/Champion to Base. Broader official movement choices, control/location edge cases, and non-battlefield destinations remain deferred.'],
  ['DEFY', 'Partial: Defy can counter supported public pending spell chain items that cost no more than 4 energy and no more than 1 premium rune during the current alpha chain window. Full official Reaction timing, broad spell/ability targets, and countering counters remain deferred.'],
  ['NOT SO FAST', 'Partial: Not So Fast can counter a supported public pending enemy spell chain item only when that item chooses your friendly Unit/Champion Unit or Gear. Ability-chain targets, broad official Reaction timing, and countering counters remain deferred.'],
  ['GUST', 'Partial: alpha chain-window Reaction support exists through Stacked Deck for returning a battlefield Unit/Champion with 3 Might or less, but full official any-time Reaction timing remains incomplete.'],
  ['STACKED DECK', 'Partial: opens the narrow alpha chain, then resolves into a private top-3 choice; official ordering and broader timing remain incomplete.'],
]);

function normalize(value) {
  return String(value ?? '')
    .trim()
    .replace(/[’â€™]/g, "'")
    .replace(/\s+/g, ' ')
    .toUpperCase();
}

function variantBaseName(value) {
  return String(value ?? '')
    .replace(/\s+\((alternate art|alt art|overnumbered|signature|promo|foil)\)$/i, '')
    .replace(/\s+-\s+(starter|champion deck)$/i, '')
    .trim();
}

function cleanText(value) {
  return String(value ?? '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .replace(/&amp;/g, '&')
    .replace(/\r/g, '')
    .trim();
}

function slugify(value) {
  return String(value ?? '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

async function fetchText(url) {
  const response = await fetch(url, { headers: { 'user-agent': 'RiftForge meta deck audit' } });
  if (!response.ok) throw new Error(`GET ${url} failed: ${response.status} ${response.statusText}`);
  return response.text();
}

async function fetchJson(url) {
  return JSON.parse(await fetchText(url));
}

function extractDeckSlugs(html) {
  return [...new Set([...html.matchAll(/data-deck="([^"]+)"/g)].map((match) => match[1]))];
}

function decodeIndexedCards(payload) {
  if (!Array.isArray(payload?.names) || !Array.isArray(payload?.data)) {
    throw new Error('Unexpected DotGG card catalog shape.');
  }
  return payload.data.map((row) => Object.fromEntries(payload.names.map((name, index) => [name, row[index]])));
}

function loadLocalCards() {
  if (!fs.existsSync(CACHE_PATH)) {
    throw new Error(`Card cache not found at ${CACHE_PATH}. Start the server once to populate it.`);
  }
  const root = JSON.parse(fs.readFileSync(CACHE_PATH, 'utf8'));
  return Object.values(root.cards ?? root);
}

function byNormalizedName(cards) {
  const exact = new Map();
  const base = new Map();
  for (const card of cards) {
    const name = card.name;
    if (!name) continue;
    pushMap(exact, normalize(name), card);
    pushMap(base, normalize(variantBaseName(name)), card);
  }
  return { exact, base };
}

function pushMap(map, key, value) {
  const values = map.get(key) ?? [];
  values.push(value);
  map.set(key, values);
}

function uniqueById(values) {
  const seen = new Set();
  return values.filter((value) => {
    const id = value.id ?? value.slug ?? value.name;
    if (seen.has(id)) return false;
    seen.add(id);
    return true;
  });
}

function resolveLocal(sourceCard, localByName) {
  const sourceName = sourceCard?.name;
  if (!sourceName) return { status: 'UNRESOLVED', reason: 'DotGG card metadata did not include a name.' };

  const exact = uniqueById(localByName.exact.get(normalize(sourceName)) ?? []);
  if (exact.length === 1) return { status: 'RESOLVED', card: exact[0], method: 'exact-name' };
  const exactSet = filterBySourceSet(exact, sourceCard);
  if (exactSet.length === 1) return { status: 'RESOLVED', card: exactSet[0], method: 'exact-name-and-set' };
  if (exact.length > 1) return { status: 'AMBIGUOUS', reason: `Multiple local cards match exact name "${sourceName}".`, candidates: exact.map(minCard) };

  const base = uniqueById(localByName.base.get(normalize(variantBaseName(sourceName))) ?? []);
  if (base.length === 1) return { status: 'RESOLVED', card: base[0], method: 'base-name' };
  const baseSet = filterBySourceSet(base, sourceCard);
  if (baseSet.length === 1) return { status: 'RESOLVED', card: baseSet[0], method: 'base-name-and-set' };
  if (base.length > 1) return { status: 'AMBIGUOUS', reason: `Multiple local cards match base name "${variantBaseName(sourceName)}".`, candidates: base.map(minCard) };

  return { status: 'UNRESOLVED', reason: `No local RiftForge card matched "${sourceName}".` };
}

function filterBySourceSet(candidates, sourceCard) {
  const sourceSet = normalize(sourceCard?.setName);
  if (!sourceSet) return [];
  return candidates.filter((card) => normalize(card.set) === sourceSet);
}

function minCard(card) {
  return { id: card.id, name: card.name, type: card.type, set: card.set };
}

function sourceCardDefinition(dotggCard) {
  if (!dotggCard) return null;
  return {
    id: dotggCard.id,
    name: dotggCard.name,
    type: sourceType(dotggCard),
    supertype: dotggCard.supertype ?? null,
    setName: dotggCard.set_name ?? null,
    cost: dotggCard.cost ?? null,
    premiumCost: dotggCard.alt_cost ?? dotggCard.premium_cost ?? null,
    domains: dotggCard.color ? String(dotggCard.color).split(',').map((value) => value.trim()).filter(Boolean) : [],
    rulesText: cleanText(dotggCard.effect),
    imageUrl: dotggCard.image ?? null,
    banned: dotggCard.banned === true || dotggCard.banned === 1 || dotggCard.banned === '1',
  };
}

function sourceType(dotggCard) {
  if (String(dotggCard.supertype ?? '').toLowerCase() === 'champion') return 'Champion';
  return dotggCard.type ?? null;
}

function detectedKeywords(card) {
  const values = new Set(card?.keywords ?? []);
  const text = card?.rulesText ?? card?.effect ?? '';
  for (const match of String(text).matchAll(/\[([A-Za-z][A-Za-z -]*(?:\s+\d+)?)\]/g)) {
    values.add(match[1].replace(/\s+/g, ' ').trim());
  }
  return [...values].filter(Boolean).sort((a, b) => a.localeCompare(b));
}

function isDefy(card) {
  return normalize(card?.name) === 'DEFY' && textOf(card).includes('counter a spell');
}

function isNotSoFast(card) {
  const text = textOf(card);
  return normalize(card?.name) === 'NOT SO FAST'
    && text.includes('counter an enemy spell or ability')
    && text.includes('friendly unit or gear');
}

function isDiscipline(card) {
  const text = textOf(card);
  return normalize(card?.name) === 'DISCIPLINE' && text.includes('give a unit') && text.includes('+2') && text.includes('draw 1');
}

function isEnGarde(card) {
  const text = textOf(card);
  return normalize(card?.name) === 'EN GARDE' && text.includes('friendly unit') && text.includes('+1') && text.includes('additional +1');
}

function isDefiantDance(card) {
  const text = textOf(card);
  return normalize(card?.name) === 'DEFIANT DANCE'
    && text.includes('give a unit')
    && text.includes('+2')
    && text.includes('another unit')
    && text.includes('-2');
}

function isFlash(card) {
  const text = textOf(card);
  return normalize(card?.name) === 'FLASH'
    && text.includes('move up to 2 friendly units')
    && text.includes('base');
}

function isCharm(card) {
  return normalize(card?.name) === 'CHARM'
    && textOf(card).trim() === 'move an enemy unit.';
}

function isStackedDeck(card) {
  const text = textOf(card);
  return text.includes('look at the top 3') && text.includes('put 1') && text.includes('hand') && text.includes('recycle');
}

function textOf(card) {
  return String(card?.rulesText ?? card?.effect ?? '').toLowerCase();
}

function isUnsupportedAction(card) {
  if (!card) return false;
  const type = String(card.type ?? '').toLowerCase();
  const text = textOf(card);
  if (type === 'gear') return !text.includes('[equip]');
  if (type !== 'spell') return false;
  const supportedFriendlyEnemyReturn = text.includes('return')
    && text.includes('friendly unit')
    && text.includes('enemy unit');
  const requiresMultipleTargets = (text.includes('another unit') && !isDefiantDance(card))
    || (text.includes('a friendly unit and an enemy unit') && !supportedFriendlyEnemyReturn);
  const supportedEffect = text.includes(':rb_might:')
    || text.includes('return a unit')
    || text.includes('move up to 2 friendly units')
    || supportedFriendlyEnemyReturn
    || text.includes('ready it')
    || text.includes('draw 1')
    || isCharm(card)
    || isDefy(card)
    || isNotSoFast(card)
    || isDiscipline(card)
    || isEnGarde(card)
    || isDefiantDance(card)
    || isFlash(card)
    || isStackedDeck(card);
  return (text.includes('counter a spell') && !isDefy(card))
    || (text.includes('counter an enemy spell') && !isNotSoFast(card))
    || requiresMultipleTargets
    || !supportedEffect;
}

function supportSummary(localCard, sourceCard, resolution) {
  if (resolution.status !== 'RESOLVED') {
    return {
      status: 'NOT_AUDITED',
      reason: resolution.reason ?? 'Source card could not be resolved to local RiftForge card data.',
      blocked: true,
    };
  }
  const card = localCard ?? sourceCard;
  if (BANNED_NAMES.has(normalize(card.name)) || sourceCard?.banned) {
    return { status: 'BANNED', reason: 'This card is banned in the current Constructed format.', blocked: true };
  }
  if (isUnsupportedAction(card)) {
    return { status: 'UNSUPPORTED', reason: "Blocked in enforced play: this card's effect is not supported yet.", blocked: true };
  }
  if (SUPPORTED_CARD_NAMES.has(normalize(card.name))) {
    return { status: 'SUPPORTED', reason: 'Implemented and covered by current support policy.', blocked: false };
  }
  return {
    status: 'PARTIAL',
    reason: PARTIAL_REASONS.get(normalize(card.name)) ?? 'Playable for alpha testing, but card-specific behavior may be incomplete.',
    blocked: false,
  };
}

function classifyRole(sourceCard) {
  const type = String(sourceCard?.type ?? '').toLowerCase();
  const supertype = String(sourceCard?.supertype ?? '').toLowerCase();
  if (type === 'legend') return 'Legend';
  if (type === 'rune') return 'Rune Deck';
  if (type === 'battlefield') return 'Battlefields';
  if (type === 'unit' && supertype === 'champion') return 'Champion / MainDeck candidate';
  if (type === 'champion') return 'Champion / MainDeck candidate';
  return 'Main Deck';
}

function mechanicBucket(card) {
  if (!card) return 'Unresolved / missing card data';
  const type = String(card.type ?? '').toLowerCase();
  const text = textOf(card);
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

function summarizeDeckRows(rows) {
  const counts = { SUPPORTED: 0, PARTIAL: 0, UNSUPPORTED: 0, BANNED: 0, NOT_AUDITED: 0 };
  for (const row of rows) counts[row.support.status] = (counts[row.support.status] ?? 0) + 1;
  const unresolved = rows.filter((row) => row.resolution.status !== 'RESOLVED');
  const blocked = rows.filter((row) => ['UNSUPPORTED', 'BANNED', 'NOT_AUDITED'].includes(row.support.status));
  return {
    uniqueCards: rows.length,
    counts,
    unresolved: unresolved.map((row) => row.source),
    canUseInEnforced: blocked.length === 0,
    topBlockers: topBlockers(rows),
  };
}

function topBlockers(rows) {
  const blockerRows = rows.filter((row) => row.support.status === 'UNSUPPORTED' || row.support.status === 'NOT_AUDITED' || row.support.status === 'BANNED');
  const partialRows = rows.filter((row) => row.support.status === 'PARTIAL');
  return [...blockerRows, ...partialRows].slice(0, 5).map((row) => ({
    cardName: row.source.name ?? row.source.sourceCardId,
    status: row.support.status,
    bucket: row.bucket,
    reason: row.support.reason,
  }));
}

function archetypeStatus(decks) {
  if (decks.length === 0) return 'Not Audited';
  if (decks.some((deck) => deck.summary.counts.UNSUPPORTED > 0 || deck.summary.counts.BANNED > 0 || deck.summary.counts.NOT_AUDITED > 0)) {
    return 'Blocked';
  }
  if (decks.some((deck) => deck.summary.counts.PARTIAL > 0)) return 'Partial';
  return 'Playable';
}

function markdownTable(headers, rows) {
  const escape = (value) => String(value ?? '').replaceAll('|', '\\|').replace(/\s+/g, ' ').trim();
  return [
    `| ${headers.join(' | ')} |`,
    `| ${headers.map(() => '---').join(' | ')} |`,
    ...rows.map((row) => `| ${row.map(escape).join(' | ')} |`),
  ].join('\n');
}

function sectionBreakdown(rows) {
  const sections = new Map();
  for (const row of rows) {
    const values = sections.get(row.role) ?? [];
    values.push(row);
    sections.set(row.role, values);
  }
  return [...sections.entries()].map(([section, sectionRows]) => ({
    section,
    cards: sectionRows.map((row) => `${row.quantity}x ${row.source.name ?? row.source.sourceCardId}`).join(', '),
  }));
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`);
}

function writeText(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, data);
}

function archetypeMarkdown(source, decks) {
  const headers = ['Deck slug', 'Deck name', 'Unique cards', 'Supported', 'Partial', 'Unsupported', 'Not Audited', 'Enforced playable'];
  const rows = decks.map((deck) => [
    deck.slug,
    deck.humanName,
    deck.summary.uniqueCards,
    deck.summary.counts.SUPPORTED,
    deck.summary.counts.PARTIAL,
    deck.summary.counts.UNSUPPORTED,
    deck.summary.counts.NOT_AUDITED,
    deck.summary.canUseInEnforced ? 'Yes' : 'No',
  ]);
  const blockers = [...new Map(decks.flatMap((deck) => deck.summary.topBlockers).map((item) => [item.cardName, item])).values()];
  const unresolved = decks.flatMap((deck) => deck.summary.unresolved);
  const deckSections = decks.map((deck) => {
    const sectionRows = sectionBreakdown(deck.cards).map((entry) => [entry.section, entry.cards]);
    return [
      `### ${deck.humanName}`,
      '',
      `- Source deck slug: \`${deck.slug}\``,
      `- Source API: ${deck.deckApiUrl}`,
      `- Extracted file: \`${path.relative(ROOT, deck.outputFile).replaceAll('\\', '/')}\``,
      '',
      markdownTable(['Section', 'Cards'], sectionRows),
      '',
      'Top blockers:',
      '',
      ...deck.summary.topBlockers.map((item) => `- ${item.cardName}: ${item.status} - ${item.bucket}. ${item.reason}`),
      '',
    ].join('\n');
  });

  return [
    `# ${source.legend} Meta Deck Audit`,
    '',
    `Captured: ${CAPTURED_DATE}`,
    '',
    `Source guide: ${source.guideUrl}`,
    '',
    '## Extraction Status',
    '',
    `The Riftbound.gg guide page embeds one or more deck widgets with \`data-deck\` slugs. This audit fetches each slug from the DotGG deck API and resolves card ids through the DotGG card catalog before matching names against the local RiftForge card cache.`,
    '',
    `Extraction status: ${decks.length > 0 ? 'Extracted' : 'Manual extraction needed'}`,
    '',
    '## Support Summary',
    '',
    markdownTable(headers, rows),
    '',
    '## Unresolved Cards',
    '',
    unresolved.length === 0
      ? '- None.'
      : unresolved.map((card) => `- ${card.sourceCardId}: ${card.name ?? 'unknown name'}`).join('\n'),
    '',
    '## Shared Top Blockers',
    '',
    blockers.length === 0
      ? '- None.'
      : blockers.map((item) => `- ${item.cardName}: ${item.status} - ${item.bucket}. ${item.reason}`).join('\n'),
    '',
    '## Decklists',
    '',
    ...deckSections,
    '## Recommended Implementation Order',
    '',
    recommendationFor(source.key),
    '',
  ].join('\n');
}

function recommendationFor(key) {
  const map = {
    irelia: '- Continue the current Irelia slice: tighten unsupported Irelia spells, Champion/Legend text, and Battlefield effects that appear in these guide lists.',
    diana: '- Treat Diana as the next implementation target after Irelia because its lists are interaction-heavy and should stress Reaction/chain/priority, targeting, and combat timing.',
    annie: '- Use Annie as the first Aurora-shell input while waiting for a Miss Fortune guide/list; audit shared Aurora cards before implementing one-off pieces.',
    'master-yi': '- Keep Master Yi tracked as the raw meta leader, but pick implementation work only after reviewing these guide lists with gameplay notes.',
    leblanc: '- Audit LeBlanc after Diana/Aurora unless playtester reports make it urgent.',
    vex: '- Audit Vex after Diana/Aurora unless playtester reports make it urgent.',
    azir: '- Audit Azir token/location/scoring needs after higher-priority interaction decks.',
    sivir: '- Keep Sivir as an additional guide-sourced archetype; prioritize only if reviewers request it.',
  };
  return map[key] ?? '- Use the blockers above to choose the smallest safe support slice.';
}

function mainRoadmapMarkdown(results) {
  const rows = results.map((result) => {
    const stats = META_STATS[result.source.key] ?? { share: 'n/a', winRate: 'n/a', decks: 'n/a' };
    const status = archetypeStatus(result.decks);
    const blockers = [...new Map(result.decks.flatMap((deck) => deck.summary.topBlockers).map((item) => [item.cardName, item])).values()]
      .slice(0, 5)
      .map((item) => `${item.cardName} (${item.status})`)
      .join(', ') || 'None from extracted list';
    return [
      result.source.legend,
      stats.share,
      stats.winRate,
      stats.decks,
      status,
      result.decks.length ? `${result.decks.length} list(s) extracted` : 'Manual extraction needed',
      blockers,
      result.source.priority,
    ];
  });

  return [
    '# Meta Deck Support Roadmap',
    '',
    `Last updated: ${CAPTURED_DATE}`,
    '',
    'This roadmap combines reviewer meta priorities with actual guide decklists extracted from Riftbound.gg. Decklists are captured as reviewable JSON under `decks/meta/`; support summaries are generated from the current local card cache and the same conservative support rules used by RiftForge frontend/backend support gates.',
    '',
    'Do not treat an archetype as supported just because a list is present. `Supported` means a card is implemented and directly covered by policy/tests; `Partial` means playable alpha behavior may exist but exact rules can be incomplete; `Unsupported` and `Not Audited` still block supported-only enforced play.',
    '',
    '## Extraction Method',
    '',
    '- Riftbound.gg guide pages contain embedded deck widgets with `data-deck` slugs rather than complete static card lists.',
    '- `scripts/meta-deck-audit.mjs` fetches each guide, extracts those slugs, loads each list through `https://api.dotgg.gg/cgfw/getdeck?game=riftbound&slug=<slug>`, and resolves DotGG card ids through the DotGG card catalog.',
    '- Each source card is matched by exact/base name against `~/.riftforge/cards-cache.json`. Unresolved or ambiguous names are recorded instead of guessed.',
    '',
    '## Meta Priority Table',
    '',
    markdownTable(['Legend / archetype', 'Meta share', 'Win rate', 'Deck count', 'Current support status', 'Guide list status', 'Top blockers', 'Next implementation target'], rows),
    '',
    '## Reviewer-Prioritized Support Order',
    '',
    '1. Finish the current/Irelia support slice.',
    '2. Diana interaction deck.',
    '3. Aurora shell: Annie now has a guide-sourced list; Miss Fortune still needs a representative list.',
    '4. Master Yi audit pending representative gameplay notes, despite the raw meta lead.',
    '5. LeBlanc.',
    '6. Vex.',
    '7. Azir.',
    '8. Sivir as an additional guide-sourced archetype if reviewer demand rises.',
    '',
    'Diana remains the recommended next implementation target because reviewer signal says it is interaction-heavy and growing. It should stress Reaction/chain/priority, targeting, and combat timing more usefully than picking solely by raw meta share.',
    '',
    '## Extracted Audit Files',
    '',
    ...results.map((result) => `- ${result.source.legend}: \`docs/meta/${result.source.key}.md\`, \`decks/meta/${result.source.key}/\``),
    '- Miss Fortune / Aurora: no guide URL was supplied in this sprint, so it remains manual-list-needed.',
    '',
    '## Needed From Reviewers',
    '',
    '- Miss Fortune Aurora deck list.',
    '- Gameplay notes for Master Yi, especially mandatory interaction patterns and must-work cards.',
    '- Notes on which Diana/Annie lists are preferred for playtesting if multiple tournament lists are present.',
    '- Cards that must be exact before external testers should use supported-cards-only mode.',
    '',
    '## Recommended Next Sprint',
    '',
    'Use the extracted Diana lists to choose one narrow support slice, preferably the highest-repeat unsupported/partial interaction pattern shared across Diana lists. Keep Miss Fortune in the Aurora bucket until a real list is provided, and do not implement from archetype names alone.',
    '',
  ].join('\n');
}

function summaryJson(results) {
  return {
    capturedDate: CAPTURED_DATE,
    extraction: {
      guidePages: GUIDE_SOURCES.map((source) => source.guideUrl),
      deckApi: `${DOTGG_DECK_API}&slug=<slug>`,
      cardApi: DOTGG_CARD_API,
      localCardCache: CACHE_PATH,
    },
    archetypes: results.map((result) => ({
      key: result.source.key,
      legend: result.source.legend,
      guideUrl: result.source.guideUrl,
      supportStatus: archetypeStatus(result.decks),
      extractedDecks: result.decks.map((deck) => ({
        slug: deck.slug,
        humanName: deck.humanName,
        outputFile: path.relative(ROOT, deck.outputFile).replaceAll('\\', '/'),
        summary: deck.summary,
      })),
    })),
  };
}

const localCards = loadLocalCards();
const localByName = byNormalizedName(localCards);
const cardCatalog = decodeIndexedCards(await fetchJson(DOTGG_CARD_API));
const sourceCardsById = new Map(cardCatalog.map((card) => [card.id, sourceCardDefinition(card)]));

const results = [];
for (const source of GUIDE_SOURCES) {
  console.log(`Extracting ${source.legend} from ${source.guideUrl}`);
  const guideHtml = await fetchText(source.guideUrl);
  const slugs = extractDeckSlugs(guideHtml);
  const decks = [];
  for (const slug of slugs) {
    const deckApiUrl = `${DOTGG_DECK_API}&slug=${encodeURIComponent(slug)}`;
    const deckPayload = await fetchJson(deckApiUrl);
    const deckMap = deckPayload.deck ?? {};
    const cards = Object.entries(deckMap).map(([sourceCardId, quantityValue]) => {
      const sourceCard = sourceCardsById.get(sourceCardId) ?? {
        id: sourceCardId,
        name: null,
        type: null,
        supertype: null,
        rulesText: '',
      };
      const resolution = resolveLocal(sourceCard, localByName);
      const localCard = resolution.card ?? null;
      const support = supportSummary(localCard, sourceCard, resolution);
      return {
        source: {
          sourceCardId,
          name: sourceCard.name,
          type: sourceCard.type,
          supertype: sourceCard.supertype,
          setName: sourceCard.setName,
          rulesText: sourceCard.rulesText,
          sourceImageUrl: sourceCard.imageUrl,
        },
        quantity: Number.parseInt(quantityValue, 10),
        role: classifyRole(sourceCard),
        resolution: resolution.status === 'RESOLVED'
          ? { status: resolution.status, method: resolution.method, localCard: minCard(localCard) }
          : resolution,
        support,
        keywords: detectedKeywords(localCard ?? sourceCard),
        bucket: mechanicBucket(localCard ?? sourceCard),
      };
    }).sort((a, b) => a.role.localeCompare(b.role) || String(a.source.name).localeCompare(String(b.source.name)));
    const deck = {
      sourceGuideUrl: source.guideUrl,
      deckApiUrl,
      sourceDeckSlug: slug,
      slug,
      humanName: deckPayload.humanname ?? slug,
      format: deckPayload.format ?? null,
      sourceBoards: deckPayload.boards ?? null,
      capturedDate: CAPTURED_DATE,
      legend: source.legend,
      cards,
    };
    deck.summary = summarizeDeckRows(cards);
    const outputFile = path.join(ROOT, 'decks', 'meta', source.key, `${slug}.json`);
    deck.outputFile = outputFile;
    writeJson(outputFile, deck);
    decks.push(deck);
  }
  results.push({ source, decks });
  writeText(path.join(ROOT, 'docs', 'meta', `${source.key}.md`), archetypeMarkdown(source, decks));
}

writeJson(path.join(ROOT, 'decks', 'meta', 'audit-summary.json'), summaryJson(results));
writeText(path.join(ROOT, 'docs', 'META_DECK_SUPPORT.md'), mainRoadmapMarkdown(results));

console.log('Wrote meta deck audit outputs:');
console.log('- docs/META_DECK_SUPPORT.md');
console.log('- docs/meta/*.md');
console.log('- decks/meta/*/*.json');
console.log('- decks/meta/audit-summary.json');
