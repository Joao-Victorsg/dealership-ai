import dynamic from "next/dynamic";
import Link from "next/link";
import type { Metadata } from "next";
import { Suspense } from "react";
import { getInventory, getInventoryFilterOptions } from "@/lib/api/inventory";
import { CarGrid } from "@/components/inventory/CarGrid";
import { CarCard } from "@/components/inventory/CarCard";
import { FilterChips } from "@/components/inventory/FilterChips";
import { InventoryFilterToggle } from "@/components/inventory/InventoryFilterToggle";
import { InventorySkeleton } from "@/components/inventory/InventorySkeleton";
import { InventorySortSelect } from "@/components/inventory/InventorySortSelect";
import { EmptyState } from "@/components/shared/EmptyState";
import { DEFAULT_FILTERS } from "@/lib/api/types";
import type {
  InventoryFilters,
  InventoryQueryParams,
  Car,
  CarCardProps,
} from "@/lib/api/types";

const InventoryFiltersPanel = dynamic(
  () =>
    import("@/components/inventory/InventoryFilters").then(
      (module) => module.InventoryFilters
    ),
  { loading: () => null }
);

export const metadata: Metadata = {
  title: "Inventory — Aurelio Motors",
  description: "Browse our full inventory of new and pre-owned cars. Filter by category, type, manufacturer, year, and price.",
};

interface InventoryPageProps {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}

function firstParam(
  val: string | string[] | undefined
): string {
  if (Array.isArray(val)) return val[0] ?? "";
  return val ?? "";
}

function buildFilters(
  raw: Record<string, string | string[] | undefined>
): InventoryFilters {
  return {
    q: firstParam(raw["q"]),
    category:
      (firstParam(raw["category"]) as InventoryFilters["category"]) ?? "",
    type: (firstParam(raw["type"]) as InventoryFilters["type"]) ?? "",
    condition:
      (firstParam(raw["condition"]) as InventoryFilters["condition"]) ?? "",
    manufacturer: firstParam(raw["manufacturer"]),
    yearMin: firstParam(raw["yearMin"]),
    yearMax: firstParam(raw["yearMax"]),
    priceMin: firstParam(raw["priceMin"]),
    priceMax: firstParam(raw["priceMax"]),
    color: firstParam(raw["color"]),
    kmMin: firstParam(raw["kmMin"]),
    kmMax: firstParam(raw["kmMax"]),
    sortBy:
      (firstParam(raw["sortBy"]) as InventoryFilters["sortBy"]) ||
      DEFAULT_FILTERS.sortBy,
    sortDirection:
      (firstParam(raw["sortDirection"]) as InventoryFilters["sortDirection"]) ||
      DEFAULT_FILTERS.sortDirection,
    page: Number(firstParam(raw["page"])) || 0,
  };
}

function buildQueryParams(filters: InventoryFilters): InventoryQueryParams {
  const params: InventoryQueryParams = {};
  if (filters.q) params.q = filters.q;
  if (filters.category) params.category = filters.category;
  if (filters.type) params.type = filters.type;
  if (filters.condition) params.condition = filters.condition;
  if (filters.manufacturer) params.manufacturer = filters.manufacturer;
  if (filters.yearMin) params.yearMin = Number(filters.yearMin);
  if (filters.yearMax) params.yearMax = Number(filters.yearMax);
  if (filters.priceMin) params.priceMin = Number(filters.priceMin);
  if (filters.priceMax) params.priceMax = Number(filters.priceMax);
  if (filters.color) params.color = filters.color;
  if (filters.kmMin) params.kmMin = Number(filters.kmMin);
  if (filters.kmMax) params.kmMax = Number(filters.kmMax);
  params.sortBy = filters.sortBy;
  params.sortDirection = filters.sortDirection;
  params.page = filters.page;
  return params;
}

function toQueryString(filters: InventoryFilters, overrides?: Partial<InventoryFilters>): string {
  const merged = { ...filters, ...overrides };
  const params = new URLSearchParams();

  if (merged.q) params.set("q", merged.q);
  if (merged.category) params.set("category", merged.category);
  if (merged.type) params.set("type", merged.type);
  if (merged.condition) params.set("condition", merged.condition);
  if (merged.manufacturer) params.set("manufacturer", merged.manufacturer);
  if (merged.yearMin) params.set("yearMin", merged.yearMin);
  if (merged.yearMax) params.set("yearMax", merged.yearMax);
  if (merged.priceMin) params.set("priceMin", merged.priceMin);
  if (merged.priceMax) params.set("priceMax", merged.priceMax);
  if (merged.color) params.set("color", merged.color);
  if (merged.kmMin) params.set("kmMin", merged.kmMin);
  if (merged.kmMax) params.set("kmMax", merged.kmMax);
  if (merged.sortBy) params.set("sortBy", merged.sortBy);
  if (merged.sortDirection) params.set("sortDirection", merged.sortDirection);
  if (merged.page > 0) params.set("page", String(merged.page));

  return params.toString();
}

function mapCarToCardProps(car: Car): CarCardProps {
  const cdnUrl = process.env.NEXT_PUBLIC_CDN_URL;
  return {
    id: car.id,
    manufacturer: car.manufacturer,
    model: car.model,
    manufacturingYear: car.manufacturingYear,
    category: car.category,
    isNew: car.isNew,
    type: car.type,
    externalColor: car.externalColor,
    kilometers: car.kilometers,
    listedValue: car.listedValue,
    status: car.status,
    imageUrl: car.imageKey && cdnUrl ? `${cdnUrl}/${car.imageKey}` : null,
  };
}

async function InventoryResults({
  queryParams,
  filters,
}: {
  queryParams: InventoryQueryParams;
  filters: InventoryFilters;
}) {
  const response = await getInventory(queryParams);
  const cars = response.data ?? [];
  const page = response.meta?.page ?? 0;
  const totalElements = response.meta?.totalElements ?? 0;
  const totalPages = response.meta?.totalPages ?? 0;

  return (
    <>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <InventoryFilterToggle />
          <span className="tabular text-sm text-muted-foreground">
            {totalElements.toLocaleString("en-US")} {totalElements === 1 ? "car" : "cars"}
          </span>
        </div>
        <InventorySortSelect />
      </div>

      {cars.length === 0 ? (
        <EmptyState
          title="No cars match your filters"
          description="Try widening your price or year range, or clearing a filter or two."
          action={
            <Link
              href="/inventory"
              className="inline-flex rounded-md border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:border-border-strong"
            >
              Clear all filters
            </Link>
          }
        />
      ) : (
        <CarGrid>
          {cars.map((car) => (
            <CarCard key={car.id} {...mapCarToCardProps(car)} />
          ))}
        </CarGrid>
      )}

      {totalPages > 1 && (
        <nav
          aria-label="Inventory pagination"
          className="mt-8 flex items-center justify-center gap-2"
        >
          {page > 0 && (
            <Link
              href={`/inventory?${toQueryString(filters, { page: page - 1 })}`}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:border-border-strong"
            >
              Previous
            </Link>
          )}
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          {page + 1 < totalPages && (
            <Link
              href={`/inventory?${toQueryString(filters, { page: page + 1 })}`}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm font-medium text-foreground hover:border-border-strong"
            >
              Next
            </Link>
          )}
        </nav>
      )}
    </>
  );
}

export default async function InventoryPage({
  searchParams,
}: InventoryPageProps) {
  const rawParams = await searchParams;
  const filters = buildFilters(rawParams);
  const queryParams = buildQueryParams(filters);
  const filterOptionsResponse = await getInventoryFilterOptions();
  const manufacturerOptions = filterOptionsResponse.data?.manufacturers ?? [];
  const exteriorColorOptions = filterOptionsResponse.data?.exteriorColors ?? [];

  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Inventory</p>
        <h1 className="mt-2 font-display">Every car we have, in one place.</h1>
        <p className="mt-2 max-w-2xl text-muted-foreground">
          Filter by what matters to you. Pricing is final — what you see is what you pay,
          plus the standard <span className="text-foreground">10% transaction tax</span> at checkout.
        </p>
      </header>

      <div className="grid gap-8 lg:grid-cols-[260px_1fr]">
        <Suspense fallback={null}>
          <InventoryFiltersPanel
            manufacturerOptions={manufacturerOptions}
            exteriorColorOptions={exteriorColorOptions}
          />
        </Suspense>

        <div>
          <div className="mb-5">
            <Suspense fallback={null}>
              <FilterChips filters={filters} />
            </Suspense>
          </div>
          <Suspense fallback={<InventorySkeleton />}>
            <InventoryResults queryParams={queryParams} filters={filters} />
          </Suspense>
        </div>
      </div>
    </div>
  );
}
