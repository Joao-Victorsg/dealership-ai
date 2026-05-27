# Data Model: Client API — Customer Profile Management

**Feature**: `001-client-api-profile`  
**Phase**: 1 — Design output  
**Date**: 2026-04-17

---

## Entities

### Client

The root entity representing a registered customer's business profile. One per customer.
Retained permanently (never physically deleted). Fields marked `[IMMUTABLE]` must never
be modified through any standard update endpoint.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL, generated | `[IMMUTABLE]` Internal identifier. Preserved after anonymization for referential integrity. |
| `keycloakId` | String | UNIQUE, NOT NULL | `[IMMUTABLE]` Keycloak `sub` claim. Replaced with random UUID on anonymization. |
| `firstName` | String | NOT NULL | Replaced with `"ANONYMIZED"` on deletion. |
| `lastName` | String | NOT NULL | Replaced with `"ANONYMIZED"` on deletion. |
| `cpf` | String | NOT NULL | `[IMMUTABLE via standard endpoints]` Stored as AES-256-GCM ciphertext. Never logged. Correctable via admin-only endpoint. Replaced with random UUID on anonymization. |
| `cpfHash` | String | UNIQUE, NOT NULL | HMAC-SHA256(cpf + secret). Used for uniqueness enforcement and lookup. Replaced with random UUID on anonymization. |
| `phoneNumber` | String | NOT NULL | Brazilian format: `+55 (DD) NNNNN-NNNN`. Replaced with `"ANONYMIZED"` on deletion. |
| `address` | Address | Embedded, NOT NULL | See Address embedded type below. |
| `createdAt` | LocalDateTime | NOT NULL | `[IMMUTABLE]` Set at creation; never updated. |
| `deletedAt` | LocalDateTime | NULL | Set on anonymization. `null` = active profile. |

**Business rules**:
- A profile is **active** if `deletedAt IS NULL`.
- A profile is **anonymized/inactive** if `deletedAt IS NOT NULL`.
- Inactive profiles reject all update operations with a 422 response.
- `keycloakId` uniqueness is enforced both at DB level (UNIQUE constraint) and service level
  (checked before insert to return a meaningful 422, not a raw constraint exception).
- `cpfHash` uniqueness is enforced the same way as `keycloakId`.

---

### Address (embedded)

Embedded within `Client`. Not a separate table. All address fields are blanked out on
anonymization.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `postcode` | String | NOT NULL | CEP format: `NNNNN-NNN` or `NNNNNNN`. Always persisted. |
| `streetNumber` | String | NOT NULL | Client-provided. Always persisted. |
| `streetName` | String | NULL | Resolved from ViaCEP. Empty if lookup failed. |
| `city` | String | NULL | Resolved from ViaCEP. Empty if lookup failed. |
| `state` | String | NULL | 2-char state abbreviation, resolved from ViaCEP. Empty if lookup failed. |
| `addressSearched` | Boolean | NOT NULL, default `false` | `true` only when all resolved fields were populated by a successful ViaCEP call. |

**Business rules**:
- `addressSearched = true` requires that `streetName`, `city`, and `state` are all non-empty
  AND were populated by a ViaCEP lookup.
- Manual updates to address fields do NOT set `addressSearched = true`.
- On any address update: attempt ViaCEP lookup for the new postcode. Success → populate all
  fields + set `addressSearched = true`. Failure → persist only `postcode` + `streetNumber`,
  set `addressSearched = false`.

---

## Database Schema

### Table: `clients`

```sql
CREATE TABLE clients (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id    VARCHAR(255) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    cpf            VARCHAR(100) NOT NULL,   -- AES-256-GCM ciphertext
    cpf_hash       VARCHAR(64)  NOT NULL,   -- HMAC-SHA256 hex, for uniqueness
    phone_number   VARCHAR(20)  NOT NULL,
    postcode       VARCHAR(10)  NOT NULL,
    street_number  VARCHAR(20)  NOT NULL,
    street_name    VARCHAR(200),
    city           VARCHAR(100),
    state          VARCHAR(2),
    address_searched BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP   NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMP
);

CREATE UNIQUE INDEX uq_clients_keycloak_id ON clients (keycloak_id);
CREATE UNIQUE INDEX uq_clients_cpf_hash    ON clients (cpf_hash);
```

---

## State Transitions

```
              POST /clients
                   │
                   ▼
             ┌─────────────┐
             │   ACTIVE    │◄──── PATCH /clients/{id}
             │ deletedAt   │◄──── PATCH /clients/{id}/address
             │ IS NULL     │◄──── PATCH /clients/{id}/cpf (admin)
             └─────────────┘
                   │
         DELETE /clients/{id}
                   │
                   ▼
             ┌─────────────┐
             │  INACTIVE   │
             │ deletedAt   │  (permanent — no reactivation path)
             │ IS NOT NULL │
             └─────────────┘
```

---

## Address Resolution State

```
addressSearched = false                  addressSearched = true
  (pending or failed lookup)               (fully resolved)
         │                                        │
         │  address update + ViaCEP succeeds      │
         └───────────────────────────────────────►│
         │                                        │
         │◄───────────────────────────────────────┘
         │  address update + ViaCEP fails
         │  (stays false, postcode+streetNumber updated)
```

---

## Validation Rules

### CPF Validation
- Must contain exactly 11 digits (ignoring formatting separators).
- Must pass the official Brazilian CPF modulo-11 check digit algorithm.
- Two consecutive equal digits (e.g., `111.111.111-11`) that happen to pass modulo-11
  checks are still valid per the algorithm.

### Phone Number Validation
- Must match the Brazilian format: `+55` country code, 2-digit area code (DDD), 9-digit
  mobile number or 8-digit landline number.
- Accepted pattern: `^\+55\s?\(?\d{2}\)?\s?\d{4,5}-?\d{4}$`

### Postcode (CEP) Validation
- Must match `^\d{5}-?\d{3}$` (with or without hyphen).

---

## DTO Shapes (logical — not code)

### CreateClientRequest
```
keycloakId    : required, string
firstName     : required, string, max 100
lastName      : required, string, max 100
cpf           : required, valid CPF format
phoneNumber   : required, valid Brazilian phone
postcode      : required, valid CEP
streetNumber  : required, string, max 20
```

### UpdateClientRequest (PATCH /clients/{id})
```
firstName     : optional, string, max 100
lastName      : optional, string, max 100
phoneNumber   : optional, valid Brazilian phone
```
Fields not in this shape are silently ignored.

### UpdateAddressRequest (PATCH /clients/{id}/address)
```
postcode      : required, valid CEP
streetNumber  : required, string, max 20
```

### UpdateCpfRequest (PATCH /clients/{id}/cpf — admin only)
```
cpf           : required, valid CPF format
```

### ClientResponse
```
id            : UUID
firstName     : string
lastName      : string
phoneNumber   : string
address:
  postcode        : string
  streetNumber    : string
  streetName      : string (null if not resolved)
  city            : string (null if not resolved)
  state           : string (null if not resolved)
  addressSearched : boolean
createdAt     : ISO-8601 datetime
deletedAt     : ISO-8601 datetime or null
```
**Note**: `cpf` and `keycloakId` are never included in the response body.
