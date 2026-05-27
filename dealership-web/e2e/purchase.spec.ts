// e2e/purchase.spec.ts
// E2E tests for the purchase confirmation flow.
// Source: tasks.md T077; spec.md US4 (SC1-8)
//
// BFF responses come from the mock HTTP server at localhost:4000 (e2e/mock-bff-server.ts).
// Auth signalled via X-E2E-SESSION cookie forwarded by Next.js middleware to the mock server:
//   "customer"  -> authenticated customer
// Car scenarios:
//   car-001     -> AVAILABLE Toyota Corolla, purchase succeeds (201)
//   car-racy    -> AVAILABLE Toyota Corolla, purchase returns 409 CAR_NOT_AVAILABLE

import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const SESSION_COOKIE = {
  name: "X-E2E-SESSION",
  value: "customer",
  domain: "localhost",
  path: "/",
  httpOnly: false,
  secure: false,
};

test.describe("purchase confirmation - happy path", () => {
  test.beforeEach(async ({ context }) => {
    await context.addCookies([SESSION_COOKIE]);
  });

  test("shows car and profile data on purchase page", async ({ page }) => {
    await page.goto("/purchase/car-001", { waitUntil: "networkidle" });
    // car-001: Toyota Corolla; profile: Joao Silva
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/jo.o silva/i)).toBeVisible({ timeout: 15000 });
  });

  test("shows financial summary", async ({ page }) => {
    await page.goto("/purchase/car-001", { waitUntil: "networkidle" });
    // car-001 listedValue: 120000 -> R$ 120.000; with 10% tax total: R$ 132.000
    await expect(page.getByText(/R\$\s*120[\.,]000/)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/R\$\s*132[\.,]000/)).toBeVisible();
  });

  test("redirects to success page after confirmation", async ({ page }) => {
    await page.goto("/purchase/car-001", { waitUntil: "networkidle" });
    await expect(page.getByRole("button", { name: /confirmar compra/i })).toBeVisible({ timeout: 15000 });
    await page.getByRole("button", { name: /confirmar compra/i }).click();
    await page.waitForURL("**/purchase/success");
    await expect(page.getByText(/compra realizada/i)).toBeVisible();
  });

  test("axe-core scan on purchase page", async ({ page }) => {
    await page.goto("/purchase/car-001", { waitUntil: "networkidle" });
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });
});

test.describe("purchase - CAR_NOT_AVAILABLE error (SC-007)", () => {
  test("shows inline targeted message and link to inventory", async ({
    page,
    context,
  }) => {
    await context.addCookies([SESSION_COOKIE]);
    // car-racy: appears AVAILABLE but mock server returns 409 on purchase submission
    await page.goto("/purchase/car-racy", { waitUntil: "networkidle" });
    await expect(page.getByRole("button", { name: /confirmar compra/i })).toBeVisible({ timeout: 15000 });
    await page.getByRole("button", { name: /confirmar compra/i }).click();

    // Inline error message (targeted inline per spec SC-007)
    await expect(page.getByText(/nao esta mais disponivel|nao esta disponivel|indisponivel/i)).toBeVisible({ timeout: 10000 });
    // Link back to inventory
    await expect(
      page.getByRole("link", { name: /ver outros veiculos|ver inventario/i })
    ).toBeVisible();
  });
});

test.describe("purchase - double-click prevention (SC-006)", () => {
  test("button is disabled after first click", async ({ page, context }) => {
    await context.addCookies([SESSION_COOKIE]);
    await page.goto("/purchase/car-001", { waitUntil: "networkidle" });
    const button = page.getByRole("button", { name: /confirmar compra/i });
    await expect(button).toBeVisible({ timeout: 15000 });
    await button.click();
    // Button should be disabled immediately after first click
    await expect(button).toBeDisabled();
  });
});
