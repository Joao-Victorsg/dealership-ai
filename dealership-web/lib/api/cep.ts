// lib/api/cep.ts
// BFF CEP lookup client function.
// ⚠️ Assumed contract — BFF team must implement GET /api/v1/cep/{cep}.
// The web app MUST NOT call ViaCEP directly (research.md R-05).
// Source: contracts/bff-api.md §CEP Lookup; research.md R-05

import { bffFetch, BffError } from "@/lib/api/client";
import type { CepLookupResult } from "@/lib/api/types";

/** ⚠️ Assumed BFF response shape for GET /api/v1/cep/{cep} */
interface BffCepSuccessResponse {
  data: {
    street: string;
    neighborhood: string;
    city: string;
    state: string;
  };
}

/**
 * Looks up a CEP address via the BFF.
 * ⚠️ Calls GET /api/v1/cep/{cep} — an ASSUMED endpoint that BFF team must implement.
 *
 * @param cep - 8 raw digits (no hyphen)
 * @returns CepLookupResult — either { ok: true, ...address } or { ok: false, error }
 *
 * CEP lookup failures are non-blocking (spec US2 SC2, R-05) — callers must handle
 * the { ok: false } case by showing a warning without blocking form submission.
 */
export async function lookupCep(cep: string): Promise<CepLookupResult> {
  try {
    const response = await bffFetch<BffCepSuccessResponse>(
      `/api/v1/cep/${encodeURIComponent(cep)}`
    );
    const { street, neighborhood, city, state } = response.data;
    return { ok: true, street, neighborhood, city, state };
  } catch (error) {
    if (error instanceof BffError) {
      if (error.code === "NOT_FOUND") {
        return { ok: false, error: "CEP não encontrado." };
      }
      if (error.code === "DOWNSTREAM_UNAVAILABLE") {
        return {
          ok: false,
          error: "Serviço de CEP indisponível. Preencha o endereço manualmente.",
        };
      }
      return { ok: false, error: "Não foi possível consultar o CEP." };
    }
    return {
      ok: false,
      error: "Erro ao consultar o CEP. Verifique sua conexão.",
    };
  }
}
