// stryker.config.mjs
// Mutation testing configuration for lib/ layer.
// Run: npm run mutate
// Thresholds: 90% mutation score, 90% line coverage, 90% branch coverage.

/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
export default {
  testRunner: "vitest",
  coverageAnalysis: "perTest",
  mutate: [
    "lib/**/*.ts",
    "!lib/**/*.d.ts",
    "!lib/api/types.ts", // Types-only file — no logic to mutate
  ],
  vitest: {
    configFile: "vitest.config.ts",
  },
  thresholds: {
    high: 90,
    low: 85,
    break: 80,
  },
  reporters: ["html", "clear-text", "progress"],
  htmlReporter: {
    fileName: "reports/mutation/mutation.html",
  },
};
