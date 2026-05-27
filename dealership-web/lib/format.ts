// lib/format.ts
// Locale-aware formatting utilities for currency, distance, and dates.
// All user-visible formatted values must flow through these functions.
// Source: FR-041; research.md R-12

/**
 * Formats a BRL value using the pt-BR locale with two decimal places.
 * Example: 45000 → "R$\u00a045.000,00"
 */
export function formatBRL(value: number): string {
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/**
 * Formats a kilometer value with thousands separator and "km" suffix.
 * Example: 15000 → "15.000 km"
 */
export function formatKm(value: number): string {
  return `${value.toLocaleString("pt-BR")} km`;
}

/**
 * Formats an ISO-8601 date string to "DD de MMM. de YYYY" in Portuguese.
 * Example: "2024-01-15T10:00:00Z" → "15 de jan. de 2024"
 */
export function formatDate(iso: string): string {
  const date = new Date(iso);
  return date.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}
