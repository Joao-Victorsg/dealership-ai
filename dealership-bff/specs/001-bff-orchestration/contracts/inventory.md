# Contract: Inventory Endpoints

**Base path**: `/api/v1/inventory`
**Auth requirement**: Public — no token required for any endpoint in this group
**Caching**: All responses cached in Redis (TTL 5 min) by composite key; cache is invalidated per car ID on successful sale
**Envelope**: All responses use `ApiResponse<T>` or `ApiErrorResponse`

---

## GET /api/v1/inventory

List cars with optional filters, sorting, and pagination. Results are served from the Redis cache when available.

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | ❌ | Free-text search across model, manufacturer, year, color, and other attributes |
| `category` | string | ❌ | Car category: `SEDAN`, `SUV`, `HATCHBACK`, `COUPE`, `CONVERTIBLE`, `MINIVAN`, `PICKUP`, `OTHER` |
| `type` | string | ❌ | Vehicle type (e.g., `ELECTRIC`, `HYBRID`, `GASOLINE`, `DIESEL`) |
| `condition` | string | ❌ | `NEW` or `USED` |
| `manufacturer` | string | ❌ | Manufacturer name (partial match supported) |
| `yearMin` | integer | ❌ | Minimum manufacturing year (inclusive) |
| `yearMax` | integer | ❌ | Maximum manufacturing year (inclusive) |
| `priceMin` | decimal | ❌ | Minimum listed price (inclusive) |
| `priceMax` | decimal | ❌ | Maximum listed price (inclusive) |
| `color` | string | ❌ | External color (partial match) |
| `kmMin` | decimal | ❌ | Minimum odometer reading (inclusive) |
| `kmMax` | decimal | ❌ | Maximum odometer reading (inclusive) |
| `sortBy` | string | ❌ | `PRICE` \| `YEAR` \| `REGISTRATION_DATE` (default: `REGISTRATION_DATE`) |
| `sortDirection` | string | ❌ | `ASC` \| `DESC` (default: `DESC`) |
| `page` | integer | ❌ | Zero-based page number (default: `0`) |
| `size` | integer | ❌ | Page size (default: `20`, max: `100`) |

### Success Response — 200 OK

```json
{
  "data": [
    {
      "id": "3f8a1c2d-...",
      "model": "Civic",
      "manufacturer": "Honda",
      "manufacturingYear": 2023,
      "externalColor": "Pearl White",
      "internalColor": "Black",
      "vin": "1HGBH41JXMN109186",
      "status": "AVAILABLE",
      "category": "SEDAN",
      "type": "GASOLINE",
      "isNew": false,
      "kilometers": 15000.00,
      "propulsionType": "FRONT_WHEEL_DRIVE",
      "listedValue": 145000.00,
      "imageKey": "cars/3f8a1c2d-.../abc123.jpg",
      "optionalItems": ["Sunroof", "Leather seats"],
      "registrationDate": "2026-01-15T10:00:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "a1b2c3d4-...",
    "page": 0,
    "pageSize": 20,
    "totalElements": 142,
    "totalPages": 8
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | `priceMin > priceMax`, `yearMin > yearMax`, invalid enum values for category/type/condition/sortBy |
| 400 | `VALIDATION_ERROR` | Search query `q` contains characters not permitted after sanitization |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Car API circuit open, timeout, or error (and Redis cache miss) |

---

## GET /api/v1/inventory/{id}

Retrieve a single car by its ID. Result is cached in Redis under `car-by-id:{id}` (TTL 5 min).

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Car's unique identifier |

### Success Response — 200 OK

```json
{
  "data": {
    "id": "3f8a1c2d-...",
    "model": "Civic",
    "manufacturer": "Honda",
    "manufacturingYear": 2023,
    "externalColor": "Pearl White",
    "internalColor": "Black",
    "vin": "1HGBH41JXMN109186",
    "status": "AVAILABLE",
    "category": "SEDAN",
    "type": "GASOLINE",
    "isNew": false,
    "kilometers": 15000.00,
    "propulsionType": "FRONT_WHEEL_DRIVE",
    "listedValue": 145000.00,
    "imageKey": "cars/3f8a1c2d-.../abc123.jpg",
    "optionalItems": ["Sunroof", "Leather seats"],
    "registrationDate": "2026-01-15T10:00:00Z"
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "b2c3d4e5-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | `id` is not a valid UUID |
| 404 | `NOT_FOUND` | No car exists with the given ID |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Car API circuit open, timeout, or error |

---

## Caching Behavior

| Cache | Key | TTL | Eviction |
|-------|-----|-----|----------|
| `car-listings` | `InventoryFilterRequest.toCacheKey()` (deterministic, sorted) | 5 min | TTL expiry only |
| `car-by-id` | `{carId}` | 5 min | Explicit eviction on successful purchase for that car ID |

> If Redis is temporarily unavailable, the BFF falls through to the Car API directly (cache-miss degradation). Responses are not cached until Redis becomes available again.

---

## Search Sanitization

The `q` parameter is sanitized by `InputSanitizationFilter` before any service logic is invoked:
- Stripped of characters not in: `[a-zA-Z0-9À-ÿ .,'"-]`
- Leading/trailing whitespace trimmed
- Multiple consecutive spaces collapsed to one
- If the sanitized value is blank (was entirely injection characters), the `q` parameter is treated as absent (no search query)
- The sanitized value — never the raw value — is forwarded to the Car API and logged
