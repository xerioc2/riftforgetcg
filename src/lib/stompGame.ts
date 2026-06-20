import { Client } from '@stomp/stompjs';
import { getGameServerUrl } from './env';
import type { LocalPlayer } from './localPlayer';
import type { TargetRole } from './cardActions';
import type { CombatDamageAssignment, LiveGameState, PresenceSummary, RoomState } from '../types';

export type MoveRequest =
  | { type: 'DEAL_CARD'; playerId: string; cardId: string; targetZone: string; x: number; y: number }
  | { type: 'TAP_CARD'; playerId: string; instanceId: string }
  | { type: 'FLIP_CARD'; playerId: string; instanceId: string }
  | { type: 'PLAY_CARD'; playerId: string; instanceId: string; targetZone: string; x: number; y: number; targetInstanceId?: string; targetChainItemId?: string; targets?: { role: TargetRole; instanceId: string }[]; accelerate?: boolean; paymentRuneIds?: string[]; premiumRuneIds?: string[] }
  | { type: 'MOVE_CARD'; playerId: string; instanceId: string; targetZone: string; x: number; y: number }
  | { type: 'REPOSITION_CARD'; playerId: string; instanceId: string; x: number; y: number }
  | { type: 'MOVE_TO_BATTLEFIELD'; playerId: string; instanceId: string; battlefieldLocationId?: string; targetZone?: 'BASE' | 'BATTLEFIELD'; paymentRuneIds?: string[]; premiumRuneIds?: string[] }
  | { type: 'MOVE_TO_BASE'; playerId: string; instanceId: string }
  | { type: 'SELECT_BATTLEFIELD'; playerId: string; battlefieldCardId: string }
  | { type: 'TAP_RUNE'; playerId: string; runeInstanceId: string }
  | { type: 'DISCARD_RUNE'; playerId: string; runeInstanceId: string }
  | { type: 'MULLIGAN'; playerId: string; discardInstanceIds: string[] }
  | { type: 'UNDO_RUNES'; playerId: string }
  | { type: 'PASS_PHASE'; playerId: string }
  | { type: 'PASS_CHAIN_FOCUS'; playerId: string }
  | { type: 'RESOLVE_CHAIN_TOP'; playerId: string }
  | { type: 'PASS_SHOWDOWN_FOCUS'; playerId: string }
  | { type: 'ASSIGN_COMBAT_DAMAGE'; playerId: string; assignments: CombatDamageAssignment[] }
  | { type: 'RESOLVE_SHOWDOWN'; playerId: string }
  | { type: 'ADJUST_SCORE'; playerId: string; targetPlayerId: string; delta: number }
  | { type: 'VISION_CHOICE'; playerId: string; recycle: boolean }
  | {
      type: 'RESOLVE_CHOICE';
      playerId: string;
      choiceId: string;
      selectedOptionId?: string;
      selectedCardOptionId?: string;
      selectedAction?: 'HAND' | 'TOP' | 'BOTTOM';
      selectedTargetInstanceId?: string;
      assignments?: { optionId: string; action: 'TOP' | 'BOTTOM'; order: number }[];
    }
  | { type: 'DISMISS_REVEALED'; playerId: string; instanceId: string }
  | { type: 'HIDE_CARD'; playerId: string; instanceId: string; paymentRuneId: string }
  | { type: 'EQUIP_GEAR'; playerId: string; gearInstanceId: string; targetInstanceId: string; paymentRuneIds?: string[]; premiumRuneIds?: string[] }
  | { type: 'ACTIVATE_ABILITY'; playerId: string; sourceInstanceId: string; abilityKey?: string; targetInstanceId?: string | null; paymentRuneIds?: string[]; premiumRuneIds?: string[] };

export type ServerMessage = { type: 'STATE_UPDATE'; state: LiveGameState } | { type: 'ERROR'; message: string; playerId: string };
export type MatchNotification = { roomCode: string; sessionToken?: string };
export type MessageSource = 'room' | 'user';

function wsEndpoint(params: Record<string, string | undefined>): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value && value.trim()) query.set(key, value);
  });
  const suffix = query.toString();
  return `${getGameServerUrl().replace(/^http/, 'ws')}/ws${suffix ? `?${suffix}` : ''}`;
}

export function createGameClient(
  roomCode: string,
  player: LocalPlayer,
  sessionToken: string | undefined,
  onMessage: (msg: ServerMessage, source: MessageSource) => void,
  onConnected: () => void,
  onConnectionChange?: (connected: boolean) => void,
  useUserState = true,
): Client {
  const wsUrl = wsEndpoint(useUserState
    ? { playerId: player.id, playerName: player.name, roomCode, sessionToken: sessionToken ?? '' }
    : { playerId: player.id, playerName: player.name, role: 'SPECTATOR' });
  const connectHeaders: Record<string, string> = useUserState
    ? { playerId: player.id, playerName: player.name, roomCode, sessionToken: sessionToken ?? '' }
    : { playerId: player.id, playerName: player.name };
  let receivedUserState = false;
  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders,
    onConnect: () => {
      client.subscribe(`/topic/game/${roomCode}`, (frame) => {
        const msg = JSON.parse(frame.body) as ServerMessage;
        if (!useUserState || msg.type === 'ERROR' || (msg.type === 'STATE_UPDATE' && !receivedUserState)) onMessage(msg, 'room');
      });
      if (useUserState) {
        client.subscribe(`/user/topic/game/${roomCode}`, (frame) => {
          const msg = JSON.parse(frame.body) as ServerMessage;
          if (msg.type === 'STATE_UPDATE') receivedUserState = true;
          onMessage(msg, 'user');
        });
      }
      onConnected();
      onConnectionChange?.(true);
    },
    onWebSocketClose: () => onConnectionChange?.(false),
    onStompError: () => onConnectionChange?.(false),
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
}

export function createLobbyClient(
  roomCode: string,
  player: LocalPlayer,
  sessionToken: string | undefined,
  onRoom: (room: RoomState) => void,
  onPresence?: (presence: PresenceSummary) => void,
): Client {
  const wsUrl = wsEndpoint(sessionToken
    ? { playerId: player.id, playerName: player.name, roomCode, sessionToken }
    : { playerId: player.id, playerName: player.name, role: 'LOBBY' });
  const connectHeaders: Record<string, string> = sessionToken
    ? { playerId: player.id, playerName: player.name, roomCode, sessionToken }
    : { playerId: player.id, playerName: player.name };
  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders,
    onConnect: () => {
      client.subscribe(`/topic/lobby/${roomCode}`, (frame) => {
        onRoom(JSON.parse(frame.body) as RoomState);
      });
      if (onPresence) {
        client.subscribe('/topic/presence', (frame) => {
          onPresence(JSON.parse(frame.body) as PresenceSummary);
        });
      }
    },
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
}

export function createMatchmakingClient(player: LocalPlayer, onMatch: (notification: MatchNotification) => void, onConnected?: () => void): Client {
  const wsUrl = wsEndpoint({ playerId: player.id, playerName: player.name, role: 'MATCHMAKING' });
  const seenRoomCodes = new Set<string>();
  const handleNotification = (frame: { body: string }) => {
    const notification = JSON.parse(frame.body) as MatchNotification;
    if (seenRoomCodes.has(notification.roomCode)) return;
    seenRoomCodes.add(notification.roomCode);
    onMatch(notification);
  };
  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders: { playerId: player.id, playerName: player.name },
    onConnect: () => {
      client.subscribe('/user/queue/matchmaking', handleNotification);
      client.subscribe(`/topic/matchmaking/${player.id}`, handleNotification);
      onConnected?.();
    },
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
}

export function createPresenceClient(player: LocalPlayer, onPresence: (presence: PresenceSummary) => void, onDisconnect?: () => void): Client {
  const wsUrl = wsEndpoint({ playerId: player.id, playerName: player.name, role: 'LOBBY' });
  const client = new Client({
    brokerURL: wsUrl,
    connectHeaders: { playerId: player.id, playerName: player.name, role: 'LOBBY' },
    onConnect: () => {
      client.subscribe('/topic/presence', (frame) => {
        onPresence(JSON.parse(frame.body) as PresenceSummary);
      });
    },
    onWebSocketClose: () => onDisconnect?.(),
    onStompError: () => onDisconnect?.(),
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
}

export function sendMove(client: Client, roomCode: string, move: MoveRequest): void {
  client.publish({ destination: `/app/game/${roomCode}/move`, body: JSON.stringify(move) });
}

export function joinGame(client: Client, roomCode: string): void {
  client.publish({ destination: `/app/game/${roomCode}/join`, body: '{}' });
}
