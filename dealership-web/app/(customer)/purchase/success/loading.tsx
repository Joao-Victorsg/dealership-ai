// app/(customer)/purchase/success/loading.tsx

export default function PurchaseSuccessLoading() {
  return (
    <div className="mx-auto max-w-lg animate-pulse px-4 py-20 text-center sm:px-6">
      <div className="mx-auto mb-6 h-16 w-16 rounded-full bg-muted" />
      <div className="mx-auto mb-4 h-8 w-64 rounded-[var(--radius)] bg-muted" />
      <div className="mx-auto h-5 w-80 rounded-[var(--radius)] bg-muted" />
    </div>
  );
}
