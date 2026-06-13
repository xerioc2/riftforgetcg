import type { LiveGameState } from '../types';

export type DebugInfoPayload = {
  app: 'RiftForge';
  appVersion: string;
  buildTag: string;
  buildDate: string;
  roomCode: string;
  phase: string;
  activePlayerId: string;
  playerId: string;
  playerName: string;
  opponentIds: string[];
  turnNumber: number | null;
  gameMode: string | null;
  activeShowdown: boolean;
  lastError: string | null;
  serverUrl: string;
  generatedAt: string;
};

type BuildDebugInfoInput = {
  roomCode: string;
  state: LiveGameState | null;
  player: { id: string; name: string };
  lastError?: string | null;
  serverUrl: string;
  appVersion: string;
  buildTag: string;
  buildDate: string;
  generatedAt?: string;
};

export function buildDebugInfo({
  roomCode,
  state,
  player,
  lastError,
  serverUrl,
  appVersion,
  buildTag,
  buildDate,
  generatedAt = new Date().toISOString(),
}: BuildDebugInfoInput): DebugInfoPayload {
  return {
    app: 'RiftForge',
    appVersion,
    buildTag,
    buildDate,
    roomCode,
    phase: state?.currentPhase ?? 'unknown',
    activePlayerId: state?.activePlayerId ?? 'unknown',
    playerId: player.id,
    playerName: player.name,
    opponentIds: state?.players.filter((statePlayer) => statePlayer.userId !== player.id).map((statePlayer) => statePlayer.userId) ?? [],
    turnNumber: state?.turnNumber ?? null,
    gameMode: state?.gameMode ?? null,
    activeShowdown: Boolean(state?.activeShowdown),
    lastError: lastError ?? null,
    serverUrl,
    generatedAt,
  };
}
