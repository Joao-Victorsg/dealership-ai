// app/(auth)/complete-registration/loading.tsx

export default function RegistrationLoading() {
  return (
    <div className="mx-auto w-full max-w-md animate-pulse space-y-4 px-4 py-8">
      <div className="h-8 w-48 rounded-[var(--radius)] bg-muted" />
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-14 rounded-[var(--radius)] bg-muted" />
        ))}
      </div>
      <div className="h-11 rounded-[var(--radius)] bg-muted" />
    </div>
  );
}
