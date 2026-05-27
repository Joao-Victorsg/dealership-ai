# Contracts — Event and Downstream Interfaces

## A) Inbound SQS Message Contract

### Body JSON schema (business fields)
```json
{
  "SaleId": "string (required, non-empty)",
  "CarId": "string (required, non-empty)"
}
```

### Processing contract rules
- Only `SaleId` and `CarId` are business-driving inputs.
- Missing/malformed required fields => **permanent failure**.
- Duplicate deliveries must be idempotent.

## B) Outbound Keycloak Token Contract

### Request
- Method: `POST`
- URL: `${KEYCLOAK_TOKEN_URL}`
- Content-Type: `application/x-www-form-urlencoded`
- Body:
  - `grant_type=client_credentials`
  - `client_id=<from KEYCLOAK_CLIENT_ID env config>`
  - `client_secret=<from Secrets Manager secret payload via KEYCLOAK_SECRET_ID>`

### Response (minimum required)
```json
{
  "access_token": "string",
  "expires_in": 300
}
```

### Contract rules
- Token cached in memory and proactively refreshed before expiry.
- Persistent auth failure after refresh attempt => **permanent failure**.

## C) Outbound Car API Status Update Contract

### Request
- Method: `PATCH` (fixed; no method fallback)
- URL: `${CAR_API_BASE_URL}/api/v1/cars/{carId}`
- Headers:
  - `Authorization: Bearer <access_token>` (required)
  - `Content-Type: application/json`
- Body:
```json
{
  "status": "SOLD"
}
```

### Response classification contract
- `2xx` => success
- `409` (already sold/target state) => success (idempotent ack)
- `429`, `502`, `503`, `504`, network timeout/connection errors => transient failure
- `400`, `401`, `403`, `404` => permanent failure

## F) Runtime Configuration Contract (app inputs)
- `KEYCLOAK_SECRET_ID` is the required Lambda application environment variable.
- `KEYCLOAK_SECRET_ID` contains the AWS Secrets Manager secret identifier value used by the app at runtime.
- ARN terminology may be used only when referring to AWS resource identity conceptually; app runtime input remains `KEYCLOAK_SECRET_ID`.

## G) Field Naming Mapping Contract
- External event payload fields are `SaleId` and `CarId`.
- Internal structured log fields are `sale_id` and `car_id`.
- Mapping is deterministic: `SaleId -> sale_id`, `CarId -> car_id`.

## D) Circuit Breaker Contract
- All Car API calls execute through one shared cold-start breaker instance.
- Breaker parameters are config-driven.
- Open breaker => immediate transient failure and no outbound Car API call.

## E) Logging Contract (per processed message)
Structured JSON fields:
- `sale_id`
- `car_id`
- `outcome` (`success|transient_failure|permanent_failure`)
- `duration_ms`
- `downstream_http_status` (optional)
- `diagnostic_context` (for permanent failures, redacted)

Sensitive values (`client_secret`, `access_token`, raw auth headers) are never logged.
