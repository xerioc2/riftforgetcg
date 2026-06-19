import { useEffect } from 'react';
import { cardDisplayStats } from '../lib/cardDisplayStats';
import { keywordDescription } from '../lib/cardKeywords';
import { cardSupportStatus } from '../lib/deckSupport';
import { equipCostForCard, equipCostLabel, isEquipRulesText } from '../lib/equipment';
import type { CardInstance, RiftCard } from '../types';

const supportBadgeClass: Record<string, string> = {
  SUPPORTED: 'border-mint/40 text-mint',
  PARTIAL: 'border-forge/50 text-forge',
  UNSUPPORTED: 'border-ember/50 text-ember',
  BANNED: 'border-ember/50 text-ember',
  NOT_AUDITED: 'border-slate-500 text-slate-400',
};

export function CardInspectModal({
  card,
  cardDef,
  allInstances,
  cardsById,
  onClose,
}: {
  card: CardInstance;
  cardDef: RiftCard | undefined;
  allInstances?: CardInstance[];
  cardsById?: Map<string, RiftCard>;
  onClose: () => void;
}) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const stats = cardDisplayStats(cardDef, card, { allInstances, cardsById });
  const support = cardSupportStatus(cardDef);
  const equipCost = isEquipRulesText(cardDef) ? equipCostForCard(cardDef) : null;

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
              <span className={`badge ${supportBadgeClass[support.status]}`} title={support.reason}>{support.status.replace('_', ' ')}</span>
            </div>
          </div>
          {cardDef?.cost != null ? <span className="badge text-forge">Cost {cardDef.cost}</span> : null}
        </div>
        {stats.hasCombatStats ? (
          <div className="mt-4 grid grid-cols-2 gap-2 border-y border-line py-3 text-sm font-semibold text-slate-200">
            <span className={stats.mightModified ? 'text-forge' : undefined}>{stats.mightLabel}</span>
            <span>{stats.healthLabel}</span>
            {stats.damageLabel ? <span className="col-span-2 text-ember">{stats.damageLabel}</span> : null}
            {stats.equipmentModifierLabels.length ? <span className="col-span-2 text-forge">{stats.equipmentModifierLabels.join('; ')}</span> : null}
          </div>
        ) : null}
        {equipCost ? (
          <div className="mt-4 border border-forge/35 bg-[#130f06] px-3 py-2 text-sm font-semibold text-forge">
            Equip cost: {equipCostLabel(equipCost)}
          </div>
        ) : null}
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
                <p className="mt-1 text-xs text-slate-400">{keywordDescription(keyword)}</p>
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
