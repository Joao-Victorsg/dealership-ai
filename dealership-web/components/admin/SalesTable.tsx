"use client";
// components/admin/SalesTable.tsx
// Paginated admin sales table with date range filter.
// Source: tasks.md T072; spec.md US8 SC2; data-model.md §6
// ⚠️ SaleRecord shape is assumed (R-08).

import { useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import type { SaleRecord } from "@/lib/api/types";
import { formatBRL, formatDate } from "@/lib/format";
import type { BffPageMeta } from "@/lib/api/types";

interface SalesTableProps {
  records: SaleRecord[];
  meta: BffPageMeta;
  from?: string;
  to?: string;
}

export function SalesTable({ records, meta, from, to }: SalesTableProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [fromInput, setFromInput] = useState(from ?? "");
  const [toInput, setToInput] = useState(to ?? "");

  function applyDateFilter() {
    const params = new URLSearchParams(searchParams.toString());
    if (fromInput) {
      params.set("from", fromInput);
    } else {
      params.delete("from");
    }
    if (toInput) {
      params.set("to", toInput);
    } else {
      params.delete("to");
    }
    params.delete("page");
    router.push(`${pathname}?${params.toString()}`);
  }

  function goToPage(page: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`${pathname}?${params.toString()}`);
  }

  return (
    <div>
      {/* Date filter */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label
            htmlFor="from"
            className="mb-1 block text-xs font-medium text-muted-foreground"
          >
            De
          </label>
          <input
            id="from"
            type="date"
            value={fromInput}
            onChange={(e) => setFromInput(e.target.value)}
            className="rounded-[var(--radius)] border border-input bg-background px-3 py-1.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>
        <div>
          <label
            htmlFor="to"
            className="mb-1 block text-xs font-medium text-muted-foreground"
          >
            Até
          </label>
          <input
            id="to"
            type="date"
            value={toInput}
            onChange={(e) => setToInput(e.target.value)}
            className="rounded-[var(--radius)] border border-input bg-background px-3 py-1.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>
        <button
          type="button"
          onClick={applyDateFilter}
          className="rounded-[var(--radius)] bg-muted px-4 py-1.5 text-sm font-medium text-foreground hover:bg-muted/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Filtrar
        </button>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-[var(--radius-lg)] border border-border">
        <table className="w-full min-w-[900px] text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="px-4 py-3 text-left font-semibold text-foreground">
                Data
              </th>
              <th className="px-4 py-3 text-left font-semibold text-foreground">
                Comprador
              </th>
              <th className="px-4 py-3 text-left font-semibold text-foreground">
                Veículo
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                Valor
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                Taxa (10%)
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                Total
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {records.length === 0 && (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  Nenhuma venda no período selecionado.
                </td>
              </tr>
            )}
            {records.map((record) => (
              <tr
                key={record.id}
                className="hover:bg-muted/30 transition-colors"
              >
                <td className="px-4 py-3 text-muted-foreground">
                  {formatDate(record.registeredAt)}
                </td>
                <td className="px-4 py-3 text-foreground">
                  {record.buyer.firstName} {record.buyer.lastName}
                </td>
                <td className="px-4 py-3 text-foreground">
                  {record.vehicle.manufacturer} {record.vehicle.model} (
                  {record.vehicle.manufacturingYear})
                </td>
                <td className="px-4 py-3 text-right tabular-nums text-foreground">
                  {formatBRL(record.listedValue)}
                </td>
                <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
                  {formatBRL(record.taxAmount)}
                </td>
                <td className="px-4 py-3 text-right tabular-nums font-semibold text-foreground">
                  {formatBRL(record.finalValue)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {meta.totalPages > 1 && (
        <nav
          aria-label="Paginação de vendas"
          className="mt-4 flex items-center justify-center gap-2"
        >
          {meta.page > 0 && (
            <button
              type="button"
              onClick={() => goToPage(meta.page - 1)}
              className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80"
            >
              Anterior
            </button>
          )}
          <span className="text-sm text-muted-foreground">
            Página {meta.page + 1} de {meta.totalPages}
          </span>
          {meta.page + 1 < meta.totalPages && (
            <button
              type="button"
              onClick={() => goToPage(meta.page + 1)}
              className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80"
            >
              Próxima
            </button>
          )}
        </nav>
      )}
    </div>
  );
}
