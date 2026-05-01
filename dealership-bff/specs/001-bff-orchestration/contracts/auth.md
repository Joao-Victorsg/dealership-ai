# Contract: Auth Endpoints

**Base path**: `/api/v1/auth`
**Auth requirement**: Public (no token required for any endpoint in this group)
**Envelope**: All responses use `ApiResponse<T>` or `ApiErrorResponse` — never a naked object

---

## POST /api/v1/auth/register

Register a new user. Orchestrates Keycloak user creation followed by Client API profile creation.

### Request

```json
{
  "email": "user@example.com",
  "password": "securepassword",
  "firstName": "João",
  "lastName": "Silva",
  "cpf": "12345678909",
  "phone": "11987654321",
  "cep": "01310100"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | string | ✅ | RFC 5322 format |
| `password` | string | ✅ | Min 8 characters |
| `firstName` | string | ✅ | Non-blank |
| `lastName` | string | ✅ | Non-blank |
| `cpf` | string | ✅ | 11 digits + valid check digits (Brazilian CPF) |
| `phone` | string | ✅ | 10 or 11 Brazilian digits (DDD + number) |
| `cep` | string | ✅ | 8 digits (normalized, hyphen stripped) |

### Success Response — 201 Created

```json
{
  "data": null,
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "a3f2c1d4-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | Any field fails format validation; `details` contains field-level errors |
| 422 | `DUPLICATE_IDENTITY` | Email already registered in Keycloak |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Keycloak or Client API unreachable |

---

## POST /api/v1/auth/login

Exchange user credentials for an access token. Stores refresh token in HttpOnly cookie.

### Request

```json
{
  "email": "user@example.com",
  "password": "securepassword"
}
```

### Success Response — 200 OK

**Response body**:
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "b5e8a2c1-..."
  }
}
```

**Set-Cookie header** (not in body):
```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=86400
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 400 | `VALIDATION_ERROR` | Missing or blank email/password |
| 401 | `AUTHENTICATION_REQUIRED` | Invalid credentials |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Keycloak unreachable |

---

## POST /api/v1/auth/logout

Invalidates the session. Revokes the refresh token at Keycloak and clears the HttpOnly cookie.

### Request

No body required. The refresh token is read from the HttpOnly cookie.

### Success Response — 204 No Content

```json
{
  "data": null,
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "c7d4b1a0-..."
  }
}
```

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 503 | `DOWNSTREAM_UNAVAILABLE` | Keycloak unreachable (cookie is still cleared) |

---

## POST /api/v1/auth/refresh

Explicitly refresh the access token using the HttpOnly cookie. Transparent refresh also happens automatically on all protected endpoints; this endpoint is available for clients that need to proactively refresh.

### Request

No body. Reads refresh token from HttpOnly cookie.

### Success Response — 200 OK

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "meta": {
    "timestamp": "2026-04-26T14:00:00Z",
    "requestId": "d9f6c3b2-..."
  }
}
```

**Set-Cookie header**: new refresh token replaces the old cookie.

### Error Responses

| Status | `error.code` | Scenario |
|--------|--------------|----------|
| 401 | `AUTHENTICATION_REQUIRED` | Refresh token cookie absent, expired, or revoked |
| 503 | `DOWNSTREAM_UNAVAILABLE` | Keycloak unreachable |

---

## Envelope Schema

### Success (all Auth endpoints)
```json
{
  "data": { ... } | null,
  "meta": {
    "timestamp": "ISO-8601 Instant",
    "requestId": "UUID string"
  }
}
```

### Error (all Auth endpoints)
```json
{
  "error": {
    "code": "ErrorCode enum value",
    "message": "User-safe description",
    "details": [
      { "field": "cpf", "reason": "must match Brazilian CPF format" }
    ]
  },
  "meta": {
    "timestamp": "ISO-8601 Instant",
    "requestId": "UUID string"
  }
}
```
