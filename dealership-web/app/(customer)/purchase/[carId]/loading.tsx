// app/(customer)/purchase/[carId]/loading.tsx
// Skeleton for purchase confirmation page.

export default function PurchaseLoading() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8 animate-pulse">
      <div className="mb-8 h-8 w-64 rounded-[var(--radius)] bg-muted" />
      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <div className="h-36 rounded-[var(--radius-lg)] bg-muted" />
          <div className="h-28 rounded-[var(--radius-lg)] bg-muted" />
        </div>
        <div className="space-y-4">
          <div className="h-40 rounded-[var(--radius-lg)] bg-muted" />
          <div className="h-12 rounded-[var(--radius)] bg-muted" />
        </div>
      </div>
    </div>
  );
}
