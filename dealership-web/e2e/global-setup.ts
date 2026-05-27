// e2e/global-setup.ts
// Playwright global setup: starts the mock BFF HTTP server before tests run.
// The server URL is baked into the Next.js dev server via webServer.env in playwright.config.ts.

import { startMockBffServer } from "./mock-bff-server";
import type * as http from "http";

export default async function globalSetup(): Promise<void> {
  const server = await startMockBffServer();
  // Store on process so global-teardown can shut it down.
  // Both hooks run in the same Playwright runner process.
  (process as NodeJS.Process & { __E2E_MOCK_BFF__?: http.Server }).__E2E_MOCK_BFF__ = server;
}
