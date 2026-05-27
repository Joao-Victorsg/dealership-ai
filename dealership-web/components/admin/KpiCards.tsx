// components/admin/KpiCards.tsx
// Admin sales KPI summary — 4 cards.
// Source: tasks.md T071; spec.md US8 SC1; data-model.md §6
// ⚠️ SalesKpiSummary shape is assumed (R-08).

import type { SalesKpiSummary } from "@/lib/api/types";
import { formatBRL } from "@/lib/format";

interface KpiCardsProps {
  summary: SalesKpiSummary;
}

interface KpiCardProps {
  label: string;
  value: string;
}

function KpiCard({ label, value }: KpiCardProps) {
  return (
    <div className="rounded-[var(--radius-lg)] border border-border bg-card p-5">
      <p className="mb-1 text-sm text-muted-foreground">{label}</p>
      <p className="tabular-nums text-2xl font-bold text-foreground">{value}</p>
    </div>
  );
}

export function KpiCards({ summary }: KpiCardsProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <KpiCard
        label="Receita total"
        value={formatBRL(summary.totalRevenue)}
      />
      <KpiCard
        label="Veículos vendidos"
        value={summary.totalCarsSold.toLocaleString("pt-BR")}
      />
      <KpiCard
        label="Ticket médio"
        value={formatBRL(summary.averageSaleValue)}
      />
      <KpiCard
        label="Vendas neste mês"
        value={summary.currentMonthCount.toLocaleString("pt-BR")}
      />
    </div>
  );
}
