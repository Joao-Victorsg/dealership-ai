// app/(customer)/account/loading.tsx
// Skeleton for the account page.

export default function AccountLoading() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8 animate-pulse">
      <div className="mb-8 h-8 w-48 rounded-[var(--radius)] bg-muted" />
      <div className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="h-14 rounded-[var(--radius)] bg-muted" />
          <div className="h-14 rounded-[var(--radius)] bg-muted" />
        </div>
        <div className="h-14 rounded-[var(--radius)] bg-muted" />
        <div className="h-14 rounded-[var(--radius)] bg-muted" />
        <div className="h-14 rounded-[var(--radius)] bg-muted" />
        <div className="h-32 rounded-[var(--radius-lg)] bg-muted" />
        <div className="h-10 w-40 rounded-[var(--radius)] bg-muted" />
      </div>
    </div>
  );
}
