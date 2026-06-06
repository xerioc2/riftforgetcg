import type { DeckCard, RiftCard } from '../types';

export function ManaCurve({ entries }: { entries: Array<DeckCard & { card: RiftCard }> }) {
  if (entries.length === 0) return null;

  const buckets = Array.from({ length: 8 }, (_, cost) =>
    entries.filter((entry) => (entry.card.cost ?? 0) >= 7 ? cost === 7 : (entry.card.cost ?? 0) === cost).reduce((sum, entry) => sum + entry.quantity, 0),
  );
  const max = Math.max(...buckets, 1);

  return (
    <div className="mt-4 border border-line bg-ink p-3">
      <h3 className="text-sm font-semibold text-white">Mana curve</h3>
      <div className="mt-3 grid h-32 grid-cols-8 items-end gap-2">
        {buckets.map((count, index) => (
          <div className="flex h-full flex-col justify-end gap-2" key={index}>
            <span className="h-4 text-center text-xs text-slate-400">{count > 0 ? count : ''}</span>
            <div className="flex h-24 items-end border border-line bg-panel">
              <div className="w-full bg-forge" style={{ height: `${(count / max) * 100}%` }} />
            </div>
            <span className="text-center text-xs text-slate-500">{index === 7 ? '7+' : index}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
