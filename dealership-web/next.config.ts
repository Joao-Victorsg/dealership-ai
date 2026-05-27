import type { NextConfig } from "next";

/**
 * CDN hostname — read at build time from NEXT_PUBLIC_CDN_URL.
 * Falls back to localhost:8080 (dev / CI) when the env var is absent.
 * OI-4: Set NEXT_PUBLIC_CDN_URL=https://cdn.dealership.example.com in production.
 */
function cdnHostname(): string {
  try {
    const raw = process.env.NEXT_PUBLIC_CDN_URL;
    if (raw) return new URL(raw).hostname;
  } catch {
    // ignore malformed URL
  }
  return "localhost";
}

const bffBase = process.env.NEXT_PUBLIC_BFF_URL ?? "https://app.localhost:4443";
const keycloakBase = process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? "https://auth.localhost:4443";
const isDev = process.env.NODE_ENV !== "production";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      // Local dev / BFF static assets
      {
        protocol: "https",
        hostname: "app.localhost",
        port: "4443",
        pathname: "/static/**",
      },
      {
        protocol: "http",
        hostname: "localhost",
        port: "8083",
        pathname: "/static/**",
      },
      // CDN — resolved from NEXT_PUBLIC_CDN_URL at build time (OI-4)
      ...(cdnHostname() !== "localhost"
        ? [
            {
              protocol: "https" as const,
              hostname: cdnHostname(),
              pathname: "/**",
            },
          ]
        : []),
    ],
  },

  // Server Actions are stable in Next.js 14+ — no experimental flag required.

  async headers() {
    const cdnHost = cdnHostname();
    const imgSrc =
      cdnHost !== "localhost"
        ? `img-src 'self' data: https://${cdnHost}`
        : `img-src 'self' data: ${bffBase}`;

    return [
      {
        source: "/(.*)",
        headers: [
          {
            key: "Content-Security-Policy",
            value: [
              "default-src 'self'",
              // Next.js App Router streaming uses inline scripts for hydration.
              // Keep unsafe-inline enabled in production so the app doesn't fall
              // back to non-hydrated shells (forms degrade to GET submissions).
              isDev
                ? "script-src 'self' 'unsafe-inline' 'unsafe-eval'"
                : "script-src 'self' 'unsafe-inline'",
              "style-src 'self' 'unsafe-inline'",
              imgSrc,
              "font-src 'self' https://fonts.gstatic.com",
              // BFF + optional Keycloak (extend when OIDC domain is confirmed)
              isDev
                ? `connect-src 'self' ws://localhost:* wss://app.localhost:4443 https://app.localhost:4443 https://api.localhost:4443 https://auth.localhost:4443 http://localhost:* ${bffBase} ${keycloakBase}`
                : `connect-src 'self' ${bffBase} ${keycloakBase}`,
              "frame-ancestors 'none'",
              "object-src 'none'",
              "base-uri 'self'",
              "form-action 'self'",
            ].join("; "),
          },
          {
            key: "X-Frame-Options",
            value: "DENY",
          },
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
          {
            key: "Strict-Transport-Security",
            value: "max-age=63072000; includeSubDomains; preload",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
