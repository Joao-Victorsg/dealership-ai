// lib/stores/inventory.ts
// Zustand store for inventory UI state.
// Manages mobile filter panel open/close state.
// Source: tasks.md T038; research.md R-14

import { create } from "zustand";

interface InventoryStore {
  isFilterPanelOpen: boolean;
  openFilterPanel: () => void;
  closeFilterPanel: () => void;
  toggleFilterPanel: () => void;
}

export const useInventoryStore = create<InventoryStore>((set) => ({
  isFilterPanelOpen: false,
  openFilterPanel: () => set({ isFilterPanelOpen: true }),
  closeFilterPanel: () => set({ isFilterPanelOpen: false }),
  toggleFilterPanel: () =>
    set((state) => ({ isFilterPanelOpen: !state.isFilterPanelOpen })),
}));
