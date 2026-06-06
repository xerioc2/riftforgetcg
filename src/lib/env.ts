export const config = {
  riftcodexApiBase: (import.meta.env.VITE_RIFTCODEX_API_BASE as string | undefined) ?? 'https://api.riftcodex.com',
  gameServerUrl: (import.meta.env.VITE_GAME_SERVER_URL as string | undefined) ?? 'http://localhost:8080',
};

export const GAME_SERVER_URL = config.gameServerUrl;
