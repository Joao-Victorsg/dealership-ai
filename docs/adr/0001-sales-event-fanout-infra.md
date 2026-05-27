# Split SNS/SQS ownership for Car Sold Event fanout

We decided to provision eventing as shared infrastructure split into `infra-sns` and `infra-sqs`, with `sales-topic` owned by `infra-sns` and the consumer queues/subscriptions owned by `infra-sqs`. This separates publication from consumption concerns, avoids coupling messaging ownership to `sales-api/infra`, and prepares future lambdas through two dedicated consumer queues (`car-status-queue`, `invoice-queue`) with per-queue DLQs for failure isolation.
