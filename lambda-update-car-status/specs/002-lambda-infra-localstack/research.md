# Phase 0 Research — Lambda Infrastructure LocalStack

## Decision 1: IaC approach for LocalStack-first provisioning
- **Decision**: Use an isolated IaC module under `infra/localstack` executed with **Terraform CLI only** against LocalStack endpoints.
- **Rationale**: Enforces reproducible, idempotent provisioning and keeps parity-focused topology/security definitions versioned.
- **Alternatives considered**:
  - Manual `awslocal` scripts only: rejected due to lower drift control and weaker declarative idempotency.
  - OpenTofu or mixed Terraform/OpenTofu execution: rejected due to FR-015 scope and release-evidence ambiguity.
  - Mixing local and production definitions in one unscoped script: rejected due to maintainability and parity-audit risk.

## Decision 2: Resource dependency order and activation strategy
- **Decision**: Provision in this order: log group -> DLQ -> source queue with redrive policy -> secret/config bindings -> IAM role/policies -> Lambda function -> event source mapping enablement.
- **Rationale**: Prevents partial runtime activation before prerequisites exist and satisfies OQ-002 dependency ordering requirement.
- **Alternatives considered**:
  - Create mapping before role/config readiness: rejected due to invocation failures and noisy retries.

## Decision 3: Runtime package contract gate
- **Decision**: Keep `build/package.sh` as required pre-deploy gate and enforce contract checks: `GOOS=linux`, `GOARCH=arm64`, output executable named `bootstrap`, zip contains only `bootstrap`.
- **Rationale**: Directly satisfies FR-005/FR-006 and blocks invalid artifacts before function update.
- **Alternatives considered**:
  - Trusting CI build output without structural verification: rejected (runtime mismatch risk).

## Decision 4: Queue + DLQ + mapping behavior contract
- **Decision**: Define one source queue and one DLQ with explicit redrive policy (`maxReceiveCount` configurable), and a single enabled event source mapping tied to source queue ARN.
- **Rationale**: Meets FR-003/FR-004 and enables deterministic retry/failure routing tests.
- **Alternatives considered**:
  - Multiple ingestion queues in this feature: rejected as out-of-scope complexity.

## Decision 5: IAM least-privilege scope
- **Decision**: Execution role includes only CloudWatch Logs write actions, SQS consume lifecycle actions on source queue, and Secrets Manager read action for required secret identifiers.
- **Rationale**: Aligns with FR-007 and constitution security principles.
- **Alternatives considered**:
  - Wildcard `*` permissions for speed: rejected due to security drift and parity risk.

## Decision 6: Configuration and secret wiring policy
- **Decision**: Infrastructure must require explicit values/identifiers for mandatory runtime env vars and secret IDs; deployment fails when required values are absent/empty.
- **Rationale**: Supports Article XI fail-fast startup and FR-009 contract enforcement.
- **Alternatives considered**:
  - Optional defaults for critical auth/timeout fields: rejected as unsafe behavior.

## Decision 7: New Relic observability implementation boundary
- **Decision**: Implement observability by attaching New Relic Lambda Extension layer ARN(s) and required NR environment variables in infrastructure; do not require New Relic business-code SDK instrumentation.
- **Rationale**: Satisfies constitution Article VIII and user requirement for layer-only approach.
- **Alternatives considered**:
  - In-code New Relic SDK instrumentation as default: rejected (vendor coupling not required).

## Decision 8: LocalStack validation and parity gates
- **Decision**: Post-deploy smoke validation must include: successful message invocation evidence, controlled-failure path evidence reaching DLQ, and parity check report covering topology, IAM scope, runtime profile, and required config contract.
- **Rationale**: Meets FR-012/FR-013 and OQ-004/OQ-006 with explicit approval gates.
- **Alternatives considered**:
  - Success-only smoke test: rejected as insufficient operational confidence.

## Decision 9: Deploy orchestrator audit gate evidence
- **Decision**: Treat `../devutils/deploy-all.sh` as a mandatory audit gate with exactly two accepted outcomes recorded in evidence: (1) explicit execution of `../lambda-update-car-status/infra/localstack`; or (2) standardized skip record `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>`.
- **Rationale**: Satisfies FR-016 and SC-007 with deterministic, reviewable orchestration traceability.
- **Alternatives considered**:
  - Informal reviewer note without log proof: rejected due to unverifiable gate execution.

## Decision 10: Constitutional release-gate coverage in planning
- **Decision**: Include Article X/XII release gates as mandatory planning checks and evidence: automated tests, race detection, lint, and vulnerability scan, in addition to Terraform and smoke/parity gates.
- **Rationale**: Ensures constitutional compliance is explicit and auditable in infra delivery workflows.
- **Alternatives considered**:
  - Running only Terraform validation + smoke checks: rejected because it omits constitutional release requirements.

## Decision 11: SC-004 smoke methodology references
- **Decision**: Standardize SC-004 measurement through scripted methodology references (`scripts/localstack/smoke-success.sh` + evidence template) that publish exactly 50 valid messages in <=5 minutes and calculate publish-to-first-invocation latency pass/fail (`>=48/50 within 60s`).
- **Rationale**: Prevents inconsistent manual interpretation and makes SC-004 measurable and repeatable.
- **Alternatives considered**:
  - Ad hoc manual smoke checks: rejected due to non-repeatable timing measurements.

## Decision 12: Observability contract fields + redaction enforcement
- **Decision**: The observability gate must validate per-message structured logs for constitutional fields `sale_id`, `car_id`, `outcome`, and `duration`, and require `downstream_http_status` whenever downstream HTTP was attempted; the same smoke dataset must pass redaction scan with zero unredacted secret/token/PII sentinel values.
- **Rationale**: Aligns FR-008/FR-017 with Constitution Article VIII and removes ambiguity from earlier generic structured-log checks.
- **Alternatives considered**:
  - Generic log-shape validation without constitutional fields: rejected due to incomplete compliance evidence.
  - Redaction checks on a separate dataset: rejected due to traceability gaps between field coverage and leak detection.

## Clarification Resolution Status
All technical clarifications are resolved. No `NEEDS CLARIFICATION` markers remain.

