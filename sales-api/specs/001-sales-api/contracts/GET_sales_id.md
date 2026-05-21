# Contract: GET /api/v1/sales/{id}

**Retrieve a Single Sale by ID**

---

## Overview

| Property | Value |
|----------|-------|
| Method | `GET` |
| Path | `/api/v1/sales/{id}` |
| Authentication | Required — Bearer JWT |
| Authorization | `ROLE_CLIENT` (own sale only) or `ROLE_STAFF` / `ROLE_ADMIN` (any sale) |
| Cache | Redis (`"sales"` cache, key = sale ID, TTL = 24 hours) |

---

## Request

### Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | `Bearer <jwt>` |

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID v4 | Yes | The unique identifier of the sale |

---

## Response

### 200 OK

```json
{
  "data": {
    "id":           "550e8400-e29b-41d4-a716-446655440000",
    "carId":        "7f3b2a10-1234-5678-abcd-ef0123456789",
    "clientId":     "9c1d0e22-aaaa-bbbb-cccc-000000000001",
    "saleValue":    49500.00,
    "registeredAt": "2026-04-19T14:30:00Z",
    "clientSnapshot": {
      "firstName": "João",
      "lastName":  "Silva",
      "cpf":       "123.456.789-09",
      "email":     "joao@example.com",
      "address": {
        "street": "Avenida Paulista", "number": "1000",
        "complement": "Apto 12", "neighborhood": "Bela Vista",
        "city": "São Paulo", "state": "SP", "postcode": "01310-100"
      }
    },
    "carSnapshot": {
      "model": "Civic", "manufacturingYear": 2024, "manufacturer": "Honda",
      "externalColor": "Pearl White", "internalColor": "Black",
      "optionalItems": ["Sunroof"], "type": "SEDAN", "category": "STANDARD",
      "vin": "1HGBH41JXMN109186", "listedValue": 45000.00, "status": "AVAILABLE"
    }
  }
}
```

### 401 Unauthorized

No valid JWT or invalid `aud` claim.

### 403 Forbidden

Two distinct cases:
1. The caller has `ROLE_CLIENT` but the sale does not belong to them (`sale.clientId ≠ jwt.sub`).
   Returns 403 — **not 404** — to prevent IDOR information leakage.
2. The caller's role does not permit this endpoint.

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  403,
    "error":   "Forbidden",
    "message": "You do not have access to this sale"
  }
}
```

### 404 Not Found

The sale ID does not exist in the database (for any role).

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  404,
    "error":   "Not Found",
    "message": "Sale not found: 550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## Authorization Logic

```
if sale not found → 404  (for all roles)
if ROLE_CLIENT and sale.clientId ≠ jwt.sub → 403
if ROLE_CLIENT and sale.clientId == jwt.sub → 200
if ROLE_STAFF or ROLE_ADMIN → 200 (any sale)
```

The 404 check MUST precede the ownership check. A `ROLE_CLIENT` making a request
for a non-existent sale ID receives 404, not 403 — because no sale exists to have an
owner. A `ROLE_CLIENT` requesting an existing sale belonging to another client receives
403 to prevent ID enumeration.

---

## Caching Behavior

- Cache name: `"sales"`, key: `id.toString()`
- TTL: 24 hours
- Cache is populated on the first DB hit; subsequent requests for the same ID
  are served from Redis without DB query.
- No eviction is required — sales are immutable after creation.

---

## Security

- Method-level: `@PreAuthorize("hasAnyRole('CLIENT', 'STAFF', 'ADMIN')")`
- Ownership check in service: enforced only when caller has `ROLE_CLIENT`
