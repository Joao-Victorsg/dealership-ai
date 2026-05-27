# Dealership Eventing

This context defines the shared language for sale-driven asynchronous integrations in Dealership. It exists to keep event, topic, and queue semantics consistent across services and automations.

## Language

**Car Sold Event**:
A domain event emitted when a vehicle sale is finalized.
_Avoid_: Sale notification, purchase event

**Sales Topic**:
The shared publication channel that receives **Car Sold Event** and fans out to downstream consumers.
_Avoid_: Sales queue, sales-api queue

**Car Status Queue**:
The queue that receives **Car Sold Event** to trigger car status update processing.
_Avoid_: Car queue, inventory queue

**Invoice Queue**:
The queue that receives **Car Sold Event** to trigger invoice workflow processing.
_Avoid_: Billing queue, email queue

**Sale Event Payload**:
The canonical sales message carrying sale identifiers, monetary value, timestamp, and full client/car snapshots.
_Avoid_: Minimal sale event, partial payload

**Invoice Workflow**:
The asynchronous process that generates the invoice artifact and notifies the customer from a **Sale Event Payload**.
_Avoid_: Billing API flow, sync invoice flow

**Transaction Tax**:
The platform surcharge applied at checkout and reflected in the sale total.
_Avoid_: Service fee, processing fee

**Dead-letter Queue (DLQ)**:
A failure-isolation queue that stores messages a consumer queue could not process after retries.
_Avoid_: Retry queue, backup queue

## Relationships

- **Sales API** publishes **Car Sold Event** to **Sales Topic**
- **Car Sold Event** is serialized as a **Sale Event Payload**
- **Sale Event Payload** carries a sale total that already includes **Transaction Tax**
- **Sales Topic** delivers **Car Sold Event** to **Car Status Queue**
- **Sales Topic** delivers **Car Sold Event** to **Invoice Queue**
- **Invoice Workflow** consumes **Sale Event Payload** directly from **Invoice Queue**
- **Car Status Queue** routes failed messages to its **Dead-letter Queue (DLQ)**
- **Invoice Queue** routes failed messages to its **Dead-letter Queue (DLQ)**

## Example dialogue

> **Dev:** "When a sale is completed, do we publish directly to a queue?"
> **Domain expert:** "No — we publish a **Car Sold Event** to the **Sales Topic**, and each consumer queue receives its own copy."

## Flagged ambiguities

- "Sales messaging owned by sales-api infra" vs "shared integration infra" — resolved: **Sales Topic** and consumer queues belong to a dedicated shared infra.
- "Invoice workflow needs extra API lookups" vs "event already carries snapshots" — resolved: **Sale Event Payload** is complete enough for invoice generation without enrichment fetches.
- "Invoice tax rule (2% legacy) vs checkout tax rule" — resolved: **Transaction Tax** follows the checkout rule (10%) already reflected in sale totals.
