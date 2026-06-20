import type React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { CardModal } from '../components/CardModal';
import { ManaCurve } from '../components/ManaCurve';
import { useCardStore } from '../store/cards';
import { useDeckStore } from '../store/decks';
import { validateDeck } from '../lib/deckValidation';
import { cardSupportStatus } from '../lib/deckSupport';
import { exportDeckText, importDeckText } from '../lib/deckImport';
import { getDeckLegendCardId } from '../lib/deckUtils';
import { STARTER_DECKS, resolveStarterDeck, type StarterDeckSpec } from '../lib/starterDecks';
import type { CardFilters, Deck, RiftCard } from '../types';
import { notifyError, notifySuccess } from '../store/toasts';

const emptyFilters: CardFilters = {
  search: '',
  champion: '',
  domain: '',
  type: '',
  cost: '',
};

const cx = (...classes: Array<string | false | undefined>) => classes.filter(Boolean).join(' ');

export function DeckBuild() {
  const { cards, loading, error, loadCards } = useCardStore();
  const { decks, activeDeckId, setActiveDeck, setDecks, createDeck, updateDeck, deleteDeck } = useDeckStore();
  const [filters, setFilters] = useState<CardFilters>(emptyFilters);
  const [saveMessage, setSaveMessage] = useState('');
  const [selectedCard, setSelectedCard] = useState<RiftCard | null>(null);
  const [showImport, setShowImport] = useState(false);

  useEffect(() => {
    void loadCards();
  }, [loadCards]);

  const activeDeck = decks.find((deck) => deck.id === activeDeckId) ?? decks[0];
  const cardsById = useMemo(() => new Map(cards.map((card) => [card.id, card])), [cards]);
  const validation = useMemo(() => validateDeck(activeDeck, cardsById), [activeDeck, cardsById]);

  const filterOptions = useMemo(() => {
    const unique = (values: Array<string | undefined>) => [...new Set(values.filter(Boolean) as string[])].sort();
    return {
      champions: unique(cards.filter((card) => card.type === 'Legend').map((card) => card.name)),
      domains: unique(cards.flatMap((card) => card.domains)),
      types: unique(cards.map((card) => card.type)),
      costs: unique(cards.map((card) => (card.cost === undefined ? undefined : String(card.cost)))),
    };
  }, [cards]);

  const filteredCards = useMemo(() => {
    const query = filters.search.trim().toLowerCase();
    return cards.filter((card) => {
      const searchTarget = `${card.name} ${card.rulesText ?? ''} ${card.champion ?? ''}`.toLowerCase();
      return (
        (!query || searchTarget.includes(query)) &&
        (!filters.champion || card.name === filters.champion) &&
        (!filters.domain || card.domains.includes(filters.domain)) &&
        (!filters.type || card.type === filters.type) &&
        (!filters.cost || String(card.cost ?? '') === filters.cost)
      );
    });
  }, [cards, filters]);

  const deckEntries = activeDeck.cards
    .map((entry) => ({ ...entry, card: cardsById.get(entry.cardId) }))
    .filter((entry): entry is typeof entry & { card: RiftCard } => Boolean(entry.card))
    .sort((a, b) => (a.card.cost ?? 99) - (b.card.cost ?? 99) || a.card.name.localeCompare(b.card.name));

  const patchDeck = (patch: Partial<Deck>) => updateDeck({ ...activeDeck, ...patch });

  const addCard = (card: RiftCard) => {
    if (card.type === 'Legend') {
      patchDeck({
        legendCardId: card.id,
        cards: activeDeck.cards.filter((entry) => cardsById.get(entry.cardId)?.type !== 'Legend'),
      });
      return;
    }

    if (card.type === 'Champion') {
      patchDeck({
        championCardId: card.id,
        cards: activeDeck.cards.filter((entry) => entry.cardId !== card.id),
      });
      return;
    }

    const existing = activeDeck.cards.find((entry) => entry.cardId === card.id);
    const nextCards = existing
      ? activeDeck.cards.map((entry) => (entry.cardId === card.id ? { ...entry, quantity: entry.quantity + 1 } : entry))
      : [...activeDeck.cards, { cardId: card.id, quantity: 1 }];
    patchDeck({ cards: nextCards });
  };

  const setQuantity = (cardId: string, quantity: number) => {
    patchDeck({
      cards: activeDeck.cards
        .map((entry) => (entry.cardId === cardId ? { ...entry, quantity } : entry))
        .filter((entry) => entry.quantity > 0),
    });
  };

  const exportDeck = async () => {
    await navigator.clipboard.writeText(exportDeckText(activeDeck, cardsById));
    setSaveMessage('Deck copied to clipboard in tournament format.');
  };

  const importDeck = (text: string) => {
    const result = importDeckText(text, cards);
    if (result.matchedLines === 0) return result;

    patchDeck({
      legendCardId: result.legendCardId,
      championCardId: result.championCardId,
      cards: result.cards,
    });
    const skipped = result.skippedSideboard ? ` Skipped ${result.skippedSideboard} sideboard cards.` : '';
    setSaveMessage(
      `Imported ${result.cards.reduce((sum, card) => sum + card.quantity, 0)} cards${result.legendCardId ? ' and a legend' : ''}.${skipped}`,
    );
    return result;
  };

  const loadStarterDeck = (spec: StarterDeckSpec) => {
    const result = resolveStarterDeck(spec, cards);
    if (!result.deck) {
      const message = `Missing cards: ${result.missingCards.join(', ')}.`;
      setSaveMessage(`Could not load ${spec.name}. ${message}`);
      notifyError(`Could not load ${spec.name}`, message);
      return;
    }
    const nextDecks = [...decks, result.deck];
    setDecks(nextDecks);
    setActiveDeck(result.deck.id);
    setSaveMessage(`Loaded starter deck: ${spec.name}.`);
    notifySuccess('Starter deck loaded', spec.name);
  };

  return (
    <main className="h-auto min-h-[calc(100vh-73px)] bg-ink text-slate-100 xl:h-[calc(100vh-73px)] xl:overflow-hidden">
      <div className="mx-auto grid h-full max-w-[1500px] gap-5 px-5 py-5 xl:grid-cols-[minmax(0,1fr)_430px]">
        <section className="flex min-h-0 min-w-0 flex-col">
          <div className="shrink-0 pb-4">
            <div className="mb-4 flex flex-wrap items-center gap-3 text-sm text-slate-300">
            <StatusPill tone={cards.length ? 'good' : 'warn'}>{cards.length} cards cached</StatusPill>
            <StatusPill tone="good">Standalone mode</StatusPill>
            <button className="btn-secondary" onClick={() => void loadCards()} disabled={loading}>
              {loading ? 'Refreshing...' : 'Refresh cards'}
            </button>
            </div>
            <FilterBar filters={filters} options={filterOptions} onChange={setFilters} />
            {error ? <Notice tone="bad">{error}</Notice> : null}
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto pr-1 xl:pb-2">
            <div className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-4">
              {filteredCards.map((card) => (
                <CardTile key={card.id} card={card} selected={getDeckLegendCardId(activeDeck) === card.id} onAdd={() => addCard(card)} onDetails={() => setSelectedCard(card)} />
              ))}
            </div>
            {!loading && filteredCards.length === 0 ? <Notice tone="warn">No cards match these filters. Try refreshing or clearing a filter.</Notice> : null}
          </div>
        </section>

        <aside className="min-h-0 xl:overflow-y-auto xl:pr-1">
          <DeckBuilder
            deck={activeDeck}
            decks={decks}
            entries={deckEntries}
            validation={validation}
            onSelectDeck={setActiveDeck}
            onCreateDeck={createDeck}
            onDeleteDeck={deleteDeck}
            onRename={(name) => patchDeck({ name })}
            onQuantity={setQuantity}
            onSave={() => setSaveMessage('Deck saved locally.')}
            onImport={() => setShowImport(true)}
            onExport={() => void exportDeck()}
            onLoadStarter={loadStarterDeck}
            canSave
            saveMessage={saveMessage}
          />
        </aside>
      </div>
      <CardModal card={selectedCard} onClose={() => setSelectedCard(null)} />
      <DeckImportModal open={showImport} catalog={cards} onClose={() => setShowImport(false)} onImport={importDeck} />
    </main>
  );
}

function StatusPill({ children, tone }: { children: React.ReactNode; tone: 'good' | 'warn' }) {
  return <span className={cx('rounded-sm border px-3 py-2', tone === 'good' ? 'border-mint/40 text-mint' : 'border-forge/50 text-forge')}>{children}</span>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: 'bad' | 'warn' }) {
  return <div className={cx('mt-4 border px-4 py-3 text-sm', tone === 'bad' ? 'border-ember/50 text-ember' : 'border-forge/50 text-forge')}>{children}</div>;
}

function FilterBar({
  filters,
  options,
  onChange,
}: {
  filters: CardFilters;
  options: { champions: string[]; domains: string[]; types: string[]; costs: string[] };
  onChange: (filters: CardFilters) => void;
}) {
  const setFilter = (key: keyof CardFilters, value: string) => onChange({ ...filters, [key]: value });

  return (
    <div className="grid gap-3 border border-line bg-panel p-4 shadow-glow md:grid-cols-[minmax(220px,1.4fr)_repeat(4,minmax(120px,1fr))_auto]">
      <input className="input" placeholder="Search cards" value={filters.search} onChange={(event) => setFilter('search', event.target.value)} />
      <Select value={filters.champion} label="Legend" options={options.champions} onChange={(value) => setFilter('champion', value)} />
      <Select value={filters.domain} label="Domain" options={options.domains} onChange={(value) => setFilter('domain', value)} />
      <Select value={filters.type} label="Type" options={options.types} onChange={(value) => setFilter('type', value)} />
      <Select value={filters.cost} label="Cost" options={options.costs} onChange={(value) => setFilter('cost', value)} />
      <button className="btn-secondary" onClick={() => onChange(emptyFilters)}>
        Clear
      </button>
    </div>
  );
}

function Select({ value, label, options, onChange }: { value: string; label: string; options: string[]; onChange: (value: string) => void }) {
  return (
    <select className="input" value={value} onChange={(event) => onChange(event.target.value)} aria-label={label}>
      <option value="">{label}</option>
      {options.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </select>
  );
}

function CardTile({ card, selected, onAdd, onDetails }: { card: RiftCard; selected: boolean; onAdd: () => void; onDetails: () => void }) {
  const support = cardSupportStatus(card);
  return (
    <article className={cx('flex min-h-[320px] flex-col overflow-hidden border bg-panel', selected ? 'border-forge' : 'border-line')}>
      <div className="flex aspect-[5/3] items-center justify-center bg-slate-950">
        {card.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt={card.name} loading="lazy" /> : <div className="px-5 text-center text-sm text-slate-500">{card.name}</div>}
      </div>
      <div className="flex flex-1 flex-col gap-3 p-4">
        <div>
          <div className="flex items-start justify-between gap-3">
            <h2 className="text-base font-semibold text-white">{card.name}</h2>
            {card.cost !== undefined ? <span className="badge">{card.cost}</span> : null}
          </div>
          <p className="mt-1 text-xs uppercase tracking-wide text-slate-400">{[card.type, card.rarity, card.set].filter(Boolean).join(' / ')}</p>
          <p
            className={cx(
              'mt-2 inline-flex border px-2 py-1 text-xs font-semibold uppercase tracking-wide',
              support.status === 'SUPPORTED'
                ? 'border-mint/40 text-mint'
                : support.status === 'PARTIAL'
                  ? 'border-forge/50 text-forge'
                  : 'border-ember/50 text-ember',
            )}
            title={support.reason}
          >
            {support.status.replace('_', ' ')}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {card.domains.map((domain) => (
            <span className="badge border-mint/30 text-mint" key={domain}>
              {domain}
            </span>
          ))}
        </div>
        {card.rulesText ? <p className="line-clamp-4 text-sm leading-6 text-slate-300">{card.rulesText}</p> : null}
        <div className="mt-auto grid grid-cols-2 gap-2">
          <button className="btn-secondary" onClick={onDetails}>
            Details
          </button>
          <button className="btn-primary" onClick={onAdd}>
            {card.type === 'Legend' ? 'Set legend' : card.type === 'Champion' ? 'Add champion' : 'Add card'}
          </button>
        </div>
      </div>
    </article>
  );
}

function DeckBuilder({
  deck,
  decks,
  entries,
  validation,
  onSelectDeck,
  onCreateDeck,
  onDeleteDeck,
  onRename,
  onQuantity,
  onSave,
  onImport,
  onExport,
  onLoadStarter,
  canSave,
  saveMessage,
}: {
  deck: Deck;
  decks: Deck[];
  entries: Array<{ cardId: string; quantity: number; card: RiftCard }>;
  validation: ReturnType<typeof validateDeck>;
  onSelectDeck: (id: string) => void;
  onCreateDeck: () => void;
  onDeleteDeck: (id: string) => void;
  onRename: (name: string) => void;
  onQuantity: (cardId: string, quantity: number) => void;
  onSave: () => void;
  onImport: () => void;
  onExport: () => void;
  onLoadStarter: (spec: StarterDeckSpec) => void;
  canSave: boolean;
  saveMessage: string;
}) {
  return (
    <section className="flex h-full min-h-0 flex-col border border-line bg-panel p-4 shadow-glow">
      <div className="shrink-0 border-b border-line bg-panel pb-4">
        <div className="flex items-center gap-3">
        <select className="input flex-1" value={deck.id} onChange={(event) => onSelectDeck(event.target.value)} aria-label="Deck">
          {decks.map((existing) => (
            <option key={existing.id} value={existing.id}>
              {existing.name}
            </option>
          ))}
        </select>
        <button className="btn-secondary" onClick={onCreateDeck}>
          New
        </button>
        </div>

        <input className="input mt-3 w-full text-lg font-semibold" value={deck.name} onChange={(event) => onRename(event.target.value)} />

        <div className="mt-4 flex flex-wrap gap-3">
          <button className="btn-primary" onClick={onSave} disabled={!canSave}>
            Save
          </button>
          <button className="btn-secondary" onClick={onImport}>
            Import
          </button>
          <button className="btn-secondary" onClick={onExport} disabled={entries.length === 0}>
            Export
          </button>
          <button className="btn-secondary border-ember/60 text-ember" onClick={() => onDeleteDeck(deck.id)}>
            Delete
          </button>
        </div>
        {saveMessage ? <p className="mt-3 text-sm text-slate-300">{saveMessage}</p> : null}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto pt-4">
        <div className="grid grid-cols-2 gap-3 text-center">
        <DeckMetric label="Main Cards" value={`${validation.mainDeckCards}/39`} />
        <DeckMetric label="Chosen Champion" value={validation.champion?.name ?? 'None'} />
        <DeckMetric label="Runes" value={`${validation.runeCards}/12`} />
        <DeckMetric label="Battlefields" value={`${validation.battlefieldCards}/3`} />
        <DeckMetric label="Legend" value={validation.legend?.name ?? 'None'} />
        <div className="col-span-2">
          <DeckMetric label="Domains" value={validation.domains.join(', ') || 'Unset'} />
        </div>
        </div>

        <div className={cx('mt-4 border px-3 py-3 text-sm', validation.valid ? 'border-mint/40 text-mint' : 'border-forge/50 text-forge')}>
          {validation.valid ? 'Deck passes constructed validation.' : validation.messages.slice(0, 4).join(' ')}
        </div>
        <ValidationReport validation={validation} />

        <div className="mt-4 border border-line bg-ink p-3">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-sm font-semibold text-white">Deck presets</p>
            <p className="mt-1 text-xs text-slate-400">Uploaded meta lists and alpha playtest decks with known limitations.</p>
          </div>
        </div>
        <div className="mt-3 space-y-3">
          {STARTER_DECKS.map((starter) => (
            <div className="border border-line bg-panel p-3" key={starter.id}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-semibold text-white">{starter.name}</p>
                  <p className="mt-1 text-xs uppercase tracking-wide text-forge">{starter.status}</p>
                </div>
                <button className="btn-secondary shrink-0 px-3 py-2 text-sm" onClick={() => onLoadStarter(starter)}>
                  Load
                </button>
              </div>
              <p className="mt-2 text-sm leading-5 text-slate-300">{starter.description}</p>
              {starter.warnings.length > 0 ? (
                <p className="mt-2 text-xs leading-5 text-ember">{starter.warnings.join(' ')}</p>
              ) : null}
            </div>
          ))}
        </div>
        </div>

        <div className="mt-4 max-h-[46vh] overflow-auto border border-line">
        {entries.map((entry) => (
          <div className="grid grid-cols-[44px_minmax(0,1fr)_92px] items-center gap-3 border-b border-line px-3 py-3 last:border-b-0" key={entry.cardId}>
            <span className="text-center text-lg font-semibold text-forge">{entry.quantity}</span>
            <div className="min-w-0">
              <p className="truncate font-medium text-white">{entry.card.name}</p>
              <p className="truncate text-xs text-slate-400">{[entry.card.type, entry.card.domains.join(', ')].filter(Boolean).join(' / ')}</p>
            </div>
            <div className="flex items-center justify-end gap-2">
              <button className="icon-btn" onClick={() => onQuantity(entry.cardId, entry.quantity - 1)} aria-label={`Remove ${entry.card.name}`}>
                -
              </button>
              <button className="icon-btn" onClick={() => onQuantity(entry.cardId, entry.quantity + 1)} aria-label={`Add ${entry.card.name}`}>
                +
              </button>
            </div>
          </div>
        ))}
        {entries.length === 0 ? <div className="px-3 py-8 text-center text-sm text-slate-400">Add cards from the browser or import a decklist.</div> : null}
        </div>

        <ManaCurve entries={entries} />
      </div>
    </section>
  );
}

function DeckImportModal({
  open,
  catalog,
  onClose,
  onImport,
}: {
  open: boolean;
  catalog: RiftCard[];
  onClose: () => void;
  onImport: (text: string) => ReturnType<typeof importDeckText>;
}) {
  const [text, setText] = useState('');
  const [unmatched, setUnmatched] = useState<string[]>([]);
  const preview = useMemo(() => (text.trim() ? importDeckText(text, catalog) : null), [catalog, text]);
  const catalogById = useMemo(() => new Map(catalog.map((card) => [card.id, card])), [catalog]);
  const previewCounts = useMemo(() => {
    const result = { main: 0, champion: 0, runes: 0, battlefields: 0 };
    if (!preview) return result;
    result.champion = preview.championCardId ? 1 : 0;
    for (const entry of preview.cards) {
      const card = catalogById.get(entry.cardId);
      if (!card) continue;
      if (card.type === 'Rune') result.runes += entry.quantity;
      else if (card.type === 'Battlefield') result.battlefields += entry.quantity;
      else if (card.type !== 'Legend') result.main += entry.quantity;
    }
    return result;
  }, [catalogById, preview]);

  if (!open) return null;

  const handleImport = () => {
    const result = onImport(text);
    setUnmatched(result.unmatched);
    if (result.matchedLines > 0 && result.unmatched.length === 0) onClose();
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 px-4 py-8" onClick={onClose}>
      <section className="w-full max-w-2xl border border-line bg-panel p-5 shadow-glow" onClick={(event) => event.stopPropagation()}>
        <div className="flex items-center justify-between gap-4">
          <h2 className="text-xl font-semibold text-white">Import decklist</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close import">
            x
          </button>
        </div>
        <textarea
          className="input mt-4 min-h-72 w-full resize-y font-mono text-sm leading-6"
          value={text}
          onChange={(event) => {
            setText(event.target.value);
            setUnmatched([]);
          }}
          placeholder={'Legend:\n1 Irelia, Blade Dancer\n\nChampion:\n1 Irelia, Fervent\n\nMainDeck:\n3 Defy'}
          autoFocus
        />
        {preview ? (
          <div className="mt-4 border border-line bg-ink px-3 py-3 text-sm text-slate-300">
            <p className="font-semibold text-white">Import preview</p>
            <div className="mt-2 grid grid-cols-2 gap-2 text-xs sm:grid-cols-5">
              <span className="badge">Legend {preview.legendCardId ? '1' : '0'}</span>
              <span className="badge">Chosen Champion {previewCounts.champion}</span>
              <span className="badge">Main {previewCounts.main}</span>
              <span className="badge">Runes {previewCounts.runes}</span>
              <span className="badge">Battlefields {previewCounts.battlefields}</span>
            </div>
            {preview.skippedSideboard > 0 ? <p className="mt-2 text-xs text-slate-400">Skipped {preview.skippedSideboard} sideboard cards.</p> : null}
            {preview.unmatched.length > 0 ? (
              <p className="mt-2 text-xs text-ember">Unresolved cards will be listed below. Check spelling, punctuation, and alternate card names.</p>
            ) : null}
          </div>
        ) : null}
        {unmatched.length > 0 ? (
          <div className="mt-4 max-h-32 overflow-auto border border-ember/50 px-3 py-2 text-sm text-ember">
            <p className="font-semibold">Could not match:</p>
            {unmatched.map((line) => (
              <p className="mt-1" key={line}>
                {line}
              </p>
            ))}
          </div>
        ) : null}
        <div className="mt-4 flex justify-end gap-3">
          <button className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary" onClick={handleImport} disabled={!text.trim()}>
            Import deck
          </button>
        </div>
      </section>
    </div>
  );
}

function DeckMetric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="border border-line bg-ink px-2 py-3">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 truncate text-sm font-semibold text-white">{value}</p>
    </div>
  );
}

function ValidationReport({ validation }: { validation: ReturnType<typeof validateDeck> }) {
  const sections = [
    {
      label: 'Illegal',
      tone: 'bad' as const,
      note: '',
      items: validation.messages,
    },
    {
      label: 'Banned',
      tone: 'bad' as const,
      note: 'Not legal in constructed.',
      items: validation.bannedCards.map((card) => card.name),
    },
    {
      label: 'Unsupported',
      tone: 'bad' as const,
      note: 'Blocked in enforced play.',
      items: validation.unsupportedCards.map(({ card, reason }) => `${card.name}: ${reason}`),
    },
    {
      label: 'Partial',
      tone: 'warn' as const,
      note: 'Playable for alpha testing, but rules may be incomplete.',
      items: validation.partialCards.map(({ card }) => card.name),
    },
    {
      label: 'Missing data',
      tone: 'bad' as const,
      note: 'Not available in supported-only mode yet.',
      items: validation.missingCardIds,
    },
  ].filter((section) => section.items.length > 0);

  if (sections.length === 0) {
    return (
      <div className="mt-3 border border-mint/30 bg-ink px-3 py-3 text-sm text-mint">
        Legal for current constructed validation. All cards have a support status.
      </div>
    );
  }

  return (
    <div className="mt-3 space-y-2">
      {sections.map((section) => (
        <div
          className={cx(
            'border bg-ink px-3 py-3 text-xs leading-5',
            section.tone === 'bad' ? 'border-ember/50 text-ember' : 'border-forge/50 text-forge',
          )}
          key={section.label}
        >
          <p className="font-semibold">{section.label}</p>
          {section.note ? <p className="mt-1 text-slate-400">{section.note}</p> : null}
          <p className="mt-1">{section.items.slice(0, 5).join(' ')}</p>
          {section.items.length > 5 ? <p className="mt-1">+{section.items.length - 5} more</p> : null}
        </div>
      ))}
    </div>
  );
}
