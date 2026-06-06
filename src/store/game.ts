import { create } from 'zustand';
import type { CardInstance, ChatMessage, LiveGameState, RiftCard, ZoneName } from '../types';

type GameStore = {
  state: LiveGameState | null;
  chat: ChatMessage[];
  initState: (roomCode: string, playerIds: string[]) => LiveGameState;
  setState: (state: LiveGameState) => void;
  addChat: (msg: ChatMessage) => void;
  moveCard: (instanceId: string, x: number, y: number, zone: ZoneName) => void;
  tapCard: (instanceId: string) => void;
  flipCard: (instanceId: string) => void;
  addCardToBoard: (card: RiftCard, ownerId: string, zone: ZoneName, x: number, y: number) => void;
  removeCard: (instanceId: string) => void;
  adjustScore: (userId: string, delta: number) => void;
  appendLog: (userId: string, text: string) => void;
  bringToFront: (instanceId: string) => void;
};

const touch = (state: LiveGameState): LiveGameState => ({ ...state, updatedAt: new Date().toISOString() });

const updateCards = (state: LiveGameState | null, updater: (cards: CardInstance[]) => CardInstance[]): LiveGameState | null => {
  if (!state) return state;
  return touch({ ...state, cards: updater(state.cards) });
};

const appendLogToState = (state: LiveGameState | null, userId: string, text: string): LiveGameState | null => {
  if (!state) return state;
  return touch({
    ...state,
    log: [...state.log, { id: crypto.randomUUID(), timestamp: new Date().toISOString(), userId, text }],
  });
};

export const useGameStore = create<GameStore>((set) => ({
  state: null,
  chat: [],
  initState: (roomCode, playerIds) => {
    const state: LiveGameState = {
      roomCode,
      cards: [],
      players: playerIds.map((userId) => ({ userId, score: 0 })),
      log: [],
      updatedAt: new Date().toISOString(),
    };
    set({ state, chat: [] });
    return state;
  },
  setState: (state) => set({ state }),
  addChat: (msg) => set((store) => ({ chat: [...store.chat, msg] })),
  moveCard: (instanceId, x, y, zone) =>
    set((store) => ({
      state: updateCards(store.state, (cards) => cards.map((card) => (card.instanceId === instanceId ? { ...card, x, y, zone } : card))),
    })),
  tapCard: (instanceId) =>
    set((store) => ({
      state: updateCards(store.state, (cards) => cards.map((card) => (card.instanceId === instanceId ? { ...card, tapped: !card.tapped } : card))),
    })),
  flipCard: (instanceId) =>
    set((store) => ({
      state: updateCards(store.state, (cards) => cards.map((card) => (card.instanceId === instanceId ? { ...card, faceDown: !card.faceDown } : card))),
    })),
  addCardToBoard: (card, ownerId, zone, x, y) =>
    set((store) => {
      if (!store.state) return { state: store.state };
      const maxZ = Math.max(0, ...store.state.cards.map((instance) => instance.zIndex));
      return {
        state: touch({
          ...store.state,
          cards: [
            ...store.state.cards,
            {
              instanceId: crypto.randomUUID(),
              cardId: card.id,
              ownerId,
              zone,
              x,
              y,
              tapped: false,
              faceDown: zone === 'deck',
              zIndex: maxZ + 1,
            },
          ],
        }),
      };
    }),
  removeCard: (instanceId) => set((store) => ({ state: updateCards(store.state, (cards) => cards.filter((card) => card.instanceId !== instanceId)) })),
  adjustScore: (userId, delta) =>
    set((store) => {
      if (!store.state) return { state: store.state };
      return {
        state: touch({
          ...store.state,
          players: store.state.players.map((player) => (player.userId === userId ? { ...player, score: Math.max(0, player.score + delta) } : player)),
        }),
      };
    }),
  appendLog: (userId, text) => set((store) => ({ state: appendLogToState(store.state, userId, text) })),
  bringToFront: (instanceId) =>
    set((store) => {
      if (!store.state) return { state: store.state };
      const maxZ = Math.max(0, ...store.state.cards.map((card) => card.zIndex));
      return {
        state: updateCards(store.state, (cards) => cards.map((card) => (card.instanceId === instanceId ? { ...card, zIndex: maxZ + 1 } : card))),
      };
    }),
}));
