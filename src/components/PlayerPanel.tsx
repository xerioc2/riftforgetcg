import type { RevealedHandSnapshot, RiftCard } from '../types';

type VisibleCard = { instanceId: string; card: RiftCard };

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
  runeDeckCount: number;
  discardCards: VisibleCard[];
  revealedSnapshot?: RevealedHandSnapshot;
  revealedCards: VisibleCard[];
  onDismissRevealed: (instanceId: string) => void;
  onHover?: (card: RiftCard | null) => void;
  cardScale?: number;
  onCardScaleChange?: (value: number) => void;
  onLeave?: () => void;
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
  runeDeckCount,
  discardCards,
  revealedSnapshot,
  revealedCards,
  onDismissRevealed,
  onHover,
  cardScale = 1.3,
  onCardScaleChange,
  onLeave,
}: PlayerPanelProps) {
  const pendingEnergy = effectiveEnergy != null ? effectiveEnergy - energy : 0;
  const visibleRevealed = revealedCards.filter(({ instanceId }) => !revealedSnapshot?.dismissedInstanceIds.includes(instanceId));

  return (
      <div
        className={`pointer-events-auto absolute ${isMe ? 'left-2' : 'left-2 top-2'} z-20 w-48 border bg-panel/95 p-2 shadow-glow ${isActive ? 'border-forge' : 'border-line'}`}
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
          <span className="badge text-slate-400">Rune {runeDeckCount}</span>
          <span className="badge text-slate-400">Trash {discardCards.length}</span>
        </div>
        {discardCards.length > 0 ? (
          <div className="mt-2 max-h-40 overflow-y-auto border-t border-line pt-2">
            <p className="mb-1 text-[10px] font-semibold uppercase text-slate-500">Trash</p>
            {discardCards.map(({ instanceId, card }) => (
              <div
                key={instanceId}
                className="flex items-center gap-2 py-0.5 text-[11px] text-slate-300 hover:text-white"
                title={card.name}
                onMouseEnter={() => onHover?.(card)}
                onMouseLeave={() => onHover?.(null)}
              >
                <span className="shrink-0 text-[9px] text-slate-500">{card.type}</span>
                <span className="truncate">{card.name}</span>
              </div>
            ))}
          </div>
        ) : null}
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
        {isMe ? (
          <div className="mt-2 flex items-center gap-2 border-t border-line pt-2">
            <input
              id="card-size"
              type="range"
              min="0.8"
              max="2"
              step="0.1"
              value={cardScale}
              onChange={(event) => onCardScaleChange?.(Number(event.target.value))}
              className="flex-1 accent-forge"
              title="Card size"
            />
            <button className="btn-secondary min-h-6 px-2 py-0.5 text-xs" onClick={onLeave}>
              Leave
            </button>
          </div>
        ) : null}
      </div>
  );
}
