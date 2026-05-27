// components/car/CarDetail.tsx
// Renders all fields for a single car — RSC compatible.
// Source: tasks.md T042; FR-041 through FR-044

import { formatBRL, formatKm, formatDate } from "@/lib/format";
import { computeTax, computeTotal, TAX_RATE } from "@/lib/pricing";
import type { Car } from "@/lib/api/types";
import {
  CategoryBadge,
  ConditionBadge,
  StatusBadge,
  TypeBadge,
} from "@/components/inventory/CarBadges";

const CATEGORY_LABELS: Record<string, string> = {
  SEDAN: "Sedã",
  SUV: "SUV",
  HATCHBACK: "Hatch",
  PICKUP: "Picape",
  COUPE: "Coupé",
  CONVERTIBLE: "Conversível",
  MINIVAN: "Minivan",
  OTHER: "Outro",
};

const TYPE_LABELS: Record<string, string> = {
  ELECTRIC: "Elétrico",
  HYBRID: "Híbrido",
  GASOLINE: "Gasolina",
  DIESEL: "Diesel",
};

const PROPULSION_LABELS: Record<string, string> = {
  FRONT_WHEEL_DRIVE: "Tração dianteira",
  REAR_WHEEL_DRIVE: "Tração traseira",
  ALL_WHEEL_DRIVE: "Tração integral",
  FOUR_WHEEL_DRIVE: "Tração 4x4",
};

interface CarDetailProps {
  car: Car;
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border py-3 text-sm last:border-b-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium text-foreground">{value}</dd>
    </div>
  );
}

export function CarDetail({ car }: CarDetailProps) {
  const tax = computeTax(car.listedValue);
  const total = computeTotal(car.listedValue);
  const taxRatePercent = (TAX_RATE * 100).toFixed(0);

  return (
    <article aria-label={`${car.manufacturer} ${car.model} ${car.manufacturingYear}`}>
      {/* Title */}
      <header className="mb-6">
        <div className="mb-3 flex flex-wrap items-center gap-2">
          <ConditionBadge isNew={car.isNew} />
          <StatusBadge status={car.status} />
          <TypeBadge type={car.type} />
          <CategoryBadge category={car.category} />
        </div>
        <h1 className="font-display text-3xl font-semibold text-foreground">
          {car.manufacturer} {car.model}
        </h1>
        <p className="mt-1 tabular text-lg text-muted-foreground">
          {car.manufacturingYear}
        </p>
      </header>

      {/* Pricing */}
      <section
        aria-labelledby="pricing-heading"
        className="mb-8 rounded-[var(--radius-lg)] border border-border bg-card p-6"
      >
        <h2 id="pricing-heading" className="sr-only">
          Valores
        </h2>
        <dl>
          <DetailRow label="Valor anunciado" value={formatBRL(car.listedValue)} />
          <DetailRow
            label={`Imposto estimado (${taxRatePercent}%)`}
            value={formatBRL(tax)}
          />
          <div className="flex justify-between gap-4 pt-3 text-base">
            <dt className="font-semibold text-foreground">Total estimado</dt>
            <dd className="tabular text-right text-xl font-bold text-primary">
              {formatBRL(total)}
            </dd>
          </div>
        </dl>
      </section>

      {/* Technical specs */}
      <section aria-labelledby="specs-heading" className="mb-8">
        <h2
          id="specs-heading"
          className="mb-4 text-lg font-semibold text-foreground"
        >
          Especificações
        </h2>
        <dl className="rounded-[var(--radius-lg)] border border-border bg-card px-4">
          <DetailRow
            label="Estado"
            value={car.isNew ? "Novo" : "Seminovo"}
          />
          <DetailRow label="Quilometragem" value={formatKm(car.kilometers)} />
          <DetailRow
            label="Categoria"
            value={CATEGORY_LABELS[car.category] ?? car.category}
          />
          <DetailRow
            label="Combustível"
            value={TYPE_LABELS[car.type] ?? car.type}
          />
          <DetailRow
            label="Tração"
            value={PROPULSION_LABELS[car.propulsionType] ?? car.propulsionType}
          />
          <DetailRow label="Cor externa" value={car.externalColor} />
          <DetailRow label="Cor interna" value={car.internalColor} />
          <DetailRow label="VIN" value={car.vin} />
          <DetailRow label="Data de cadastro" value={formatDate(car.registrationDate)} />
        </dl>
      </section>

      {/* Optional items */}
      {car.optionalItems.length > 0 && (
        <section aria-labelledby="optionals-heading" className="mb-8">
          <h2
            id="optionals-heading"
            className="mb-4 text-lg font-semibold text-foreground"
          >
            Opcionais
          </h2>
          <ul className="flex flex-wrap gap-2">
            {car.optionalItems.map((item) => (
              <li
                key={item}
                className="rounded-[var(--radius-full)] border border-border bg-card px-3 py-1 text-sm text-muted-foreground"
              >
                {item}
              </li>
            ))}
          </ul>
        </section>
      )}
    </article>
  );
}
