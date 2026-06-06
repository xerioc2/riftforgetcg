import { create } from 'zustand';
import type { Deck } from '../types';

const STORAGE_KEY = 'riftforge.decks.v1';

const readDecks = (): Deck[] => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? (JSON.parse(saved) as Deck[]) : [];
  } catch {
    return [];
  }
};

const writeDecks = (decks: Deck[]) => localStorage.setItem(STORAGE_KEY, JSON.stringify(decks));

const createStarterDeck = (): Deck => ({
  id: crypto.randomUUID(),
  name: 'New deck',
  cards: [],
  updatedAt: new Date().toISOString(),
});

type DeckStore = {
  decks: Deck[];
  activeDeckId?: string;
  setActiveDeck: (id: string) => void;
  setDecks: (decks: Deck[]) => void;
  createDeck: () => void;
  updateDeck: (deck: Deck) => void;
  deleteDeck: (id: string) => void;
};

export const useDeckStore = create<DeckStore>((set) => {
  const initialDecks = readDecks();
  const decks = initialDecks.length ? initialDecks : [createStarterDeck()];
  writeDecks(decks);

  return {
    decks,
    activeDeckId: decks[0]?.id,
    setActiveDeck: (id) => set({ activeDeckId: id }),
    setDecks: (nextDecks) =>
      set(() => {
        const decks = nextDecks.length ? nextDecks : [createStarterDeck()];
        writeDecks(decks);
        return { decks, activeDeckId: decks[0]?.id };
      }),
    createDeck: () =>
      set((state) => {
        const deck = createStarterDeck();
        const next = [...state.decks, deck];
        writeDecks(next);
        return { decks: next, activeDeckId: deck.id };
      }),
    updateDeck: (deck) =>
      set((state) => {
        const next = state.decks.map((existing) =>
          existing.id === deck.id ? { ...deck, updatedAt: new Date().toISOString() } : existing,
        );
        writeDecks(next);
        return { decks: next };
      }),
    deleteDeck: (id) =>
      set((state) => {
        const next = state.decks.filter((deck) => deck.id !== id);
        const decks = next.length ? next : [createStarterDeck()];
        writeDecks(decks);
        return { decks, activeDeckId: decks[0]?.id };
      }),
  };
});
