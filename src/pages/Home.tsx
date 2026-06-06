import { useEffect, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { Link, useNavigate } from 'react-router-dom';
import { GAME_SERVER_URL } from '../lib/env';
import { useLocalPlayer } from '../lib/playerContext';
import { createMatchmakingClient } from '../lib/stompGame';
import { useDeckStore } from '../store/decks';

export function Home() {
  const player = useLocalPlayer();
  const navigate = useNavigate();
  const { decks, activeDeckId } = useDeckStore();
  const activeDeck = decks.find((deck) => deck.id === activeDeckId) ?? decks[0];
  const [joinCode, setJoinCode] = useState('');
  const [message, setMessage] = useState('');
  const [withBot, setWithBot] = useState(false);
  const [searching, setSearching] = useState(false);
  const [queueSize, setQueueSize] = useState(0);
  const matchClientRef = useRef<Client | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const searchingRef = useRef(false);

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

  const stopPolling = () => {
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = null;
  };

  const updateQueueSize = async () => {
    try {
      const response = await fetch(`${GAME_SERVER_URL}/api/matchmaking/status`);
      if (!response.ok) return;
      const data = (await response.json()) as { queueSize: number };
      setQueueSize(data.queueSize);
    } catch {
      // Keep waiting; a reconnect or later poll may recover.
    }
  };

  const handleFindMatch = async () => {
    if (searchingRef.current) return;
    if (!activeDeck || activeDeck.cards.length === 0) {
      setMessage('Select a deck in the deck builder first.');
      return;
    }

    setMessage('');
    setSearching(true);
    searchingRef.current = true;
    const deckCardIds = activeDeck.cards.flatMap((entry) => Array.from({ length: entry.quantity }, () => entry.cardId));
    matchClientRef.current = createMatchmakingClient(
      player,
      (notification) => {
        if (!searchingRef.current) return;
        stopPolling();
        void matchClientRef.current?.deactivate();
        matchClientRef.current = null;
        searchingRef.current = false;
        setSearching(false);
        navigate(`/game/${notification.roomCode}`);
      },
      () => {
        if (!searchingRef.current) return;
        void fetch(`${GAME_SERVER_URL}/api/matchmaking/join`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ playerId: player.id, playerName: player.name, deckCardIds }),
        })
          .then((response) => {
            if (!response.ok) throw new Error('Unable to join queue.');
            if (!searchingRef.current) {
              void fetch(`${GAME_SERVER_URL}/api/matchmaking/leave`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerId: player.id }),
              });
              return;
            }
            void updateQueueSize();
            pollRef.current = setInterval(() => void updateQueueSize(), 3000);
          })
          .catch(() => {
            if (!searchingRef.current) return;
            setMessage('Unable to join queue.');
            searchingRef.current = false;
            setSearching(false);
            void matchClientRef.current?.deactivate();
            matchClientRef.current = null;
          });
      },
    );
  };

  const handleCancelSearch = async () => {
    stopPolling();
    void matchClientRef.current?.deactivate();
    matchClientRef.current = null;
    await fetch(`${GAME_SERVER_URL}/api/matchmaking/leave`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id }),
    });
    searchingRef.current = false;
    setSearching(false);
    setQueueSize(0);
  };

  useEffect(() => {
    return () => {
      stopPolling();
      if (!searchingRef.current) return;
      void matchClientRef.current?.deactivate();
      void fetch(`${GAME_SERVER_URL}/api/matchmaking/leave`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerId: player.id }),
      });
    };
  }, [player.id]);

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
          <article className="border border-line bg-panel p-4 shadow-glow sm:col-span-2">
            <h2 className="text-lg font-semibold text-white">Find Match</h2>
            <p className="mt-2 text-sm text-slate-400">Join the queue and get paired with a random opponent.</p>
            {searching ? (
              <>
                <p className="mt-3 animate-pulse text-sm text-slate-400">
                  Searching... ({queueSize} player{queueSize !== 1 ? 's' : ''} in queue)
                </p>
                <button className="btn-secondary mt-3 w-full" onClick={() => void handleCancelSearch()}>
                  Cancel
                </button>
              </>
            ) : (
              <button className="btn-primary mt-5 w-full" onClick={() => void handleFindMatch()} disabled={!activeDeck}>
                Find Match
              </button>
            )}
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
