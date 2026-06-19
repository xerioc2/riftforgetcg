import fs from 'node:fs';
import path from 'node:path';
import { loadLocalCards, normalizeDeck } from './meta-deck-parser.mjs';

const ROOT = path.resolve(import.meta.dirname, '..');
const RAW_DIR = path.join(ROOT, 'decks', 'meta', 'raw');
const NORMALIZED_DIR = path.join(ROOT, 'decks', 'meta', 'normalized');
const DOCS_DIR = path.join(ROOT, 'docs', 'meta');

const DECKS = [
  { file: 'irelia_wins_s3_shanghai_city_challenge.txt', key: 'irelia', archetype: 'Irelia, Blade Dancer', priority: 'Finish current/Irelia support polish' },
  { file: 'diana_wins_s3_suzhou_city_challenge.txt', key: 'diana', archetype: 'Diana, Scorn of the Moon', priority: 'Next reviewer-prioritized interaction deck' },
  { file: 'annie_4th_at_lille_regional_qualifier.txt', key: 'annie', archetype: 'Annie, Dark Child', priority: 'Aurora shell with future Miss Fortune list' },
  { file: 'master_yi_wins_s3_guangzhou_city_challenge.txt', key: 'master-yi', archetype: 'Master Yi, Wuju Bladesman', priority: 'Audit before implementation; gameplay notes still useful' },
  { file: 'leblanc_wins_s3_zhongshan_city_challenge.txt', key: 'leblanc', archetype: 'LeBlanc, Deceiver', priority: 'Later raw-meta audit' },
  { file: 'vex_top_8_at_s3_zhongshan_city_challenge.txt', key: 'vex', archetype: 'Vex, Gloomist', priority: 'Later raw-meta audit' },
  { file: 'azir_wins_lille_regional_qualifier.txt', key: 'azir', archetype: 'Azir, Emperor of the Sands', priority: 'Later raw-meta audit' },
  { file: 'sivir_2nd_at_sydney_regional_qualifier.txt', key: 'sivir', archetype: 'Sivir, Battle Mistress', priority: 'Additional guide-sourced archetype' },
  { file: 'fiora_wins_s3_beijing_city_challenge.txt', key: 'fiora', archetype: 'Fiora, Grand Duelist', priority: 'Additional meta data' },
  { file: 'draven_wins_new_zealand_10k_open.txt', key: 'draven', archetype: 'Draven, Glorious Executioner', priority: 'Additional meta data' },
];

function slugFromFile(file) {
  return file.replace(/\.txt$/i, '');
}

function eventFromFile(file) {
  return slugFromFile(file).replaceAll('_', ' ');
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`);
}

function writeText(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, data);
}

function markdownTable(headers, rows) {
  const escape = (value) => String(value ?? '').replaceAll('|', '\\|').replace(/\s+/g, ' ').trim();
  return [
    `| ${headers.join(' | ')} |`,
    `| ${headers.map(() => '---').join(' | ')} |`,
    ...rows.map((row) => `| ${row.map(escape).join(' | ')} |`),
  ].join('\n');
}

function sectionRows(deck) {
  const sections = new Map();
  for (const entry of deck.entries) {
    const values = sections.get(entry.section) ?? [];
    values.push(`${entry.quantity}x ${entry.originalName} (${entry.sourceCardId})`);
    sections.set(entry.section, values);
  }
  return [...sections.entries()].map(([section, cards]) => [section, cards.join(', ')]);
}

function supportStatus(deck) {
  if (deck.supportSummary.counts.UNSUPPORTED > 0 || deck.supportSummary.counts.BANNED > 0 || deck.supportSummary.counts.NOT_AUDITED > 0) {
    return 'Blocked';
  }
  if (deck.supportSummary.counts.PARTIAL > 0) return 'Partial';
  return 'Playable';
}

function docFor(deck) {
  const blockers = deck.supportSummary.topBlockers.length
    ? deck.supportSummary.topBlockers.map((item) => `- ${item.cardName} (${item.sourceCardId}): ${item.status} - ${item.bucket}. ${item.reason}`).join('\n')
    : '- None.';
  const unresolved = deck.supportSummary.unresolvedCards.length
    ? deck.supportSummary.unresolvedCards.map((item) => `- ${item.originalName} (${item.sourceCardId}): ${item.reason}`).join('\n')
    : '- None.';
  return [
    `# ${deck.archetype} Uploaded Meta Deck Audit`,
    '',
    `Source raw file: \`${deck.rawSourceFile}\``,
    `Normalized deck: \`${deck.normalizedFile}\``,
    `Event/result: ${deck.eventResult}`,
    '',
    '## Validation',
    '',
    `- Total cards: ${deck.validation.totalCards}`,
    `- Inferred main deck count: ${deck.validation.inferredMainDeckCount}`,
    `- Rune count: ${deck.validation.runeCount}`,
    `- Battlefield count: ${deck.validation.battlefieldCount}`,
    `- Legend count: ${deck.validation.legendCount}`,
    `- Champion candidate count: ${deck.validation.championCandidateCount}`,
    `- Constructed shape check: ${deck.validation.validConstructedShape ? 'Pass' : 'Needs review'}`,
    '',
    deck.validation.issues.length ? deck.validation.issues.map((issue) => `- ${issue}`).join('\n') : '- No shape issues found.',
    '',
    '## Support Summary',
    '',
    `- Status: ${supportStatus(deck)}`,
    `- Supported: ${deck.supportSummary.counts.SUPPORTED}`,
    `- Partial: ${deck.supportSummary.counts.PARTIAL}`,
    `- Unsupported: ${deck.supportSummary.counts.UNSUPPORTED}`,
    `- Not Audited: ${deck.supportSummary.counts.NOT_AUDITED}`,
    `- Enforced playable: ${deck.supportSummary.canUseInEnforced ? 'Yes' : 'No'}`,
    '',
    '## Unresolved Cards',
    '',
    unresolved,
    '',
    '## Top Blockers',
    '',
    blockers,
    '',
    '## Normalized Sections',
    '',
    markdownTable(['Section', 'Cards'], sectionRows(deck)),
    '',
    '## Recommended Implementation Order',
    '',
    recommendation(deck.key),
    '',
  ].join('\n');
}

function recommendation(key) {
  const recommendations = {
    irelia: '- Continue current Irelia polish: Zhonya\'s Hourglass now has narrow Partial would-die replacement support; remaining blockers should focus on broader timing/replacement caveats and other Partial meta-card text.',
    diana: '- Diana remains next: start with repeated interaction blockers such as Abandon/Hard Bargain or a shared Gear/effect blocker if it appears in the chosen support slice.',
    annie: '- Use Annie as the first Aurora-shell list while waiting for Miss Fortune; focus on shared Reaction/bounce/damage blockers only after MF is available.',
    'master-yi': '- Review gameplay notes before implementation; likely blockers are Gear, Champion/Legend text, and remaining movement/location edge cases beyond Charm\'s narrow alpha support.',
    leblanc: '- Defer until Diana/Aurora unless tester demand rises; blockers skew toward destroy/hidden/counter-style spell effects.',
    vex: '- Defer until Diana/Aurora unless tester demand rises; blockers include Blast Cone, Emperor\'s Divide, Switcheroo, and hidden/facedown pieces. Charm now has narrow Partial support.',
    azir: '- Defer until higher-priority interaction decks; blockers include Facebreaker, Hidden Blade, Wind Wall, and remaining movement/location edge cases beyond Charm\'s narrow alpha support.',
    sivir: '- Keep as additional data; blockers include Dazzling Aurora, Flurry of Blades, Lunar Boon, Pack of Wonders, and Sabotage.',
    fiora: '- Treat as additional data for equipment/combat work; blockers include Challenge, Hidden Blade, and several Partial Gear/combat texts.',
    draven: '- Treat as additional data for Aurora/Draven shell work; blockers include Switcheroo, Edge of Night, Hard Bargain, and token/hidden pieces.',
  };
  return recommendations[key] ?? '- Choose the smallest repeated blocker before implementing broader systems.';
}

function mainSummary(decks) {
  const rows = decks.map((deck) => [
    deck.archetype,
    deck.rawSourceFile,
    supportStatus(deck),
    deck.validation.validConstructedShape ? 'Pass' : 'Needs review',
    `${deck.supportSummary.counts.SUPPORTED}/${deck.supportSummary.counts.PARTIAL}/${deck.supportSummary.counts.UNSUPPORTED}/${deck.supportSummary.counts.NOT_AUDITED}`,
    deck.supportSummary.topBlockers.map((item) => item.cardName).join(', ') || 'None',
    deck.priority,
  ]);
  return [
    '# Uploaded Meta Deck Support Summary',
    '',
    'Generated from exact raw exports in `decks/meta/raw/`.',
    '',
    markdownTable(['Archetype', 'Raw source', 'Support status', 'Shape', 'S/P/U/NA', 'Top blockers', 'Priority note'], rows),
    '',
    'Near-term order: Irelia polish, Diana next, Annie/Aurora after Miss Fortune arrives, Master Yi after gameplay notes, then LeBlanc/Vex/Azir/Sivir, with Fiora/Draven as additional meta data.',
    '',
  ].join('\n');
}

const cards = loadLocalCards();
const normalizedDecks = [];
for (const config of DECKS) {
  const rawPath = path.join(RAW_DIR, config.file);
  const rawText = fs.readFileSync(rawPath, 'utf8');
  const normalized = normalizeDeck(rawText, config.file, cards);
  const slug = slugFromFile(config.file);
  const normalizedFile = path.join(NORMALIZED_DIR, `${slug}.json`);
  const deck = {
    ...normalized,
    archetype: config.archetype,
    key: config.key,
    eventResult: eventFromFile(config.file),
    priority: config.priority,
    rawSourceFile: path.relative(ROOT, rawPath).replaceAll('\\', '/'),
    normalizedFile: path.relative(ROOT, normalizedFile).replaceAll('\\', '/'),
  };
  writeJson(normalizedFile, deck);
  writeText(path.join(DOCS_DIR, `${config.key}-uploaded.md`), docFor(deck));
  normalizedDecks.push(deck);
}

writeJson(path.join(NORMALIZED_DIR, 'uploaded-audit-summary.json'), {
  capturedDate: '2026-06-19',
  decks: normalizedDecks.map((deck) => ({
    archetype: deck.archetype,
    rawSourceFile: deck.rawSourceFile,
    normalizedFile: deck.normalizedFile,
    validation: deck.validation,
    supportSummary: deck.supportSummary,
  })),
});
writeText(path.join(DOCS_DIR, 'UPLOADED_META_DECKS.md'), mainSummary(normalizedDecks));

console.log(`Imported ${normalizedDecks.length} uploaded meta decklist(s).`);
