# Feature Specification: Lambda Infrastructure LocalStack

**Feature Branch**: `002-lambda-infra-localstack`  
**Created**: 2026-05-22  
**Status**: Draft  
**Input**: User description: "Agora quero que faça a especificação da infraestrutura dessa lambda. Lembre-se que estamos utilizando localstack e a linguagem é Go. Temos alguns exemplos no diretório ../../dealership."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Provision local-first infrastructure baseline (Priority: P1)

As a platform engineer, I need a reproducible infrastructure baseline for this Lambda in a LocalStack-first environment so I can validate queue flow, runtime wiring, and permissions before any cloud deployment.

**Why this priority**: Without a stable baseline, all downstream validation is blocked and delivery risk increases.

**Independent Test**: Provision the stack in a clean LocalStack environment and verify all required resources are created and linked with expected names, policies, and lifecycle settings.

**Acceptance Scenarios**:

1. **Given** an empty LocalStack environment, **When** infrastructure provisioning is executed, **Then** the Lambda function, source queue, dead-letter queue, log group, required IAM permissions, and secret/config resources are created successfully.
2. **Given** the infrastructure is already provisioned, **When** provisioning is executed again, **Then** the result remains stable without duplicate or conflicting resources.

---

### User Story 2 - Deploy Go Lambda artifact with runtime-compatible configuration (Priority: P1)

As a release engineer, I need deployment rules that guarantee the Lambda artifact is compatible with the target runtime so the function can start and process queue events consistently.

**Why this priority**: Runtime mismatch or invalid packaging causes startup failure and blocks all message processing.

**Independent Test**: Build and package the Lambda artifact, deploy it to the provisioned environment, and confirm startup plus successful processing of a valid queue message.

**Acceptance Scenarios**:

1. **Given** the deployment pipeline produces a Linux arm64 bootstrap zip artifact, **When** the function is deployed, **Then** the runtime accepts the artifact and the function initializes successfully.
2. **Given** a valid sale message is placed on the source queue after deployment, **When** event source mapping invokes the function, **Then** processing runs and the message exits the source queue according to success semantics.

---

### User Story 3 - Operate with safe defaults, observability, and environment parity rules (Priority: P2)

As an SRE, I need operational guardrails for logging, retry routing, and environment parity so local validation is trustworthy and production rollout risk is reduced.

**Why this priority**: Operational blind spots and parity gaps cause incident response delays and environment-specific failures.

**Independent Test**: Simulate success and failure paths in LocalStack, confirm logs and retry routing behavior, and verify mandatory parity checks before production-targeted deployment.

**Acceptance Scenarios**:

1. **Given** a transient processing failure occurs, **When** the message is retried based on queue policy, **Then** retry behavior follows configured redrive rules and unresolved messages are routed to dead-letter handling.
2. **Given** infrastructure changes are proposed, **When** validation gates run, **Then** local-first checks, parity checks, and observability gate checks must pass before approval.

---

### Edge Cases

- LocalStack endpoints are reachable but one required service (for example queue or secrets) is not available.
- The deployment artifact does not contain exactly one `bootstrap` executable.
- Runtime architecture differs from packaged artifact architecture.
- Event source mapping exists but points to the wrong queue or disabled state.
- Dead-letter queue exists but redrive linkage from the source queue is missing.
- Required runtime configuration keys are absent or empty at deployment time.
- A message completes without a downstream call attempt and no `downstream_http_status` is emitted.
- A smoke-test message is processed but no structured per-message log record is available for constitutional observability fields.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The infrastructure specification MUST remain scoped to platform resources and deployment flow for `lambda-update-car-status`; it MUST NOT redefine business logic behavior owned by application code.
- **FR-002**: The infrastructure MUST follow a LocalStack-first execution model where local provisioning and validation are mandatory before production-targeted rollout.
- **FR-003**: The infrastructure MUST define a single event ingestion topology composed of one source queue and one dead-letter queue with explicit redrive linkage.
- **FR-004**: The infrastructure MUST define a Lambda function resource wired to the source queue through event source mapping.
- **FR-005**: The deployment artifact contract MUST require a Linux arm64 `bootstrap` binary packaged as a zip containing only `bootstrap`.
- **FR-006**: The Lambda runtime target MUST be specified as custom runtime on Amazon Linux 2023 (`provided.al2023`) with arm64 architecture.
- **FR-007**: The infrastructure MUST require least-privilege execution permissions limited to logging, queue consumption lifecycle, and retrieval of required secret/config values.
- **FR-008**: The infrastructure MUST define log retention of at least 14 days and enforce an observability gate that passes only when 100% of smoke-test messages have per-message structured logs containing constitutional fields `sale_id`, `car_id`, `outcome`, and `duration`, plus `downstream_http_status` whenever a downstream call is attempted.
- **FR-009**: The infrastructure MUST require explicit configuration inputs for all mandatory runtime settings and fail deployment validation when required values are missing. Canonical required runtime config/secret key inventory (all mandatory, non-empty) is: `CAR_API_BASE_URL`, `CAR_API_TIMEOUT_MS`, `KEYCLOAK_TOKEN_URL`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_SECRET_ID`, `KEYCLOAK_REFRESH_SKEW_SECONDS`, `BREAKER_MAX_REQUESTS`, `BREAKER_INTERVAL_MS`, `BREAKER_TIMEOUT_MS`, `BREAKER_READY_TO_TRIP_FAILURES`, and `LOG_LEVEL`.
- **FR-010**: The infrastructure MUST enforce a deterministic naming and tagging schema for every managed resource. Required naming pattern: `<env>-lambda-update-car-status-<resource-type>-<instance>` where `<env>` is one of `local|dev|stg|prod`, `<resource-type>` is one of `fn|queue|dlq|log|role|policy|secret|mapping`, and `<instance>` is a zero-padded 2-digit ordinal (`01`, `02`, ...). Required tags/labels on 100% of resources: `service=lambda-update-car-status`, `environment=<env>`, `managed_by=terraform`, `owner=<team-id>`, `cost_center=<cost-code>`, `data_classification=<public|internal|restricted>`. Verification acceptance: for each provisioning run, execute a full inventory audit of all managed resources and pass only when 100% of resources satisfy both the naming pattern and all required tag keys/enums; any single violation MUST fail validation.
- **FR-011**: The infrastructure MUST provide a repeatable provisioning workflow that is idempotent across re-runs in the same environment.
- **FR-012**: The infrastructure MUST include automated smoke validation after deployment, including queue-to-function invocation confirmation.
- **FR-013**: The infrastructure MUST keep local and production-targeted definitions aligned in topology and security intent, allowing only environment-specific endpoint and account differences.
- **FR-014**: The infrastructure specification MUST reuse established monorepo infrastructure conventions (module isolation, dependency order, LocalStack endpoint scoping) adapted for this Lambda’s narrower scope. Verification acceptance: each validation cycle MUST publish a convention-conformance matrix at `specs/002-lambda-infra-localstack/evidence/convention-conformance-matrix.md` with exactly these three convention categories and a monorepo reference for each; validation passes only when conformance is confirmed for 3/3 categories and fails if any category is missing, unreferenced, or marked non-conformant.
- **FR-015**: Infrastructure-as-code execution for this feature MUST use Terraform CLI only; OpenTofu or any alternative IaC executor is out of scope for execution, validation, and release evidence.
- **FR-016**: The deploy orchestrator audit gate MUST require `../devutils/deploy-all.sh` to either execute the infrastructure step at `../lambda-update-car-status/infra/localstack` or emit a standardized explicit skip record `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>` when that path is absent.
- **FR-017**: The observability gate MUST fail if any smoke-test log line contains unredacted secret values, credential material, access tokens, or PII; only redacted representations are allowed in validation evidence.

### Operational & Quality Requirements *(mandatory for backend/services)*

- **OQ-001**: Provisioning and deployment documentation MUST be executable as a linear runbook in `specs/002-lambda-infra-localstack/quickstart.md`. This runbook artifact MUST contain: (a) complete prerequisites, (b) exact command sequence, (c) exact success output snippets per step, (d) one explicit failure example, and (e) expected recovery path. At minimum, the documented success outputs MUST include these exact snippets mapped to their steps: `Terraform has been successfully initialized!`, `Success! The configuration is valid.`, `Apply complete! Resources:`, and `SMOKE_TEST_RESULT: PASS`. The required failure example is LocalStack endpoint unavailable, with an error snippet containing `connection refused` against `localhost:4566`. The recovery path MUST require: start/restore LocalStack service, re-run validation and apply steps in order, and confirm final smoke evidence contains `SMOKE_TEST_RESULT: PASS`; if recovery fails twice, the runbook MUST require recording blocker evidence and escalation owner.
- **OQ-002**: Resource dependency order verification MUST be explicit and evidenced in every deployment run. The required order is: (1) source queue and dead-letter queue ready, (2) required secret/config bindings ready, (3) Lambda function deployment complete, (4) event source mapping enabled. Verification expectation: pass only when run evidence shows this exact sequence with no out-of-order activation; any run that enables mapping before steps 1-3 complete MUST fail quality gates.
- **OQ-003**: Post-deploy verification includes function readiness, event source mapping state, and dead-letter routing readiness.
- **OQ-004**: LocalStack validation includes both successful processing and controlled failure-routing scenarios.
- **OQ-005**: Deployment gates require artifact contract validation (runtime, architecture, zip structure) before function update.
- **OQ-006**: Environment parity checks identify and block drift in critical topology, permission scope, and required configuration contracts.
- **OQ-007**: The specification MUST include a dedicated `Scope Boundaries` section with explicit `In Scope` and `Out of Scope` bullet lists; each `In Scope` item maps to at least one functional requirement, and `Out of Scope` explicitly includes application business rules and domain workflow changes.
- **OQ-008**: Observability validation evidence includes per-message field-availability results for constitutional fields and a redaction scan result over the same smoke-test dataset.

### Key Entities *(include if feature involves data)*

- **Lambda Deployment Artifact**: Versioned package contract containing the executable entrypoint (`bootstrap`) and runtime compatibility metadata.
- **Queue Topology**: Source queue and dead-letter queue relationship governing delivery, retry, and terminal failure routing.
- **Function Runtime Profile**: Declared runtime/architecture/execution settings that determine whether the Go artifact can initialize and run.
- **Infrastructure Environment Definition**: Set of resource names, tags, endpoints, and required configuration/secret bindings for a target environment.
- **Post-Deploy Validation Report**: Evidence set confirming provisioning success, deployment success, and queue-triggered invocation behavior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of mandatory resources in the defined infrastructure scope are created successfully in a clean LocalStack environment on first provisioning run.
- **SC-002**: 100% of immediate re-runs of provisioning in the same LocalStack environment complete without creating conflicting duplicate resources.
- **SC-003**: 100% of deployment attempts with runtime-incompatible or malformed artifacts are blocked before function activation.
- **SC-004**: For each validation run, publish exactly 50 valid smoke-test messages within a 5-minute window; measure per-message latency from message publish timestamp to first recorded function invocation timestamp; pass when at least 48 of 50 messages (96%) invoke within 60 seconds, fail otherwise.
- **SC-005**: 100% of controlled failure smoke tests route unresolved messages according to configured dead-letter behavior.
- **SC-006**: 100% of approved infrastructure changes include recorded local-first validation evidence and parity check results.
- **SC-007**: In 100% of orchestrated deployment runs using `../devutils/deploy-all.sh`, logs contain either evidence of execution of `../lambda-update-car-status/infra/localstack` or a standardized skip record matching `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>`.
- **SC-008**: For each validation run, evaluate exactly 50 smoke-test messages; pass only when 50/50 messages have at least one associated structured log record containing `sale_id`, `car_id`, `outcome`, and `duration`, and every message with a downstream call attempt also has `downstream_http_status` present.
- **SC-009**: For each validation run, redaction checks over the same smoke-test log dataset report 0 unredacted occurrences of seeded secret/token/PII sentinel values.

## Scope Boundaries

### In Scope

- Terraform-only infrastructure provisioning and validation workflow for `lambda-update-car-status`.
- LocalStack resource baseline for Lambda, source queue, dead-letter queue, logging, permissions, and required configuration/secret bindings.
- Deployment orchestration gate behavior in `../devutils/deploy-all.sh` for this feature's infrastructure step.
- Infrastructure smoke-test evidence for success path and failure-routing path.

### Out of Scope

- Any change to Lambda business logic, domain rules, or message transformation behavior.
- Redefinition of message contract semantics owned by application code.
- Migration to non-Terraform IaC execution tools.

## Assumptions

- Existing application code and message contract behavior from feature `001-car-status-updater` remain unchanged and are not re-specified here.
- LocalStack is the default developer/test execution environment and is available with queue, lambda, logs, and secrets capabilities enabled.
- The build process continues to produce the Go Lambda artifact using Linux/arm64 and `bootstrap` zip conventions already used in this repository.
- Monorepo infrastructure examples (such as modular resource separation and LocalStack endpoint scoping patterns used in neighboring projects) are used as reference style, then reduced to this Lambda’s minimal required footprint.
- Production cloud rollout details (account strategy, networking perimeter, centralized observability backends) are handled by platform standards outside this feature scope.
