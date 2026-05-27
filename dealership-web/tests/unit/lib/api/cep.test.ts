// tests/unit/lib/api/cep.test.ts
// Unit tests for lib/api/cep.ts — lookupCep()
// Source: tasks.md T097

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { vi } from "vitest";
import { lookupCep } from "@/lib/api/cep";

const TEST_BASE = "http://localhost:8080";

const server = setupServer();

beforeEach(() => {
  server.listen({ onUnhandledRequest: "error" });
  vi.stubEnv("NEXT_PUBLIC_BFF_URL", TEST_BASE);
});

afterEach(() => {
  server.resetHandlers();
  vi.unstubAllEnvs();
});

afterEach(() => {
  server.close();
});

describe("lookupCep", () => {
  it("returns { ok: true, ...address } on 200 success", async () => {
    server.use(
      http.get(`${TEST_BASE}/api/v1/cep/01310100`, () =>
        HttpResponse.json({
          data: {
            street: "Avenida Paulista",
            neighborhood: "Bela Vista",
            city: "São Paulo",
            state: "SP",
          },
          meta: { requestId: "cep-1" },
        })
      )
    );

    const result = await lookupCep("01310100");
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.street).toBe("Avenida Paulista");
      expect(result.neighborhood).toBe("Bela Vista");
      expect(result.city).toBe("São Paulo");
      expect(result.state).toBe("SP");
    }
  });

  it("returns { ok: false, error } on BFF 404 NOT_FOUND", async () => {
    server.use(
      http.get(`${TEST_BASE}/api/v1/cep/99999999`, () =>
        HttpResponse.json(
          {
            data: null,
            meta: { requestId: "cep-404" },
            error: { code: "NOT_FOUND", message: "CEP not found" },
          },
          { status: 404 }
        )
      )
    );

    const result = await lookupCep("99999999");
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.error).toBeTruthy();
      expect(typeof result.error).toBe("string");
    }
  });

  it("returns { ok: false, error } on DOWNSTREAM_UNAVAILABLE", async () => {
    server.use(
      http.get(`${TEST_BASE}/api/v1/cep/01310100`, () =>
        HttpResponse.json(
          {
            data: null,
            meta: { requestId: "cep-down" },
            error: {
              code: "DOWNSTREAM_UNAVAILABLE",
              message: "ViaCEP unavailable",
            },
          },
          { status: 503 }
        )
      )
    );

    const result = await lookupCep("01310100");
    expect(result.ok).toBe(false);
    if (!result.ok) {
      // Message should mention service unavailability
      expect(result.error.toLowerCase()).toMatch(/indispon|serviço|cep/);
    }
  });

  it("returns { ok: false, error } on network failure", async () => {
    server.use(
      http.get(`${TEST_BASE}/api/v1/cep/01310100`, () =>
        HttpResponse.error()
      )
    );

    const result = await lookupCep("01310100");
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(typeof result.error).toBe("string");
    }
  });

  it("encodes the CEP in the URL path", async () => {
    let capturedUrl = "";
    server.use(
      http.get(`${TEST_BASE}/api/v1/cep/:cep`, ({ params }) => {
        capturedUrl = params.cep as string;
        return HttpResponse.json({
          data: {
            street: "Rua Teste",
            neighborhood: "Centro",
            city: "Rio de Janeiro",
            state: "RJ",
          },
          meta: { requestId: "cep-enc" },
        });
      })
    );

    await lookupCep("20040020");
    expect(capturedUrl).toBe("20040020");
  });
});
