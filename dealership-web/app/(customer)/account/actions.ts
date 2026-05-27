"use server";
// app/(customer)/account/actions.ts
// Server Action for updating the customer profile.
// Source: tasks.md T062; spec.md US5 SC1-4; research.md R-04
//
// IMPORTANT: cpf and streetNumber are NEVER included in the PATCH body.
// The BFF contract explicitly rejects them (R-04).

import { updateProfile } from "@/lib/api/profile";
import { getBFFErrorMessage } from "@/lib/errors";
import { BffError } from "@/lib/api/client";
import { cepUnmask } from "@/lib/utils/cep";
import { profileSchema, type ProfileFormValues } from "./schema";

export interface UpdateProfileResult {
  ok: boolean;
  fieldErrors?: Record<string, string>;
  message?: string;
  requestId?: string;
}

export async function updateProfileAction(
  values: ProfileFormValues
): Promise<UpdateProfileResult> {
  const parsed = profileSchema.safeParse(values);
  if (!parsed.success) {
    const fieldErrors: Record<string, string> = {};
    for (const [field, msgs] of Object.entries(
      parsed.error.flatten().fieldErrors
    )) {
      fieldErrors[field] = msgs?.[0] ?? "Inválido";
    }
    return { ok: false, fieldErrors };
  }

  try {
    await updateProfile({
      firstName: parsed.data.firstName,
      lastName: parsed.data.lastName,
      phone: parsed.data.phone.replace(/\D/g, ""),
      cep: cepUnmask(parsed.data.cep),
      // cpf and streetNumber intentionally omitted (R-04)
    });
  } catch (err: unknown) {
    if (err instanceof BffError) {
      if (err.code === "VALIDATION_ERROR" && err.details?.length) {
        const fieldErrors: Record<string, string> = {};
        for (const detail of err.details) {
          fieldErrors[detail.field] = detail.message;
        }
        return { ok: false, fieldErrors, requestId: err.requestId };
      }
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }

  return { ok: true };
}
