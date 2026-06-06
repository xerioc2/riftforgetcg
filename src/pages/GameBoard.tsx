import type React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { Arrow, Layer, Rect, Stage } from 'react-konva';
import { useNavigate, useParams } from 'react-router-dom';
import { CardPreview } from '../components/CardPreview';
import { GameSidebar } from '../components/GameSidebar';
import { HandRack } from '../components/HandRack';
import { PhaseBar } from '../components/PhaseBar';
import { PlayerPanel } from '../components/PlayerPanel';
import { CardSprite } from '../components/board/CardSprite';
import { RuneSprite } from '../components/board/RuneSprite';
import { computeLayout, type ZoneRect } from '../components/board/BoardLayout';
import { ZoneOverlay } from '../components/board/ZoneOverlay';
import { config } from '../lib/env';
import { useLocalPlayer } from '../lib/playerContext';
import { play, preload } from '../lib/sfx';
import { createGameClient, joinGame, sendMove } from '../lib/stompGame';
import { useCardStore } from '../store/cards';
import { useDeckStore } from '../store/decks';
import { useGameStore } from '../store/game';
import type { CardInstance, LiveGameState, RiftCard, ZoneName } from '../types';

const NAV_HEIGHT = 73;
const SIDEBAR_WIDTH = 280;
const PHASE_BAR_HEIGHT = 48;
const HAND_HEIGHT = 172;
const BOTTOM_CHROME = PHASE_BAR_HEIGHT + HAND_HEIGHT;

function pointZone(x: number, y: number, zones: ZoneRect[], fallback: ZoneName): ZoneName {
  return zones.find((zone) => x >= zone.x && x <= zone.x + zone.width && y >= zone.y && y <= zone.y + zone.height)?.zoneName ?? fallback;
}

function sameZone(zone: string, expected: ZoneName | string) {
  return zone.toLowerCase() === expected.toLowerCase();
}

export function GameBoard() {
  const { code } = useParams();
  const navigate = useNavigate();
  const player = useLocalPlayer();
  const { cards, loadCards } = useCardStore();
  const { decks, activeDeckId } = useDeckStore();
  const { state, chat, setState, addChat } = useGameStore();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [size, setSize] = useState({
    width: Math.max(320, window.innerWidth - SIDEBAR_WIDTH),
    height: Math.max(480, window.innerHeight - NAV_HEIGHT - BOTTOM_CHROME),
  });
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null);
  const [cardScale, setCardScale] = useState(1.3);
  const [hoveredCard, setHoveredCard] = useState<RiftCard | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const prevCards = useRef<Map<string, CardInstance>>(new Map());
  const prevState = useRef<LiveGameState | null>(null);
  const pendingAnimations = useRef<Set<string>>(new Set());

  const roomCode = code?.toUpperCase() ?? '';
  const activeDeck = decks.find((deck) => deck.id === activeDeckId) ?? decks[0];
  const cardsById = useMemo(() => new Map(cards.map((card) => [card.id, card])), [cards]);
  const playerIds = useMemo(() => {
    const ids = state?.players.map((item) => item.userId) ?? [];
    return [player.id, ...ids.filter((id) => id !== player.id)];
  }, [player.id, state?.players]);
  const zones = useMemo(() => computeLayout(size.width, size.height, playerIds), [playerIds, size.height, size.width]);
  const runePositions = useMemo(() => {
    const positionsByOwner = new Map<string, { x: number; y: number }[]>();
    for (const playerId of playerIds) {
      const zone = zones.find((candidate) => candidate.zoneName === 'rune' && candidate.ownerId === playerId);
      if (!zone) continue;
      const positions = Array.from({ length: 8 }, (_, index) => ({
        x: zone.x + 20 + index * Math.max(30, (zone.width - 40) / 7),
        y: zone.y + zone.height / 2,
      }));
      positionsByOwner.set(playerId, positions);
    }
    return positionsByOwner;
  }, [playerIds, zones]);
  const cardPositions = useMemo(() => new Map((state?.cards ?? []).map((card) => [card.instanceId, { x: card.x, y: card.y }])), [state?.cards]);
  const hand = useMemo(() => (state?.cards ?? []).filter((card) => card.ownerId === player.id && sameZone(card.zone, 'hand')), [player.id, state?.cards]);
  const activeBattlefieldCards = useMemo(
    () => (state?.cards ?? []).filter((card) => state?.currentPhase === 'ATTACK_DECLARE' && card.ownerId === state.activePlayerId && sameZone(card.zone, 'battlefield')),
    [state?.activePlayerId, state?.cards, state?.currentPhase],
  );
  const deckCards = useMemo(() => {
    const onBoardCounts = new Map<string, number>();
    for (const instance of state?.cards ?? []) {
      if (instance.ownerId === player.id) onBoardCounts.set(instance.cardId, (onBoardCounts.get(instance.cardId) ?? 0) + 1);
    }
    return activeDeck.cards
      .map((entry) => {
        const card = cardsById.get(entry.cardId);
        if (!card || card.type === 'Champion') return null;
        return { card, remaining: Math.max(0, entry.quantity - (onBoardCounts.get(entry.cardId) ?? 0)) };
      })
      .filter((entry): entry is { card: RiftCard; remaining: number } => Boolean(entry && entry.remaining > 0));
  }, [activeDeck.cards, cardsById, player.id, state?.cards]);

  useEffect(() => {
    if (cards.length === 0) void loadCards();
  }, [cards.length, loadCards]);

  useEffect(() => {
    preload();
  }, []);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;
    const observer = new ResizeObserver(([entry]) => {
      setSize({
        width: Math.max(320, entry.contentRect.width - SIDEBAR_WIDTH),
        height: Math.max(480, entry.contentRect.height - BOTTOM_CHROME),
      });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  const handleIncomingState = useCallback(
    (incoming: LiveGameState) => {
      const prevPhase = prevState.current?.currentPhase;
      const prevScore = prevState.current?.players.find((item) => item.userId === player.id)?.score ?? 0;
      const newScore = incoming.players.find((item) => item.userId === player.id)?.score ?? 0;
      let newBattlefieldCards = 0;
      incoming.cards.forEach((card) => {
        const prev = prevCards.current.get(card.instanceId);
        if (prev && sameZone(prev.zone, 'hand') && sameZone(card.zone, 'base')) {
          pendingAnimations.current.add(card.instanceId);
          newBattlefieldCards += 1;
        }
        if (prev && prev.tapped !== card.tapped) void play('tap');
      });
      if (newScore > prevScore) void play('score');
      if (incoming.winnerId === player.id && prevState.current?.winnerId !== incoming.winnerId) void play('victory');
      else if (incoming.winnerId && prevState.current?.winnerId !== incoming.winnerId) void play('defeat');
      if (newBattlefieldCards > 0) void play('cardPlay');
      if (incoming.currentPhase === 'COMBAT_RESOLVE' && prevPhase !== 'COMBAT_RESOLVE') void play('attack');
      prevCards.current = new Map(incoming.cards.map((card) => [card.instanceId, card]));
      prevState.current = incoming;
      setState(incoming);
    },
    [player.id, setState],
  );

  useEffect(() => {
    if (!roomCode) return;
    let cancelled = false;
    const setup = async () => {
      try {
        const stateResponse = await fetch(`${config.gameServerUrl}/api/game/${roomCode}/state`);
        if (!stateResponse.ok) throw new Error('Game state not found.');
        const initialState = (await stateResponse.json()) as LiveGameState;
        if (cancelled) return;
        handleIncomingState(initialState);

        const client = createGameClient(
          roomCode,
          player,
          (msg) => {
            if (msg.type === 'STATE_UPDATE') handleIncomingState(msg.state);
            if (msg.type === 'ERROR') addChat({ id: crypto.randomUUID(), userId: msg.playerId, email: null, text: msg.message, sentAt: new Date().toISOString() });
          },
          () => joinGame(client, roomCode),
        );
        if (cancelled) {
          void client.deactivate();
          return;
        }
        stompClientRef.current = client;
        setLoading(false);
      } catch (setupError) {
        if (!cancelled) {
          setError(setupError instanceof Error ? setupError.message : 'Unable to load game.');
          setLoading(false);
        }
      }
    };
    void setup();
    return () => {
      cancelled = true;
      void stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    };
  }, [addChat, handleIncomingState, player, roomCode]);

  const publishMove = (move: Parameters<typeof sendMove>[2]) => {
    if (!stompClientRef.current) return;
    sendMove(stompClientRef.current, roomCode, move);
  };

  const dealCard = (card: RiftCard) => {
    publishMove({ type: 'DEAL_CARD', playerId: player.id, cardId: card.id, targetZone: 'HAND', x: size.width / 2, y: size.height - 54 });
  };

  const drawHand = () => {
    const pool = deckCards.flatMap(({ card, remaining }) => Array.from({ length: remaining }, () => card));
    [...pool]
      .sort(() => Math.random() - 0.5)
      .slice(0, 5)
      .forEach((card, index) => {
        publishMove({ type: 'DEAL_CARD', playerId: player.id, cardId: card.id, targetZone: 'HAND', x: 120 + index * 92, y: size.height - 54 });
      });
  };

  const playFromHand = (instanceId: string) => {
    const base = zones.find((zone) => zone.zoneName === 'base' && zone.ownerId === player.id) ?? zones.find((zone) => zone.zoneName === 'base');
    const baseCount = state?.cards.filter((card) => card.ownerId === player.id && sameZone(card.zone, 'base')).length ?? 0;
    publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'BASE', x: base ? base.x + 80 + baseCount * 90 : size.width / 2, y: base ? base.y + 80 : size.height / 2 });
  };

  const discardFromHand = (instanceId: string) => {
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: 'DISCARD', x: size.width - 76, y: size.height - 170 });
  };

  const moveInstance = (instanceId: string, x: number, y: number) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    if (!instance) return;
    const zone = pointZone(x, y, zones, instance.zone);
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: zone.toUpperCase().replace(/-/g, '_'), x, y });
  };

  const sendChat = (text: string) => {
    addChat({ id: crypto.randomUUID(), userId: player.id, email: player.name, text, sentAt: new Date().toISOString() });
  };

  const handleReset = async () => {
    await fetch(`${config.gameServerUrl}/api/game/${roomCode}/reset`, { method: 'POST' });
  };

  if (loading) return <CenteredState>Loading game...</CenteredState>;

  if (error || !state) {
    return (
      <CenteredState>
        <p>{error || 'Game unavailable.'}</p>
        <button className="btn-secondary mt-4" onClick={() => navigate('/')}>
          Leave
        </button>
      </CenteredState>
    );
  }

  const opponent = state.players.find((statePlayer) => statePlayer.userId !== player.id);
  const me = state.players.find((statePlayer) => statePlayer.userId === player.id);
  const opponentName = opponent?.userId === 'bot-player-riftbot' ? 'RiftBot' : opponent?.userId ?? 'Opponent';
  const opponentHand = state.cards.filter((card) => card.ownerId === opponent?.userId && sameZone(card.zone, 'hand')).length;
  const myUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === player.id && !rune.tapped).length;
  const opponentUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === opponent?.userId && !rune.tapped).length;
  const isMyTurn = state.activePlayerId === player.id;
  const canPass =
    state.currentPhase === 'BLOCK_DECLARE'
      ? !isMyTurn && state.players.some((statePlayer) => statePlayer.userId === player.id)
      : isMyTurn;

  return (
    <main className="relative bg-ink text-slate-100" style={{ height: `calc(100vh - ${NAV_HEIGHT}px)` }} ref={containerRef}>
      <Stage width={size.width} height={size.height} onMouseDown={(event) => event.target === event.target.getStage() && setSelectedInstanceId(null)}>
        <Layer listening={false}>
          <ZoneOverlay zones={zones} />
        </Layer>
        <Layer listening={false}>
          {activeBattlefieldCards.map((card) => (
            <Rect key={card.instanceId} x={card.x - 44} y={card.y - 60} width={88} height={120} fill="rgba(216,176,93,0.08)" stroke="#d8b05d" shadowColor="#d8b05d" shadowBlur={16} shadowOpacity={0.9} cornerRadius={6} />
          ))}
        </Layer>
        <Layer>
          {(state.runes ?? []).map((rune) => {
            const ownerRunes = (state.runes ?? []).filter((candidate) => candidate.ownerId === rune.ownerId);
            const position = runePositions.get(rune.ownerId)?.[ownerRunes.indexOf(rune)];
            if (!position) return null;
            return (
              <RuneSprite
                key={rune.instanceId}
                instanceId={rune.instanceId}
                tapped={rune.tapped}
                normalEnergy={rune.normalEnergy}
                isOwner={rune.ownerId === player.id}
                x={position.x}
                y={position.y}
                onTap={(id) => publishMove({ type: 'TAP_RUNE', playerId: player.id, runeInstanceId: id })}
                onDiscard={(id) => publishMove({ type: 'DISCARD_RUNE', playerId: player.id, runeInstanceId: id })}
              />
            );
          })}
        </Layer>
        <Layer>
          {[...state.cards]
            .filter((instance) => !sameZone(instance.zone, 'hand') && !sameZone(instance.zone, 'deck'))
            .sort((a, b) => a.zIndex - b.zIndex)
            .map((instance) => {
              const cardDef = cardsById.get(instance.cardId);
              if (!cardDef) return null;
              return (
                <CardSprite
                  key={instance.instanceId}
                  instance={instance}
                  cardDef={cardDef}
                  isOwner={instance.ownerId === player.id}
                  selected={selectedInstanceId === instance.instanceId}
                  onDragEnd={moveInstance}
                  onClick={setSelectedInstanceId}
                  onDoubleClick={(id) => publishMove({ type: 'TAP_CARD', playerId: player.id, instanceId: id })}
                  onContextMenu={(id) => publishMove({ type: 'FLIP_CARD', playerId: player.id, instanceId: id })}
                  animate={pendingAnimations.current.delete(instance.instanceId)}
                  scale={cardScale}
                  onHover={setHoveredCard}
                />
              );
            })}
        </Layer>
        <Layer listening={false}>
          {state.currentPhase === 'BLOCK_DECLARE'
            ? Object.entries(state.blockerToAttacker ?? {}).map(([blockerId, attackerId]) => {
                const blocker = cardPositions.get(blockerId);
                const attacker = cardPositions.get(attackerId);
                if (!blocker || !attacker) return null;
                return <Arrow key={blockerId} points={[blocker.x + 40, blocker.y + 56, attacker.x + 40, attacker.y + 56]} stroke="#e56c4f" strokeWidth={2} fill="#e56c4f" pointerLength={8} pointerWidth={6} />;
              })
            : null}
        </Layer>
      </Stage>

      {opponent ? (
        <PlayerPanel name={opponentName} score={opponent.score} handCount={opponentHand} energy={opponent.availableEnergy ?? 0} untappedRunes={opponentUntappedRunes} isActive={state.activePlayerId === opponent.userId} isMe={false} />
      ) : null}
      {me ? (
        <PlayerPanel
          name={player.name}
          score={me.score}
          handCount={hand.length}
          energy={me.availableEnergy ?? 0}
          untappedRunes={myUntappedRunes}
          isActive={state.activePlayerId === player.id}
          isMe
        />
      ) : null}
      <PhaseBar currentPhase={state.currentPhase ?? 'MAIN'} isMyTurn={isMyTurn} canPass={canPass} opponentName={opponentName} onPassPhase={() => publishMove({ type: 'PASS_PHASE', playerId: player.id })} />
      <GameSidebar log={state.log} chat={chat} deckCards={deckCards} onSend={sendChat} onDeal={dealCard} onDrawHand={drawHand} onHover={setHoveredCard} />
      <div className="pointer-events-auto absolute right-[288px] top-2 z-20 flex items-center gap-2">
        <details className="relative">
          <summary className="icon-btn cursor-pointer select-none text-xs" title="Settings" aria-label="Settings">
            &#9881;
          </summary>
          <div className="absolute right-0 top-10 flex items-center gap-2 border border-line bg-panel p-2 shadow-glow">
            <label className="text-xs text-slate-300" htmlFor="card-size">
              Size
            </label>
            <input id="card-size" type="range" min="0.8" max="2" step="0.1" value={cardScale} onChange={(event) => setCardScale(Number(event.target.value))} className="w-24 accent-forge" />
            <span className="w-8 text-right text-xs text-forge">{Math.round(cardScale * 100)}%</span>
          </div>
        </details>
        <button className="btn-secondary min-h-8 px-3 py-1 text-xs" onClick={() => navigate('/')}>
          Leave
        </button>
      </div>
      <div className="absolute bottom-0 left-0" style={{ right: `${SIDEBAR_WIDTH}px` }}>
        <HandRack instances={hand} cards={cardsById} onPlay={playFromHand} onDiscard={discardFromHand} cardScale={cardScale} onHover={setHoveredCard} embedded />
      </div>
      <CardPreview card={hoveredCard} />
      {state.winnerId ? (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center gap-6 bg-ink/90">
          <h1 className={`text-5xl font-bold ${state.winnerId === player.id ? 'text-forge' : 'text-ember'}`}>{state.winnerId === player.id ? 'Victory!' : 'Defeat'}</h1>
          <div className="flex gap-4">
            <button className="btn-primary" onClick={() => void handleReset()}>
              Play Again
            </button>
            <button className="btn-secondary" onClick={() => navigate('/')}>
              Leave
            </button>
          </div>
        </div>
      ) : null}
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

export default GameBoard;
