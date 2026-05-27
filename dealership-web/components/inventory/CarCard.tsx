"use client";

import Image from "next/image";
import Link from "next/link";
import { formatBRL, formatKm } from "@/lib/format";
import type { CarCardProps } from "@/lib/api/types";
import { CategoryBadge, ConditionBadge, StatusBadge, TypeBadge } from "@/components/inventory/CarBadges";
import { CarPlaceholder } from "@/components/shared/CarPlaceholder";

export function CarCard({
  id,
  manufacturer,
  model,
  manufacturingYear,
  category,
  isNew,
  type,
  externalColor,
  kilometers,
  listedValue,
  status,
  imageUrl,
}: CarCardProps) {
  return (
    <Link
      href={`/inventory/${id}`}
      className="group flex flex-col overflow-hidden rounded-lg border border-border bg-card transition-all duration-200 ease-[var(--ease-out-expo)] hover:-translate-y-0.5 hover:border-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
    >
      <div className="relative aspect-[16/10] overflow-hidden bg-muted">
        {imageUrl ? (
          <Image
            src={imageUrl}
            alt={`${manufacturer} ${model} ${manufacturingYear}`}
            fill
            className="object-cover transition-transform duration-500 ease-[var(--ease-out-expo)] group-hover:scale-[1.03]"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
          />
        ) : (
          <CarPlaceholder
            externalColor={externalColor}
            className="h-full w-full transition-transform duration-500 ease-[var(--ease-out-expo)] group-hover:scale-[1.03]"
          />
        )}
        <div className="absolute left-3 top-3 flex flex-wrap gap-1.5">
          <ConditionBadge isNew={isNew} />
        </div>
        <div className="absolute right-3 top-3">
          <StatusBadge status={status} />
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div>
          <div className="flex items-baseline justify-between gap-2">
            <h3 className="font-display text-lg font-semibold leading-tight">
              {manufacturer} <span className="text-foreground/85">{model}</span>
            </h3>
            <span className="tabular text-sm text-muted-foreground">{manufacturingYear}</span>
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5">
            <TypeBadge type={type} />
            <CategoryBadge category={category} />
          </div>
        </div>

        <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <div>
            <dt className="sr-only">Exterior color</dt>
            <dd>{externalColor}</dd>
          </div>
          <div className="text-right">
            <dt className="sr-only">Kilometers</dt>
            <dd className="tabular">{formatKm(kilometers)}</dd>
          </div>
        </dl>

        <div className="mt-auto flex items-end justify-between border-t border-border pt-3">
          <div>
            <div className="text-[10px] uppercase tracking-[0.18em] text-muted-foreground">Price</div>
            <div className="tabular font-display text-xl font-semibold text-primary">
              {formatBRL(listedValue)}
            </div>
          </div>
          <span className="text-xs text-foreground/70 transition-colors group-hover:text-foreground">
            View details →
          </span>
        </div>
      </div>
    </Link>
  );
}
