// e2e/inventory.spec.ts
// E2E tests for the inventory browse journey.
// Source: tasks.md T075; spec.md US1 (SC1-5)
//
// BFF responses come from the mock HTTP server at localhost:4000 (e2e/mock-bff-server.ts).
// The mock server is started by e2e/global-setup.ts before the Next.js dev server.
// No page.route() mocking is needed — the Next.js RSC fetches data server-side.
// NOTE: inventory uses React Suspense streaming — wait for networkidle to ensure
// the Suspense boundary resolves before asserting content.

import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.describe("inventory browse", () => {
  test("shows car list on /inventory", async ({ page }) => {
    await page.goto("/inventory", { waitUntil: "networkidle" });
    // Mock server always returns Toyota Corolla + Honda HR-V
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
    await expect(page.getByText("HR-V")).toBeVisible();
  });

  test("shows result count", async ({ page }) => {
    await page.goto("/inventory", { waitUntil: "networkidle" });
    await expect(page.getByText(/2 carro/i)).toBeVisible({ timeout: 15000 });
  });

  test("category filter narrows results — SUV", async ({ page }) => {
    await page.goto("/inventory?category=SUV", { waitUntil: "networkidle" });
    // Mock server filters by category — only Honda HR-V (SUV) returned
    await expect(page.getByText("HR-V")).toBeVisible({ timeout: 15000 });
    // Toyota Corolla (SEDAN) should not appear
    await expect(page.getByRole("heading", { name: /toyota corolla/i })).not.toBeVisible();
  });

  test("manufacturer and color filters expose backend options", async ({ page }) => {
    await page.goto("/inventory", { waitUntil: "networkidle" });
    await expect(page.getByRole("option", { name: "Toyota" })).toBeVisible({ timeout: 15000 });
    await expect(page.getByRole("option", { name: "Cinza Grafite" })).toBeVisible();
  });

  test("shows empty state when no results", async ({ page }) => {
    // CONVERTIBLE is not in the mock server inventory -> returns empty list
    await page.goto("/inventory?category=CONVERTIBLE", { waitUntil: "networkidle" });
    await expect(
      page.getByText(/nenhum veículo|nenhum resultado/i)
    ).toBeVisible({ timeout: 15000 });
  });

  test("mobile viewport — 375px", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto("/inventory", { waitUntil: "networkidle" });
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
  });

  test("axe-core accessibility scan", async ({ page }) => {
    await page.goto("/inventory", { waitUntil: "networkidle" });
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  test("axe-core accessibility scan — mobile", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto("/inventory", { waitUntil: "networkidle" });
    await expect(page.getByText("Toyota Corolla")).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });
});
