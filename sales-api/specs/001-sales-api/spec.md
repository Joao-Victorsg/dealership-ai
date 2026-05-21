# Feature Specification: Sales API

**Feature Branch**: `001-sales-api`
**Created**: 2026-04-19
**Status**: Ready
**Input**: User description: "Develop the Sales API, the transaction management service for an automotive dealership platform..."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Register a Sale (Priority: P1)

An authenticated client submits a complete sale request through the BFF.
The BFF has already assembled the client snapshot, the car snapshot, and
the car's current status. The Sales API validates the request, applies
the 10% tax to the car value, persists the sale record, and publishes the
sale event. The client receives the newly created sale record in the
response, including the final sale value with tax applied.

**Why this priority**: This is the core capability of the service — every
other story depends on sale records existing. Nothing downstream works
without it.

**Independent Test**: Can be fully tested by submitting a valid sale
request with an Available car snapshot and asserting the response contains
the correct SaleId, registration date, and final value (car value × 1.10),
as well as verifying the sale event was published.

**Acceptance Scenarios**:

1. **Given** an authenticated client with ROLE_CLIENT whose identity matches the ClientId in the request, **When** they submit a valid sale request with an Available car, **Then** the sale is persisted, the final value equals the car value plus 10% tax, a sale event is published, and the response returns 201 with the sale details.
2. **Given** a valid sale request, **When** the car status in the snapshot is Sold, **Then** the sale is rejected with 422 and no event is published.
3. **Given** a valid sale request, **When** the car status in the snapshot is Unavailable, **Then** the sale is rejected with 422 and no event is published.
4. **Given** an authenticated client with ROLE_CLIENT, **When** they submit a sale where the ClientId does not match their own identity, **Then** the sale is rejected with 403.
5. **Given** a valid sale request, **When** the SNS event publish fails after all retries, **Then** the sale is NOT persisted and the caller receives 503 Service Unavailable.
6. **Given** an unauthenticated request, **When** the sale registration endpoint is called, **Then** the response is 401.

---

### User Story 2 — Retrieve My Sales (Priority: P2)

An authenticated client queries their own sales history. The Sales API
returns a paginated list of sale records belonging to the authenticated
client. The client may filter results by registration date range.

**Why this priority**: Clients need visibility into their own purchase
history. This is the primary read use case for the client role and
delivers direct user value independently of any other read story.

**Independent Test**: Can be fully tested by creating several sales for a
client and querying the endpoint with and without date filters, asserting
the correct records and correct pagination metadata are returned.

**Acceptance Scenarios**:

1. **Given** an authenticated client with ROLE_CLIENT, **When** they request their sales without filters, **Then** they receive a paginated list of their own sale records.
2. **Given** an authenticated client with ROLE_CLIENT, **When** they filter by a date range, **Then** only sales registered within that range are returned.
3. **Given** an authenticated client with ROLE_CLIENT who has no sales, **When** they request their sales, **Then** they receive an empty paginated response (not an error).
4. **Given** an authenticated client with ROLE_CLIENT, **When** they attempt to retrieve sales belonging to another client, **Then** they receive 403.
5. **Given** a request without pagination parameters, **When** the list endpoint is called, **Then** the response applies the default page size (20) without error.
6. **Given** an authenticated client with ROLE_CLIENT, **When** they request a single sale by its identifier that belongs to them, **Then** they receive 200 with the full sale details.
7. **Given** an authenticated client with ROLE_CLIENT, **When** they request a single sale by its identifier that belongs to another client, **Then** they receive 403.

---

### User Story 3 — Staff Retrieves Sales Records (Priority: P3)

An authenticated staff or admin user queries sales records for operational
purposes. They may filter by ClientId, by CarId, or by registration date
range. Results are paginated.

**Why this priority**: Operational visibility for staff is important but
not user-facing. Sales must exist first (P1), and the client-facing read
story (P2) takes precedence as it directly serves the primary actor.

**Independent Test**: Can be fully tested by creating sales and querying
the staff endpoint with various filter combinations, asserting correct
results and that clients cannot access this endpoint.

**Acceptance Scenarios**:

1. **Given** an authenticated user with ROLE_STAFF or ROLE_ADMIN, **When** they query sales without filters, **Then** they receive a paginated list of all sales.
2. **Given** an authenticated user with ROLE_STAFF or ROLE_ADMIN, **When** they filter by ClientId, **Then** only sales for that client are returned.
3. **Given** an authenticated user with ROLE_STAFF or ROLE_ADMIN, **When** they filter by CarId, **Then** only the sale for that car is returned (at most one, since a car can only be sold once).
4. **Given** an authenticated user with ROLE_STAFF or ROLE_ADMIN, **When** they filter by a date range, **Then** only sales registered within that range are returned.
5. **Given** an authenticated client with ROLE_CLIENT, **When** they attempt to access the staff sales endpoint, **Then** they receive 403.
6. **Given** an authenticated user with ROLE_STAFF or ROLE_ADMIN, **When** they request a single sale by its identifier, **Then** they receive 200 with the full sale details regardless of which client owns it.

---

### Edge Cases

- What happens when a sale request omits the CarId or ClientId? → 400 with a field-level validation error.
- What happens when the car value in the snapshot is zero or negative? → 400 with a field-level validation error.
- What happens when the client snapshot is incomplete (missing required fields)? → 400 with field-level errors for each missing field.
- What happens when the car snapshot is incomplete? → 400 with field-level errors.
- What happens when the SNS circuit breaker is open at registration time? → Sale is rejected immediately with 503 Service Unavailable; no persistence occurs.
- What happens when a request is made without a valid JWT? → 401.
- What happens when the JWT audience (`aud`) claim does not match `dealership`? → 401.
- What happens when a client requests a specific SaleId that belongs to another client? → 403, not 404, to prevent IDOR.
- What happens when pagination parameters exceed `max-page-size=100`? → Response is capped at 100 results.
- What happens when two clients simultaneously attempt to register a sale for the same car? → The unique constraint on `car_id` ensures only one succeeds; the other receives 422 with a "car already sold" error.

---

## Requirements *(mandatory)*

### Functional Requirements

**Sale Registration**

- **FR-001**: The system MUST accept a sale registration request containing a CarId, a ClientId, the full client snapshot, and the full car snapshot, including the car's current status.
- **FR-002**: The system MUST apply a 10% tax to the car value from the snapshot and store the result as the final sale value. The caller MUST NOT pre-calculate this value.
- **FR-003**: The system MUST reject sale registration when the car status in the snapshot is anything other than Available, returning 422.
- **FR-004**: The system MUST reject sale registration when the authenticated caller's identity does not match the ClientId in the request, returning 403.
- **FR-005**: The system MUST publish a self-contained sale event upon successful registration. The event payload MUST be serialized as a JSON string in the SNS message body. The payload MUST include the full client snapshot, the full car snapshot, the SaleId, the registration date, and the final sale value.
- **FR-006**: The system MUST NOT persist the sale if the event publication fails after all retries. Both operations succeed or neither does. The caller MUST receive 503 Service Unavailable when this occurs.
- **FR-007**: The system MUST generate a UUID v4 as the unique identifier for every sale at registration time.
- **FR-008**: The system MUST record the exact timestamp of registration for every sale.

**Sale Retrieval**

- **FR-009**: Authenticated clients MUST be able to retrieve their own sales, with optional filtering by registration date range.
- **FR-010**: Authenticated staff and admin users MUST be able to retrieve any sale, with optional filtering by ClientId, CarId, and registration date range.
- **FR-011**: All list endpoints MUST return paginated results. Default page size is 20; maximum is 100. Unbounded queries are not permitted.
- **FR-012**: A client MUST NOT be able to retrieve a sale belonging to another client. Such attempts MUST return 403.
- **FR-018**: The system MUST expose a single-sale retrieval endpoint (`GET /sales/{id}`). Authenticated clients may only retrieve sales that belong to them (403 otherwise); authenticated staff and admin may retrieve any sale by identifier.

**Immutability**

- **FR-013**: Once registered, no field of a sale record may be modified.
- **FR-014**: Sales MUST NOT be deleted under any circumstance.
- **FR-017**: The system MUST enforce a unique constraint on CarId across all sale records. If two concurrent registrations attempt to record the same CarId, the one that loses the constraint race MUST be rejected with 422 and a descriptive "car already sold" error.

**Security**

- **FR-015**: All endpoints MUST require a valid authenticated JWT. Requests without a valid token MUST be rejected with 401.
- **FR-016**: The system MUST validate the JWT `aud` claim against the value `dealership` in all non-local environments. Tokens with an incorrect audience MUST be rejected with 401.

### Key Entities

- **Sale**: The central record of a completed transaction. Carries a UUID v4 identifier, registration timestamp, the final sale value (with tax applied), the CarId, the ClientId, the client snapshot (name, CPF, address, email), and the car snapshot (model, year, manufacturer, external color, internal color, optional items, type, category, VIN, value). Append-only — never updated, never deleted.

- **Client Snapshot**: The state of the client at the moment of purchase. Fields: first name, last name, CPF, address (street, number, complement, neighborhood, city, state, postcode), and email. Stored as part of the sale record.

- **Car Snapshot**: The state of the car at the moment of purchase. Fields: model, manufacturing year, manufacturer, external color, internal color, optional items (list), type, category, VIN, and listed value. Also includes the car's status at the time of the request (must be Available). Stored as part of the sale record.

- **Sale Event**: The outbound event published to SNS upon successful registration. Self-contained — carries the full client snapshot, full car snapshot, SaleId, registration date, and final value. Serialized as a JSON string in the SNS message body (no schema registry, no message attributes). No downstream consumer needs to make additional service calls.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A valid sale can be registered end-to-end — including event publication — in under 3 seconds under normal operating conditions.
- **SC-002**: 100% of sale registrations either fully succeed (persisted + event published) or fully fail (neither persisted nor event published). There are no partial outcomes.
- **SC-003**: All business rule violations (invalid car status, ownership mismatch, missing fields) produce a structured error response with a clear, field-level description — zero generic error messages for predictable conditions.
- **SC-004**: All list endpoints return results within 1 second for data sets up to 10,000 sale records with standard pagination.
- **SC-005**: The service handles transient SNS failures gracefully — retrying up to 3 times — without exposing retry mechanics to the caller.
- **SC-006**: 100% of business rules (tax calculation, car status check, ownership check) have corresponding automated tests. No rule is untested.
- **SC-007**: Integration test suite covers the full sale registration flow, the SNS publish failure rollback scenario, and all security rules (authentication, authorization, ownership).

---

## Assumptions

- The BFF is the sole caller of this API. It assembles the complete sale request — including both snapshots and the car's current status — before calling the Sales API. The Sales API does not validate whether the BFF assembled the request correctly; it trusts the snapshot as accurate at the time of submission.
- The CarId and ClientId in the request are UUIDs issued by the Car API and Client API respectively. The Sales API stores them as opaque identifiers and does not verify their existence against those services.
- The car's status field in the snapshot is the status at the moment the BFF read it. The Sales API enforces the Available rule independently and rejects if the value is not Available — but it does not re-fetch the car status from the Car API.
- The client's Keycloak subject ID is the same identifier used as the ClientId. The Sales API compares the JWT subject claim directly to the ClientId field in the request to enforce ownership.
- A given car can appear in at most one successful sale. The Sales API enforces this via a unique database constraint on CarId — the downstream car status update is a complementary guard, not the primary one.
- The SNS topic already exists in the target environment. The Sales API does not create it.
- Pagination uses offset-based paging (page number + page size) consistent with the rest of the dealership platform.
- There is no rate limiting at the API level — rate limiting is handled at the API gateway layer.

---

## Clarifications

### Session 2026-04-19

- Q: How should the system handle concurrent sale registration for the same car? → A: Enforce a unique database constraint on `car_id`; the registration that loses the race is rejected with 422 and a "car already sold" error.
- Q: Should there be a single-sale GET endpoint in addition to list endpoints? → A: Yes — `GET /sales/{id}` is required for both roles: clients may retrieve only their own sale (403 if not theirs), staff and admin may retrieve any sale.
- Q: What HTTP status should be returned when the SNS publish fails after all retries? → A: 503 Service Unavailable — the request was valid; the failure is an external infrastructure problem.
- Q: What format should SaleId use? → A: UUID v4, consistent with CarId and ClientId across the platform.
- Q: What format should the SNS sale event payload use? → A: JSON string as the SNS message body (no schema registry, no message attributes).
