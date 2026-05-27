// e2e/purchase-history.spec.ts
// E2E tests for the purchase history page.
// Source: tasks.md T078; spec.md US6 (SC1-3)
//
// Auth signalled via X-E2E-SESSION cookie forwarded to the mock BFF server:
//   "customer"        -> authenticated, has 1 purchase (Honda Civic)
//   "customer-empty"  -> authenticated, has 0 purchases

import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const BASE_COOKIE = {
  domain: "localhost",
  path: "/",
  httpOnly: false,
  secure: false,
};

test.describe("purchase history page", () => {
  test("shows purchase list", async ({ page, context }) => {
    await context.addCookies([
      { ...BASE_COOKIE, name: "X-E2E-SESSION", value: "customer" },
    ]);
    await page.goto("/account/purchases", { waitUntil: "networkidle" });
    // Mock server returns Honda Civic purchase for "customer" persona
    await expect(page.getByText("Honda Civic")).toBeVisible({ timeout: 15000 });
    await expect(page.getByText("2022")).toBeVisible();
  });

  test("shows empty state with inventory link", async ({ page, context }) => {
    await context.addCookies([
      { ...BASE_COOKIE, name: "X-E2E-SESSION", value: "customer-empty" },
    ]);
    await page.goto("/account/purchases", { waitUntil: "networkidle" });
    await expect(page.getByText(/nenhuma compra/i)).toBeVisible({ timeout: 15000 });
    await expect(
      page.getByRole("link", { name: /ver veiculos|ver nosso inventario/i })
    ).toBeVisible();
  });

  test("axe-core scan - desktop", async ({ page, context }) => {
    await context.addCookies([
      { ...BASE_COOKIE, name: "X-E2E-SESSION", value: "customer" },
    ]);
    await page.goto("/account/purchases", { waitUntil: "networkidle" });
    await expect(page.getByText("Honda Civic")).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  test("axe-core scan - mobile 375px", async ({ page, context }) => {
    await context.addCookies([
      { ...BASE_COOKIE, name: "X-E2E-SESSION", value: "customer" },
    ]);
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto("/account/purchases", { waitUntil: "networkidle" });
    await expect(page.getByText("Honda Civic")).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });
});
