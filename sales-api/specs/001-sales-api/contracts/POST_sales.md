# Contract: POST /api/v1/sales

**Register a Sale**

---

## Overview

| Property | Value |
|----------|-------|
| Method | `POST` |
| Path | `/api/v1/sales` |
| Authentication | Required — Bearer JWT |
| Authorization | `ROLE_CLIENT` only |
| Idempotency | Not idempotent — each call creates a new sale record |
| Cache | Not applicable (write endpoint) |

---

## Request

### Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes | `Bearer <jwt>` |
| `Content-Type` | Yes | `application/json` |

### Body Schema

```json
{
  "carId":    "string (UUID v4)",
  "clientId": "string (UUID v4)",
  "clientSnapshot": {
    "firstName":    "string",
    "lastName":     "string",
    "cpf":          "string (format: 000.000.000-00)",
    "email":        "string (valid email)",
    "address": {
      "street":       "string",
      "number":       "string",
      "complement":   "string | null",
      "neighborhood": "string",
      "city":         "string",
      "state":        "string (2 chars, e.g. SP)",
      "postcode":     "string"
    }
  },
  "carSnapshot": {
    "model":             "string",
    "manufacturingYear": "integer (1886–2100)",
    "manufacturer":      "string",
    "externalColor":     "string",
    "internalColor":     "string",
    "optionalItems":     ["string"] ,
    "type":              "string",
    "category":          "string",
    "vin":               "string (17 chars, uppercase, no I/O/Q)",
    "listedValue":       "number (> 0)",
    "status":            "AVAILABLE | SOLD | UNAVAILABLE"
  }
}
```

### Business Constraints on Request

- The `carSnapshot.status` MUST be `AVAILABLE`; anything else results in 422.
- The JWT `sub` claim MUST equal `clientId`; mismatch results in 403.
- The `carId` MUST NOT already appear in the `sales` table; duplicate results in 422.
- The `clientId` MUST NOT equal `carId`.
- `carSnapshot.listedValue` MUST be > 0.
- `carSnapshot.vin` MUST match `[A-HJ-NPR-Z0-9]{17}` (standard VIN, no I, O, Q).

---

## Response

### 201 Created

Sale was persisted and the sale event was published to SNS.

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

**Notes:**
- `saleValue` = `carSnapshot.listedValue × 1.10` (10% tax applied by the service).
- The caller MUST NOT pre-calculate the tax; it is always applied server-side.
- `registeredAt` is set by the database (`DEFAULT NOW()`); the caller's clock is not used.
- `Location` header is set to `GET /api/v1/sales/{id}`.

### 400 Bad Request — Validation Error

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":    400,
    "error":     "Bad Request",
    "message":   "Validation failed",
    "fieldErrors": [
      { "field": "carSnapshot.vin",   "message": "must match \"[A-HJ-NPR-Z0-9]{17}\"" },
      { "field": "clientSnapshot.cpf","message": "must match \"\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\"" }
    ]
  }
}
```

### 401 Unauthorized

No valid JWT provided, JWT expired, or `aud` claim does not equal `dealership`.

```json
{ "data": { "status": 401, "error": "Unauthorized", "message": "..." } }
```

### 403 Forbidden

JWT `sub` does not match `clientId` in the request body.

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  403,
    "error":   "Forbidden",
    "message": "You may only register a sale for your own client identity"
  }
}
```

### 422 Unprocessable Entity

Business rule violation. Possible messages:

| `message` | Trigger |
|-----------|---------|
| `"Car status must be AVAILABLE to register a sale"` | `carSnapshot.status` ≠ AVAILABLE |
| `"Car has already been sold"` | Unique constraint on `car_id` violated |

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  422,
    "error":   "Unprocessable Entity",
    "message": "Car status must be AVAILABLE to register a sale"
  }
}
```

### 503 Service Unavailable

SNS publish failed after all retries or circuit breaker is open.
The sale was NOT persisted.

```json
{
  "data": {
    "timestamp": "2026-04-19T14:30:00Z",
    "status":  503,
    "error":   "Service Unavailable",
    "message": "Event publishing is currently unavailable. Please retry."
  }
}
```

---

## Side Effects

1. Row inserted in `sales` table (only on 201).
2. Sale event published to SNS topic (only on 201).
3. Structured log entry written with `SaleId`, `CarId`, `ClientId`, `saleValue` in MDC.
4. Structured log entry written with SNS publish result (success/failure), `SaleId`, topic ARN in MDC.

---

## Security

- Method-level: `@PreAuthorize("hasRole('CLIENT')")`
- Ownership check in service: `jwt.subject == request.clientId` → 403 if mismatch
- No endpoint access for `ROLE_STAFF` or `ROLE_ADMIN`
