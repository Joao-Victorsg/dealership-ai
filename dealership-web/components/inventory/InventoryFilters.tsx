"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useInventoryStore } from "@/lib/stores/inventory";
import type { CarCategory, CarType } from "@/lib/api/types";

const CATEGORY_OPTIONS: CarCategory[] = [
  "SUV",
  "SEDAN",
  "HATCHBACK",
  "PICKUP",
  "COUPE",
  "CONVERTIBLE",
  "MINIVAN",
  "OTHER",
];

const TYPE_OPTIONS: CarType[] = ["ELECTRIC", "HYBRID", "GASOLINE", "DIESEL"];

const CATEGORY_LABELS: Record<CarCategory, string> = {
  SUV: "SUV",
  SEDAN: "Sedan",
  HATCHBACK: "Hatch",
  PICKUP: "Pick-up",
  COUPE: "Sport",
  CONVERTIBLE: "Convertible",
  MINIVAN: "Minivan",
  OTHER: "Other",
};

const TYPE_LABELS: Record<CarType, string> = {
  ELECTRIC: "Electric",
  HYBRID: "Hybrid",
  GASOLINE: "Gasoline",
  DIESEL: "Diesel",
};

export function InventoryFilters({
  manufacturerOptions,
  exteriorColorOptions,
}: {
  manufacturerOptions: string[];
  exteriorColorOptions: string[];
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { isFilterPanelOpen, closeFilterPanel } = useInventoryStore();

  function applyFilter(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.delete("page");
    router.push(`${pathname}?${params.toString()}`);
  }

  function clearAll() {
    router.push(pathname);
  }

  const current = {
    q: searchParams.get("q") ?? "",
    category: searchParams.get("category") ?? "",
    type: searchParams.get("type") ?? "",
    condition: searchParams.get("condition") ?? "",
    manufacturer: searchParams.get("manufacturer") ?? "",
    color: searchParams.get("color") ?? "",
    priceMin: searchParams.get("priceMin") ?? "",
    priceMax: searchParams.get("priceMax") ?? "",
    yearMin: searchParams.get("yearMin") ?? "",
    yearMax: searchParams.get("yearMax") ?? "",
    kmMin: searchParams.get("kmMin") ?? "",
    kmMax: searchParams.get("kmMax") ?? "",
  };

  const panelContent = (
    <div className="space-y-7">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-foreground">Filters</h2>
        <button
          type="button"
          onClick={clearAll}
          className="text-xs text-muted-foreground underline-offset-2 hover:underline"
        >
          Clear all
        </button>
      </div>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Search
        </legend>
        <input
          type="search"
          placeholder="Brand, model..."
          value={current.q}
          onChange={(e) => applyFilter("q", e.target.value)}
          aria-label="Search inventory"
          className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
        />
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Condition
        </legend>
        <div className="flex gap-2">
          {(["NEW", "USED"] as const).map((condition) => (
            <button
              key={condition}
              type="button"
              onClick={() => applyFilter("condition", current.condition === condition ? "" : condition)}
              aria-pressed={current.condition === condition}
              className={`rounded-md border px-3 py-1.5 text-xs transition-colors ${
                current.condition === condition
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border bg-card text-muted-foreground hover:border-border-strong"
              }`}
            >
              {condition === "NEW" ? "New" : "Pre-owned"}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Category
        </legend>
        <div className="grid grid-cols-2 gap-1.5">
          {CATEGORY_OPTIONS.map((category) => (
            <button
              key={category}
              type="button"
              onClick={() => applyFilter("category", current.category === category ? "" : category)}
              aria-pressed={current.category === category}
              className={`rounded-md border px-2.5 py-1.5 text-left text-xs transition-colors ${
                current.category === category
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border bg-card text-foreground/80 hover:border-border-strong"
              }`}
            >
              {CATEGORY_LABELS[category]}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Type
        </legend>
        <div className="grid grid-cols-2 gap-1.5">
          {TYPE_OPTIONS.map((type) => (
            <button
              key={type}
              type="button"
              onClick={() => applyFilter("type", current.type === type ? "" : type)}
              aria-pressed={current.type === type}
              className={`rounded-md border px-2.5 py-1.5 text-left text-xs transition-colors ${
                current.type === type
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border bg-card text-foreground/80 hover:border-border-strong"
              }`}
            >
              {TYPE_LABELS[type]}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Manufacturer
        </legend>
        <select
          value={current.manufacturer}
          onChange={(e) => applyFilter("manufacturer", e.target.value)}
          aria-label="Manufacturer"
          className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">Any</option>
          {manufacturerOptions.map((manufacturer) => (
            <option key={manufacturer} value={manufacturer}>
              {manufacturer}
            </option>
          ))}
        </select>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Exterior color
        </legend>
        <select
          value={current.color}
          onChange={(e) => applyFilter("color", e.target.value)}
          aria-label="Exterior color"
          className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
        >
          <option value="">Any</option>
          {exteriorColorOptions.map((color) => (
            <option key={color} value={color}>
              {color}
            </option>
          ))}
        </select>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Year
        </legend>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            placeholder="From"
            value={current.yearMin}
            onChange={(e) => applyFilter("yearMin", e.target.value)}
            aria-label="Minimum year"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
          <input
            type="number"
            placeholder="To"
            value={current.yearMax}
            onChange={(e) => applyFilter("yearMax", e.target.value)}
            aria-label="Maximum year"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Price (BRL)
        </legend>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            placeholder="Min"
            value={current.priceMin}
            onChange={(e) => applyFilter("priceMin", e.target.value)}
            aria-label="Minimum price"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
          <input
            type="number"
            placeholder="Max"
            value={current.priceMax}
            onChange={(e) => applyFilter("priceMax", e.target.value)}
            aria-label="Maximum price"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
          Kilometers
        </legend>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            placeholder="Min"
            value={current.kmMin}
            onChange={(e) => applyFilter("kmMin", e.target.value)}
            aria-label="Minimum kilometers"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
          <input
            type="number"
            placeholder="Max"
            value={current.kmMax}
            onChange={(e) => applyFilter("kmMax", e.target.value)}
            aria-label="Maximum kilometers"
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          />
        </div>
      </fieldset>
    </div>
  );

  return (
    <>
      <aside aria-label="Search filters" className="hidden w-64 shrink-0 lg:block">
        <div className="sticky top-20 rounded-xl border border-border bg-card p-5">
          {panelContent}
        </div>
      </aside>

      {isFilterPanelOpen && (
        <>
          <div
            className="fixed inset-0 z-40 bg-foreground/20 backdrop-blur-sm lg:hidden"
            aria-hidden="true"
            onClick={closeFilterPanel}
          />
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Search filters"
            className="fixed inset-y-0 left-0 z-50 w-[320px] overflow-y-auto border-r border-border bg-card p-6 shadow-xl lg:hidden"
          >
            <div className="mb-4 flex items-center justify-between">
              <p className="font-display text-lg font-semibold">Filters</p>
              <button
                type="button"
                onClick={closeFilterPanel}
                aria-label="Close filters"
                className="flex h-8 w-8 items-center justify-center rounded-[var(--radius-sm)] text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            {panelContent}
          </div>
        </>
      )}
    </>
  );
}
