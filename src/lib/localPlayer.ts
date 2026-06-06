export interface LocalPlayer {
  id: string;
  name: string;
}

const KEY = 'riftforge:player';

export function getLocalPlayer(): LocalPlayer | null {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? 'null') as LocalPlayer | null;
  } catch {
    return null;
  }
}

export function saveLocalPlayer(player: LocalPlayer): void {
  localStorage.setItem(KEY, JSON.stringify(player));
}

export function getOrCreateLocalPlayer(): LocalPlayer {
  const existing = getLocalPlayer();
  if (existing) return existing;
  const player: LocalPlayer = { id: crypto.randomUUID(), name: '' };
  saveLocalPlayer(player);
  return player;
}
