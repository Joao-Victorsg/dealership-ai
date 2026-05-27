// tests/unit/lib/errors.test.ts
// Unit tests for lib/errors.ts — getBFFErrorMessage()
// Source: tasks.md T091

import { describe, it, expect } from "vitest";
import { getBFFErrorMessage } from "@/lib/errors";
import type { BffErrorCode } from "@/lib/api/types";

const ALL_KNOWN_CODES: BffErrorCode[] = [
  "NOT_FOUND",
  "AUTHENTICATION_REQUIRED",
  "FORBIDDEN",
  "VALIDATION_ERROR",
  "DUPLICATE_IDENTITY",
  "CAR_NOT_AVAILABLE",
  "DOWNSTREAM_UNAVAILABLE",
  "RATE_LIMIT_EXCEEDED",
  "INTERNAL_ERROR",
];

describe("getBFFErrorMessage", () => {
  it("returns a non-empty Portuguese string for every known error code", () => {
    for (const code of ALL_KNOWN_CODES) {
      const message = getBFFErrorMessage(code);
      expect(message, `code ${code}`).toBeTruthy();
      expect(typeof message).toBe("string");
      expect(message.length).toBeGreaterThan(5);
    }
  });

  it("returns a generic fallback for unknown codes", () => {
    const message = getBFFErrorMessage("UNKNOWN_CODE_XYZ");
    expect(message).toBeTruthy();
    expect(typeof message).toBe("string");
  });

  it("returns distinct messages for different codes", () => {
    const messages = ALL_KNOWN_CODES.map((c) => getBFFErrorMessage(c));
    const unique = new Set(messages);
    // All messages should be unique
    expect(unique.size).toBe(ALL_KNOWN_CODES.length);
  });

  it("message for NOT_FOUND references unavailable or not found", () => {
    const msg = getBFFErrorMessage("NOT_FOUND").toLowerCase();
    expect(msg.includes("encontr") || msg.includes("disponív")).toBe(true);
  });

  it("message for AUTHENTICATION_REQUIRED references login or session", () => {
    const msg = getBFFErrorMessage("AUTHENTICATION_REQUIRED").toLowerCase();
    expect(
      msg.includes("sessão") ||
        msg.includes("login") ||
        msg.includes("autent") ||
        msg.includes("entrar")
    ).toBe(true);
  });

  it("message for CAR_NOT_AVAILABLE references car or purchase", () => {
    const msg = getBFFErrorMessage("CAR_NOT_AVAILABLE").toLowerCase();
    expect(
      msg.includes("veículo") ||
        msg.includes("carro") ||
        msg.includes("compra") ||
        msg.includes("disponív")
    ).toBe(true);
  });

  it("message for DUPLICATE_IDENTITY references existing registration", () => {
    const msg = getBFFErrorMessage("DUPLICATE_IDENTITY").toLowerCase();
    expect(
      msg.includes("cpf") ||
        msg.includes("cadastrad") ||
        msg.includes("e-mail") ||
        msg.includes("conta") ||
        msg.includes("login") ||
        msg.includes("existente")
    ).toBe(true);
  });
});
