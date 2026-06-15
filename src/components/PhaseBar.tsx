const PHASE_LABELS: Record<string, string> = {
  AWAKEN: 'Awaken',
  BEGINNING: 'Beginning',
  CHANNEL: 'Channel',
  DRAW: 'Draw',
  MAIN: 'Main Phase',
  END: 'End Phase',
};

const PHASES = Object.keys(PHASE_LABELS);

const SETUP_LABELS: Record<string, string> = {
  SELECT_BATTLEFIELD: 'Setup: Battlefield selection',
  MULLIGAN: 'Setup: Mulligan',
};

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
  passLabel?: string;
  opponentName: string;
  onPassPhase: () => void;
  activeShowdown?: boolean;
  showdownStep?: string;
  showdownFocusName?: string;
  showdownReadyToResolve?: boolean;
  activeChain?: boolean;
  chainFocusName?: string;
  chainReadyToResolve?: boolean;
  chainItemCount?: number;
  guidance?: string;
  legalActionHint?: string;
  waitingStatus?: string;
  connectionStatus?: string;
  bottom?: number;
}

export function PhaseBar({
  currentPhase,
  isMyTurn,
  canPass,
  passLabel,
  opponentName,
  onPassPhase,
  activeShowdown = false,
  showdownStep,
  showdownFocusName,
  showdownReadyToResolve = false,
  activeChain = false,
  chainFocusName,
  chainReadyToResolve = false,
  chainItemCount = 0,
  guidance,
  legalActionHint,
  waitingStatus,
  connectionStatus,
  bottom = 172,
}: PhaseBarProps) {
  const setupLabel = SETUP_LABELS[currentPhase];
  const isPregameSetup = Boolean(setupLabel);
  const showdownLabel = showdownReadyToResolve
    ? 'Showdown: Ready to Resolve'
    : showdownStep ? `Showdown: ${SHOWDOWN_STEP_LABELS[showdownStep] ?? showdownStep}` : 'Showdown';
  const chainLabel = chainReadyToResolve ? 'Chain: Ready to Resolve' : 'Chain: Focus';
  const currentLabel = activeChain && currentPhase === 'MAIN'
    ? `Main Phase - ${chainLabel}`
    : activeShowdown && currentPhase === 'MAIN' ? `Main Phase - ${showdownLabel}` : PHASE_LABELS[currentPhase] ?? currentPhase;
  return (
    <div className="pointer-events-auto absolute left-0 z-20 flex h-12 items-center gap-3 border-t border-line bg-panel/95 px-3" style={{ right: '280px', bottom }}>
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 overflow-x-auto">
          {PHASES.map((phase) => (
            <span className={`shrink-0 px-3 py-1 text-xs font-semibold transition-colors ${currentPhase === phase ? 'bg-forge text-ink' : 'text-slate-500'}`} key={phase}>
              {phase === currentPhase ? currentLabel : PHASE_LABELS[phase] ?? phase}
            </span>
          ))}
        </div>
        <div className="mt-0.5 flex min-w-0 items-center gap-2 overflow-hidden text-[11px] leading-4">
          {setupLabel ? <span className="shrink-0 font-semibold text-forge">{setupLabel}</span> : null}
          {activeChain ? <span className="shrink-0 font-semibold text-forge">Chain: {chainItemCount}</span> : null}
          {activeChain && chainFocusName ? <span className="shrink-0 font-semibold text-forge">Focus: {chainFocusName}</span> : null}
          {activeShowdown && showdownFocusName ? <span className="shrink-0 font-semibold text-forge">Focus: {showdownFocusName}</span> : null}
          {guidance ? <span className="truncate text-slate-300">{guidance}</span> : null}
          {legalActionHint ? <span className="truncate text-forge">{legalActionHint}</span> : null}
          {waitingStatus ? <span className="truncate text-slate-400">{waitingStatus}</span> : null}
          {connectionStatus ? <span className="truncate text-slate-500">{connectionStatus}</span> : null}
        </div>
      </div>
      <div className="ml-auto flex items-center gap-3">
        <span className={`whitespace-nowrap text-xs font-semibold ${isMyTurn ? 'text-forge' : 'text-slate-300'}`}>
          {isPregameSetup ? 'Pregame setup' : isMyTurn ? 'Your turn' : `${opponentName}'s turn`}
        </span>
        {canPass ? (
          <button className="btn-primary min-h-7 px-4 py-1 text-xs" onClick={onPassPhase}>
            {passLabel ?? (activeShowdown ? 'Resolve Showdown' : 'Pass')}
          </button>
        ) : null}
      </div>
    </div>
  );
}
