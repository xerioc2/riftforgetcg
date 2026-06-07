import type React from 'react';
import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { Arrow, Layer, Rect, Stage, Text } from 'react-konva';
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
import { cardTargetScope, unsupportedCardReason } from '../lib/cardActions';
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
const DEFAULT_HAND_HEIGHT = 172;
const CARD_PREVIEW_DELAY_MS = 2000;

function pointZone(x: number, y: number, zones: ZoneRect[], fallback: ZoneName): ZoneName {
  return zones.find((zone) => x >= zone.x && x <= zone.x + zone.width && y >= zone.y && y <= zone.y + zone.height)?.zoneName ?? fallback;
}

function sameZone(zone: string, expected: ZoneName | string) {
  return zone.toLowerCase() === expected.toLowerCase();
}

function autoPlaceInZone(zone: ZoneRect, existingCount: number, cardW = 88, cardH = 112): { x: number; y: number } {
  const cols = Math.max(1, Math.floor((zone.width - 16) / (cardW + 8)));
  const col = existingCount % cols;
  const row = Math.floor(existingCount / cols);
  return {
    x: zone.x + 12 + col * (cardW + 8) + cardW / 2,
    y: zone.y + 8 + row * (cardH / 2 + 8) + cardH / 4,
  };
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
    height: Math.max(480, window.innerHeight - NAV_HEIGHT - PHASE_BAR_HEIGHT - DEFAULT_HAND_HEIGHT),
  });
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null);
  const [cardScale, setCardScale] = useState(1.3);
  const [hoveredCard, setHoveredCard] = useState<RiftCard | null>(null);
  const [pendingAttackers, setPendingAttackers] = useState<Set<string>>(new Set());
  const [pendingBlocker, setPendingBlocker] = useState<string | null>(null);
  const [pendingBlockTarget, setPendingBlockTarget] = useState<string | null>(null);
  const [pendingBlocks, setPendingBlocks] = useState<Map<string, string>>(new Map());
  const [pendingSpellInstanceId, setPendingSpellInstanceId] = useState<string | null>(null);
  const [pendingRuneTaps, setPendingRuneTaps] = useState<Set<string>>(new Set());
  const [mulliganKeepIds, setMulliganKeepIds] = useState<Set<string>>(new Set());
  const [handHeight, setHandHeight] = useState(230);
  const [wsConnected, setWsConnected] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const prevCards = useRef<Map<string, CardInstance>>(new Map());
  const prevState = useRef<LiveGameState | null>(null);
  const pendingAnimations = useRef<Set<string>>(new Set());
  const previewShowTimer = useRef<number | null>(null);
  const previewClearTimer = useRef<number | null>(null);
  const hasConnectedRef = useRef(false);

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
  const handInstanceKey = useMemo(() => hand.map((card) => card.instanceId).sort().join(','), [hand]);
  const hasMulliganed = state?.mulligansDone?.includes(player.id) ?? false;
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
    return () => {
      if (previewShowTimer.current != null) window.clearTimeout(previewShowTimer.current);
      if (previewClearTimer.current != null) window.clearTimeout(previewClearTimer.current);
    };
  }, []);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;
    const observer = new ResizeObserver(([entry]) => {
      setSize({
        width: Math.max(320, entry.contentRect.width - SIDEBAR_WIDTH),
        height: Math.max(480, entry.contentRect.height - PHASE_BAR_HEIGHT - handHeight),
      });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, [handHeight]);

  useEffect(() => {
    if (state?.currentPhase !== 'MAIN') {
      setPendingRuneTaps(new Set());
      setPendingSpellInstanceId(null);
    }
  }, [state?.currentPhase]);

  useEffect(() => {
    if (state?.currentPhase === 'MULLIGAN' && !hasMulliganed) {
      setMulliganKeepIds(new Set(handInstanceKey ? handInstanceKey.split(',') : []));
    }
  }, [handInstanceKey, hasMulliganed, state?.currentPhase]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setPendingSpellInstanceId(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
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
          (connected) => {
            setWsConnected(connected);
            if (connected && hasConnectedRef.current) {
              void fetch(`${config.gameServerUrl}/api/game/${roomCode}/state`)
                .then((r) => r.json() as Promise<LiveGameState>)
                .then(handleIncomingState)
                .catch(() => {});
            }
            if (connected) hasConnectedRef.current = true;
          },
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

  const handleCardHover = (card: RiftCard | null) => {
    if (previewShowTimer.current != null) window.clearTimeout(previewShowTimer.current);
    if (previewClearTimer.current != null) window.clearTimeout(previewClearTimer.current);
    if (card) {
      previewShowTimer.current = window.setTimeout(() => setHoveredCard(card), CARD_PREVIEW_DELAY_MS);
      return;
    }
    previewClearTimer.current = window.setTimeout(() => setHoveredCard(null), 1000);
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

  const commitRunesForCard = (cardDef: RiftCard | undefined) => {
    const playerEnergy = state?.players.find((statePlayer) => statePlayer.userId === player.id)?.availableEnergy ?? 0;
    const selectedRunes = new Set(pendingRuneTaps);
    let plannedEnergy = (state?.runes ?? [])
      .filter((rune) => selectedRunes.has(rune.instanceId))
      .reduce((total, rune) => total + rune.normalEnergy, 0);
    const neededEnergy = Math.max(0, (cardDef?.cost ?? 0) - playerEnergy);
    for (const rune of state?.runes ?? []) {
      if (plannedEnergy >= neededEnergy) break;
      if (rune.ownerId !== player.id || rune.tapped || selectedRunes.has(rune.instanceId)) continue;
      selectedRunes.add(rune.instanceId);
      plannedEnergy += rune.normalEnergy;
    }
    for (const runeId of selectedRunes) {
      publishMove({ type: 'TAP_RUNE', playerId: player.id, runeInstanceId: runeId });
    }
    setPendingRuneTaps(new Set());
  };

  const playFromHand = (instanceId: string) => {
    const cardDef = cardsById.get(state?.cards.find((c) => c.instanceId === instanceId)?.cardId ?? '');
    const unsupportedReason = unsupportedCardReason(cardDef);
    if (unsupportedReason) {
      addChat({ id: crypto.randomUUID(), userId: player.id, email: null, text: unsupportedReason, sentAt: new Date().toISOString() });
      return;
    }
    if (cardTargetScope(cardDef)) {
      setPendingSpellInstanceId(instanceId);
      return;
    }
    commitRunesForCard(cardDef);
    const isRune = cardDef?.type?.toLowerCase() === 'rune';
    if (isRune) {
      const runeZone = zones.find((zone) => zone.zoneName === 'rune' && zone.ownerId === player.id);
      const runeCount = state?.runes?.filter((rune) => rune.ownerId === player.id).length ?? 0;
      const runeX = runeZone ? runeZone.x + 20 + runeCount * Math.max(30, (runeZone.width - 40) / 10) : size.width / 2;
      const runeY = runeZone ? runeZone.y + runeZone.height / 2 : size.height / 2;
      publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'RUNE', x: runeX, y: runeY });
      return;
    }
    const base = zones.find((zone) => zone.zoneName === 'base' && zone.ownerId === player.id);
    const baseCount = state?.cards.filter((card) => card.ownerId === player.id && sameZone(card.zone, 'base')).length ?? 0;
    const position = base ? autoPlaceInZone(base, baseCount) : { x: size.width / 2, y: size.height / 2 };
    publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'BASE', ...position });
  };

  const discardFromHand = (instanceId: string) => {
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: 'DISCARD', x: size.width - 76, y: size.height - 170 });
  };

  const moveInstance = (instanceId: string, x: number, y: number) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    if (!instance) return;
    const zone = pointZone(x, y, zones, instance.zone);
    const targetZoneRect = zones.find((candidate) => candidate.zoneName === zone && candidate.ownerId === instance.ownerId) ?? zones.find((candidate) => candidate.zoneName === zone);
    const snappedX = targetZoneRect ? Math.max(targetZoneRect.x + 12, Math.min(targetZoneRect.x + targetZoneRect.width - 12, x)) : x;
    const snappedY = targetZoneRect ? Math.max(targetZoneRect.y + 12, Math.min(targetZoneRect.y + targetZoneRect.height - 12, y)) : y;
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: zone.toUpperCase().replace(/-/g, '_'), x: snappedX, y: snappedY });
  };

  const handleCardClick = (instanceId: string) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    if (!instance) return;
    const isOwnBattlefieldCard = instance.ownerId === player.id && sameZone(instance.zone, 'battlefield');
    const isEnemyBattlefieldCard = instance.ownerId !== player.id && sameZone(instance.zone, 'battlefield');

    const pendingCard = cardsById.get(state?.cards.find((card) => card.instanceId === pendingSpellInstanceId)?.cardId ?? '');
    const targetScope = cardTargetScope(pendingCard);
    const isValidPendingTarget = targetScope === 'friendly' ? isOwnBattlefieldCard : targetScope === 'enemy' ? isEnemyBattlefieldCard : isOwnBattlefieldCard || isEnemyBattlefieldCard;
    if (pendingSpellInstanceId && isValidPendingTarget) {
      const spellInstance = state?.cards.find((card) => card.instanceId === pendingSpellInstanceId);
      if (!spellInstance) {
        setPendingSpellInstanceId(null);
        return;
      }
      const spellDef = cardsById.get(spellInstance.cardId);
      commitRunesForCard(spellDef);
      publishMove({
        type: 'PLAY_CARD',
        playerId: player.id,
        instanceId: pendingSpellInstanceId,
        targetZone: 'BASE',
        x: 0,
        y: 0,
        targetInstanceId: instanceId,
      });
      setPendingSpellInstanceId(null);
      return;
    }

    if (state?.currentPhase === 'ATTACK_DECLARE' && isMyTurn) {
      if (!isOwnBattlefieldCard) return;
      setPendingAttackers((previous) => {
        const next = new Set(previous);
        if (next.has(instanceId)) next.delete(instanceId);
        else next.add(instanceId);
        return next;
      });
      return;
    }

    if (state?.currentPhase === 'BLOCK_DECLARE' && canPass) {
      if (isOwnBattlefieldCard) {
        if (pendingBlocks.has(instanceId)) {
          setPendingBlocks((previous) => {
            const next = new Map(previous);
            next.delete(instanceId);
            return next;
          });
          setPendingBlocker(null);
          setPendingBlockTarget(null);
        } else if (!instance.tapped) {
          if (pendingBlockTarget) {
            setPendingBlocks((previous) => new Map(previous).set(instanceId, pendingBlockTarget));
            setPendingBlockTarget(null);
          } else {
            setPendingBlocker(instanceId);
          }
        }
        return;
      }
      const isDeclaredAttacker = state.declaredAttackers?.includes(instanceId);
      if (!isEnemyBattlefieldCard || !isDeclaredAttacker) return;
      if (pendingBlocker) {
        setPendingBlocks((previous) => new Map(previous).set(pendingBlocker, instanceId));
        setPendingBlocker(null);
      } else {
        const existingBlocker = [...pendingBlocks.entries()].find(([, attackerId]) => attackerId === instanceId)?.[0];
        if (existingBlocker) {
          setPendingBlocks((previous) => {
            const next = new Map(previous);
            next.delete(existingBlocker);
            return next;
          });
        } else {
          setPendingBlockTarget(instanceId);
        }
      }
      return;
    }

    setSelectedInstanceId(instanceId);
  };

  const handleRuneTap = (runeInstanceId: string) => {
    const rune = state?.runes?.find((candidate) => candidate.instanceId === runeInstanceId);
    if (!rune || rune.ownerId !== player.id) return;
    setPendingRuneTaps((previous) => {
      const next = new Set(previous);
      if (next.has(runeInstanceId)) next.delete(runeInstanceId);
      else if (!rune.tapped) next.add(runeInstanceId);
      return next;
    });
  };

  const clearPendingDeclarations = () => {
    setPendingAttackers(new Set());
    setPendingBlocker(null);
    setPendingBlockTarget(null);
    setPendingBlocks(new Map());
  };

  const handlePassPhase = () => {
    const opponentId = state?.players.find((statePlayer) => statePlayer.userId !== player.id)?.userId;
    if (state?.currentPhase === 'ATTACK_DECLARE' && pendingAttackers.size > 0 && opponentId) {
      publishMove({ type: 'DECLARE_ATTACK', playerId: player.id, attackerInstanceIds: [...pendingAttackers], targetPlayerId: opponentId });
    }
    if (state?.currentPhase === 'BLOCK_DECLARE' && pendingBlocks.size > 0) {
      publishMove({ type: 'DECLARE_BLOCK', playerId: player.id, blockerToAttacker: Object.fromEntries(pendingBlocks) });
    }
    publishMove({ type: 'PASS_PHASE', playerId: player.id });
    clearPendingDeclarations();
  };

  const submitMulligan = (keepInstanceIds: string[]) => {
    publishMove({ type: 'MULLIGAN', playerId: player.id, keepInstanceIds });
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
  const opponentName = opponent?.name?.trim() || opponent?.userId || 'Opponent';
  const opponentHand = state.cards.filter((card) => card.ownerId === opponent?.userId && sameZone(card.zone, 'hand')).length;
  const myUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === player.id && !rune.tapped).length;
  const spendableEnergy = (me?.availableEnergy ?? 0)
    + (state.runes ?? [])
        .filter((rune) => rune.ownerId === player.id && !rune.tapped)
        .reduce((total, rune) => total + rune.normalEnergy, 0);
  const opponentUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === opponent?.userId && !rune.tapped).length;
  const isMyTurn = state.activePlayerId === player.id;
  const canPass =
    state.currentPhase === 'MULLIGAN'
      ? false
      : state.currentPhase === 'BLOCK_DECLARE'
      ? !isMyTurn && state.players.some((statePlayer) => statePlayer.userId === player.id)
      : isMyTurn;

  return (
    <main className="relative bg-ink text-slate-100" style={{ height: `calc(100vh - ${NAV_HEIGHT}px)` }} ref={containerRef}>
      <Stage width={size.width} height={size.height} onMouseDown={(event) => event.target === event.target.getStage() && setSelectedInstanceId(null)}>
        <Layer listening={false}>
          <ZoneOverlay zones={zones} />
        </Layer>
        <Layer listening={false}>
          {state.currentPhase === 'ATTACK_DECLARE' && isMyTurn
            ? state.cards
                .filter((card) => pendingAttackers.has(card.instanceId))
                .map((card) => (
                  <Rect
                    key={card.instanceId}
                    x={card.x - 44}
                    y={card.y - 60}
                    width={88}
                    height={120}
                    fill="rgba(216,176,93,0.18)"
                    stroke="#d8b05d"
                    shadowColor="#d8b05d"
                    shadowBlur={24}
                    shadowOpacity={1}
                    cornerRadius={6}
                  />
                ))
            : null}
          {state.currentPhase === 'BLOCK_DECLARE' && canPass
            ? state.cards
                .filter((card) => card.ownerId !== player.id && sameZone(card.zone, 'battlefield') && state.declaredAttackers?.includes(card.instanceId))
                .map((card) => {
                  const blocked = [...pendingBlocks.values()].includes(card.instanceId);
                  const selected = pendingBlockTarget === card.instanceId;
                  const color = selected ? '#d8b05d' : blocked ? '#6fd3b6' : '#e56c4f';
                  return (
                    <Fragment key={card.instanceId}>
                      <Rect
                        x={card.x - 44}
                        y={card.y - 60}
                        width={88}
                        height={120}
                        fill={selected ? 'rgba(216,176,93,0.18)' : blocked ? 'rgba(111,211,182,0.10)' : 'rgba(229,108,79,0.10)'}
                        stroke={color}
                        strokeWidth={blocked ? 2 : 1}
                        shadowColor={color}
                        shadowBlur={selected ? 24 : blocked ? 0 : 20}
                        cornerRadius={6}
                      />
                      {blocked ? <Text x={card.x + 28} y={card.y - 56} text="✓" fontSize={18} fontStyle="bold" fill="#6fd3b6" /> : null}
                    </Fragment>
                  );
                })
            : null}
          {state.currentPhase === 'BLOCK_DECLARE' && canPass && pendingBlocker
            ? (() => {
                const card = cardPositions.get(pendingBlocker);
                return card ? <Rect x={card.x - 44} y={card.y - 60} width={88} height={120} fill="rgba(111,211,182,0.18)" stroke="#6fd3b6" shadowColor="#6fd3b6" shadowBlur={20} cornerRadius={6} /> : null;
              })()
            : null}
          {state.currentPhase === 'BLOCK_DECLARE' && canPass && pendingBlockTarget
            ? state.cards
                .filter((card) => card.ownerId === player.id && sameZone(card.zone, 'battlefield') && !card.tapped && !pendingBlocks.has(card.instanceId))
                .map((card) => <Rect key={card.instanceId} x={card.x - 44} y={card.y - 60} width={88} height={120} fill="rgba(111,211,182,0.08)" stroke="#6fd3b6" shadowColor="#6fd3b6" shadowBlur={10} cornerRadius={6} />)
            : null}
          {state.currentPhase === 'BLOCK_DECLARE' && canPass
            ? [...pendingBlocks.entries()].map(([blockerId, attackerId]) => {
                const blocker = cardPositions.get(blockerId);
                const attacker = cardPositions.get(attackerId);
                if (!blocker || !attacker) return null;
                return (
                  <Fragment key={blockerId}>
                    <Rect x={blocker.x - 44} y={blocker.y - 60} width={88} height={120} stroke="#6fd3b6" strokeWidth={2} cornerRadius={6} />
                    <Arrow points={[blocker.x, blocker.y, attacker.x, attacker.y]} stroke="#6fd3b6" strokeWidth={2} fill="#6fd3b6" pointerLength={8} pointerWidth={6} />
                  </Fragment>
                );
              })
            : null}
          {state.currentPhase === 'ATTACK_DECLARE' && isMyTurn ? <Text x={0} y={8} width={size.width} text="Click your units to attack - Pass when ready" align="center" fontSize={13} fontStyle="bold" fill="#d8b05d" /> : null}
          {state.currentPhase === 'BLOCK_DECLARE' && canPass ? (
            <Fragment>
              <Text
                x={0}
                y={8}
                width={size.width}
                text={pendingBlockTarget ? 'Now click one of your units to block' : pendingBlocker ? 'Now click the attacker you want to block' : 'Click an attacker to block it, or Pass to let them through'}
                align="center"
                fontSize={13}
                fontStyle="bold"
                fill="#6fd3b6"
              />
              <Text x={0} y={27} width={size.width} text={`${pendingBlocks.size} of ${state.declaredAttackers?.length ?? 0} attacks covered`} align="center" fontSize={11} fill="#94a3b8" />
            </Fragment>
          ) : null}
          {pendingSpellInstanceId
            ? state.cards
                .filter((card) => {
                  if (!sameZone(card.zone, 'battlefield')) return false;
                  const pendingCardDef = cardsById.get(state.cards.find((candidate) => candidate.instanceId === pendingSpellInstanceId)?.cardId ?? '');
                  const scope = cardTargetScope(pendingCardDef);
                  return scope === 'friendly' ? card.ownerId === player.id : scope === 'enemy' ? card.ownerId !== player.id : true;
                })
                .map((card) => <Rect key={`target-${card.instanceId}`} x={card.x - 44} y={card.y - 60} width={88} height={120} fill="rgba(229,108,79,0.12)" stroke="#e56c4f" shadowColor="#e56c4f" shadowBlur={20} cornerRadius={6} />)
            : null}
          {pendingSpellInstanceId ? <Text x={0} y={8} width={size.width} text="Click a highlighted unit to target it  ·  Esc to cancel" align="center" fontSize={13} fontStyle="bold" fill="#e56c4f" /> : null}
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
                pending={pendingRuneTaps.has(rune.instanceId)}
                x={position.x}
                y={position.y}
                onTap={handleRuneTap}
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
              const specialZone = sameZone(instance.zone, 'champion') || sameZone(instance.zone, 'legend') ? zones.find((zone) => sameZone(zone.zoneName, instance.zone) && zone.ownerId === instance.ownerId) : undefined;
              const displayInstance = specialZone ? { ...instance, x: specialZone.x + 48, y: specialZone.y + 60 } : instance;
              return (
                <CardSprite
                  key={instance.instanceId}
                  instance={displayInstance}
                  cardDef={cardDef}
                  isOwner={instance.ownerId === player.id}
                  selected={selectedInstanceId === instance.instanceId}
                  onDragEnd={moveInstance}
                  onClick={handleCardClick}
                  onDoubleClick={(id) => {
                    if (!['ATTACK_DECLARE', 'BLOCK_DECLARE'].includes(state.currentPhase ?? '')) publishMove({ type: 'TAP_CARD', playerId: player.id, instanceId: id });
                  }}
                  onContextMenu={(id) => {
                    if (!['ATTACK_DECLARE', 'BLOCK_DECLARE'].includes(state.currentPhase ?? '')) publishMove({ type: 'FLIP_CARD', playerId: player.id, instanceId: id });
                  }}
                  animate={pendingAnimations.current.delete(instance.instanceId)}
                  scale={cardScale}
                  onHover={handleCardHover}
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
          effectiveEnergy={(me.availableEnergy ?? 0) + pendingRuneTaps.size}
          untappedRunes={myUntappedRunes}
          isActive={state.activePlayerId === player.id}
          isMe
          bottom={handHeight + PHASE_BAR_HEIGHT + 4}
        />
      ) : null}
      <PhaseBar currentPhase={state.currentPhase ?? 'MAIN'} isMyTurn={isMyTurn} canPass={canPass} opponentName={opponentName} onPassPhase={handlePassPhase} bottom={handHeight} />
      <GameSidebar log={state.log} chat={chat} deckCards={deckCards} onSend={sendChat} onDeal={dealCard} onDrawHand={drawHand} onHover={handleCardHover} />
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
      <div
        className="pointer-events-auto absolute left-0 z-30 flex h-2 cursor-ns-resize items-center justify-center bg-line/50 hover:bg-forge/40"
        style={{ bottom: handHeight + PHASE_BAR_HEIGHT - 4, right: `${SIDEBAR_WIDTH}px` }}
        onMouseDown={(event) => {
          event.preventDefault();
          const startY = event.clientY;
          const startHeight = handHeight;
          const onMove = (moveEvent: MouseEvent) => {
            const delta = startY - moveEvent.clientY;
            setHandHeight(Math.max(120, Math.min(320, startHeight + delta)));
          };
          const onUp = () => {
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup', onUp);
          };
          window.addEventListener('mousemove', onMove);
          window.addEventListener('mouseup', onUp);
        }}
      >
        <div className="h-0.5 w-16 rounded-full bg-slate-600" />
      </div>
      <div className="absolute bottom-0 left-0" style={{ right: `${SIDEBAR_WIDTH}px`, height: handHeight }}>
        <HandRack
          instances={hand}
          cards={cardsById}
          onPlay={playFromHand}
          onDiscard={discardFromHand}
          cardScale={cardScale}
          onHover={handleCardHover}
          effectiveEnergy={spendableEnergy}
          canPlayCards={state.currentPhase === 'MAIN' && isMyTurn}
          embedded
          maxHeight={handHeight}
        />
      </div>
      <CardPreview card={hoveredCard} />
      {state.currentPhase === 'MULLIGAN' ? (
        <div className="absolute inset-0 z-40 flex items-center justify-center bg-ink/95 px-6 py-8">
          {hasMulliganed ? (
            <section className="border border-line bg-panel px-10 py-8 text-center shadow-glow">
              <h2 className="text-xl font-semibold text-forge">Opening hand locked in</h2>
              <p className="mt-2 text-sm text-slate-400">Waiting for opponent...</p>
            </section>
          ) : (
            <section className="w-full max-w-5xl border border-line bg-panel p-6 shadow-glow">
              <div className="mb-5 flex items-end justify-between gap-4">
                <div>
                  <h2 className="text-2xl font-semibold text-forge">Choose your opening hand</h2>
                  <p className="mt-1 text-sm text-slate-400">Checked cards stay. Unchecked cards are shuffled back and replaced.</p>
                </div>
                <span className="text-sm font-semibold text-slate-300">{mulliganKeepIds.size} kept</span>
              </div>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
                {hand.map((instance) => {
                  const card = cardsById.get(instance.cardId);
                  const checked = mulliganKeepIds.has(instance.instanceId);
                  return (
                    <label
                      key={instance.instanceId}
                      className={`cursor-pointer border p-2 transition-colors ${checked ? 'border-forge bg-forge/10' : 'border-line bg-ink/60'}`}
                    >
                      <input
                        type="checkbox"
                        className="mb-2 accent-forge"
                        checked={checked}
                        onChange={() =>
                          setMulliganKeepIds((previous) => {
                            const next = new Set(previous);
                            if (next.has(instance.instanceId)) next.delete(instance.instanceId);
                            else next.add(instance.instanceId);
                            return next;
                          })
                        }
                      />
                      {card?.imageUrl ? <img className="aspect-[5/7] w-full object-contain" src={card.imageUrl} alt={card.name} /> : null}
                      <span className="mt-2 block truncate text-sm font-semibold text-slate-100">{card?.name ?? 'Unknown card'}</span>
                    </label>
                  );
                })}
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button className="btn-secondary" onClick={() => submitMulligan([...mulliganKeepIds])}>
                  Keep Selected
                </button>
                <button className="btn-primary" onClick={() => submitMulligan(hand.map((card) => card.instanceId))}>
                  Keep All
                </button>
              </div>
            </section>
          )}
        </div>
      ) : null}
      {!wsConnected && !loading && !error ? (
        <div className="pointer-events-none absolute inset-x-0 top-0 z-40 flex items-center justify-center gap-2 bg-ember/90 px-4 py-2 text-sm font-medium text-white">
          <span className="animate-pulse">●</span>
          Connection lost — reconnecting…
        </div>
      ) : null}
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
