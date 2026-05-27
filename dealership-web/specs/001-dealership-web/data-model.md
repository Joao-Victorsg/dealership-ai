# Data Model: dealership-web — Full Web Application

**Phase**: Phase 1  
**Date**: 2026-05-01  
**Branch**: `001-dealership-web`  
**Source**: BFF contracts + research.md

---

## Overview

All data flows from the dealership-bff REST API. The web application defines
TypeScript types that mirror the BFF response shapes. Server-side types live
in `lib/api/types.ts` (used in Server Components and Server Actions). Client
components receive narrowed, UI-friendly shapes as props — not raw BFF responses.

---

## 1. BFF Response Envelope

```typescript
// lib/api/types.ts

/** Successful BFF response */
export interface BffResponse<T> {
  data: T;
  meta: BffMeta;
}

/** BFF meta block (non-paginated) */
export interface BffMeta {
  timestamp: string;      // ISO-8601
  requestId: string;      // UUID — surfaced as support reference in error UI
}

/** BFF meta block (paginated) */
export interface BffPageMeta extends BffMeta {
  page: number;           // zero-based
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
  | 'CAR_NOT_AVAILABLE'
  | 'VALIDATION_ERROR'
  | 'AUTHENTICATION_REQUIRED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'RATE_LIMIT_EXCEEDED'
  | 'DOWNSTREAM_UNAVAILABLE'
  | 'DUPLICATE_IDENTITY'
  | 'INTERNAL_ERROR';
```

---

## 2. Car / Inventory

```typescript
// lib/api/types.ts (continued)

export type CarStatus = 'AVAILABLE' | 'SOLD' | 'UNAVAILABLE';
export type CarCategory = 'SEDAN' | 'SUV' | 'HATCHBACK' | 'COUPE' | 'CONVERTIBLE' | 'MINIVAN' | 'PICKUP' | 'OTHER';
export type CarType = 'ELECTRIC' | 'HYBRID' | 'GASOLINE' | 'DIESEL';

/** Car as returned by GET /api/v1/inventory and GET /api/v1/inventory/{id} */
export interface Car {
  id: string;                   // UUID
  model: string;
  manufacturer: string;
  manufacturingYear: number;
  externalColor: string;
  internalColor: string;
  vin: string;
  status: CarStatus;
  category: CarCategory;
  type: CarType;
  isNew: boolean;               // true = New condition, false = Pre-owned
  kilometers: number;
  propulsionType: string;       // e.g., 'FRONT_WHEEL_DRIVE'
  listedValue: number;          // BRL
  imageKey: string | null;      // relative CDN key, e.g., "cars/{id}/main.jpg"
  optionalItems: string[];
  registrationDate: string;     // ISO-8601 timestamp
}

/** Paginated inventory response: data is Car[] with BffPageMeta */
export type CarListResponse = BffResponse<Car[]> & { meta: BffPageMeta };
export type CarDetailResponse = BffResponse<Car>;

/** Query params for GET /api/v1/inventory */
export interface InventoryQueryParams {
  q?: string;
  category?: CarCategory;
  type?: CarType;
  condition?: 'NEW' | 'USED';
  manufacturer?: string;
  yearMin?: number;
  yearMax?: number;
  priceMin?: number;
  priceMax?: number;
  color?: string;
  kmMin?: number;
  kmMax?: number;
  sortBy?: 'PRICE' | 'YEAR' | 'REGISTRATION_DATE';
  sortDirection?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}
```

**UI convenience type** (props passed to `<CarCard>`, `<CarDetail>`):

```typescript
// components/inventory/types.ts

/** Resolved image URL injected by server before passing to components */
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
  listedValue: number;           // BRL — formatted by lib/format.ts in the component
  status: CarStatus;
  imageUrl: string | null;       // full URL — NEXT_PUBLIC_CDN_URL + imageKey
}
```

---

## 3. Authentication

```typescript
// lib/api/types.ts (continued)

/** POST /api/v1/auth/register request body (AUTHENTICATED — requires SESSION from Keycloak) */
export interface RegisterRequest {
  cpf: string;                   // 11 digits, no formatting
  phone: string;                 // 10 or 11 digits, no formatting
  cep: string;                   // 8 digits, no hyphen
  streetNumber: string;          // non-blank
  // firstName + lastName come from Keycloak JWT (given_name / family_name) — do NOT send
  // email is in Keycloak — do NOT send
}

// POST /api/v1/auth/register success: BffResponse<null>
// POST /api/v1/auth/logout success: BffResponse<null> with 204
```

**Route: `app/(auth)/complete-registration/` form state** (React Hook Form + Zod):

```typescript
// app/(auth)/complete-registration/schema.ts

export const registerSchema = z.object({
  cpf: z.string().refine(isValidCpf, 'CPF inválido'),      // isValidCpf from lib/utils/cpf.ts
  phone: z.string().refine(isValidPhone, 'Telefone inválido'),
  cep: z.string().regex(CEP_REGEX_MASKED, 'CEP inválido'), // CEP_REGEX_MASKED from lib/utils/cep.ts
  streetNumber: z.string().min(1, 'Número é obrigatório'),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
```

---

## 4. Customer Profile

```typescript
// lib/api/types.ts (continued)

export interface CustomerAddress {
  street: string;
  number: string;            // NOT updatable via PATCH /api/v1/profile — see R-04
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;             // UF, e.g., "SP"
  cep: string;               // 8 digits
}

export interface CustomerProfile {
  id: string;                // UUID
  firstName: string;
  lastName: string;
  cpf: string;               // read-only — never send in PATCH body
  email: string;             // from Keycloak JWT claim
  phone: string;
  createdAt: string;         // ISO-8601
  address: CustomerAddress;
}

/** PATCH /api/v1/profile request body — DO NOT include cpf */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  cep?: string;              // 8 digits, no hyphen
  // streetNumber: NOT supported by current BFF contract (see research.md R-04)
}
```

**Profile form state** (React Hook Form + Zod):

```typescript
// app/(customer)/account/schema.ts

export const profileSchema = z.object({
  firstName: z.string().min(1, 'Nome é obrigatório').max(100),
  lastName: z.string().min(1, 'Sobrenome é obrigatório').max(100),
  phone: z.string().refine(isValidPhone, 'Telefone inválido'),
  cep: z.string().regex(CEP_REGEX_MASKED, 'CEP inválido'),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
```

---

## 5. Purchases

```typescript
// lib/api/types.ts (continued)

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
  // NOTE: internalColor, type, optionalItems, imageKey are NOT in the BFF snapshot
  // response. See research.md R-07. These fields are intentionally absent.
}

export interface PurchaseClientSnapshot {
  firstName: string;
  lastName: string;
  cpf: string;
}

export interface Purchase {
  id: string;                      // UUID
  registeredAt: string;            // ISO-8601 timestamp
  status: 'COMPLETED';
  vehicle: PurchaseVehicleSnapshot;
  client: PurchaseClientSnapshot;
}

/** POST /api/v1/purchases request */
export interface CreatePurchaseRequest {
  carId: string;                   // UUID
}

/** POST /api/v1/purchases success */
export type CreatePurchaseResponse = BffResponse<Purchase>;

/** GET /api/v1/purchases success */
export type PurchaseListResponse = BffResponse<Purchase[]> & { meta: BffPageMeta };
```

**Purchase financial summary** (derived, not from BFF):

```typescript
// lib/pricing.ts

export const TAX_RATE = 0.10;

export function computeTax(listedValue: number): number {
  return listedValue * TAX_RATE;
}

export function computeTotal(listedValue: number): number {
  return listedValue + computeTax(listedValue);
}
```

---

## 6. Admin (Assumed Contracts — Pending BFF Spec)

> **⚠️ These types are based on assumed contracts (see research.md R-08).**  
> They must be validated against the official BFF admin spec before implementation.

```typescript
// lib/api/types.ts (continued)

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
  taxAmount: number;             // assumed to be returned by admin endpoint
  finalValue: number;            // assumed to be returned by admin endpoint
}

/** GET /api/v1/admin/sales — assumed query params */
export interface SalesQueryParams {
  from?: string;  // ISO-8601 Instant
  to?: string;    // ISO-8601 Instant
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
```

---

## 7. CEP Resolution

```typescript
// lib/api/types.ts (continued)

/**
 * BFF CEP lookup response (⚠️ assumed contract — see contracts/bff-api.md §CEP Lookup)
 * Returned by GET /api/v1/cep/{cep} — called via lib/api/cep.ts
 */
export type CepLookupResponse =
  | { ok: true; street: string; neighborhood: string; city: string; state: string }
  | { ok: false; error: string };

/** Convenience alias used by form hooks and Server Actions */
export type CepLookupResult = CepLookupResponse;
```

---

## 8. Filter State

```typescript
// lib/hooks/use-inventory-filters.ts

/** Client-side representation of active inventory filters (synced to URL params) */
export interface InventoryFilters {
  q: string;
  category: CarCategory | '';
  type: CarType | '';
  condition: 'NEW' | 'USED' | '';
  manufacturer: string;
  yearMin: string;
  yearMax: string;
  priceMin: string;
  priceMax: string;
  color: string;
  kmMin: string;
  kmMax: string;
  sortBy: 'PRICE' | 'YEAR' | 'REGISTRATION_DATE';
  sortDirection: 'ASC' | 'DESC';
  page: number;
}

export const DEFAULT_FILTERS: InventoryFilters = {
  q: '',
  category: '',
  type: '',
  condition: '',
  manufacturer: '',
  yearMin: '',
  yearMax: '',
  priceMin: '',
  priceMax: '',
  color: '',
  kmMin: '',
  kmMax: '',
  sortBy: 'REGISTRATION_DATE',
  sortDirection: 'DESC',
  page: 0,
};
```

---

## 9. Entity Relationships

```
[anonymous visitor]
    │
    ├── browses ──► [Car]  (via GET /api/v1/inventory)
    │                  └── detail ──► [Car] (via GET /api/v1/inventory/{id})
    │
    └── registers ──► [CustomerProfile]  (via POST /api/v1/auth/register)
                           │
                           └── (after login via PKCE)
                                │
                                ├── views/updates ──► [CustomerProfile] (GET|PATCH /api/v1/profile)
                                ├── purchases ──► [Purchase]  (POST /api/v1/purchases)
                                │                   └── creates snapshot of [Car] + [CustomerProfile]
                                └── views history ──► [Purchase[]] (GET /api/v1/purchases)

[admin user]  (ROLE_ADMIN via Keycloak)
    ├── manages ──► [Car]  (POST|PATCH /api/v1/admin/inventory — assumed)
    └── views ──► [SaleRecord[]]  (GET /api/v1/admin/sales — assumed)
```

---

## 10. State Transitions

### Car Status
```
AVAILABLE ──(purchase confirmed)──► SOLD
AVAILABLE ──(admin action)──────────► UNAVAILABLE
UNAVAILABLE ──(admin action)────────► AVAILABLE
SOLD ──(no transition back)
```

### Authentication State (web app perspective)
```
[no SESSION cookie]
    └── access protected route ──► redirect to /oauth2/authorization/keycloak
                                         │
                                         └── (Keycloak PKCE flow)
                                                │
                                                └── callback sets SESSION cookie
                                                       │
                                                       └── [SESSION cookie present]
                                                              │
                                                              └── BFF returns 401 ──► redirect to auth
                                                              └── user clicks logout ──► POST /api/v1/auth/logout
                                                                                              │
                                                                                              └── [no SESSION cookie]
```
