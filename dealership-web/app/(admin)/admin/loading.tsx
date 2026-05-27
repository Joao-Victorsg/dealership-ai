// app/(admin)/admin/loading.tsx
// Skeleton for the admin inventory page.

export default function AdminLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8 animate-pulse">
      <div className="mb-8 h-8 w-64 rounded-[var(--radius)] bg-muted" />
      <div className="mb-4 flex justify-between">
        <div className="h-8 w-32 rounded-[var(--radius)] bg-muted" />
        <div className="h-9 w-32 rounded-[var(--radius)] bg-muted" />
      </div>
      <div className="rounded-[var(--radius-lg)] border border-border">
        {[1, 2, 3, 4, 5].map((i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border px-4 py-4 last:border-0"
          >
            <div className="h-4 w-48 rounded bg-muted" />
            <div className="h-4 w-12 rounded bg-muted" />
            <div className="ml-auto h-4 w-24 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}
