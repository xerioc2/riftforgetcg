import type { PlayerGameState } from '../../types';

export function ScorePanel({ players, onAdjust, emailMap }: { players: PlayerGameState[]; onAdjust: (userId: string, delta: number) => void; emailMap: Map<string, string> }) {
  return (
    <div className="pointer-events-auto absolute left-1/2 top-4 flex -translate-x-1/2 gap-2 rounded-sm border border-line bg-panel/95 p-2 shadow-glow">
      {players.map((player) => (
        <div className="min-w-32 border border-line bg-ink px-3 py-2 text-center" key={player.userId}>
          <p className="truncate text-xs text-slate-400">{emailMap.get(player.userId) ?? player.userId.slice(0, 8)}</p>
          <div className="mt-2 flex items-center justify-center gap-2">
            <button className="icon-btn" onClick={() => onAdjust(player.userId, -1)}>
              -
            </button>
            <span className="min-w-8 text-lg font-semibold text-white">{player.score}</span>
            <button className="icon-btn" onClick={() => onAdjust(player.userId, 1)}>
              +
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
