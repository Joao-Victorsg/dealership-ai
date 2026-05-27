// lib/errors.ts
// Maps BFF error codes to user-safe Portuguese messages.
// Used throughout Server Actions and Client Components to render errors.
// Source: research.md R-13

import type { BffErrorCode } from "@/lib/api/types";

const ERROR_MESSAGES: Record<BffErrorCode, string> = {
  CAR_NOT_AVAILABLE:
    "Este carro não está mais disponível. Outro comprador finalizou a compra.",
  VALIDATION_ERROR: "Verifique os campos e tente novamente.",
  AUTHENTICATION_REQUIRED:
    "Sua sessão expirou. Por favor, faça login novamente.",
  FORBIDDEN: "Você não tem permissão para realizar esta ação.",
  NOT_FOUND: "O item solicitado não foi encontrado.",
  RATE_LIMIT_EXCEEDED:
    "Muitas tentativas. Aguarde um momento e tente novamente.",
  DOWNSTREAM_UNAVAILABLE:
    "Nosso serviço está temporariamente indisponível. Tente novamente em instantes.",
  DUPLICATE_IDENTITY:
    "Este e-mail já está cadastrado. Faça login ou use outro e-mail.",
  INTERNAL_ERROR: "Ocorreu um erro interno. Por favor, tente novamente.",
};

const FALLBACK_MESSAGE =
  "Ocorreu um erro inesperado. Por favor, tente novamente.";

/**
 * Returns a user-safe Portuguese error message for a given BFF error code.
 * Unknown codes fall back to a generic message — never exposes internals.
 */
export function getBFFErrorMessage(code: BffErrorCode | string): string {
  return ERROR_MESSAGES[code as BffErrorCode] ?? FALLBACK_MESSAGE;
}
