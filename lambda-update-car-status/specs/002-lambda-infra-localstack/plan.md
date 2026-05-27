# Implementation Plan: Lambda Infrastructure LocalStack

**Branch**: `002-lambda-infra-localstack` | **Date**: 2026-05-22 | **Spec**: `specs/002-lambda-infra-localstack/spec.md`
**Input**: Feature specification from `specs/002-lambda-infra-localstack/spec.md`

## Summary

Deliver an infra-only, Terraform-only implementation for `lambda-update-car-status` with LocalStack-first provisioning, deterministic naming/tagging, runtime artifact contract enforcement, queue/Lambda wiring, and hard validation gates for inventory conformance (FR-010), dependency ordering (OQ-002), smoke, parity, orchestrator audit, observability field completeness, and redaction.

## Technical Context

**Language/Version**: Terraform (HCL) + Bash; Go tooling only for Lambda artifact packaging/validation  
**Primary Dependencies**: Terraform CLI, LocalStack, AWS CLI/`awslocal`, bash validation scripts (`validate-inventory-conformance.sh`, `verify-dependency-order.sh`, `validate-release-gates.sh`), `build/package.sh`  
**Storage**: N/A (infra-only scope; no datastore introduced)  
**Testing**: Shell contract/integration tests + Terraform validation + constitutional release checks (`go test`, `go test -race`, `golangci-lint`, `govulncheck`)  
**Target Platform**: LocalStack-backed AWS emulation locally with parity checks for production-targeted topology/security intent  
**Project Type**: Infrastructure module + validation scripts (infra-only, Terraform-only)  
**Performance Goals**: SC-004 (`>=48/50` invoke within 60s, 5-minute publish window) and SC-008 (`50/50` required structured fields)  
**Constraints**: Terraform CLI only, one source queue + one DLQ + one mapping, `provided.al2023` + `arm64`, zero redaction violations  
**Scale/Scope**: Single module for `lambda-update-car-status` with evidence artifacts under `specs/002-lambda-infra-localstack/evidence/`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Gate

- [x] Scope is infra-only and excludes business/domain behavior changes.
- [x] Terraform CLI is the only IaC executor in plan and evidence (FR-015).
- [x] LocalStack-first validation is mandatory before production-targeted rollout (FR-002, FR-013).
- [x] Runtime/artifact contracts enforce `bootstrap` zip + `provided.al2023` + `arm64` (FR-005, FR-006).
- [x] Least-privilege IAM and fail-fast required config/secret contract are represented (FR-007, FR-009; Articles IV/XI).
- [x] Observability/redaction gates are explicit (`sale_id`, `car_id`, `outcome`, `duration`, conditional `downstream_http_status`, zero unredacted leaks) (FR-008, FR-017, SC-008, SC-009; Article VIII).
- [x] Quality/release governance checks are mandatory evidence gates (Articles X/XII).

### Post-Design Re-Check

- [x] Research, data model, contracts, and quickstart preserve infra-only + Terraform-only boundaries.
- [x] Design artifacts keep observability field coverage and redaction as hard blockers.
- [x] No constitutional violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/002-lambda-infra-localstack/
├── plan.md
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── infra-runtime-contracts.md
├── evidence/
│   ├── orchestrator-audit.md
│   ├── us1-baseline-validation.md
│   ├── us2-runtime-deploy-validation.md
│   ├── us3-operations-parity-validation.md
│   ├── validation-traceability.md
│   ├── convention-conformance-matrix.md
│   └── final-validation-log.md
└── tasks.md
```

### Source Code (repository root)

```text
build/
└── package.sh

infra/
└── localstack/
    ├── versions.tf
    ├── providers.tf
    ├── locals.tf
    ├── variables.tf
    ├── terraform.tfvars.example
    ├── logs.tf
    ├── sqs.tf
    ├── secrets.tf
    ├── iam.tf
    ├── lambda.tf
    ├── event-source-mapping.tf
    ├── main.tf
    └── outputs.tf

scripts/
└── localstack/
    ├── preflight.sh
    ├── check-deploy-orchestrator.sh
    ├── validate-artifact-contract.sh
    ├── validate-inventory-conformance.sh
    ├── deploy.sh
    ├── smoke-success.sh
    ├── validate-observability-contract.sh
    ├── smoke-dlq.sh
    ├── verify-dependency-order.sh
    ├── parity-check.sh
    └── validate-release-gates.sh

tests/
├── contract/
│   ├── us1_queue_topology_contract_test.sh
│   ├── us1_inventory_conformance_contract_test.sh
│   ├── us2_artifact_runtime_contract_test.sh
│   ├── us2_structured_log_fields_contract_test.sh
│   ├── us2_dependency_order_contract_test.sh
│   ├── us3_parity_contract_test.sh
│   └── us3_redaction_contract_test.sh
└── integration/
    ├── us1_localstack_provisioning_test.sh
    ├── us2_event_flow_success_test.sh
    └── us3_failure_to_dlq_test.sh
```

**Structure Decision**: Keep a single infra feature slice centered on `infra/localstack` + `scripts/localstack`, with Terraform-only execution paths and explicit contract/integration test inventory including FR-010 naming/tagging conformance, OQ-002 dependency-order verification, and US3 redaction coverage.

## Phase 0 — Research Output

`research.md` resolves Terraform execution, dependency ordering, inventory conformance strategy, artifact/runtime gate details, observability/redaction strategy, parity gate policy, and orchestrator audit behavior with no remaining `NEEDS CLARIFICATION` markers.

## Phase 1 — Design Output

- `data-model.md`: defines artifact, topology, mapping, IAM, config/secrets, observability binding, and post-deploy validation entities.
- `contracts/infra-runtime-contracts.md`: defines Terraform-only execution and mandatory deployment/runtime/observability/parity/orchestrator/release contracts.
- `quickstart.md`: linear LocalStack-first runbook with artifact checks, deploy sequence, smoke/parity gates, and constitutional release commands.

## Complexity Tracking

No constitutional violations requiring justification.
