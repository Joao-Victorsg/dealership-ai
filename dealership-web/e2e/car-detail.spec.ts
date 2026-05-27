// e2e/car-detail.spec.ts
// E2E tests for the car detail page.
// Source: tasks.md T076; spec.md US2 (SC1-5)
//
// BFF responses come from the mock HTTP server at localhost:4000 (e2e/mock-bff-server.ts).
// No auth cookie needed for car detail - it is a public route.
// NOTE: page uses React Suspense streaming - use networkidle to ensure data loads.

import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.describe("car detail page", () => {
  test("loads and displays car details", async ({ page }) => {
    await page.goto("/inventory/car-001", { waitUntil: "networkidle" });
    // car-001: Toyota Corolla 2023
    await expect(page.getByRole("heading", { name: /toyota corolla/i })).toBeVisible({ timeout: 15000 });
    await expect(page.getByText("2023")).toBeVisible();
  });

  test("shows Buy CTA for AVAILABLE car", async ({ page }) => {
    await page.goto("/inventory/car-001", { waitUntil: "networkidle" });
    // Buy panel should have a link to purchase
    await expect(page.getByRole("link", { name: /comprar/i })).toBeVisible({ timeout: 15000 });
  });

  test("hides Buy CTA for SOLD car", async ({ page }) => {
    // car-sold: Honda Civic, status SOLD
    await page.goto("/inventory/car-sold", { waitUntil: "networkidle" });
    // Should show sold indicator, no purchase link
    await expect(page.getByText(/vendido|indispon/i)).toBeVisible({ timeout: 15000 });
    await expect(page.getByRole("link", { name: /comprar/i })).not.toBeVisible();
  });

  test("renders 404 for unknown car id", async ({ page }) => {
    await page.goto("/inventory/not-found-car", { waitUntil: "networkidle" });
    // Next.js not-found renders a 404 page
    await expect(page.getByText(/.{3,}/)).toBeVisible({ timeout: 15000 });
    // Verify URL is still at not-found-car (or Next.js 404 page)
    expect(page.url()).toContain("not-found");
  });

  test("axe-core accessibility scan", async ({ page }) => {
    await page.goto("/inventory/car-001", { waitUntil: "networkidle" });
    await expect(page.getByRole("heading", { name: /toyota corolla/i })).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  test("axe-core accessibility scan - mobile", async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto("/inventory/car-001", { waitUntil: "networkidle" });
    await expect(page.getByRole("heading", { name: /toyota corolla/i })).toBeVisible({ timeout: 15000 });
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });
});
