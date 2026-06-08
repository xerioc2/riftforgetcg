import React, { useEffect, useState } from 'react';

type ServerLoaderProps = {
  serverUrl: string;
  showHelp: boolean;
  onRetry: () => void;
};

const STAGES: { afterMs: number; message: string }[] = [
  { afterMs: 0,     message: 'Starting Java runtime...' },
  { afterMs: 4000,  message: 'Loading game server...' },
  { afterMs: 10000, message: 'Initializing game engine...' },
  { afterMs: 20000, message: 'Fetching card data from Riftcodex...' },
  { afterMs: 50000, message: 'Building card cache (first launch only)...' },
  { afterMs: 90000, message: 'Almost ready, hang tight...' },
];

const FILL_DURATION_MS = 110_000;

export function ServerLoader({ serverUrl, showHelp, onRetry }: ServerLoaderProps) {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const start = Date.now();
    const id = window.setInterval(() => setElapsed(Date.now() - start), 250);
    return () => window.clearInterval(id);
  }, []);

  const stage = [...STAGES].reverse().find((s) => elapsed >= s.afterMs) ?? STAGES[0];
  const fillPct = Math.min(95, (elapsed / FILL_DURATION_MS) * 100);

  return (
    <main className="grid min-h-screen place-items-center bg-panel text-slate-100">
      <div className="flex w-full max-w-md flex-col items-center px-6 text-center">
        <h1 className="text-3xl font-semibold text-forge">RiftForge</h1>
        <p className="mt-3 text-sm text-slate-300">{stage.message}</p>

        <div className="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-line">
          <div
            className="progress-fill h-full rounded-full bg-forge transition-[width] duration-500"
            style={{ '--fill': `${fillPct}%` } as React.CSSProperties}
          />
        </div>

        <p className="mt-2 text-[11px] text-slate-500">
          {Math.round(elapsed / 1000)}s elapsed
        </p>

        {showHelp ? (
          <div className="mt-6 border border-line bg-ink/60 p-4 text-left text-sm text-slate-300">
            <p className="font-semibold text-white">The game server is not responding.</p>
            <p className="mt-2">
              Tried <span className="text-forge">{serverUrl}</span> for over 2 minutes.
              Make sure the app fully closed before relaunching. If the issue persists,
              check Task Manager for a lingering <code className="text-forge">java.exe</code> process and end it.
            </p>
            <button className="btn-secondary mt-4 w-full" onClick={onRetry}>
              Retry connection
            </button>
          </div>
        ) : null}
      </div>
    </main>
  );
}
