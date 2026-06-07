import { getGameServerUrl } from './env';
import type { RoomState } from '../types';

export async function createRoom(hostId: string, hostName = 'Player'): Promise<RoomState> {
  const response = await fetch(`${getGameServerUrl()}/api/rooms`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerId: hostId, playerName: hostName }),
  });
  if (!response.ok) throw new Error('Unable to create room.');
  return (await response.json()) as RoomState;
}

export async function getRoomByCode(code: string): Promise<RoomState | null> {
  const response = await fetch(`${getGameServerUrl()}/api/rooms/${code.toUpperCase()}`);
  if (response.status === 404) return null;
  if (!response.ok) throw new Error('Unable to load room.');
  return (await response.json()) as RoomState;
}

export async function addPlayerToRoom(code: string, userId: string, playerName = 'Player'): Promise<RoomState> {
  const response = await fetch(`${getGameServerUrl()}/api/rooms/${code.toUpperCase()}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerId: userId, playerName }),
  });
  if (!response.ok) throw new Error('Unable to join room.');
  return (await response.json()) as RoomState;
}

export async function startRoom(code: string, playerId = ''): Promise<RoomState> {
  const response = await fetch(`${getGameServerUrl()}/api/rooms/${code.toUpperCase()}/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerId }),
  });
  if (!response.ok) throw new Error('Unable to start room.');
  return (await response.json()) as RoomState;
}
