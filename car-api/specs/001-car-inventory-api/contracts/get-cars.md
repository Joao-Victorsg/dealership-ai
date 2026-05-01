# GET /api/v1/cars — List, Filter, and Sort Cars

**Auth**: Public (no authentication required)

## Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | `integer` | No | Page number (0-based, default: 0) |
| `size` | `integer` | No | Page size (default: 20, max: 100) |
| `sort` | `string` | No | Sort field: `registrationDate`, `listedValue`, `manufacturingYear`. Append `,asc` or `,desc` (default: `registrationDate,desc`) |
| `status` | `string` | No | Filter: `AVAILABLE`, `SOLD`, `UNAVAILABLE` |
| `category` | `string` | No | Filter: `SUV`, `SEDAN`, `SPORT`, `HATCH`, `PICKUP` |
| `manufacturer` | `string` | No | Filter: exact match (case-insensitive) |
| `propulsionType` | `string` | No | Filter: `ELECTRIC`, `COMBUSTION` |
| `minValue` | `number` | No | Filter: minimum listed value (inclusive) |
| `maxValue` | `number` | No | Filter: maximum listed value (inclusive) |
| `minYear` | `integer` | No | Filter: minimum manufacturing year (inclusive) |
| `maxYear` | `integer` | No | Filter: maximum manufacturing year (inclusive) |
| `isNew` | `boolean` | No | Filter: `true` for new, `false` for used |

### Example Request

```
GET /api/v1/cars?status=AVAILABLE&category=SUV&minValue=20000&maxValue=50000&sort=listedValue,asc&page=0&size=20
```

## Responses

### 200 OK

```json
{
  "content": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "model": "Corolla Cross",
      "manufacturingYear": 2026,
      "manufacturer": "Toyota",
      "externalColor": "Pearl White",
      "internalColor": "Black",
      "vin": "1HGBH41JXMN109186",
      "status": "AVAILABLE",
      "optionalItems": ["Sunroof", "Leather Seats"],
      "category": "SUV",
      "kilometers": 0,
      "isNew": true,
      "propulsionType": "COMBUSTION",
      "listedValue": 38500.00,
      "imageKey": "cars/a1b2c3d4/photo.jpg",
      "registrationDate": "2026-04-12T10:30:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 400 Bad Request — Invalid Filter

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "minValue", "message": "Minimum value cannot exceed maximum value" }
  ]
}
```

## Cache Behavior

- Results are cached in `car-listings` region keyed by composite hash of all filter, sort, and page parameters
- Cache TTL: 24 hours
- Cache is fully evicted on any car registration or update

## Performance Target

- p95 < 200ms for up to 10,000 cars with any filter combination
- Pagination is enforced (max page size: 100)

