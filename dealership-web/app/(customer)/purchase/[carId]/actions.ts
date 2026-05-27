"use server";
// app/(customer)/purchase/[carId]/actions.ts
// Server Action for confirming a car purchase.
// Source: tasks.md T056; spec.md US4 SC1-4; contracts/bff-api.md §Purchase

import { redirect } from "next/navigation";
import { confirmPurchase } from "@/lib/api/purchases";
import { getBFFErrorMessage } from "@/lib/errors";
import { BffError } from "@/lib/api/client";

export interface ConfirmPurchaseResult {
  ok: false;
  code?: string;
  message?: string;
  requestId?: string;
}

/**
 * Confirms the purchase of a car.
 * On success redirects to /purchase/success (no return value).
 * On failure returns an error descriptor — caller handles inline display.
 */
export async function confirmPurchaseAction(
  carId: string
): Promise<ConfirmPurchaseResult | undefined> {
  try {
    await confirmPurchase(carId);
  } catch (err: unknown) {
    if (err instanceof BffError) {
      if (err.code === "CAR_NOT_AVAILABLE") {
        return { ok: false, code: "CAR_NOT_AVAILABLE" };
      }
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }

  redirect("/purchase/success");
}
