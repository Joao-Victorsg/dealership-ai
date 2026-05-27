"use client";
// app/(customer)/purchase/[carId]/error.tsx
// Error boundary for the purchase confirmation page.

import { ErrorDisplay } from "@/components/shared/ErrorDisplay";

interface PurchaseErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function PurchaseError({ error, reset }: PurchaseErrorProps) {
  return (
    <div className="mx-auto max-w-lg px-4 py-16 text-center sm:px-6">
      <ErrorDisplay message={error.message} />
      <button
        type="button"
        onClick={reset}
        className="mt-4 rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80"
      >
        Tentar novamente
      </button>
    </div>
  );
}
