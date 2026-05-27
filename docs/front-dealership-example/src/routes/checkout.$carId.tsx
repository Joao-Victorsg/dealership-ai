import { createFileRoute, Link, notFound, redirect, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Loader2, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CategoryBadge, ConditionBadge, TypeBadge } from "@/components/car-badges";
import { CarPlaceholder } from "@/components/car-placeholder";
import { findCar } from "@/lib/mock-data";
import { findCarFromStore, recordSale, setCarStatus } from "@/lib/car-store";
import { computeTotal, formatBRL, formatCEP, formatCPF, TAX_LABEL, TAX_RATE } from "@/lib/format";
import { getAuthSnapshot, useAuth } from "@/lib/mock-auth";

export const Route = createFileRoute("/checkout/$carId")({
  beforeLoad: ({ location, params }) => {
    if (!getAuthSnapshot().isAuthenticated) {
      throw redirect({ to: "/login", search: { redirect: location.pathname } });
    }
    const car = findCarFromStore(params.carId) ?? findCar(params.carId);
    if (!car) throw notFound();
  },
  loader: ({ params }) => ({ car: (findCarFromStore(params.carId) ?? findCar(params.carId))! }),
  head: ({ loaderData }) => ({
    meta: [
      {
        title: loaderData
          ? `Confirm purchase — ${loaderData.car.manufacturer} ${loaderData.car.model}`
          : "Confirm purchase — Aurelio Motors",
      },
    ],
  }),
  component: CheckoutPage,
});

function CheckoutPage() {
  const { car } = Route.useLoaderData();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);

  if (!user) return null;

  const tax = car.value * TAX_RATE;
  const total = computeTotal(car.value);

  const buyerName = `${user.firstName} ${user.lastName}`;
  async function confirm() {
    setSubmitting(true);
    await new Promise((r) => setTimeout(r, 900));
    recordSale({
      id: `s-${Date.now()}`,
      carId: car.id,
      manufacturer: car.manufacturer,
      model: car.model,
      year: car.year,
      saleValue: total,
      netValue: car.value,
      date: new Date().toISOString().slice(0, 10),
      buyer: buyerName,
    });
    setCarStatus(car.id, 2);
    navigate({ to: "/checkout/success" });
  }

  return (
    <div className="mx-auto w-full max-w-[1100px] px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Checkout</p>
        <h1 className="mt-2 font-display">Confirm your purchase.</h1>
        <p className="mt-2 max-w-xl text-muted-foreground">
          Review the details below. After you confirm, we'll process your order and email
          your invoice — no further steps required from you.
        </p>
      </header>

      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <section className="rounded-xl border border-border bg-card p-6">
            <h2 className="mb-4 font-display text-lg">Vehicle</h2>
            <div className="grid gap-5 sm:grid-cols-[200px_1fr]">
              <div className="aspect-[16/10] overflow-hidden rounded-md border border-border bg-surface">
                <CarPlaceholder car={car} className="h-full w-full" />
              </div>
              <div>
                <div className="font-display text-xl">
                  {car.manufacturer} {car.model}{" "}
                  <span className="text-muted-foreground tabular">{car.year}</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  <ConditionBadge isNew={car.isNew} />
                  <TypeBadge type={car.type} />
                  <CategoryBadge category={car.category} />
                </div>
                <dl className="mt-4 grid grid-cols-2 gap-x-6 gap-y-1.5 text-sm">
                  <Detail label="Exterior">{car.externalColor}</Detail>
                  <Detail label="Interior">{car.internalColor}</Detail>
                  <Detail label="Optional">
                    {car.optionalItems.length === 0 ? "None" : car.optionalItems.join(", ")}
                  </Detail>
                </dl>
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card p-6">
            <h2 className="mb-4 font-display text-lg">Buyer</h2>
            <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              <Detail label="Name">{user.firstName} {user.lastName}</Detail>
              <Detail label="CPF" mono>{formatCPF(user.cpf)}</Detail>
              <Detail label="Email">{user.email}</Detail>
              <Detail label="CEP" mono>{formatCEP(user.postCode)}</Detail>
              <Detail label="Address">
                {user.resolvedStreet}, {user.streetNumber} — {user.resolvedNeighborhood},{" "}
                {user.resolvedCity}/{user.resolvedState}
              </Detail>
            </dl>
          </section>
        </div>

        <aside>
          <div className="sticky top-20 rounded-xl border border-border bg-card p-6">
            <h2 className="font-display text-lg">Order summary</h2>
            <dl className="mt-5 space-y-3 text-sm">
              <Row label="Car value">{formatBRL(car.value)}</Row>
              <Row label={`Tax (${TAX_LABEL})`}>
                <span className="text-muted-foreground tabular">+ {formatBRL(tax)}</span>
              </Row>
              <div className="my-2 h-px bg-border" />
              <Row label="Total" emphasize>
                <span className="font-display text-xl tabular text-primary">{formatBRL(total, { full: true })}</span>
              </Row>
            </dl>
            <p className="mt-3 text-xs text-muted-foreground">
              A {TAX_LABEL} transaction tax is applied to the car value.
            </p>

            <Button
              size="lg"
              className="mt-5 w-full"
              onClick={confirm}
              aria-busy={submitting || undefined}
            >
              {submitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" /> Processing…
                </>
              ) : (
                "Confirm purchase"
              )}
            </Button>

            <Button asChild variant="ghost" className="mt-2 w-full" disabled={submitting}>
              <Link to="/inventory/$carId" params={{ carId: car.id }}>Cancel</Link>
            </Button>

            <div className="mt-5 flex items-start gap-2 text-xs text-muted-foreground">
              <ShieldCheck className="mt-0.5 size-4 text-primary" aria-hidden />
              Secure checkout. Your invoice is sent to your email automatically after confirmation.
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Detail({ label, children, mono }: { label: string; children: React.ReactNode; mono?: boolean }) {
  return (
    <div>
      <dt className="text-[10px] uppercase tracking-[0.16em] text-muted-foreground">{label}</dt>
      <dd className={mono ? "tabular" : ""}>{children}</dd>
    </div>
  );
}

function Row({ label, children, emphasize }: { label: string; children: React.ReactNode; emphasize?: boolean }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className={emphasize ? "font-medium text-foreground" : "text-muted-foreground"}>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
