import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GAME_SERVER_URL } from '../lib/env';
import { useLocalPlayer } from '../lib/playerContext';

export function Home() {
  const player = useLocalPlayer();
  const navigate = useNavigate();
  const [joinCode, setJoinCode] = useState('');
  const [message, setMessage] = useState('');
  const [withBot, setWithBot] = useState(false);

  const handleCreate = async () => {
    setMessage('');
    const res = await fetch(`${GAME_SERVER_URL}/api/rooms`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, playerName: player.name, withBot }),
    });
    if (!res.ok) {
      setMessage('Unable to create room. Is the server running?');
      return;
    }
    const room = (await res.json()) as { code: string };
    navigate(`/lobby/${room.code}`);
  };

  const handleJoin = async () => {
    const code = joinCode.trim().toUpperCase();
    setMessage('');
    const res = await fetch(`${GAME_SERVER_URL}/api/rooms/${code}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, playerName: player.name }),
    });
    if (!res.ok) {
      setMessage('Room not found or already started.');
      return;
    }
    navigate(`/lobby/${code}`);
  };

  return (
    <main className="grid min-h-[calc(100vh-73px)] place-items-center bg-ink px-5 py-10 text-slate-100">
      <section className="w-full max-w-lg">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-forge">RiftForge</p>
        <h1 className="mt-3 text-4xl font-semibold text-white">Play Riftbound</h1>
        <p className="mt-4 text-base leading-7 text-slate-300">Welcome, {player.name}. Create a room locally or join with a code.</p>

        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <article className="border border-line bg-panel p-4 shadow-glow">
            <h2 className="text-lg font-semibold text-white">Create Room</h2>
            <p className="mt-2 text-sm text-slate-400">Open a local lobby and invite players with a room code.</p>
            <button className="btn-primary mt-5 w-full" onClick={() => void handleCreate()}>
              Create
            </button>
            <label className="mt-4 flex cursor-pointer items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={withBot} onChange={(event) => setWithBot(event.target.checked)} className="accent-forge" />
              <span>
                vs <span className="text-forge">RiftBot</span> (solo playtest)
              </span>
            </label>
          </article>
          <article className="border border-line bg-panel p-4 shadow-glow">
            <h2 className="text-lg font-semibold text-white">Join Room</h2>
            <input className="input mt-4 w-full uppercase" placeholder="Room code" maxLength={4} value={joinCode} onChange={(event) => setJoinCode(event.target.value.toUpperCase())} />
            <button className="btn-secondary mt-3 w-full" onClick={() => void handleJoin()} disabled={joinCode.length < 4}>
              Join
            </button>
          </article>
        </div>

        {message ? <p className="mt-4 text-sm text-forge">{message}</p> : null}
        <Link className="mt-8 inline-flex font-semibold text-mint" to="/build">
          Build Decks →
        </Link>
      </section>
    </main>
  );
}
