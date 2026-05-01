# API Contracts: Car Inventory API

**Feature**: 001-car-inventory-api  
**Base path**: `/api/v1/cars`  
**Date**: 2026-04-12

## Overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/cars` | Staff/Admin | Register a new car |
| `GET` | `/api/v1/cars` | Public | List/filter/sort cars (paginated) |
| `GET` | `/api/v1/cars/{id}` | Public | Get car by ID |
| `PATCH` | `/api/v1/cars/{id}` | Staff/Admin | Update mutable car attributes |
| `POST` | `/api/v1/cars/{id}/image/presigned-url` | Staff/Admin | Generate presigned S3 upload URL |

## Common Headers

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Write ops only | `Bearer <JWT>` — JWT from external IdP |
| `Content-Type` | Write ops | `application/json` |

### Response Headers

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |

## Common Error Response Format

All error responses use this structure:

```json
{
  "timestamp": "2026-04-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    {
      "field": "vin",
      "message": "VIN must be exactly 17 alphanumeric characters"
    }
  ]
}
```

`fieldErrors` is present only for validation errors (400). Other errors omit this field.

## HTTP Status Codes

| Code | Usage |
|------|-------|
| `200` | Successful read or update |
| `201` | Successful creation |
| `400` | Validation error |
| `401` | Missing or invalid authentication |
| `403` | Insufficient role (not staff/admin) |
| `404` | Car not found |
| `409` | Conflict (duplicate VIN) |
| `422` | Business rule violation (e.g., modifying sold car) |
| `500` | Unexpected server error |

