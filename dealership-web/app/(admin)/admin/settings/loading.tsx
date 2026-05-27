// app/(admin)/admin/settings/loading.tsx

export default function SettingsLoading() {
  return (
    <div className="mx-auto max-w-2xl animate-pulse px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-6 h-8 w-48 rounded-[var(--radius)] bg-muted" />
      <div className="h-24 rounded-[var(--radius)] bg-muted" />
    </div>
  );
}
