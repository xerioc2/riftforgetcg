import type { RoomState } from '../types';

const KEY_PREFIX = 'riftforge:roomSession:';

export type TokenizedRoom = {
  room: RoomState;
  sessionToken: string;
};

export function saveRoomSession(roomCode: string, playerId: string, sessionToken: string): void {
  localStorage.setItem(sessionKey(roomCode, playerId), sessionToken);
}

export function getRoomSessionToken(roomCode: string, playerId: string): string | undefined {
  return localStorage.getItem(sessionKey(roomCode, playerId)) ?? undefined;
}

function sessionKey(roomCode: string, playerId: string): string {
  return `${KEY_PREFIX}${roomCode.toUpperCase()}:${playerId}`;
}
