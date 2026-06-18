import { useState } from 'react';
import { unsupportedCardReason } from '../lib/cardActions';
import { cardDisplayStats } from '../lib/cardDisplayStats';
import { keywordDescription } from '../lib/cardKeywords';
import { cardSupportStatus } from '../lib/deckSupport';
import type { CardInstance, RiftCard } from '../types';

const supportBadgeClass: Record<string, string> = {
  SUPPORTED: 'border-mint/40 text-mint',
  PARTIAL: 'border-forge/50 text-forge',
  UNSUPPORTED: 'border-ember/50 text-ember',
  BANNED: 'border-ember/50 text-ember',
  NOT_AUDITED: 'border-slate-500 text-slate-400',
};

export function CardPreview({ card, instance, onInspect }: { card: RiftCard | null; instance?: CardInstance; onInspect?: (card: CardInstance) => void }) {
  const [position, setPosition] = useState({ x: 12, y: 12 });

  if (!card) return null;
  const unsupportedReason = unsupportedCardReason(card);
  const support = cardSupportStatus(card);
  const stats = cardDisplayStats(card, instance);

  return (
    <aside
      className="group pointer-events-auto absolute z-40 w-[min(440px,calc(100vw-24px))] overflow-hidden border border-forge/70 bg-[#05080d] text-slate-100 shadow-[0_18px_60px_rgba(0,0,0,0.78)] ring-1 ring-white/5"
      style={{ left: position.x, bottom: position.y }}
    >
      <div
        className="flex h-6 cursor-move items-center border-b border-forge/20 bg-[#10151d] px-2 text-xs text-slate-400"
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
      <div className="grid grid-cols-[150px_minmax(0,1fr)] gap-4 p-4">
        <div className="flex aspect-[5/7] items-center justify-center overflow-hidden border border-slate-700/70 bg-slate-950 shadow-[0_10px_30px_rgba(0,0,0,0.45)]">
          {card.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt="" /> : <span className="px-3 text-center text-sm text-slate-400">{card.name}</span>}
        </div>
        <div className="min-w-0">
          <div className="flex items-start justify-between gap-2">
            <h2 className="text-base font-semibold leading-5 text-white">{card.name}</h2>
            {card.cost !== undefined ? <span className="badge text-forge">{card.cost}</span> : null}
          </div>
          <p className="mt-1 text-xs uppercase tracking-wide text-slate-400">{[card.type, card.rarity].filter(Boolean).join(' / ')}</p>
          <span className={`mt-2 inline-flex border px-2 py-1 text-xs font-semibold uppercase tracking-wide ${supportBadgeClass[support.status]}`} title={support.reason}>
            {support.status.replace('_', ' ')}
          </span>
          {stats.hasCombatStats ? (
            <div className="mt-3 grid grid-cols-2 gap-2 text-xs font-semibold text-slate-200">
              <span className={`border px-2 py-1 ${stats.mightModified ? 'border-forge/60 bg-[#161307] text-forge' : 'border-slate-700/80 bg-[#0b1017]'}`}>
                {stats.mightLabel}
              </span>
              <span className="border border-slate-700/80 bg-[#0b1017] px-2 py-1">
                {stats.healthLabel}
              </span>
              {stats.damageLabel ? <span className="col-span-2 border border-ember/60 bg-[#1a0d0a] px-2 py-1 text-ember">{stats.damageLabel}</span> : null}
            </div>
          ) : null}
          {['Spell', 'Gear'].includes(card.type) ? (
            <p className={`mt-3 border border-slate-700/70 bg-[#0b1017] px-2 py-2 text-xs font-semibold leading-5 ${unsupportedReason ? 'text-ember' : 'text-mint'}`}>
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
          {card.rulesText ? (
            <p className="mt-3 max-h-52 overflow-y-auto whitespace-pre-wrap border border-slate-700/70 bg-[#080c12] p-3 text-sm leading-6 text-slate-100">
              {card.rulesText}
            </p>
          ) : (
            <p className="mt-3 border border-slate-700/70 bg-[#080c12] p-3 text-sm text-slate-500">No rules text.</p>
          )}
          {card.keywords?.length ? (
            <div className="mt-3 space-y-2 border-t border-slate-700/70 pt-3">
              {card.keywords.map((keyword) => (
                <div key={keyword} className="flex items-start gap-2 border border-slate-700/60 bg-[#080c12] px-2 py-2 text-xs">
                  <span className="badge border-forge/40 text-forge">{keyword}</span>
                  <span className="leading-5 text-slate-300">{keywordDescription(keyword)}</span>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </div>
    </aside>
  );
}
