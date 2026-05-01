# Contract: Purchase Endpoints

**Base path**: `/api/v1/purchases`
**Auth requirement**: Authenticated — valid JWT with `ROLE_CLIENT` required for all endpoints
**Caching**: NEVER — purchase data is never cached
**Envelope**: All responses use `ApiResponse<T>` or `ApiErrorResponse`
**Critical**: The Sales API registration call is NEVER retried — duplicate sale registration is not recoverable

---

## POST /api/v1/purchases

Initiate a car purchase. The BFF orchestrates:
1. Availability check (Car API)
2. Parallel fetch of full car data (Car API) and full client profile (Client API)
3. Sale submission (Sales API)
4. Cache eviction for the purchased car

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization: Bearer <token>` | ✅ | Valid JWT with `ROLE_CLIENT` |

### Request Body

```json
{
  "carId": "3f8a1c2d-0000-0000-0000-000000000001"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `carId` | UUID | ✅ | Must be a valid UUID |

### Success Response — 201 Created

```json
{
  "data": {
    "id": "9b2e7f4c-...",
    "registeredAt": "2026-04-26T14:00:00Z",
    "status": "COMPLETED",
    "vehicle": {
      "id": "3f8a1c2d-...",
      "model": "Civic",
      "manufacturer": "Honda",
      "manufacturingYear": 2023,
      "externalColor": "Pearl White",
      "vin": "1HGBH41JXMN109186",
      "category": "SEDAN",
      "listedValue": 145000.00
    },
    "client": {
      "firstName": "João",
      "lastName": "Silva",
      "cpf": "12345678909"
    }
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "e5f6a7b8-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | `carId` is missing or not a valid UUID |
| 401 | `AUTHENTICATION_REQUIRED` | No valid token |
| 403 | `FORBIDDEN` | Token valid but `ROLE_CLIENT` not present |
| 404 | `NOT_FOUND` | No car found with the given `carId` |
| 409 | `CAR_NOT_AVAILABLE` | Car was available at BFF's check but the Sales API rejected it as already sold — frontend should display a user-friendly "this car is no longer available" message |
| 429 | `RATE_LIMIT_EXCEEDED` | Resilience4j RateLimiter rejected the call to Sales API |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Any downstream service (Car API, Client API, Sales API) is unreachable, circuit open, timeout, or bulkhead full |

---

## GET /api/v1/purchases

Retrieve the authenticated user's paginated purchase history from the Sales API.

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization: Bearer <token>` | ✅ | Valid JWT with `ROLE_CLIENT` |

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `from` | ISO-8601 Instant | ❌ | Filter purchases from this timestamp (inclusive) |
| `to` | ISO-8601 Instant | ❌ | Filter purchases up to this timestamp (inclusive) |
| `page` | integer | ❌ | Zero-based page number (default: `0`) |
| `size` | integer | ❌ | Page size (default: `20`) |

### Success Response — 200 OK

```json
{
  "data": [
    {
      "id": "9b2e7f4c-...",
      "registeredAt": "2026-04-26T14:00:00Z",
      "status": "COMPLETED",
      "vehicle": {
        "id": "3f8a1c2d-...",
        "model": "Civic",
        "manufacturer": "Honda",
        "manufacturingYear": 2023,
        "externalColor": "Pearl White",
        "vin": "1HGBH41JXMN109186",
        "category": "SEDAN",
        "listedValue": 145000.00
      },
      "client": {
        "firstName": "João",
        "lastName": "Silva",
        "cpf": "12345678909"
      }
    }
  ],
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "f6a7b8c9-...",
    "page": 0,
    "pageSize": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

> **Empty list** is returned as `"data": []` — never as `"data": null`.

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 401 | `AUTHENTICATION_REQUIRED` | No valid token |
| 403 | `FORBIDDEN` | Insufficient role |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Sales API circuit open, timeout, or error |

---

## Purchase Flow Invariants

- The BFF **NEVER** retries the `POST /api/v1/sales` call. If it fails for any reason other than `CAR_NOT_AVAILABLE`, the failure is translated to `DOWNSTREAM_UNAVAILABLE` (503) and returned immediately.
- The parallel fetch of car data and client profile uses `CompletableFuture.allOf`. If either call fails, both are cancelled immediately and the overall purchase fails — no partial data is ever forwarded.
- On a successful sale, the BFF evicts `car-by-id:{carId}` from the Redis cache. Listing caches (`car-listings`) expire naturally within 5 minutes.
- The assembled sale payload includes the full car snapshot (all fields) and the full client snapshot (all fields including email from JWT) — these are point-in-time snapshots, not references.

---

## Purchase History Caching Policy

Purchase history is **never cached**. Every call to `GET /api/v1/purchases` fetches fresh data from the Sales API. Attempting to cache purchase history would violate Article VIII of the constitution.
