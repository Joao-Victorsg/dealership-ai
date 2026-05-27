// app/(customer)/purchase/[carId]/page.tsx
// Purchase confirmation page — authenticated customer only.
// Loads car details and customer profile in parallel.
// Source: tasks.md T057; spec.md US4 SC1-2

import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { getCarById } from "@/lib/api/inventory";
import { getProfile } from "@/lib/api/profile";
import { BffError } from "@/lib/api/client";
import { FinancialSummary } from "@/components/purchase/FinancialSummary";
import { ConfirmButton } from "@/components/purchase/ConfirmButton";
import { formatBRL } from "@/lib/format";

interface PurchasePageProps {
  params: Promise<{ carId: string }>;
}

export async function generateMetadata({
  params,
}: PurchasePageProps): Promise<Metadata> {
  const { carId } = await params;
  try {
    const car = await getCarById(carId);
    return {
      title: `Comprar ${car.data.manufacturer} ${car.data.model} | Aurelio Motors`,
    };
  } catch {
    return { title: "Confirmar compra | Aurelio Motors" };
  }
}

export default async function PurchasePage({ params }: PurchasePageProps) {
  const { carId } = await params;

  let car;
  let profile;

  try {
    [car, profile] = await Promise.all([getCarById(carId), getProfile()]);
  } catch (err: unknown) {
    if (err instanceof BffError && err.code === "NOT_FOUND") {
      notFound();
    }
    throw err;
  }

  const carData = car.data;

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-8 font-display text-2xl font-bold text-foreground sm:text-3xl">
        Confirmar compra
      </h1>

      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        {/* Car summary */}
        <div className="space-y-6">
          <div className="rounded-[var(--radius-lg)] border border-border bg-card p-5">
            <h2 className="mb-4 text-base font-semibold text-foreground">
              Veículo
            </h2>
            <div className="space-y-1 text-sm">
              <p className="text-foreground font-medium">
                {carData.manufacturer} {carData.model} ({carData.manufacturingYear})
              </p>
              <p className="text-muted-foreground">
                Cor: {carData.externalColor} &bull; VIN: {carData.vin}
              </p>
              <p className="text-muted-foreground">
                Valor: {formatBRL(carData.listedValue)}
              </p>
            </div>
          </div>

          {/* Buyer info */}
          <div className="rounded-[var(--radius-lg)] border border-border bg-card p-5">
            <h2 className="mb-4 text-base font-semibold text-foreground">
              Comprador
            </h2>
            <div className="space-y-1 text-sm text-muted-foreground">
              <p>
                {profile.firstName} {profile.lastName}
              </p>
              <p>{profile.email}</p>
              <p>CPF: {profile.cpf}</p>
            </div>
          </div>
        </div>

        {/* Sticky financial summary + confirm */}
        <div className="lg:sticky lg:top-24 space-y-4 self-start">
          <FinancialSummary listedValue={carData.listedValue} />
          <ConfirmButton carId={carId} />
        </div>
      </div>
    </div>
  );
}
