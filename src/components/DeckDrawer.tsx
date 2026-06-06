import type { RiftCard } from '../types';

export function DeckDrawer({
  deckCards,
  onDeal,
  onDrawHand,
  onHover,
  embedded = false,
}: {
  deckCards: Array<{ card: RiftCard; remaining: number }>;
  onDeal: (card: RiftCard) => void;
  onDrawHand: () => void;
  onHover: (card: RiftCard | null) => void;
  embedded?: boolean;
}) {
  return (
    <section className={`pointer-events-auto bg-panel/95 ${embedded ? 'flex h-full flex-col' : 'absolute bottom-3 right-3 w-72 border border-line shadow-glow'}`}>
      <div className="flex items-center justify-between border-b border-line px-3 py-2">
        <h2 className="text-sm font-semibold text-white">Deck</h2>
        <button className="btn-secondary min-h-8 px-2 py-1 text-xs" onClick={onDrawHand}>
          Draw hand
        </button>
      </div>
      <div className={`${embedded ? 'min-h-0 flex-1' : 'max-h-64'} overflow-auto`}>
        {deckCards.map(({ card, remaining }) => (
          <button
            className="grid w-full grid-cols-[32px_minmax(0,1fr)] gap-2 border-b border-line px-3 py-2 text-left text-sm last:border-b-0 hover:bg-ink"
            key={card.id}
            onClick={() => onDeal(card)}
            onMouseEnter={() => onHover(card)}
            onMouseLeave={() => onHover(null)}
          >
            <span className="font-semibold text-forge">{remaining}</span>
            <span className="truncate text-slate-200">{card.name}</span>
          </button>
        ))}
        {deckCards.length === 0 ? <p className="px-3 py-6 text-sm text-slate-500">No remaining cards.</p> : null}
      </div>
    </section>
  );
}
