// tests/unit/lib/utils/cep.test.ts
// Unit tests for lib/utils/cep.ts
// Source: tasks.md T095

import { describe, it, expect } from "vitest";
import { CEP_REGEX, CEP_REGEX_MASKED, cepMask, cepUnmask } from "@/lib/utils/cep";

describe("CEP_REGEX", () => {
  it("matches exactly 8 digits", () => {
    expect(CEP_REGEX.test("01310100")).toBe(true);
  });

  it("rejects 7 digits", () => {
    expect(CEP_REGEX.test("0131010")).toBe(false);
  });

  it("rejects 9 digits", () => {
    expect(CEP_REGEX.test("013101001")).toBe(false);
  });

  it("rejects masked CEP (with hyphen)", () => {
    expect(CEP_REGEX.test("01310-100")).toBe(false);
  });

  it("rejects letters", () => {
    expect(CEP_REGEX.test("0131010A")).toBe(false);
  });
});

describe("CEP_REGEX_MASKED", () => {
  it("matches XXXXX-XXX masked format", () => {
    expect(CEP_REGEX_MASKED.test("01310-100")).toBe(true);
  });

  it("rejects unmasked 8-digit CEP", () => {
    expect(CEP_REGEX_MASKED.test("01310100")).toBe(false);
  });

  it("rejects partial masked input", () => {
    expect(CEP_REGEX_MASKED.test("01310-10")).toBe(false);
  });

  it("rejects letters in masked format", () => {
    expect(CEP_REGEX_MASKED.test("0131A-100")).toBe(false);
  });
});

describe("cepMask", () => {
  it("masks 8-digit raw CEP to XXXXX-XXX", () => {
    expect(cepMask("01310100")).toBe("01310-100");
  });

  it("handles partial input (fewer than 5 digits) without hyphen", () => {
    const result = cepMask("0131");
    expect(result).toBe("0131");
    expect(result).not.toContain("-");
  });

  it("handles exactly 5 digits without trailing hyphen", () => {
    const result = cepMask("01310");
    expect(result).toBe("01310");
  });

  it("strips non-digits before masking", () => {
    expect(cepMask("01310-100")).toBe("01310-100");
  });

  it("returns empty string for empty input", () => {
    expect(cepMask("")).toBe("");
  });
});

describe("cepUnmask", () => {
  it("removes hyphen from masked CEP", () => {
    expect(cepUnmask("01310-100")).toBe("01310100");
  });

  it("returns raw CEP unchanged", () => {
    expect(cepUnmask("01310100")).toBe("01310100");
  });

  it("handles empty string", () => {
    expect(cepUnmask("")).toBe("");
  });
});
