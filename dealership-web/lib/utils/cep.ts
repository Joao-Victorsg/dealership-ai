// lib/utils/cep.ts
// CEP validation and masking utilities.
// All constants are module-scope (never instantiated inside a function).
// Source: research.md R-12

/** Matches a raw 8-digit CEP (no hyphen) */
export const CEP_REGEX = /^\d{8}$/;

/** Matches a masked CEP in the format XXXXX-XXX */
export const CEP_REGEX_MASKED = /^\d{5}-\d{3}$/;

/**
 * Applies CEP mask: XXXXX-XXX
 * Input may be partial — masking is progressive.
 */
export function cepMask(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 8);
  return digits.replace(/^(\d{5})(\d)/, "$1-$2");
}

/**
 * Strips the hyphen from a masked CEP to get the raw 8-digit form
 * suitable for sending to the BFF.
 */
export function cepUnmask(value: string): string {
  return value.replace(/\D/g, "");
}
