// components/account/PurchaseHistoryCard.tsx
// Renders a single purchase record from the customer's history.
// Source: tasks.md T064; spec.md US6 SC1-2; research.md R-07
//
// NOTE: vehicle snapshot is partial per BFF contract (R-07).
// internalColor, type, optionalItems, imageKey are absent from the snapshot.

import type { Purchase } from "@/lib/api/types";
import { CategoryBadge } from "@/components/inventory/CarBadges";
import { CarPlaceholder } from "@/components/shared/CarPlaceholder";
import { formatBRL, formatDate } from "@/lib/format";
import { computeTax, computeTotal } from "@/lib/pricing";

interface PurchaseHistoryCardProps {
  purchase: Purchase;
}

export function PurchaseHistoryCard({ purchase }: PurchaseHistoryCardProps) {
  const { vehicle, registeredAt } = purchase;
  const tax = computeTax(vehicle.listedValue);
  const total = computeTotal(vehicle.listedValue);

  return (
    <article className="grid gap-4 rounded-xl border border-border bg-card p-4 sm:grid-cols-[180px_1fr_auto] sm:items-center">
      <div className="aspect-[16/10] overflow-hidden rounded-md border border-border bg-surface sm:aspect-auto sm:h-28">
        <CarPlaceholder
          externalColor={vehicle.externalColor}
          className="h-full w-full"
        />
      </div>

      <div>
        <div className="flex flex-wrap items-baseline gap-x-2">
          <h3 className="font-display text-lg leading-tight text-foreground">
            {vehicle.manufacturer} {vehicle.model}
          </h3>
          <span className="tabular-nums text-sm text-muted-foreground">
            {vehicle.manufacturingYear}
          </span>
        </div>

        <div className="mt-2 flex flex-wrap gap-1.5">
          <CategoryBadge category={vehicle.category} />
          <span className="inline-flex items-center gap-1.5 rounded-md border border-green-700/20 bg-green-700/5 px-2 py-1 text-xs font-medium text-green-800 dark:text-green-300">
            Concluída
          </span>
        </div>

        <div className="mt-2 space-y-1 text-xs text-muted-foreground">
          <p>VIN: {vehicle.vin}</p>
          <p>
            {vehicle.externalColor} • Compra em {formatDate(registeredAt)}
          </p>
        </div>
      </div>

      <div className="rounded-md border border-border bg-surface px-4 py-3 text-right sm:min-w-56">
        <div className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground">
          Total pago
        </div>
        <div className="font-display text-2xl tabular-nums text-primary">
          {formatBRL(total)}
        </div>
        <div className="mt-1 text-[11px] text-muted-foreground">
          Veículo {formatBRL(vehicle.listedValue)} + IOF/taxas {formatBRL(tax)}
        </div>
      </div>
    </article>
  );
}
