# Contract: GET /api/v1/sales

**Retrieve My Sales (Client)**

---

## Overview

| Property | Value |
|----------|-------|
| Method | `GET` |
| Path | `/api/v1/sales` |
| Authentication | Required — Bearer JWT |
| Authorization | `ROLE_CLIENT` only |
| Cache | Not cached (paginated + filter combination) |
| Pagination | Offset-based: `page` (0-indexed) + `size` (default 20, max 100) |

---

## Request

### Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | `Bearer <jwt>` |

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | `0` | 0-indexed page number |
| `size` | integer | No | `20` | Page size (max 100; server caps silently) |
| `sort` | string | No | `registeredAt,desc` | Sort field and direction |
| `from` | ISO-8601 datetime | No | — | Filter: sales registered on or after this timestamp |
| `to` | ISO-8601 datetime | No | — | Filter: sales registered on or before this timestamp |

**Note:** `from` and `to` are both optional. If only one is provided, it acts as
a one-sided bound. Both must be valid ISO-8601 datetime strings (e.g. `2026-01-01T00:00:00Z`).

### Ownership Enforcement

The service ignores any attempt to query another client's sales. The `clientId`
filter is derived exclusively from the authenticated JWT `sub` claim.
There is no `clientId` query parameter on this endpoint.

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
      "pageSize":   20,
      "sort":       { "sorted": true, "orders": [{ "property": "registeredAt", "direction": "DESC" }] }
    },
    "totalElements": 1,
    "totalPages":    1,
    "last":    true,
    "first":   true,
    "empty":   false,
    "numberOfElements": 1
  }
}
```

**Empty result (client has no sales):** Returns `200` with `"content": []` and
`"empty": true`. Not a 404.

### 401 Unauthorized

No valid JWT or invalid `aud` claim.

### 403 Forbidden

Caller has a valid JWT but does not have `ROLE_CLIENT`.

---

## Pagination Defaults

- Default page size: **20** (configured globally via `spring.data.web.pageable.default-page-size=20`)
- Maximum page size: **100** (configured via `spring.data.web.pageable.max-page-size=100`)
- Requests with `size > 100` are silently capped at 100.

---

## Security

- Method-level: `@PreAuthorize("hasRole('CLIENT')")`
- `clientId` is never read from the query string; always from JWT `sub`
