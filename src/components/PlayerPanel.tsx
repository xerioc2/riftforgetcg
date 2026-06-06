interface PlayerPanelProps {
  name: string;
  score: number;
  handCount: number;
  energy: number;
  untappedRunes: number;
  isActive: boolean;
  isMe: boolean;
}

export function PlayerPanel({ name, score, handCount, energy, untappedRunes, isActive, isMe }: PlayerPanelProps) {
  return (
    <div className={`pointer-events-auto absolute ${isMe ? 'bottom-[192px] left-2' : 'left-2 top-2'} z-10 w-48 border bg-panel/95 p-2 shadow-glow ${isActive ? 'border-forge' : 'border-line'}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="truncate text-sm font-bold text-white">{name}</span>
        {isActive ? <span className="shrink-0 text-[10px] font-semibold text-forge">ACTIVE</span> : null}
      </div>
      <div className="mt-1 flex items-center gap-1">
        <span className="min-w-[2rem] text-center text-2xl font-bold text-forge">{score}</span>
        <span className="ml-auto flex gap-2 text-xs text-slate-400">
          <span title="Cards in hand">Hand {handCount}</span>
          <span title="Available energy">Energy {energy}</span>
          <span className="text-forge" title="Untapped runes">
            Runes {untappedRunes}
          </span>
        </span>
      </div>
    </div>
  );
}
