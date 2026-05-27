import { createFileRoute } from "@tanstack/react-router";
import { useMemo } from "react";
import { CalendarRange, ReceiptText, TrendingUp, Wallet } from "lucide-react";
import { useCarStore } from "@/lib/car-store";
import { formatBRL, formatDate } from "@/lib/format";

export const Route = createFileRoute("/admin/sales")({
  head: () => ({
    meta: [{ title: "Sales reports · Admin — Aurelio Motors" }],
  }),
  component: SalesReportsPage,
});

function SalesReportsPage() {
  const { sales } = useCarStore();

  const totalRevenue = sales.reduce((sum, s) => sum + s.saleValue, 0);
  const totalTax = sales.reduce((sum, s) => sum + (s.saleValue - s.netValue), 0);
  const avgTicket = sales.length ? totalRevenue / sales.length : 0;

  const byMonth = useMemo(() => {
    const map = new Map<string, { label: string; count: number; revenue: number }>();
    for (let i = 5; i >= 0; i--) {
      const d = new Date();
      d.setDate(1);
      d.setMonth(d.getMonth() - i);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
      const label = new Intl.DateTimeFormat("en-US", { month: "short" }).format(d);
      map.set(key, { label, count: 0, revenue: 0 });
    }
    for (const s of sales) {
      const d = new Date(s.date);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
      const slot = map.get(key);
      if (slot) {
        slot.count += 1;
        slot.revenue += s.saleValue;
      }
    }
    return Array.from(map.values());
  }, [sales]);

  const maxRevenue = Math.max(1, ...byMonth.map((m) => m.revenue));

  const recent = [...sales].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 10);

  return (
    <div className="space-y-8">
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Kpi
          icon={<Wallet className="size-4" aria-hidden />}
          label="Total revenue"
          value={formatBRL(totalRevenue)}
          hint={`${sales.length} sale${sales.length === 1 ? "" : "s"} all-time`}
        />
        <Kpi
          icon={<TrendingUp className="size-4" aria-hidden />}
          label="Average ticket"
          value={formatBRL(avgTicket)}
          hint="Includes transaction tax"
        />
        <Kpi
          icon={<ReceiptText className="size-4" aria-hidden />}
          label="Tax collected"
          value={formatBRL(totalTax)}
          hint="8% of car value"
        />
      </section>

      <section className="rounded-xl border border-border bg-card p-6">
        <div className="flex items-center gap-2">
          <CalendarRange className="size-4 text-primary" aria-hidden />
          <h2 className="font-display text-lg">Last 6 months</h2>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          Revenue per month, including the {String.fromCharCode(0x00b7)} 8% transaction tax.
        </p>

        <div className="mt-8 grid grid-cols-6 items-end gap-3 sm:gap-6">
          {byMonth.map((m) => {
            const heightPct = (m.revenue / maxRevenue) * 100;
            return (
              <div key={m.label} className="flex flex-col items-center gap-2">
                <div
                  className="relative w-full overflow-hidden rounded-t-md bg-primary/15"
                  style={{ height: 180 }}
                >
                  <div
                    className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-primary to-primary/70 transition-all"
                    style={{ height: `${heightPct}%` }}
                    aria-hidden
                  />
                  <span className="sr-only">
                    {m.label}: {formatBRL(m.revenue)} from {m.count} sales
                  </span>
                </div>
                <div className="text-center">
                  <div className="text-xs font-medium">{m.label}</div>
                  <div className="text-[10px] uppercase tracking-[0.14em] text-muted-foreground tabular">
                    {m.count} {m.count === 1 ? "sale" : "sales"}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <div className="border-b border-border p-6">
          <h2 className="font-display text-lg">Recent sales</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            The 10 most recent transactions.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-surface text-left text-xs uppercase tracking-[0.14em] text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 font-medium">Vehicle</th>
                <th className="px-4 py-3 font-medium">Buyer</th>
                <th className="px-4 py-3 text-right font-medium">Net value</th>
                <th className="px-4 py-3 text-right font-medium">Total (incl. tax)</th>
              </tr>
            </thead>
            <tbody>
              {recent.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-12 text-center text-muted-foreground">
                    No sales recorded yet.
                  </td>
                </tr>
              ) : (
                recent.map((s) => (
                  <tr key={s.id} className="border-b border-border/60 last:border-b-0">
                    <td className="px-4 py-3 tabular text-muted-foreground">{formatDate(s.date)}</td>
                    <td className="px-4 py-3">
                      <div className="font-medium">
                        {s.manufacturer} <span className="text-foreground/80">{s.model}</span>
                      </div>
                      <div className="text-xs text-muted-foreground tabular">{s.year}</div>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{s.buyer}</td>
                    <td className="px-4 py-3 text-right tabular text-muted-foreground">
                      {formatBRL(s.netValue)}
                    </td>
                    <td className="px-4 py-3 text-right tabular font-medium text-primary">
                      {formatBRL(s.saleValue)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function Kpi({
  icon,
  label,
  value,
  hint,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  hint?: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-muted-foreground">
        <span className="grid size-7 place-items-center rounded-md bg-primary/10 text-primary">
          {icon}
        </span>
        {label}
      </div>
      <div className="mt-3 font-display text-2xl font-semibold tabular">{value}</div>
      {hint && <div className="mt-1 text-xs text-muted-foreground">{hint}</div>}
    </div>
  );
}
