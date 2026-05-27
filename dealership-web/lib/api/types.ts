// lib/api/types.ts
// All BFF response shapes, entity types, and filter state.
// Source: data-model.md §1–§8

// ---------------------------------------------------------------------------
// §1. BFF Response Envelope
// ---------------------------------------------------------------------------

/** Successful BFF response */
export interface BffResponse<T> {
  data: T;
  meta: BffMeta;
}

/** BFF meta block (non-paginated) */
export interface BffMeta {
  timestamp: string; // ISO-8601
  requestId: string; // UUID — surfaced as support reference in error UI
}

/** BFF meta block (paginated) */
export interface BffPageMeta extends BffMeta {
  page: number; // zero-based
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

/** BFF error response */
export interface BffErrorResponse {
  error: {
    code: BffErrorCode;
    message: string;
    details?: BffFieldError[];
  };
  meta: BffMeta;
}

/** Field-level error detail (from VALIDATION_ERROR) */
export interface BffFieldError {
  field: string;
  message: string;
}

/** All BFF error codes */
export type BffErrorCode =
  | "CAR_NOT_AVAILABLE"
  | "VALIDATION_ERROR"
  | "AUTHENTICATION_REQUIRED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "RATE_LIMIT_EXCEEDED"
  | "DOWNSTREAM_UNAVAILABLE"
  | "DUPLICATE_IDENTITY"
  | "INTERNAL_ERROR";

// ---------------------------------------------------------------------------
// §2. Car / Inventory
// ---------------------------------------------------------------------------

export type CarStatus = "AVAILABLE" | "SOLD" | "UNAVAILABLE";
export type CarCategory =
  | "SEDAN"
  | "SUV"
  | "HATCHBACK"
  | "COUPE"
  | "CONVERTIBLE"
  | "MINIVAN"
  | "PICKUP"
  | "OTHER";
export type CarType = "ELECTRIC" | "HYBRID" | "GASOLINE" | "DIESEL";

/** Car as returned by GET /api/v1/inventory and GET /api/v1/inventory/{id} */
export interface Car {
  id: string; // UUID
  model: string;
  manufacturer: string;
  manufacturingYear: number;
  externalColor: string;
  internalColor: string;
  vin: string;
  status: CarStatus;
  category: CarCategory;
  type: CarType;
  isNew: boolean; // true = New condition, false = Pre-owned
  kilometers: number;
  propulsionType: string; // e.g., 'FRONT_WHEEL_DRIVE'
  listedValue: number; // BRL
  imageKey: string | null; // relative CDN key, e.g., "cars/{id}/main.jpg"
  optionalItems: string[];
  registrationDate: string; // ISO-8601 timestamp
}

/** Paginated inventory response */
export type CarListResponse = BffResponse<Car[]> & { meta: BffPageMeta };
export type CarDetailResponse = BffResponse<Car>;
export interface InventoryFilterOptions {
  manufacturers: string[];
  exteriorColors: string[];
}
export type InventoryFilterOptionsResponse = BffResponse<InventoryFilterOptions>;

/** Query params for GET /api/v1/inventory */
export interface InventoryQueryParams {
  q?: string;
  category?: CarCategory;
  type?: CarType;
  condition?: "NEW" | "USED";
  manufacturer?: string;
  yearMin?: number;
  yearMax?: number;
  priceMin?: number;
  priceMax?: number;
  color?: string;
  kmMin?: number;
  kmMax?: number;
  sortBy?: "PRICE" | "YEAR" | "REGISTRATION_DATE";
  sortDirection?: "ASC" | "DESC";
  page?: number;
  size?: number;
}

/** UI convenience type — passed to <CarCard> and <CarDetail> */
export interface CarCardProps {
  id: string;
  manufacturer: string;
  model: string;
  manufacturingYear: number;
  category: CarCategory;
  isNew: boolean;
  type: CarType;
  externalColor: string;
  kilometers: number;
  listedValue: number; // BRL — formatted by lib/format.ts in the component
  status: CarStatus;
  imageUrl: string | null; // full URL — NEXT_PUBLIC_CDN_URL + imageKey
}

// ---------------------------------------------------------------------------
// §3. Authentication
// ---------------------------------------------------------------------------

/**
 * POST /api/v1/auth/register request body.
 * AUTHENTICATED — requires SESSION cookie from completed Keycloak flow.
 */
export interface RegisterRequest {
  firstName: string;
  lastName: string;
  cpf: string; // 11 digits, no formatting
  phone: string; // 10 or 11 digits, no formatting
  cep: string; // 8 digits, no hyphen
  streetNumber: string; // non-blank
}

// POST /api/v1/auth/register success: BffResponse<null>
// POST /api/v1/auth/logout success: BffResponse<null> with 204

// ---------------------------------------------------------------------------
// §4. Customer Profile
// ---------------------------------------------------------------------------

export interface CustomerAddress {
  street: string;
  number: string; // NOT updatable via PATCH /api/v1/profile — see research.md R-04
  complement?: string;
  neighborhood: string;
  city: string;
  state: string; // UF, e.g., "SP"
  cep: string; // 8 digits
}

export interface CustomerProfile {
  id: string; // UUID
  firstName: string;
  lastName: string;
  cpf: string; // read-only — never send in PATCH body
  email: string; // from Keycloak JWT claim
  phone: string;
  createdAt: string; // ISO-8601
  address: CustomerAddress;
}

/**
 * PATCH /api/v1/profile request body.
 * DO NOT include cpf or streetNumber — BFF rejects both fields.
 * See research.md R-04.
 */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  cep?: string; // 8 digits, no hyphen
  // streetNumber: NOT supported by current BFF contract (see research.md R-04)
}

// ---------------------------------------------------------------------------
// §5. Purchases
// ---------------------------------------------------------------------------

/** Vehicle snapshot captured at purchase time — subset of Car */
export interface PurchaseVehicleSnapshot {
  id: string;
  model: string;
  manufacturer: string;
  manufacturingYear: number;
  externalColor: string;
  vin: string;
  category: CarCategory;
  listedValue: number;
  // NOTE: internalColor, type, optionalItems, imageKey are NOT in the BFF snapshot response.
  // See research.md R-07. These fields are intentionally absent.
}

export interface PurchaseClientSnapshot {
  firstName: string;
  lastName: string;
  cpf: string;
}

export interface Purchase {
  id: string; // UUID
  registeredAt: string; // ISO-8601 timestamp
  status: "COMPLETED";
  vehicle: PurchaseVehicleSnapshot;
  client: PurchaseClientSnapshot;
}

/** POST /api/v1/purchases request */
export interface CreatePurchaseRequest {
  carId: string; // UUID
}

/** POST /api/v1/purchases success */
export type CreatePurchaseResponse = BffResponse<Purchase>;

/** GET /api/v1/purchases success */
export type PurchaseListResponse = BffResponse<Purchase[]> & {
  meta: BffPageMeta;
};

// ---------------------------------------------------------------------------
// §6. Admin (Assumed Contracts — Pending BFF Spec)
// ---------------------------------------------------------------------------

/**
 * ⚠️ These types are based on assumed contracts (see research.md R-08).
 * Must be validated against the official BFF admin spec before deployment.
 */

/** Request body for POST /api/v1/admin/inventory */
export interface CreateCarRequest {
  manufacturer: string;
  model: string;
  manufacturingYear: number;
  type: CarType;
  category: CarCategory;
  isNew: boolean;
  listedValue: number;
  kilometers: number;
  externalColor: string;
  internalColor: string;
  optionalItems: string[];
  vin: string;
  // imageKey: assumed to be set separately via upload endpoint (TBD)
}

/** Request body for PATCH /api/v1/admin/inventory/{carId} — all fields optional */
export type UpdateCarRequest = Partial<CreateCarRequest> & {
  status?: CarStatus;
};

/** Sales report entry */
export interface SaleRecord {
  id: string;
  registeredAt: string;
  buyer: { firstName: string; lastName: string; cpf: string };
  vehicle: PurchaseVehicleSnapshot;
  listedValue: number;
  taxAmount: number; // assumed to be returned by admin endpoint
  finalValue: number; // assumed to be returned by admin endpoint
}

/** GET /api/v1/admin/sales — assumed query params */
export interface SalesQueryParams {
  from?: string; // ISO-8601 Instant
  to?: string; // ISO-8601 Instant
  page?: number;
  size?: number;
}

/** GET /api/v1/admin/sales — assumed KPI summary (embedded in or alongside list) */
export interface SalesKpiSummary {
  totalRevenue: number;
  totalCarsSold: number;
  averageSaleValue: number;
  currentMonthCount: number;
}

// ---------------------------------------------------------------------------
// §7. CEP Resolution
// ---------------------------------------------------------------------------

/**
 * BFF CEP lookup response.
 * ⚠️ Assumed contract — see contracts/bff-api.md §CEP Lookup.
 * Returned by GET /api/v1/cep/{cep} — called via lib/api/cep.ts.
 * BFF team must implement the endpoint.
 */
export type CepLookupResponse =
  | {
      ok: true;
      street: string;
      neighborhood: string;
      city: string;
      state: string;
    }
  | { ok: false; error: string };

/** Convenience alias used by form hooks and Server Actions */
export type CepLookupResult = CepLookupResponse;

// ---------------------------------------------------------------------------
// §8. Filter State
// ---------------------------------------------------------------------------

/** Client-side representation of active inventory filters (synced to URL params) */
export interface InventoryFilters {
  q: string;
  category: CarCategory | "";
  type: CarType | "";
  condition: "NEW" | "USED" | "";
  manufacturer: string;
  yearMin: string;
  yearMax: string;
  priceMin: string;
  priceMax: string;
  color: string;
  kmMin: string;
  kmMax: string;
  sortBy: "PRICE" | "YEAR" | "REGISTRATION_DATE";
  sortDirection: "ASC" | "DESC";
  page: number;
}

export const DEFAULT_FILTERS: InventoryFilters = {
  q: "",
  category: "",
  type: "",
  condition: "",
  manufacturer: "",
  yearMin: "",
  yearMax: "",
  priceMin: "",
  priceMax: "",
  color: "",
  kmMin: "",
  kmMax: "",
  sortBy: "REGISTRATION_DATE",
  sortDirection: "DESC",
  page: 0,
};
