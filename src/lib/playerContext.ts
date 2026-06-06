import React from 'react';
import type { LocalPlayer } from './localPlayer';

export const PlayerContext = React.createContext<LocalPlayer | null>(null);

export const useLocalPlayer = () => {
  const player = React.useContext(PlayerContext);
  if (!player) throw new Error('Local player context is missing.');
  return player;
};
