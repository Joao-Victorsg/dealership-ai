// components/purchase/FinancialSummary.tsx
// Displays listed value, 10% tax, and final total for a car purchase.
// Source: tasks.md T054; spec.md US4 SC2; lib/pricing.ts

import { computeTax, computeTotal, TAX_RATE } from "@/lib/pricing";
import { formatBRL } from "@/lib/format";

interface FinancialSummaryProps {
  listedValue: number;
}

export function FinancialSummary({ listedValue }: FinancialSummaryProps) {
  const tax = computeTax(listedValue);
  const total = computeTotal(listedValue);
  const taxRatePercent = (TAX_RATE * 100).toFixed(0);

  return (
    <div className="rounded-[var(--radius-lg)] border border-border bg-card p-5">
      <h2 className="mb-4 text-base font-semibold text-foreground">
        Resumo financeiro
      </h2>

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center justify-between">
          <dt className="text-muted-foreground">Valor do veículo</dt>
          <dd className="tabular-nums text-foreground">{formatBRL(listedValue)}</dd>
        </div>

        <div className="flex items-center justify-between">
          <dt className="text-muted-foreground">
            IOF / taxas ({taxRatePercent}%)
          </dt>
          <dd className="tabular-nums text-foreground">{formatBRL(tax)}</dd>
        </div>

        <div className="my-1 border-t border-border" />

        <div className="flex items-center justify-between font-semibold">
          <dt className="text-foreground">Total</dt>
          <dd className="tabular-nums text-foreground text-base">
            {formatBRL(total)}
          </dd>
        </div>
      </dl>
    </div>
  );
}
