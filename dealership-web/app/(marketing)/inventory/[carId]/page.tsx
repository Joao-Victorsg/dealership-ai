import Link from "next/link";
import { notFound } from "next/navigation";
import { Calendar, ChevronRight, ShieldCheck } from "lucide-react";
import type { Metadata } from "next";
import { BffError } from "@/lib/api/client";
import { getCarById } from "@/lib/api/inventory";
import { computeTotal, TAX_RATE } from "@/lib/pricing";
import { formatBRL, formatDate, formatKm } from "@/lib/format";
import { CarPlaceholder } from "@/components/shared/CarPlaceholder";
import { CategoryBadge, ConditionBadge, StatusBadge, TypeBadge } from "@/components/inventory/CarBadges";

interface CarDetailPageProps {
  params: Promise<{ carId: string }>;
}

export async function generateMetadata({
  params,
}: CarDetailPageProps): Promise<Metadata> {
  const { carId } = await params;

  try {
    const response = await getCarById(carId);
    const car = response.data;
    return {
      title: `${car.manufacturer} ${car.model} ${car.manufacturingYear} — Aurelio Motors`,
      description: `${car.manufacturer} ${car.model} ${car.manufacturingYear} · ${car.externalColor} · ${formatBRL(car.listedValue)}.`,
    };
  } catch {
    return { title: "Car — Aurelio Motors" };
  }
}

export default async function CarDetailPage({ params }: CarDetailPageProps) {
  const { carId } = await params;

  let car;
  try {
    const response = await getCarById(carId);
    car = response.data;
  } catch (error: unknown) {
    if (error instanceof BffError && error.code === "NOT_FOUND") {
      notFound();
    }
    throw error;
  }

  const taxLabel = `${(TAX_RATE * 100).toFixed(0)}%`;

  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6">
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-1 text-sm text-muted-foreground">
        <Link href="/" className="hover:text-foreground">Home</Link>
        <ChevronRight className="size-4" aria-hidden />
        <Link href="/inventory" className="hover:text-foreground">Inventory</Link>
        <ChevronRight className="size-4" aria-hidden />
        <span className="text-foreground">{car.manufacturer} {car.model}</span>
      </nav>

      <div className="grid gap-10 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="reveal reveal-1">
          <div className="overflow-hidden rounded-xl border border-border bg-surface">
            <div className="aspect-[16/10]">
              <CarPlaceholder externalColor={car.externalColor} className="h-full w-full" />
            </div>
          </div>
          <div className="mt-3 grid grid-cols-3 gap-3">
            {[0, 1, 2].map((index) => (
              <div key={index} className="aspect-[16/10] overflow-hidden rounded-md border border-border bg-surface opacity-70">
                <CarPlaceholder externalColor={car.externalColor} className="h-full w-full" />
              </div>
            ))}
          </div>
        </div>

        <div className="reveal reveal-2">
          <div className="flex flex-wrap items-center gap-2">
            <ConditionBadge isNew={car.isNew} />
            <StatusBadge status={car.status} />
          </div>
          <h1 className="mt-3 font-display">
            {car.manufacturer} <span className="text-foreground/85">{car.model}</span>
          </h1>
          <p className="mt-1 text-sm text-muted-foreground tabular">{car.manufacturingYear}</p>

          <div className="mt-4 flex flex-wrap gap-2">
            <TypeBadge type={car.type} />
            <CategoryBadge category={car.category} />
          </div>

          <div className="mt-8 rounded-xl border border-border bg-card p-6">
            <div className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Price</div>
            <div className="mt-1 font-display text-4xl font-semibold tabular text-primary">
              {formatBRL(car.listedValue)}
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              A {taxLabel} transaction tax will be applied at checkout.
            </p>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row">
              <Link
                href={`/purchase/${car.id}`}
                className={`inline-flex flex-1 items-center justify-center rounded-md px-4 py-3 text-sm font-semibold ${
                  car.status === "AVAILABLE"
                    ? "bg-primary text-primary-foreground hover:bg-primary/90"
                    : "pointer-events-none bg-muted text-muted-foreground"
                }`}
              >
                {car.status === "AVAILABLE" ? "Buy this car" : "Not available"}
              </Link>
              <button
                type="button"
                className="flex-1 rounded-md border border-border bg-card px-4 py-3 text-sm font-medium text-foreground hover:border-border-strong"
              >
                Save for later
              </button>
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

      <div className="mt-14 grid gap-8 md:grid-cols-2">
        <Section title="Identity">
          <Row label="Manufacturer">{car.manufacturer}</Row>
          <Row label="Model">{car.model}</Row>
          <Row label="Year">{car.manufacturingYear}</Row>
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

        <Section title="Purchase summary">
          <Row label="Listed value">{formatBRL(car.listedValue)}</Row>
          <Row label={`Tax (${taxLabel})`}>{formatBRL(computeTotal(car.listedValue) - car.listedValue)}</Row>
          <Row label="Final value">{formatBRL(computeTotal(car.listedValue))}</Row>
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
  const map: Record<string, string> = {
    white: "#f5f5f0", pearl: "#efefe8", silver: "#c8c8c8", grey: "#7a7a7a",
    granite: "#5a5a5a", nardo: "#9a9a96", black: "#1a1a1a", carbon: "#222",
    red: "#a51d2d", guards: "#9b1b1f", tornado: "#a51d2d",
    orange: "#d96a1a", blue: "#2c5fb5", ocean: "#2a5e9c", atlantic: "#2860a0",
    beige: "#dcc9a3", brown: "#5a3a22", cognac: "#8a4a25", tartan: "#6e1c1c",
  };
  const key = Object.keys(map).find((token) => color.toLowerCase().includes(token));
  const fill = key ? map[key] : "#999";
  return (
    <span
      aria-hidden
      className="inline-block size-3.5 rounded-full ring-1 ring-border"
      style={{ background: fill }}
    />
  );
}
