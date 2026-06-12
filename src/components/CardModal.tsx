import type { RiftCard } from '../types';
import { cardSupportStatus } from '../lib/deckSupport';

const supportBadgeClass: Record<string, string> = {
  SUPPORTED: 'border-mint/40 text-mint',
  PARTIAL: 'border-forge/50 text-forge',
  UNSUPPORTED: 'border-ember/50 text-ember',
  BANNED: 'border-ember/50 text-ember',
  NOT_AUDITED: 'border-slate-500 text-slate-400',
};

export function CardModal({ card, onClose }: { card: RiftCard | null; onClose: () => void }) {
  if (!card) return null;
  const support = cardSupportStatus(card);

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 px-4 py-8" onClick={onClose}>
      <article className="grid max-h-[90vh] w-full max-w-4xl gap-5 overflow-auto border border-line bg-panel p-4 shadow-glow md:grid-cols-[340px_minmax(0,1fr)]" onClick={(event) => event.stopPropagation()}>
        <div className="flex aspect-[5/7] items-center justify-center bg-slate-950">
          {card.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt={card.name} /> : <p className="px-6 text-center text-slate-400">{card.name}</p>}
        </div>
        <div className="min-w-0">
          <div className="flex items-start justify-between gap-3">
            <h2 className="text-2xl font-semibold text-white">{card.name}</h2>
            {card.cost !== undefined ? <span className="badge text-base">{card.cost}</span> : null}
          </div>
          <p className="mt-2 text-xs uppercase tracking-wide text-slate-400">{[card.type, card.rarity, card.set].filter(Boolean).join(' / ')}</p>
          <span className={`mt-3 inline-flex border px-2 py-1 text-xs font-semibold uppercase tracking-wide ${supportBadgeClass[support.status]}`} title={support.reason}>
            {support.status.replace('_', ' ')}
          </span>
          <p className="mt-2 text-xs text-slate-400">{support.reason}</p>
          {card.champion ? <p className="mt-4 text-sm text-slate-300">Champion: {card.champion}</p> : null}
          <div className="mt-4 flex flex-wrap gap-2">
            {card.domains.map((domain) => (
              <span className="badge border-mint/30 text-mint" key={domain}>
                {domain}
              </span>
            ))}
          </div>
          {card.premiumCost !== undefined ? <p className="mt-4 text-sm text-forge">Premium cost: {card.premiumCost}</p> : null}
          {card.rulesText ? <p className="mt-5 whitespace-pre-wrap text-sm leading-6 text-slate-200">{card.rulesText}</p> : null}
          {card.flavorText ? <p className="mt-5 border-l border-forge/60 pl-4 text-sm italic leading-6 text-slate-400">{card.flavorText}</p> : null}
          <button className="btn-secondary mt-6" onClick={onClose}>
            Close
          </button>
        </div>
      </article>
    </div>
  );
}
