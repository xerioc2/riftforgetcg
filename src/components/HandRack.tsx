import { useEffect, useMemo, useRef, useState } from 'react';
import type { CardInstance, RiftCard } from '../types';

export function HandRack({
  instances,
  cards,
  onPlay,
  onDiscard,
  cardScale,
  onHover,
  effectiveEnergy,
  canPlayCards,
  canPlayReactions = false,
  embedded = false,
  maxHeight,
}: {
  instances: CardInstance[];
  cards: Map<string, RiftCard>;
  onPlay: (instanceId: string) => void;
  onDiscard: (instanceId: string) => void;
  cardScale: number;
  onHover: (card: RiftCard | null) => void;
  effectiveEnergy: number;
  canPlayCards: boolean;
  canPlayReactions?: boolean;
  embedded?: boolean;
  maxHeight?: number;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [hovered, setHovered] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [rackWidth, setRackWidth] = useState(900);
  const rackRef = useRef<HTMLDivElement | null>(null);
  const panelHeight = maxHeight ?? 172;
  const actionBarHeight = selected ? 42 : 18;

  useEffect(() => {
    const rack = rackRef.current;
    if (!rack) return;
    const observer = new ResizeObserver(([entry]) => setRackWidth(entry.contentRect.width));
    observer.observe(rack);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (selected && !instances.some((instance) => instance.instanceId === selected)) setSelected(null);
  }, [instances, selected]);

  const dimensions = useMemo(() => {
    const count = Math.max(1, instances.length);
    const gap = 8;
    const heightLimit = Math.max(90, panelHeight - actionBarHeight - 20);
    const naturalHeight = Math.min(300, heightLimit * Math.min(1, cardScale / 1.3));
    const naturalWidth = naturalHeight * (5 / 7);
    const widthLimit = Math.max(54, (rackWidth - 32 - gap * (count - 1)) / count);
    const width = Math.min(naturalWidth, widthLimit);
    return { width, height: width * (7 / 5), gap };
  }, [actionBarHeight, cardScale, instances.length, panelHeight, rackWidth]);

  const selectedInstance = instances.find((instance) => instance.instanceId === selected);
  const selectedCard = selectedInstance ? cards.get(selectedInstance.cardId) : undefined;
  const selectedCost = selectedCard?.cost ?? 0;
  const canAffordSelected = selectedCost <= effectiveEnergy;
  const selectedIsReaction = canPlayReactions && selectedCard?.type?.toLowerCase() === 'spell';
  const canPlaySelected = canPlayCards || selectedIsReaction;
  const playSelected = () => {
    if (!selectedInstance || !canPlaySelected || !canAffordSelected) return;
    onPlay(selectedInstance.instanceId);
    setSelected(null);
  };

  if (collapsed) {
    return (
      <div className="pointer-events-auto flex h-7 items-center justify-between border-t border-line bg-panel/95 px-3">
        <span className="text-xs text-slate-400">
          {instances.length} card{instances.length !== 1 ? 's' : ''} in hand
        </span>
        <button className="icon-btn min-h-6 text-xs" onClick={() => setCollapsed(false)} aria-label="Expand hand">
          ^
        </button>
      </div>
    );
  }

  return (
    <div
      ref={rackRef}
      className={`pointer-events-auto relative overflow-hidden border-t border-line bg-panel/95 ${embedded ? 'w-full' : 'absolute bottom-0 left-0 right-[280px]'}`}
      style={{ height: panelHeight, maxHeight }}
    >
      {selectedCard ? (
        <div className="absolute inset-x-0 top-0 z-[70] flex h-10 items-center justify-center gap-3 border-b border-line bg-ink/95 px-3 text-sm">
          <span className="max-w-[260px] truncate font-semibold text-white">{selectedCard.name}</span>
          <span className={canAffordSelected ? 'text-mint' : 'text-ember'}>
            Cost {selectedCost} / Spendable {effectiveEnergy}
          </span>
          <button className="btn-primary min-h-7 px-3 py-1 text-xs" disabled={!canPlaySelected || !canAffordSelected} onClick={playSelected}>
            {selectedIsReaction ? 'Respond' : 'Play'}
          </button>
          <button className="btn-secondary min-h-7 px-3 py-1 text-xs" onClick={() => setSelected(null)}>
            Cancel
          </button>
        </div>
      ) : (
        <p className="absolute inset-x-0 top-1 text-center text-[11px] text-slate-500">Select a card, then Play. Double-click also plays.</p>
      )}

      <div
        className="absolute inset-x-4 bottom-3 flex items-end justify-center"
        style={{ gap: dimensions.gap, top: actionBarHeight }}
      >
        {instances.map((instance) => {
          const card = cards.get(instance.cardId);
          const isHovered = hovered === instance.instanceId;
          const isSelected = selected === instance.instanceId;
          const isReaction = canPlayReactions && card?.type?.toLowerCase() === 'spell';
          const isPlayable = canPlayCards || isReaction;
          return (
            <button
              key={instance.instanceId}
              className="relative shrink-0 overflow-hidden border bg-ink shadow-md transition-[transform,border-color] duration-150"
              style={{
                width: dimensions.width,
                height: dimensions.height,
                transform: isHovered || isSelected ? 'translateY(-8px)' : 'translateY(0)',
                borderColor: isSelected || isReaction ? '#d8b05d' : '#2b333d',
                borderWidth: isSelected ? 2 : 1,
                zIndex: isHovered || isSelected ? 50 : 1,
                borderRadius: 4,
                opacity: isPlayable ? 1 : 0.55,
              }}
              onClick={() => setSelected(isSelected ? null : instance.instanceId)}
              onDoubleClick={() => {
                setSelected(instance.instanceId);
                if (isPlayable && (card?.cost ?? 0) <= effectiveEnergy) {
                  onPlay(instance.instanceId);
                  setSelected(null);
                }
              }}
              onContextMenu={(event) => {
                event.preventDefault();
                onDiscard(instance.instanceId);
              }}
              onMouseEnter={() => {
                setHovered(instance.instanceId);
                onHover(card ?? null);
              }}
              onMouseLeave={() => {
                setHovered(null);
                onHover(null);
              }}
            >
              {isReaction ? <span className="absolute inset-x-0 top-0 z-10 bg-forge/90 py-0.5 text-center text-[10px] font-bold uppercase text-ink">Respond</span> : null}
              {card?.imageUrl ? <img className="h-full w-full object-contain" src={card.imageUrl} alt={card.name} /> : <span className="block p-1 text-xs text-slate-300">{card?.name ?? '?'}</span>}
            </button>
          );
        })}
      </div>
      {instances.length === 0 ? <div className="py-12 text-center text-xs text-slate-500">Hand empty</div> : null}
      <button className="icon-btn absolute bottom-1 right-2 z-[80] min-h-6 text-xs" onClick={() => setCollapsed(true)} aria-label="Collapse hand">
        v
      </button>
    </div>
  );
}
