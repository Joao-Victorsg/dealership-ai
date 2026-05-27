# Tasks: Lambda Infrastructure LocalStack

**Input**: Design documents from `/specs/002-lambda-infra-localstack/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/infra-runtime-contracts.md`, `quickstart.md`

**Tests**: Included because spec/contracts require mandatory contract, smoke, parity, observability, redaction, and constitutional release gates.

**Organization**: Tasks are grouped by user story so each story remains independently implementable and testable.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize Terraform + LocalStack workspace and gate script skeletons.

- [X] T001 Create Terraform module scaffolding files in `infra/localstack/versions.tf`, `infra/localstack/providers.tf`, `infra/localstack/main.tf`, `infra/localstack/variables.tf`, and `infra/localstack/outputs.tf`
- [X] T002 Create sample LocalStack inputs in `infra/localstack/terraform.tfvars.example`
- [X] T003 [P] Create LocalStack service preflight checker in `scripts/localstack/preflight.sh`
- [X] T004 [P] Create deploy orchestrator audit helper for existing `../devutils/deploy-all.sh` in `scripts/localstack/check-deploy-orchestrator.sh`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared contracts and non-negotiable gates before story work.

**⚠️ CRITICAL**: No user story implementation starts before this phase is complete.

- [X] T005 Implement Terraform-only executor guard (fail on OpenTofu/alternate IaC invocation) in `scripts/localstack/deploy.sh`
- [X] T006 [P] Configure Terraform CLI LocalStack providers and endpoint wiring in `infra/localstack/providers.tf`
- [X] T007 [P] Define deterministic naming/tagging/environment locals in `infra/localstack/locals.tf`
- [X] T008 [P] Define required runtime config/secret validation rules in `infra/localstack/variables.tf`
- [X] T009 Implement artifact contract validator (bootstrap-only zip + linux/arm64 checks) in `scripts/localstack/validate-artifact-contract.sh`
- [X] T010 Implement ordered Terraform workflow (`fmt`/`validate`/`plan`/`apply`) in `scripts/localstack/deploy.sh`
- [X] T011 Implement FR-010 inventory audit script that scans 100% of managed resources for naming/tagging conformance and fails on a single violation in `scripts/localstack/validate-inventory-conformance.sh`
- [X] T012 Record initial orchestrator audit evidence (`executed` or `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>`) in `specs/002-lambda-infra-localstack/evidence/orchestrator-audit.md`

**Checkpoint**: Terraform-only flow, artifact gate, FR-010 inventory audit gate, and orchestrator audit gate are enforced.

---

## Phase 3: User Story 1 - Provision local-first infrastructure baseline (Priority: P1) 🎯 MVP

**Goal**: Provision deterministic baseline resources in LocalStack with idempotent re-runs.

**Independent Test**: In a clean LocalStack run, provision once and re-run apply; verify one source queue, one DLQ, redrive linkage, log group retention, IAM profile, and required secret/config resources are stable.

### Tests for User Story 1

- [X] T013 [P] [US1] Implement queue/DLQ/redrive contract assertions in `tests/contract/us1_queue_topology_contract_test.sh`
- [X] T014 [P] [US1] Implement clean-environment provisioning/idempotency integration test in `tests/integration/us1_localstack_provisioning_test.sh`
- [X] T015 [P] [US1] Implement FR-010 inventory audit contract test covering naming pattern + required tag keys/enums with fail-on-single-violation behavior in `tests/contract/us1_inventory_conformance_contract_test.sh`

### Implementation for User Story 1

- [X] T016 [P] [US1] Implement log group and retention resources in `infra/localstack/logs.tf`
- [X] T017 [P] [US1] Implement source queue, DLQ, and redrive policy resources in `infra/localstack/sqs.tf`
- [X] T018 [P] [US1] Implement required configuration/secret resources in `infra/localstack/secrets.tf`
- [X] T019 [US1] Implement least-privilege execution role and policy scope in `infra/localstack/iam.tf`
- [X] T020 [US1] Wire baseline resources and outputs in `infra/localstack/main.tf` and `infra/localstack/outputs.tf`
- [X] T021 [US1] Integrate FR-010 inventory audit gate execution into `scripts/localstack/deploy.sh` so apply validation fails immediately on first naming/tagging violation
- [X] T022 [US1] Record baseline provisioning, rerun stability, and 100%-coverage inventory audit evidence in `specs/002-lambda-infra-localstack/evidence/us1-baseline-validation.md`

**Checkpoint**: US1 baseline is reproducible and independently testable.

---

## Phase 4: User Story 2 - Deploy Go Lambda artifact with runtime-compatible configuration (Priority: P1)

**Goal**: Enforce runtime-compatible artifact deployment and verify success-path invocation SLO.

**Independent Test**: Build `lambda.zip`, pass artifact gate, deploy Lambda (`provided.al2023`, `arm64`, bootstrap), enable mapping, publish 50 valid messages, and confirm SC-004 invocation threshold.

### Tests for User Story 2

- [X] T023 [P] [US2] Implement artifact/runtime contract test in `tests/contract/us2_artifact_runtime_contract_test.sh`
- [X] T024 [P] [US2] Implement success-path event flow integration test in `tests/integration/us2_event_flow_success_test.sh`
- [X] T025 [P] [US2] Implement constitutional log-field contract test for `sale_id`, `car_id`, `outcome`, `duration`, and conditional `downstream_http_status` in `tests/contract/us2_structured_log_fields_contract_test.sh`
- [X] T026 [P] [US2] Implement OQ-002 dependency-order contract test that fails if event source mapping is enabled before queues, config/secret bindings, and Lambda deployment complete in `tests/contract/us2_dependency_order_contract_test.sh`

### Implementation for User Story 2

- [X] T027 [P] [US2] Enforce Linux/arm64 bootstrap packaging assertions in `build/package.sh`
- [X] T028 [US2] Implement Lambda runtime profile/env/layer configuration in `infra/localstack/lambda.tf`
- [X] T029 [US2] Implement enabled queue-to-function mapping in `infra/localstack/event-source-mapping.tf`
- [X] T030 [US2] Implement SC-004 smoke runner (50 messages, 300s window, >=48 within 60s) in `scripts/localstack/smoke-success.sh`
- [X] T031 [US2] Implement observability gate assertions for per-message `sale_id`, `car_id`, `outcome`, `duration`, plus `downstream_http_status` when attempted in `scripts/localstack/validate-observability-contract.sh`
- [X] T032 [US2] Implement OQ-002 order-verification script that proves exact sequence (queues+DLQ -> required secret/config bindings -> Lambda deployed -> mapping enabled) and hard-fails on out-of-order mapping enablement in `scripts/localstack/verify-dependency-order.sh`
- [X] T033 [US2] Record runtime deployment, OQ-002 sequence evidence, smoke, and observability results in `specs/002-lambda-infra-localstack/evidence/us2-runtime-deploy-validation.md`

**Checkpoint**: US2 guarantees runtime-compatible deployment and constitutional observability field coverage.

---

## Phase 5: User Story 3 - Operate with safe defaults, observability, and environment parity rules (Priority: P2)

**Goal**: Enforce failure-routing, redaction zero-leak policy, parity drift blocking, and orchestrator audit gates.

**Independent Test**: Trigger controlled failure to DLQ, run observability redaction checks on the same smoke dataset, run parity checks, and verify release flow fails on any gate violation.

### Tests for User Story 3

- [X] T034 [P] [US3] Implement failure-routing integration test to DLQ in `tests/integration/us3_failure_to_dlq_test.sh`
- [X] T035 [P] [US3] Implement parity/security-intent contract test in `tests/contract/us3_parity_contract_test.sh`
- [X] T036 [P] [US3] Implement redaction contract test with zero-violation criterion for secrets/tokens/PII sentinels in `tests/contract/us3_redaction_contract_test.sh`

### Implementation for User Story 3

- [X] T037 [US3] Implement controlled-failure smoke runner and DLQ verification in `scripts/localstack/smoke-dlq.sh`
- [X] T038 [US3] Implement redaction scanner gate with hard fail on `redaction_violation_count > 0` in `scripts/localstack/validate-observability-contract.sh`
- [X] T039 [US3] Implement parity drift checker for topology/runtime/IAM/config contracts in `scripts/localstack/parity-check.sh`
- [X] T040 [US3] Integrate orchestrator audit gate + inventory audit + order verification + smoke + observability + redaction + parity into release gate runner in `scripts/localstack/validate-release-gates.sh`
- [X] T041 [US3] Record DLQ, parity, observability, redaction, inventory, and orchestrator audit outcomes in `specs/002-lambda-infra-localstack/evidence/us3-operations-parity-validation.md`

**Checkpoint**: US3 blocks approval on routing, parity, redaction, or orchestrator gate failures.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final traceability, docs consistency, and constitutional/OQ-001 release evidence.

- [X] T042 [P] Update Terraform-only linear runbook in `specs/002-lambda-infra-localstack/quickstart.md` so OQ-001 completeness is explicit and fail-fast: include prerequisites section, exact command sequence, required success snippets (`Terraform has been successfully initialized!`, `Success! The configuration is valid.`, `Apply complete! Resources:`, `SMOKE_TEST_RESULT: PASS`) mapped to steps, one LocalStack `connection refused` failure example, and recovery path
- [X] T043 Create FR/OQ/SC traceability matrix (including FR-010, OQ-002, and SC-007/SC-008/SC-009 evidence links) in `specs/002-lambda-infra-localstack/evidence/validation-traceability.md`
- [X] T044 Create FR-014 convention-conformance matrix with exactly 3/3 categories (module isolation, dependency order, LocalStack endpoint scoping), each with monorepo reference and conformant status, in `specs/002-lambda-infra-localstack/evidence/convention-conformance-matrix.md`
- [ ] T045 Execute full LocalStack validation suite and archive outputs in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md`, failing validation if runbook evidence is missing any OQ-001 element (prerequisites, exact command sequence, required success snippets, one `connection refused` failure example, recovery path)
- [X] T046 Verify final deploy orchestrator audit status (`executed` or standardized skip record) in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md`
- [X] T047 [P] Normalize Terraform CLI evidence snippets (`terraform fmt/validate/plan/apply`) plus explicit inventory/order-verification outputs in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md`
- [X] T048 Execute and archive constitutional checks (`go test`, `go test -race`, `golangci-lint`, `govulncheck`) in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md`
- [X] T049 Implement OQ-001 quickstart completeness validator that hard-fails when `specs/002-lambda-infra-localstack/quickstart.md` omits prerequisites, exact command sequence, any required success snippet, one failure example, or recovery path in `scripts/localstack/validate-runbook-completeness.sh`
- [X] T050 Execute OQ-001 runbook completeness validator and record pass/fail output in `specs/002-lambda-infra-localstack/evidence/final-validation-log.md`

**OQ-001 runbook completeness reminder**: Recovery path must explicitly require LocalStack start/restore, re-running validation then apply in order, confirming final smoke evidence contains `SMOKE_TEST_RESULT: PASS`, and documenting blocker evidence + escalation owner after two failed recovery attempts.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Starts immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2.
- **Phase 4 (US2)**: Depends on US1 baseline resources and Phase 2 gates.
- **Phase 5 (US3)**: Depends on US2 deployed runtime/smoke dataset.
- **Phase 6 (Polish)**: Depends on completion of US1, US2, and US3.

### User Story Dependency Graph

- **US1 (P1) -> US2 (P1) -> US3 (P2)**

### Parallel Opportunities

- **Setup**: T003 and T004.
- **Foundational**: T006, T007, T008 can run in parallel after T005; T011 depends on T007.
- **US1**: T013/T014/T015 and T016/T017/T018 can run in parallel, then T019-T022.
- **US2**: T023/T024/T025/T026 and T027 can run in parallel, then T028-T033.
- **US3**: T034/T035/T036 can run in parallel, then T037-T041.
- **Polish**: T042 and T047 can run in parallel; T049 follows runbook updates and T050 runs after T049.

---

## Parallel Example: User Story 2

```bash
Task: "T023 [US2] tests/contract/us2_artifact_runtime_contract_test.sh"
Task: "T024 [US2] tests/integration/us2_event_flow_success_test.sh"
Task: "T025 [US2] tests/contract/us2_structured_log_fields_contract_test.sh"
Task: "T026 [US2] tests/contract/us2_dependency_order_contract_test.sh"
Task: "T027 [US2] build/package.sh"
```

## Parallel Example: User Story 3

```bash
Task: "T034 [US3] tests/integration/us3_failure_to_dlq_test.sh"
Task: "T035 [US3] tests/contract/us3_parity_contract_test.sh"
Task: "T036 [US3] tests/contract/us3_redaction_contract_test.sh"
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1) baseline + idempotency validation.
3. Validate US1 independently before moving forward.

### Incremental Delivery

1. Add US2 runtime-compatible deploy + SC-004 + constitutional field checks.
2. Add US3 DLQ + redaction zero-violation + parity + orchestrator audit enforcement.
3. Complete Phase 6 for evidence consolidation and release gates.

