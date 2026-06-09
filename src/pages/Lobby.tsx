import { useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { useNavigate, useParams } from 'react-router-dom';
import { deckToGameCardIds } from '../lib/deckUtils';
import { getGameServerUrl } from '../lib/env';
import { useLocalPlayer } from '../lib/playerContext';
import { createLobbyClient } from '../lib/stompGame';
import { useDeckStore } from '../store/decks';
import type { RoomState } from '../types';

const cx = (...classes: Array<string | false | undefined>) => classes.filter(Boolean).join(' ');
const BOT_ID = 'bot-player-riftbot';

export function Lobby() {
  const { code } = useParams();
  const navigate = useNavigate();
  const player = useLocalPlayer();
  const { decks, activeDeckId, setActiveDeck } = useDeckStore();
  const [room, setRoom] = useState<RoomState | null>(null);
  const [myDeckId, setMyDeckId] = useState<string | null>(activeDeckId ?? null);
  const [botDeckId, setBotDeckId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deckError, setDeckError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);
  const normalizedCode = code?.toUpperCase() ?? '';

  const deckNames = useMemo(() => new Map(decks.map((deck) => [deck.id, deck.name])), [decks]);
  const me = room?.players.find((lobbyPlayer) => lobbyPlayer.id === player.id);
  const isPlayer = Boolean(me);
  const canStart = Boolean(
    room &&
      room.players.length >= 2 &&
      room.players.filter((lobbyPlayer) => lobbyPlayer.id !== BOT_ID).every((lobbyPlayer) => lobbyPlayer.ready),
  );

  useEffect(() => {
    const setup = async () => {
      try {
        const res = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}`);
        if (!res.ok) throw new Error('Room not found.');
        setRoom((await res.json()) as RoomState);
        clientRef.current = createLobbyClient(normalizedCode, player, setRoom);
        setLoading(false);
      } catch (setupError) {
        setError(setupError instanceof Error ? setupError.message : 'Unable to load lobby.');
        setLoading(false);
      }
    };
    void setup();
    return () => {
      void clientRef.current?.deactivate();
      clientRef.current = null;
    };
  }, [normalizedCode, player]);

  useEffect(() => {
    if (room?.status === 'playing') navigate(`/game/${normalizedCode}`);
  }, [navigate, normalizedCode, room?.status]);

  const chooseDeck = (deckId: string) => {
    setActiveDeck(deckId);
    setMyDeckId(deckId);
    setDeckError(null);
  };

  const deckCardIds = (deckId: string | null) => {
    const deck = decks.find((existing) => existing.id === deckId);
    if (!deck) return [];
    return deckToGameCardIds(deck);
  };

  const selectedDeckCardIds = deckCardIds(myDeckId);
  const deckIsEmpty = selectedDeckCardIds.length === 0;

  const handleReady = async () => {
    const response = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/ready`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, deckCardIds: selectedDeckCardIds }),
    });
    if (!response.ok) {
      const body = await response.text();
      setDeckError(body || 'Deck is not valid.');
      return;
    }
    setDeckError(null);
  };

  const handleSetBotDeck = async (deckId: string) => {
    setBotDeckId(deckId || null);
    const ids = deckId ? deckCardIds(deckId) : [];
    await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/bot-deck`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deckCardIds: ids }),
    });
  };

  const handleStart = async () => {
    const response = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id }),
    });
    if (!response.ok) {
      const body = await response.text();
      setDeckError(body || 'Unable to start game.');
    }
  };

  if (loading) return <CenteredState>Loading lobby...</CenteredState>;

  if (error || !room) {
    return (
      <CenteredState>
        <p>{error || 'Lobby unavailable.'}</p>
        <button className="btn-secondary mt-4" onClick={() => navigate('/')}>
          Back
        </button>
      </CenteredState>
    );
  }

  return (
    <main className="min-h-[calc(100vh-73px)] bg-ink px-5 py-8 text-slate-100">
      <section className="mx-auto grid max-w-5xl gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="border border-line bg-panel p-5 shadow-glow">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm uppercase tracking-[0.18em] text-forge">Room code</p>
              <h1 className="mt-2 font-mono text-4xl font-semibold text-white">{room.code}</h1>
            </div>
            <button className="btn-secondary" onClick={() => void navigator.clipboard.writeText(room.code)}>
              Copy code
            </button>
          </div>

          <div className="mt-6 border border-line">
            {room.players.map((lobbyPlayer) => (
              <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 border-b border-line px-4 py-4 last:border-b-0" key={lobbyPlayer.id}>
                <div className="min-w-0">
                  <p className={cx('truncate font-medium', lobbyPlayer.id === BOT_ID ? 'text-forge' : 'text-white')}>
                    {lobbyPlayer.name || lobbyPlayer.id}
                    {lobbyPlayer.id === BOT_ID ? ' [BOT]' : ''}
                    {lobbyPlayer.id === player.id ? ' (you)' : ''}
                    {lobbyPlayer.id === room.hostId ? <span className="ml-2 badge border-forge/40 text-forge">Host</span> : null}
                  </p>
                  <p className="mt-1 truncate text-sm text-slate-400">
                    {lobbyPlayer.id === BOT_ID
                      ? `${lobbyPlayer.deckCardIds.length} card playtest deck`
                      : lobbyPlayer.deckCardIds.length > 0
                        ? deckNames.get(myDeckId ?? '') ?? `${lobbyPlayer.deckCardIds.length} cards ready`
                        : 'No deck selected'}
                  </p>
                </div>
                <span className={cx('badge', lobbyPlayer.ready ? 'border-mint/40 text-mint' : 'border-line text-slate-400')}>{lobbyPlayer.ready ? 'Ready' : 'Not ready'}</span>
              </div>
            ))}
          </div>
        </div>

        <aside className="border border-line bg-panel p-5 shadow-glow">
          {isPlayer ? (
            <>
              <h2 className="text-lg font-semibold text-white">Your setup</h2>
              <select className="input mt-4 w-full" value={myDeckId ?? ''} onChange={(event) => chooseDeck(event.target.value)} aria-label="Deck">
                <option value="">Choose deck</option>
                {decks.map((deck) => (
                  <option key={deck.id} value={deck.id}>
                    {deck.name}
                  </option>
                ))}
              </select>
              <button className="btn-primary mt-3 w-full" onClick={() => void handleReady()} disabled={!myDeckId || deckIsEmpty}>
                {me?.ready ? 'Unready' : 'Ready up'}
              </button>
              {deckError ? <p className="mt-2 text-sm text-ember">{deckError}</p> : null}
              {myDeckId && deckIsEmpty ? <p className="mt-2 text-xs text-ember">Deck is empty — add cards in the Deck Builder first.</p> : null}
            </>
          ) : (
            <button className="btn-secondary mt-4 w-full" onClick={() => navigate(`/spectate/${room.code}`)}>
              Spectate this game
            </button>
          )}

          {isPlayer && room.hostId === player.id && room.players.some((p) => p.id === BOT_ID) ? (
            <div className="mt-5 border-t border-line pt-5">
              <h2 className="text-sm font-semibold text-slate-300">RiftBot deck</h2>
              <select className="input mt-2 w-full" value={botDeckId ?? ''} onChange={(e) => void handleSetBotDeck(e.target.value)} aria-label="Bot deck">
                <option value="">Auto-generated</option>
                {decks.map((deck) => (
                  <option key={deck.id} value={deck.id}>
                    {deck.name}
                  </option>
                ))}
              </select>
              <p className="mt-1 text-xs text-slate-500">{botDeckId ? 'Bot will use your selected deck' : 'Bot will use a balanced auto deck'}</p>
            </div>
          ) : null}

          {isPlayer && room.hostId === player.id ? (
            <div className="mt-5 border-t border-line pt-5">
              <button className="btn-primary w-full" onClick={() => void handleStart()} disabled={!canStart}>
                Start Game
              </button>
              {!canStart ? <p className="mt-3 text-sm text-slate-400">Waiting for players to ready up</p> : null}
            </div>
          ) : null}

          <button className="btn-secondary mt-5 w-full border-ember/60 text-ember" onClick={() => navigate('/')}>
            Leave room
          </button>
        </aside>
      </section>
    </main>
  );
}

function CenteredState({ children }: { children: React.ReactNode }) {
  return (
    <main className="grid min-h-[calc(100vh-73px)] place-items-center bg-ink px-5 py-10 text-slate-100">
      <section className="w-full max-w-lg border border-line bg-panel p-6 text-center shadow-glow">{children}</section>
    </main>
  );
}
