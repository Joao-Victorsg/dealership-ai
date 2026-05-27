// lib/api/admin.ts
// BFF admin client functions.
// ⚠️ All endpoints use assumed contracts — BFF admin spec not yet published.
// Source: research.md R-08; data-model.md §6; contracts/bff-api.md §Admin

import { bffFetch } from "@/lib/api/client";
import type {
  BffResponse,
  Car,
  CarListResponse,
  CreateCarRequest,
  SalesKpiSummary,
  SaleRecord,
  SalesQueryParams,
  UpdateCarRequest,
} from "@/lib/api/types";
import type { BffPageMeta } from "@/lib/api/types";

/** ⚠️ Assumed — admin sales response shape */
interface AdminSalesResponse {
  data: SaleRecord[];
  meta: BffPageMeta;
  kpi: SalesKpiSummary;
}

/**
 * Fetches the full car inventory for admin (all statuses).
 * GET /api/v1/admin/inventory
 * ⚠️ Assumed contract. Requires SESSION + ROLE_ADMIN.
 * Also used as admin role probe in middleware (200 = admin, 403 = non-admin).
 */
export async function getAdminInventory(): Promise<CarListResponse> {
  return bffFetch<CarListResponse>("/api/v1/admin/inventory");
}

/**
 * Creates a new car in the inventory.
 * POST /api/v1/admin/inventory
 * ⚠️ Assumed contract.
 */
export async function createCar(body: CreateCarRequest): Promise<Car> {
  const response = await bffFetch<BffResponse<Car>>(
    "/api/v1/admin/inventory",
    {
      method: "POST",
      body,
    }
  );
  return response.data;
}

/**
 * Updates an existing car.
 * PATCH /api/v1/admin/inventory/{carId}
 * ⚠️ Assumed contract.
 */
export async function updateCar(
  id: string,
  body: UpdateCarRequest
): Promise<Car> {
  const response = await bffFetch<BffResponse<Car>>(
    `/api/v1/admin/inventory/${encodeURIComponent(id)}`,
    {
      method: "PATCH",
      body,
    }
  );
  return response.data;
}

/**
 * Fetches the sales report with KPI summary.
 * GET /api/v1/admin/sales
 * ⚠️ Assumed contract.
 */
export async function getSalesReport(
  params: SalesQueryParams = {}
): Promise<AdminSalesResponse> {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      searchParams.append(key, String(value));
    }
  });
  const query = searchParams.toString();
  const path = query ? `/api/v1/admin/sales?${query}` : "/api/v1/admin/sales";
  return bffFetch<AdminSalesResponse>(path);
}
