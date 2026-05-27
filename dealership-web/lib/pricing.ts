// lib/pricing.ts
// Purchase financial summary computations.
// Source: research.md R-06; data-model.md §5

/** Platform tax rate: 10% applied to the car's listed value */
export const TAX_RATE = 0.1;

/**
 * Computes the platform tax amount for a given listed value.
 * @param listedValue - BRL price as returned by the BFF
 */
export function computeTax(listedValue: number): number {
  return listedValue * TAX_RATE;
}

/**
 * Computes the purchase total (listed value + tax).
 * @param listedValue - BRL price as returned by the BFF
 */
export function computeTotal(listedValue: number): number {
  return listedValue + computeTax(listedValue);
}
