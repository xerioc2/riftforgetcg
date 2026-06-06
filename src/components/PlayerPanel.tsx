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
}

export function PlayerPanel({ name, score, handCount, energy, untappedRunes, isActive, isMe, effectiveEnergy, bottom = 224 }: PlayerPanelProps) {
  const pendingEnergy = effectiveEnergy != null ? effectiveEnergy - energy : 0;

  return (
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
    </div>
  );
}
