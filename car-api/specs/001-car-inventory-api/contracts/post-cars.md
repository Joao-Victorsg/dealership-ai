# POST /api/v1/cars — Register a New Car

**Auth**: Required (Staff or Admin role)  
**Content-Type**: `application/json`

## Request Body

```json
{
  "model": "Corolla Cross",
  "manufacturingYear": 2026,
  "manufacturer": "Toyota",
  "externalColor": "Pearl White",
  "internalColor": "Black",
  "vin": "1HGBH41JXMN109186",
  "status": "AVAILABLE",
  "optionalItems": ["Sunroof", "Leather Seats", "Navigation System"],
  "category": "SUV",
  "kilometers": 0,
  "isNew": true,
  "propulsionType": "COMBUSTION",
  "listedValue": 38500.00,
  "imageKey": null
}
```

### Field Specifications

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `model` | `string` | Yes | Non-blank, max 255 chars |
| `manufacturingYear` | `integer` | Yes | >= 1886, <= current year + 1 |
| `manufacturer` | `string` | Yes | Non-blank, max 255 chars |
| `externalColor` | `string` | Yes | Non-blank, max 100 chars |
| `internalColor` | `string` | Yes | Non-blank, max 100 chars |
| `vin` | `string` | Yes | Exactly 17 alphanumeric chars (auto-uppercased) |
| `status` | `string` | Yes | `AVAILABLE` or `UNAVAILABLE` only |
| `optionalItems` | `string[]` | No | List of free-form strings; empty list or null accepted |
| `category` | `string` | Yes | `SUV`, `SEDAN`, `SPORT`, `HATCH`, `PICKUP` |
| `kilometers` | `number` | Yes | >= 0; must be 0 if `isNew=true`, > 0 if `isNew=false` |
| `isNew` | `boolean` | Yes | `true` or `false` |
| `propulsionType` | `string` | Yes | `ELECTRIC` or `COMBUSTION` |
| `listedValue` | `number` | Yes | > 0, up to 2 decimal places |
| `imageKey` | `string` | No | Valid S3 object key; empty string treated as null |

## Responses

### 201 Created

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "model": "Corolla Cross",
  "manufacturingYear": 2026,
  "manufacturer": "Toyota",
  "externalColor": "Pearl White",
  "internalColor": "Black",
  "vin": "1HGBH41JXMN109186",
  "status": "AVAILABLE",
  "optionalItems": ["Sunroof", "Leather Seats", "Navigation System"],
  "category": "SUV",
  "kilometers": 0,
  "isNew": true,
  "propulsionType": "COMBUSTION",
  "listedValue": 38500.00,
  "imageKey": null,
  "registrationDate": "2026-04-12T10:30:00Z"
}
```

### 400 Bad Request — Validation Error

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "vin", "message": "VIN must be exactly 17 alphanumeric characters" },
    { "field": "listedValue", "message": "Listed value must be a positive number" }
  ]
}
```

### 401 Unauthorized

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

### 403 Forbidden

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions"
}
```

### 409 Conflict — Duplicate VIN

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "A car with this VIN already exists"
}
```

## Cache Side Effects

- Evicts ALL entries from `car-listings` cache region

