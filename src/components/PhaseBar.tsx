const PHASES = [
  { key: 'CHANNEL', label: 'Channel' },
  { key: 'MAIN', label: 'Main' },
  { key: 'ATTACK_DECLARE', label: 'Attack' },
  { key: 'BLOCK_DECLARE', label: 'Block' },
  { key: 'COMBAT_RESOLVE', label: 'Resolve' },
  { key: 'END', label: 'End' },
] as const;

interface PhaseBarProps {
  currentPhase: string;
  isMyTurn: boolean;
  canPass: boolean;
  opponentName: string;
  onPassPhase: () => void;
}

export function PhaseBar({ currentPhase, isMyTurn, canPass, opponentName, onPassPhase }: PhaseBarProps) {
  return (
    <div className="pointer-events-auto absolute bottom-[172px] left-0 z-20 flex h-12 items-center gap-1 border-t border-line bg-panel/95 px-3" style={{ right: '280px' }}>
      <div className="flex min-w-0 overflow-x-auto">
        {PHASES.map((phase) => (
          <span className={`shrink-0 px-3 py-1 text-xs font-semibold transition-colors ${currentPhase === phase.key ? 'bg-forge text-ink' : 'text-slate-500'}`} key={phase.key}>
            {phase.label}
          </span>
        ))}
      </div>
      <div className="ml-auto flex items-center gap-3">
        <span className="hidden whitespace-nowrap text-xs text-slate-400 lg:inline">
          {currentPhase === 'BLOCK_DECLARE' && canPass ? 'Your response' : isMyTurn ? 'Your turn' : `${opponentName}'s turn`}
        </span>
        <button className="btn-primary min-h-7 px-4 py-1 text-xs disabled:opacity-40" disabled={!canPass} onClick={onPassPhase}>
          Pass
        </button>
      </div>
    </div>
  );
}
