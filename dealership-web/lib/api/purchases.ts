// lib/api/purchases.ts
// BFF purchase client functions.
// Source: contracts/bff-api.md §Purchase; data-model.md §5

import { bffFetch } from "@/lib/api/client";
import type {
  CreatePurchaseResponse,
  Purchase,
  PurchaseListResponse,
} from "@/lib/api/types";

/**
 * Confirms the purchase of a specific car.
 * POST /api/v1/purchases
 *
 * Requires SESSION cookie.
 * Throws BffError CAR_NOT_AVAILABLE (409) if another buyer completed the purchase
 * since the page was loaded — must be handled with a targeted inline message (spec US4 SC4).
 */
export async function confirmPurchase(carId: string): Promise<Purchase> {
  const response = await bffFetch<CreatePurchaseResponse>("/api/v1/purchases", {
    method: "POST",
    body: { carId },
  });
  return response.data;
}

/**
 * Retrieves the authenticated customer's purchase history, paginated.
 * GET /api/v1/purchases?page={page}
 *
 * Requires SESSION cookie.
 * Note: vehicle snapshot is partial — see research.md R-07 for missing fields.
 */
export async function getPurchases(page = 0): Promise<PurchaseListResponse> {
  return bffFetch<PurchaseListResponse>(
    `/api/v1/purchases?page=${encodeURIComponent(String(page))}`
  );
}
