# Feature Specification: Client API — Customer Profile Management

**Feature Branch**: `001-client-api-profile`  
**Created**: 2026-04-17  
**Status**: Draft  
**Input**: Customer profile management service for an automotive dealership platform

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Register a New Client Profile (Priority: P1)

A new customer has already been registered in Keycloak by the BFF. The BFF
now creates the client's business profile in the Client API by sending the
Keycloak subject ID along with the client's personal information and address
postcode. The service resolves the full address automatically via the postal
code lookup service and stores the complete profile.

**Why this priority**: Registration is the foundational event. Without a
profile, no other operation in this service is possible. It is the entry
point for all downstream user stories.

**Independent Test**: Create a client profile through the registration
endpoint and verify the returned profile contains the correct personal data,
a resolved address, and a recorded registration date.

**Acceptance Scenarios**:

1. **Given** a valid Keycloak subject ID and well-formed personal data (name,
   CPF, phone, postcode, street number), **When** a profile creation request is
   submitted, **Then** the system stores the profile, resolves the full address
   via the postal code lookup, sets `isAddressSearched = true`, and returns the
   complete profile with a generated internal UUID and registration timestamp.

2. **Given** a valid profile creation request where the postal code lookup
   service is unavailable, **When** the profile creation request is submitted,
   **Then** the system still creates the profile successfully, stores the
   postcode and street number, leaves the remaining address fields empty, sets
   `isAddressSearched = false`, and returns the profile.

3. **Given** a CPF that already exists on another profile, **When** a
   registration request is submitted with that CPF, **Then** the system rejects
   the request with a 422 error identifying the CPF uniqueness violation.

4. **Given** a Keycloak subject ID that already exists on another profile,
   **When** a registration request is submitted with that subject ID, **Then**
   the system rejects the request with a 422 error identifying the duplicate
   subject ID.

5. **Given** a registration request with a malformed CPF (invalid format or
   failing digit verification), **When** the request is submitted, **Then** the
   system rejects the request with a 400 error identifying the invalid CPF field.

6. **Given** a registration request with a malformed phone number (not
   conforming to Brazilian format with country code and area code), **When** the
   request is submitted, **Then** the system rejects the request with a 400
   error identifying the invalid phone number field.

---

### User Story 2 — View Own Profile (Priority: P2)

A registered client wants to view their own profile data as stored in the
system. They send an authenticated request and receive their complete profile.

**Why this priority**: Profile reading is the most frequent operation and is
a prerequisite for any self-service action.

**Independent Test**: Authenticate as a client and retrieve their profile;
verify that personal data, address, and registration date are returned
correctly and no data from another client is accessible.

**Acceptance Scenarios**:

1. **Given** an authenticated client with a valid token, **When** they request
   their own profile, **Then** the system returns the full profile matching
   their Keycloak subject ID.

2. **Given** an authenticated client, **When** they attempt to access a profile
   that does not belong to them (by any means of ID manipulation), **Then** the
   system returns 403 — never 404.

3. **Given** an unauthenticated request, **When** the profile endpoint is
   called, **Then** the system returns 401.

---

### User Story 3 — Update Own Profile (Priority: P3)

A registered client wants to update their personal information. They can
change their first name, last name, phone number, and/or address. The system
validates the changes and re-attempts the postal code lookup if the address
is updated.

**Why this priority**: Self-service profile updates are core to the platform
experience and reduce support burden.

**Independent Test**: Authenticate as a client, send an update with a new
phone number and address postcode, and verify the profile reflects the
changes with a re-attempted address resolution.

**Acceptance Scenarios**:

1. **Given** an authenticated client, **When** they submit an update with valid
   new first name, last name, and phone number, **Then** the system updates
   those fields and returns the updated profile.

2. **Given** an authenticated client updating their address with a new postcode
   and street number, **When** the postal code lookup succeeds, **Then** all
   address fields are updated and `isAddressSearched` is set to `true`.

3. **Given** an authenticated client updating their address with a new postcode
   and street number, **When** the postal code lookup fails, **Then** only the
   postcode and street number are updated, `isAddressSearched` remains or is
   set to `false`, and the request succeeds.

4. **Given** an authenticated client, **When** they attempt to update a
   read-only field (CPF, Keycloak subject ID, registration date, internal UUID)
   via the standard update endpoint, **Then** those fields are silently ignored
   and the rest of the update is applied normally.

5. **Given** an authenticated client, **When** they attempt to update another
   client's profile, **Then** the system returns 403.

---

### User Story 4 — Administrator CPF Correction (Priority: P4)

An administrator needs to correct the CPF of a specific client due to a
registration error. This is a restricted operation not available to any
other role.

**Why this priority**: CPF is immutable under normal circumstances but
requires an administrative escape hatch for correction. Access control must
be strict to prevent misuse.

**Independent Test**: Authenticate as ROLE_ADMIN, invoke the CPF correction
endpoint for an existing client with a valid new CPF, and verify the profile
reflects the updated CPF. Verify that ROLE_CLIENT and ROLE_STAFF cannot
invoke the same endpoint.

**Acceptance Scenarios**:

1. **Given** an authenticated ROLE_ADMIN, **When** they submit a CPF correction
   request for an existing client with a valid, unique CPF, **Then** the CPF is
   updated and the updated profile is returned.

2. **Given** an authenticated ROLE_ADMIN, **When** they submit a CPF correction
   with a CPF that already belongs to another client, **Then** the system
   returns 422 with a uniqueness violation error.

3. **Given** an authenticated ROLE_ADMIN, **When** they submit a CPF correction
   with a malformed CPF, **Then** the system returns 400.

4. **Given** an authenticated ROLE_CLIENT or ROLE_STAFF, **When** they attempt
   to call the CPF correction endpoint, **Then** the system returns 403.

---

### User Story 5 — Request Account Deletion (Priority: P5)

A client requests the permanent deletion of their own account. The system
anonymizes all personal data in place, records a deletion timestamp, and
permanently deactivates the profile. The profile record is retained for
referential integrity.

**Why this priority**: Account deletion is a user right and a compliance
requirement, but less frequently exercised than reads and updates.

**Independent Test**: Authenticate as a client, invoke the deletion endpoint,
and verify all personal fields are replaced with non-identifiable values, the
Keycloak subject ID link is broken, and the profile cannot be updated or
re-accessed by the original token.

**Acceptance Scenarios**:

1. **Given** an authenticated client, **When** they invoke the account deletion
   endpoint, **Then** the system replaces all personal fields (first name, last
   name, CPF, phone number, address fields, Keycloak subject ID) with
   non-identifiable values, records a deletion timestamp, and returns 204.

2. **Given** an anonymized profile, **When** any update operation is attempted
   on it, **Then** the system rejects the request with a 422 error indicating
   the profile is inactive.

3. **Given** an authenticated client, **When** they attempt to delete another
   client's profile, **Then** the system returns 403.

4. **Given** an anonymized profile's cache entry, **When** the anonymization
   completes, **Then** the cache entry is invalidated immediately before the
   response is returned.

---

### User Story 6 — Address Retry (Priority: P6)

An external service retries the postal code lookup for clients whose address
was not successfully resolved at registration. It invokes `PATCH /clients/{id}`, providing the existing postcode and street
number. The Client
API applies the same resolution logic without distinguishing the caller.

**Why this priority**: Ensures data completeness over time without coupling
the Client API to an external retry scheduler.

**Independent Test**: Create a profile with `isAddressSearched = false`, then
invoke `PATCH /clients/{id}` with the same postcode and street number using a
ROLE_SYSTEM token, and verify that a successful lookup sets
`isAddressSearched = true`.

**Acceptance Scenarios**:

1. **Given** a profile with `isAddressSearched = false` and a valid postcode,
   **When** ROLE_SYSTEM calls `PATCH /clients/{id}` with the existing `postcode`
   and `streetNumber`, **Then** the system attempts the lookup; on success, it updates
   all address fields and sets `isAddressSearched = true`.

2. **Given** the same scenario where the lookup fails again, **When** the
   address update is processed, **Then** `isAddressSearched` remains `false`
   and only the postcode and street number are persisted.

---

### Edge Cases

- What happens when a client's profile does not exist for the authenticated
  subject? → 403 (never 404, to prevent profile existence disclosure).
- What happens when the JSON body contains extra unknown fields? → They are
  silently ignored; no error is raised.
- What happens when the postal code provided does not exist in ViaCEP? → The
  lookup is treated as a failure; `isAddressSearched` is set to `false` and
  only the postcode and street number are stored.
- What happens when multiple concurrent requests try to register the same CPF?
  → The uniqueness constraint is enforced at the data layer; only one succeeds
  and the other receives a 422.
- What happens when an anonymized client attempts to register again with the
  same CPF and Keycloak subject ID? → Registration proceeds normally.
  Anonymization replaces both `keycloakId` and `cpfHash` with random values,
  so no uniqueness constraint is violated; a fresh profile is created.

---

## Requirements *(mandatory)*

### Privacy & Security Requirements *(constitutional — non-negotiable)*

> These requirements flow from the Client API Constitution and apply to every
> feature. They are not optional and do not need re-justification per feature.

- **SEC-001**: ROLE_CLIENT (own profile) is the only role that may read profile
  data. ROLE_SYSTEM MAY invoke `PATCH /clients/{id}` for address retry only.
  ROLE_ADMIN MAY invoke the CPF correction endpoint only. ROLE_STAFF MUST
  have zero access to all endpoints.
- **SEC-002**: All endpoints MUST require a valid JWT; there are no public
  endpoints.
- **SEC-003**: Access to a resource not owned by the authenticated subject MUST
  return 403, never 404.
- **SEC-004**: CPF and personal identifiers MUST NOT appear in logs or error
  messages.
- **SEC-005**: Immutable fields (Keycloak subject ID, internal UUID, CPF,
  registration date) MUST be silently ignored or rejected if included in
  standard update payloads.
- **SEC-006**: Cache entries for modified or deleted profiles MUST be
  invalidated before returning a response.

### Functional Requirements

- **FR-001**: The system MUST allow creation of a client profile carrying:
  internal UUID, Keycloak subject ID, first name, last name, CPF, phone
  number, address (postcode, street number, street name, city, state, and
  `isAddressSearched`), and registration date.

- **FR-002**: The system MUST validate CPF using the official Brazilian format
  and digit verification algorithm; duplicate CPFs across profiles MUST be
  rejected.

- **FR-003**: The system MUST validate that phone numbers follow the Brazilian
  format (country code + area code + number).

- **FR-004**: The system MUST attempt to resolve street name, city, and state
  from the postcode via the postal code lookup service at profile creation and
  address update.

- **FR-005**: If the postal code lookup fails for any reason, the system MUST
  still persist the profile/update with only the postcode and street number;
  `isAddressSearched` MUST be set to `false`.

- **FR-006**: `isAddressSearched` MUST be set to `true` only when all address
  fields are populated through a successful postal code lookup.

- **FR-007**: The system MUST enforce uniqueness of Keycloak subject IDs across
  all profiles; duplicate subject IDs MUST be rejected with 422.

- **FR-008**: A client MUST be able to update first name, last name, phone
  number, and/or address (postcode + street number triggering ViaCEP
  re-resolution) through a single endpoint. Address fields are conditional:
  if `postcode` is present in the request, `streetNumber` MUST also be
  present, and vice versa. All other fields in update payloads MUST be
  silently ignored.

- **FR-009**: An administrator MUST be able to correct the CPF of any client
  through a dedicated restricted endpoint, subject to the same format and
  uniqueness validations.

- **FR-010**: Account deletion MUST anonymize all personal fields in place and
  record a deletion timestamp; the internal UUID MUST be preserved.

- **FR-011**: An anonymized profile MUST be permanently inactive; no update or
  re-activation is permitted.

- **FR-012**: The system MUST reject requests without a valid JWT with 401.

- **FR-013**: The system MUST reject cross-profile access attempts with 403.

- **FR-014**: Validation errors MUST return 400 with a structured body
  identifying each invalid field.

- **FR-015**: Business rule violations (duplicate CPF, inactive profile, etc.)
  MUST return 422 with a descriptive error code.

- **FR-017**: All endpoints MUST be documented via the API documentation tooling;
  undocumented endpoints MUST NOT be shipped.

- **FR-018**: ROLE_SYSTEM tokens MUST be accepted on `PATCH /clients/{id}` to
  support the external address retry service. ROLE_SYSTEM bypasses the
  ownership check and may update any client's address.

### Key Entities

- **Client**: The central entity. Represents a registered customer's business
  profile. Key attributes: internal UUID, Keycloak subject ID, first name, last
  name, CPF (sensitive), phone number, address, registration date, deletion
  timestamp (nullable). Has exactly one Address.

- **Address**: Embedded within a Client profile. Attributes: postcode (CEP),
  street number (client-provided), street name (resolved), city (resolved),
  state abbreviation (resolved), `isAddressSearched` flag.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client profile can be created end-to-end in under 300 milliseconds
  even when the postal code lookup service is unavailable.

- **SC-002**: 100% of requests to access or modify a profile that does not
  belong to the authenticated subject are rejected with 403 — zero information
  disclosure.

- **SC-003**: Every client profile creation attempt succeeds regardless of postal
  code lookup availability; no registration is lost due to an external service
  failure.

- **SC-004**: Account deletion irreversibly removes all personally identifiable
  information from the system within a single transaction; no PII survives the
  operation in readable form.

- **SC-005**: 100% of CPF uniqueness violations and malformed CPF/phone inputs
  are caught before persistence and returned with descriptive validation errors.

- **SC-006**: The CPF correction restricted endpoint is accessible only to
  ROLE_ADMIN; 100% of attempts from other roles are rejected with 403.

- **SC-007**: All address fields are populated correctly for profiles where the
  postal code lookup succeeds; the `isAddressSearched` flag accurately reflects
  the state of address resolution at all times.

---

## Assumptions

- The BFF layer orchestrates user creation in Keycloak and subsequently calls
  the Client API to create the business profile. The Client API is not involved
  in Keycloak user management.

- The Keycloak subject ID is always present and valid in the JWT; the Client API
  trusts the token's `sub` claim as the identity anchor.

- The postal code lookup service (ViaCEP) is a public HTTP API; its unavailability
  is handled via circuit breaker and graceful degradation, not retry within the
  same request.

- The external address retry service uses ROLE_SYSTEM credentials and calls the
  standard address update endpoint; no dedicated retry-specific endpoint is
  needed.

- Anonymized profiles remain in the database indefinitely for referential
  integrity; purging is out of scope for this service.

- Profile data is cached with a 24-hour TTL; cache consistency is maintained by
  invalidating cache entries on every write before returning a response.

- No multi-tenancy is required; all client profiles belong to a single
  dealership platform instance.

- The internal UUID is generated by the system at profile creation; callers do
  not provide it.
