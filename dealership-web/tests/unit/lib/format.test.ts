// tests/unit/lib/format.test.ts
// Unit tests for lib/format.ts
// Source: tasks.md T093

import { describe, it, expect } from "vitest";
import { formatBRL, formatKm, formatDate } from "@/lib/format";

describe("formatBRL", () => {
  it("formats typical car price with R$ prefix and pt-BR separators", () => {
    const result = formatBRL(75_000);
    expect(result).toContain("R$");
    // pt-BR: R$\u00a075.000,00
    expect(result).toMatch(/75[.,]?000/);
  });

  it("formats zero value", () => {
    const result = formatBRL(0);
    expect(result).toContain("R$");
    expect(result).toContain("0");
  });

  it("formats decimal values with cents", () => {
    const result = formatBRL(1234.56);
    expect(result).toContain("1");
    expect(result).toContain("234");
  });

  it("formats large value with thousands separator", () => {
    const result = formatBRL(1_000_000);
    expect(result).toContain("R$");
    // Should have 1.000.000 or 1,000,000 grouping
    expect(result.replace(/[R$\s]/g, "").length).toBeGreaterThan(6);
  });

  it("returns a string", () => {
    expect(typeof formatBRL(50_000)).toBe("string");
  });
});

describe("formatKm", () => {
  it('appends "km" suffix', () => {
    expect(formatKm(10_000)).toContain("km");
  });

  it("formats thousands with separator", () => {
    const result = formatKm(100_000);
    // Should produce something like "100.000 km" or "100,000 km"
    expect(result).toMatch(/100[.,]?000/);
    expect(result).toContain("km");
  });

  it("formats zero km", () => {
    const result = formatKm(0);
    expect(result).toContain("0");
    expect(result).toContain("km");
  });

  it("returns a string", () => {
    expect(typeof formatKm(50_000)).toBe("string");
  });
});

describe("formatDate", () => {
  it("formats ISO date in pt-BR long format", () => {
    // Use noon UTC to avoid timezone day-shift
    const result = formatDate("2024-01-15T12:00:00.000Z");
    // Should contain "janeiro" (Portuguese month)
    expect(result.toLowerCase()).toMatch(
      /jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez/
    );
    expect(result).toContain("2024");
  });

  it("includes the day number", () => {
    const result = formatDate("2024-03-07T12:00:00.000Z");
    expect(result).toMatch(/7|07/);
  });

  it("returns a string", () => {
    expect(typeof formatDate("2024-06-01T12:00:00.000Z")).toBe("string");
  });

  it("handles different months", () => {
    // Use noon UTC to avoid timezone day-shift across midnight
    const months = [
      "2024-01-15",
      "2024-04-15",
      "2024-07-15",
      "2024-10-15",
    ];
    const results = months.map((d) => formatDate(`${d}T12:00:00.000Z`));
    // All should be strings containing the year
    for (const r of results) {
      expect(typeof r).toBe("string");
      expect(r).toContain("2024");
    }
  });
});
