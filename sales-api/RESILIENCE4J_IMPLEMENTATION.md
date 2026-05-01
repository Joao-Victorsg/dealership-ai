# Resilience4j Logging & Metrics Implementation

## Overview
Implemented comprehensive logging and metrics collection for resilience4j circuit breaker and retry patterns in the SNS Publisher. This provides full observability into resilience4j behavior with structured JSON logging and Prometheus metrics.

## Changes Made

### 1. **Dependencies Added** (pom.xml)
- `resilience4j-micrometer` (v2.3.0): Enables metrics collection for circuit breaker and retry events
- `logstash-logback-encoder` (v7.4): Provides JSON formatted log output

### 2. **Configuration Class** (Resilience4jConfig.java)
Created a new Spring configuration class in `src/main/java/br/com/dealership/salesapi/config/Resilience4jConfig.java` that:

#### Circuit Breaker Event Listeners
- Listens to `onStateTransition` events from the circuit breaker
- Logs JSON events when state changes (CLOSED → OPEN → HALF_OPEN)
- Log format:
  ```json
  {
    "event_type": "CIRCUIT_BREAKER_STATE_CHANGED",
    "instance": "sns",
    "from_state": "CLOSED",
    "to_state": "OPEN",
    "timestamp": "2026-04-25T00:35:00Z"
  }
  ```

#### Retry Event Listeners
- Listens to `onRetry` events: Logs each retry attempt with attempt number and exception type
- Listens to `onError` events: Logs when retry limit is exhausted
- Log formats:
  ```json
  {
    "event_type": "RETRY_ATTEMPT",
    "instance": "sns",
    "attempt_number": 2,
    "exception_type": "SnsException",
    "timestamp": "2026-04-25T00:35:00Z"
  }
  ```
  ```json
  {
    "event_type": "RETRY_EXHAUSTED",
    "instance": "sns",
    "total_attempts": 3,
    "exception_type": "SnsException",
    "exception_message": "Failed to reach SNS endpoint",
    "timestamp": "2026-04-25T00:35:00Z"
  }
  ```

### 3. **Application Properties** (application.properties)
Updated configuration:
- Enabled resilience4j metrics collection: `management.metrics.enable.resilience4j=true`
- Exposed metrics endpoint: Added `prometheus` to `management.endpoints.web.exposure.include`
- Set logging levels:
  - `logging.level.br.com.dealership.salesapi.config.Resilience4jConfig=INFO` (captures events)
  - `logging.level.io.github.resilience4j=WARN` (suppresses verbose library logs)

### 4. **Logback Configuration** (logback-spring.xml)
Created comprehensive logging configuration:
- **Console Appender**: Standard logging to console for all logs
- **Resilience4j File Appender**: Uses Logstash JSON encoder for structured JSON output
- **Async Appender**: Improves performance by async writing of JSON logs
- Logs are written to `logs/sales-api.log` with automatic rotation (10MB per file, 30-day retention)

## Observability Features

### 1. **JSON Structured Logging**
- All resilience4j events logged as JSON for easy parsing and analysis
- Consistent event structure with `event_type`, `instance`, `timestamp`, and event-specific fields
- Integrated with log aggregation systems (ELK, Splunk, etc.)

### 2. **Prometheus Metrics**
Metrics automatically exposed at `/actuator/prometheus`:
- `resilience4j_circuitbreaker_state`: Current circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_calls_total`: Total calls by outcome (success/failure)
- `resilience4j_circuitbreaker_calls_metrics`: Call duration metrics
- `resilience4j_retry_calls_total`: Total retry calls by outcome
- `resilience4j_retry_calls_metrics`: Retry-specific metrics

### 3. **Metrics Integration**
- New Relic already configured to ingest metrics via `micrometer-registry-new-relic`
- All resilience4j metrics automatically published to New Relic dashboard
- Metrics available for alerting and dashboarding

## How It Works

### Circuit Breaker Monitoring
1. When circuit breaker state changes (e.g., CLOSED → OPEN), `Resilience4jConfig` logs a JSON event
2. Event captured by async appender and written to `logs/sales-api.log`
3. Prometheus scraper collects `resilience4j_circuitbreaker_state` metric
4. New Relic ingests the metric for monitoring

### Retry Monitoring
1. When a retry is triggered, `Resilience4jConfig` logs a JSON event with attempt number
2. When max retries exceeded, logs `RETRY_EXHAUSTED` event with exception details
3. Prometheus collects retry metrics: `resilience4j_retry_calls_total`
4. New Relic provides visibility into retry patterns

## Testing & Verification

To verify the implementation:

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Check Actuator endpoints:**
   - Metrics: `http://localhost:8080/actuator/metrics`
   - Resilience4j metrics: `http://localhost:8080/actuator/metrics?tag=resilience4j`
   - Prometheus metrics: `http://localhost:8080/actuator/prometheus`

4. **Check JSON logs:**
   ```bash
   tail -f logs/sales-api.log
   ```

## MDC Integration (Future Enhancement)

The current implementation can be enhanced to include MDC (Mapped Diagnostic Context) for request tracking:
- Modify `Resilience4jConfig` to include MDC fields (e.g., `saleId`, `clientId`) in JSON events
- This enables end-to-end tracing of a sale through retry/circuit breaker events
- Already implemented in `SaleService` with MDC fields

## Notes

- JSON encoder uses LogStash format for compatibility with log aggregation platforms
- Async appender prevents blocking on I/O operations
- Log retention set to 30 days with 1GB total size cap
- All logs include application name and timestamp for correlation

