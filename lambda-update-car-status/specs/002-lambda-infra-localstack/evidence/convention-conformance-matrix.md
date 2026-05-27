# FR-014 Convention Conformance Matrix

| Category | Monorepo Reference | Conformant | Notes |
|---|---|---|---|
| Module isolation | `infra/localstack/*` + `scripts/localstack/*` | yes | Infra-only slice isolated from business code paths. |
| Dependency order | `infra/localstack/event-source-mapping.tf` + `scripts/localstack/verify-dependency-order.sh` | yes | Explicit depends_on and gate enforcement. |
| LocalStack endpoint scoping | `infra/localstack/providers.tf` | yes | AWS provider endpoints scoped to LocalStack URL. |
