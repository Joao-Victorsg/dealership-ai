"use client";
// components/admin/InventoryTable.tsx
// Sortable admin inventory table with status filter tabs and row actions.
// Source: tasks.md T066; spec.md US7 SC1; data-model.md §6
// ⚠️ Uses assumed admin contract shapes.

import { useState, useTransition } from "react";
import type { Car, CarStatus } from "@/lib/api/types";
import { formatBRL, formatKm } from "@/lib/format";

type StatusTab = "ALL" | CarStatus;

interface InventoryTableProps {
  cars: Car[];
  onEdit: (car: Car) => void;
  onStatusChange: (carId: string, newStatus: CarStatus) => Promise<void>;
}

const STATUS_LABELS: Record<CarStatus, string> = {
  AVAILABLE: "Disponível",
  SOLD: "Vendido",
  UNAVAILABLE: "Indisponível",
};

const STATUS_COLORS: Record<CarStatus, string> = {
  AVAILABLE:
    "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300",
  SOLD: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300",
  UNAVAILABLE:
    "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300",
};

const TABS: Array<{ value: StatusTab; label: string }> = [
  { value: "ALL", label: "Todos" },
  { value: "AVAILABLE", label: "Disponíveis" },
  { value: "SOLD", label: "Vendidos" },
  { value: "UNAVAILABLE", label: "Indisponíveis" },
];

export function InventoryTable({
  cars,
  onEdit,
  onStatusChange,
}: InventoryTableProps) {
  const [activeTab, setActiveTab] = useState<StatusTab>("ALL");
  const [confirmingId, setConfirmingId] = useState<string | null>(null);
  const [pendingStatus, setPendingStatus] = useState<CarStatus | null>(null);
  const [isPending, startTransition] = useTransition();

  const filtered =
    activeTab === "ALL" ? cars : cars.filter((c) => c.status === activeTab);

  function requestStatusChange(carId: string, newStatus: CarStatus) {
    setConfirmingId(carId);
    setPendingStatus(newStatus);
  }

  function cancelStatusChange() {
    setConfirmingId(null);
    setPendingStatus(null);
  }

  function confirmStatusChange() {
    if (!confirmingId || !pendingStatus) return;
    const id = confirmingId;
    const status = pendingStatus;
    setConfirmingId(null);
    setPendingStatus(null);
    startTransition(async () => {
      await onStatusChange(id, status);
    });
  }

  return (
    <div>
      {/* Status tabs */}
      <div
        role="tablist"
        aria-label="Filtrar por status"
        className="mb-4 flex gap-1 border-b border-border"
      >
        {TABS.map((tab) => (
          <button
            key={tab.value}
            role="tab"
            aria-selected={activeTab === tab.value}
            type="button"
            onClick={() => setActiveTab(tab.value)}
            className="px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring data-[selected=true]:border-b-2 data-[selected=true]:border-primary data-[selected=true]:text-foreground"
            data-selected={activeTab === tab.value}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Confirmation dialog */}
      {confirmingId && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
        >
          <div className="w-full max-w-sm rounded-[var(--radius-lg)] bg-card p-6 shadow-xl">
            <h2
              id="confirm-dialog-title"
              className="mb-3 text-base font-semibold text-foreground"
            >
              Confirmar alteração de status
            </h2>
            <p className="mb-5 text-sm text-muted-foreground">
              Tem certeza que deseja alterar o status para{" "}
              <strong>
                {pendingStatus ? STATUS_LABELS[pendingStatus] : ""}
              </strong>
              ?
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={cancelStatusChange}
                className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={confirmStatusChange}
                disabled={isPending}
                className="rounded-[var(--radius)] bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto rounded-[var(--radius-lg)] border border-border">
        <table className="w-full min-w-[800px] text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="px-4 py-3 text-left font-semibold text-foreground">
                Veículo
              </th>
              <th className="px-4 py-3 text-left font-semibold text-foreground">
                Ano
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                Valor
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                KM
              </th>
              <th className="px-4 py-3 text-center font-semibold text-foreground">
                Status
              </th>
              <th className="px-4 py-3 text-right font-semibold text-foreground">
                Ações
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {filtered.length === 0 && (
              <tr>
                <td
                  colSpan={6}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  Nenhum veículo encontrado.
                </td>
              </tr>
            )}
            {filtered.map((car) => (
              <tr key={car.id} className="hover:bg-muted/30 transition-colors">
                <td className="px-4 py-3 font-medium text-foreground">
                  {car.manufacturer} {car.model}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {car.manufacturingYear}
                </td>
                <td className="px-4 py-3 text-right tabular-nums text-foreground">
                  {formatBRL(car.listedValue)}
                </td>
                <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
                  {formatKm(car.kilometers)}
                </td>
                <td className="px-4 py-3 text-center">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[car.status]}`}
                  >
                    {STATUS_LABELS[car.status]}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => onEdit(car)}
                      className="rounded-[var(--radius-sm)] px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      Editar
                    </button>
                    {car.status !== "SOLD" && (
                      <button
                        type="button"
                        onClick={() =>
                          requestStatusChange(
                            car.id,
                            car.status === "AVAILABLE"
                              ? "UNAVAILABLE"
                              : "AVAILABLE"
                          )
                        }
                        className="rounded-[var(--radius-sm)] px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      >
                        {car.status === "AVAILABLE"
                          ? "Indisponibilizar"
                          : "Disponibilizar"}
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
