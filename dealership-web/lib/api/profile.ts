// lib/api/profile.ts
// BFF customer profile client functions.
// Source: contracts/bff-api.md §Profile; research.md R-04; data-model.md §4

import { bffFetch } from "@/lib/api/client";
import type {
  BffResponse,
  CustomerProfile,
  UpdateProfileRequest,
} from "@/lib/api/types";

/**
 * Fetches the authenticated customer's profile.
 * GET /api/v1/profile
 * Requires SESSION cookie. Throws BffError AUTHENTICATION_REQUIRED on 401.
 */
export async function getProfile(): Promise<CustomerProfile> {
  const response = await bffFetch<BffResponse<CustomerProfile>>(
    "/api/v1/profile"
  );
  return response.data;
}

/**
 * Updates the authenticated customer's profile.
 * PATCH /api/v1/profile
 *
 * Accepted fields: firstName, lastName, phone, cep.
 * DO NOT include cpf or streetNumber — BFF rejects both (see research.md R-04).
 *
 * Throws BffError VALIDATION_ERROR (with field details) on invalid input.
 */
export async function updateProfile(
  body: UpdateProfileRequest
): Promise<CustomerProfile> {
  const response = await bffFetch<BffResponse<CustomerProfile>>(
    "/api/v1/profile",
    {
      method: "PATCH",
      body,
    }
  );
  return response.data;
}
