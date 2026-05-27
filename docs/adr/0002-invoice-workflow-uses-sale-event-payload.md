# Invoice workflow consumes complete Sale Event Payload

We decided that the `dealership-ai` invoice workflow will consume the `SaleEventPayload` published by `sales-api` directly, without enrichment lambdas for car/client lookups. The payload already includes `saleId`, `saleValue` (with checkout tax), `registeredAt`, `clientSnapshot`, and `carSnapshot`, so keeping only `start-invoice-workflow -> invoice-processor -> send-email` reduces orchestration complexity and removes extra downstream dependencies while preserving business data fidelity.
