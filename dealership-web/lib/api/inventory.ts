// lib/api/inventory.ts
// BFF inventory client functions.
// Source: contracts/bff-api.md §Inventory; data-model.md §2

import { bffFetch } from "@/lib/api/client";
import type {
  CarListResponse,
  CarDetailResponse,
  InventoryFilterOptionsResponse,
  InventoryQueryParams,
} from "@/lib/api/types";

/**
 * Fetches a paginated, filtered list of cars from the BFF.
 * GET /api/v1/inventory
 * Public endpoint — no authentication required.
 */
export async function getInventory(
  params: InventoryQueryParams = {}
): Promise<CarListResponse> {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "" && value !== null) {
      searchParams.append(key, String(value));
    }
  });

  const query = searchParams.toString();
  const path = query ? `/api/v1/inventory?${query}` : "/api/v1/inventory";
  return bffFetch<CarListResponse>(path);
}

/**
 * Fetches a single car by its ID.
 * GET /api/v1/inventory/{id}
 * Public endpoint — no authentication required.
 * Throws BffError with code NOT_FOUND if the car does not exist.
 */
export async function getCarById(id: string): Promise<CarDetailResponse> {
  return bffFetch<CarDetailResponse>(`/api/v1/inventory/${encodeURIComponent(id)}`);
}

/**
 * Fetches available inventory filter options.
 * GET /api/v1/inventory/filter-options
 * Public endpoint — no authentication required.
 */
export async function getInventoryFilterOptions(): Promise<InventoryFilterOptionsResponse> {
  return bffFetch<InventoryFilterOptionsResponse>("/api/v1/inventory/filter-options");
}
