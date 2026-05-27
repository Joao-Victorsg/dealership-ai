// lib/utils/cpf.ts
// CPF validation and masking utilities.
// All constants are module-scope (never instantiated inside a function).
// Source: research.md R-12

/** Matches a CPF in the masked format: XXX.XXX.XXX-XX */
export const CPF_REGEX = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/;

/** Matches a raw unformatted 11-digit CPF */
export const CPF_REGEX_RAW = /^\d{11}$/;

/**
 * Validates a CPF using the standard check-digit algorithm.
 * Accepts both masked (XXX.XXX.XXX-XX) and raw (11-digit) formats.
 * Returns false for all-same-digit sequences (e.g., "000.000.000-00").
 */
export function isValidCpf(cpf: string): boolean {
  const digits = cpf.replace(/\D/g, "");

  if (digits.length !== 11) return false;

  // Reject all-same-digit CPFs (e.g., "00000000000")
  if (/^(\d)\1{10}$/.test(digits)) return false;

  // First check digit
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += Number(digits[i]) * (10 - i);
  }
  let remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;
  if (remainder !== Number(digits[9])) return false;

  // Second check digit
  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += Number(digits[i]) * (11 - i);
  }
  remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;
  if (remainder !== Number(digits[10])) return false;

  return true;
}

/**
 * Applies CPF mask: XXX.XXX.XXX-XX
 * Input may be partial — masking is progressive.
 */
export function cpfMask(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 11);
  return digits
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
}
