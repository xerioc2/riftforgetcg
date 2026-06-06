import { useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { Layer, Stage } from 'react-konva';
import { useNavigate, useParams } from 'react-router-dom';
import { GameChat } from '../components/GameChat';
import { GameLog } from '../components/GameLog';
import { CardSprite } from '../components/board/CardSprite';
import { computeLayout } from '../components/board/BoardLayout';
import { ScorePanel } from '../components/board/ScorePanel';
import { ZoneOverlay } from '../components/board/ZoneOverlay';
import { config } from '../lib/env';
import { createGameClient } from '../lib/stompGame';
import { useCardStore } from '../store/cards';
import { useGameStore } from '../store/game';

const NAV_HEIGHT = 73;

function sameZone(zone: string, expected: string) {
  return zone.toLowerCase() === expected;
}

export function SpectatorView() {
  const { code } = useParams();
  const navigate = useNavigate();
  const { cards, loadCards } = useCardStore();
  const { state, chat, setState, addChat } = useGameStore();
  const [size, setSize] = useState({ width: window.innerWidth, height: Math.max(480, window.innerHeight - NAV_HEIGHT) });
  const [loading, setLoading] = useState(true);
  const [wsConnected, setWsConnected] = useState(false);
  const spectator = useMemo(() => ({ id: `spectator-${crypto.randomUUID()}`, name: 'Spectator' }), []);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const hasConnectedRef = useRef(false);
  const roomCode = code?.toUpperCase() ?? '';
  const cardsById = useMemo(() => new Map(cards.map((card) => [card.id, card])), [cards]);
  const playerIds = useMemo(() => state?.players.map((player) => player.userId) ?? [], [state?.players]);
  const zones = useMemo(() => computeLayout(size.width, size.height, playerIds), [playerIds, size.height, size.width]);
  const emailMap = useMemo(() => new Map(playerIds.map((id) => [id, id])), [playerIds]);

  useEffect(() => {
    if (cards.length === 0) void loadCards();
  }, [cards.length, loadCards]);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;
    const observer = new ResizeObserver(([entry]) => {
      setSize({ width: Math.max(320, entry.contentRect.width), height: Math.max(480, entry.contentRect.height) });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    let cancelled = false;
    const refreshState = async () => {
      const response = await fetch(`${config.gameServerUrl}/api/game/${roomCode}/state`);
      if (!response.ok) throw new Error('Game state not found.');
      const incoming = await response.json();
      if (!cancelled) setState(incoming);
    };
    const setup = async () => {
      try {
        await refreshState();
        if (cancelled) return;
        const client = createGameClient(
          roomCode,
          spectator,
          (msg) => {
            if (msg.type === 'STATE_UPDATE') setState(msg.state);
            if (msg.type === 'ERROR') addChat({ id: crypto.randomUUID(), userId: msg.playerId, email: null, text: msg.message, sentAt: new Date().toISOString() });
          },
          () => setLoading(false),
          (connected) => {
            setWsConnected(connected);
            if (connected && hasConnectedRef.current) void refreshState().catch(() => {});
            if (connected) hasConnectedRef.current = true;
          },
        );
        stompClientRef.current = client;
      } catch {
        if (!cancelled) setLoading(false);
      }
    };
    void setup();
    return () => {
      cancelled = true;
      void stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    };
  }, [addChat, roomCode, setState, spectator]);

  if (loading && !state) {
    return (
      <main className="grid min-h-[calc(100vh-73px)] place-items-center bg-ink text-forge">
        <p>Loading spectator view...</p>
      </main>
    );
  }

  return (
    <main className="relative bg-ink text-slate-100" style={{ height: `calc(100vh - ${NAV_HEIGHT}px)` }} ref={containerRef}>
      <Stage width={size.width} height={size.height}>
        <Layer listening={false}>
          <ZoneOverlay zones={zones} />
        </Layer>
        <Layer listening={false}>
          {(state?.cards ?? [])
            .filter((instance) => !sameZone(instance.zone, 'deck'))
            .sort((a, b) => a.zIndex - b.zIndex)
            .map((instance) => {
              const cardDef = cardsById.get(instance.cardId);
              if (!cardDef) return null;
              return (
                <CardSprite
                  key={instance.instanceId}
                  instance={{ ...instance, faceDown: instance.faceDown || sameZone(instance.zone, 'hand') }}
                  cardDef={cardDef}
                  isOwner={false}
                  selected={false}
                  onDragEnd={() => {}}
                  onClick={() => {}}
                  onDoubleClick={() => {}}
                  onContextMenu={() => {}}
                />
              );
            })}
        </Layer>
      </Stage>
      <button className="btn-secondary pointer-events-auto absolute left-4 top-4 bg-panel/95" onClick={() => navigate('/')}>
        Leave
      </button>
      <div className="absolute right-4 top-4 rounded-sm border border-mint/40 bg-panel/95 px-3 py-2 text-sm font-semibold text-mint">Spectating</div>
      {state ? <ScorePanel players={state.players} onAdjust={() => {}} emailMap={emailMap} /> : null}
      <div className="absolute right-3 top-16 grid w-80 gap-3">
        <GameLog entries={state?.log ?? []} />
        <GameChat messages={chat} onSend={() => {}} readOnly />
      </div>
      {!wsConnected && !loading ? (
        <div className="pointer-events-none absolute inset-x-0 top-0 z-40 flex items-center justify-center gap-2 bg-ember/90 px-4 py-2 text-sm font-medium text-white">
          <span className="animate-pulse">●</span>
          Connection lost — reconnecting…
        </div>
      ) : null}
      {state?.winnerId ? (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center gap-6 bg-ink/90">
          <h1 className="text-5xl font-bold text-forge">{state.players.find((player) => player.userId === state.winnerId)?.userId ?? state.winnerId} wins!</h1>
          <button className="btn-secondary" onClick={() => navigate('/')}>
            Back to Home
          </button>
        </div>
      ) : null}
    </main>
  );
}

export default SpectatorView;
