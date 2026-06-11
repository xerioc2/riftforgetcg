const PHASE_LABELS: Record<string, string> = {
  MULLIGAN: 'Mulligan',
  AWAKEN: 'Awaken',
  BEGINNING: 'Beginning',
  CHANNEL: 'Channel',
  DRAW: 'Draw',
  MAIN: 'Main Phase',
  END: 'End Phase',
};

const PHASES = Object.keys(PHASE_LABELS);

const SHOWDOWN_STEP_LABELS: Record<string, string> = {
  STAGED: 'Staged',
  ACTION_WINDOW: 'Action Window',
  ASSIGN_DAMAGE: 'Assign Damage',
  RESOLVE_DAMAGE: 'Resolve Damage',
  CLEANUP: 'Cleanup',
  COMPLETE: 'Complete',
};

interface PhaseBarProps {
  currentPhase: string;
  isMyTurn: boolean;
  canPass: boolean;
  opponentName: string;
  onPassPhase: () => void;
  activeShowdown?: boolean;
  showdownStep?: string;
  bottom?: number;
}

export function PhaseBar({ currentPhase, isMyTurn, canPass, opponentName, onPassPhase, activeShowdown = false, showdownStep, bottom = 172 }: PhaseBarProps) {
  const showdownLabel = showdownStep ? `Showdown: ${SHOWDOWN_STEP_LABELS[showdownStep] ?? showdownStep}` : 'Showdown';
  const currentLabel = activeShowdown && currentPhase === 'MAIN' ? `Main Phase - ${showdownLabel}` : PHASE_LABELS[currentPhase] ?? currentPhase;
  return (
    <div className="pointer-events-auto absolute left-0 z-20 flex h-12 items-center gap-1 border-t border-line bg-panel/95 px-3" style={{ right: '280px', bottom }}>
      <div className="flex min-w-0 overflow-x-auto">
        {PHASES.map((phase) => (
          <span className={`shrink-0 px-3 py-1 text-xs font-semibold transition-colors ${currentPhase === phase ? 'bg-forge text-ink' : 'text-slate-500'}`} key={phase}>
            {phase === currentPhase ? currentLabel : PHASE_LABELS[phase] ?? phase}
          </span>
        ))}
      </div>
      <div className="ml-auto flex items-center gap-3">
        <span className={`whitespace-nowrap text-xs font-semibold ${isMyTurn ? 'text-forge' : 'text-slate-300'}`}>
          {isMyTurn ? 'Your turn' : `${opponentName}'s turn`}
        </span>
        {canPass ? (
          <button className="btn-primary min-h-7 px-4 py-1 text-xs" onClick={onPassPhase}>
            {activeShowdown ? 'Resolve Showdown' : 'Pass'}
          </button>
        ) : null}
      </div>
    </div>
  );
}
