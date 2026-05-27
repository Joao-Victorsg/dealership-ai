import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CARS, type Car, type CarCategory, type CarType } from "@/lib/mock-data";
import { formatBRL } from "@/lib/format";

export interface Filters {
  categories: Set<CarCategory>;
  types: Set<CarType>;
  conditions: Set<"new" | "used">;
  manufacturers: Set<string>;
  minYear: number;
  maxYear: number;
  minPrice: number;
  maxPrice: number;
}

export const ALL_CATEGORIES: CarCategory[] = ["SUV", "Sedan", "Sport", "Hatch", "Pick-up"];
export const ALL_TYPES: CarType[] = ["Electric", "Combustion"];
export const MANUFACTURERS = Array.from(new Set(CARS.map((c) => c.manufacturer))).sort();

export const DEFAULT_FILTERS: Filters = {
  categories: new Set(),
  types: new Set(),
  conditions: new Set(),
  manufacturers: new Set(),
  minYear: 2018,
  maxYear: 2025,
  minPrice: 0,
  maxPrice: 1000000,
};

export function applyFilters(cars: Car[], f: Filters) {
  return cars.filter((c) => {
    if (f.categories.size && !f.categories.has(c.category)) return false;
    if (f.types.size && !f.types.has(c.type)) return false;
    if (f.conditions.size) {
      if (c.isNew && !f.conditions.has("new")) return false;
      if (!c.isNew && !f.conditions.has("used")) return false;
    }
    if (f.manufacturers.size && !f.manufacturers.has(c.manufacturer)) return false;
    if (c.year < f.minYear || c.year > f.maxYear) return false;
    if (c.value < f.minPrice || c.value > f.maxPrice) return false;
    return true;
  });
}

interface FilterPanelProps {
  filters: Filters;
  onChange: (f: Filters) => void;
}

export function FilterPanel({ filters, onChange }: FilterPanelProps) {
  const update = (patch: Partial<Filters>) => onChange({ ...filters, ...patch });

  const toggle = <T,>(set: Set<T>, value: T): Set<T> => {
    const next = new Set(set);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    return next;
  };

  return (
    <div className="space-y-7">
      <Group title="Condition">
        <CheckRow
          label="New"
          checked={filters.conditions.has("new")}
          onChange={() => update({ conditions: toggle(filters.conditions, "new") })}
        />
        <CheckRow
          label="Pre-owned"
          checked={filters.conditions.has("used")}
          onChange={() => update({ conditions: toggle(filters.conditions, "used") })}
        />
      </Group>

      <Group title="Category">
        {ALL_CATEGORIES.map((c) => (
          <CheckRow
            key={c}
            label={c}
            checked={filters.categories.has(c)}
            onChange={() => update({ categories: toggle(filters.categories, c) })}
          />
        ))}
      </Group>

      <Group title="Type">
        {ALL_TYPES.map((t) => (
          <CheckRow
            key={t}
            label={t}
            checked={filters.types.has(t)}
            onChange={() => update({ types: toggle(filters.types, t) })}
          />
        ))}
      </Group>

      <Group title="Manufacturer">
        <div className="max-h-44 overflow-y-auto pr-1">
          {MANUFACTURERS.map((m) => (
            <CheckRow
              key={m}
              label={m}
              checked={filters.manufacturers.has(m)}
              onChange={() => update({ manufacturers: toggle(filters.manufacturers, m) })}
            />
          ))}
        </div>
      </Group>

      <Group title="Year">
        <RangeRow>
          <NumberInput
            label="From"
            value={filters.minYear}
            min={2010}
            max={filters.maxYear}
            onChange={(v) => update({ minYear: v })}
          />
          <NumberInput
            label="To"
            value={filters.maxYear}
            min={filters.minYear}
            max={2026}
            onChange={(v) => update({ maxYear: v })}
          />
        </RangeRow>
      </Group>

      <Group title="Price (BRL)">
        <RangeRow>
          <NumberInput
            label="Min"
            value={filters.minPrice}
            min={0}
            max={filters.maxPrice}
            step={10000}
            onChange={(v) => update({ minPrice: v })}
          />
          <NumberInput
            label="Max"
            value={filters.maxPrice}
            min={filters.minPrice}
            max={2000000}
            step={10000}
            onChange={(v) => update({ maxPrice: v })}
          />
        </RangeRow>
        <p className="mt-1 text-xs text-muted-foreground tabular">
          {formatBRL(filters.minPrice)} – {formatBRL(filters.maxPrice)}
        </p>
      </Group>

      <Button
        variant="outline"
        className="w-full"
        onClick={() => onChange(structuredClone(DEFAULT_FILTERS))}
      >
        Reset all filters
      </Button>
    </div>
  );
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <fieldset>
      <legend className="mb-2 text-xs font-semibold uppercase tracking-[0.16em] text-foreground">
        {title}
      </legend>
      <div className="flex flex-col gap-1">{children}</div>
    </fieldset>
  );
}

function CheckRow({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: () => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2 rounded-md px-1 py-1.5 text-sm hover:bg-muted">
      <input
        type="checkbox"
        checked={checked}
        onChange={onChange}
        className="size-4 rounded border-input text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
      <span>{label}</span>
    </label>
  );
}

function RangeRow({ children }: { children: React.ReactNode }) {
  return <div className="grid grid-cols-2 gap-2">{children}</div>;
}

function NumberInput({
  label,
  value,
  min,
  max,
  step = 1,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  onChange: (v: number) => void;
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-[10px] uppercase tracking-[0.14em] text-muted-foreground">{label}</span>
      <input
        type="number"
        value={value}
        min={min}
        max={max}
        step={step}
        onChange={(e) => onChange(Number(e.target.value) || 0)}
        className="h-9 rounded-md border border-input bg-background px-2 text-sm tabular focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
    </label>
  );
}

export function ActiveFilters({
  filters,
  onChange,
}: {
  filters: Filters;
  onChange: (f: Filters) => void;
}) {
  const chips: { key: string; label: string; remove: () => void }[] = [];

  filters.categories.forEach((c) =>
    chips.push({
      key: `cat-${c}`,
      label: c,
      remove: () => {
        const n = new Set(filters.categories);
        n.delete(c);
        onChange({ ...filters, categories: n });
      },
    }),
  );
  filters.types.forEach((t) =>
    chips.push({
      key: `type-${t}`,
      label: t,
      remove: () => {
        const n = new Set(filters.types);
        n.delete(t);
        onChange({ ...filters, types: n });
      },
    }),
  );
  filters.conditions.forEach((c) =>
    chips.push({
      key: `cond-${c}`,
      label: c === "new" ? "New" : "Pre-owned",
      remove: () => {
        const n = new Set(filters.conditions);
        n.delete(c);
        onChange({ ...filters, conditions: n });
      },
    }),
  );
  filters.manufacturers.forEach((m) =>
    chips.push({
      key: `mfr-${m}`,
      label: m,
      remove: () => {
        const n = new Set(filters.manufacturers);
        n.delete(m);
        onChange({ ...filters, manufacturers: n });
      },
    }),
  );

  if (!chips.length) return null;
  return (
    <div className="flex flex-wrap items-center gap-2">
      {chips.map((c) => (
        <button
          key={c.key}
          onClick={c.remove}
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 text-xs hover:border-border-strong"
        >
          {c.label}
          <X className="size-3" aria-hidden />
          <span className="sr-only">Remove filter</span>
        </button>
      ))}
      <button
        onClick={() => onChange(structuredClone(DEFAULT_FILTERS))}
        className="text-xs text-muted-foreground underline-offset-2 hover:underline"
      >
        Clear all
      </button>
    </div>
  );
}
