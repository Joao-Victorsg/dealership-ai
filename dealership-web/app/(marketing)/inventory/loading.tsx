// app/(marketing)/inventory/loading.tsx
// Shown by Next.js Suspense during inventory page fetch.
// Source: tasks.md T040

import { InventorySkeleton } from "@/components/inventory/InventorySkeleton";

export default function InventoryLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 h-8 w-48 animate-pulse rounded-[var(--radius)] bg-muted" />
      <InventorySkeleton />
    </div>
  );
}
