# PATCH /api/v1/cars/{id} — Update Mutable Car Attributes

**Auth**: Required (Staff or Admin role)  
**Content-Type**: `application/json`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | Car unique identifier |

## Request Body

All fields are optional. Include only the fields to update. At least one field must be present.

```json
{
  "status": "UNAVAILABLE",
  "listedValue": 35000.00,
  "imageKey": "cars/a1b2c3d4/updated-photo.jpg"
}
```

### Field Specifications

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `status` | `string` | No | `AVAILABLE`, `SOLD`, `UNAVAILABLE`. Cannot change if current status is `SOLD`. |
| `listedValue` | `number` | No | > 0, up to 2 decimal places |
| `imageKey` | `string` | No | Valid S3 object key; empty string removes image (sets to null) |

### Update Examples

**Change status only:**
```json
{ "status": "UNAVAILABLE" }
```

**Update price only:**
```json
{ "listedValue": 35000.00 }
```

**Update image (triggers deletion of previous S3 object):**
```json
{ "imageKey": "cars/a1b2c3d4/new-photo.jpg" }
```

**Remove image:**
```json
{ "imageKey": "" }
```

## Responses

### 200 OK

Returns the full updated car record:

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "model": "Corolla Cross",
  "manufacturingYear": 2026,
  "manufacturer": "Toyota",
  "externalColor": "Pearl White",
  "internalColor": "Black",
  "vin": "1HGBH41JXMN109186",
  "status": "UNAVAILABLE",
  "optionalItems": ["Sunroof", "Leather Seats"],
  "category": "SUV",
  "kilometers": 0,
  "isNew": true,
  "propulsionType": "COMBUSTION",
  "listedValue": 35000.00,
  "imageKey": "cars/a1b2c3d4/updated-photo.jpg",
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

### 404 Not Found

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Car not found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 422 Unprocessable Entity — Business Rule Violation

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot modify a sold car"
}
```

## Cache Side Effects

- Evicts `car-by-id::{carId}` from `car-by-id` cache region
- Evicts ALL entries from `car-listings` cache region
- If `imageKey` changes and previous image existed: deletes previous S3 object asynchronously

