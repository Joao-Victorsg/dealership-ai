// app/(customer)/account/purchases/loading.tsx
// Skeleton for the purchase history page.

export default function PurchasesLoading() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8 animate-pulse">
      <div className="mb-8 h-8 w-48 rounded-[var(--radius)] bg-muted" />
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-40 rounded-[var(--radius-lg)] bg-muted"
          />
        ))}
      </div>
    </div>
  );
}
