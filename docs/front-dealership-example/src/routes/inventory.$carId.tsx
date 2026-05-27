import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { ChevronRight, ShieldCheck, Calendar } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CategoryBadge, ConditionBadge, StatusBadge, TypeBadge } from "@/components/car-badges";
import { CarPlaceholder } from "@/components/car-placeholder";
import { findCar } from "@/lib/mock-data";
import { findCarFromStore } from "@/lib/car-store";
import { formatBRL, formatDate, formatKm, TAX_LABEL } from "@/lib/format";
import { getAuthSnapshot } from "@/lib/mock-auth";

export const Route = createFileRoute("/inventory/$carId")({
  loader: ({ params }) => {
    const car = findCarFromStore(params.carId) ?? findCar(params.carId);
    if (!car) throw notFound();
    return { car };
  },
  head: ({ loaderData }) => ({
    meta: [
      {
        title: loaderData
          ? `${loaderData.car.manufacturer} ${loaderData.car.model} ${loaderData.car.year} — Aurelio Motors`
          : "Car — Aurelio Motors",
      },
      {
        name: "description",
        content: loaderData
          ? `${loaderData.car.manufacturer} ${loaderData.car.model} ${loaderData.car.year} · ${loaderData.car.externalColor} · ${formatBRL(loaderData.car.value)}.`
          : "",
      },
    ],
  }),
  notFoundComponent: () => (
    <div className="mx-auto max-w-md px-4 py-20 text-center">
      <h1 className="font-display">Car not found</h1>
      <p className="mt-2 text-muted-foreground">The vehicle you're looking for is no longer listed.</p>
      <Button asChild className="mt-6"><Link to="/inventory">Back to inventory</Link></Button>
    </div>
  ),
  component: CarDetail,
});

function CarDetail() {
  const { car } = Route.useLoaderData();
  const navigate = useNavigate();

  const buy = () => {
    if (getAuthSnapshot().isAuthenticated) {
      navigate({ to: "/checkout/$carId", params: { carId: car.id } });
    } else {
      navigate({ to: "/login", search: { redirect: `/checkout/${car.id}` } });
    }
  };

  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6">
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-1 text-sm text-muted-foreground">
        <Link to="/" className="hover:text-foreground">Home</Link>
        <ChevronRight className="size-4" aria-hidden />
        <Link to="/inventory" className="hover:text-foreground">Inventory</Link>
        <ChevronRight className="size-4" aria-hidden />
        <span className="text-foreground">{car.manufacturer} {car.model}</span>
      </nav>

      <div className="grid gap-10 lg:grid-cols-[1.2fr_0.8fr]">
        {/* Visual */}
        <div className="reveal reveal-1">
          <div className="overflow-hidden rounded-xl border border-border bg-surface">
            <div className="aspect-[16/10]">
              <CarPlaceholder car={car} className="h-full w-full" />
            </div>
          </div>
          <div className="mt-3 grid grid-cols-3 gap-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="aspect-[16/10] overflow-hidden rounded-md border border-border bg-surface opacity-70">
                <CarPlaceholder car={car} className="h-full w-full" />
              </div>
            ))}
          </div>
        </div>

        {/* Info column */}
        <div className="reveal reveal-2">
          <div className="flex flex-wrap items-center gap-2">
            <ConditionBadge isNew={car.isNew} />
            <StatusBadge status={car.status} />
          </div>
          <h1 className="mt-3 font-display">
            {car.manufacturer} <span className="text-foreground/85">{car.model}</span>
          </h1>
          <p className="mt-1 text-sm text-muted-foreground tabular">{car.year}</p>

          <div className="mt-4 flex flex-wrap gap-2">
            <TypeBadge type={car.type} />
            <CategoryBadge category={car.category} />
          </div>

          <div className="mt-8 rounded-xl border border-border bg-card p-6">
            <div className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Price</div>
            <div className="mt-1 font-display text-4xl font-semibold tabular text-primary">
              {formatBRL(car.value)}
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              A {TAX_LABEL} transaction tax will be applied at checkout.
            </p>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row">
              <Button size="lg" className="flex-1" onClick={buy} disabled={car.status !== 1}>
                {car.status === 1 ? "Buy this car" : "Not available"}
              </Button>
              <Button size="lg" variant="outline" className="flex-1">
                Save for later
              </Button>
            </div>
            <div className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
              <ShieldCheck className="size-4 text-primary" aria-hidden />
              180-point inspection · Verified history
            </div>
          </div>

          <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground">
            <Calendar className="size-3.5" aria-hidden />
            Listed on {formatDate(car.registrationDate)}
          </div>
        </div>
      </div>

      {/* Detail sections */}
      <div className="mt-14 grid gap-8 md:grid-cols-2">
        <Section title="Identity">
          <Row label="Manufacturer">{car.manufacturer}</Row>
          <Row label="Model">{car.model}</Row>
          <Row label="Year">{car.year}</Row>
          <Row label="Condition">{car.isNew ? "New" : "Pre-owned"}</Row>
        </Section>

        <Section title="Characteristics">
          <Row label="Category">{car.category}</Row>
          <Row label="Type">{car.type}</Row>
          <Row label="Kilometers">{formatKm(car.kilometers)}</Row>
        </Section>

        <Section title="Appearance">
          <Row label="Exterior color">
            <span className="inline-flex items-center gap-2">
              <ColorSwatch color={car.externalColor} /> {car.externalColor}
            </span>
          </Row>
          <Row label="Interior color">
            <span className="inline-flex items-center gap-2">
              <ColorSwatch color={car.internalColor} /> {car.internalColor}
            </span>
          </Row>
        </Section>

        <Section title="Optional items">
          {car.optionalItems.length === 0 ? (
            <p className="text-sm text-muted-foreground">No optional items.</p>
          ) : (
            <ul className="grid grid-cols-1 gap-1.5 text-sm sm:grid-cols-2">
              {car.optionalItems.map((item) => (
                <li key={item} className="flex items-center gap-2">
                  <span className="size-1.5 rounded-full bg-primary" aria-hidden /> {item}
                </li>
              ))}
            </ul>
          )}
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-border bg-card p-6">
      <h2 className="mb-4 font-display text-lg">{title}</h2>
      <dl className="space-y-2">{children}</dl>
    </section>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4 border-b border-border/60 py-2 text-sm last:border-b-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{children}</dd>
    </div>
  );
}

function ColorSwatch({ color }: { color: string }) {
  // Best-effort visual — neutral fallback if unknown
  const map: Record<string, string> = {
    white: "#f5f5f0", pearl: "#efefe8", silver: "#c8c8c8", grey: "#7a7a7a",
    granite: "#5a5a5a", nardo: "#9a9a96", black: "#1a1a1a", carbon: "#222",
    red: "#a51d2d", guards: "#9b1b1f", tornado: "#a51d2d",
    orange: "#d96a1a", blue: "#2c5fb5", ocean: "#2a5e9c", atlantic: "#2860a0",
    beige: "#dcc9a3", brown: "#5a3a22", cognac: "#8a4a25", tartan: "#6e1c1c",
  };
  const k = Object.keys(map).find((m) => color.toLowerCase().includes(m));
  const fill = k ? map[k] : "#999";
  return (
    <span
      aria-hidden
      className="inline-block size-3.5 rounded-full ring-1 ring-border"
      style={{ background: fill }}
    />
  );
}
