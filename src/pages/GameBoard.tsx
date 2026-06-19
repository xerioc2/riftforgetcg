import type React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { Layer, Rect, RegularPolygon, Stage, Text } from 'react-konva';
import { useNavigate, useParams } from 'react-router-dom';
import { CardInspectModal } from '../components/CardInspectModal';
import { CardPreview } from '../components/CardPreview';
import { GameSidebar } from '../components/GameSidebar';
import { HandRack } from '../components/HandRack';
import { PhaseBar } from '../components/PhaseBar';
import { PlayerPanel } from '../components/PlayerPanel';
import { BattlefieldLocationDisplay } from '../components/board/BattlefieldLocationDisplay';
import { CardSprite } from '../components/board/CardSprite';
import { RuneSprite } from '../components/board/RuneSprite';
import {
  autoPlaceInZone,
  battlefieldLocationForPoint,
  battlefieldLocationLabel,
  battlefieldZoneForCard,
  clampPlacementToZone,
  computeLayout,
  DEFAULT_BATTLEFIELD_LOCATION_ID,
  normalizeBattlefieldLocationId,
  runeSlotPositions,
  type ZoneRect,
} from '../components/board/BoardLayout';
import { ZoneOverlay } from '../components/board/ZoneOverlay';
import { getGameServerUrl } from '../lib/env';
import { readableHttpError } from '../lib/http';
import { canUseSupportedChainResponse, hasUnsupportedAdditionalCost, isActionCard, isAmbushCard, isDefyCounterCard, isDisciplineReactionCard, isEnGardeReactionCard, isEquipCard, isGustReactionCard, isLegalTargetForMode, isNotSoFastCounterCard, isReactionCard, multiTargetRequirementsForCard, noLegalTargetsMessage, targetModeForCard, targetPromptForMode, unsupportedCardReason, type TargetMode, type TargetRequirement, type TargetRole } from '../lib/cardActions';
import { appBuildLabel, APP_VERSION, BUILD_DATE, BUILD_TAG } from '../lib/appMetadata';
import { buildDebugInfo } from '../lib/debugInfo';
import { attachedGearDisplayPosition, attachedGearDisplayScale, attachedGearIsIndependentBoardPiece, attachedGearNamesForHost } from '../lib/attachments';
import { battlefieldLaneDisplays } from '../lib/battlefieldDisplay';
import { isBotPlayer, legalActionHint, phaseGuidance, waitingStatusText } from '../lib/gameGuidance';
import { equipCostForCard, equipCostLabel, runeMatchesEquipDomain } from '../lib/equipment';
import { loadPriorityStopSettings, savePriorityStopSettings, shouldLocalAutoPassPriority, type PriorityStopSettings } from '../lib/priorityStops';
import { useLocalPlayer } from '../lib/playerContext';
import { getServerBuildInfo } from '../lib/serverBuildInfo';
import { getRoomSessionToken } from '../lib/roomSession';
import { play, preload } from '../lib/sfx';
import { createGameClient, joinGame, sendMove } from '../lib/stompGame';
import { useCardStore } from '../store/cards';
import { useGameStore } from '../store/game';
import { notifyError, notifySuccess, notifyWarning, useToastStore } from '../store/toasts';
import type { CardInstance, ChainItem, LegalAction, LiveGameState, PendingCardChoiceAssignment, RevealedHandSnapshot, RiftCard, ZoneName } from '../types';

const NAV_HEIGHT = 73;
const SIDEBAR_WIDTH = 280;
const PHASE_BAR_HEIGHT = 48;
const DEFAULT_HAND_HEIGHT = 172;
const CARD_PREVIEW_DELAY_MS = 2000;
const TOTAL_RUNE_SLOTS = 11;
const GITHUB_ISSUES_URL = 'https://github.com/xerioc2/riftforgetcg/issues/new/choose';
const ALPHA_LIMITATIONS = [
  'Multi-location Battlefield lanes are alpha support; Battlefield effects, hidden slots, and full official location rules are deferred.',
  'Reaction and chain/counterspell timing are not fully implemented.',
  'Hidden cards can be hidden, but play-from-hidden timing is incomplete.',
  'Ambush-as-Reaction and additional-cost Ambush cards are incomplete.',
  'Quick-Draw, Weaponmaster, XP, Hunt, Level, and Buff are deferred.',
  'Predict/top-deck ordering is partial and not rules-complete yet.',
  'Some cards are Partial or Unsupported; support badges and ready warnings are the source of truth.',
];

function pointZone(x: number, y: number, zones: ZoneRect[], fallback: ZoneName): ZoneName {
  return zones.find((zone) => x >= zone.x && x <= zone.x + zone.width && y >= zone.y && y <= zone.y + zone.height)?.zoneName ?? fallback;
}

function sameZone(zone: string, expected: ZoneName | string) {
  return zone.toLowerCase() === expected.toLowerCase();
}

function hasKeyword(card: RiftCard | undefined, keyword: string) {
  return card?.keywords?.some((value) => value.toUpperCase().startsWith(keyword.toUpperCase())) ?? false;
}

function hasHidden(card: RiftCard | undefined) {
  return hasKeyword(card, 'HIDDEN') || (card?.rulesText ?? '').toLowerCase().includes('[hidden]');
}

function sharesDomain(card: RiftCard | undefined, rune: RiftCard | undefined) {
  if (!card || !rune) return false;
  return rune.domains
    .filter((domain) => domain !== 'COLORLESS')
    .some((domain) => card.domains.some((cardDomain) => cardDomain.toUpperCase() === domain.toUpperCase()));
}

function hasMaskedOwnHand(state: LiveGameState, playerId: string) {
  return state.cards.some((card) => card.ownerId === playerId && sameZone(card.zone, 'hand') && card.cardId === 'hidden');
}

function canTakeAction(state: LiveGameState | null, action: LegalAction) {
  return state?.legalActions?.includes(action) ?? false;
}

function zoneKey(ownerId: string, zoneName: string) {
  return `${ownerId}:${zoneName.toLowerCase()}`;
}

function visualZoneFor(instance: CardInstance, zones: ZoneRect[]) {
  if (sameZone(instance.zone, 'battlefield')) {
    return battlefieldZoneForCard(zones, instance.ownerId, instance.battlefieldLocationId);
  }
  return zones.find((zone) => sameZone(zone.zoneName, instance.zone) && zone.ownerId === instance.ownerId);
}

function visualZoneKey(instance: CardInstance) {
  if (sameZone(instance.zone, 'battlefield')) {
    return `${instance.ownerId}:battlefield:${normalizeBattlefieldLocationId(instance.battlefieldLocationId)}`;
  }
  return zoneKey(instance.ownerId, instance.zone);
}

function shouldAutoPlacePublicCard(instance: CardInstance) {
  if (!sameZone(instance.zone, 'base') && !sameZone(instance.zone, 'battlefield')) return false;
  if (instance.x === 0 && instance.y === 0) return true;
  return instance.ownerId.toLowerCase().startsWith('bot-player-');
}

function PriorityStopToggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex items-center justify-between gap-2">
      <span>{label}</span>
      <input
        type="checkbox"
        className="h-3.5 w-3.5 accent-[#e0b15a]"
        checked={checked}
        onChange={(event) => onChange(event.currentTarget.checked)}
      />
    </label>
  );
}

export function GameBoard() {
  const { code } = useParams();
  const navigate = useNavigate();
  const player = useLocalPlayer();
  const { cards, loadCards } = useCardStore();
  const { state, chat, setState, addChat } = useGameStore();
  const lastError = useToastStore((store) => store.lastError);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [size, setSize] = useState({
    width: Math.max(320, window.innerWidth - SIDEBAR_WIDTH),
    height: Math.max(480, window.innerHeight - NAV_HEIGHT - PHASE_BAR_HEIGHT - DEFAULT_HAND_HEIGHT),
  });
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null);
  const [cardScale, setCardScale] = useState(1.3);
  const [hoveredCard, setHoveredCard] = useState<RiftCard | null>(null);
  const [hoveredInstance, setHoveredInstance] = useState<CardInstance | undefined>();
  const [inspectCard, setInspectCard] = useState<CardInstance | null>(null);
  const [pendingTargetSelection, setPendingTargetSelection] = useState<{ instanceId: string; mode: TargetMode } | null>(null);
  const [pendingChainTargetSelection, setPendingChainTargetSelection] = useState<{ instanceId: string; mode: 'DEFY' | 'NOT_SO_FAST' } | null>(null);
  const [pendingMultiTargetSelection, setPendingMultiTargetSelection] = useState<{ instanceId: string; requirements: TargetRequirement[]; selected: { role: TargetRole; instanceId: string }[] } | null>(null);
  const [pendingAccelerate, setPendingAccelerate] = useState<{ instanceId: string; card: RiftCard } | null>(null);
  const [pendingVision, setPendingVision] = useState<{ logId: string; cardId: string; cardName: string } | null>(null);
  const [choiceSubmitting, setChoiceSubmitting] = useState(false);
  const [selectedChoiceCardOptionId, setSelectedChoiceCardOptionId] = useState<string | null>(null);
  const [choiceAssignments, setChoiceAssignments] = useState<PendingCardChoiceAssignment[]>([]);
  const [pendingRuneTaps, setPendingRuneTaps] = useState<Set<string>>(new Set());
  const [mulliganDiscardIds, setMulliganDiscardIds] = useState<Set<string>>(new Set());
  const [showAlphaInfo, setShowAlphaInfo] = useState(false);
  const [handHeight, setHandHeight] = useState(230);
  const [wsConnected, setWsConnected] = useState(false);
  const [waitingTooLong, setWaitingTooLong] = useState(false);
  const [awaitingServerUpdate, setAwaitingServerUpdate] = useState(false);
  const [lastSubmittedActionType, setLastSubmittedActionType] = useState<string | null>(null);
  const [lastActionFailureMessage, setLastActionFailureMessage] = useState<string | null>(null);
  const [priorityStops, setPriorityStops] = useState<PriorityStopSettings>(() => loadPriorityStopSettings());
  const boardFrameRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const prevCards = useRef<Map<string, CardInstance>>(new Map());
  const prevState = useRef<LiveGameState | null>(null);
  const pendingAnimations = useRef<Set<string>>(new Set());
  const previewShowTimer = useRef<number | null>(null);
  const previewClearTimer = useRef<number | null>(null);
  const hasConnectedRef = useRef(false);
  const handledVisionLogRef = useRef<string | null>(null);
  const lastUserStateAt = useRef(0);
  const roomFallbackTimer = useRef<number | null>(null);
  const lastLocalAutoPassKey = useRef<string | null>(null);

  const roomCode = code?.toUpperCase() ?? '';
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
      positionsByOwner.set(playerId, runeSlotPositions(zone, TOTAL_RUNE_SLOTS));
    }
    return positionsByOwner;
  }, [playerIds, zones]);
  const battlefieldDisplays = useMemo(() => {
    if (!state) return [];
    return battlefieldLaneDisplays(state.players, player.id, cardsById, zones);
  }, [cardsById, player.id, state, zones]);
  const hand = useMemo(() => (state?.cards ?? []).filter((card) => card.ownerId === player.id && sameZone(card.zone, 'hand')), [player.id, state?.cards]);
  const handInstanceKey = useMemo(() => hand.map((card) => card.instanceId).sort().join(','), [hand]);
  const hasMulliganed = state?.mulligansDone?.includes(player.id) ?? false;
  const roomSessionToken = useMemo(() => (roomCode ? getRoomSessionToken(roomCode, player.id) : undefined), [player.id, roomCode]);
  const playerStateUrl = useMemo(() => {
    if (!roomCode) return '';
    const params = new URLSearchParams({ playerId: player.id });
    if (roomSessionToken) params.set('sessionToken', roomSessionToken);
    return `${getGameServerUrl()}/api/game/${roomCode}/state?${params.toString()}`;
  }, [player.id, roomCode, roomSessionToken]);
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
    savePriorityStopSettings(priorityStops);
  }, [priorityStops]);

  useEffect(() => {
    const element = boardFrameRef.current;
    if (!element) return;
    const observer = new ResizeObserver(([entry]) => {
      setSize({
        width: Math.max(720, entry.contentRect.width),
        height: Math.max(420, entry.contentRect.height),
      });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, [handHeight]);

  useEffect(() => {
    if (state?.currentPhase !== 'MAIN' || state?.activeShowdown) {
      setPendingRuneTaps(new Set());
      setPendingTargetSelection(null);
      setPendingMultiTargetSelection(null);
    }
  }, [state?.activeShowdown, state?.currentPhase]);

  useEffect(() => {
    if (state?.currentPhase === 'MULLIGAN' && !hasMulliganed) {
      setMulliganDiscardIds(new Set());
    }
  }, [handInstanceKey, hasMulliganed, state?.currentPhase]);

  useEffect(() => {
    setChoiceSubmitting(false);
    setSelectedChoiceCardOptionId(null);
    setChoiceAssignments([]);
  }, [state?.pendingChoice?.choiceId]);

  useEffect(() => {
    const activePlayer = state?.players.find((statePlayer) => statePlayer.userId === state.activePlayerId);
    const botIsActing = Boolean(state && state.activePlayerId !== player.id && isBotPlayer(state.activePlayerId, activePlayer?.name));
    setWaitingTooLong(false);
    if (!botIsActing) return;
    const timer = window.setTimeout(() => setWaitingTooLong(true), 4500);
    return () => window.clearTimeout(timer);
  }, [player.id, state?.activePlayerId, state?.activeShowdown?.step, state?.currentPhase, state?.players]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setPendingTargetSelection(null);
        setPendingMultiTargetSelection(null);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const handleIncomingState = useCallback(
    (incoming: LiveGameState) => {
      const prevShowdown = prevState.current?.activeShowdown;
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
      if (incoming.activeShowdown && !prevShowdown) void play('attack');
      const playerLogs = incoming.log.filter((entry) => entry.userId === player.id);
      const visionLog = [...playerLogs].reverse().find((entry) => entry.text.startsWith('VISION_PEEK|'));
      const visionResolution = [...playerLogs].reverse().find((entry) => entry.text.startsWith('VISION_RESOLVED|'));
      if (visionLog && visionLog.id !== handledVisionLogRef.current) {
        const visionIndex = incoming.log.findIndex((entry) => entry.id === visionLog.id);
        const resolutionIndex = visionResolution ? incoming.log.findIndex((entry) => entry.id === visionResolution.id) : -1;
        if (visionIndex > resolutionIndex) {
          const [, cardId, cardName] = visionLog.text.split('|');
          handledVisionLogRef.current = visionLog.id;
          setPendingVision({ logId: visionLog.id, cardId, cardName });
        }
      }
      prevCards.current = new Map(incoming.cards.map((card) => [card.instanceId, card]));
      prevState.current = incoming;
      setAwaitingServerUpdate(false);
      setState(incoming);
    },
    [player.id, setState],
  );

  useEffect(() => {
    if (!roomCode || !playerStateUrl) return;
    let cancelled = false;
    const setup = async () => {
      try {
        const stateResponse = await fetch(playerStateUrl);
        if (!stateResponse.ok) throw new Error(await stateResponse.text() || 'Game state not found.');
        const initialState = (await stateResponse.json()) as LiveGameState;
        if (cancelled) return;
        handleIncomingState(initialState);

        const client = createGameClient(
          roomCode,
          player,
          getRoomSessionToken(roomCode, player.id),
          (msg, source) => {
            if (msg.type === 'STATE_UPDATE') {
              if (source === 'user') {
                lastUserStateAt.current = Date.now();
                if (roomFallbackTimer.current != null) {
                  window.clearTimeout(roomFallbackTimer.current);
                  roomFallbackTimer.current = null;
                }
                handleIncomingState(msg.state);
              } else if (!hasMaskedOwnHand(msg.state, player.id)) {
                handleIncomingState(msg.state);
              } else if (roomFallbackTimer.current == null) {
                roomFallbackTimer.current = window.setTimeout(() => {
                  roomFallbackTimer.current = null;
                  if (Date.now() - lastUserStateAt.current < 1900) return;
                  void fetch(playerStateUrl)
                    .then((r) => (r.ok ? r.json() as Promise<LiveGameState> : Promise.reject(new Error('State refresh failed.'))))
                    .then(handleIncomingState)
                    .catch(() => notifyWarning('Player update missing', 'Unable to refresh your private state. Reconnecting may fix it.'));
                }, 2000);
              }
            }
            if (msg.type === 'ERROR') {
              addChat({ id: crypto.randomUUID(), userId: msg.playerId, email: null, text: msg.message, sentAt: new Date().toISOString() });
              setAwaitingServerUpdate(false);
              setLastActionFailureMessage(msg.message);
              notifyError('Action failed', msg.message);
              window.setTimeout(() => {
                void fetch(playerStateUrl)
                  .then((r) => (r.ok ? r.json() as Promise<LiveGameState> : Promise.reject(new Error('State refresh failed.'))))
                  .then(handleIncomingState)
                  .catch(() => notifyWarning('Unable to refresh state after failed action.'));
              }, 250);
            }
          },
          () => joinGame(client, roomCode),
          (connected) => {
            setWsConnected(connected);
            if (connected && hasConnectedRef.current) {
              void fetch(playerStateUrl)
                .then((r) => (r.ok ? r.json() as Promise<LiveGameState> : Promise.reject(new Error('State refresh failed.'))))
                .then(handleIncomingState)
                .catch(() => notifyWarning('Reconnected', 'Private state refresh failed; waiting for the next server update.'));
            }
            if (connected) {
              if (hasConnectedRef.current) notifySuccess('Reconnected');
              hasConnectedRef.current = true;
            } else if (hasConnectedRef.current) {
              notifyWarning('Connection lost', 'RiftForge is trying to reconnect.');
            }
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
          const message = setupError instanceof Error ? setupError.message : 'Unable to load game.';
          setError(message);
          notifyError('Unable to load game', message);
          setLoading(false);
        }
      }
    };
    void setup();
    return () => {
      cancelled = true;
      if (roomFallbackTimer.current != null) {
        window.clearTimeout(roomFallbackTimer.current);
        roomFallbackTimer.current = null;
      }
      void stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    };
  }, [addChat, handleIncomingState, player, playerStateUrl, roomCode]);

  const fetchProjectedState = useCallback(() => {
    if (!playerStateUrl) return;
    void fetch(playerStateUrl)
      .then((r) => (r.ok ? r.json() as Promise<LiveGameState> : Promise.reject(new Error('State refresh failed.'))))
      .then(handleIncomingState)
      .catch(() => notifyWarning('Unable to refresh state.'));
  }, [handleIncomingState, playerStateUrl]);

  useEffect(() => {
    if (!awaitingServerUpdate) return;
    const timer = window.setTimeout(() => {
      setAwaitingServerUpdate(false);
      fetchProjectedState();
    }, 2500);
    return () => window.clearTimeout(timer);
  }, [awaitingServerUpdate, fetchProjectedState]);

  useEffect(() => {
    if (!waitingTooLong) return;
    notifyWarning('Still waiting for server update', 'Refreshing the current game state once.');
    fetchProjectedState();
  }, [fetchProjectedState, waitingTooLong]);

  const publishMove = (move: Parameters<typeof sendMove>[2]) => {
    if (!stompClientRef.current || !wsConnected) {
      addChat({
        id: crypto.randomUUID(),
        userId: player.id,
        email: null,
        text: 'Connection is not ready. Reconnecting...',
        sentAt: new Date().toISOString(),
      });
      notifyWarning('Connection is not ready', 'RiftForge is reconnecting. Try again in a moment.');
      return;
    }
    setAwaitingServerUpdate(true);
    setLastSubmittedActionType(move.type);
    setLastActionFailureMessage(null);
    sendMove(stompClientRef.current, roomCode, move);
    window.setTimeout(fetchProjectedState, 300);
  };

  const assignCombatDamage = () => {
    if (!canTakeAction(state, 'ASSIGN_COMBAT_DAMAGE')) {
      notifyWarning('Action unavailable', 'The server has no combat damage assignment action available right now.');
      return;
    }
    if (!state?.activeShowdown) return;
    const assignmentState = state.combatAssignmentState;
    if (!assignmentState || assignmentState.assigningPlayerId !== player.id || !assignmentState.canAutoAssign) {
      notifyWarning(
        'Cannot auto-assign combat damage',
        'The server did not provide a valid automatic assignment for this board state.',
      );
      return;
    }
    const assignments = assignmentState.suggestedAssignments ?? [];
    publishMove({ type: 'ASSIGN_COMBAT_DAMAGE', playerId: player.id, assignments });
  };

  useEffect(() => {
    if (!state?.pendingChoice) setChoiceSubmitting(false);
  }, [state?.pendingChoice]);

  useEffect(() => {
    if (!state?.chainState) setPendingChainTargetSelection(null);
  }, [state?.chainState]);

  useEffect(() => {
    const chain = state?.chainState;
    const autoPassKey = chain
      ? `${chain.chainId}:${chain.focusedPlayerId}:${chain.consecutivePasses ?? 0}:${chain.readyToResolveTop ? 'ready' : 'open'}:${state?.updatedAt ?? ''}`
      : null;
    if (!shouldLocalAutoPassPriority({
      settings: priorityStops,
      legalActions: state?.legalActions,
      chainState: chain,
      playerId: player.id,
      activePlayerId: state?.activePlayerId,
    })) {
      lastLocalAutoPassKey.current = null;
      return;
    }
    if (!autoPassKey || lastLocalAutoPassKey.current === autoPassKey) return;
    lastLocalAutoPassKey.current = autoPassKey;
    publishMove({ type: 'PASS_CHAIN_FOCUS', playerId: player.id });
  }, [player.id, priorityStops, state?.activePlayerId, state?.chainState, state?.legalActions, state?.updatedAt]);

  const handleCardHover = (card: RiftCard | null, instance?: CardInstance) => {
    if (previewShowTimer.current != null) window.clearTimeout(previewShowTimer.current);
    if (previewClearTimer.current != null) window.clearTimeout(previewClearTimer.current);
    if (card) {
      previewShowTimer.current = window.setTimeout(() => {
        setHoveredCard(card);
        setHoveredInstance(instance);
      }, CARD_PREVIEW_DELAY_MS);
      return;
    }
    previewClearTimer.current = window.setTimeout(() => {
      setHoveredCard(null);
      setHoveredInstance(undefined);
    }, 1000);
  };

  const selectPaymentRunesForCard = (cardDef: RiftCard | undefined, additionalCost = 0) => {
    const playerEnergy = state?.players.find((statePlayer) => statePlayer.userId === player.id)?.availableEnergy ?? 0;
    const selectedRunes = new Set<string>();
    let plannedEnergy = 0;
    const neededEnergy = Math.max(0, (cardDef?.cost ?? 0) + additionalCost - playerEnergy);
    for (const runeId of pendingRuneTaps) {
      if (plannedEnergy >= neededEnergy) break;
      const rune = state?.runes?.find((candidate) => candidate.instanceId === runeId);
      if (!rune || rune.ownerId !== player.id || rune.tapped) continue;
      selectedRunes.add(rune.instanceId);
      plannedEnergy += rune.normalEnergy;
    }
    for (const rune of state?.runes ?? []) {
      if (plannedEnergy >= neededEnergy) break;
      if (rune.ownerId !== player.id || rune.tapped || selectedRunes.has(rune.instanceId)) continue;
      selectedRunes.add(rune.instanceId);
      plannedEnergy += rune.normalEnergy;
    }
    const premiumRuneIds: string[] = [];
    const premiumCost = cardDef?.premiumCost ?? 0;
    if (premiumCost > 0) {
      for (const rune of state?.runes ?? []) {
        if (premiumRuneIds.length >= premiumCost) break;
        if (rune.ownerId !== player.id || rune.tapped || selectedRunes.has(rune.instanceId)) continue;
        if (!sharesDomain(cardDef, cardsById.get(rune.cardId))) continue;
        premiumRuneIds.push(rune.instanceId);
      }
    }
    if (plannedEnergy < neededEnergy) {
      notifyWarning('Insufficient energy', `${cardDef?.name ?? 'That card'} needs ${neededEnergy} energy from ready runes.`);
      return null;
    }
    if (premiumRuneIds.length < premiumCost) {
      notifyWarning('Insufficient premium payment', `${cardDef?.name ?? 'That card'} needs ${premiumCost} matching domain rune${premiumCost === 1 ? '' : 's'}.`);
      return null;
    }
    setPendingRuneTaps(new Set());
    return { paymentRuneIds: [...selectedRunes], premiumRuneIds };
  };

  const equipPaymentForCard = (cardDef: RiftCard | undefined, showWarnings: boolean) => {
    const cost = equipCostForCard(cardDef);
    const playerEnergy = state?.players.find((statePlayer) => statePlayer.userId === player.id)?.availableEnergy ?? 0;
    const readyRunes = (state?.runes ?? []).filter((rune) => rune.ownerId === player.id && !rune.tapped);
    const premiumRuneIds: string[] = [];
    const reservedRuneIds = new Set<string>();
    for (const domain of cost.premiumDomains) {
      const rune = readyRunes.find((candidate) =>
        !reservedRuneIds.has(candidate.instanceId)
        && runeMatchesEquipDomain(domain, cardsById.get(candidate.cardId)));
      if (!rune) {
        if (showWarnings) notifyWarning('Insufficient equip payment', `${cardDef?.name ?? 'That Equipment'} needs a ${domain.toLowerCase()} rune to equip.`);
        return null;
      }
      premiumRuneIds.push(rune.instanceId);
      reservedRuneIds.add(rune.instanceId);
    }
    const selectedRunes = new Set<string>();
    let plannedEnergy = 0;
    const neededEnergy = Math.max(0, cost.energyCost - playerEnergy);
    for (const runeId of pendingRuneTaps) {
      if (plannedEnergy >= neededEnergy) break;
      const rune = state?.runes?.find((candidate) => candidate.instanceId === runeId);
      if (!rune || rune.ownerId !== player.id || rune.tapped || reservedRuneIds.has(rune.instanceId)) continue;
      selectedRunes.add(rune.instanceId);
      plannedEnergy += rune.normalEnergy;
    }
    for (const rune of readyRunes) {
      if (plannedEnergy >= neededEnergy) break;
      if (reservedRuneIds.has(rune.instanceId) || selectedRunes.has(rune.instanceId)) continue;
      selectedRunes.add(rune.instanceId);
      plannedEnergy += rune.normalEnergy;
    }
    if (plannedEnergy < neededEnergy) {
      if (showWarnings) notifyWarning('Insufficient energy', `${cardDef?.name ?? 'That Equipment'} needs ${neededEnergy} energy from ready runes to equip.`);
      return null;
    }
    return { paymentRuneIds: [...selectedRunes], premiumRuneIds };
  };

  const canPayEquipCost = (cardDef: RiftCard | undefined) => equipPaymentForCard(cardDef, false) != null;

  const selectPaymentRunesForEquip = (cardDef: RiftCard | undefined) => {
    const payment = equipPaymentForCard(cardDef, true);
    if (!payment) return null;
    setPendingRuneTaps(new Set());
    return payment;
  };

  const legalTargetsForMode = (mode: TargetMode) =>
    (state?.cards ?? []).filter((instance) => {
      const targetDef = cardsById.get(instance.cardId);
      return isLegalTargetForMode(instance, targetDef, mode, player.id);
    });

  const isLegalDefyChainTarget = (item: ChainItem) => {
    if (item.status && item.status !== 'PENDING') return false;
    if (!item.counterable || !item.targetableOnChain) return false;
    if (item.chainItemType && item.chainItemType !== 'SPELL') return false;
    if (item.sourceCardName === 'Hidden chain item') return false;
    const sourceCard = item.sourceCardId ? cardsById.get(item.sourceCardId) : undefined;
    if (!sourceCard || sourceCard.type?.toLowerCase() !== 'spell') return false;
    return (sourceCard.cost ?? 0) <= 4 && (sourceCard.premiumCost ?? 0) <= 1;
  };

  const legalDefyChainTargets = () => (state?.chainState?.chainItems ?? []).filter(isLegalDefyChainTarget);

  const isLegalNotSoFastChainTarget = (item: ChainItem) => {
    if (item.status && item.status !== 'PENDING') return false;
    if (!item.counterable || !item.targetableOnChain) return false;
    if (item.chainItemType && item.chainItemType !== 'SPELL') return false;
    if (item.sourceCardName === 'Hidden chain item') return false;
    if (item.controllerPlayerId === player.id) return false;
    const sourceCard = item.sourceCardId ? cardsById.get(item.sourceCardId) : undefined;
    if (!sourceCard || sourceCard.type?.toLowerCase() !== 'spell') return false;
    return (item.chainTargets ?? []).some((target) =>
      target.publicSafe
      && target.targetControllerPlayerId === player.id
      && ['UNIT', 'CHAMPION_UNIT', 'GEAR'].includes(String(target.targetKind ?? '').toUpperCase()));
  };

  const legalNotSoFastChainTargets = () => (state?.chainState?.chainItems ?? []).filter(isLegalNotSoFastChainTarget);

  const hasLegalGustChainTarget = () => legalTargetsForMode('GUST_BATTLEFIELD_UNIT').length > 0;
  const hasLegalDisciplineChainTarget = () => legalTargetsForMode('ANY_BATTLEFIELD_UNIT').length > 0;
  const hasLegalEnGardeChainTarget = () => legalTargetsForMode('FRIENDLY_UNIT').length > 0;

  const canPlayChainReactionCard = (card: RiftCard | undefined) => {
    return chainActive && canUseSupportedChainResponse(card, {
      canPlayCard: canTakeAction(state, 'PLAY_CARD'),
      hasLegalGustTarget: hasLegalGustChainTarget(),
      hasLegalDisciplineTarget: hasLegalDisciplineChainTarget(),
      hasLegalEnGardeTarget: hasLegalEnGardeChainTarget(),
      hasLegalDefyTarget: legalDefyChainTargets().length > 0,
      hasLegalNotSoFastTarget: legalNotSoFastChainTargets().length > 0,
    });
  };

  const beginChainTargetSelection = (instanceId: string, mode: 'DEFY' | 'NOT_SO_FAST') => {
    const hasTarget = mode === 'DEFY'
      ? legalDefyChainTargets().length > 0
      : legalNotSoFastChainTargets().length > 0;
    if (!hasTarget) {
      notifyWarning('No legal chain targets', mode === 'DEFY'
        ? 'Defy needs a pending public spell chain item it can counter.'
        : 'Not So Fast needs an enemy pending spell that chooses your friendly Unit or Gear.');
      return false;
    }
    setPendingTargetSelection(null);
    setPendingMultiTargetSelection(null);
    setPendingChainTargetSelection({ instanceId, mode });
    return true;
  };

  const beginTargetSelection = (instanceId: string, mode: TargetMode) => {
    if (legalTargetsForMode(mode).length === 0) {
      notifyWarning('No legal targets', noLegalTargetsMessage(mode));
      return false;
    }
    setPendingMultiTargetSelection(null);
    setPendingChainTargetSelection(null);
    setPendingTargetSelection({ instanceId, mode });
    return true;
  };

  const beginMultiTargetSelection = (instanceId: string, requirements: TargetRequirement[]) => {
    const missing = requirements.find((requirement) => legalTargetsForMode(requirement.mode).length === 0);
    if (missing) {
      notifyWarning('No legal targets', noLegalTargetsMessage(missing.mode));
      return false;
    }
    setPendingTargetSelection(null);
    setPendingChainTargetSelection(null);
    setPendingMultiTargetSelection({ instanceId, requirements, selected: [] });
    return true;
  };

  const playCounterReaction = (instanceId: string, targetChainItemId: string) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    const cardDef = instance ? cardsById.get(instance.cardId) : undefined;
    const payment = selectPaymentRunesForCard(cardDef);
    if (!payment) return;
    publishMove({
      type: 'PLAY_CARD',
      playerId: player.id,
      instanceId,
      targetZone: 'BASE',
      x: 0,
      y: 0,
      targetChainItemId,
      ...payment,
    });
    setPendingChainTargetSelection(null);
  };

  const playCardToBase = (
    instanceId: string,
    cardDef: RiftCard | undefined,
    accelerate = false,
    targetInstanceId?: string,
    targets?: { role: TargetRole; instanceId: string }[],
  ) => {
    const payment = selectPaymentRunesForCard(cardDef, accelerate ? 1 : 0);
    if (!payment) return;
    const isRune = cardDef?.type?.toLowerCase() === 'rune';
    if (isRune) {
      const runeZone = zones.find((zone) => zone.zoneName === 'rune' && zone.ownerId === player.id);
      const runeCount = state?.runes?.filter((rune) => rune.ownerId === player.id).length ?? 0;
      const runePosition = runeZone ? runeSlotPositions(runeZone, TOTAL_RUNE_SLOTS)[Math.min(runeCount, TOTAL_RUNE_SLOTS - 1)] : undefined;
      const runeX = runePosition?.x ?? size.width / 2;
      const runeY = runePosition?.y ?? size.height / 2;
      publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'RUNE', x: runeX, y: runeY, targetInstanceId, targets, accelerate, ...payment });
      return;
    }
    const base = zones.find((zone) => zone.zoneName === 'base' && zone.ownerId === player.id);
    const baseCount = state?.cards.filter((card) => card.ownerId === player.id && sameZone(card.zone, 'base')).length ?? 0;
    const position = base ? autoPlaceInZone(base, baseCount, baseCount + 1) : { x: size.width / 2, y: size.height / 2 };
    publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'BASE', ...position, targetInstanceId, targets, accelerate, ...payment });
  };

  const deployChampionFromZone = (instanceId: string, battlefieldLocationId = DEFAULT_BATTLEFIELD_LOCATION_ID) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    const cardDef = instance ? cardsById.get(instance.cardId) : undefined;
    if (!state || !instance || !cardDef) return;
    if (instance.ownerId !== player.id || !sameZone(instance.zone, 'champion') || cardDef.type?.toLowerCase() !== 'champion') {
      notifyWarning('Champion unavailable', 'Only your chosen Champion can be deployed from the Champion zone.');
      return;
    }
    if (state.activeShowdown || state.currentPhase !== 'MAIN' || state.activePlayerId !== player.id) {
      notifyWarning('Champion unavailable', 'You can deploy your Champion during Main Phase.');
      return;
    }
    if (!canTakeAction(state, 'MOVE_TO_BATTLEFIELD')) {
      notifyWarning('Champion unavailable', 'The server has no Champion deploy action available right now.');
      return;
    }
    const availableEnergy = state.players.find((statePlayer) => statePlayer.userId === player.id)?.availableEnergy ?? 0;
    const readyRuneEnergy = (state.runes ?? [])
      .filter((rune) => rune.ownerId === player.id && !rune.tapped)
      .reduce((total, rune) => total + rune.normalEnergy, 0);
    if (availableEnergy + readyRuneEnergy < (cardDef.cost ?? 0)) {
      notifyWarning('Not enough energy', `${cardDef.name} needs ${cardDef.cost ?? 0} energy to deploy.`);
      return;
    }
    const payment = selectPaymentRunesForCard(cardDef);
    if (!payment) return;
    publishMove({ type: 'MOVE_TO_BATTLEFIELD', playerId: player.id, instanceId, battlefieldLocationId, ...payment });
  };

  const playCardWithAmbush = (instanceId: string) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    const cardDef = instance ? cardsById.get(instance.cardId) : undefined;
    if (!instance || !cardDef) return;
    if (!canTakeAction(state, 'PLAY_CARD')) {
      notifyWarning('Ambush unavailable', 'Ambush can only be used during currently supported play windows.');
      return;
    }
    if (showdownActive || !isMyTurn) {
      notifyWarning('Ambush reaction timing unavailable', 'Ambush-as-Reaction timing is not implemented yet.');
      return;
    }
    if (!isAmbushCard(cardDef) || cardDef.type?.toLowerCase() !== 'unit') {
      notifyWarning('Ambush unavailable', 'Only units with Ambush can be played this way.');
      return;
    }
    if (hasUnsupportedAdditionalCost(cardDef)) {
      notifyWarning('Additional cost unsupported', 'That Ambush card has an additional cost RiftForge cannot pay yet.');
      return;
    }
    const hasFriendlyBattlefieldUnit = state?.cards.some((card) => {
      const def = cardsById.get(card.cardId);
      const type = def?.type?.toLowerCase();
      return card.ownerId === player.id && sameZone(card.zone, 'battlefield') && (type === 'unit' || type === 'champion');
    });
    if (!hasFriendlyBattlefieldUnit) {
      notifyWarning('Ambush unavailable', 'Ambush requires a friendly unit at that battlefield.');
      return;
    }
    const payment = selectPaymentRunesForCard(cardDef);
    if (!payment) return;
    const battlefield = battlefieldZoneForCard(zones, player.id, DEFAULT_BATTLEFIELD_LOCATION_ID);
    const battlefieldCount = state?.cards.filter((card) =>
      card.ownerId === player.id
      && sameZone(card.zone, 'battlefield')
      && normalizeBattlefieldLocationId(card.battlefieldLocationId) === DEFAULT_BATTLEFIELD_LOCATION_ID).length ?? 0;
    const position = battlefield ? autoPlaceInZone(battlefield, battlefieldCount, battlefieldCount + 1) : { x: size.width / 2, y: size.height / 2 };
    publishMove({ type: 'PLAY_CARD', playerId: player.id, instanceId, targetZone: 'BATTLEFIELD', ...position, ...payment });
  };

  const hideFromHand = (instanceId: string) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    const cardDef = instance ? cardsById.get(instance.cardId) : undefined;
    if (!instance || !cardDef) return;
    if (!canTakeAction(state, 'HIDE_CARD')) {
      notifyWarning('Hidden unavailable', 'Hidden can only be used during your Main Phase with a ready rune.');
      return;
    }
    if (!hasHidden(cardDef)) {
      notifyWarning('Hidden unavailable', 'Only cards with Hidden can be hidden.');
      return;
    }
    const paymentRune = (state?.runes ?? []).find((rune) => rune.ownerId === player.id && !rune.tapped);
    if (!paymentRune) {
      notifyWarning('Hidden unavailable', 'You need a ready rune to hide that card.');
      return;
    }
    publishMove({ type: 'HIDE_CARD', playerId: player.id, instanceId, paymentRuneId: paymentRune.instanceId });
  };

  const playFromHand = (instanceId: string) => {
    const cardDef = cardsById.get(state?.cards.find((c) => c.instanceId === instanceId)?.cardId ?? '');
    const type = cardDef?.type?.toLowerCase();
    if (chainActive) {
      if (!isReactionCard(cardDef)) {
        notifyWarning('Chain active', 'Only supported Reaction cards can be played while the chain is active.');
        return;
      }
      if (!canTakeAction(state, 'PLAY_CARD')) {
        notifyWarning('Reaction unavailable', 'Wait for your chain focus before playing a Reaction.');
        return;
      }
      if (isGustReactionCard(cardDef)) {
        beginTargetSelection(instanceId, 'GUST_BATTLEFIELD_UNIT');
        return;
      }
      if (isDisciplineReactionCard(cardDef)) {
        beginTargetSelection(instanceId, 'ANY_BATTLEFIELD_UNIT');
        return;
      }
      if (isEnGardeReactionCard(cardDef)) {
        beginTargetSelection(instanceId, 'FRIENDLY_UNIT');
        return;
      }
      if (isDefyCounterCard(cardDef)) {
        beginChainTargetSelection(instanceId, 'DEFY');
        return;
      }
      if (isNotSoFastCounterCard(cardDef)) {
        beginChainTargetSelection(instanceId, 'NOT_SO_FAST');
        return;
      }
    } else if (showdownActive) {
      if (isReactionCard(cardDef)) {
        notifyWarning('Reaction timing unavailable', 'Reaction timing is not implemented yet.');
        return;
      }
      if (!isActionCard(cardDef)) {
        notifyWarning('Action unavailable', 'Only supported Action cards can be played during this showdown window.');
        return;
      }
      if (!canTakeAction(state, 'PLAY_CARD')) {
        notifyWarning('Action unavailable', 'Only showdown participants can play Action cards here.');
        return;
      }
    }
    if (isReactionCard(cardDef)) {
      notifyWarning('Reaction timing unavailable', 'Supported Reactions can only be played during an active chain window in this alpha.');
      return;
    }
    if (!canTakeAction(state, 'PLAY_CARD')) {
      notifyWarning(
        'Action unavailable',
        isMyTurn ? 'You can only play cards during Main or supported action windows.' : 'Waiting for the active player or a supported action window.',
      );
      return;
    }
    if (type === 'legend' || type === 'battlefield') {
      const message = type === 'legend'
        ? 'Legends are identity cards and are not played from hand.'
        : 'Battlefields are selected during setup and are not played from hand.';
      notifyWarning('Cannot play that card', message);
      return;
    }
    const unsupportedReason = unsupportedCardReason(cardDef);
    if (unsupportedReason) {
      addChat({ id: crypto.randomUUID(), userId: player.id, email: null, text: unsupportedReason, sentAt: new Date().toISOString() });
      notifyWarning('Unsupported card effect', unsupportedReason);
      return;
    }
    const multiTargetRequirements = multiTargetRequirementsForCard(cardDef);
    if (multiTargetRequirements.length > 0) {
      beginMultiTargetSelection(instanceId, multiTargetRequirements);
      return;
    }
    const targetMode = targetModeForCard(cardDef);
    if (targetMode === 'UNSUPPORTED') {
      notifyWarning('Unsupported targeting', 'That card needs a targeting flow that is not supported yet.');
      return;
    }
    if (targetMode !== 'NONE') {
      beginTargetSelection(instanceId, targetMode);
      return;
    }
    const canAccelerate = cardDef?.type?.toLowerCase() === 'unit' && hasKeyword(cardDef, 'ACCELERATE') && spendableEnergy >= (cardDef.cost ?? 0) + 1;
    if (canAccelerate && cardDef) {
      setPendingAccelerate({ instanceId, card: cardDef });
      return;
    }
    playCardToBase(instanceId, cardDef);
  };

  const discardFromHand = (instanceId: string) => {
    if (!canTakeAction(state, 'SANDBOX_MOVE_CARD')) {
      notifyWarning('Sandbox only', 'Free-form hand discard is only available in Sandbox games.');
      return;
    }
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: 'DISCARD', x: size.width - 76, y: size.height - 170 });
  };

  const moveInstance = (instanceId: string, x: number, y: number) => {
    if (!state) return;
    const instance = state.cards.find((card) => card.instanceId === instanceId);
    if (!instance) return;
    if (instance.attachedToInstanceId) {
      notifyWarning('Attached gear', 'Attached Equipment follows its host and cannot be moved directly.');
      return;
    }
    const targetZoneRect = zones.find((candidate) => x >= candidate.x && x <= candidate.x + candidate.width && y >= candidate.y && y <= candidate.y + candidate.height);
    const zone = targetZoneRect?.zoneName ?? pointZone(x, y, zones, instance.zone);
    const targetBattlefieldLocationId = sameZone(zone, 'battlefield')
      ? normalizeBattlefieldLocationId(battlefieldLocationForPoint(x, y, zones))
      : undefined;
    if (sameZone(instance.zone, 'battlefield') && sameZone(zone, 'battlefield') && instance.ownerId === player.id) {
      const currentLocationId = normalizeBattlefieldLocationId(instance.battlefieldLocationId);
      const destinationLocationId = targetBattlefieldLocationId ?? DEFAULT_BATTLEFIELD_LOCATION_ID;
      if (destinationLocationId !== currentLocationId) {
        if (!canTakeAction(state, 'MOVE_TO_BATTLEFIELD')) {
          notifyWarning('Action unavailable', 'Battlefield lane movement is available only during your Main Phase before a showdown starts.');
          window.setTimeout(fetchProjectedState, 100);
          return;
        }
        const cardDef = cardsById.get(instance.cardId);
        const type = cardDef?.type?.toLowerCase();
        if (type !== 'unit' && type !== 'champion') {
          notifyWarning('Cannot move lanes', 'Only face-up Units and Champions can move between battlefield lanes.');
          window.setTimeout(fetchProjectedState, 100);
          return;
        }
        publishMove({
          type: 'MOVE_TO_BATTLEFIELD',
          playerId: player.id,
          instanceId,
          battlefieldLocationId: destinationLocationId,
        });
        return;
      }
    }
    if (sameZone(instance.zone, 'legend') && sameZone(zone, 'battlefield') && instance.ownerId === player.id) {
      notifyWarning('Legend unavailable', 'Legends cannot be moved to the battlefield in this alpha model.');
      window.setTimeout(fetchProjectedState, 100);
      return;
    }
    if ((sameZone(instance.zone, 'base') || sameZone(instance.zone, 'champion')) && sameZone(zone, 'battlefield') && instance.ownerId === player.id) {
      if (!canTakeAction(state, 'MOVE_TO_BATTLEFIELD')) {
        notifyWarning(
          'Action unavailable',
          sameZone(instance.zone, 'champion')
            ? 'You can only play your Champion during a legal play window.'
            : 'Units can move to a battlefield only when the server marks that action legal.',
        );
        window.setTimeout(fetchProjectedState, 100);
        return;
      }
      const cardDef = cardsById.get(instance.cardId);
      const type = cardDef?.type?.toLowerCase();
      if (type !== 'unit' && type !== 'champion') {
        notifyWarning('Cannot start showdown', 'Only Units and Champions can move to the battlefield.');
        window.setTimeout(fetchProjectedState, 100);
        return;
      }
      if (sameZone(instance.zone, 'champion')) {
        deployChampionFromZone(instanceId, targetBattlefieldLocationId ?? DEFAULT_BATTLEFIELD_LOCATION_ID);
        window.setTimeout(fetchProjectedState, 100);
        return;
      }
      publishMove({
        type: 'MOVE_TO_BATTLEFIELD',
        playerId: player.id,
        instanceId,
        battlefieldLocationId: targetBattlefieldLocationId ?? DEFAULT_BATTLEFIELD_LOCATION_ID,
      });
      return;
    }
    const snappedZoneRect = sameZone(instance.zone, 'battlefield') && sameZone(zone, 'battlefield')
      ? visualZoneFor(instance, zones)
      : targetZoneRect && targetZoneRect.ownerId === instance.ownerId
        ? targetZoneRect
        : zones.find((candidate) => candidate.zoneName === zone && candidate.ownerId === instance.ownerId) ?? zones.find((candidate) => candidate.zoneName === zone);
    const snappedX = snappedZoneRect ? Math.max(snappedZoneRect.x + 12, Math.min(snappedZoneRect.x + snappedZoneRect.width - 12, x)) : x;
    const snappedY = snappedZoneRect ? Math.max(snappedZoneRect.y + 12, Math.min(snappedZoneRect.y + snappedZoneRect.height - 12, y)) : y;
    if (sameZone(instance.zone, zone) && instance.ownerId === player.id && canTakeAction(state, 'REPOSITION_CARD')) {
      publishMove({ type: 'REPOSITION_CARD', playerId: player.id, instanceId, x: snappedX, y: snappedY });
      return;
    }
    if (!canTakeAction(state, 'SANDBOX_MOVE_CARD')) {
      notifyWarning('Sandbox only', 'Free-form card movement is only available in Sandbox games.');
      window.setTimeout(fetchProjectedState, 100);
      return;
    }
    publishMove({ type: 'MOVE_CARD', playerId: player.id, instanceId, targetZone: zone.toUpperCase().replace(/-/g, '_'), x: snappedX, y: snappedY });
  };

  const handleCardClick = (instanceId: string) => {
    const instance = state?.cards.find((card) => card.instanceId === instanceId);
    if (!instance) return;
    const targetCard = cardsById.get(instance.cardId);
    if (state?.pendingChoice?.type === 'TARGET_GEAR' && state.pendingChoice.playerId === player.id) {
      if (!isLegalTargetForMode(instance, targetCard, 'ANY_PUBLIC_GEAR', player.id)) {
        notifyWarning('Invalid target', 'Choose a Gear in play, or use Cancel to decline.');
        return;
      }
      if (!canTakeAction(state, 'RESOLVE_CHOICE')) {
        notifyWarning('Choice unavailable', 'The server has not approved resolving this choice yet.');
        return;
      }
      setChoiceSubmitting(true);
      publishMove({
        type: 'RESOLVE_CHOICE',
        playerId: player.id,
        choiceId: state.pendingChoice.choiceId,
        selectedTargetInstanceId: instanceId,
      });
      return;
    }
    if (pendingMultiTargetSelection) {
      const requirement = pendingMultiTargetSelection.requirements[pendingMultiTargetSelection.selected.length];
      if (!requirement) {
        setPendingMultiTargetSelection(null);
        return;
      }
      const alreadySelected = pendingMultiTargetSelection.selected.some((target) => target.instanceId === instanceId);
      const isValidMultiTarget = isLegalTargetForMode(instance, targetCard, requirement.mode, player.id);
      if (alreadySelected) {
        notifyWarning('Invalid target', 'Targets must be different cards.');
        return;
      }
      if (!isValidMultiTarget) {
        notifyWarning('Invalid target', `${requirement.prompt} Press Esc or right-click to cancel.`);
        return;
      }
      const nextSelected = [...pendingMultiTargetSelection.selected, { role: requirement.role, instanceId }];
      if (nextSelected.length < pendingMultiTargetSelection.requirements.length) {
        setPendingMultiTargetSelection({ ...pendingMultiTargetSelection, selected: nextSelected });
        return;
      }
      const spellInstance = state?.cards.find((card) => card.instanceId === pendingMultiTargetSelection.instanceId);
      const spellDef = spellInstance ? cardsById.get(spellInstance.cardId) : undefined;
      playCardToBase(pendingMultiTargetSelection.instanceId, spellDef, false, undefined, nextSelected);
      setPendingMultiTargetSelection(null);
      return;
    }
    const isValidPendingTarget = pendingTargetSelection
      ? isLegalTargetForMode(instance, targetCard, pendingTargetSelection.mode, player.id)
      : false;
    if (pendingTargetSelection && !isValidPendingTarget) {
      notifyWarning('Invalid target', `${targetPromptForMode(pendingTargetSelection.mode)} Press Esc or right-click to cancel.`);
      return;
    }
    if (pendingTargetSelection && isValidPendingTarget) {
      const spellInstance = state?.cards.find((card) => card.instanceId === pendingTargetSelection.instanceId);
      if (!spellInstance) {
        setPendingTargetSelection(null);
        return;
      }
      const spellDef = cardsById.get(spellInstance.cardId);
      if (isEquipCard(spellDef) && sameZone(spellInstance.zone, 'base')) {
        if (!canTakeAction(state, 'EQUIP_GEAR')) {
          notifyWarning('Equip unavailable', 'Equipment can be attached only when the server marks Equip legal.');
          return;
        }
        const payment = selectPaymentRunesForEquip(spellDef);
        if (!payment) return;
        publishMove({ type: 'EQUIP_GEAR', playerId: player.id, gearInstanceId: pendingTargetSelection.instanceId, targetInstanceId: instanceId, ...payment });
        setPendingTargetSelection(null);
        return;
      }
      playCardToBase(pendingTargetSelection.instanceId, spellDef, false, instanceId);
      setPendingTargetSelection(null);
      return;
    }

    if (instance.attachedToInstanceId) {
      setSelectedInstanceId(null);
      setInspectCard(instance);
      return;
    }

    if (instance.ownerId === player.id && sameZone(instance.zone, 'base') && isEquipCard(targetCard)) {
      if (instance.attachedToInstanceId) {
        notifyWarning('Already equipped', 'Attached Equipment cannot be re-equipped until its host leaves play.');
        return;
      }
      if (legalTargetsForMode('FRIENDLY_UNIT_FOR_EQUIP').length === 0) {
        notifyWarning('No legal targets', noLegalTargetsMessage('FRIENDLY_UNIT_FOR_EQUIP'));
        return;
      }
      if (!canPayEquipCost(targetCard)) {
        const cost = equipCostForCard(targetCard);
        notifyWarning('Equip unavailable', `${targetCard?.name ?? 'That Equipment'} needs ${equipCostLabel(cost)}.`);
        return;
      }
      if (!canTakeAction(state, 'EQUIP_GEAR')) {
        notifyWarning('Equip unavailable', 'Equipment can be attached during your Main Phase when a legal target and payment are available.');
        return;
      }
      beginTargetSelection(instanceId, 'FRIENDLY_UNIT_FOR_EQUIP');
      return;
    }

    if (instance.ownerId === player.id && sameZone(instance.zone, 'legend')) {
      notifyWarning('Legend unavailable', 'Legends cannot be moved to the battlefield in this alpha model.');
    }
    setSelectedInstanceId(instanceId);
  };

  const handleRuneTap = (runeInstanceId: string) => {
    const rune = state?.runes?.find((candidate) => candidate.instanceId === runeInstanceId);
    if (!rune || rune.ownerId !== player.id || rune.tapped) return;
    if (!canTakeAction(state, 'TAP_RUNE')) {
      notifyWarning('Action unavailable', 'Runes can be tapped only during supported payment or channel windows.');
      return;
    }
    setPendingRuneTaps((previous) => {
      const next = new Set(previous);
      if (next.has(runeInstanceId)) next.delete(runeInstanceId);
      else next.add(runeInstanceId);
      return next;
    });
  };

  const handlePassPhase = () => {
    if (canTakeAction(state, 'RESOLVE_CHAIN_TOP')) {
      publishMove({ type: 'RESOLVE_CHAIN_TOP', playerId: player.id });
      return;
    }
    if (canTakeAction(state, 'PASS_CHAIN_FOCUS')) {
      publishMove({ type: 'PASS_CHAIN_FOCUS', playerId: player.id });
      return;
    }
    if (canTakeAction(state, 'ASSIGN_COMBAT_DAMAGE')) {
      assignCombatDamage();
      return;
    }
    if (canTakeAction(state, 'RESOLVE_SHOWDOWN')) {
      publishMove({ type: 'RESOLVE_SHOWDOWN', playerId: player.id });
      return;
    }
    if (canTakeAction(state, 'PASS_SHOWDOWN_FOCUS')) {
      publishMove({ type: 'PASS_SHOWDOWN_FOCUS', playerId: player.id });
      return;
    }
    if (!canTakeAction(state, 'PASS_PHASE')) {
      notifyWarning('Action unavailable', isMyTurn ? 'The server has no pass action available right now.' : `Waiting for ${opponentName}.`);
      return;
    }
    publishMove({ type: 'PASS_PHASE', playerId: player.id });
  };

  const submitMulligan = (discardInstanceIds: string[]) => {
    if (discardInstanceIds.length === 0 && !canTakeAction(state, 'KEEP_HAND')) {
      notifyWarning('Action unavailable', 'Keeping your hand is not available yet.');
      return;
    }
    if (discardInstanceIds.length > 0 && !canTakeAction(state, 'MULLIGAN')) {
      notifyWarning('Action unavailable', 'Mulligan is not available yet.');
      return;
    }
    publishMove({ type: 'MULLIGAN', playerId: player.id, discardInstanceIds });
  };

  const selectBattlefield = (battlefieldCardId: string) => {
    if (!canTakeAction(state, 'SELECT_BATTLEFIELD')) {
      notifyWarning('Action unavailable', 'Choose your Battlefield before mulligan begins.');
      return;
    }
    publishMove({ type: 'SELECT_BATTLEFIELD', playerId: player.id, battlefieldCardId });
  };

  const sendChat = (text: string) => {
    addChat({ id: crypto.randomUUID(), userId: player.id, email: player.name, text, sentAt: new Date().toISOString() });
  };

  const handleReset = async () => {
    const params = new URLSearchParams({ playerId: player.id });
    if (roomSessionToken) params.set('sessionToken', roomSessionToken);
    const response = await fetch(`${getGameServerUrl()}/api/game/${roomCode}/reset?${params.toString()}`, { method: 'POST' });
    if (!response.ok) notifyError('Reset failed', await readableHttpError(response, 'Unable to reset game.'));
  };

  const buildCurrentDebugInfo = () =>
    buildDebugInfo({
      roomCode,
      state,
      player,
      lastError,
      serverUrl: getGameServerUrl(),
      appVersion: APP_VERSION,
      buildTag: BUILD_TAG,
      buildDate: BUILD_DATE,
      ...getServerBuildInfo(),
      awaitingServerUpdate,
      lastSubmittedActionType,
      lastActionFailureMessage,
    });

  const copyDebugInfo = async () => {
    const payload = buildCurrentDebugInfo();
    await navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
    notifySuccess('Debug info copied', 'It is safe to paste publicly. Add screenshots or reproduction steps if you have them.');
  };

  const reportIssue = async () => {
    const payload = buildCurrentDebugInfo();
    await navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
    notifySuccess('Debug info copied', 'It is safe to paste publicly. Paste it into the GitHub issue form with screenshots and steps.');
    window.open(GITHUB_ISSUES_URL, '_blank', 'noopener,noreferrer');
  };

  if (loading) return <CenteredState>Loading game...</CenteredState>;

  if (error || !state) {
    return (
      <CenteredState>
        <p>{error || 'Game unavailable.'}</p>
        <button className="btn-secondary mt-4" onClick={() => navigate('/')}>
          Home
        </button>
      </CenteredState>
    );
  }

  const opponent = state.players.find((statePlayer) => statePlayer.userId !== player.id);
  const me = state.players.find((statePlayer) => statePlayer.userId === player.id);
  const activePlayer = state.players.find((statePlayer) => statePlayer.userId === state.activePlayerId);
  const opponentName = opponent?.name?.trim() || opponent?.userId || 'Opponent';
  const activePlayerName = activePlayer?.userId === player.id ? 'you' : activePlayer?.name?.trim() || activePlayer?.userId || opponentName;
  const activePlayerIsBot = isBotPlayer(activePlayer?.userId, activePlayer?.name);
  const showdownFocusPlayer = state.activeShowdown?.focusedPlayerId
    ? state.players.find((statePlayer) => statePlayer.userId === state.activeShowdown?.focusedPlayerId)
    : undefined;
  const showdownFocusName = state.activeShowdown?.focusedPlayerId === player.id
    ? 'you'
    : showdownFocusPlayer?.name?.trim() || showdownFocusPlayer?.userId;
  const chainFocusPlayer = state.chainState?.focusedPlayerId
    ? state.players.find((statePlayer) => statePlayer.userId === state.chainState?.focusedPlayerId)
    : undefined;
  const chainFocusName = state.chainState?.focusedPlayerId === player.id
    ? 'you'
    : chainFocusPlayer?.name?.trim() || chainFocusPlayer?.userId;
  const opponentHand = state.cards.filter((card) => card.ownerId === opponent?.userId && sameZone(card.zone, 'hand')).length;
  const visibleCardsFor = (ownerId: string | undefined, zone: string) =>
    state.cards
      .filter((instance) => instance.ownerId === ownerId && sameZone(instance.zone, zone))
      .map((instance) => ({ instanceId: instance.instanceId, card: cardsById.get(instance.cardId) }))
      .filter((entry): entry is { instanceId: string; card: RiftCard } => Boolean(entry.card));
  const revealedCardsFor = (snapshot: RevealedHandSnapshot | undefined) =>
    (snapshot?.instanceIds ?? [])
      .map((instanceId) => {
        const instance = state.cards.find((candidate) => candidate.instanceId === instanceId);
        const card = instance ? cardsById.get(instance.cardId) : undefined;
        return card ? { instanceId, card } : null;
      })
      .filter((entry): entry is { instanceId: string; card: RiftCard } => Boolean(entry));
  const opponentDeckCount = opponent?.deckCount ?? 0;
  const myDeckCount = me?.deckCount ?? 0;
  const opponentDiscardCards = visibleCardsFor(opponent?.userId, 'discard');
  const myDiscardCards = visibleCardsFor(player.id, 'discard');
  const myHiddenCards = visibleCardsFor(player.id, 'hidden');
  const opponentHiddenCount = state.cards.filter((card) => card.ownerId === opponent?.userId && sameZone(card.zone, 'hidden')).length;
  const opponentRevealedSnapshot = state.revealedHands?.find((snapshot) => snapshot.revealedToPlayerId === player.id && snapshot.revealedOwnerId === opponent?.userId);
  const myRevealedSnapshot = state.revealedHands?.find((snapshot) => snapshot.revealedToPlayerId === player.id && snapshot.revealedOwnerId === player.id);
  const dismissRevealed = (instanceId: string) => publishMove({ type: 'DISMISS_REVEALED', playerId: player.id, instanceId });
  const myUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === player.id && !rune.tapped).length;
  const pendingRuneEnergy = (state.runes ?? [])
    .filter((rune) => pendingRuneTaps.has(rune.instanceId))
    .reduce((total, rune) => total + rune.normalEnergy, 0);
  const spendableEnergy = (me?.availableEnergy ?? 0)
    + (state.runes ?? [])
        .filter((rune) => rune.ownerId === player.id && !rune.tapped)
        .reduce((total, rune) => total + rune.normalEnergy, 0);
  const opponentUntappedRunes = (state.runes ?? []).filter((rune) => rune.ownerId === opponent?.userId && !rune.tapped).length;
  const isMyTurn = state.activePlayerId === player.id;
  const showdownActive = Boolean(state.activeShowdown);
  const chainActive = Boolean(state.chainState);
  const canPlayCards = canTakeAction(state, 'PLAY_CARD');
  const canResolveChoice = canTakeAction(state, 'RESOLVE_CHOICE');
  const canMoveToBattlefield = canTakeAction(state, 'MOVE_TO_BATTLEFIELD');
  const canResolveChainTop = canTakeAction(state, 'RESOLVE_CHAIN_TOP');
  const canPassChainFocus = canTakeAction(state, 'PASS_CHAIN_FOCUS');
  const canAssignCombatDamage = canTakeAction(state, 'ASSIGN_COMBAT_DAMAGE');
  const canResolveShowdown = canTakeAction(state, 'RESOLVE_SHOWDOWN');
  const canPassShowdownFocus = canTakeAction(state, 'PASS_SHOWDOWN_FOCUS');
  const canPlayReactions = chainActive && canPlayCards;
  const hasSpellReaction = hand.some((instance) => cardsById.get(instance.cardId)?.type?.toLowerCase() === 'spell');
  const ownRuneZone = zones.find((zone) => zone.zoneName === 'rune' && zone.ownerId === player.id);
  const hasTappedOwnRune = (state.runes ?? []).some((rune) => rune.ownerId === player.id && rune.tapped);
  const canUndoRunes = canTakeAction(state, 'UNDO_RUNES') && hasTappedOwnRune;
  const canPass = canTakeAction(state, 'PASS_PHASE') || canResolveChainTop || canPassChainFocus || canAssignCombatDamage || canResolveShowdown || canPassShowdownFocus;
  const passLabel = canResolveChainTop ? 'Resolve Chain' : canPassChainFocus ? 'Pass Chain' : canAssignCombatDamage ? 'Resolve Damage' : canResolveShowdown ? 'Resolve Showdown' : canPassShowdownFocus ? 'Pass Focus' : 'Pass';
  const canSelectBattlefield = canTakeAction(state, 'SELECT_BATTLEFIELD');
  const canMulligan = canTakeAction(state, 'MULLIGAN');
  const canKeepHand = canTakeAction(state, 'KEEP_HAND');
  const canHideCards = canTakeAction(state, 'HIDE_CARD');
  const hasFriendlyBattlefieldUnit = state.cards.some((card) => {
    const def = cardsById.get(card.cardId);
    const type = def?.type?.toLowerCase();
    return card.ownerId === player.id && sameZone(card.zone, 'battlefield') && (type === 'unit' || type === 'champion');
  });
  const canAmbushCards = canPlayCards && isMyTurn && !showdownActive && hasFriendlyBattlefieldUnit;
  const selectedInstance = selectedInstanceId ? state.cards.find((card) => card.instanceId === selectedInstanceId) : undefined;
  const selectedCardDef = selectedInstance ? cardsById.get(selectedInstance.cardId) : undefined;
  const selectedChampionDeployable = Boolean(
    selectedInstance
      && selectedInstance.ownerId === player.id
      && sameZone(selectedInstance.zone, 'champion')
      && selectedCardDef?.type?.toLowerCase() === 'champion'
      && canMoveToBattlefield
      && isMyTurn
      && !showdownActive
      && spendableEnergy >= (selectedCardDef.cost ?? 0),
  );
  const battlefieldChoices = me?.battlefieldChoices ?? [];
  const selectedBattlefield = me?.selectedBattlefieldId ?? null;
  const chainItems = state.chainState?.chainItems ?? [];
  const topChainItem = chainItems.length > 0 ? chainItems[chainItems.length - 1] : undefined;
  const chainPlayerNames = Object.fromEntries(state.players.map((statePlayer) => [
    statePlayer.userId,
    statePlayer.userId === player.id ? 'you' : statePlayer.name || statePlayer.userId,
  ]));
  const playerNames = Object.fromEntries(state.players.map((statePlayer) => [
    statePlayer.userId,
    statePlayer.userId === player.id ? 'you' : statePlayer.name || statePlayer.userId,
  ]));
  const activeShowdownLocationLabel = state.activeShowdown
    ? battlefieldLocationLabel(state.activeShowdown.locationId)
    : undefined;
  const chainTargetNames = Object.fromEntries(state.cards
    .filter((instance) => !instance.faceDown && !['hand', 'deck', 'hidden'].includes(instance.zone.toLowerCase()))
    .map((instance) => [instance.instanceId, cardsById.get(instance.cardId)?.name ?? 'card']));
  const guidanceText = phaseGuidance(
    state.currentPhase,
    showdownActive,
    state.activeShowdown?.step,
    chainActive,
    state.chainState?.readyToResolveTop,
    topChainItem,
  );
  const actionHintText = legalActionHint(state.legalActions, { isPlayer: Boolean(me), isMyTurn, activePlayerName, activePlayerIsBot });
  const waitingText = waitingStatusText({ isMyTurn, activePlayerName, activePlayerIsBot, waitingLong: waitingTooLong });
  const connectionText = wsConnected ? 'Connected.' : 'Reconnecting to player-specific updates...';
  const priorityWindowIsMine = state.chainState?.focusedPlayerId === player.id && canPassChainFocus;
  const localEmptyPriorityWindow = priorityWindowIsMine && !canPlayCards;
  const updatePriorityStop = (key: keyof PriorityStopSettings, value: boolean) => {
    setPriorityStops((previous) => ({ ...previous, [key]: value }));
  };
  const boardCardRenderItems = [...state.cards]
    .filter((instance) => !sameZone(instance.zone, 'hand') && !sameZone(instance.zone, 'deck') && !sameZone(instance.zone, 'hidden'))
    .sort((a, b) => a.zIndex - b.zIndex)
    .map((instance) => ({ instance, zone: visualZoneFor(instance, zones) }));
  const zoneTotals = new Map<string, number>();
  boardCardRenderItems.forEach(({ instance }) => {
    if (!shouldAutoPlacePublicCard(instance)) return;
    const key = visualZoneKey(instance);
    zoneTotals.set(key, (zoneTotals.get(key) ?? 0) + 1);
  });
  const zoneCounts = new Map<string, number>();
  const boardCardDisplayItems = boardCardRenderItems.map(({ instance, zone }) => {
    const key = visualZoneKey(instance);

    if (instance.attachedToInstanceId) {
      const host = state.cards.find((candidate) => candidate.instanceId === instance.attachedToInstanceId);
      const hostZone = host ? visualZoneFor(host, zones) : undefined;
      if (host && hostZone) {
        let hostPosition = { x: host.x, y: host.y };
        if ((sameZone(host.zone, 'champion') || sameZone(host.zone, 'legend'))) {
          hostPosition = { x: hostZone.x + hostZone.width / 2, y: hostZone.y + Math.min(60, hostZone.height / 2) };
        } else if (shouldAutoPlacePublicCard(host)) {
          hostPosition = autoPlaceInZone(hostZone, 0, 1);
        } else if (sameZone(host.zone, 'base') || sameZone(host.zone, 'battlefield')) {
          hostPosition = clampPlacementToZone(host, hostZone);
        }
        const position = attachedGearDisplayPosition(hostPosition);
        return { instance, displayInstance: { ...instance, ...position }, displayScale: attachedGearDisplayScale(cardScale) };
      }
    }

    if ((sameZone(instance.zone, 'champion') || sameZone(instance.zone, 'legend')) && zone) {
      return { instance, displayInstance: { ...instance, x: zone.x + zone.width / 2, y: zone.y + Math.min(60, zone.height / 2) } };
    }

    if (zone && shouldAutoPlacePublicCard(instance)) {
      const zoneIndex = zoneCounts.get(key) ?? 0;
      zoneCounts.set(key, zoneIndex + 1);
      return { instance, displayInstance: { ...instance, ...autoPlaceInZone(zone, zoneIndex, zoneTotals.get(key) ?? zoneIndex + 1) } };
    }

    if (zone && (sameZone(instance.zone, 'base') || sameZone(instance.zone, 'battlefield'))) {
      return { instance, displayInstance: { ...instance, ...clampPlacementToZone(instance, zone) } };
    }

    return { instance, displayInstance: instance };
  });
  const canDragBoardCard = (instance: CardInstance, cardDef: RiftCard) => {
    if (instance.ownerId !== player.id || !attachedGearIsIndependentBoardPiece(instance)) return false;
    if (sameZone(instance.zone, 'legend')) return false;
    if (sameZone(instance.zone, 'champion')) {
      return cardDef.type?.toLowerCase() === 'champion'
        && canMoveToBattlefield
        && isMyTurn
        && !showdownActive
        && !instance.tapped
        && spendableEnergy >= (cardDef.cost ?? 0);
    }
    if (sameZone(instance.zone, 'base') || sameZone(instance.zone, 'battlefield')) {
      return canMoveToBattlefield || canTakeAction(state, 'REPOSITION_CARD') || canTakeAction(state, 'SANDBOX_MOVE_CARD');
    }
    return canTakeAction(state, 'SANDBOX_MOVE_CARD');
  };
  const currentMultiTargetRequirement = pendingMultiTargetSelection?.requirements[pendingMultiTargetSelection.selected.length];
  const pendingChoice = state.pendingChoice;
  const isGearTargetChoice = pendingChoice?.type === 'TARGET_GEAR' && pendingChoice.playerId === player.id;
  const activeTargetMode = pendingTargetSelection?.mode ?? currentMultiTargetRequirement?.mode ?? (isGearTargetChoice ? 'ANY_PUBLIC_GEAR' : undefined);
  const selectedMultiTargetIds = new Set(pendingMultiTargetSelection?.selected.map((target) => target.instanceId) ?? []);
  const pendingCardOptions = pendingChoice?.cardOptions ?? [];
  const isTopDeckChoice = pendingChoice?.type === 'TOP_DECK_PICK_ONE';
  const isPredictChoice = pendingChoice?.type === 'PREDICT_ORDER';
  const predictChoiceReady = isPredictChoice && pendingCardOptions.length > 0 && choiceAssignments.length === pendingCardOptions.length;
  const cancelPendingGearTargetChoice = () => {
    if (!pendingChoice || !isGearTargetChoice || choiceSubmitting) return;
    setChoiceSubmitting(true);
    publishMove({
      type: 'RESOLVE_CHOICE',
      playerId: player.id,
      choiceId: pendingChoice.choiceId,
      selectedOptionId: 'DECLINE',
    });
  };
  const assignChoiceCard = (optionId: string, action: 'TOP' | 'BOTTOM') => {
    setChoiceAssignments((current) => {
      const withoutOption = current.filter((assignment) => assignment.optionId !== optionId);
      return [...withoutOption, { optionId, action, order: withoutOption.length }];
    });
  };

  return (
    <main className="relative overflow-hidden bg-ink text-slate-100" style={{ height: `calc(100vh - ${NAV_HEIGHT}px)` }}>
      {inspectCard ? (
        <CardInspectModal
          card={inspectCard}
          cardDef={cardsById.get(inspectCard.cardId)}
          allInstances={state.cards}
          cardsById={cardsById}
          onClose={() => setInspectCard(null)}
        />
      ) : null}
      <div ref={boardFrameRef} className="absolute left-0 top-0 overflow-hidden" style={{ right: `${SIDEBAR_WIDTH}px`, bottom: handHeight + PHASE_BAR_HEIGHT }}>
        <Stage
          width={size.width}
          height={size.height}
          onMouseDown={(event) => event.target === event.target.getStage() && setSelectedInstanceId(null)}
          onContextMenu={(event) => {
            event.evt.preventDefault();
            if (pendingTargetSelection) setPendingTargetSelection(null);
            if (pendingMultiTargetSelection) setPendingMultiTargetSelection(null);
            if (isGearTargetChoice) cancelPendingGearTargetChoice();
          }}
        >
          <Layer listening={false}>
            <ZoneOverlay
              zones={zones}
              activeShowdownLocationId={state.activeShowdown?.locationId}
              battlefieldController={state.battlefieldController}
              playerNames={playerNames}
            />
          </Layer>
          <Layer>
            {battlefieldDisplays.map((display) => (
              <BattlefieldLocationDisplay
                key={`selected-battlefield-${display.locationId}`}
                frame={display.frame}
                card={display.card}
                label={display.label}
                imageUrl={display.imageUrl}
                onHover={handleCardHover}
              />
            ))}
          </Layer>
          <Layer>
            {state.players.flatMap((statePlayer) => {
              const ownerRunes = (state.runes ?? []).filter((rune) => rune.ownerId === statePlayer.userId);
              const remaining = statePlayer.runePoolRemaining ?? 10;
              const discardedSlots = Math.max(0, TOTAL_RUNE_SLOTS - ownerRunes.length - remaining);
              const positions = runePositions.get(statePlayer.userId) ?? [];
              return Array.from({ length: discardedSlots }, (_, index) => {
                const position = positions[ownerRunes.length + index];
                return position ? <RegularPolygon key={`discarded-rune-${statePlayer.userId}-${index}`} x={position.x} y={position.y} sides={6} radius={14} rotation={30} stroke="#4b5563" strokeWidth={1.5} opacity={0.25} listening={false} /> : null;
              });
            })}
            {(state.runes ?? []).map((rune) => {
              const ownerRunes = (state.runes ?? []).filter((candidate) => candidate.ownerId === rune.ownerId);
              const position = runePositions.get(rune.ownerId)?.[ownerRunes.indexOf(rune)];
              const runeCard = cardsById.get(rune.cardId);
              if (!position) return null;
              return (
                <RuneSprite
                  key={rune.instanceId}
                  instanceId={rune.instanceId}
                  cardId={rune.cardId}
                  cardDef={runeCard}
                  tapped={rune.tapped}
                  normalEnergy={rune.normalEnergy}
                  premiumEnergy={rune.premiumEnergy}
                  isOwner={rune.ownerId === player.id}
                  pending={pendingRuneTaps.has(rune.instanceId)}
                  x={position.x}
                  y={position.y}
                  onTap={handleRuneTap}
                  onDiscard={(id) => {
                    if (canTakeAction(state, 'DISCARD_RUNE')) publishMove({ type: 'DISCARD_RUNE', playerId: player.id, runeInstanceId: id });
                    else notifyWarning('Action unavailable', 'Discarding a rune is only legal during supported payment windows.');
                  }}
                  onHover={handleCardHover}
                />
              );
            })}
          </Layer>
          <Layer>
            {boardCardDisplayItems.map(({ instance, displayInstance, displayScale }) => {
              const cardDef = cardsById.get(instance.cardId);
              if (!cardDef) return null;
              const attachedGearNames = attachedGearNamesForHost(state.cards, cardsById, instance.instanceId);
              return (
                <CardSprite
                  key={instance.instanceId}
                  instance={displayInstance}
                  cardDef={cardDef}
                  isOwner={canDragBoardCard(instance, cardDef)}
                  selected={selectedInstanceId === instance.instanceId && !instance.attachedToInstanceId}
                  onDragEnd={moveInstance}
                  onClick={handleCardClick}
                  onDoubleClick={(id) => {
                    const source = state.cards.find((card) => card.instanceId === id);
                    const sourceDef = source ? cardsById.get(source.cardId) : undefined;
                    if (source?.ownerId === player.id && sameZone(source.zone, 'champion') && sourceDef?.type?.toLowerCase() === 'champion') {
                      deployChampionFromZone(id);
                    } else if (source?.ownerId === player.id && sameZone(source?.zone ?? '', 'legend')) {
                      notifyWarning('Legend unavailable', 'Legends cannot be moved to the battlefield in this alpha model.');
                    } else if (canTakeAction(state, 'SANDBOX_TAP_CARD')) publishMove({ type: 'TAP_CARD', playerId: player.id, instanceId: id });
                    else notifyWarning('Sandbox only', 'Manual tap is only available in Sandbox games.');
                  }}
                  onContextMenu={(id) => {
                    if (pendingTargetSelection) {
                      setPendingTargetSelection(null);
                      return;
                    }
                    if (pendingMultiTargetSelection) {
                      setPendingMultiTargetSelection(null);
                      return;
                    }
                    if (isGearTargetChoice) {
                      cancelPendingGearTargetChoice();
                      return;
                    }
                    if (canTakeAction(state, 'SANDBOX_FLIP_CARD')) publishMove({ type: 'FLIP_CARD', playerId: player.id, instanceId: id });
                    else notifyWarning('Sandbox only', 'Manual flip is only available in Sandbox games.');
                  }}
                  animate={pendingAnimations.current.delete(instance.instanceId)}
                  scale={displayScale ?? cardScale}
                  attachedGearNames={attachedGearNames}
                  allInstances={state.cards}
                  cardsById={cardsById}
                  onHover={handleCardHover}
                />
              );
            })}
          </Layer>
          <Layer listening={false}>
            {activeTargetMode
              ? boardCardDisplayItems
                  .filter(({ instance }) => {
                    const cardDef = cardsById.get(instance.cardId);
                    return isLegalTargetForMode(instance, cardDef, activeTargetMode, player.id)
                      && !selectedMultiTargetIds.has(instance.instanceId);
                  })
                  .map(({ instance, displayInstance }) => <Rect key={`target-top-${instance.instanceId}`} x={displayInstance.x - 44} y={displayInstance.y - 60} width={88} height={120} fill="rgba(229,108,79,0.12)" stroke="#e56c4f" shadowColor="#e56c4f" shadowBlur={20} cornerRadius={6} />)
              : null}
            {pendingMultiTargetSelection
              ? boardCardDisplayItems
                  .filter(({ instance }) => selectedMultiTargetIds.has(instance.instanceId))
                  .map(({ instance, displayInstance }) => <Rect key={`selected-target-${instance.instanceId}`} x={displayInstance.x - 48} y={displayInstance.y - 64} width={96} height={128} fill="rgba(234,179,8,0.10)" stroke="#eab308" shadowColor="#eab308" shadowBlur={16} cornerRadius={6} />)
              : null}
            {pendingTargetSelection ? <Text x={0} y={8} width={size.width} text={`${targetPromptForMode(pendingTargetSelection.mode)} Esc/right-click to cancel.`} align="center" fontSize={13} fontStyle="bold" fill="#e56c4f" /> : null}
            {pendingMultiTargetSelection && currentMultiTargetRequirement ? <Text x={0} y={8} width={size.width} text={`${currentMultiTargetRequirement.prompt} (${pendingMultiTargetSelection.selected.length + 1}/${pendingMultiTargetSelection.requirements.length}) Esc/right-click to cancel.`} align="center" fontSize={13} fontStyle="bold" fill="#e56c4f" /> : null}
            {isGearTargetChoice ? <Text x={0} y={8} width={size.width} text="Choose a Gear in play. Right-click to cancel." align="center" fontSize={13} fontStyle="bold" fill="#e56c4f" /> : null}
          </Layer>
        </Stage>
      </div>
      {ownRuneZone ? (
        <div className="pointer-events-none absolute flex items-center gap-2" style={{ left: ownRuneZone.x + 8, top: ownRuneZone.y + 4, maxWidth: ownRuneZone.width - 16 }}>
          <span className="truncate text-xs text-slate-500">Left-click: tap (1 energy) - Right-click: discard (2 energy, permanent)</span>
          {canUndoRunes ? (
            <button className="btn-secondary pointer-events-auto min-h-6 shrink-0 px-2 py-0.5 text-xs" onClick={() => publishMove({ type: 'UNDO_RUNES', playerId: player.id })}>
              Undo
            </button>
          ) : null}
        </div>
      ) : null}

      {opponent ? (
        <PlayerPanel
          name={opponentName}
          score={opponent.score}
          handCount={opponentHand}
          energy={opponent.availableEnergy ?? 0}
          untappedRunes={opponentUntappedRunes}
          isActive={state.activePlayerId === opponent.userId}
          isMe={false}
          deckCount={opponentDeckCount}
          runeDeckCount={opponent.runePoolRemaining ?? 0}
          discardCards={opponentDiscardCards}
          revealedSnapshot={opponentRevealedSnapshot}
          revealedCards={revealedCardsFor(opponentRevealedSnapshot)}
          onDismissRevealed={dismissRevealed}
          onHover={handleCardHover}
        />
      ) : null}
      {me ? (
        <PlayerPanel
          name={player.name}
          score={me.score}
          handCount={hand.length}
          energy={me.availableEnergy ?? 0}
          effectiveEnergy={(me.availableEnergy ?? 0) + pendingRuneEnergy}
          untappedRunes={myUntappedRunes}
          isActive={state.activePlayerId === player.id}
          isMe
          bottom={handHeight + PHASE_BAR_HEIGHT + 4}
          deckCount={myDeckCount}
          runeDeckCount={me.runePoolRemaining ?? 0}
          discardCards={myDiscardCards}
          revealedSnapshot={myRevealedSnapshot}
          revealedCards={revealedCardsFor(myRevealedSnapshot)}
          onDismissRevealed={dismissRevealed}
          onHover={handleCardHover}
          cardScale={cardScale}
          onCardScaleChange={setCardScale}
          onLeave={() => navigate('/')}
        />
      ) : null}
      <PhaseBar
        currentPhase={state.currentPhase ?? 'MAIN'}
        isMyTurn={isMyTurn}
        canPass={canPass}
        passLabel={passLabel}
        opponentName={opponentName}
        onPassPhase={handlePassPhase}
        activeShowdown={showdownActive}
        showdownStep={state.activeShowdown?.step}
        showdownFocusName={showdownFocusName}
        showdownLocationLabel={activeShowdownLocationLabel}
        showdownReadyToResolve={state.activeShowdown?.readyToResolve}
        activeChain={chainActive}
        chainFocusName={chainFocusName}
        chainReadyToResolve={state.chainState?.readyToResolveTop}
        chainItemCount={state.chainState?.chainItems?.length ?? 0}
        chainItems={state.chainState?.chainItems ?? []}
        chainPlayerNames={chainPlayerNames}
        chainTargetNames={chainTargetNames}
        chainTargetSelectionActive={Boolean(pendingChainTargetSelection)}
        isChainItemTargetable={(item) => pendingChainTargetSelection?.mode === 'NOT_SO_FAST'
          ? isLegalNotSoFastChainTarget(item)
          : isLegalDefyChainTarget(item)}
        onChainItemTarget={(itemId) => {
          if (pendingChainTargetSelection) playCounterReaction(pendingChainTargetSelection.instanceId, itemId);
        }}
        onCancelChainTargetSelection={() => setPendingChainTargetSelection(null)}
        guidance={guidanceText}
        legalActionHint={actionHintText}
        waitingStatus={waitingText}
        connectionStatus={connectionText}
        bottom={handHeight}
      />
      {canPlayReactions && hasSpellReaction ? (
        <div className="pointer-events-none absolute left-0 z-20 flex justify-center text-xs font-medium text-forge" style={{ right: `${SIDEBAR_WIDTH}px`, bottom: handHeight + PHASE_BAR_HEIGHT + 6 }}>
          {chainActive ? 'Supported chain response available' : 'Supported Action window available'}
        </div>
      ) : null}

      {chainActive ? (
        <div className="pointer-events-auto absolute right-[296px] z-30 w-64 border border-line bg-panel/95 p-3 text-xs text-slate-300 shadow-glow" style={{ bottom: handHeight + PHASE_BAR_HEIGHT + 118 }}>
          <div className="flex items-center justify-between gap-2">
            <p className="font-semibold uppercase tracking-wide text-forge">Priority stops</p>
            {priorityWindowIsMine ? <span className="text-[10px] uppercase text-mint">Your priority</span> : <span className="text-[10px] uppercase text-slate-500">Public window</span>}
          </div>
          {localEmptyPriorityWindow ? (
            <p className="mt-1 text-[11px] text-slate-400">No legal responses are available to you. You may still pass manually.</p>
          ) : null}
          <div className="mt-2 grid gap-1.5">
            <PriorityStopToggle
              label="Auto-pass empty windows"
              checked={priorityStops.autoPassEmptyWindows}
              onChange={(value) => updatePriorityStop('autoPassEmptyWindows', value)}
            />
            <PriorityStopToggle
              label="Hold opponent turn"
              checked={priorityStops.holdOpponentTurn}
              onChange={(value) => updatePriorityStop('holdOpponentTurn', value)}
            />
            <PriorityStopToggle
              label="Hold showdowns"
              checked={priorityStops.holdShowdowns}
              onChange={(value) => updatePriorityStop('holdShowdowns', value)}
            />
            <PriorityStopToggle
              label="Hold before my spells"
              checked={priorityStops.holdBeforeMySpells}
              onChange={(value) => updatePriorityStop('holdBeforeMySpells', value)}
            />
            <PriorityStopToggle
              label="Hold before opponent spells"
              checked={priorityStops.holdBeforeOpponentSpells}
              onChange={(value) => updatePriorityStop('holdBeforeOpponentSpells', value)}
            />
          </div>
        </div>
      ) : null}

      {canAssignCombatDamage ? (
        <div className="pointer-events-auto absolute left-1/2 z-30 w-80 -translate-x-1/2 border border-forge/60 bg-[#05080d] p-3 text-sm text-slate-100 shadow-[0_14px_44px_rgba(0,0,0,0.7)]" style={{ bottom: handHeight + PHASE_BAR_HEIGHT + 12 }}>
          <p className="text-xs uppercase text-slate-500">Combat damage</p>
          <p className="mt-1 font-semibold text-forge">Resolve your combat damage</p>
          <p className="mt-1 text-xs text-slate-400">
            Damage pool: {state.combatAssignmentState?.damagePool ?? 0}. RiftForge will use the server-planned assignment for this lane, respecting Tank and lethal-first rules.
          </p>
          <button className="btn-primary mt-3 w-full" onClick={assignCombatDamage}>
            Resolve Combat Damage
          </button>
        </div>
      ) : null}

      {selectedChampionDeployable && selectedInstance && selectedCardDef ? (
        <div className="pointer-events-auto absolute left-1/2 top-3 z-30 w-72 -translate-x-1/2 border border-forge/60 bg-[#05080d] p-3 text-sm text-slate-100 shadow-[0_14px_44px_rgba(0,0,0,0.7)]">
          <p className="text-xs uppercase text-slate-500">Champion zone</p>
          <p className="mt-1 font-semibold text-forge">{selectedCardDef.name}</p>
          <p className="mt-1 text-xs text-slate-400">Deploy from the Champion zone for {selectedCardDef.cost ?? 0} energy.</p>
          <button className="btn-primary mt-3 w-full" onClick={() => deployChampionFromZone(selectedInstance.instanceId)}>
            Deploy Champion
          </button>
        </div>
      ) : null}

      <div className="pointer-events-auto absolute right-[296px] top-3 z-20 flex max-w-[260px] flex-col gap-2 text-xs">
        {opponentHiddenCount > 0 ? (
          <div className="border border-line bg-panel/90 px-3 py-2 text-slate-300">
            {opponentName} has {opponentHiddenCount} hidden card{opponentHiddenCount === 1 ? '' : 's'}.
          </div>
        ) : null}
        {myHiddenCards.length > 0 ? (
          <div className="border border-forge/50 bg-panel/95 px-3 py-2">
            <p className="font-semibold text-forge">Hidden cards</p>
            <div className="mt-1 flex flex-wrap gap-1">
              {myHiddenCards.map(({ instanceId, card }) => (
                <button
                  key={instanceId}
                  className="border border-line bg-ink px-2 py-1 text-left text-slate-200 hover:border-forge"
                  onClick={() => notifyWarning('Hidden timing unavailable', 'Playing hidden cards later is not implemented yet.')}
                  onMouseEnter={() => handleCardHover(card)}
                  onMouseLeave={() => handleCardHover(null)}
                >
                  {card.name}
                </button>
              ))}
            </div>
          </div>
        ) : null}
      </div>
      <GameSidebar log={state.log} chat={chat} onSend={sendChat} />
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
          onAmbush={playCardWithAmbush}
          onHide={hideFromHand}
          onDiscard={discardFromHand}
          cardScale={cardScale}
          onHover={handleCardHover}
          effectiveEnergy={spendableEnergy}
          canPlayCards={canPlayCards && !chainActive}
          canAmbushCards={canAmbushCards}
          canHideCards={canHideCards}
          canPlayReactions={canPlayReactions}
          canPlayReactionCard={canPlayChainReactionCard}
          embedded
          maxHeight={handHeight}
        />
      </div>
      <CardPreview
        card={hoveredCard}
        instance={hoveredInstance}
        allInstances={state.cards}
        cardsById={cardsById}
        onInspect={setInspectCard}
      />
      {pendingAccelerate ? (
        <div className="absolute inset-0 z-40 grid place-items-center bg-ink/80 px-5">
          <section className="w-full max-w-sm border border-line bg-panel p-5 shadow-glow">
            <h2 className="text-lg font-semibold text-forge">Accelerate {pendingAccelerate.card.name}?</h2>
            <p className="mt-2 text-sm text-slate-300">Pay 1 extra energy to enter ready?</p>
            <div className="mt-5 flex justify-end gap-3">
              <button
                className="btn-secondary"
                onClick={() => {
                  playCardToBase(pendingAccelerate.instanceId, pendingAccelerate.card);
                  setPendingAccelerate(null);
                }}
              >
                No
              </button>
              <button
                className="btn-primary"
                onClick={() => {
                  playCardToBase(pendingAccelerate.instanceId, pendingAccelerate.card, true);
                  setPendingAccelerate(null);
                }}
              >
                Yes
              </button>
            </div>
          </section>
        </div>
      ) : null}
      {pendingVision ? (
        <div className="absolute inset-0 z-40 grid place-items-center bg-ink/80 px-5">
          <section className="w-full max-w-md border border-line bg-panel p-5 shadow-glow">
            <h2 className="text-lg font-semibold text-forge">Vision</h2>
            <div className="mt-4 flex items-center gap-4">
              {cardsById.get(pendingVision.cardId)?.imageUrl ? (
                <img className="w-32 object-contain" src={cardsById.get(pendingVision.cardId)?.imageUrl} alt={pendingVision.cardName} />
              ) : null}
              <div>
                <p className="text-xs uppercase text-slate-500">Top of your deck</p>
                <p className="mt-1 font-semibold text-slate-100">{pendingVision.cardName}</p>
              </div>
            </div>
            <div className="mt-5 flex justify-end gap-3">
              <button
                className="btn-secondary"
                onClick={() => {
                  publishMove({ type: 'VISION_CHOICE', playerId: player.id, recycle: true });
                  setPendingVision(null);
                }}
              >
                Recycle (bottom of deck)
              </button>
              <button
                className="btn-primary"
                onClick={() => {
                  publishMove({ type: 'VISION_CHOICE', playerId: player.id, recycle: false });
                  setPendingVision(null);
                }}
              >
                Keep on top
              </button>
            </div>
          </section>
        </div>
      ) : null}
      {pendingChoice ? (
        <div className="absolute inset-0 z-40 grid place-items-center bg-ink/85 px-5">
          <section className={`w-full ${pendingCardOptions.length > 0 ? 'max-w-4xl' : 'max-w-md'} border border-forge/60 bg-[#05080d] p-5 text-slate-100 shadow-[0_18px_60px_rgba(0,0,0,0.78)] ring-1 ring-white/5`}>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Choice pending</p>
            <h2 className="mt-1 text-lg font-semibold text-forge">{pendingChoice.prompt}</h2>
            {pendingChoice.sourceCardId ? (
              <p className="mt-2 text-sm text-slate-400">
                Source: {cardsById.get(pendingChoice.sourceCardId)?.name ?? pendingChoice.sourceCardId}
              </p>
            ) : null}
            {pendingCardOptions.length > 0 ? (
              <div className="mt-5 grid gap-3 md:grid-cols-3">
                {pendingCardOptions.map((option) => {
                  const selected = selectedChoiceCardOptionId === option.optionId;
                  const assignment = choiceAssignments.find((item) => item.optionId === option.optionId);
                  return (
                    <div
                      key={option.optionId}
                      className={`border bg-panel/95 p-3 shadow-lg ${selected ? 'border-forge' : 'border-line'} ${assignment ? 'opacity-80' : ''}`}
                    >
                      {option.imageUrl ? (
                        <img src={option.imageUrl} alt={option.name} className="mx-auto aspect-[5/7] max-h-48 rounded border border-line object-cover" />
                      ) : (
                        <div className="grid aspect-[5/7] max-h-48 place-items-center border border-line bg-ink text-sm text-slate-400">{option.name}</div>
                      )}
                      <h3 className="mt-3 text-sm font-semibold text-slate-100">{option.name}</h3>
                      {option.rulesText ? <p className="mt-2 max-h-20 overflow-auto text-xs leading-relaxed text-slate-300">{option.rulesText}</p> : null}
                      {isTopDeckChoice ? (
                        <button
                          type="button"
                          className={`mt-3 w-full ${selected ? 'btn-primary' : 'btn-secondary'}`}
                          onClick={() => setSelectedChoiceCardOptionId(option.optionId)}
                        >
                          {selected ? 'Selected' : 'Choose for hand'}
                        </button>
                      ) : null}
                      {isPredictChoice ? (
                        <div className="mt-3 grid grid-cols-2 gap-2">
                          <button
                            type="button"
                            className={assignment?.action === 'TOP' ? 'btn-primary' : 'btn-secondary'}
                            disabled={choiceSubmitting}
                            onClick={() => assignChoiceCard(option.optionId, 'TOP')}
                          >
                            Top
                          </button>
                          <button
                            type="button"
                            className={assignment?.action === 'BOTTOM' ? 'btn-primary' : 'btn-secondary'}
                            disabled={choiceSubmitting}
                            onClick={() => assignChoiceCard(option.optionId, 'BOTTOM')}
                          >
                            Bottom
                          </button>
                        </div>
                      ) : null}
                      {assignment ? <p className="mt-2 text-xs text-forge">Assigned {assignment.action.toLowerCase()}.</p> : null}
                    </div>
                  );
                })}
              </div>
            ) : null}
            <div className="mt-5 flex flex-wrap justify-end gap-3">
              {isTopDeckChoice ? (
                <button
                  className="btn-primary"
                  disabled={!canResolveChoice || choiceSubmitting || !selectedChoiceCardOptionId}
                  onClick={() => {
                    if (!canResolveChoice || !pendingChoice || !selectedChoiceCardOptionId) {
                      notifyWarning('Choice unavailable', 'Choose one card before resolving this effect.');
                      return;
                    }
                    setChoiceSubmitting(true);
                    publishMove({
                      type: 'RESOLVE_CHOICE',
                      playerId: player.id,
                      choiceId: pendingChoice.choiceId,
                      selectedCardOptionId: selectedChoiceCardOptionId,
                      selectedAction: 'HAND',
                    });
                  }}
                >
                  {choiceSubmitting ? 'Submitting...' : 'Put selected into hand'}
                </button>
              ) : null}
              {isPredictChoice ? (
                <button
                  className="btn-primary"
                  disabled={!canResolveChoice || choiceSubmitting || !predictChoiceReady}
                  onClick={() => {
                    if (!canResolveChoice || !pendingChoice || !predictChoiceReady) {
                      notifyWarning('Choice unavailable', 'Assign every card to top or bottom before resolving.');
                      return;
                    }
                    setChoiceSubmitting(true);
                    publishMove({
                      type: 'RESOLVE_CHOICE',
                      playerId: player.id,
                      choiceId: pendingChoice.choiceId,
                      assignments: choiceAssignments,
                    });
                  }}
                >
                  {choiceSubmitting ? 'Submitting...' : 'Resolve order'}
                </button>
              ) : null}
              {!isTopDeckChoice && !isPredictChoice ? pendingChoice.options.map((option) => (
                <button
                  key={option.id}
                  className={option.id === 'NO' || option.id === 'DECLINE' ? 'btn-secondary' : 'btn-primary'}
                  disabled={!canResolveChoice || choiceSubmitting}
                  onClick={() => {
                    if (!canResolveChoice || !pendingChoice) {
                      notifyWarning('Choice unavailable', 'The server has not approved resolving this choice yet.');
                      return;
                    }
                    setChoiceSubmitting(true);
                    publishMove({ type: 'RESOLVE_CHOICE', playerId: player.id, choiceId: pendingChoice.choiceId, selectedOptionId: option.id });
                  }}
                >
                  {choiceSubmitting ? 'Submitting...' : option.label}
                </button>
              )) : null}
            </div>
          </section>
        </div>
      ) : null}
      {state.currentPhase === 'SELECT_BATTLEFIELD' ? (
        <div className="absolute inset-0 z-30 flex items-center justify-center bg-ink/95 px-6 py-8">
          {selectedBattlefield ? (
            <section className="border border-line bg-panel px-10 py-8 text-center shadow-glow">
              <h2 className="text-xl font-semibold text-forge">Battlefield locked in</h2>
              <p className="mt-2 text-sm text-slate-400">Waiting for the other player to choose...</p>
              <p className="mt-3 text-sm text-slate-300">{cardsById.get(selectedBattlefield)?.name ?? selectedBattlefield}</p>
            </section>
          ) : battlefieldChoices.length === 0 ? (
            <section className="border border-line bg-panel px-10 py-8 text-center shadow-glow">
              <h2 className="text-xl font-semibold text-forge">Waiting for Battlefields...</h2>
              <p className="mt-2 text-sm text-slate-400">The server is preparing your Battlefield choices. If this persists, copy debug info and restart the room.</p>
              <button type="button" className="btn-secondary mt-4" onClick={() => navigate('/')}>Back to Home</button>
            </section>
          ) : (
            <section className="w-full max-w-4xl border border-line bg-panel p-6 shadow-glow">
              <div className="mb-5">
                <h2 className="text-2xl font-semibold text-forge">Choose your Battlefield</h2>
                <p className="mt-1 text-sm text-slate-400">Pick one of your three Battlefields. It will be revealed and locked before mulligans begin.</p>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                {battlefieldChoices.map((cardId) => {
                  const card = cardsById.get(cardId);
                  return (
                    <button
                      type="button"
                      key={cardId}
                      className="group border border-line bg-void p-3 text-left transition-colors hover:border-forge hover:bg-forge/10 disabled:cursor-not-allowed disabled:opacity-50"
                      disabled={!wsConnected || !canSelectBattlefield}
                      onMouseEnter={() => handleCardHover(card ?? null)}
                      onMouseLeave={() => handleCardHover(null)}
                      onClick={() => selectBattlefield(cardId)}
                    >
                      {card?.imageUrl ? <img className="aspect-[5/7] w-full object-contain" src={card.imageUrl} alt={card.name} /> : null}
                      <span className="mt-3 block truncate text-sm font-semibold text-forge">{card?.name ?? cardId}</span>
                      <span className="mt-1 block text-xs text-slate-400">Select Battlefield</span>
                    </button>
                  );
                })}
              </div>
              <div className="mt-6 flex items-center justify-between gap-3">
                {!wsConnected ? <p className="text-sm text-ember">Connection lost - reconnecting...</p> : <p className="text-sm text-slate-500">Mulligans unlock after both players select.</p>}
              </div>
            </section>
          )}
        </div>
      ) : null}
      {state.currentPhase === 'MULLIGAN' && (canKeepHand || canMulligan || hasMulliganed) ? (
        <div className="absolute inset-0 z-30 flex items-center justify-center bg-ink/95 px-6 py-8">
          {hasMulliganed ? (
            <section className="border border-line bg-panel px-10 py-8 text-center shadow-glow">
              <h2 className="text-xl font-semibold text-forge">Opening hand locked in</h2>
              <p className="mt-2 text-sm text-slate-400">Waiting for opponent...</p>
            </section>
          ) : hand.length === 0 ? (
            <section className="border border-line bg-panel px-10 py-8 text-center shadow-glow">
              <h2 className="text-xl font-semibold text-forge">Waiting for cards...</h2>
              <p className="mt-2 text-sm text-slate-400">The server is preparing your opening hand. If this persists, copy debug info and restart the room.</p>
              <button type="button" className="btn-secondary mt-4" onClick={() => navigate('/')}>Back to Home</button>
            </section>
          ) : (
            <section className="w-full max-w-5xl border border-line bg-panel p-6 shadow-glow">
              <div className="mb-5 flex items-end justify-between gap-4">
                <div>
                  <h2 className="text-2xl font-semibold text-forge">Choose your opening hand</h2>
                  <p className="mt-1 text-sm text-slate-400">Checked cards stay. Unchecked cards are shuffled back and replaced.</p>
                </div>
                <span className="text-sm font-semibold text-slate-300">
                  Keep {hand.length - mulliganDiscardIds.size} / Discard {mulliganDiscardIds.size}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
                {hand.map((instance) => {
                  const card = cardsById.get(instance.cardId);
                  const checked = mulliganDiscardIds.has(instance.instanceId);
                  return (
                    <button
                      type="button"
                      key={instance.instanceId}
                      aria-pressed={checked}
                      className={`cursor-pointer border p-2 text-left transition-all ${
                        checked ? 'border-ember bg-ember/10 opacity-70' : 'border-forge bg-forge/10 opacity-100'
                      }`}
                      onMouseEnter={() => handleCardHover(card ?? null, instance)}
                      onMouseLeave={() => handleCardHover(null)}
                      onClick={() =>
                        setMulliganDiscardIds((previous) => {
                          const next = new Set(previous);
                          if (next.has(instance.instanceId)) next.delete(instance.instanceId);
                          else if (next.size < 2) next.add(instance.instanceId);
                          return next;
                        })
                      }
                    >
                      {card?.imageUrl ? <img className="aspect-[5/7] w-full object-contain" src={card.imageUrl} alt={card.name} /> : null}
                      <span className={`mt-2 block truncate text-sm font-semibold ${checked ? 'text-ember' : 'text-forge'}`}>
                        {checked ? 'Mulligan: ' : 'Keep: '}
                        {card?.name ?? 'Unknown card'}
                      </span>
                    </button>
                  );
                })}
              </div>
              <div className="mt-6 flex justify-end gap-3">
                {!wsConnected ? <p className="mr-auto self-center text-sm text-ember">Connection lost - reconnecting...</p> : null}
                <button className="btn-secondary disabled:opacity-40" disabled={!wsConnected || !canMulligan} onClick={() => submitMulligan([...mulliganDiscardIds])}>
                  Mulligan Selected
                </button>
                <button className="btn-primary disabled:opacity-40" disabled={!wsConnected || !canKeepHand} onClick={() => submitMulligan([])}>
                  Keep All
                </button>
              </div>
            </section>
          )}
        </div>
      ) : null}
      {!wsConnected && !loading && !error ? (
        <div className="pointer-events-none absolute inset-x-0 top-0 z-40 flex items-center justify-center gap-2 bg-ember/90 px-4 py-2 text-sm font-medium text-white">
          <span className="animate-pulse">!</span>
          Connection lost - reconnecting to player-specific updates...
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
              Home
            </button>
          </div>
        </div>
      ) : null}
      <div className="absolute bottom-3 left-3 z-30 flex flex-wrap gap-2">
        <button className="border border-line bg-panel px-3 py-2 text-xs font-semibold text-slate-300 hover:border-forge hover:text-forge" onClick={() => void copyDebugInfo()}>
          Copy debug info
        </button>
        <button className="border border-line bg-panel px-3 py-2 text-xs font-semibold text-slate-300 hover:border-forge hover:text-forge" onClick={() => void reportIssue()}>
          Report issue
        </button>
        <button className="border border-forge/50 bg-panel px-3 py-2 text-xs font-semibold text-forge hover:bg-forge hover:text-ink" onClick={() => setShowAlphaInfo(true)}>
          Alpha limits
        </button>
      </div>
      {showAlphaInfo ? (
        <div className="fixed inset-0 z-[100] grid place-items-center bg-black/70 p-4" onMouseDown={() => setShowAlphaInfo(false)}>
          <section className="w-full max-w-lg border border-line bg-panel p-5 shadow-glow" onMouseDown={(event) => event.stopPropagation()}>
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Public alpha</p>
                <h2 className="mt-1 text-xl font-semibold text-forge">Known playtest limitations</h2>
                <p className="mt-1 text-xs text-slate-500">Build {appBuildLabel()}</p>
              </div>
              <button className="icon-btn" aria-label="Close alpha limitations" onClick={() => setShowAlphaInfo(false)}>
                x
              </button>
            </div>
            <ul className="mt-4 space-y-2 text-sm leading-5 text-slate-300">
              {ALPHA_LIMITATIONS.map((item) => (
                <li key={item} className="border-l-2 border-forge/40 pl-3">
                  {item}
                </li>
              ))}
            </ul>
            <p className="mt-4 text-xs leading-5 text-slate-500">
              Support badges mean: Partial is playable for alpha testing but may be inaccurate; Unsupported is blocked in enforced play when supported-cards-only mode is enabled; Banned is not constructed legal; Not Audited is not available in supported-only mode yet.
            </p>
          </section>
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
