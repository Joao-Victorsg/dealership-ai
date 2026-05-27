// tests/unit/lib/utils/cpf.test.ts
// Unit tests for lib/utils/cpf.ts
// Source: tasks.md T094

import { describe, it, expect } from "vitest";
import { CPF_REGEX, CPF_REGEX_RAW, isValidCpf, cpfMask } from "@/lib/utils/cpf";

// Known valid CPF (mathematically valid check digits)
const VALID_CPF_RAW = "52998224725";
const VALID_CPF_MASKED = "529.982.247-25";

// Known invalid CPF (wrong check digit)
const INVALID_CPF_WRONG_DIGIT = "52998224726";
const INVALID_CPF_WRONG_DIGIT_MASKED = "529.982.247-26";

const ALL_SAME_DIGIT_CPFS = [
  "00000000000",
  "11111111111",
  "22222222222",
  "33333333333",
  "44444444444",
  "55555555555",
  "66666666666",
  "77777777777",
  "88888888888",
  "99999999999",
];

describe("CPF_REGEX", () => {
  it("matches a valid masked CPF", () => {
    expect(CPF_REGEX.test(VALID_CPF_MASKED)).toBe(true);
  });

  it("does not match unmasked CPF", () => {
    expect(CPF_REGEX.test(VALID_CPF_RAW)).toBe(false);
  });

  it("does not match partial input", () => {
    expect(CPF_REGEX.test("529.982.247")).toBe(false);
  });
});

describe("CPF_REGEX_RAW", () => {
  it("matches 11-digit raw CPF", () => {
    expect(CPF_REGEX_RAW.test(VALID_CPF_RAW)).toBe(true);
  });

  it("does not match masked CPF", () => {
    expect(CPF_REGEX_RAW.test(VALID_CPF_MASKED)).toBe(false);
  });

  it("does not match 10-digit input", () => {
    expect(CPF_REGEX_RAW.test("5299822472")).toBe(false);
  });
});

describe("isValidCpf", () => {
  it("returns true for a known valid raw CPF", () => {
    expect(isValidCpf(VALID_CPF_RAW)).toBe(true);
  });

  it("returns true for a known valid masked CPF", () => {
    expect(isValidCpf(VALID_CPF_MASKED)).toBe(true);
  });

  it("returns false for a CPF with wrong check digit", () => {
    expect(isValidCpf(INVALID_CPF_WRONG_DIGIT)).toBe(false);
  });

  it("returns false for a masked CPF with wrong check digit", () => {
    expect(isValidCpf(INVALID_CPF_WRONG_DIGIT_MASKED)).toBe(false);
  });

  it("returns false for all-same-digit CPFs", () => {
    for (const cpf of ALL_SAME_DIGIT_CPFS) {
      expect(isValidCpf(cpf), `all-same ${cpf}`).toBe(false);
    }
  });

  it("returns false for CPF with wrong length", () => {
    expect(isValidCpf("123456789")).toBe(false);
    expect(isValidCpf("1234567890123")).toBe(false);
  });

  it("returns false for empty string", () => {
    expect(isValidCpf("")).toBe(false);
  });
});

describe("cpfMask", () => {
  it("masks raw 11-digit CPF to XXX.XXX.XXX-XX format", () => {
    expect(cpfMask(VALID_CPF_RAW)).toBe(VALID_CPF_MASKED);
  });

  it("handles partial input without crashing", () => {
    const result = cpfMask("529");
    expect(typeof result).toBe("string");
    expect(result.startsWith("529")).toBe(true);
  });

  it("handles empty string", () => {
    expect(cpfMask("")).toBe("");
  });

  it("strips non-digits before masking", () => {
    const result = cpfMask("529.982.247-25");
    expect(result).toBe(VALID_CPF_MASKED);
  });
});
