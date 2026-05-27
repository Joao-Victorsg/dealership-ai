"use client";
// components/inventory/FilterChips.tsx
// Renders one chip per active filter. Chips update URL params.
// Source: tasks.md T037; data-model.md §8, FR-004

import { useRouter, useSearchParams, usePathname } from "next/navigation";
import type { InventoryFilters } from "@/lib/api/types";
import { DEFAULT_FILTERS } from "@/lib/api/types";

interface FilterChipsProps {
  filters: InventoryFilters;
}

const FILTER_LABELS: Record<string, string> = {
  category: "Category",
  type: "Type",
  condition: "Condition",
  manufacturer: "Manufacturer",
  color: "Color",
  kmMax: "KM max",
  kmMin: "KM min",
  priceMin: "Price min",
  priceMax: "Price max",
  yearMin: "Year min",
  yearMax: "Year max",
  sortBy: "Sort",
};

const CONDITION_LABELS: Record<string, string> = {
  NEW: "New",
  USED: "Pre-owned",
};

const SORT_BY_LABELS: Record<string, string> = {
  REGISTRATION_DATE: "Recently added",
  PRICE: "Price",
  YEAR: "Year",
};

const SORT_DIR_LABELS: Record<string, string> = {
  ASC: "ascending",
  DESC: "descending",
};

function chipLabel(key: string, value: string, allFilters: InventoryFilters): string {
  if (key === "condition") return CONDITION_LABELS[value] ?? value;
  if (key === "sortBy") {
    const dir = SORT_DIR_LABELS[allFilters.sortDirection] ?? allFilters.sortDirection;
    return `Sort: ${SORT_BY_LABELS[value] ?? value} (${dir})`;
  }
  return `${FILTER_LABELS[key] ?? key}: ${value}`;
}

// Keys that should not appear as chips (handled separately in UI)
const EXCLUDED_KEYS = new Set(["page", "q", "sortDirection"]);

export function FilterChips({ filters }: FilterChipsProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const activeFilters = Object.entries(filters).filter(([key, value]) => {
    if (EXCLUDED_KEYS.has(key)) return false;
    if (value === undefined || value === null || value === "") return false;
    const defaultValue = DEFAULT_FILTERS[key as keyof InventoryFilters];
    return value !== defaultValue;
  });

  if (activeFilters.length === 0) return null;

  function clearFilter(key: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.delete(key);
    if (key === "sortBy") params.delete("sortDirection");
    params.delete("page");
    router.push(`${pathname}?${params.toString()}`);
  }

  function clearAll() {
    router.push(pathname);
  }

  return (
    <div
      role="group"
      aria-label="Active filters"
      className="flex flex-wrap items-center gap-2"
    >
      {activeFilters.map(([key, value]) => (
        <span
          key={key}
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 text-xs font-medium text-foreground"
        >
          {chipLabel(key, String(value), filters)}
          <button
            type="button"
            onClick={() => clearFilter(key)}
            aria-label={`Remove filter ${FILTER_LABELS[key] ?? key}`}
            className="ml-1 flex h-4 w-4 items-center justify-center rounded-full hover:bg-muted focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <svg
              aria-hidden="true"
              className="h-3 w-3"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={3}
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </span>
      ))}

      <button
        type="button"
        onClick={clearAll}
        className="text-xs text-muted-foreground underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-[var(--radius-sm)] px-1"
      >
        Clear all
      </button>
    </div>
  );
}
