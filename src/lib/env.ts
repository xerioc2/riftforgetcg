function resolveGameServerUrl(): string {
  const fromEnv = import.meta.env.VITE_GAME_SERVER_URL as string | undefined;
  if (fromEnv) return fromEnv;
  // When served from Spring Boot (same origin), use the current host so ngrok/LAN URLs work automatically.
  // Tauri apps still fall back to localhost because the bundled server is always on 8080.
  const isTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
  if (!isTauri && typeof window !== 'undefined') return window.location.origin;
  return 'http://localhost:8080';
}

export const config = {
  riftcodexApiBase: (import.meta.env.VITE_RIFTCODEX_API_BASE as string | undefined) ?? 'https://api.riftcodex.com',
  gameServerUrl: resolveGameServerUrl(),
};

export function getGameServerUrl(): string {
  const override = localStorage.getItem('riftforge.serverUrl');
  if (override && override.trim()) return override.trim();
  return config.gameServerUrl;
}
