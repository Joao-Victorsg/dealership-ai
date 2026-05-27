// app/(marketing)/inventory/[carId]/loading.tsx
// Source: tasks.md T044

import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";

export default function CarDetailLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <LoadingSkeleton className="aspect-video w-full rounded-[var(--radius-lg)]" />
          <LoadingSkeleton className="h-8 w-3/4" />
          <LoadingSkeleton className="h-6 w-1/2" />
          <div className="space-y-2 pt-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <LoadingSkeleton key={i} className="h-4 w-full" />
            ))}
          </div>
        </div>
        <div>
          <LoadingSkeleton className="h-48 w-full rounded-[var(--radius-lg)]" />
        </div>
      </div>
    </div>
  );
}
