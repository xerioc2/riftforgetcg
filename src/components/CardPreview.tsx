import type { RiftCard } from '../types';

export function CardPreview({ card }: { card: RiftCard | null }) {
  if (!card) return null;

  return (
    <aside className="pointer-events-none absolute bottom-3 left-3 z-40 grid w-[min(420px,calc(100vw-24px))] grid-cols-[150px_minmax(0,1fr)] gap-4 border border-forge/50 bg-panel/98 p-3 shadow-glow">
      <div className="flex aspect-[5/7] items-center justify-center overflow-hidden bg-slate-950">
        {card.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt="" /> : <span className="px-3 text-center text-sm text-slate-400">{card.name}</span>}
      </div>
      <div className="min-w-0">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-base font-semibold text-white">{card.name}</h2>
          {card.cost !== undefined ? <span className="badge text-forge">{card.cost}</span> : null}
        </div>
        <p className="mt-1 text-xs uppercase text-slate-400">{[card.type, card.rarity].filter(Boolean).join(' / ')}</p>
        <div className="mt-3 flex flex-wrap gap-1">
          {card.domains.map((domain) => (
            <span className="badge border-mint/30 text-mint" key={domain}>
              {domain}
            </span>
          ))}
        </div>
        {card.rulesText ? <p className="mt-3 max-h-44 overflow-hidden whitespace-pre-wrap text-sm leading-5 text-slate-200">{card.rulesText}</p> : <p className="mt-3 text-sm text-slate-500">No rules text.</p>}
      </div>
    </aside>
  );
}
