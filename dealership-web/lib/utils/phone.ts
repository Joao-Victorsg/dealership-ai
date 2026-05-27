// lib/utils/phone.ts
// Brazilian phone number validation and masking utilities.
// All constants are module-scope (never instantiated inside a function).
// Source: research.md R-12

/** Matches a raw phone: 10 digits (landline) or 11 digits (mobile) */
export const PHONE_REGEX = /^\d{10,11}$/;

/**
 * Returns true for valid Brazilian phone numbers:
 * - 10-digit format: (XX) XXXX-XXXX (landline)
 * - 11-digit format: (XX) XXXXX-XXXX (mobile)
 * Accepts raw digit strings or masked strings (strips non-digits before checking).
 */
export function isValidPhone(phone: string): boolean {
  const digits = phone.replace(/\D/g, "");
  return digits.length === 10 || digits.length === 11;
}

/**
 * Applies Brazilian phone mask:
 * - 10 digits → (XX) XXXX-XXXX
 * - 11 digits → (XX) XXXXX-XXXX
 * Input may be partial — masking is progressive.
 */
export function phoneMask(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 11);

  if (digits.length <= 2) {
    return digits.length > 0 ? `(${digits}` : "";
  }
  if (digits.length <= 6) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  }
  if (digits.length <= 10) {
    // 10-digit (landline) or partial mobile: (XX) XXXX-XXXX
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  }
  // 11-digit mobile: (XX) XXXXX-XXXX
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}
