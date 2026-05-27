"use server";
// app/(auth)/complete-registration/actions.ts
// Server Action for completing customer registration.
// Source: tasks.md T047; spec.md US2 SC3-5; contracts/bff-api.md §Auth
import { registerUser } from "@/lib/api/auth";
import { getBFFErrorMessage } from "@/lib/errors";
import { BffError } from "@/lib/api/client";
import { cepUnmask } from "@/lib/utils/cep";
import { registerSchema } from "./schema";

export interface RegisterActionResult {
  ok: boolean;
  fieldErrors?: Record<string, string>;
  message?: string;
  requestId?: string;
  redirectTo?: string;
}

export async function registerAction(
  values: {
    firstName: string;
    lastName: string;
    cpf: string;
    phone: string;
    cep: string;
    streetNumber: string;
  }
): Promise<RegisterActionResult> {
  const parsed = registerSchema.safeParse(values);
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
    const phoneDigits = parsed.data.phone.replace(/\D/g, "");
    const normalizedPhone = phoneDigits.startsWith("55")
      ? `+${phoneDigits}`
      : `+55${phoneDigits}`;

    await registerUser({
      firstName: parsed.data.firstName,
      lastName: parsed.data.lastName,
      cpf: parsed.data.cpf,
      phone: normalizedPhone,
      cep: cepUnmask(parsed.data.cep),
      streetNumber: parsed.data.streetNumber,
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
      if (err.code === "DUPLICATE_IDENTITY") {
        const duplicateByKeycloak = /keycloak/i.test(err.message);
        if (duplicateByKeycloak) {
          return { ok: true, redirectTo: "/inventory" };
        }
      }
      return {
        ok: false,
        message: getBFFErrorMessage(err.code),
        requestId: err.requestId,
      };
    }
    return { ok: false, message: getBFFErrorMessage("INTERNAL_ERROR") };
  }

  return { ok: true, redirectTo: "/inventory" };
}
