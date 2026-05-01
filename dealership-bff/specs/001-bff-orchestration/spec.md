# Feature Specification: BFF Orchestration Service

**Feature Branch**: `master`
**Created**: 2026-04-26
**Status**: Draft
**Input**: User description: "Develop the BFF (Backend for Frontend), the orchestration and composition service..."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Public Inventory Browsing and Search (Priority: P1)

An anonymous visitor opens the dealership website and browses the car inventory without logging in. They can search by free text, apply filters such as category, condition, price range, and kilometers, sort results by price or year, and navigate through pages of results. The BFF receives the request, sanitizes all inputs, forwards the query to the Car API, and returns the composed, paginated response wrapped in the standard envelope.

**Why this priority**: This is the top of the purchase funnel. Every visitor lands here before any authentication happens. It must work independently of all auth infrastructure and delivers immediate value as a standalone read-only browsing experience.

**Independent Test**: Can be fully tested by sending unauthenticated HTTP requests to the inventory and search endpoints and verifying that paginated, enveloped results are returned without any token.

**Acceptance Scenarios**:

1. **Given** an anonymous user, **When** they request the inventory listing with no filters, **Then** the BFF returns a paginated list of cars in the `data` field, with pagination metadata in `meta`, and HTTP 200.
2. **Given** an anonymous user, **When** they search with a free-text query (e.g., "red Honda 2022"), **Then** the BFF sanitizes the query, forwards it to the Car API, and returns matching results paginated.
3. **Given** an anonymous user, **When** they apply filters (e.g., category, price range, condition) and a sort order, **Then** the BFF constructs the composite cache key, returns cached results if available, or fetches from the Car API and caches for 5 minutes.
4. **Given** an anonymous user, **When** they submit a search query containing injection characters, **Then** the BFF sanitizes the input, and either forwards a safe version or rejects the request with a 400 validation error envelope.
5. **Given** the Car API is unavailable (circuit open), **When** the user requests inventory, **Then** the BFF returns a 503 error envelope with code `DOWNSTREAM_UNAVAILABLE`.

---

### User Story 2 — User Login and Token Lifecycle (Priority: P2)

A registered user logs in with their email and password. The BFF exchanges credentials with Keycloak using the Resource Owner Password Credentials flow and, on success, stores the refresh token in an HttpOnly cookie and returns the access token to the frontend. When the access token later expires, the frontend makes a request, and the BFF transparently refreshes it using the cookie before responding — the user never sees a 401 for a valid session. When the user logs out, the BFF clears the HttpOnly cookie.

**Why this priority**: Authentication is the gateway to all personalized and transactional features. Without a working auth flow, purchase, profile, and history stories cannot function. It is independently demonstrable without any of those features.

**Independent Test**: Can be fully tested by performing a login call and verifying the access token in the response body and the HttpOnly refresh token cookie, then making a request with an expired token to confirm transparent refresh, then logging out and confirming the cookie is cleared.

**Acceptance Scenarios**:

1. **Given** a registered user, **When** they provide valid email and password, **Then** the BFF returns the access token in the response body and sets the refresh token in an HttpOnly, Secure, SameSite cookie; HTTP 200.
2. **Given** a registered user, **When** they provide incorrect credentials, **Then** the BFF returns a 401 error envelope with code `AUTHENTICATION_REQUIRED`.
3. **Given** a user with an expired access token but a valid refresh token cookie, **When** they make any authenticated request, **Then** the BFF silently refreshes the token and fulfills the request without returning 401.
4. **Given** a user whose refresh token has also expired, **When** they make any authenticated request, **Then** the BFF returns a 401 error envelope with code `AUTHENTICATION_REQUIRED` and clears the stale cookie.
5. **Given** a logged-in user, **When** they call the logout endpoint, **Then** the BFF invalidates the session and clears the HttpOnly cookie; the former refresh token is no longer usable.

---

### User Story 3 — User Registration (Priority: P3)

A new user registers by providing their email, password, CPF, phone, name, and CEP. The BFF first validates and sanitizes all inputs, then creates the user account in Keycloak, then creates the client profile in the Client API using the Keycloak subject ID. If the Client API step fails, the BFF attempts to delete the Keycloak user as a compensating action to avoid orphaned identities. The user receives a clear error in any failure scenario and can safely retry.

**Why this priority**: Registration is the entry point for all personalized features but can be deferred after a working auth loop is proven. It is more complex than login due to the two-phase orchestration and compensation logic, but is independently completable.

**Independent Test**: Can be fully tested by submitting a registration request and verifying a new user exists in both Keycloak and the Client API; then simulating a Client API failure and confirming the Keycloak user is deleted and the caller receives a clean error envelope.

**Acceptance Scenarios**:

1. **Given** a new user with valid inputs, **When** they submit the registration form, **Then** the BFF creates the Keycloak account and then the Client API profile; returns 201 with a success envelope.
2. **Given** a new user, **When** they submit a CPF in an invalid format, **Then** the BFF rejects the request immediately with a 400 validation error envelope containing field-level details; no downstream call is made.
3. **Given** the Keycloak creation succeeds but the Client API call fails, **When** registration is processed, **Then** the BFF attempts to delete the Keycloak user, returns a clear error envelope, and the user can retry without a duplicate identity error.
4. **Given** the Keycloak deletion compensating action also fails, **When** registration is processed, **Then** the BFF logs the Keycloak subject ID for manual reconciliation and returns a clear error envelope to the user.
5. **Given** a user attempts to register with an email that already exists in Keycloak, **When** registration is processed, **Then** the BFF returns a meaningful error envelope and does not create a duplicate profile.

---

### User Story 4 — Client Profile View and Update (Priority: P4)

An authenticated user views their own profile. They can update their name, phone number, address, and other editable fields. The CPF is displayed but cannot be modified through any BFF endpoint. All profile requests carry the user's JWT and are forwarded to the Client API.

**Why this priority**: Profile management is a core account feature needed before purchase (which requires a full profile snapshot) but is simpler than the purchase flow. It depends only on working auth (P2).

**Independent Test**: Can be fully tested by authenticating and calling the profile GET and PATCH endpoints; verifying that CPF updates are rejected with a 400 envelope and all other fields are updated correctly.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request their profile, **Then** the BFF calls the Client API with the user's token and returns the profile data in the standard envelope; HTTP 200.
2. **Given** an authenticated user, **When** they update allowed profile fields (name, phone, CEP), **Then** the BFF validates the inputs, forwards the update to the Client API, and returns the updated profile.
3. **Given** an authenticated user, **When** they attempt to update their CPF, **Then** the BFF rejects the request with a 400 validation error envelope; the Client API is never called.
4. **Given** an unauthenticated request, **When** the profile endpoint is accessed, **Then** the BFF returns a 401 error envelope with code `AUTHENTICATION_REQUIRED`.

---

### User Story 5 — Car Purchase (Priority: P5)

An authenticated user selects a car and initiates a purchase. The BFF first verifies the car's availability with the Car API, then fetches the full client profile and full car data in parallel, assembles the sale payload (car snapshot + client snapshot including email + sale intent), and submits it to the Sales API. If the car was sold between the availability check and the Sales API processing, the user receives a specific, meaningful error. The car cache entry is invalidated upon a successful sale.

**Why this priority**: This is the highest-value transactional flow and the culmination of all other stories. It is the last to implement because it depends on working auth (P2) and profile (P4), and it exercises the most complex resilience, parallelism, and compensation logic.

**Independent Test**: Can be fully tested end-to-end by initiating a purchase for an available car and verifying the sale is recorded; then running a race condition simulation where the car is sold before the Sales API call and confirming the `CAR_NOT_AVAILABLE` error envelope is returned.

**Acceptance Scenarios**:

1. **Given** an authenticated user and an available car, **When** they initiate a purchase, **Then** the BFF verifies availability, fetches client and car data in parallel, assembles the payload, submits to the Sales API, invalidates the car cache, and returns a 201 success envelope.
2. **Given** the car becomes unavailable between the BFF's availability check and the Sales API processing, **When** the Sales API rejects the sale, **Then** the BFF returns a specific error envelope with code `CAR_NOT_AVAILABLE` — not a generic error.
3. **Given** a purchase request, **When** one of the parallel data-fetching calls (client profile or car data) fails, **Then** the overall purchase fails immediately, the other parallel call is cancelled, and the user receives an error envelope.
4. **Given** a purchase request, **When** the Sales API bulkhead is full, **Then** the BFF returns a 503 error envelope with code `DOWNSTREAM_UNAVAILABLE`; the sale is never retried.
5. **Given** a successful purchase, **When** the BFF invalidates the car cache, **Then** the next inventory request for that car ID fetches fresh data from the Car API.

---

### User Story 6 — Purchase History (Priority: P6)

An authenticated user views a list of their past purchases. The BFF calls the Sales API with the user's token and returns the paginated purchase history in the standard envelope. This data is never cached.

**Why this priority**: Viewing history is a read-only convenience feature with no dependencies on the purchase flow beyond auth. It is last because it adds the least new capability to implement after purchase is working.

**Independent Test**: Can be fully tested by authenticating and calling the purchase history endpoint; verifying that completed purchases appear in the `data` field with pagination metadata in `meta`, and that the response is never served from cache.

**Acceptance Scenarios**:

1. **Given** an authenticated user with past purchases, **When** they request their purchase history, **Then** the BFF returns paginated results in the standard envelope with pagination metadata in `meta`; HTTP 200.
2. **Given** an unauthenticated request, **When** the history endpoint is accessed, **Then** the BFF returns a 401 error envelope.
3. **Given** an authenticated user with no purchases, **When** they request their history, **Then** the BFF returns an empty list in the `data` field (not null) with correct pagination metadata.

---

### Edge Cases

- What happens when a car is sold between the BFF's availability check and the Sales API submission (race condition)?
- How does the system handle Keycloak creating the user but the Client API registration call timing out — is the user orphaned?
- What happens when parallel calls in the purchase flow return at different speeds and one times out?
- How does the system behave when Redis is temporarily unavailable — does inventory browsing still work without caching?
- What happens when a client submits a phone number or CEP with extra whitespace, dashes, or dots — is it normalized or rejected?
- What happens when the Sales API is unreachable during purchase and the circuit breaker opens mid-surge?
- How does the transparent token refresh behave if the refresh token cookie is present but malformed?

---

## Requirements *(mandatory)*

### Functional Requirements

#### Response Envelope

- **FR-001**: Every response from the BFF MUST be wrapped in a standardized envelope regardless of HTTP status code.
- **FR-002**: Successful responses MUST include a `data` field containing the payload and a `meta` field containing `timestamp` and `requestId`.
- **FR-003**: Error responses MUST include an `error` field (with `code`, `message`, and optional `details`) and a `meta` field.
- **FR-004**: The `data` and `error` fields MUST be mutually exclusive; no response may contain both.
- **FR-005**: The `requestId` in `meta` MUST be identical to the trace ID used in all log entries for that request.
- **FR-006**: Error `code` values MUST come from a defined business-meaningful enum (e.g., `CAR_NOT_AVAILABLE`, `VALIDATION_ERROR`, `AUTHENTICATION_REQUIRED`, `RATE_LIMIT_EXCEEDED`, `DOWNSTREAM_UNAVAILABLE`).
- **FR-007**: Error `message` values MUST be safe for display to the user; they MUST never contain stack traces, internal service names, or database errors.
- **FR-008**: Validation error responses MUST include a `details` list with the field name and the specific reason it failed.
- **FR-009**: Paginated list responses MUST include page, page size, total elements, and total pages in `meta`.
- **FR-010**: A global exception handler MUST intercept all unhandled exceptions and translate them into the error envelope. The default Spring error format is explicitly prohibited.

#### Public Inventory

- **FR-011**: Unauthenticated users MUST be able to list the car inventory.
- **FR-012**: Inventory listing MUST support free-text search across model, manufacturer, year, color, and other car attributes.
- **FR-013**: Inventory MUST support filtering by: category, type, condition, manufacturer, year range, price range, external color, and kilometers range.
- **FR-014**: Inventory results MUST support sorting by price, year, and registration date.
- **FR-015**: All inventory and search results MUST be paginated; the page and page size are specified by the caller.

#### Authentication

- **FR-016**: The BFF MUST accept user login via email and password.
- **FR-017**: On successful login, the BFF MUST store the refresh token in an HttpOnly, Secure cookie inaccessible to JavaScript.
- **FR-018**: On successful login, the BFF MUST return the access token to the frontend in the response body.
- **FR-019**: When an access token expires, the BFF MUST transparently attempt a refresh using the HttpOnly cookie before returning 401.
- **FR-020**: The BFF MUST support a logout endpoint that invalidates the session and clears the HttpOnly cookie.

#### Registration

- **FR-021**: The BFF MUST create the user in Keycloak first, then create the client profile in the Client API using the Keycloak subject ID.
- **FR-022**: If the Client API call fails after Keycloak creation, the BFF MUST attempt to delete the Keycloak user as a compensating action.
- **FR-023**: If the compensating deletion fails, the BFF MUST log the Keycloak subject ID for manual reconciliation.
- **FR-024**: In all registration failure scenarios, the user MUST receive a clear error envelope and MUST be able to retry without encountering a duplicate identity error.
- **FR-025**: All registration input fields (email, CPF, CEP, phone) MUST be validated before any downstream call is made.

#### Client Profile

- **FR-026**: Authenticated users MUST be able to retrieve their profile via the Client API.
- **FR-027**: Authenticated users MUST be able to update their profile fields, except CPF.
- **FR-028**: The CPF field MUST NOT be updatable through any BFF endpoint; update attempts MUST be rejected with a 400 validation error envelope without calling the Client API.

#### Purchase Flow

- **FR-029**: Before initiating a purchase, the BFF MUST verify the car is available by calling the Car API.
- **FR-030**: The BFF MUST fetch the client's full profile and the car's full data in parallel when assembling the sale payload.
- **FR-031**: The assembled sale payload MUST include a full car snapshot, a full client snapshot (including email), and the sale intent.
- **FR-032**: If the Sales API indicates the car is no longer available, the BFF MUST return a specific error envelope with a distinct `CAR_NOT_AVAILABLE` code — not a generic error.
- **FR-033**: The BFF MUST NEVER retry the sale registration call; duplicate sale registration is not recoverable.
- **FR-034**: On a successful sale, the BFF MUST invalidate the corresponding car cache entry in Redis.

#### Purchase History

- **FR-035**: Authenticated users MUST be able to retrieve their paginated purchase history from the Sales API.

#### Security

- **FR-036**: Every value received from the client MUST be sanitized before use in any downstream call, log entry, or business logic.
- **FR-037**: Inputs failing format validation MUST be rejected immediately with a 400 structured error envelope; they MUST NOT be silently truncated or partially accepted.
- **FR-038**: CPF, CEP, and phone number inputs MUST be validated against their expected Brazilian formats.
- **FR-039**: Search queries MUST be sanitized to prevent injection before being forwarded to the Car API.
- **FR-040**: The BFF MUST propagate the authenticated user's JWT in the `Authorization` header on all protected downstream calls.
- **FR-041**: The BFF MUST strip all client-supplied internal headers before forwarding requests downstream.

#### Resilience

- **FR-042**: Every outbound call MUST be protected by a resilience chain in order: Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead → call.
- **FR-043**: Each downstream service (Car API, Client API, Sales API, Keycloak) MUST have its own independent circuit breaker instance.
- **FR-044**: Retry MUST only be applied to idempotent (non-mutating) calls; the Sales API registration call MUST never be retried.
- **FR-045**: A semaphore-based Bulkhead MUST be applied to Sales API calls to protect against purchase surges.
- **FR-046**: All resilience parameters (window size, failure threshold, retry attempts, timeouts, rate limits, bulkhead concurrency) MUST be externalized to application properties.

#### Caching

- **FR-047**: Car inventory and individual car responses from the Car API MUST be cached in Redis with a TTL of 5 minutes.
- **FR-048**: Inventory cache keys MUST be constructed deterministically from the active filter and sort parameters.
- **FR-049**: User-specific data (profile, purchase history, checkout data) MUST NEVER be cached.
- **FR-050**: The car cache entry MUST be invalidated when the BFF receives a successful sale response for that car.

#### Observability

- **FR-051**: Every request MUST be logged with: method, path, response status, latency, and authenticated subject ID (when available).
- **FR-052**: Request bodies MUST NEVER be logged.
- **FR-053**: Search queries MUST be logged in sanitized form only; raw client input MUST never appear in logs.
- **FR-054**: Resilience events (circuit breaker transitions, retry attempts, rate limiter rejections, bulkhead rejections) MUST produce structured log entries identifying the downstream service and the `requestId`.
- **FR-055**: All parallel downstream calls MUST carry the same trace ID so the full composition can be correlated in observability tooling.

---

### Key Entities

- **Car**: A vehicle in inventory. Key attributes: ID, model, manufacturer, year, external color, condition, category, type, price, kilometers, registration date, availability status.
- **Client**: A registered user's profile. Key attributes: ID, Keycloak subject ID, full name, CPF, email, phone, address (CEP).
- **Sale**: A completed purchase record. Contains a full car snapshot (immutable at time of sale), a full client snapshot (including email), sale intent, and timestamp.
- **ResponseEnvelope**: The standard wrapper for every BFF response. Contains either `data` or `error`, and always contains `meta` (timestamp + requestId).
- **ErrorCode**: A bounded enum of business-meaningful error codes. Defined values include at minimum: `CAR_NOT_AVAILABLE`, `VALIDATION_ERROR`, `AUTHENTICATION_REQUIRED`, `RATE_LIMIT_EXCEEDED`, `DOWNSTREAM_UNAVAILABLE`.
- **Token**: The Keycloak-issued access token (returned to frontend) and refresh token (stored only in HttpOnly cookie).
- **CarPage**: Paginated inventory result. Contains a list of Cars and pagination metadata (current page, page size, total elements, total pages).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All composed endpoints respond within 500 ms at p99 latency under normal operating load.
- **SC-002**: All composed endpoints respond within 300 ms at p50 latency under normal operating load.
- **SC-003**: 100% of responses — including all error paths — use the standard envelope format; no raw Spring error response ever reaches the frontend.
- **SC-004**: Unit test line coverage and branch coverage are each ≥ 90% for all non-generated application code.
- **SC-005**: Mutation score (PITest) is ≥ 90% for all tested application code.
- **SC-006**: Every resilience scenario (circuit open, retry exhausted, rate limit rejected, bulkhead full) has a dedicated integration test that confirms the correct error envelope is returned.
- **SC-007**: Security integration tests confirm: unauthenticated access to protected endpoints is rejected with an error envelope; incorrect-role access is rejected; malformed CPF/CEP/phone is blocked with a 400 envelope.
- **SC-008**: A car that has just been sold stops appearing as available in the inventory within 5 minutes for any user.
- **SC-009**: The refresh token is never accessible via JavaScript (verified by confirming the HttpOnly flag on the cookie in integration tests).
- **SC-010**: All parallel purchase assembly calls are confirmed to execute concurrently (not sequentially) in integration tests, as measured by total elapsed time vs. sum of individual call durations.

---

## Assumptions

- Keycloak is pre-configured with a `dealership-bff` client that supports the Resource Owner Password Credentials flow, and a `dealership-system` client for service-to-service credentials.
- The Car API, Client API, and Sales API are already deployed; their contracts and base URLs are stable and provided via externalized configuration.
- A Redis (ElastiCache-compatible) instance is available in the deployment environment for inventory caching.
- The BFF and the frontend share a domain or the deployment environment is configured to permit cross-origin HttpOnly cookies with the correct `SameSite` policy.
- The New Relic Java Agent is deployed in the runtime environment; no New Relic SDK calls will appear in application code.
- The BFF has no database of its own; all persistent state lives in the downstream services.
- If Redis becomes temporarily unavailable, the BFF continues to serve inventory requests directly from the Car API (cache miss degradation is acceptable).
- Brazilian-standard formats apply to CPF (11 digits with check digits), CEP (8 digits), and phone numbers.
- "Full client snapshot" in the sale payload means all client profile fields at the time of purchase, including email obtained from Keycloak or the Client API — not a reference to a future profile state.
- Logout behavior includes both clearing the HttpOnly cookie on the BFF side and revoking the token at Keycloak via the backchannel logout endpoint.
