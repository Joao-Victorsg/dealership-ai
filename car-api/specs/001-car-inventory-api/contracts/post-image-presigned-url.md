# POST /api/v1/cars/{id}/image/presigned-url — Generate Presigned Upload URL

**Auth**: Required (Staff or Admin role)  
**Content-Type**: `application/json`

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `UUID` | Yes | Car unique identifier |

## Request Body

```json
{
  "contentType": "image/jpeg"
}
```

### Field Specifications

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `contentType` | `string` | Yes | MIME type: `image/jpeg`, `image/png`, `image/webp` |

## Responses

### 200 OK

```json
{
  "presignedUrl": "https://s3.amazonaws.com/bucket/cars/a1b2c3d4/abc123.jpg?X-Amz-Algorithm=...",
  "objectKey": "cars/a1b2c3d4-e5f6-7890-abcd-ef1234567890/abc123.jpg",
  "expiresIn": 900
}
```

| Field | Type | Description |
|-------|------|-------------|
| `presignedUrl` | `string` | Presigned S3 PUT URL for direct upload |
| `objectKey` | `string` | S3 object key to send back via PATCH after upload |
| `expiresIn` | `integer` | URL expiration in seconds (default: 900 = 15 min) |

### 400 Bad Request

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "contentType", "message": "Unsupported content type. Allowed: image/jpeg, image/png, image/webp" }
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

### 404 Not Found

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Car not found with id: a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

## Upload Flow

1. Client calls this endpoint to get a presigned URL and object key
2. Client uploads the image directly to S3 using the presigned PUT URL
3. Client calls `PATCH /api/v1/cars/{id}` with `{ "imageKey": "<objectKey>" }` to persist the reference
4. Car API deletes the previous S3 object if one existed

## Notes

- The presigned URL is for PUT operations only (upload)
- The S3 bucket is private; images are served via CloudFront by the BFF
- Object key pattern: `cars/{carId}/{uuid}.{extension}`
- TTL of 15 minutes limits the window for URL misuse

