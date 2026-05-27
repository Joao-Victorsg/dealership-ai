// lib/api/auth.ts
// BFF authentication client functions.
// Source: contracts/bff-api.md §Auth; research.md R-02

import { bffFetch } from "@/lib/api/client";
import type { BffResponse, RegisterRequest } from "@/lib/api/types";

/**
 * Completes the post-Keycloak registration by submitting the customer profile.
 * POST /api/v1/auth/register
 *
 * AUTHENTICATED — requires a valid SESSION cookie from the completed Keycloak
 * PKCE flow. The user must have gone through /oauth2/authorization/keycloak-register
 * before calling this.
 *
 * Throws BffError with code VALIDATION_ERROR (includes field-level details),
 * or DUPLICATE_IDENTITY if email already registered.
 */
export async function registerUser(body: RegisterRequest): Promise<void> {
  await bffFetch<BffResponse<null>>("/api/v1/auth/register", {
    method: "POST",
    body,
  });
}

/**
 * Invalidates the current session and initiates OIDC end-session with Keycloak.
 * POST /api/v1/auth/logout
 *
 * On success the BFF clears the SESSION cookie and returns 204.
 * On 401 (already logged out), this is treated as a success — caller should redirect to /.
 */
export async function logoutUser(): Promise<void> {
  await bffFetch<BffResponse<null>>("/api/v1/auth/logout", {
    method: "POST",
  });
}
