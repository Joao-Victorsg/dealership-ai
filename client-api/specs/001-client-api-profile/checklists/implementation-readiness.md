# Implementation Readiness Checklist: Client API — Customer Profile Management

**Purpose**: Validate that requirements are complete, clear, and consistent enough to begin implementation without ambiguity blockers — with emphasis on security & authorization correctness
**Created**: 2026-04-18
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md)
**Depth**: Standard (20–30 items) | **Audience**: Author, pre-implementation self-review
**Focus**: All dimensions, security & authorization emphasized

---

## Security & Authorization Requirement Completeness

- [ ] CHK001 — Is the ROLE_SYSTEM ownership bypass rule for `PATCH /clients/{id}` documented with enough precision that an implementer can derive the authority-check condition unambiguously — i.e., does the spec state the exact mechanism (`SecurityContextHolder` check on ROLE_SYSTEM authority) rather than relying on research.md or task prose? [Clarity, FR-018, SEC-001, tasks T030]

- [ ] CHK002 — Is ROLE_ADMIN access explicitly restricted to only the CPF correction endpoint in the spec? SEC-001 states ROLE_ADMIN "MAY invoke the CPF correction endpoint only", but does any part of the spec explicitly state that ROLE_ADMIN cannot read profile data via `GET /clients/me`? Without this negative statement, the access boundary is incomplete. [Completeness, SEC-001, Gap]

- [ ] CHK003 — Is ROLE_STAFF zero-access stated at a per-endpoint level, or only as a global rule in SEC-001? A single global statement may not be sufficient — can the access denial for ROLE_STAFF be verified for each of the 5 endpoints independently? [Completeness, SEC-001]

- [ ] CHK004 — Is the "403, never 404" rule for ownership mismatches traceable through BOTH the service-layer behavior (throw `ClientNotFoundException` for mismatch) AND the exception handler mapping (`ClientNotFoundException` → 403)? Both layers must be specified to guarantee the policy is implemented consistently. [Consistency, SEC-003, tasks T016, T030, T042]

- [ ] CHK005 — Is the re-registration edge case (anonymized client re-registers with the same CPF and Keycloak subject ID) fully consistent with FR-007 (duplicate keycloakId → 422)? The edge case states re-registration succeeds because anonymization randomizes both values — but does the spec confirm the randomization happens BEFORE any constraint check, so FR-007 is never triggered? [Consistency, FR-007, Edge Cases]

---

## API Design Requirement Clarity

- [ ] CHK006 — Is the conditional co-dependency rule (`postcode` and `streetNumber` must arrive together or not at all) classified as a **validation error (400)** or a **business rule violation (422)**? FR-008 and FR-014 both apply here — the spec must explicitly assign this case to one response code, not leave it to implementer interpretation. [Clarity, FR-008, FR-014]

- [ ] CHK007 — Is the behavior of `PATCH /clients/{id}` when all optional fields are absent (empty JSON body `{}`) specified? Is an all-absent payload silently accepted as a no-op (200 + unchanged profile) or rejected as a 400 (at least one field required)? [Coverage, FR-008, Gap]

- [ ] CHK008 — Is `addressSearched` named consistently across all specification artifacts? The spec prose uses `isAddressSearched`, `data-model.md` uses `addressSearched`, and `contracts/openapi.yml` uses `addressSearched`. Can the implementer determine the authoritative JSON field name and Java property name without resolving a naming conflict? [Clarity, data-model.md, Spec §FR-006]

- [ ] CHK009 — Is the behavior of `PATCH /clients/{id}` when ONLY personal fields (no address fields) are sent fully specified for the ROLE_SYSTEM case? US6/FR-018 documents ROLE_SYSTEM sending address fields for retry — but is it defined what happens when ROLE_SYSTEM sends personal-only updates? Should this be rejected or silently accepted? [Coverage, FR-018, Gap]

- [ ] CHK010 — Are the HTTP semantics for `PATCH /clients/{id}/cpf` on an anonymized profile specified? The spec covers inactive-profile rejection for personal and address updates (FR-011), but does it explicitly extend this constraint to the CPF correction endpoint as well? [Coverage, FR-011, Gap]

---

## Data Model & Persistence Requirement Completeness

- [ ] CHK011 — Is the CPF anonymization replacement value precisely specified to be compatible with the `CpfEncryptionConverter`? The spec states "random UUID string" — but a UUID string is 36 characters, not 11 digits. Does the spec (not just tasks.md) confirm that the converter handles arbitrary-length plaintext, or does the CPF validation regex need to be bypassed at anonymization time? [Clarity, FR-010, Gap]

- [ ] CHK012 — Is the cache eviction strategy unified and documented in a single authoritative location? After the D1 fix, all writes evict by `keycloakId` via programmatic eviction — but is this rule stated in the spec/plan, or only scattered across individual task descriptions in tasks.md? [Consistency, SEC-006, Spec Assumptions]

- [ ] CHK013 — Does `data-model.md` explicitly document the `addressSearched = false` value applied during anonymization, or is this detail only found in tasks.md (T042)? The spec should be the authority on what anonymization produces. [Completeness, FR-010, data-model.md]

- [ ] CHK014 — Is the `cpf VARCHAR(100)` column size in the Flyway migration sufficient for AES-256-GCM ciphertext? The `data-model.md` states 100 chars is sufficient for base64-encoded ciphertext — is this rationale documented and verifiable (e.g., what is the maximum ciphertext length formula)? [Clarity, data-model.md, research.md]

---

## Acceptance Criteria Quality & Measurability

- [ ] CHK015 — Is SC-001 (300 ms profile creation when ViaCEP is unavailable) achievable and measurable in a Testcontainers integration test? The 300 ms budget includes Spring context, JPA write, WireMock network roundtrip, and Redis check. Is there a risk this threshold is environment-dependent and produces flaky results? [Measurability, SC-001]

- [ ] CHK016 — Is SC-002 ("100% of cross-profile requests rejected with 403") traceable to security integration tests covering ALL 5 endpoints, not just `GET /clients/me` and `PATCH /clients/{id}`? Do T040, T041, T046, T048 collectively cover `PATCH /clients/{id}/cpf` and `DELETE /clients/{id}` for cross-profile attempts? [Traceability, SC-002, [Gap]]

- [ ] CHK017 — Is SC-004 ("no PII survives anonymization in readable form") objectively verifiable? Does "readable form" explicitly include the `cpf` column containing a new encrypted random value (not empty string), and the `cpf_hash` column containing a new random hash? Can a test independently verify each anonymized field in the DB row? [Measurability, SC-004, FR-010]

---

## Scenario Coverage

- [ ] CHK018 — Is the ViaCEP "unknown postcode" scenario (`"erro": true` in response) explicitly distinguished from a network failure at the specification level? Both currently produce `addressSearched = false`, but do they trigger the circuit breaker differently? Does any part of the spec (not just research.md) state this parity? [Clarity, FR-005, Gap]

- [ ] CHK019 — Is the scenario where ROLE_SYSTEM calls `PATCH /clients/{id}` on an anonymized profile explicitly specified? FR-011 says "permanently inactive; no update or re-activation permitted" — does this apply to ROLE_SYSTEM callers too, or does ROLE_SYSTEM bypass the inactive check as well as the ownership check? [Coverage, FR-011, FR-018, Gap]

- [ ] CHK020 — Is the race condition window between the service-layer `cpf_hash` uniqueness check and the database `INSERT` addressed in the spec? For concurrent registrations with the same CPF, the service check may pass for both before either commits — does the spec require the DB constraint to be the final enforcement layer with a 422 mapping? [Coverage, FR-007, Edge Cases]

- [ ] CHK021 — Is there a specified scenario for what happens when `PATCH /clients/{id}` receives a postcode that ViaCEP recognizes but has no street name (e.g., a PO Box CEP that returns only the city)? Should `addressSearched` be `true` or `false` when partial but not empty data is returned? [Coverage, FR-005, FR-006, Gap]

---

## Testing Requirement Coverage

- [ ] CHK022 — Does the spec explicitly require integration tests to assert that `cpf` and `keycloakId` are absent from ALL API response bodies, not just implied by listing the `ClientResponse` fields? This is the primary guard against accidental PII exposure in responses. [Completeness, SEC-004, Gap]

- [ ] CHK023 — Is the re-registration edge case (anonymized client registers again) covered by at least one integration test task? No test currently validates that a fresh `POST /clients` with a previously-used CPF succeeds after that CPF has been anonymized. [Coverage, Edge Cases, Gap]

- [ ] CHK024 — Are security integration tests (`ClientControllerSecurityIT`) scoped to cover all 5 endpoints for the full role matrix (ROLE_CLIENT, ROLE_ADMIN, ROLE_SYSTEM, ROLE_STAFF, unauthenticated)? Or are some endpoint × role combinations tested only in `ClientControllerIT`? The division between the two IT files should be specified. [Completeness, Constitution Article IX]

---

## Constitutional Compliance

- [ ] CHK025 — Are all 11 constitution gates in `plan.md` still valid after the architectural change that merged the address endpoint into `PATCH /clients/{id}`? Specifically, gate #8 references "every endpoint" having a Testcontainers integration test — does tasks.md map integration tests to all 5 remaining endpoints? [Consistency, plan.md §Constitution Check]

- [ ] CHK026 — Is the "no New Relic SDK imports in `src/main`" constitution requirement (Article X / gate #10) enforceable at PR review time? Does any task, build rule, or checklist item create a verifiable enforcement mechanism — or is it only a post-hoc compliance check in T051? [Completeness, Constitution Article X]

- [ ] CHK027 — Is "permanently inactive" (FR-011) semantically consistent with the re-registration edge case? FR-011 says no re-activation is permitted — the re-registration scenario creates a NEW record, not re-activating the old one. Is this distinction stated explicitly so that an implementer does not conflate the two scenarios? [Consistency, FR-011, Edge Cases]

---

## Notes

- Mark items `[x]` as resolved; add inline findings for any `[ ]` that surface gaps
- **High-priority items before coding**: CHK006 (400 vs 422 for co-dependency), CHK008 (addressSearched naming), CHK011 (CPF anonymization value), CHK019 (ROLE_SYSTEM on inactive profile)
- Items marked `[Gap]` require either a spec update or an explicit out-of-scope decision before implementation
- After resolving gaps, re-run `/speckit.analyze` to confirm coverage before starting `/speckit.implement`
