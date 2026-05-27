import { createFileRoute, Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { CategoryBadge, ConditionBadge, TypeBadge } from "@/components/car-badges";
import { CarPlaceholder } from "@/components/car-placeholder";
import { EmptyState } from "@/components/empty-state";
import { PURCHASES } from "@/lib/mock-data";
import { formatBRL, formatDate } from "@/lib/format";

export const Route = createFileRoute("/account/purchases")({
  head: () => ({
    meta: [
      { title: "My purchases — Aurelio Motors" },
      { name: "description", content: "See every car you've purchased through Aurelio Motors." },
    ],
  }),
  component: PurchasesPage,
});

function PurchasesPage() {
  if (PURCHASES.length === 0) {
    return (
      <EmptyState
        title="No purchases yet"
        description="When you buy a car through Aurelio, your invoices and history will live here."
        action={
          <Button asChild>
            <Link to="/inventory">Browse inventory</Link>
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      {PURCHASES.map((p) => (
        <article
          key={p.id}
          className="grid gap-4 rounded-xl border border-border bg-card p-4 sm:grid-cols-[180px_1fr_auto] sm:items-center"
        >
          <div className="aspect-[16/10] overflow-hidden rounded-md border border-border bg-surface sm:aspect-auto sm:h-28">
            <CarPlaceholder car={p.car} className="h-full w-full" />
          </div>

          <div>
            <div className="flex flex-wrap items-baseline gap-x-2">
              <h3 className="font-display text-lg leading-tight">
                {p.car.manufacturer} {p.car.model}
              </h3>
              <span className="tabular text-sm text-muted-foreground">{p.car.year}</span>
            </div>
            <div className="mt-2 flex flex-wrap gap-1.5">
              <ConditionBadge isNew={p.car.isNew} />
              <TypeBadge type={p.car.type} />
              <CategoryBadge category={p.car.category} />
            </div>
            <div className="mt-2 text-xs text-muted-foreground">
              {p.car.externalColor} · {p.car.internalColor} interior · Purchased on {formatDate(p.date)}
            </div>
          </div>

          <div className="text-right">
            <div className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground">Sale value</div>
            <div className="font-display text-2xl tabular text-primary">{formatBRL(p.saleValue)}</div>
            <div className="text-[11px] text-muted-foreground">Tax included</div>
          </div>
        </article>
      ))}
    </div>
  );
}
