// app/(customer)/account/purchases/page.tsx
// Customer purchase history — paginated list.
// Source: tasks.md T065; spec.md US6 SC1-3

import type { Metadata } from "next";
import Link from "next/link";
import { getPurchases } from "@/lib/api/purchases";
import { PurchaseHistoryCard } from "@/components/account/PurchaseHistoryCard";
import { EmptyState } from "@/components/shared/EmptyState";

export const metadata: Metadata = {
  title: "Minhas compras | Aurelio Motors",
};

interface PurchasesPageProps {
  searchParams: Promise<{ page?: string }>;
}

export default async function PurchasesPage({
  searchParams,
}: PurchasesPageProps) {
  const { page: pageParam } = await searchParams;
  const page = Math.max(0, parseInt(pageParam ?? "0", 10) || 0);

  const response = await getPurchases(page);
  const { data: purchases, meta } = response;

  return (
    <div>
      <header className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-foreground sm:text-3xl">
            Minhas compras
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {meta.totalElements === 1
              ? "1 compra registrada"
              : `${meta.totalElements} compras registradas`}
          </p>
        </div>
        <Link
          href="/inventory"
          className="inline-flex w-fit items-center rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm font-medium hover:border-border-strong"
        >
          Buscar veículos
        </Link>
      </header>

      {purchases.length === 0 ? (
        <EmptyState
          title="Nenhuma compra ainda"
          description="Quando você finalizar uma compra, ela aparecerá aqui."
          action={
            <Link
              href="/inventory"
              className="inline-block rounded-[var(--radius)] bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Ver veículos disponíveis
            </Link>
          }
        />
      ) : (
        <div className="space-y-5">
          {purchases.map((purchase) => (
            <PurchaseHistoryCard key={purchase.id} purchase={purchase} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {meta.totalPages > 1 && (
        <nav
          aria-label="Paginação de compras"
          className="mt-8 flex items-center justify-center gap-2"
        >
          {page > 0 && (
            <Link
              href={`/account/purchases?page=${page - 1}`}
              className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              Anterior
            </Link>
          )}
          <span className="text-sm text-muted-foreground">
            Página {page + 1} de {meta.totalPages}
          </span>
          {page + 1 < meta.totalPages && (
            <Link
              href={`/account/purchases?page=${page + 1}`}
              className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              Próxima
            </Link>
          )}
        </nav>
      )}
    </div>
  );
}
