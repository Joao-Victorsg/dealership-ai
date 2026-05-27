"use client";

import { useInventoryStore } from "@/lib/stores/inventory";

export function InventoryFilterToggle() {
  const toggleFilterPanel = useInventoryStore((state) => state.toggleFilterPanel);

  return (
    <button
      type="button"
      onClick={toggleFilterPanel}
      className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium text-foreground lg:hidden"
    >
      <svg
        aria-hidden="true"
        className="h-4 w-4"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M4 6h16M7 12h10M10 18h4" />
      </svg>
      Filters
    </button>
  );
}
