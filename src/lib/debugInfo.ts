import type { LiveGameState } from '../types';

export type DebugInfoPayload = {
  app: 'RiftForge';
  appVersion: string;
  buildTag: string;
  buildDate: string;
  serverVersion: string;
  serverBuildTime: string;
  serverGitSha: string;
  serverFullGitSha: string;
  serverBuildTimestamp: string;
  serverReleaseTag: string;
  serverJarSha256: string;
  roomCode: string;
  phase: string;
  activePlayerId: string;
  playerId: string;
  playerName: string;
  opponentIds: string[];
  turnNumber: number | null;
  gameMode: string | null;
  activeShowdown: boolean;
  chainState: {
    active: boolean;
    itemCount: number;
    focusedPlayerId: string | null;
    readyToResolveTop: boolean;
  };
  pendingChoice: {
    active: boolean;
    type: string | null;
    playerId: string | null;
  };
  legalActions: string[];
  awaitingServerUpdate: boolean;
  lastSubmittedActionType: string | null;
  lastActionFailureMessage: string | null;
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
  serverVersion?: string;
  serverBuildTime?: string;
  serverGitSha?: string;
  serverFullGitSha?: string;
  serverBuildTimestamp?: string;
  serverReleaseTag?: string;
  serverJarSha256?: string;
  awaitingServerUpdate?: boolean;
  lastSubmittedActionType?: string | null;
  lastActionFailureMessage?: string | null;
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
  serverVersion = 'unknown',
  serverBuildTime = 'unknown',
  serverGitSha = 'unknown',
  serverFullGitSha = 'unknown',
  serverBuildTimestamp = 'unknown',
  serverReleaseTag = 'unknown',
  serverJarSha256 = 'unknown',
  awaitingServerUpdate = false,
  lastSubmittedActionType = null,
  lastActionFailureMessage = null,
  generatedAt = new Date().toISOString(),
}: BuildDebugInfoInput): DebugInfoPayload {
  return {
    app: 'RiftForge',
    appVersion,
    buildTag,
    buildDate,
    serverVersion,
    serverBuildTime,
    serverGitSha,
    serverFullGitSha,
    serverBuildTimestamp,
    serverReleaseTag,
    serverJarSha256,
    roomCode,
    phase: state?.currentPhase ?? 'unknown',
    activePlayerId: state?.activePlayerId ?? 'unknown',
    playerId: player.id,
    playerName: player.name,
    opponentIds: state?.players.filter((statePlayer) => statePlayer.userId !== player.id).map((statePlayer) => statePlayer.userId) ?? [],
    turnNumber: state?.turnNumber ?? null,
    gameMode: state?.gameMode ?? null,
    activeShowdown: Boolean(state?.activeShowdown),
    chainState: {
      active: Boolean(state?.chainState),
      itemCount: state?.chainState?.chainItems?.length ?? 0,
      focusedPlayerId: state?.chainState?.focusedPlayerId ?? null,
      readyToResolveTop: state?.chainState?.readyToResolveTop ?? false,
    },
    pendingChoice: {
      active: Boolean(state?.pendingChoice),
      type: state?.pendingChoice?.type ?? null,
      playerId: state?.pendingChoice?.playerId ?? null,
    },
    legalActions: state?.legalActions ?? [],
    awaitingServerUpdate,
    lastSubmittedActionType,
    lastActionFailureMessage,
    lastError: lastError ?? null,
    serverUrl,
    generatedAt,
  };
}
