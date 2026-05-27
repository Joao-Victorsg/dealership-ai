import "server-only";

import { headers } from "next/headers";
import type { BffResponse, CustomerProfile } from "@/lib/api/types";

export interface HeaderUserSummary {
  firstName: string;
  lastName: string;
  email: string;
}

export interface HeaderAuthState {
  isAuthenticated: boolean;
  isAdmin: boolean;
  canAccessAccount: boolean;
  user: HeaderUserSummary | null;
}

const UNAUTHENTICATED_HEADER_STATE: HeaderAuthState = {
  isAuthenticated: false,
  isAdmin: false,
  canAccessAccount: false,
  user: null,
};

export async function getHeaderAuthState(): Promise<HeaderAuthState> {
  const bffUrl = process.env.BFF_URL;
  if (!bffUrl) {
    return UNAUTHENTICATED_HEADER_STATE;
  }

  try {
    const requestHeaders = await headers();
    const cookieHeader = requestHeaders.get("cookie") ?? "";

    const profilePromise = fetch(`${bffUrl}/api/v1/profile`, {
      method: "GET",
      cache: "no-store",
      headers: {
        cookie: cookieHeader,
      },
    });

    const adminPromise = fetch(`${bffUrl}/api/v1/admin/inventory`, {
      method: "GET",
      cache: "no-store",
      headers: {
        cookie: cookieHeader,
      },
    });

    const [profileResponse, adminResponse] = await Promise.all([
      profilePromise,
      adminPromise,
    ]);

    const isAuthenticated =
      profileResponse.status === 200 || profileResponse.status === 403;
    const canAccessAccount = profileResponse.status === 200;
    const isAdmin = isAuthenticated && adminResponse.status === 200;

    let user: HeaderUserSummary | null = null;
    if (canAccessAccount) {
      const profilePayload =
        (await profileResponse.json()) as BffResponse<CustomerProfile>;
      if (profilePayload?.data) {
        user = {
          firstName: profilePayload.data.firstName,
          lastName: profilePayload.data.lastName,
          email: profilePayload.data.email,
        };
      }
    }

    return { isAuthenticated, isAdmin, canAccessAccount, user };
  } catch {
    return UNAUTHENTICATED_HEADER_STATE;
  }
}
