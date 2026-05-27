"use server";
// app/(admin)/admin/actions.ts
// Server Actions for admin inventory management.
// Source: tasks.md T068; spec.md US7 SC2-3; lib/api/admin.ts
// ⚠️ Endpoint shapes are assumed (R-08).

import { revalidatePath } from "next/cache";
import { createCar, updateCar } from "@/lib/api/admin";
import { getBFFErrorMessage } from "@/lib/errors";
import { BffError } from "@/lib/api/client";
import type { CarStatus } from "@/lib/api/types";

export interface AdminActionResult {
  ok: boolean;
  message?: string;
  requestId?: string;
}

export interface CarFormInput {
  manufacturer: string;
  model: string;
  manufacturingYear: number;
  type: "GASOLINE" | "DIESEL" | "ELECTRIC" | "HYBRID";
  category:
    | "SEDAN"
    | "SUV"
    | "HATCHBACK"
    | "COUPE"
    | "CONVERTIBLE"
    | "MINIVAN"
    | "PICKUP"
    | "OTHER";
  isNew: boolean;
  listedValue: number;
  kilometers: number;
  externalColor: string;
  internalColor: string;
  vin: string;
  optionalItemsRaw: string;
}

function parseOptionalItems(raw: string): string[] {
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

/**
 * Creates a new car in the inventory.
 * ⚠️ Assumed endpoint POST /api/v1/admin/inventory.
 */
export async function createCarAction(
  input: CarFormInput
): Promise<AdminActionResult> {
  try {
    await createCar({
      manufacturer: input.manufacturer,
      model: input.model,
      manufacturingYear: input.manufacturingYear,
      type: input.type,
      category: input.category,
      isNew: input.isNew,
      listedValue: input.listedValue,
      kilometers: input.kilometers,
      externalColor: input.externalColor,
      internalColor: input.internalColor,
      vin: input.vin,
      optionalItems: parseOptionalItems(input.optionalItemsRaw),
    });
    revalidatePath("/admin");
    return { ok: true };
  } catch (err: unknown) {
    if (err instanceof BffError) {
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }
}

/**
 * Updates an existing car.
 * ⚠️ Assumed endpoint PATCH /api/v1/admin/inventory/{carId}.
 */
export async function updateCarAction(
  id: string,
  input: Partial<CarFormInput>
): Promise<AdminActionResult> {
  try {
    const body = {
      ...(input.manufacturer !== undefined && {
        manufacturer: input.manufacturer,
      }),
      ...(input.model !== undefined && { model: input.model }),
      ...(input.manufacturingYear !== undefined && {
        manufacturingYear: input.manufacturingYear,
      }),
      ...(input.type !== undefined && { type: input.type }),
      ...(input.category !== undefined && { category: input.category }),
      ...(input.isNew !== undefined && { isNew: input.isNew }),
      ...(input.listedValue !== undefined && {
        listedValue: input.listedValue,
      }),
      ...(input.kilometers !== undefined && { kilometers: input.kilometers }),
      ...(input.externalColor !== undefined && {
        externalColor: input.externalColor,
      }),
      ...(input.internalColor !== undefined && {
        internalColor: input.internalColor,
      }),
      ...(input.vin !== undefined && { vin: input.vin }),
      ...(input.optionalItemsRaw !== undefined && {
        optionalItems: parseOptionalItems(input.optionalItemsRaw),
      }),
    };
    await updateCar(id, body);
    revalidatePath("/admin");
    return { ok: true };
  } catch (err: unknown) {
    if (err instanceof BffError) {
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }
}

/**
 * Changes the status of a car (Available / Unavailable).
 */
export async function changeCarStatusAction(
  id: string,
  status: CarStatus
): Promise<AdminActionResult> {
  try {
    await updateCar(id, { status });
    revalidatePath("/admin");
    return { ok: true };
  } catch (err: unknown) {
    if (err instanceof BffError) {
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }
}
