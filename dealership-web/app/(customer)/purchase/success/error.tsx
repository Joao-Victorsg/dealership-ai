"use client";
// app/(customer)/purchase/success/error.tsx

import Link from "next/link";
import { ErrorDisplay } from "@/components/shared/ErrorDisplay";

interface PurchaseSuccessErrorProps {
  error: Error & { digest?: string };
}

export default function PurchaseSuccessError({ error }: PurchaseSuccessErrorProps) {
  return (
    <div className="mx-auto max-w-lg px-4 py-16 text-center sm:px-6">
      <ErrorDisplay message={error.message} requestId={error.digest} />
      <Link
        href="/inventory"
        className="mt-6 inline-block rounded-[var(--radius)] bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
      >
        Ver estoque
      </Link>
    </div>
  );
}
