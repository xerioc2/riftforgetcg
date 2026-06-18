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
  activeShowdownDetails: {
    locationId: string | null;
    step: string | null;
    attackingPlayerId: string | null;
    focusedPlayerId: string | null;
    readyToResolve: boolean;
  };
  battlefieldController: Record<string, string>;
  selectedBattlefields: Array<{
    playerId: string;
    selectedBattlefieldId: string | null;
  }>;
  publicCards: Array<{
    instanceId: string;
    cardId: string | null;
    ownerId: string;
    zone: string;
    battlefieldLocationId: string | null;
    attachedToInstanceId: string | null;
    faceDown: boolean;
  }>;
  cardZoneCounts: Record<string, number>;
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

const PUBLIC_DEBUG_ZONES = new Set(['base', 'battlefield', 'champion', 'legend', 'discard', 'limbo']);

function isPublicDebugCard(card: LiveGameState['cards'][number]) {
  return PUBLIC_DEBUG_ZONES.has(card.zone.toLowerCase());
}

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
    activeShowdownDetails: {
      locationId: state?.activeShowdown?.locationId ?? null,
      step: state?.activeShowdown?.step ?? null,
      attackingPlayerId: state?.activeShowdown?.attackingPlayerId ?? null,
      focusedPlayerId: state?.activeShowdown?.focusedPlayerId ?? null,
      readyToResolve: state?.activeShowdown?.readyToResolve ?? false,
    },
    battlefieldController: state?.battlefieldController ?? {},
    selectedBattlefields: state?.players.map((statePlayer) => ({
      playerId: statePlayer.userId,
      selectedBattlefieldId: statePlayer.selectedBattlefieldId ?? null,
    })) ?? [],
    publicCards: state?.cards
      .filter(isPublicDebugCard)
      .map((card) => ({
        instanceId: card.instanceId,
        cardId: card.faceDown ? null : card.cardId,
        ownerId: card.ownerId,
        zone: card.zone,
        battlefieldLocationId: card.battlefieldLocationId ?? null,
        attachedToInstanceId: card.attachedToInstanceId ?? null,
        faceDown: card.faceDown,
      })) ?? [],
    cardZoneCounts: state?.cards.reduce<Record<string, number>>((counts, card) => {
      const key = `${card.ownerId}:${card.zone}`;
      counts[key] = (counts[key] ?? 0) + 1;
      return counts;
    }, {}) ?? {},
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
