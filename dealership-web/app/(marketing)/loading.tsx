// app/(marketing)/loading.tsx
// Source: tasks.md T045

import { LoadingSkeleton } from "@/components/shared/LoadingSkeleton";

export default function HomeLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      {/* Hero skeleton */}
      <LoadingSkeleton className="mb-10 h-64 w-full rounded-[var(--radius-xl)]" />
      {/* Category tiles */}
      <div className="mb-10 grid grid-cols-2 gap-4 sm:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <LoadingSkeleton
            key={i}
            className="h-24 w-full rounded-[var(--radius-lg)]"
          />
        ))}
      </div>
      {/* Recently added grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <LoadingSkeleton
            key={i}
            className="h-48 w-full rounded-[var(--radius-lg)]"
          />
        ))}
      </div>
    </div>
  );
}
