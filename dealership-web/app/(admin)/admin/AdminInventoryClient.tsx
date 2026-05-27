"use client";
// app/(admin)/admin/AdminInventoryClient.tsx
// Client component wrapping InventoryTable and CarFormModal.
// Separates server/client boundary for the admin inventory page.

import { useState } from "react";
import dynamic from "next/dynamic";
import type { Car, CarStatus } from "@/lib/api/types";
import { InventoryTable } from "@/components/admin/InventoryTable";
import { createCarAction, updateCarAction, changeCarStatusAction } from "./actions";
import type { CarFormInput } from "./actions";

const CarFormModal = dynamic(
  () =>
    import("@/components/admin/CarFormModal").then((m) => ({
      default: m.CarFormModal,
    })),
  { ssr: false }
);

interface AdminInventoryClientProps {
  initialCars: Car[];
}

export function AdminInventoryClient({
  initialCars,
}: AdminInventoryClientProps) {
  const [editingCar, setEditingCar] = useState<Car | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  async function handleModalSubmit(values: CarFormInput) {
    setActionError(null);
    let result;
    if (editingCar) {
      result = await updateCarAction(editingCar.id, values);
    } else {
      result = await createCarAction(values);
    }
    if (result.ok) {
      setEditingCar(null);
      setIsCreating(false);
    } else {
      setActionError(result.message ?? "Erro ao salvar.");
    }
  }

  async function handleStatusChange(carId: string, newStatus: CarStatus) {
    setActionError(null);
    const result = await changeCarStatusAction(carId, newStatus);
    if (!result.ok) {
      setActionError(result.message ?? "Erro ao alterar status.");
    }
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-foreground">Inventário</h2>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-[var(--radius)] bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Novo veículo
        </button>
      </div>

      {actionError && (
        <p
          role="alert"
          className="mb-4 rounded-[var(--radius)] border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive"
        >
          {actionError}
        </p>
      )}

      <InventoryTable
        cars={initialCars}
        onEdit={(car) => setEditingCar(car)}
        onStatusChange={handleStatusChange}
      />

      {(isCreating || editingCar) && (
        <CarFormModal
          car={editingCar ?? undefined}
          onSubmit={handleModalSubmit}
          onClose={() => {
            setIsCreating(false);
            setEditingCar(null);
            setActionError(null);
          }}
        />
      )}
    </div>
  );
}
