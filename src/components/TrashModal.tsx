import type { RiftCard } from '../types';

export type VisibleCard = { instanceId: string; card: RiftCard };

export function TrashModal({ cards, ownerName, onClose }: { cards: VisibleCard[]; ownerName: string; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-[100] grid place-items-center bg-black/75 p-5" onMouseDown={onClose}>
      <section className="max-h-[85vh] w-full max-w-5xl overflow-auto border border-line bg-panel p-5 shadow-glow" onMouseDown={(event) => event.stopPropagation()}>
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-forge">{ownerName}&apos;s Trash</h2>
            <p className="mt-1 text-xs text-slate-500">{cards.length} card{cards.length === 1 ? '' : 's'}</p>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close trash">
            X
          </button>
        </div>
        {cards.length ? (
          <div className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-6">
            {cards.map(({ instanceId, card }) => (
              <figure key={instanceId} className="min-w-0">
                <div className="aspect-[5/7] overflow-hidden border border-line bg-ink">
                  {card.imageUrl ? <img className="h-full w-full object-contain" src={card.imageUrl} alt={card.name} /> : null}
                </div>
                <figcaption className="mt-1 truncate text-xs font-medium text-slate-300">{card.name}</figcaption>
              </figure>
            ))}
          </div>
        ) : (
          <p className="mt-8 text-center text-sm text-slate-500">Trash is empty.</p>
        )}
      </section>
    </div>
  );
}
