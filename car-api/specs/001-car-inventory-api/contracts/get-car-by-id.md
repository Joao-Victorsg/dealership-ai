# GET /api/v1/cars/{id} — Get Car by ID

**Auth**: Public (no authentication required)

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | Car unique identifier |

### Example Request

```
GET /api/v1/cars/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

## Responses

### 200 OK

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
  "imageKey": "cars/a1b2c3d4/photo.jpg",
  "registrationDate": "2026-04-12T10:30:00Z"
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

## Cache Behavior

- Result is cached in `car-by-id` region keyed by car UUID
- Cache TTL: 24 hours
- Cache entry is evicted when the car is updated

