# Contract: GET /api/v1/sales/staff

**Staff — Retrieve All Sales with Filters**

---

## Overview

| Property | Value |
|----------|-------|
| Method | `GET` |
| Path | `/api/v1/sales/staff` |
| Authentication | Required — Bearer JWT |
| Authorization | `ROLE_STAFF` or `ROLE_ADMIN` only |
| Cache | Not cached (multi-filter + pagination combination) |
| Pagination | Offset-based: `page` (0-indexed) + `size` (default 20, max 100) |

---

## Request

### Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | `Bearer <jwt>` |

### Query Parameters

All filters are optional and may be combined freely.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | integer | No | 0-indexed page number (default: 0) |
| `size` | integer | No | Page size (default: 20, max: 100) |
| `sort` | string | No | Sort field and direction (default: `registeredAt,desc`) |
| `clientId` | UUID | No | Filter: return only sales for this client |
| `carId` | UUID | No | Filter: return only the sale for this car (at most one result, since a car can only be sold once) |
| `from` | ISO-8601 datetime | No | Filter: sales registered on or after this timestamp |
| `to` | ISO-8601 datetime | No | Filter: sales registered on or before this timestamp |

**Notes:**
- All four filters are independent and additive (AND semantics).
- `carId` filter returns at most one result by definition (unique constraint on `car_id`).

---

## Response

### 200 OK

```json
{
  "data": {
    "content": [
      {
        "id":           "550e8400-e29b-41d4-a716-446655440000",
        "carId":        "7f3b2a10-1234-5678-abcd-ef0123456789",
        "clientId":     "9c1d0e22-aaaa-bbbb-cccc-000000000001",
        "saleValue":    49500.00,
        "registeredAt": "2026-04-19T14:30:00Z",
        "clientSnapshot": { "...": "..." },
        "carSnapshot":   { "...": "..." }
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize":   20
    },
    "totalElements": 1,
    "totalPages":    1,
    "last":  true,
    "first": true,
    "empty": false,
    "numberOfElements": 1
  }
}
```

### 401 Unauthorized

No valid JWT or invalid `aud` claim.

### 403 Forbidden

Caller has a valid JWT but does not have `ROLE_STAFF` or `ROLE_ADMIN`.
A `ROLE_CLIENT` attempting to access this endpoint receives 403.

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  403,
    "error":   "Forbidden",
    "message": "Access denied"
  }
}
```

---

## Filter Combinations

| `clientId` | `carId` | `from/to` | Behavior |
|---|---|---|---|
| — | — | — | All sales, paginated |
| ✓ | — | — | All sales for this client |
| — | ✓ | — | The single sale for this car (0 or 1 result) |
| ✓ | ✓ | — | Sales for this client AND this car (0 or 1) |
| — | — | ✓ | All sales within date range |
| ✓ | — | ✓ | Client's sales within date range |
| — | ✓ | ✓ | Car's sale if within date range |
| ✓ | ✓ | ✓ | Intersection of all filters |

Implementation uses **Spring Data JPA `Specification<Sale>`** to handle
the combinatorial filter logic cleanly without method explosion.

---

## Pagination Defaults

- Default page size: **20**
- Maximum page size: **100** (requests with `size > 100` are silently capped)

---

## Security

- Method-level: `@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")`
- No ownership enforcement — staff and admin see all sales
- `ROLE_CLIENT` is explicitly excluded; attempted access returns 403
