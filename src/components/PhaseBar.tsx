import type { ChainItem } from '../types';

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
  chainItems?: ChainItem[];
  chainTargetSelectionActive?: boolean;
  isChainItemTargetable?: (item: ChainItem) => boolean;
  onChainItemTarget?: (itemId: string) => void;
  onCancelChainTargetSelection?: () => void;
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
  chainItems = [],
  chainTargetSelectionActive = false,
  isChainItemTargetable = () => false,
  onChainItemTarget,
  onCancelChainTargetSelection,
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
  const topToBottomChainItems = [...chainItems].reverse();
  return (
    <>
      {activeChain ? (
        <div className="pointer-events-auto absolute left-3 z-20 max-w-[520px] border border-forge/40 bg-ink/95 p-3 shadow-glow" style={{ right: '296px', bottom: bottom + 52 }}>
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-forge">Chain</p>
              <p className="text-[11px] text-slate-400">
                {chainReadyToResolve ? 'Top item is ready to resolve.' : 'Waiting for chain responses.'}
              </p>
            </div>
            {chainFocusName ? <span className="shrink-0 text-xs font-semibold text-slate-200">Focus: {chainFocusName}</span> : null}
          </div>
          {topToBottomChainItems.length > 0 ? (
            <ol className="mt-2 max-h-28 space-y-1 overflow-y-auto">
              {topToBottomChainItems.map((item, index) => {
                const label = item.publicDescription || item.sourceCardName || 'Hidden chain item';
                const status = item.status && item.status !== 'PENDING' ? item.status.toLowerCase() : undefined;
                const targetable = chainTargetSelectionActive && isChainItemTargetable(item);
                return (
                  <li key={item.itemId} className={`flex items-center gap-2 border px-2 py-1 text-xs ${targetable ? 'border-mint/70 bg-mint/10' : index === 0 ? 'border-forge/60 bg-forge/10' : 'border-line bg-panel/80'}`}>
                    <span className="shrink-0 text-[10px] font-bold uppercase text-slate-500">{index === 0 ? 'Top' : `#${topToBottomChainItems.length - index}`}</span>
                    <span className="min-w-0 flex-1 truncate text-slate-100">{label}</span>
                    {status ? <span className="shrink-0 rounded-sm border border-slate-600 px-1.5 py-0.5 text-[10px] uppercase text-slate-300">{status}</span> : null}
                    {targetable ? (
                      <button className="btn-secondary min-h-6 px-2 py-0.5 text-[10px]" onClick={() => onChainItemTarget?.(item.itemId)}>
                        Counter
                      </button>
                    ) : null}
                  </li>
                );
              })}
            </ol>
          ) : null}
          {chainTargetSelectionActive ? (
            <div className="mt-2 flex items-center justify-between gap-2 text-[11px] text-slate-300">
              <span>Choose a pending public spell item to counter.</span>
              <button className="btn-secondary min-h-6 px-2 py-0.5 text-[10px]" onClick={onCancelChainTargetSelection}>Cancel</button>
            </div>
          ) : null}
        </div>
      ) : null}
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
    </>
  );
}
