// proxy.ts
// Route protection via BFF probe strategy.
// OI-1: SESSION cookie is HttpOnly — cannot be read from JS.
//       Strategy: probe GET /api/v1/profile (401 = unauthenticated, 403 = profile incomplete).
// OI-2: Admin role detection: GET /api/v1/admin/inventory (403 = non-admin, 200 = admin).
// Note: BFF_URL is used for server-to-server calls; never exposed to the client.

import { type NextRequest, NextResponse } from "next/server";

function getBffUrl(): string | null {
  return process.env.BFF_URL ?? null;
}

/** Routes that require an authenticated session */
const CUSTOMER_PATHS = ["/account", "/purchase", "/complete-registration"];

/** Routes that require ROLE_ADMIN */
const ADMIN_PATH_PREFIX = "/admin";

function isCustomerPath(pathname: string): boolean {
  return CUSTOMER_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`));
}

function isAdminPath(pathname: string): boolean {
  return pathname === ADMIN_PATH_PREFIX || pathname.startsWith(`${ADMIN_PATH_PREFIX}/`);
}

type ProfileAccess = "unauthenticated" | "registered" | "needs_registration";

/**
 * Probes /api/v1/profile to infer session and registration state.
 */
async function getProfileAccess(request: NextRequest): Promise<ProfileAccess> {
  const bffUrl = getBffUrl();
  if (!bffUrl) return "unauthenticated";
  try {
    const response = await fetch(`${bffUrl}/api/v1/profile`, {
      method: "GET",
      cache: "no-store",
      headers: {
        cookie: request.headers.get("cookie") ?? "",
      },
    });
    if (response.status === 200) return "registered";
    if (response.status === 403) return "needs_registration";
    return "unauthenticated";
  } catch {
    // BFF unreachable — safest fallback is unauthenticated.
    return "unauthenticated";
  }
}

/**
 * Probes the BFF admin inventory to check ROLE_ADMIN.
 * Returns true if the user has the admin role.
 */
async function isAdmin(request: NextRequest): Promise<boolean> {
  const bffUrl = getBffUrl();
  if (!bffUrl) return false;
  try {
    const response = await fetch(`${bffUrl}/api/v1/admin/inventory`, {
      method: "GET",
      cache: "no-store",
      headers: {
        cookie: request.headers.get("cookie") ?? "",
      },
    });
    return response.status === 200;
  } catch {
    return false;
  }
}

/**
 * Builds the Keycloak login redirect URL.
 * Falls back to "/" if NEXT_PUBLIC_BFF_URL is not configured, to avoid
 * a redirect loop where "undefined/oauth2/..." resolves back into /admin/*.
 */
function buildLoginUrl(request: NextRequest, pathname: string): URL {
  const bffPublicUrl = process.env.NEXT_PUBLIC_BFF_URL;
  if (!bffPublicUrl) {
    // BFF URL not configured (e.g., E2E without env vars) — redirect to home
    return new URL("/", request.url);
  }
  // bffPublicUrl is a full URL, so new URL(full) ignores the base — no loop risk
  const loginUrl = new URL(
    `${bffPublicUrl}/oauth2/authorization/keycloak`
  );
  loginUrl.searchParams.set("redirect", pathname);
  return loginUrl;
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const completeRegistrationPath = "/complete-registration";

  if (isAdminPath(pathname)) {
    const [profileAccess, admin] = await Promise.all([
      getProfileAccess(request),
      isAdmin(request),
    ]);
    if (profileAccess === "unauthenticated") {
      return NextResponse.redirect(buildLoginUrl(request, pathname));
    }
    if (!admin) {
      return NextResponse.redirect(new URL("/", request.url));
    }
  }

  if (isCustomerPath(pathname)) {
    const profileAccess = await getProfileAccess(request);
    if (profileAccess === "unauthenticated") {
      return NextResponse.redirect(buildLoginUrl(request, pathname));
    }
    const isCompleteRegistrationRoute =
      pathname === completeRegistrationPath ||
      pathname.startsWith(`${completeRegistrationPath}/`);
    if (profileAccess === "registered" && isCompleteRegistrationRoute) {
      return NextResponse.redirect(new URL("/inventory", request.url));
    }
    if (profileAccess === "needs_registration" && !isCompleteRegistrationRoute) {
      return NextResponse.redirect(new URL(completeRegistrationPath, request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths EXCEPT:
     * - _next/static (static files)
     * - _next/image (image optimization)
     * - favicon.ico
     * - public folder assets
     */
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
