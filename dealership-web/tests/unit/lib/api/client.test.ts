// tests/unit/lib/api/client.test.ts
// Unit tests for lib/api/client.ts
// Source: tasks.md T090

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { bffFetch, BffError } from "@/lib/api/client";
import type { BffErrorResponse } from "@/lib/api/types";

// ---------------------------------------------------------------------------
// MSW server
// ---------------------------------------------------------------------------

const TEST_BASE = "http://localhost:8080";

const server = setupServer();

beforeEach(() => {
  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  server.resetHandlers();
});

afterEach(() => {
  server.close();
});

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function bffErrorBody(
  code: string,
  message: string,
  requestId = "req-123"
): BffErrorResponse {
  return {
    meta: { requestId, timestamp: new Date().toISOString() },
    error: { code: code as never, message },
  };
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("bffFetch", () => {
  describe("URL routing", () => {
    it("uses BFF_URL on the server side", async () => {
      vi.stubEnv("BFF_URL", TEST_BASE);
      vi.stubEnv("NEXT_PUBLIC_BFF_URL", "");
      // Mock window to simulate server environment
      const originalWindow = global.window;
      // @ts-expect-error -- simulate server: window is undefined
      delete global.window;

      server.use(
        http.get(`${TEST_BASE}/api/v1/ping`, () =>
          HttpResponse.json({ data: "pong", meta: { requestId: "r1" } })
        )
      );

      const result = await bffFetch<{ data: string }>("/api/v1/ping");
      expect(result).toEqual({ data: "pong", meta: { requestId: "r1" } });

      // @ts-expect-error -- restore
      global.window = originalWindow;
      vi.unstubAllEnvs();
    });

    it("uses NEXT_PUBLIC_BFF_URL on the client side", async () => {
      vi.stubEnv("NEXT_PUBLIC_BFF_URL", TEST_BASE);

      server.use(
        http.get(`${TEST_BASE}/api/v1/ping`, () =>
          HttpResponse.json({ data: "pong", meta: { requestId: "r2" } })
        )
      );

      const result = await bffFetch<{ data: string }>("/api/v1/ping");
      expect(result).toEqual({ data: "pong", meta: { requestId: "r2" } });

      vi.unstubAllEnvs();
    });

    it("throws if NEXT_PUBLIC_BFF_URL is not set on client", async () => {
      vi.stubEnv("NEXT_PUBLIC_BFF_URL", "");
      await expect(bffFetch("/api/v1/ping")).rejects.toThrow(
        "NEXT_PUBLIC_BFF_URL"
      );
      vi.unstubAllEnvs();
    });
  });

  describe("happy path", () => {
    beforeEach(() => {
      vi.stubEnv("NEXT_PUBLIC_BFF_URL", TEST_BASE);
    });

    afterEach(() => {
      vi.unstubAllEnvs();
    });

    it("parses JSON response on 200", async () => {
      server.use(
        http.get(`${TEST_BASE}/api/v1/inventory`, () =>
          HttpResponse.json({ data: [], meta: { requestId: "r3" } })
        )
      );

      const result = await bffFetch<{ data: unknown[] }>("/api/v1/inventory");
      expect(result.data).toEqual([]);
    });

    it("returns null on 204 No Content", async () => {
      server.use(
        http.post(`${TEST_BASE}/api/v1/auth/logout`, () =>
          new HttpResponse(null, { status: 204 })
        )
      );

      const result = await bffFetch("/api/v1/auth/logout", { method: "POST" });
      expect(result).toBeNull();
    });

    it("sends Content-Type when body is provided", async () => {
      let contentType: string | null = null;

      server.use(
        http.post(`${TEST_BASE}/api/v1/purchases`, ({ request }) => {
          contentType = request.headers.get("content-type");
          return HttpResponse.json({ data: { id: "p1" }, meta: { requestId: "r4" } });
        })
      );

      await bffFetch("/api/v1/purchases", {
        method: "POST",
        body: { carId: "car-1" },
      });

      expect(contentType).toContain("application/json");
    });
  });

  describe("error handling", () => {
    beforeEach(() => {
      vi.stubEnv("NEXT_PUBLIC_BFF_URL", TEST_BASE);
    });

    afterEach(() => {
      vi.unstubAllEnvs();
    });

    it("throws BffError with typed code on 4xx with BffErrorResponse body", async () => {
      server.use(
        http.get(`${TEST_BASE}/api/v1/inventory/bad-id`, () =>
          HttpResponse.json(bffErrorBody("NOT_FOUND", "Car not found"), {
            status: 404,
          })
        )
      );

      await expect(bffFetch("/api/v1/inventory/bad-id")).rejects.toSatisfy(
        (err: unknown) => {
          expect(err).toBeInstanceOf(BffError);
          const bffErr = err as BffError;
          expect(bffErr.code).toBe("NOT_FOUND");
          expect(bffErr.requestId).toBe("req-123");
          return true;
        }
      );
    });

    it("throws AUTHENTICATION_REQUIRED on 401 with error body", async () => {
      server.use(
        http.get(`${TEST_BASE}/api/v1/profile`, () =>
          HttpResponse.json(
            bffErrorBody("AUTHENTICATION_REQUIRED", "Auth required"),
            { status: 401 }
          )
        )
      );

      const err = await bffFetch("/api/v1/profile").catch((e: unknown) => e);
      expect(err).toBeInstanceOf(BffError);
      expect((err as BffError).code).toBe("AUTHENTICATION_REQUIRED");
    });

    it("throws AUTHENTICATION_REQUIRED on bare 401 (no body)", async () => {
      server.use(
        http.get(`${TEST_BASE}/api/v1/profile`, () =>
          new HttpResponse(null, { status: 401 })
        )
      );

      const err = await bffFetch("/api/v1/profile").catch((e: unknown) => e);
      expect(err).toBeInstanceOf(BffError);
      expect((err as BffError).code).toBe("AUTHENTICATION_REQUIRED");
    });

    it("throws INTERNAL_ERROR on 500 without parseable body", async () => {
      server.use(
        http.get(`${TEST_BASE}/api/v1/inventory`, () =>
          new HttpResponse("Internal Server Error", {
            status: 500,
            headers: { "Content-Type": "text/plain" },
          })
        )
      );

      const err = await bffFetch("/api/v1/inventory").catch((e: unknown) => e);
      expect(err).toBeInstanceOf(BffError);
      expect((err as BffError).code).toBe("INTERNAL_ERROR");
    });

    it("includes field details on VALIDATION_ERROR", async () => {
      server.use(
        http.post(`${TEST_BASE}/api/v1/auth/register`, () =>
          HttpResponse.json(
            {
              data: null,
              meta: { requestId: "req-v" },
              error: {
                code: "VALIDATION_ERROR",
                message: "Invalid input",
                details: [{ field: "cpf", message: "CPF inválido" }],
              },
            },
            { status: 422 }
          )
        )
      );

      const err = await bffFetch("/api/v1/auth/register", {
        method: "POST",
        body: {},
      }).catch((e: unknown) => e);

      expect(err).toBeInstanceOf(BffError);
      const bffErr = err as BffError;
      expect(bffErr.code).toBe("VALIDATION_ERROR");
      expect(bffErr.details).toHaveLength(1);
      expect(bffErr.details?.[0].field).toBe("cpf");
    });
  });
});
