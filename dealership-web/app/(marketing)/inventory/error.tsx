"use client";
// app/(marketing)/inventory/error.tsx
// Error boundary for the inventory page.
// Source: tasks.md T040

import { ErrorDisplay } from "@/components/shared/ErrorDisplay";

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function InventoryError({ error, reset }: ErrorPageProps) {
  return (
    <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
      <div className="flex flex-col items-center gap-6">
        <ErrorDisplay
          message="Failed to load inventory. Please try again."
          requestId={error.digest}
        />
        <button
          type="button"
          onClick={reset}
          className="rounded-[var(--radius)] bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Try again
        </button>
      </div>
    </div>
  );
}
