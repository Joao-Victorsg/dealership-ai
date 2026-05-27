// app/(customer)/purchase/success/page.tsx
// Shown after a successful purchase confirmation.
// Source: tasks.md T058; spec.md US4 SC3

import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Compra realizada! | Aurelio Motors",
};

export default function PurchaseSuccessPage() {
  return (
    <div className="mx-auto max-w-lg px-4 py-20 text-center sm:px-6">
      {/* Success icon */}
      <div
        aria-hidden="true"
        className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30"
      >
        <svg
          className="h-8 w-8 text-green-600 dark:text-green-400"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
      </div>

      <h1 className="font-display text-3xl font-bold text-foreground">
        Compra realizada!
      </h1>
      <p className="mt-3 text-muted-foreground">
        Parabéns! Sua compra foi confirmada com sucesso. Em breve entraremos em
        contato com os próximos passos.
      </p>

      <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
        <Link
          href="/account/purchases"
          className="w-full rounded-[var(--radius)] bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:w-auto"
        >
          Ver minhas compras
        </Link>
        <Link
          href="/inventory"
          className="w-full rounded-[var(--radius)] bg-muted px-6 py-2.5 text-sm font-medium text-foreground hover:bg-muted/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:w-auto"
        >
          Continuar explorando
        </Link>
      </div>
    </div>
  );
}
