// app/(admin)/admin/sales/loading.tsx
// Skeleton for the admin sales page.

export default function AdminSalesLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8 animate-pulse">
      <div className="mb-8 h-8 w-56 rounded-[var(--radius)] bg-muted" />
      <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-24 rounded-[var(--radius-lg)] bg-muted" />
        ))}
      </div>
      <div className="rounded-[var(--radius-lg)] border border-border">
        {[1, 2, 3, 4, 5].map((i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border px-4 py-4 last:border-0"
          >
            <div className="h-4 w-24 rounded bg-muted" />
            <div className="h-4 w-32 rounded bg-muted" />
            <div className="ml-auto h-4 w-20 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}
