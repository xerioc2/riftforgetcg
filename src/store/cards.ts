import { create } from 'zustand';
import { fetchCardsFromRiftcodex, loadCachedCards } from '../lib/riftcodex';
import type { RiftCard } from '../types';

type CardStore = {
  cards: RiftCard[];
  loading: boolean;
  error?: string;
  loadCards: () => Promise<void>;
};

export const useCardStore = create<CardStore>((set) => ({
  cards: loadCachedCards(),
  loading: false,
  async loadCards() {
    set({ loading: true, error: undefined });
    try {
      const cards = await fetchCardsFromRiftcodex();
      set({ cards, loading: false });
    } catch (error) {
      set({
        loading: false,
        error: error instanceof Error ? error.message : 'Unable to load Riftcodex cards.',
      });
    }
  },
}));
