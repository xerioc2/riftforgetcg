import { useState } from 'react';
import type { CardInstance, RiftCard } from '../types';

export function HandRack({
  instances,
  cards,
  onPlay,
  onDiscard,
  cardScale,
  onHover,
  embedded = false,
}: {
  instances: CardInstance[];
  cards: Map<string, RiftCard>;
  onPlay: (instanceId: string) => void;
  onDiscard: (instanceId: string) => void;
  cardScale: number;
  onHover: (card: RiftCard | null) => void;
  embedded?: boolean;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [hovered, setHovered] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const cardWidth = Math.round(64 * cardScale);
  const cardHeight = Math.round(96 * cardScale);
  const availableWidth = Math.max(240, window.innerWidth - 380);
  const spacing =
    instances.length > 1
      ? Math.min(cardWidth + 4, Math.max(44, (availableWidth - cardWidth) / (instances.length - 1)))
      : cardWidth + 4;
  const totalWidth = instances.length > 0 ? spacing * (instances.length - 1) + cardWidth : 0;

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
    <div className={`pointer-events-auto relative border-t border-line bg-panel/95 ${embedded ? 'w-full' : 'absolute bottom-0 left-0 right-[280px]'}`} style={{ height: cardHeight + 44 }}>
      <div className="relative mx-auto" style={{ width: Math.min(totalWidth, availableWidth), height: cardHeight + 32 }}>
        {instances.map((instance, index) => {
          const card = cards.get(instance.cardId);
          const isHovered = hovered === instance.instanceId;
          const isSelected = selected === instance.instanceId;
          return (
            <button
              key={instance.instanceId}
              className="absolute bottom-6 overflow-hidden border transition-transform"
              style={{
                width: cardWidth,
                height: cardHeight,
                left: index * spacing,
                transform: isHovered || isSelected ? 'translateY(-28px)' : 'translateY(0)',
                borderColor: isSelected ? '#d8b05d' : '#2b333d',
                borderWidth: isSelected ? 2 : 1,
                zIndex: isHovered || isSelected ? 50 : index,
                borderRadius: 4,
                background: '#101418',
              }}
              onClick={() => {
                if (isSelected) {
                  onPlay(instance.instanceId);
                  setSelected(null);
                } else {
                  setSelected(instance.instanceId);
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
              {card?.imageUrl ? <img className="h-full w-full object-cover" src={card.imageUrl} alt={card.name} /> : <span className="block p-1 text-xs text-slate-300">{card?.name ?? '?'}</span>}
            </button>
          );
        })}
      </div>
      {instances.length === 0 ? <div className="py-2 text-center text-xs text-slate-500">Hand empty</div> : null}
      <button className="icon-btn absolute bottom-1 right-2 min-h-6 text-xs" onClick={() => setCollapsed(true)} aria-label="Collapse hand">
        v
      </button>
    </div>
  );
}
