const PHASE_LABELS: Record<string, string> = {
  MULLIGAN: 'Mulligan',
  CHANNEL: 'Channel',
  MAIN: 'Main Phase',
  ATTACK_DECLARE: 'Declare Attackers',
  BLOCK_DECLARE: 'Declare Blockers',
  COMBAT_RESOLVE: 'Combat',
  END: 'End Phase',
};

const PHASES = Object.keys(PHASE_LABELS);

interface PhaseBarProps {
  currentPhase: string;
  isMyTurn: boolean;
  canPass: boolean;
  opponentName: string;
  onPassPhase: () => void;
  bottom?: number;
}

export function PhaseBar({ currentPhase, isMyTurn, canPass, opponentName, onPassPhase, bottom = 172 }: PhaseBarProps) {
  return (
    <div className="pointer-events-auto absolute left-0 z-20 flex h-12 items-center gap-1 border-t border-line bg-panel/95 px-3" style={{ right: '280px', bottom }}>
      <div className="flex min-w-0 overflow-x-auto">
        {PHASES.map((phase) => (
          <span className={`shrink-0 px-3 py-1 text-xs font-semibold transition-colors ${currentPhase === phase ? 'bg-forge text-ink' : 'text-slate-500'}`} key={phase}>
            {PHASE_LABELS[phase] ?? phase}
          </span>
        ))}
      </div>
      <div className="ml-auto flex items-center gap-3">
        <span className={`whitespace-nowrap text-xs font-semibold ${isMyTurn ? 'text-forge' : 'text-slate-300'}`}>
          {currentPhase === 'BLOCK_DECLARE' && canPass ? 'Your response' : isMyTurn ? 'Your turn' : `${opponentName}'s turn`}
        </span>
        <button className="btn-primary min-h-7 px-4 py-1 text-xs disabled:opacity-40" disabled={!canPass} onClick={onPassPhase}>
          Pass
        </button>
      </div>
    </div>
  );
}
