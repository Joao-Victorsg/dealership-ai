// e2e/auth-redirect.spec.ts
// E2E tests for authentication redirect flows.
// Source: tasks.md T053; spec.md US3 (SC1-4); research.md R-02, OI-1, OI-2
//
// The Next.js middleware probes BFF server-side (HEAD /api/v1/profile).
// page.route() cannot intercept server-side Node.js fetches, so auth state is
// signalled via the X-E2E-SESSION cookie, which the middleware forwards to the
// mock BFF server (e2e/mock-bff-server.ts):
//   absent / "none"  -> BFF returns 401 (unauthenticated)
//   "customer"       -> BFF returns 200 for profile, 403 for admin inventory
//   "needs-registration" -> BFF returns 403 for profile until onboarding completes
//   "admin"          -> BFF returns 200 for both probes

import { test, expect } from "@playwright/test";

const E2E_SESSION_COOKIE = {
  name: "X-E2E-SESSION",
  url: process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000",
  httpOnly: false,
  secure: false,
};

// --- Unauthenticated user redirects -----------------------------------------

const PROTECTED_ROUTES = [
  "/account",
  "/account/purchases",
  "/purchase/car-001",
  "/admin",
  "/admin/inventory",
];

test.describe("unauthenticated redirects", () => {
  // Default: no X-E2E-SESSION cookie -> mock BFF returns 401 for auth probe
  for (const route of PROTECTED_ROUTES) {
    test("redirects unauthenticated user away from " + route, async ({ page }) => {
      await page.goto(route, { waitUntil: "commit" });
      const finalUrl = page.url();
      // Should be redirected to login or homepage — not still at the protected route
      const routePath = new URL(route, "http://localhost:3000").pathname;
      expect(finalUrl).not.toMatch(new RegExp("^http://localhost:3000" + routePath.replace("/", "\\/") + "($|\\?)"));
    });
  }
});

// --- Non-admin blocked from admin routes ------------------------------------

test.describe("non-admin blocked from admin routes", () => {
  test.beforeEach(async ({ context }) => {
    await context.addCookies([{ ...E2E_SESSION_COOKIE, value: "customer" }]);
  });

  const adminRoutes = ["/admin", "/admin/inventory"];

  for (const route of adminRoutes) {
    test("blocks non-admin from " + route, async ({ page }) => {
      await page.goto(route, { waitUntil: "commit" });
      const finalUrl = page.url();
      // Should be redirected away from admin
      expect(finalUrl).not.toContain("/admin");
    });
  }
});

// --- Admin can access admin routes ------------------------------------------

test.describe("admin can access admin routes", () => {
  test.beforeEach(async ({ context }) => {
    await context.addCookies([{ ...E2E_SESSION_COOKIE, value: "admin" }]);
  });

  test("admin can navigate to /admin", async ({ page }) => {
    await page.goto("/admin", { waitUntil: "commit" });
    expect(page.url()).toContain("/admin");
  });
});

// --- Complete-registration requires authentication ---------------------------

test.describe("complete-registration requires auth", () => {
  test("unauthenticated user is redirected away from complete-registration", async ({
    page,
  }) => {
    await page.goto("/complete-registration", { waitUntil: "commit" });
    const finalUrl = page.url();
    expect(finalUrl).not.toContain("/complete-registration");
  });
});

test.describe("needs-registration users stay in onboarding", () => {
  test.beforeEach(async ({ context }) => {
    await context.addCookies([
      { ...E2E_SESSION_COOKIE, value: "needs-registration" },
    ]);
  });

  test("redirects protected customer route to complete-registration", async ({
    page,
  }) => {
    await page.goto("/account", { waitUntil: "commit" });
    expect(page.url()).toContain("/complete-registration");
  });

  test("allows incomplete user to stay on complete-registration", async ({
    page,
  }) => {
    await page.goto("/complete-registration", { waitUntil: "commit" });
    expect(page.url()).toContain("/complete-registration");
  });
});
