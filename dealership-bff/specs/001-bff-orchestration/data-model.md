# Data Model: BFF Orchestration Service

**Phase**: 1 — Design artifacts
**Date**: 2026-04-26

> The BFF has **no database**. This document describes the BFF's domain model: the DTOs it owns, the Feign client types it uses internally, and the mapping relationships between them. No JPA entities, no schema migrations.

---

## Domain Overview

```
Frontend ←──── BFF ────────────────────────────────────────────────────────→ Downstream Services
               │
               │  owns (BFF DTOs)          maps from (Feign internal types)
               │
               ├─ VehicleResponse           ← CarApiCarResponse
               ├─ ProfileResponse           ← ClientApiClientResponse
               ├─ PurchaseResponse          ← SalesApiSaleResponse
               ├─ TokenResponse             ← KeycloakTokenResponse (access token only)
               │
               │  wraps all of the above in:
               ├─ ApiResponse<T>            (success envelope)
               └─ ApiErrorResponse          (error envelope)
```

---

## BFF-Owned Response DTOs

### `ApiResponse<T>` — Success Envelope

```
ApiResponse<T>
├── data: T                          # The actual payload
└── meta: ResponseMeta
    ├── timestamp: Instant           # UTC timestamp of response generation
    ├── requestId: String            # Trace ID — same as MDC requestId in all log entries
    ├── page: Integer                # null for non-paginated responses
    ├── pageSize: Integer            # null for non-paginated responses
    ├── totalElements: Long          # null for non-paginated responses
    └── totalPages: Integer          # null for non-paginated responses
```

**Immutability contract**: Record. Static factory: `ApiResponse.of(T data, ResponseMeta meta)`. For paginated: `ApiResponse.paged(T data, ResponseMeta meta)` where meta includes pagination fields.

### `ApiErrorResponse` — Error Envelope

```
ApiErrorResponse
├── error: ErrorBody
│   ├── code: ErrorCode              # Business-meaningful enum — never HTTP status names
│   ├── message: String              # User-safe message — no stack traces or internal details
│   └── details: List<ErrorDetail>   # Non-null but may be empty; non-empty for VALIDATION_ERROR
│       └── ErrorDetail
│           ├── field: String        # The name of the field that failed validation
│           └── reason: String       # Human-readable reason (e.g., "must match CPF format")
└── meta: ResponseMeta
    ├── timestamp: Instant
    └── requestId: String            # Pagination fields are null on error responses
```

**Immutability contract**: Records. Static factory: `ApiErrorResponse.of(ErrorBody error, ResponseMeta meta)`.

### `ErrorCode` — Business Error Enum

| Value | HTTP Status | Scenario |
|-------|-------------|----------|
| `CAR_NOT_AVAILABLE` | 409 | Car was sold before BFF's sale submission reached the Sales API |
| `VALIDATION_ERROR` | 400 | Client input failed format or constraint validation |
| `AUTHENTICATION_REQUIRED` | 401 | No valid token and no usable refresh token |
| `FORBIDDEN` | 403 | Authenticated but insufficient role for the requested operation |
| `NOT_FOUND` | 404 | Requested resource does not exist (car, client) |
| `RATE_LIMIT_EXCEEDED` | 429 | Resilience4j RateLimiter rejected the call |
| `DOWNSTREAM_UNAVAILABLE` | 503 | Circuit breaker open, timeout, bulkhead full, or downstream 5xx |
| `DUPLICATE_IDENTITY` | 422 | Registration attempted with an already-existing email or CPF |
| `INTERNAL_ERROR` | 500 | Unexpected unhandled exception — no detail leaked to client |

---

## BFF Request DTOs

### `LoginRequest`
```
LoginRequest
├── email: String      @NotBlank @Email
└── password: String   @NotBlank
```

### `RegisterRequest`
```
RegisterRequest
├── email: String      @NotBlank @Email
├── password: String   @NotBlank @Size(min=8)
├── firstName: String  @NotBlank
├── lastName: String   @NotBlank
├── cpf: String        @NotBlank — validated by InputSanitizationFilter (11 digits + check digit algorithm)
├── phone: String      @NotBlank — validated by InputSanitizationFilter (Brazilian format)
└── cep: String        @NotBlank — validated by InputSanitizationFilter (8 digits)
```

### `UpdateProfileRequest`
```
UpdateProfileRequest
├── firstName: String  @Size(min=1, max=100) — optional
├── lastName: String   @Size(min=1, max=100) — optional
├── phone: String      — optional, validated by InputSanitizationFilter if present
└── cep: String        — optional, validated by InputSanitizationFilter if present
```
> CPF is NOT a field in `UpdateProfileRequest`. Attempting to send `cpf` in the request body is treated as a validation error with code `VALIDATION_ERROR` and field detail `cpf: "CPF cannot be updated through this endpoint"`.

### `PurchaseRequest`
```
PurchaseRequest
└── carId: UUID   @NotNull
```

### `InventoryFilterRequest`
```
InventoryFilterRequest
├── q: String                        # free-text search query — sanitized by filter
├── category: String                 # optional enum value (SEDAN, SUV, HATCHBACK, etc.)
├── type: String                     # optional
├── condition: String                # optional (NEW / USED)
├── manufacturer: String             # optional
├── yearMin: Integer                 # optional
├── yearMax: Integer                 # optional
├── priceMin: BigDecimal             # optional
├── priceMax: BigDecimal             # optional
├── color: String                    # optional (externalColor)
├── kmMin: BigDecimal                # optional
├── kmMax: BigDecimal                # optional
├── sortBy: String                   # PRICE | YEAR | REGISTRATION_DATE
├── sortDirection: String            # ASC | DESC
└── page: int, size: int             # pagination
```
**`toCacheKey()`**: Produces a deterministic, sorted, URL-encoded string from all non-null fields. Used as the Spring Cache key for `"car-listings"`.

---

## BFF Response DTOs (Mapped from Downstream)

### `VehicleResponse` — mapped from Car API

```
VehicleResponse
├── id: UUID
├── model: String
├── manufacturer: String
├── manufacturingYear: Integer
├── externalColor: String
├── internalColor: String
├── vin: String
├── status: String                   # "AVAILABLE" / "SOLD" — BFF maps CarStatus enum to String
├── category: String
├── type: String
├── isNew: Boolean
├── kilometers: BigDecimal
├── propulsionType: String
├── listedValue: BigDecimal
├── imageKey: String                 # S3 key — frontend constructs the URL from a base URL config
├── optionalItems: List<String>      # unmodifiable
└── registrationDate: Instant
```

### `ProfileResponse` — mapped from Client API

```
ProfileResponse
├── id: UUID
├── firstName: String
├── lastName: String
├── cpf: String                      # displayed but not editable through BFF
├── phone: String
├── email: String                    # extracted from JWT claims, not from Client API (Client API does not store email)
├── createdAt: Instant
└── address: AddressView
    ├── street: String
    ├── number: String
    ├── complement: String
    ├── neighborhood: String
    ├── city: String
    ├── state: String
    └── cep: String
```

### `PurchaseResponse` — mapped from Sales API

```
PurchaseResponse
├── id: UUID
├── registeredAt: Instant
├── status: String
├── vehicle: VehicleSnapshot
│   ├── id: UUID
│   ├── model: String
│   ├── manufacturer: String
│   ├── manufacturingYear: Integer
│   ├── externalColor: String
│   ├── vin: String
│   ├── category: String
│   └── listedValue: BigDecimal
└── client: ClientSnapshot
    ├── firstName: String
    ├── lastName: String
    └── cpf: String
```

### `TokenResponse`

```
TokenResponse
└── accessToken: String   # JWT — returned in response body; refresh token is HttpOnly cookie only
```

---

## Feign Client Internal DTOs

These types are internal to the `feign.*` packages and MUST NOT be exposed to controllers or frontend.

### `feign.car.dto` — Car API types

| Type | Purpose |
|------|---------|
| `CarApiCarResponse` | Mirrors Car API's `CarResponse` record |
| `CarApiPageResponse<T>` | Mirrors Spring `Page<T>` JSON structure (`content`, `totalElements`, `totalPages`, `number`, `size`) |
| `CarApiFilterParams` | Query params forwarded to Car API GET /api/v1/cars |

### `feign.client.dto` — Client API types

| Type | Purpose |
|------|---------|
| `ClientApiClientResponse` | Mirrors Client API's `ClientResponse` record |
| `ClientApiAddressResponse` | Mirrors Client API's `AddressResponse` record |
| `ClientApiCreateRequest` | Body for Client API POST /clients |
| `ClientApiUpdateRequest` | Body for Client API PATCH /clients/{id} |

### `feign.sales.dto` — Sales API types

| Type | Purpose |
|------|---------|
| `SalesApiSaleResponse` | Mirrors Sales API's `SaleResponse` record |
| `SalesApiRegisterRequest` | Body for Sales API POST /api/v1/sales — includes `carId`, `clientId`, `clientSnapshot`, `carSnapshot` |
| `SalesApiCarSnapshotRequest` | Fields: model, manufacturer, externalColor, internalColor, manufacturingYear, optionalItems, type, category, vin, listedValue, status |
| `SalesApiClientSnapshotRequest` | Fields: firstName, lastName, cpf, email, address |
| `SalesApiAddressSnapshotRequest` | Fields mirroring Sales API's `AddressSnapshotRequest` |
| `SalesApiPageResponse<T>` | Same page wrapper pattern as `CarApiPageResponse` |

### `feign.keycloak.dto` — Keycloak types

| Type | Purpose |
|------|---------|
| `KeycloakTokenResponse` | Fields: `access_token`, `refresh_token`, `expires_in`, `refresh_expires_in`, `token_type` |
| `KeycloakCreateUserRequest` | Fields: `username` (email), `email`, `enabled`, `credentials` (list of `KeycloakCredential`) |
| `KeycloakCredential` | Fields: `type` ("password"), `value`, `temporary` (false) |

---

## Entity Relationships (Conceptual, Cross-Service)

```
Keycloak User (sub = UUID)
  │
  └─── identified by sub ──→ Client API Client (keycloakId = sub)
                                │
                                └─── participates in ──→ Sales API Sale (clientSnapshot)
                                                           │
                                                           └─── references ──→ Car API Car (carSnapshot)
```

> The BFF is the only layer that stitches these relationships together. No service stores cross-service IDs except Client API (which stores the Keycloak subject ID).

---

## Validation Rules

| Field | Rule | Enforcement Point |
|-------|------|-------------------|
| CPF | 11 digits; Luhn-style check digit algorithm (Brazilian CPF validation) | `InputSanitizationFilter` — rejects before service layer |
| CEP | 8 digits (digits only, with or without hyphen — normalize to 8 digits) | `InputSanitizationFilter` |
| Phone | Brazilian mobile: 10 or 11 digits (DDD + number), optionally formatted with parentheses/hyphens — normalize | `InputSanitizationFilter` |
| Search query (`q`) | Strip characters that are not alphanumeric, space, or common punctuation (`.`, `,`, `-`, `'`) | `InputSanitizationFilter` — log sanitized form, never raw |
| Email | RFC 5322 `@Email` constraint | Jakarta Validation in request record constructor |
| Password | Minimum 8 characters | Jakarta Validation in `RegisterRequest` |
| Price range | priceMin ≤ priceMax when both specified | `InventoryFilterRequest` record constructor |
| Year range | yearMin ≤ yearMax when both specified | `InventoryFilterRequest` record constructor |
| CPF in update | Must not be present in `UpdateProfileRequest` | `GlobalExceptionHandler` / record constructor (`ValidationError`) |

---

## State Machine: Registration Flow

```
START
  │
  ▼
[Validate all inputs]  ──fail──→ 400 VALIDATION_ERROR (no downstream calls)
  │ pass
  ▼
[Keycloak: create user] ──fail──→ translate error → return error envelope (user can retry)
  │ success
  │ keycloakSubjectId = response.sub
  ▼
[Client API: create client(keycloakSubjectId, ...)]
  │ success ──────────────────────────────────────────────────────────→ 201 success envelope
  │ fail
  ▼
[Keycloak: delete user(keycloakSubjectId)]  [compensating action]
  │ success ──→ return registration error envelope (user can retry cleanly)
  │ fail
  ▼
[Log ERROR: "Registration compensation failed; orphaned Keycloak user {keycloakSubjectId} requestId={requestId}"]
  └──→ return registration error envelope (user can retry — next attempt gets a new Keycloak user)
```

---

## State Machine: Purchase Flow

```
START
  │
  ▼
[Validate PurchaseRequest.carId]  ──fail──→ 400 VALIDATION_ERROR
  │ pass
  ▼
[Car API: GET /api/v1/cars/{carId}]  ──not found──→ 404 NOT_FOUND
  │ found AND status = AVAILABLE
  │ found AND status ≠ AVAILABLE ──→ 409 CAR_NOT_AVAILABLE
  ▼
[Parallel]
  ├── Car API: GET /api/v1/cars/{carId}      [full car snapshot]
  └── Client API: GET /clients/me             [full client snapshot]
       │
       either fails → cancel remaining → translate → error envelope
       │ both succeed
  ▼
[Assemble RegisterSaleRequest]
  │
  ▼
[Sales API: POST /api/v1/sales]  ──409 CAR_NOT_AVAILABLE──→ 409 CAR_NOT_AVAILABLE envelope
  │ success                       ──bulkhead full──────────→ 503 DOWNSTREAM_UNAVAILABLE
  ▼
[Cache evict car-by-id:{carId}]
  │
  ▼
201 purchase success envelope
```

> **NEVER retry** the Sales API POST. If it fails for any reason other than CAR_NOT_AVAILABLE, the error is translated to `DOWNSTREAM_UNAVAILABLE` (503) and returned immediately.
