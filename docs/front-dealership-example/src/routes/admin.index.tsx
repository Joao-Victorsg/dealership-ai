import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import {
  Pencil,
  Plus,
  Search,
  Trash2,
  TrendingUp,
  Wallet,
  CheckCircle2,
  PackageCheck,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { CarPlaceholder } from "@/components/car-placeholder";
import { ConditionBadge, StatusBadge, TypeBadge } from "@/components/car-badges";
import { VehicleForm } from "@/components/admin/vehicle-form";
import { deleteCar, useCarStore } from "@/lib/car-store";
import { type Car } from "@/lib/mock-data";
import { formatBRL, formatKm } from "@/lib/format";
import { toast } from "sonner";

export const Route = createFileRoute("/admin/")({
  head: () => ({
    meta: [{ title: "Inventory · Admin — Aurelio Motors" }],
  }),
  component: AdminInventoryPage,
});

function AdminInventoryPage() {
  const { cars, sales } = useCarStore();
  const [query, setQuery] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Car | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Car | null>(null);

  const totalValue = useMemo(
    () => cars.filter((c) => c.status === 1).reduce((sum, c) => sum + c.value, 0),
    [cars],
  );
  const activeListings = useMemo(() => cars.filter((c) => c.status === 1).length, [cars]);
  const soldThisMonth = useMemo(() => {
    const now = new Date();
    return sales.filter((s) => {
      const d = new Date(s.date);
      return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
    }).length;
  }, [sales]);
  const monthRevenue = useMemo(() => {
    const now = new Date();
    return sales
      .filter((s) => {
        const d = new Date(s.date);
        return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
      })
      .reduce((sum, s) => sum + s.saleValue, 0);
  }, [sales]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return cars;
    return cars.filter(
      (c) =>
        c.manufacturer.toLowerCase().includes(q) ||
        c.model.toLowerCase().includes(q) ||
        c.externalColor.toLowerCase().includes(q),
    );
  }, [cars, query]);

  function openCreate() {
    setEditing(null);
    setFormOpen(true);
  }
  function openEdit(car: Car) {
    setEditing(car);
    setFormOpen(true);
  }
  function performDelete() {
    if (!confirmDelete) return;
    deleteCar(confirmDelete.id);
    toast.success(`${confirmDelete.manufacturer} ${confirmDelete.model} removed.`);
    setConfirmDelete(null);
  }

  return (
    <div className="space-y-8">
      {/* KPIs */}
      <section
        aria-label="Inventory key metrics"
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        <Kpi
          icon={<Wallet className="size-4" aria-hidden />}
          label="Total inventory value"
          value={formatBRL(totalValue)}
          hint={`${activeListings} active vehicle${activeListings === 1 ? "" : "s"}`}
        />
        <Kpi
          icon={<PackageCheck className="size-4" aria-hidden />}
          label="Active listings"
          value={String(activeListings)}
          hint="Currently available for sale"
        />
        <Kpi
          icon={<CheckCircle2 className="size-4" aria-hidden />}
          label="Sold this month"
          value={String(soldThisMonth)}
          hint={monthName()}
        />
        <Kpi
          icon={<TrendingUp className="size-4" aria-hidden />}
          label="Revenue (this month)"
          value={formatBRL(monthRevenue)}
          hint="Includes 8% transaction tax"
        />
      </section>

      {/* Toolbar */}
      <section
        aria-label="Inventory tools"
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card p-4"
      >
        <div className="relative w-full max-w-sm">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden
          />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by manufacturer, model, or color"
            aria-label="Search inventory"
            className="h-10 w-full rounded-md border border-input bg-background pl-9 pr-3 text-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>
        <Button onClick={openCreate}>
          <Plus className="size-4" /> Add vehicle
        </Button>
      </section>

      {/* Table */}
      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-surface text-left text-xs uppercase tracking-[0.14em] text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Vehicle</th>
                <th className="px-4 py-3 font-medium">Year</th>
                <th className="px-4 py-3 font-medium">Kilometers</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Price</th>
                <th className="px-4 py-3 text-right font-medium">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-sm text-muted-foreground">
                    No vehicles match your search.
                  </td>
                </tr>
              ) : (
                filtered.map((car) => (
                  <tr key={car.id} className="border-b border-border/60 last:border-b-0">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="size-14 shrink-0 overflow-hidden rounded-md border border-border bg-surface">
                          <CarPlaceholder car={car} className="h-full w-full" />
                        </div>
                        <div className="min-w-0">
                          <div className="font-medium">
                            {car.manufacturer}{" "}
                            <span className="text-foreground/80">{car.model}</span>
                          </div>
                          <div className="mt-1 flex flex-wrap items-center gap-1.5">
                            <ConditionBadge isNew={car.isNew} />
                            <TypeBadge type={car.type} />
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 tabular text-muted-foreground">{car.year}</td>
                    <td className="px-4 py-3 tabular text-muted-foreground">
                      {formatKm(car.kilometers)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={car.status} />
                    </td>
                    <td className="px-4 py-3 text-right tabular font-medium text-primary">
                      {formatBRL(car.value)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="inline-flex gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Edit ${car.manufacturer} ${car.model}`}
                          onClick={() => openEdit(car)}
                        >
                          <Pencil className="size-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Delete ${car.manufacturer} ${car.model}`}
                          onClick={() => setConfirmDelete(car)}
                        >
                          <Trash2 className="size-4 text-destructive" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <VehicleForm open={formOpen} onOpenChange={setFormOpen} initial={editing} />

      <AlertDialog
        open={!!confirmDelete}
        onOpenChange={(open) => !open && setConfirmDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove this vehicle?</AlertDialogTitle>
            <AlertDialogDescription>
              {confirmDelete && (
                <>
                  This will permanently remove{" "}
                  <span className="font-medium text-foreground">
                    {confirmDelete.manufacturer} {confirmDelete.model} {confirmDelete.year}
                  </span>{" "}
                  from the inventory. This action can't be undone.
                </>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={performDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Remove vehicle
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
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

function monthName() {
  return new Intl.DateTimeFormat("en-US", { month: "long", year: "numeric" }).format(new Date());
}
