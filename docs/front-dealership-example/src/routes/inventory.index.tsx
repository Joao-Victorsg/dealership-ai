import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { SlidersHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { CarCard } from "@/components/car-card";
import {
  ActiveFilters,
  applyFilters,
  DEFAULT_FILTERS,
  FilterPanel,
  type Filters,
} from "@/components/car-filters";
import { EmptyState } from "@/components/empty-state";
import { useCarStore } from "@/lib/car-store";

export const Route = createFileRoute("/inventory/")({
  head: () => ({
    meta: [
      { title: "Inventory — Aurelio Motors" },
      {
        name: "description",
        content: "Browse our full inventory of new and pre-owned cars. Filter by category, type, manufacturer, year, and price.",
      },
    ],
  }),
  component: InventoryPage,
});

type SortKey = "recent" | "priceAsc" | "priceDesc" | "yearDesc" | "kmAsc";

function InventoryPage() {
  const { cars } = useCarStore();
  const [filters, setFilters] = useState<Filters>(structuredClone(DEFAULT_FILTERS));
  const [sort, setSort] = useState<SortKey>("recent");
  const [mobileOpen, setMobileOpen] = useState(false);

  const results = useMemo(() => {
    const filtered = applyFilters(cars, filters);
    const sorted = [...filtered];
    switch (sort) {
      case "priceAsc": sorted.sort((a, b) => a.value - b.value); break;
      case "priceDesc": sorted.sort((a, b) => b.value - a.value); break;
      case "yearDesc": sorted.sort((a, b) => b.year - a.year); break;
      case "kmAsc": sorted.sort((a, b) => a.kilometers - b.kilometers); break;
      case "recent":
      default:
        sorted.sort((a, b) => b.registrationDate.localeCompare(a.registrationDate));
    }
    return sorted;
  }, [cars, filters, sort]);

  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Inventory</p>
        <h1 className="mt-2 font-display">Every car we have, in one place.</h1>
        <p className="mt-2 max-w-2xl text-muted-foreground">
          Filter by what matters to you. Pricing is final — what you see is what you pay,
          plus the standard {""}
          <span className="text-foreground">8% transaction tax</span> at checkout.
        </p>
      </header>

      <div className="grid gap-8 lg:grid-cols-[260px_1fr]">
        <aside className="hidden lg:block">
          <div className="sticky top-20">
            <FilterPanel filters={filters} onChange={setFilters} />
          </div>
        </aside>

        <div>
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
                <SheetTrigger asChild>
                  <Button variant="outline" size="sm" className="lg:hidden">
                    <SlidersHorizontal className="size-4" /> Filters
                  </Button>
                </SheetTrigger>
                <SheetContent side="left" className="w-[320px] overflow-y-auto">
                  <SheetHeader>
                    <SheetTitle className="font-display">Filters</SheetTitle>
                  </SheetHeader>
                  <div className="mt-6">
                    <FilterPanel filters={filters} onChange={setFilters} />
                  </div>
                </SheetContent>
              </Sheet>
              <span className="tabular text-sm text-muted-foreground">
                {results.length} {results.length === 1 ? "car" : "cars"}
              </span>
            </div>

            <label className="flex items-center gap-2 text-sm">
              <span className="text-muted-foreground">Sort</span>
              <select
                value={sort}
                onChange={(e) => setSort(e.target.value as SortKey)}
                className="h-9 rounded-md border border-input bg-background px-2 text-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="recent">Recently added</option>
                <option value="priceAsc">Price: low to high</option>
                <option value="priceDesc">Price: high to low</option>
                <option value="yearDesc">Year: newest first</option>
                <option value="kmAsc">Kilometers: lowest first</option>
              </select>
            </label>
          </div>

          <div className="mb-5">
            <ActiveFilters filters={filters} onChange={setFilters} />
          </div>

          {results.length === 0 ? (
            <EmptyState
              title="No cars match your filters"
              description="Try widening your price or year range, or clearing a filter or two."
              action={
                <Button variant="outline" onClick={() => setFilters(structuredClone(DEFAULT_FILTERS))}>
                  Clear all filters
                </Button>
              }
            />
          ) : (
            <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-5">
              {results.map((c) => (
                <CarCard key={c.id} car={c} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
