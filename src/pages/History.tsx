import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { config } from '../lib/env';
import type { MatchRecord } from '../types';

export function History() {
  const [matches, setMatches] = useState<MatchRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    void fetch(`${config.gameServerUrl}/api/matches`)
      .then((response) => {
        if (!response.ok) throw new Error('Unable to load match history.');
        return response.json() as Promise<MatchRecord[]>;
      })
      .then(setMatches)
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'Unable to load match history.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="min-h-[calc(100vh-73px)] bg-ink px-5 py-10 text-slate-100">
      <section className="mx-auto max-w-6xl">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.18em] text-forge">RiftForge</p>
            <h1 className="mt-2 text-3xl font-semibold text-white">Match History</h1>
          </div>
          <Link className="btn-secondary" to="/">Back to Home</Link>
        </div>

        {loading ? <div className="border border-line bg-panel p-6 text-slate-300">Loading matches...</div> : null}
        {error ? <div className="border border-ember/60 bg-panel p-6 text-ember">{error}</div> : null}
        {!loading && !error && matches.length === 0 ? (
          <div className="border border-line bg-panel p-8 text-center text-slate-300">No matches yet - play a PvP game to record one</div>
        ) : null}
        {matches.length > 0 ? (
          <div className="overflow-x-auto border border-line bg-panel">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-line bg-ink/60 text-xs uppercase text-slate-400">
                <tr>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Players</th>
                  <th className="px-4 py-3">Score</th>
                  <th className="px-4 py-3">Winner</th>
                  <th className="px-4 py-3">Turns</th>
                </tr>
              </thead>
              <tbody>
                {matches.map((match) => {
                  const winner = match.players.find((candidate) => candidate.userId === match.winnerId);
                  return (
                    <tr key={match.id} className="border-b border-line/70 last:border-0">
                      <td className="px-4 py-4 text-slate-300">{new Date(match.completedAt).toLocaleString()}</td>
                      <td className="px-4 py-4">{match.players.map((candidate) => candidate.name || candidate.userId).join(' vs ')}</td>
                      <td className="px-4 py-4">{match.players.map((candidate) => candidate.score).join(' - ')}</td>
                      <td className="px-4 py-4 font-semibold text-forge">{winner?.name || winner?.userId || match.winnerId}</td>
                      <td className="px-4 py-4 text-slate-300">{match.turnCount}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </main>
  );
}
