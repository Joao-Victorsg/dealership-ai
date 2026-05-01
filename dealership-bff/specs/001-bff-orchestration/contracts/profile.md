# Contract: Profile Endpoints

**Base path**: `/api/v1/profile`
**Auth requirement**: Authenticated — valid JWT with `ROLE_CLIENT` required for all endpoints
**Caching**: NEVER — profile data is always fetched fresh from the Client API
**Envelope**: All responses use `ApiResponse<T>` or `ApiErrorResponse`

---

## GET /api/v1/profile

Retrieve the authenticated user's client profile from the Client API. The `email` field is populated from the JWT `email` claim (Keycloak) because the Client API does not store email.

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization: Bearer <token>` | ✅ | Valid JWT with `ROLE_CLIENT` |

### Success Response — 200 OK

```json
{
  "data": {
    "id": "7c3e9f1a-...",
    "firstName": "João",
    "lastName": "Silva",
    "cpf": "12345678909",
    "email": "joao@example.com",
    "phone": "11987654321",
    "createdAt": "2026-01-10T09:30:00Z",
    "address": {
      "street": "Avenida Paulista",
      "number": "1000",
      "complement": "Apto 42",
      "neighborhood": "Bela Vista",
      "city": "São Paulo",
      "state": "SP",
      "cep": "01310100"
    }
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "c3d4e5f6-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 401 | `AUTHENTICATION_REQUIRED` | No valid token and refresh failed or cookie absent |
| 403 | `FORBIDDEN` | Token valid but `ROLE_CLIENT` not present |
| 404 | `NOT_FOUND` | No client profile found for the JWT subject — profile was never created or was deleted |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Client API circuit open, timeout, or error |

---

## PATCH /api/v1/profile

Update the authenticated user's profile. Only the fields listed below may be updated. CPF is displayed in GET but CANNOT be updated through this endpoint — attempting to send `cpf` in the request body is a validation error.

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization: Bearer <token>` | ✅ | Valid JWT with `ROLE_CLIENT` |

### Request Body

```json
{
  "firstName": "João Carlos",
  "lastName": "Silva",
  "phone": "11912345678",
  "cep": "04578000"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `firstName` | string | ❌ | Non-blank if present, max 100 chars |
| `lastName` | string | ❌ | Non-blank if present, max 100 chars |
| `phone` | string | ❌ | Brazilian phone format (10 or 11 digits) if present |
| `cep` | string | ❌ | 8 digits if present |
| `cpf` | — | ❌ | **Must not be sent** — presence causes `VALIDATION_ERROR` with field detail |

At least one field must be present.

### Success Response — 200 OK

```json
{
  "data": {
    "id": "7c3e9f1a-...",
    "firstName": "João Carlos",
    "lastName": "Silva",
    "cpf": "12345678909",
    "email": "joao@example.com",
    "phone": "11912345678",
    "createdAt": "2026-01-10T09:30:00Z",
    "address": {
      "street": "Avenida Paulista",
      "number": "1000",
      "complement": "Apto 42",
      "neighborhood": "Bela Vista",
      "city": "São Paulo",
      "state": "SP",
      "cep": "04578000"
    }
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "d4e5f6a7-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | `cpf` field present in body; phone/CEP format invalid; empty body |
| 401 | `AUTHENTICATION_REQUIRED` | No valid token |
| 403 | `FORBIDDEN` | Token valid but insufficient role |
| 404 | `NOT_FOUND` | Profile not found |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Client API unreachable |

---

## CPF Policy

The CPF is:
- **Readable** via `GET /api/v1/profile`
- **Not updatable** via `PATCH /api/v1/profile`
- The BFF enforces this: the `UpdateProfileRequest` record does not contain a `cpf` field. Sending `cpf` in the JSON body is treated as an unknown field and ignored, OR — if strict mode is enabled — is rejected with `VALIDATION_ERROR`. Default: strict mode ON (Jackson configured with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` for request types).

> Note: CPF update is possible at the Client API level via `PATCH /clients/{id}/cpf` (requires `ROLE_ADMIN`), but the BFF does not expose this endpoint.
