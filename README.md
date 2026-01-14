# KYC Service

KYC decisioning service that aggregates multiple verification providers and produces a final KYC decision.

---

## Features

- REST API (`/kyc/verify`)
- Parallel calls to external verification providers
- Rule-based decision engine (`APPROVED`, `MANUAL_REVIEW`, `REJECTED`)
- Configurable timeouts, retries and thresholds
- Rate limiting (pluggable)
- Structured logging
- Micrometer metrics
- Externalized configuration (`application.yml`)
- Test coverage (unit + integration)

---

## Logging

- Logs input and output of the decision engine  
- Logs provider results and final decision  
- Can be shipped to ELK / OpenSearch / Splunk via DevOps pipeline  
- Traceability can be extended with correlation IDs  

---

## Metrics

Micrometer metrics exposed via `/actuator/prometheus`:

- `kyc.decision.total` — decision count by result  
- `kyc.decision.reason` — decision count by reason  
- `kyc.decision.latency` — end-to-end processing latency  

Dashboards and alerts can be added through Prometheus → Grafana.

---

## Rate Limiting

Current implementation uses an in-memory token bucket (`SimpleRateLimiter`).

For production deployments distributed rate limiting is required:

**Options:**

- Redis (`INCR` + `EXPIRE` + Lua scripts)
- PostgreSQL (bucket tables + `FOR UPDATE SKIP LOCKED`)
- Kafka-based throttling (high-throughput scenario)

> Reason: multiple KYC service instances must enforce synchronized limits.

---

## Configuration Example

```yaml
kyc:
  endpoints:
    document: "http://localhost:9561"
    biometric: "http://localhost:9562"
    address: "http://localhost:9563"
    sanctions: "http://localhost:9564"
  timeout:
    document: 5s
    biometric: 8s
    address: 5s
    sanctions: 3s
  retry:
    attempts: 3
    backoff: 200ms
  rate-limit:
    window-millis: 60000
    limit: 10
  decision:
    confidence-threshold: 80
