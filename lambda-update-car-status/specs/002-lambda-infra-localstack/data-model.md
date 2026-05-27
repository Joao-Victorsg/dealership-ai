# Data Model — Lambda Infrastructure LocalStack

## 1) LambdaRuntimeArtifact
- **Purpose**: Deployment package contract consumed by Lambda update operation.
- **Fields**:
  - `file_name` (string, fixed: `lambda.zip`)
  - `entrypoint` (string, fixed: `bootstrap`)
  - `target_os` (string, fixed: `linux`)
  - `target_arch` (string, fixed: `arm64`)
  - `runtime` (string, fixed: `provided.al2023`)
  - `sha256` (string, required)
- **Validation rules**:
  - Zip contains exactly one file named `bootstrap`.
  - Artifact must be built from `GOOS=linux GOARCH=arm64 CGO_ENABLED=0`.
- **State transitions**:
  - `not_built` -> `built` -> `validated` -> `deployed`.

## 2) QueueTopology
- **Purpose**: Event ingestion and terminal failure routing definition.
- **Fields**:
  - `source_queue_name` (string, required)
  - `dlq_name` (string, required)
  - `redrive_max_receive_count` (integer, required, >0)
  - `visibility_timeout_seconds` (integer, required, >0)
  - `message_retention_seconds` (integer, required)
- **Validation rules**:
  - Source queue must reference DLQ ARN in redrive policy.
  - Exactly one source queue and one DLQ for this feature scope.
- **State transitions**:
  - `undefined` -> `provisioned` -> `validated`.

## 3) EventSourceMappingProfile
- **Purpose**: Binding between source queue and Lambda function.
- **Fields**:
  - `function_name` (string, required)
  - `event_source_arn` (string, required; must equal source queue ARN)
  - `enabled` (boolean, required)
  - `batch_size` (integer, required, >0)
- **Validation rules**:
  - Mapping is created only after queue and function exist.
  - Mapping must be enabled for smoke test runs.
- **State transitions**:
  - `absent` -> `created_disabled` (optional transient) -> `enabled` -> `verified`.

## 4) LambdaExecutionAccessProfile
- **Purpose**: Least-privilege IAM policy set for runtime execution.
- **Fields**:
  - `role_name` (string, required)
  - `log_actions` (set, required)
  - `sqs_actions` (set, required)
  - `secret_actions` (set, required)
  - `resource_scope` (list of ARNs, required)
- **Validation rules**:
  - No wildcard `*` on actions/resources unless explicitly justified and documented.
  - Policy scope must include source queue and required secret resources only.
- **State transitions**:
  - `draft` -> `attached` -> `validated`.

## 5) RuntimeConfigAndSecretsBinding
- **Purpose**: Required runtime input contract for Lambda startup.
- **Fields**:
  - `environment_variables` (map, required)
  - `required_keys` (list, required)
  - `secret_identifiers` (list, required)
- **Validation rules**:
  - Required keys must be non-empty before deployment completes.
  - Secret identifiers must resolve in target environment.
- **State transitions**:
  - `unbound` -> `bound` -> `startup_validated`.

## 6) ObservabilityLayerBinding
- **Purpose**: Vendor-agnostic observability enablement through Lambda layers.
- **Fields**:
  - `new_relic_extension_layer_arn` (string, required per environment)
  - `new_relic_env` (map, required keys vary by account policy)
  - `business_sdk_dependency` (boolean, fixed: `false` by default)
- **Validation rules**:
  - Extension layer must be attached at deploy time.
  - Plan must not require business-code SDK dependency unless separately approved.
- **State transitions**:
  - `not_configured` -> `attached` -> `telemetry_verified`.

## 7) PostDeployValidationReport
- **Purpose**: Evidence artifact for local-first gates and parity checks.
- **Fields**:
  - `iac_executor` (enum: `terraform_cli`, required)
  - `provisioning_result` (enum: `pass|fail`)
  - `artifact_contract_result` (enum: `pass|fail`)
  - `smoke_success_result` (enum: `pass|fail`)
  - `sc004_messages_published` (integer, required, fixed: `50`)
  - `sc004_window_seconds` (integer, required, fixed: `300`)
  - `sc004_messages_within_60s` (integer, required, pass threshold: `>=48`)
  - `observability_required_fields` (list, required, fixed: `sale_id`, `car_id`, `outcome`, `duration`)
  - `observability_messages_with_required_fields` (integer, required, fixed pass value: `50`)
  - `observability_downstream_attempted_count` (integer, required, >=0)
  - `observability_downstream_status_present_count` (integer, required, must equal attempted count)
  - `redaction_scan_result` (enum: `pass|fail`)
  - `redaction_violation_count` (integer, required, fixed pass value: `0`)
  - `smoke_failure_to_dlq_result` (enum: `pass|fail`)
  - `parity_check_result` (enum: `pass|fail`)
  - `orchestrator_audit_result` (enum: `executed|skip_recorded|fail`)
  - `orchestrator_skip_record` (string, optional, pattern: `SKIP_LAMBDA_INFRA_LOCALSTACK:<reason>`)
  - `release_tests_result` (enum: `pass|fail`)
  - `release_race_result` (enum: `pass|fail`)
  - `release_lint_result` (enum: `pass|fail`)
  - `release_vuln_scan_result` (enum: `pass|fail`)
  - `generated_at` (timestamp)
- **Validation rules**:
  - Approval requires `iac_executor = terraform_cli`.
  - Approval requires all result fields = `pass` and `sc004_messages_within_60s >= 48`.
  - Approval requires `observability_messages_with_required_fields = 50`.
  - Approval requires `observability_downstream_status_present_count = observability_downstream_attempted_count`.
  - Approval requires `redaction_violation_count = 0`.
  - `orchestrator_audit_result` must be `executed` or `skip_recorded`; if `skip_recorded`, `orchestrator_skip_record` is mandatory.
  - Missing evidence blocks rollout progression.
- **State transitions**:
  - `pending` -> `collected` -> `approved` or `rejected`.

## Relationships
- `LambdaRuntimeArtifact` is deployed to Lambda configured by `RuntimeConfigAndSecretsBinding` and `ObservabilityLayerBinding`.
- `QueueTopology` feeds `EventSourceMappingProfile`.
- `LambdaExecutionAccessProfile` grants permissions required by queue consumption and secret retrieval.
- `PostDeployValidationReport` references all deployed entities as verification evidence.

