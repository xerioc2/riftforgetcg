import { useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { useNavigate, useParams } from 'react-router-dom';
import { deckSupportEntries, unsupportedDeckEntries } from '../lib/deckSupport';
import { deckToGameCardIds } from '../lib/deckUtils';
import { validateDeck } from '../lib/deckValidation';
import { getGameServerUrl } from '../lib/env';
import { readableHttpError } from '../lib/http';
import { useLocalPlayer } from '../lib/playerContext';
import { getRoomSessionToken } from '../lib/roomSession';
import { createLobbyClient } from '../lib/stompGame';
import { useCardStore } from '../store/cards';
import { useDeckStore } from '../store/decks';
import { notifyError, notifyWarning } from '../store/toasts';
import type { PresenceSummary, RoomState } from '../types';

const cx = (...classes: Array<string | false | undefined>) => classes.filter(Boolean).join(' ');
const BOT_ID = 'bot-player-riftbot';

export function Lobby() {
  const { code } = useParams();
  const navigate = useNavigate();
  const player = useLocalPlayer();
  const { cards, loadCards } = useCardStore();
  const { decks, activeDeckId, setActiveDeck } = useDeckStore();
  const [room, setRoom] = useState<RoomState | null>(null);
  const [myDeckId, setMyDeckId] = useState<string | null>(activeDeckId ?? null);
  const [botDeckId, setBotDeckId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deckError, setDeckError] = useState<string | null>(null);
  const [supportedCardsOnly, setSupportedCardsOnly] = useState(false);
  const [presence, setPresence] = useState<PresenceSummary | null>(null);
  const clientRef = useRef<Client | null>(null);
  const normalizedCode = code?.toUpperCase() ?? '';

  const deckNames = useMemo(() => new Map(decks.map((deck) => [deck.id, deck.name])), [decks]);
  const cardsById = useMemo(() => new Map(cards.map((card) => [card.id, card])), [cards]);
  const selectedDeck = useMemo(() => decks.find((deck) => deck.id === myDeckId), [decks, myDeckId]);
  const deckValidation = useMemo(() => selectedDeck ? validateDeck(selectedDeck, cardsById) : null, [cardsById, selectedDeck]);
  const supportEntries = useMemo(() => deckSupportEntries(selectedDeck, cardsById), [cardsById, selectedDeck]);
  const unsupportedCards = useMemo(() => unsupportedDeckEntries(selectedDeck, cardsById), [cardsById, selectedDeck]);
  const partialCards = useMemo(() => supportEntries.filter((entry) => entry.status === 'PARTIAL'), [supportEntries]);
  const me = room?.players.find((lobbyPlayer) => lobbyPlayer.id === player.id);
  const isPlayer = Boolean(me);
  const canStart = Boolean(
    room &&
      room.players.length >= 2 &&
      room.players.filter((lobbyPlayer) => lobbyPlayer.id !== BOT_ID).every((lobbyPlayer) => lobbyPlayer.ready),
  );

  useEffect(() => {
    if (cards.length === 0) void loadCards();
  }, [cards.length, loadCards]);

  useEffect(() => {
    let cancelled = false;
    const setup = async () => {
      try {
        const res = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}`);
        if (!res.ok) throw new Error('Room not found.');
        setRoom((await res.json()) as RoomState);
        clientRef.current = createLobbyClient(normalizedCode, player, getRoomSessionToken(normalizedCode, player.id), setRoom, setPresence);
        void fetch(`${getGameServerUrl()}/api/presence`)
          .then((presenceResponse) => presenceResponse.ok ? presenceResponse.json() as Promise<PresenceSummary> : null)
          .then((nextPresence) => {
            if (nextPresence && !cancelled) setPresence(nextPresence);
          })
          .catch(() => {});
        setLoading(false);
      } catch (setupError) {
        const message = setupError instanceof Error ? setupError.message : 'Unable to load lobby.';
        setError(message);
        notifyError('Unable to load lobby', message);
        setLoading(false);
      }
    };
    void setup();
    return () => {
      cancelled = true;
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
  const sessionToken = getRoomSessionToken(normalizedCode, player.id);

  const handleReady = async () => {
    const response = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/ready`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, sessionToken, deckCardIds: selectedDeckCardIds, supportedCardsOnly }),
    });
    if (!response.ok) {
      const message = await readableHttpError(response, 'Deck is not valid.');
      setDeckError(message);
      notifyError('Ready failed', message);
      return;
    }
    const nextRoom = (await response.json()) as RoomState;
    setRoom(nextRoom);
    setDeckError(null);
    const nextMe = nextRoom.players.find((lobbyPlayer) => lobbyPlayer.id === player.id);
    if (nextMe?.deckWarnings && nextMe.deckWarnings.length > 0) {
      notifyWarning('Deck has partial support', nextMe.deckWarnings.slice(0, 2).join(' '));
    }
    if (unsupportedCards.length > 0) {
      notifyWarning('Deck has unsupported effects', `${unsupportedCards.slice(0, 3).map(({ card }) => card.name).join(', ')} may not work fully yet.`);
    }
  };

  const handleSetBotDeck = async (deckId: string) => {
    setBotDeckId(deckId || null);
    const ids = deckId ? deckCardIds(deckId) : [];
    const response = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/bot-deck`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, sessionToken, deckCardIds: ids }),
    });
    if (!response.ok) {
      const message = await readableHttpError(response, 'Unable to set bot deck.');
      setDeckError(message);
      notifyError('Unable to set bot deck', message);
    }
  };

  const handleStart = async () => {
    const response = await fetch(`${getGameServerUrl()}/api/rooms/${normalizedCode}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: player.id, sessionToken }),
    });
    if (!response.ok) {
      const message = await readableHttpError(response, 'Unable to start game.');
      setDeckError(message);
      notifyError('Unable to start game', message);
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
              {presence ? (
                <p className="mt-2 text-sm text-slate-400">
                  {presence.onlinePlayers} player{presence.onlinePlayers === 1 ? '' : 's'} online
                </p>
              ) : null}
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
              <button className="btn-primary mt-3 w-full" onClick={() => void handleReady()} disabled={!myDeckId || deckIsEmpty || (!me?.ready && !deckValidation?.valid)}>
                {me?.ready ? 'Unready' : 'Ready up'}
              </button>
              {deckValidation && !deckValidation.valid ? (
                <div className="mt-3 border border-ember/50 px-3 py-2 text-xs leading-5 text-ember">
                  <p className="font-semibold">Deck is not legal yet:</p>
                  <p className="mt-1">{deckValidation.messages.slice(0, 3).join(' ')}</p>
                </div>
              ) : null}
              <label className="mt-3 flex items-start gap-2 border border-line bg-ink px-3 py-2 text-xs leading-5 text-slate-300">
                <input
                  className="mt-1"
                  type="checkbox"
                  checked={supportedCardsOnly}
                  onChange={(event) => setSupportedCardsOnly(event.target.checked)}
                  disabled={Boolean(me?.ready)}
                />
                <span>
                  Supported-cards-only mode
                  <span className="block text-slate-500">Blocks unsupported or not-audited cards. Partial cards still show warnings.</span>
                </span>
              </label>
              {unsupportedCards.length > 0 ? (
                <div className="mt-3 border border-forge/50 px-3 py-2 text-xs leading-5 text-forge">
                  <p className="font-semibold">Unsupported card effects in this deck:</p>
                  <p className="mt-1">
                    {unsupportedCards.slice(0, 5).map(({ card }) => card.name).join(', ')}
                    {unsupportedCards.length > 5 ? `, +${unsupportedCards.length - 5} more` : ''}
                  </p>
                </div>
              ) : null}
              {partialCards.length > 0 ? (
                <div className="mt-3 border border-forge/50 px-3 py-2 text-xs leading-5 text-forge">
                  <p className="font-semibold">Partial support warnings:</p>
                  <p className="mt-1">
                    {partialCards.slice(0, 5).map(({ card }) => card.name).join(', ')}
                    {partialCards.length > 5 ? `, +${partialCards.length - 5} more` : ''}
                  </p>
                </div>
              ) : null}
              {me?.deckWarnings && me.deckWarnings.length > 0 ? (
                <div className="mt-3 border border-forge/50 px-3 py-2 text-xs leading-5 text-forge">
                  <p className="font-semibold">Server deck warnings:</p>
                  <p className="mt-1">{me.deckWarnings.slice(0, 3).join(' ')}</p>
                </div>
              ) : null}
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
