import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright E2E configuration.
 * A Node.js mock HTTP server (e2e/mock-bff-server.ts) handles BFF responses at
 * localhost:4000. It is started by global-setup.ts before the Next.js dev server
 * and stopped by global-teardown.ts after all tests complete.
 * Auth state is signalled to the mock server via the X-E2E-SESSION cookie.
 * @axe-core/playwright accessibility scans run inside every test.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: "html",

  globalSetup: "./e2e/global-setup.ts",
  globalTeardown: "./e2e/global-teardown.ts",

  use: {
    baseURL: process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000",
    trace: "on-first-retry",
  },

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "mobile",
      use: { ...devices["iPhone 12"] },
    },
  ],

  // Start the Next.js dev server with BFF_URL pointing to the mock server.
  // global-setup.ts starts the mock server on port 4000 before this command runs.
  webServer: {
    command: "npm run dev",
    url: process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
    env: {
      BFF_URL: "http://127.0.0.1:4000",
      NEXT_PUBLIC_BFF_URL: "http://127.0.0.1:4000",
    },
  },
});

