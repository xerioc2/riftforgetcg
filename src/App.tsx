import React, { Suspense, useEffect, useMemo, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Nav } from './components/Nav';
import { ServerLoader } from './components/ServerLoader';
import { ToastHost } from './components/ToastHost';
import { UpdateAvailableBanner } from './components/UpdateAvailableBanner';
import { getGameServerCandidates, getGameServerUrl, initServerUrl, setResolvedServerUrl } from './lib/env';
import { getOrCreateLocalPlayer, saveLocalPlayer } from './lib/localPlayer';
import { PlayerContext } from './lib/playerContext';
import { checkLatestRelease, dismissRelease, type ReleaseUpdate } from './lib/releaseUpdates';
import { DeckBuild } from './pages/DeckBuild';
import { Home } from './pages/Home';
import { History } from './pages/History';
import { Lobby } from './pages/Lobby';

const GameBoard = React.lazy(() => import('./pages/GameBoard'));
const SpectatorView = React.lazy(() => import('./pages/SpectatorView'));

function App() {
  const initialPlayer = useMemo(() => getOrCreateLocalPlayer(), []);
  const [player, setPlayer] = useState(initialPlayer);
  const [name, setName] = useState(initialPlayer.name);
  const [serverUrlInitialized, setServerUrlInitialized] = useState(false);
  const [serverReady, setServerReady] = useState(false);
  const [serverCheckStartedAt, setServerCheckStartedAt] = useState(() => Date.now());
  const [showServerHelp, setShowServerHelp] = useState(false);
  const [releaseUpdate, setReleaseUpdate] = useState<ReleaseUpdate | null>(null);

  useEffect(() => {
    let cancelled = false;
    void initServerUrl().finally(() => {
      if (!cancelled) setServerUrlInitialized(true);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!serverUrlInitialized) return;
    let cancelled = false;
    let interval = 0;
    const poll = async () => {
      for (const serverUrl of getGameServerCandidates()) {
        try {
          const response = await fetch(`${serverUrl}/api/health`);
          if (!response.ok) continue;
          const data = (await response.json()) as { status?: string };
          if (!cancelled && data.status === 'ok') {
            setResolvedServerUrl(serverUrl);
            setServerReady(true);
            setShowServerHelp(false);
            window.clearInterval(interval);
            return;
          }
        } catch {
          // Try the next local port while the bundled server starts.
        }
      }
    };
    void poll();
    interval = window.setInterval(() => void poll(), 300);
    const helpTimer = window.setTimeout(() => setShowServerHelp(true), 120_000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
      window.clearTimeout(helpTimer);
    };
  }, [serverCheckStartedAt, serverUrlInitialized]);

  useEffect(() => {
    if (!serverReady) return;
    let cancelled = false;
    void checkLatestRelease()
      .then((update) => {
        if (!cancelled) setReleaseUpdate(update);
      })
      .catch((error) => {
        console.debug('RiftForge release check failed.', error);
      });
    return () => {
      cancelled = true;
    };
  }, [serverReady]);

  if (!serverUrlInitialized || !serverReady) {
    return (
      <ServerLoader
        serverUrl={getGameServerUrl()}
        showHelp={showServerHelp}
        onRetry={() => {
          setShowServerHelp(false);
          setServerCheckStartedAt(Date.now());
        }}
      />
    );
  }

  if (!player.name.trim()) {
    return (
      <main className="grid min-h-screen place-items-center bg-ink px-5 text-slate-100">
        <section className="w-full max-w-md border border-line bg-panel p-5 shadow-glow">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-forge">RiftForge</p>
          <h1 className="mt-3 text-3xl font-semibold text-white">Choose a display name</h1>
          <input className="input mt-5 w-full" value={name} onChange={(event) => setName(event.target.value)} placeholder="Display name" />
          <button
            className="btn-primary mt-4 w-full"
            disabled={!name.trim()}
            onClick={() => {
              const next = { ...player, name: name.trim() };
              saveLocalPlayer(next);
              setPlayer(next);
            }}
          >
            Continue
          </button>
        </section>
      </main>
    );
  }

  return (
    <PlayerContext.Provider value={player}>
      <BrowserRouter>
        <Nav />
        {releaseUpdate ? (
          <UpdateAvailableBanner
            update={releaseUpdate}
            onDismiss={() => {
              dismissRelease(releaseUpdate.latestTag);
              setReleaseUpdate(null);
            }}
          />
        ) : null}
        <ToastHost />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/build" element={<DeckBuild />} />
          <Route path="/history" element={<History />} />
          <Route path="/lobby/:code" element={<Lobby />} />
          <Route
            path="/game/:code"
            element={
              <Suspense fallback={<div className="flex h-screen items-center justify-center bg-ink text-sm text-forge">Loading...</div>}>
                <GameBoard />
              </Suspense>
            }
          />
          <Route
            path="/spectate/:code"
            element={
              <Suspense fallback={<div className="flex h-screen items-center justify-center bg-ink text-sm text-forge">Loading...</div>}>
                <SpectatorView />
              </Suspense>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </PlayerContext.Provider>
  );
}

export default App;
