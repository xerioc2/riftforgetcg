function resolveGameServerUrl(): string {
  const fromEnv = import.meta.env.VITE_GAME_SERVER_URL as string | undefined;
  if (fromEnv) return fromEnv;
  // When served from Spring Boot (same origin), use the current host so ngrok/LAN URLs work automatically.
  // Tauri apps still fall back to localhost because the bundled server is always on 8080.
  const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
  if (!isTauri && typeof window !== 'undefined') return window.location.origin;
  return 'http://localhost:8080';
}

let _resolvedServerUrl: string | null = null;

export const config = {
  riftcodexApiBase: (import.meta.env.VITE_RIFTCODEX_API_BASE as string | undefined) ?? 'https://api.riftcodex.com',
  gameServerUrl: resolveGameServerUrl(),
};

export async function initServerUrl(): Promise<void> {
  const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
  if (!isTauri) return;
  try {
    const { invoke } = await import('@tauri-apps/api/core');
    const port = await invoke<number>('get_server_port');
    _resolvedServerUrl = `http://localhost:${port}`;
  } catch {
    // Leave the default localhost:8080 URL in place.
  }
}

export function setResolvedServerUrl(url: string): void {
  _resolvedServerUrl = url;
}

export function getGameServerCandidates(): string[] {
  const override = localStorage.getItem('riftforge.serverUrl');
  if (override && override.trim()) return [override.trim()];
  if (_resolvedServerUrl) return [_resolvedServerUrl];
  const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
  if (isTauri) return Array.from({ length: 20 }, (_, index) => `http://localhost:${8080 + index}`);
  return [config.gameServerUrl];
}

export function getGameServerUrl(): string {
  if (_resolvedServerUrl) return _resolvedServerUrl;
  const override = localStorage.getItem('riftforge.serverUrl');
  if (override && override.trim()) return override.trim();
  return config.gameServerUrl;
}
