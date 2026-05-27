// app/(admin)/admin/page.tsx
// Admin inventory management page.
// Source: tasks.md T069; spec.md US7 SC1-3; data-model.md §6

import type { Metadata } from "next";
import { getAdminInventory } from "@/lib/api/admin";
import { AdminInventoryClient } from "./AdminInventoryClient";

export const metadata: Metadata = {
  title: "Inventário | Admin | Aurelio Motors",
};

export default async function AdminInventoryPage() {
  const response = await getAdminInventory();
  const cars = response.data;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-8 font-display text-2xl font-bold text-foreground sm:text-3xl">
        Painel de inventário
      </h1>
      <AdminInventoryClient initialCars={cars} />
    </div>
  );
}
