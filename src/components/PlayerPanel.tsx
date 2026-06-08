import { useState } from 'react';
import { TrashModal, type VisibleCard } from './TrashModal';
import type { RevealedHandSnapshot } from '../types';

interface PlayerPanelProps {
  name: string;
  score: number;
  handCount: number;
  energy: number;
  untappedRunes: number;
  isActive: boolean;
  isMe: boolean;
  effectiveEnergy?: number;
  bottom?: number;
  deckCount: number;
  discardCards: VisibleCard[];
  revealedSnapshot?: RevealedHandSnapshot;
  revealedCards: VisibleCard[];
  onDismissRevealed: (instanceId: string) => void;
}

export function PlayerPanel({
  name,
  score,
  handCount,
  energy,
  untappedRunes,
  isActive,
  isMe,
  effectiveEnergy,
  bottom = 224,
  deckCount,
  discardCards,
  revealedSnapshot,
  revealedCards,
  onDismissRevealed,
}: PlayerPanelProps) {
  const [trashOpen, setTrashOpen] = useState(false);
  const pendingEnergy = effectiveEnergy != null ? effectiveEnergy - energy : 0;
  const visibleRevealed = revealedCards.filter(({ instanceId }) => !revealedSnapshot?.dismissedInstanceIds.includes(instanceId));

  return (
    <>
      <div
        className={`pointer-events-auto absolute ${isMe ? 'left-2' : 'left-2 top-2'} z-30 w-48 border bg-panel/95 p-2 shadow-glow ${isActive ? 'border-forge' : 'border-line'}`}
        style={isMe ? { bottom } : undefined}
      >
        <div className="flex items-center justify-between gap-2">
          <span className="truncate text-sm font-bold text-white">{name}</span>
          {isActive ? <span className="shrink-0 text-[10px] font-semibold text-forge">ACTIVE</span> : null}
        </div>
        <div className="mt-1 flex items-center gap-1">
          <span className="min-w-[2rem] text-center text-2xl font-bold text-forge">{score}</span>
          <span className="ml-auto flex gap-2 text-xs text-slate-400">
            <span title="Cards in hand">Hand {handCount}</span>
            <span className={pendingEnergy > 0 ? 'text-mint' : undefined} title="Available energy">
              Energy {energy}
              {pendingEnergy > 0 ? `+${pendingEnergy}` : ''}
            </span>
            <span className="text-forge" title="Untapped runes">
              Runes {untappedRunes}
            </span>
          </span>
        </div>
        <div className="mt-2 flex gap-1 border-t border-line pt-2 text-[11px]">
          <span className="badge text-slate-400">Deck {deckCount}</span>
          <button className="badge cursor-pointer text-slate-300 hover:border-forge hover:text-forge" onClick={() => setTrashOpen(true)}>
            Trash {discardCards.length}
          </button>
        </div>
        {visibleRevealed.length ? (
          <div className="mt-2 border-t border-line pt-2">
            <p className="text-[10px] font-semibold uppercase text-ember">Revealed hand</p>
            <div className="mt-1 flex gap-1 overflow-x-auto pb-1">
              {visibleRevealed.map(({ instanceId, card }) => (
                <div className="relative w-10 shrink-0" key={instanceId} title={card.name}>
                  <div className="aspect-[5/7] overflow-hidden border border-ember/60 bg-ink">
                    {card.imageUrl ? <img className="h-full w-full object-contain" src={card.imageUrl} alt={card.name} /> : null}
                  </div>
                  <button
                    className="absolute -right-1 -top-1 grid h-4 w-4 place-items-center border border-line bg-panel text-[9px] text-slate-300 hover:text-ember"
                    aria-label={`Dismiss ${card.name}`}
                    onClick={() => onDismissRevealed(instanceId)}
                  >
                    X
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>
      {trashOpen ? <TrashModal cards={discardCards} ownerName={name} onClose={() => setTrashOpen(false)} /> : null}
    </>
  );
}
