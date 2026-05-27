"use client";
// components/car/BuyPanel.tsx
// Purchase CTA panel — status-aware.
// AVAILABLE → shows "Comprar" button; SOLD/UNAVAILABLE → shows status message.
// Source: tasks.md T043; FR-071

import { useRouter } from "next/navigation";
import { formatBRL } from "@/lib/format";
import { computeTotal } from "@/lib/pricing";
import type { CarStatus } from "@/lib/api/types";

interface BuyPanelProps {
  carId: string;
  listedValue: number;
  status: CarStatus;
}

const STATUS_MESSAGES: Record<Exclude<CarStatus, "AVAILABLE">, string> = {
  SOLD: "Este veículo foi vendido.",
  UNAVAILABLE: "Este veículo não está disponível no momento.",
};

export function BuyPanel({ carId, listedValue, status }: BuyPanelProps) {
  const router = useRouter();
  const total = computeTotal(listedValue);

  if (status !== "AVAILABLE") {
    return (
      <div
        aria-live="polite"
        className="rounded-[var(--radius-lg)] border border-border bg-card p-6"
      >
        <p className="text-center text-muted-foreground">
          {STATUS_MESSAGES[status]}
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Preço final</p>
      <p className="tabular mt-1 font-display text-4xl font-semibold text-primary">
        {formatBRL(total)}
      </p>

      <p className="mt-2 text-xs text-muted-foreground">
        A taxa da plataforma já está incluída no total mostrado acima.
      </p>

      <div className="mt-5 flex flex-col gap-2 sm:flex-row">
        <button
          type="button"
          onClick={() => router.push(`/purchase/${carId}`)}
          className="flex-1 rounded-md bg-primary py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Comprar este carro
        </button>
        <button
          type="button"
          className="flex-1 rounded-md border border-border bg-card px-3 py-3 text-sm font-medium text-foreground hover:border-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Salvar para depois
        </button>
      </div>
    </div>
  );
}
