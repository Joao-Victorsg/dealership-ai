// components/inventory/InventorySkeleton.tsx
// Grid of skeleton CarCard placeholders.
// Source: tasks.md T035; research.md R-14, FR-040

import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";

const SKELETON_COUNT = 6;

function CarCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-[var(--radius-lg)] border border-border bg-card">
      {/* Image area */}
      <LoadingSkeleton className="aspect-video w-full rounded-none" />
      <div className="p-4 space-y-2">
        {/* Manufacturer + model */}
        <LoadingSkeleton className="h-5 w-3/4" />
        {/* Year + category */}
        <LoadingSkeleton className="h-4 w-1/2" />
        {/* Price */}
        <LoadingSkeleton className="h-6 w-2/5 mt-3" />
        {/* Km */}
        <LoadingSkeleton className="h-4 w-1/3" />
      </div>
    </div>
  );
}

export function InventorySkeleton() {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label="Loading vehicles"
      className="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-5"
    >
      {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
        <CarCardSkeleton key={i} />
      ))}
    </div>
  );
}
