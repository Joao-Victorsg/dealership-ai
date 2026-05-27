# Data Model — Car Status Updater Lambda

## 1) SaleStatusEvent
- **Purpose**: Minimal inbound SQS business payload.
- **Fields**:
  - `sale_id` (string, required, non-empty)
  - `car_id` (string, required, non-empty)
- **Validation rules**:
  - Missing/empty `sale_id` or `car_id` => permanent failure.
  - Extra fields are ignored for business decisions.
- **State transitions**:
  - `received` -> `validated` -> `processed` or `permanent_failure`.

## 2) AuthSecret
- **Purpose**: Secret material loaded once from AWS Secrets Manager at cold start.
- **Fields**:
  - `client_secret` (string, required, sensitive)
- **Validation rules**:
  - Missing/empty `client_secret` fails startup.
  - `client_id` and `token_url` are sourced from environment configuration, not from secret payload.
  - Values are never logged.
- **State transitions**:
  - `unloaded` -> `loaded_and_validated` (cold start only).

## 3) AccessTokenCacheEntry
- **Purpose**: Warm-instance OAuth2 token reuse.
- **Fields**:
  - `access_token` (string, sensitive)
  - `expires_at` (timestamp)
  - `refresh_before` (duration/configured safety window)
- **Validation rules**:
  - If `now >= expires_at - refresh_before`, refresh before use.
  - Refresh/auth failure after retry boundary => permanent auth failure.
- **State transitions**:
  - `empty` -> `active` -> `refreshing` -> `active`
  - `refreshing` -> `auth_failed_permanent` (on persistent failure).

## 4) CarStatusUpdateRequest
- **Purpose**: Outbound Car API operation intent.
- **Fields**:
  - `car_id` (string)
  - `target_status` (constant: `Sold`)
  - `authorization` (Bearer token header, derived)
- **Validation rules**:
  - Always include Bearer token header.
  - Call only through configured timeout and circuit breaker.
- **State transitions**:
  - `ready` -> `sent` -> `succeeded` | `already_sold_success` | `failed`.

## 5) ProcessingOutcomeRecord
- **Purpose**: Normalized per-message result for logging and SQS response behavior.
- **Fields**:
  - `sale_id` (string)
  - `car_id` (string)
  - `outcome` (enum: `success`, `transient_failure`, `permanent_failure`)
  - `downstream_status` (int, optional)
  - `duration_ms` (int64)
  - `diagnostic_context` (object/string, required for permanent failures, redacted)
- **Validation rules**:
  - Structured JSON log emitted for every processed message.
  - No secret/token values in any field.
- **State transitions**:
  - `started` -> terminal (`success` | `transient_failure` | `permanent_failure`).

## Relationships
- `SaleStatusEvent` drives one `CarStatusUpdateRequest`.
- `AuthSecret` initializes `AccessTokenCacheEntry` acquisition.
- `AccessTokenCacheEntry` authorizes `CarStatusUpdateRequest`.
- Every processing path produces one `ProcessingOutcomeRecord`.

## Naming Mapping
- External event contract fields are `SaleId` and `CarId`.
- Internal/log model fields are `sale_id` and `car_id`.
- Mapping is fixed: `SaleId -> sale_id`, `CarId -> car_id`.

