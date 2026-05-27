// e2e/global-teardown.ts
// Playwright global teardown: gracefully shuts down the mock BFF server after all tests.

import type * as http from "http";

export default async function globalTeardown(): Promise<void> {
  const server = (
    process as NodeJS.Process & { __E2E_MOCK_BFF__?: http.Server }
  ).__E2E_MOCK_BFF__;
  if (server) {
    await new Promise<void>((resolve) => server.close(() => resolve()));
    console.log("[mock-bff] Server stopped.");
  }
}
