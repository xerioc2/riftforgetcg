import { useEffect } from 'react';
import { KEYWORD_DESCRIPTIONS } from '../lib/cardKeywords';
import type { CardInstance, RiftCard } from '../types';

export function CardInspectModal({ card, cardDef, onClose }: { card: CardInstance; cardDef: RiftCard | undefined; onClose: () => void }) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const mightBonus = card.mightBonus ?? 0;
  const baseMight = cardDef?.power ?? 0;
  const maxHealth = cardDef?.health ?? 0;

  return (
    <div className="fixed inset-0 z-[100] grid place-items-center overflow-y-auto bg-black/70 p-4" onMouseDown={onClose}>
      <section className="w-full max-w-[380px] border border-line bg-panel p-4 shadow-glow" onMouseDown={(event) => event.stopPropagation()}>
        <div className="flex aspect-[5/7] items-center justify-center overflow-hidden bg-slate-950">
          {cardDef?.imageUrl ? <img className="h-full w-full object-contain" src={cardDef.imageUrl} alt={cardDef.name} /> : <span className="text-slate-500">No card image</span>}
        </div>
        <div className="mt-4 flex items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-white">{cardDef?.name ?? 'Unknown card'}</h2>
            <div className="mt-2 flex flex-wrap gap-1">
              {cardDef?.type ? <span className="badge text-forge">{cardDef.type}</span> : null}
              {cardDef?.domains.map((domain) => <span className="badge border-mint/30 text-mint" key={domain}>{domain}</span>)}
            </div>
          </div>
          {cardDef?.cost != null ? <span className="badge text-forge">Cost {cardDef.cost}</span> : null}
        </div>
        <div className="mt-4 grid grid-cols-2 gap-2 border-y border-line py-3 text-sm font-semibold text-slate-200">
          <span>Might {baseMight + mightBonus}{mightBonus > 0 ? ` (+${mightBonus})` : ''}</span>
          <span>Health {card.currentHealth ?? maxHealth}/{maxHealth}</span>
        </div>
        <div className="mt-4">
          <h3 className="text-xs font-semibold uppercase text-slate-500">Rules</h3>
          <p className={`mt-2 whitespace-pre-wrap text-sm leading-5 ${cardDef?.rulesText ? 'text-slate-200' : 'text-slate-500'}`}>{cardDef?.rulesText || 'No effect text.'}</p>
        </div>
        {cardDef?.keywords?.length ? (
          <div className="mt-4 space-y-2">
            <h3 className="text-xs font-semibold uppercase text-slate-500">Keywords</h3>
            {cardDef.keywords.map((keyword) => (
              <div key={keyword} className="border-l-2 border-forge/50 pl-3">
                <span className="badge border-forge/40 text-forge">{keyword}</span>
                <p className="mt-1 text-xs text-slate-400">{KEYWORD_DESCRIPTIONS[keyword.toUpperCase()] ?? 'Card keyword.'}</p>
              </div>
            ))}
          </div>
        ) : null}
        <div className="mt-5 flex justify-end">
          <button className="btn-secondary" onClick={onClose}>Close</button>
        </div>
      </section>
    </div>
  );
}
