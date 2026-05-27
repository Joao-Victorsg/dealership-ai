import Link from "next/link";
import { ArrowRight, ShieldCheck, Sparkles, Wrench } from "lucide-react";
import type { Metadata } from "next";
import { HeroSearch } from "@/components/marketing/HeroSearch";
import { CategoryTiles } from "@/components/marketing/CategoryTiles";
import { CarCard } from "@/components/inventory/CarCard";
import { getInventory } from "@/lib/api/inventory";
import type { Car, CarCardProps } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "Aurelio Motors — Find your next drive",
  description:
    "New and pre-owned cars from Brazil's most-loved manufacturers. Search, compare, and buy with confidence.",
};

function mapCarToCardProps(car: Car): CarCardProps {
  const cdnUrl = process.env.NEXT_PUBLIC_CDN_URL;
  return {
    id: car.id,
    manufacturer: car.manufacturer,
    model: car.model,
    manufacturingYear: car.manufacturingYear,
    category: car.category,
    isNew: car.isNew,
    type: car.type,
    externalColor: car.externalColor,
    kilometers: car.kilometers,
    listedValue: car.listedValue,
    status: car.status,
    imageUrl: car.imageKey && cdnUrl ? `${cdnUrl}/${car.imageKey}` : null,
  };
}

export default async function HomePage() {
  const response = await getInventory({
    sortBy: "REGISTRATION_DATE",
    sortDirection: "DESC",
    size: 6,
  });
  const featured = response.data ?? [];

  return (
    <>
      <section className="grain border-b border-border">
        <div className="mx-auto grid w-full max-w-[1280px] gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[1.1fr_0.9fr] lg:py-24">
          <div className="flex flex-col justify-center reveal reveal-1">
            <span className="inline-flex w-fit items-center gap-2 rounded-full border border-border bg-surface px-3 py-1 text-xs uppercase tracking-[0.18em] text-muted-foreground">
              <span className="size-1.5 rounded-full bg-primary" /> New & pre-owned
            </span>
            <h1 className="mt-5 font-display tracking-tight">
              Find your next drive,
              <br />
              <span className="italic text-primary">with clarity.</span>
            </h1>
            <p className="mt-5 text-base text-muted-foreground sm:text-lg">
              A complete online dealership. Browse our full inventory, compare
              specs side by side, and complete your purchase from home —
              the keys come to you.
            </p>
            <div className="mt-8">
              <HeroSearch />
            </div>
            <div className="mt-6 flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-muted-foreground">
              <Trust icon={ShieldCheck} label="Verified history" />
              <Trust icon={Sparkles} label="180-point inspection" />
              <Trust icon={Wrench} label="3-year service plan" />
            </div>
          </div>

          <div className="reveal reveal-2 hidden lg:block">
            <HeroVisual />
          </div>
        </div>
      </section>

      <section className="mx-auto w-full max-w-[1280px] px-4 py-16 sm:px-6">
        <div className="mb-8 flex items-end justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">By category</p>
            <h2 className="mt-2 font-display">Browse by what fits your life.</h2>
          </div>
          <Link href="/inventory" className="hidden text-sm text-primary hover:underline sm:inline">
            All categories →
          </Link>
        </div>
        <CategoryTiles />
      </section>

      <section className="mx-auto w-full max-w-[1280px] px-4 pb-16 sm:px-6">
        <div className="grid gap-4 md:grid-cols-2">
          <SplitCard
            kicker="New cars"
            title="Straight from the factory."
            copy="The latest models, full warranty, zero kilometers — and the satisfaction of being the first to drive it."
            cta="Browse new"
            href="/inventory?condition=NEW"
          />
          <SplitCard
            kicker="Pre-owned"
            title="Lightly driven, deeply checked."
            copy="Each pre-owned car passes our 180-point inspection. Real value, real history, no surprises."
            cta="Browse pre-owned"
            tone="muted"
            href="/inventory?condition=USED"
          />
        </div>
      </section>

      <section className="bg-surface">
        <div className="mx-auto w-full max-w-[1280px] px-4 py-16 sm:px-6">
          <div className="mb-8 flex items-end justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
                Recently added
              </p>
              <h2 className="mt-2 font-display">Featured inventory.</h2>
            </div>
            <Link
              href="/inventory"
              className="inline-flex items-center gap-2 rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:border-border-strong"
            >
              View all <ArrowRight className="size-4" aria-hidden />
            </Link>
          </div>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {featured.map((car) => (
              <CarCard key={car.id} {...mapCarToCardProps(car)} />
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function Trust({
  icon: Icon,
  label,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <Icon className="size-4 text-primary" aria-hidden /> {label}
    </span>
  );
}

function SplitCard({
  kicker,
  title,
  copy,
  cta,
  tone = "primary",
  href,
}: {
  kicker: string;
  title: string;
  copy: string;
  cta: string;
  tone?: "primary" | "muted";
  href: string;
}) {
  return (
    <Link
      href={href}
      className={`group relative flex min-h-[260px] flex-col justify-between overflow-hidden rounded-xl border border-border p-8 transition-all hover:-translate-y-0.5 hover:border-border-strong ${
        tone === "primary" ? "bg-card" : "bg-surface"
      }`}
    >
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">{kicker}</p>
        <h3 className="mt-3 font-display text-2xl">{title}</h3>
        <p className="mt-3 max-w-md text-sm text-muted-foreground">{copy}</p>
      </div>
      <span className="mt-6 inline-flex w-fit items-center gap-1.5 text-sm font-medium text-primary">
        {cta} <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" aria-hidden />
      </span>
    </Link>
  );
}

function HeroVisual() {
  return (
    <div className="relative h-full min-h-[420px] overflow-hidden rounded-2xl border border-border bg-surface">
      <div
        className="absolute inset-0"
        style={{
          background:
            "radial-gradient(120% 80% at 30% 0%, color-mix(in oklab, var(--color-primary) 18%, transparent) 0%, transparent 60%), radial-gradient(80% 60% at 100% 100%, color-mix(in oklab, var(--color-primary) 12%, transparent) 0%, transparent 70%)",
        }}
      />
      <svg
        viewBox="0 0 600 420"
        className="absolute inset-0 h-full w-full"
        preserveAspectRatio="xMidYMid slice"
        aria-hidden
      >
        <line x1="0" y1="320" x2="600" y2="320" stroke="oklch(0 0 0 / 0.08)" />
        <g fill="oklch(0.22 0.005 60)">
          <path d="M60,310 Q90,230 200,210 L380,200 Q470,200 520,250 L560,278 Q575,285 575,300 L575,320 L60,320 Z" />
          <path
            d="M200,208 Q250,150 380,150 Q450,150 470,200"
            fill="oklch(0.32 0.012 60)"
          />
          <path
            d="M225,200 Q270,168 370,168 Q435,168 455,200 Z"
            fill="oklch(0.78 0.04 240 / 0.5)"
          />
        </g>
        <circle cx="180" cy="328" r="32" fill="oklch(0.18 0.005 60)" />
        <circle cx="180" cy="328" r="14" fill="oklch(0.55 0.005 75)" />
        <circle cx="450" cy="328" r="32" fill="oklch(0.18 0.005 60)" />
        <circle cx="450" cy="328" r="14" fill="oklch(0.55 0.005 75)" />
      </svg>
      <div className="absolute bottom-6 left-6 right-6 flex items-center justify-between rounded-lg border border-border bg-card/90 p-4 backdrop-blur">
        <div>
          <div className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground">Featured</div>
          <div className="font-display text-base">Porsche 911 Carrera · 2022</div>
        </div>
        <div className="font-display text-lg tabular text-primary">R$ 845.000</div>
      </div>
    </div>
  );
}
