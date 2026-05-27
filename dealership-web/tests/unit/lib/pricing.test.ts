// tests/unit/lib/pricing.test.ts
// Unit tests for lib/pricing.ts
// Source: tasks.md T092

import { describe, it, expect } from "vitest";
import { TAX_RATE, computeTax, computeTotal } from "@/lib/pricing";

describe("TAX_RATE", () => {
  it("is exactly 0.10", () => {
    expect(TAX_RATE).toBe(0.1);
  });
});

describe("computeTax", () => {
  it("returns 10% of input value", () => {
    expect(computeTax(100_000)).toBeCloseTo(10_000);
  });

  it("returns 0 for zero input", () => {
    expect(computeTax(0)).toBe(0);
  });

  it("handles large integer values correctly", () => {
    expect(computeTax(1_000_000)).toBeCloseTo(100_000);
  });

  it("handles decimal values", () => {
    expect(computeTax(99.99)).toBeCloseTo(9.999);
  });

  it("is always positive for positive input", () => {
    expect(computeTax(50_000)).toBeGreaterThan(0);
  });
});

describe("computeTotal", () => {
  it("returns 110% of input value", () => {
    expect(computeTotal(100_000)).toBeCloseTo(110_000);
  });

  it("returns 0 for zero input", () => {
    expect(computeTotal(0)).toBe(0);
  });

  it("equals listedValue + computeTax(listedValue)", () => {
    const listed = 75_000;
    expect(computeTotal(listed)).toBeCloseTo(listed + computeTax(listed));
  });

  it("handles large values without overflow", () => {
    expect(computeTotal(10_000_000)).toBeCloseTo(11_000_000);
  });

  it("is always greater than listedValue for positive input", () => {
    expect(computeTotal(50_000)).toBeGreaterThan(50_000);
  });
});
