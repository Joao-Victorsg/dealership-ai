"use client";
// components/purchase/ConfirmButton.tsx
// Purchase confirmation button with optimistic disable on first click.
// Source: tasks.md T055; spec.md US4 SC1-4

import { useTransition } from "react";
import Link from "next/link";
import { confirmPurchaseAction } from "@/app/(customer)/purchase/[carId]/actions";
import { ErrorDisplay } from "@/components/shared/ErrorDisplay";
import { useState } from "react";

interface ConfirmButtonProps {
  carId: string;
}

export function ConfirmButton({ carId }: ConfirmButtonProps) {
  const [isPending, startTransition] = useTransition();
  const [unavailable, setUnavailable] = useState(false);
  const [error, setError] = useState<{
    message: string;
    requestId?: string;
  } | null>(null);

  function handleConfirm() {
    setError(null);
    setUnavailable(false);
    startTransition(async () => {
      const result = await confirmPurchaseAction(carId);
      if (!result) return; // redirect was called — no result returned
      if (result.code === "CAR_NOT_AVAILABLE") {
        setUnavailable(true);
        return;
      }
      setError({
        message: result.message ?? "Erro ao confirmar compra.",
        requestId: result.requestId,
      });
    });
  }

  return (
    <div className="space-y-3">
      <button
        type="button"
        onClick={handleConfirm}
        disabled={isPending || unavailable}
        className="w-full rounded-[var(--radius)] bg-primary py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isPending ? "Confirmando…" : "Confirmar compra"}
      </button>

      {unavailable && (
        <div
          role="alert"
          className="rounded-[var(--radius)] border border-destructive/30 bg-destructive/10 p-4 text-sm"
        >
          <p className="font-medium text-destructive">
            Este veículo não está mais disponível.
          </p>
          <p className="mt-1 text-muted-foreground">
            Infelizmente outro comprador finalizou a compra antes de você.
          </p>
          <Link
            href="/inventory"
            className="mt-2 inline-block text-sm font-medium text-primary underline-offset-2 hover:underline"
          >
            Ver outros veículos
          </Link>
        </div>
      )}

      {error && (
        <ErrorDisplay message={error.message} requestId={error.requestId} />
      )}
    </div>
  );
}
