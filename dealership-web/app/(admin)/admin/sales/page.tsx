// app/(admin)/admin/sales/page.tsx
// Admin sales report page with KPI summary and paginated table.
// Source: tasks.md T073; spec.md US8 SC1-3; data-model.md §6
// ⚠️ Endpoint shape assumed (R-08).

import type { Metadata } from "next";
import { getSalesReport } from "@/lib/api/admin";
import { KpiCards } from "@/components/admin/KpiCards";
import { SalesTable } from "@/components/admin/SalesTable";

export const metadata: Metadata = {
  title: "Vendas | Admin | Aurelio Motors",
};

interface SalesPageProps {
  searchParams: Promise<{ from?: string; to?: string; page?: string }>;
}

export default async function AdminSalesPage({
  searchParams,
}: SalesPageProps) {
  const { from, to, page: pageParam } = await searchParams;
  const page = Math.max(0, parseInt(pageParam ?? "0", 10) || 0);

  const report = await getSalesReport({
    from: from ?? undefined,
    to: to ?? undefined,
    page,
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-8 font-display text-2xl font-bold text-foreground sm:text-3xl">
        Relatório de vendas
      </h1>

      <div className="mb-8">
        <KpiCards summary={report.kpi} />
      </div>

      <SalesTable
        records={report.data}
        meta={report.meta}
        from={from}
        to={to}
      />
    </div>
  );
}
