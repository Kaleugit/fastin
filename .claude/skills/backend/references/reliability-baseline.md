# Reliability Baseline

## Timeouts And Retries
- Every external dependency call needs bounded timeout.
- Retry only idempotent/known-safe operations.
- Use bounded retry and backoff.

## Error Handling
- Fail loudly in logs, safely for end users.
- Do not swallow exceptions that hide real incidents.
- Keep error classes/codes meaningful.

## Idempotency
- Protect operations that can be replayed (queue/webhook/client retry).
- Use idempotency keys or dedup strategies where required.

## Observability
- Emit logs with correlation/request identifiers.
- Track core service metrics (latency, error rate, throughput).
- Add enough signals for root-cause diagnosis.
