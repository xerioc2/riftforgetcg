import { useState } from 'react';
import { unsupportedCardReason } from '../lib/cardActions';
import { keywordDescription } from '../lib/cardKeywords';
import type { CardInstance, RiftCard } from '../types';

export function CardPreview({ card, instance, onInspect }: { card: RiftCard | null; instance?: CardInstance; onInspect?: (card: CardInstance) => void }) {
  const [position, setPosition] = useState({ x: 12, y: 12 });

  if (!card) return null;
  const unsupportedReason = unsupportedCardReason(card);

  return (
    <aside className="group pointer-events-auto absolute z-40 w-[min(420px,calc(100vw-24px))] border border-forge/50 bg-panel/98 shadow-glow" style={{ left: position.x, bottom: position.y }}>
      <div
        className="flex h-5 cursor-move items-center bg-line/60 px-2 text-xs text-slate-500"
        onMouseDown={(event) => {
          event.preventDefault();
          const startX = event.clientX;
          const startY = event.clientY;
          const startPosition = position;
          const onMove = (moveEvent: MouseEvent) => {
            setPosition({
              x: Math.max(0, Math.min(window.innerWidth - 440, startPosition.x + moveEvent.clientX - startX)),
              y: Math.max(0, Math.min(window.innerHeight - 300, startPosition.y + startY - moveEvent.clientY)),
            });
          };
          const onUp = () => {
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup', onUp);
          };
          window.addEventListener('mousemove', onMove);
          window.addEventListener('mouseup', onUp);
        }}
      >
        ...
        {instance && onInspect ? (
          <button
            className="icon-btn ml-auto min-h-5 px-1 text-xs opacity-0 transition-opacity group-hover:opacity-100"
            title="Inspect card"
            aria-label="Inspect card"
            onMouseDown={(event) => event.stopPropagation()}
            onClick={() => onInspect(instance)}
          >
            &#9432;
          </button>
        ) : null}
      </div>
      <div className="grid grid-cols-[150px_minmax(0,1fr)] gap-4 p-3">
        <div className="flex aspect-[5/7] items-center justify-center overflow-hidden bg-slate-950">
          {card.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt="" /> : <span className="px-3 text-center text-sm text-slate-400">{card.name}</span>}
        </div>
        <div className="min-w-0">
          <div className="flex items-start justify-between gap-2">
            <h2 className="text-base font-semibold text-white">{card.name}</h2>
            {card.cost !== undefined ? <span className="badge text-forge">{card.cost}</span> : null}
          </div>
          <p className="mt-1 text-xs uppercase text-slate-400">{[card.type, card.rarity].filter(Boolean).join(' / ')}</p>
          {card.power != null && card.health != null ? (
            <p className="mt-2 text-xs font-semibold text-slate-300">
              Might {card.power}
              {(instance?.mightBonus ?? 0) > 0 ? ` (+${instance?.mightBonus})` : ''} / Guard {instance?.currentHealth ?? card.health}/{card.health}
            </p>
          ) : null}
          {['Spell', 'Gear'].includes(card.type) ? (
            <p className={`mt-2 text-xs font-semibold ${unsupportedReason ? 'text-ember' : 'text-mint'}`}>
              {unsupportedReason ?? 'No blocked effect detected for alpha play'}
            </p>
          ) : null}
          <div className="mt-3 flex flex-wrap gap-1">
            {card.domains.map((domain) => (
              <span className="badge border-mint/30 text-mint" key={domain}>
                {domain}
              </span>
            ))}
          </div>
          {card.rulesText ? <p className="mt-3 max-h-44 overflow-hidden whitespace-pre-wrap text-sm leading-5 text-slate-200">{card.rulesText}</p> : <p className="mt-3 text-sm text-slate-500">No rules text.</p>}
          {card.keywords?.length ? (
            <div className="mt-3 space-y-1">
              {card.keywords.map((keyword) => (
                <div key={keyword} className="flex items-start gap-2 text-xs">
                  <span className="badge border-forge/40 text-forge">{keyword}</span>
                  <span className="text-slate-400">{keywordDescription(keyword)}</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </div>
    </aside>
  );
}
